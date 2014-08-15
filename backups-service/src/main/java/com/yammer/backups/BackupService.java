package com.yammer.backups;

/*
 * #%L
 * Backups
 * %%
 * Copyright (C) 2013 - 2014 Microsoft Corporation
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.codahale.metrics.health.HealthCheck;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.microsoft.windowsazure.services.blob.client.CloudBlobContainer;
import com.sun.jersey.api.model.Parameter;
import com.sun.jersey.spi.inject.InjectableProvider;
import com.yammer.backups.api.ClientPermission;
import com.yammer.backups.api.Location;
import com.yammer.backups.api.Node;
import com.yammer.backups.api.metadata.AbstractMetadata;
import com.yammer.backups.api.metadata.BackupMetadata;
import com.yammer.backups.api.metadata.VerificationMetadata;
import com.yammer.backups.auth.TemporaryTokenGenerator;
import com.yammer.backups.auth.TokenAuthProvider;
import com.yammer.backups.auth.TokenAuthenticator;
import com.yammer.backups.auth.TypedAuthProvider;
import com.yammer.backups.codec.BackupCodecFactory;
import com.yammer.backups.codec.CodecFactory;
import com.yammer.backups.config.BackupConfiguration;
import com.yammer.backups.error.*;
import com.yammer.backups.healthchecks.FileStorageConnectivityHealthCheck;
import com.yammer.backups.healthchecks.ScheduledHealthCheck;
import com.yammer.backups.healthchecks.file.MissingBackupsHealthCheck;
import com.yammer.backups.healthchecks.file.StorageCapacityHealthCheck;
import com.yammer.backups.healthchecks.metadata.FailedMetadataHealthCheck;
import com.yammer.backups.healthchecks.metadata.OverdueMetadataHealthCheck;
import com.yammer.backups.config.DistributedLockConfiguration;
import com.yammer.backups.lock.DistributedLockManager;
import com.yammer.backups.lock.LockManager;
import com.yammer.backups.policy.*;
import com.yammer.backups.processor.BackupProcessor;
import com.yammer.backups.processor.BackupProcessorListener;
import com.yammer.backups.processor.ServiceRegistry;
import com.yammer.backups.processor.VerificationProcessor;
import com.yammer.backups.processor.scheduled.OrphanedVerificationProcessor;
import com.yammer.backups.processor.scheduled.RetentionPolicyProcessor;
import com.yammer.backups.processor.scheduled.TimedOutMetadataProcessor;
import com.yammer.backups.provider.BackupMetadataProvider;
import com.yammer.backups.provider.VerificationMetadataProvider;
import com.yammer.backups.resources.DashboardResource;
import com.yammer.backups.resources.api.BackupResource;
import com.yammer.backups.resources.api.ServicesResource;
import com.yammer.backups.resources.api.StatusResource;
import com.yammer.backups.resources.api.VerificationResource;
import com.yammer.backups.service.metadata.ServiceMetadata;
import com.yammer.backups.storage.metadata.MetadataStorage;
import com.yammer.backups.storage.metadata.azure.AzureTableMetadataStorage;
import com.yammer.dropwizard.authenticator.LdapAuthenticator;
import com.yammer.dropwizard.authenticator.ResourceAuthenticator;
import com.yammer.storage.file.FileStorage;
import com.yammer.storage.file.azure.AzureFileStorage;
import com.yammer.storage.file.instrumented.InstrumentedFileStorage;
import com.yammer.storage.file.local.LocalFileStorage;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.auth.Auth;
import io.dropwizard.auth.CachingAuthenticator;
import io.dropwizard.auth.basic.BasicAuthProvider;
import io.dropwizard.auth.basic.BasicCredentials;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;
import io.dropwizard.views.ViewBundle;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.EnumSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

public class BackupService extends Application<BackupConfiguration> {

    private static final Duration HEALTH_CHECK_FREQUENCY = Duration.minutes(2);
    private static final String AZURE_LOCK_BUCKET = "DISTRIBUTEDLOCKS";

    @SuppressWarnings("SignatureDeclareThrowsException")
    public static void main(String[] args) throws Exception {
        new BackupService().run(args);
    }

    @Override
    public void initialize(Bootstrap<BackupConfiguration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/assets", "/assets", "index.htm", "assets"));
        bootstrap.addBundle(new AssetsBundle("/META-INF/resources/webjars", "/webjars", "index.htm", "webjars"));
        bootstrap.addBundle(new ViewBundle());
    }

    @Override
    public void run(BackupConfiguration configuration, Environment environment) throws URISyntaxException, InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        final FileStorage localStorage = new InstrumentedFileStorage("local-files", new LocalFileStorage(
            configuration.getLocalConfiguration().getStorageConfiguration()), environment.metrics());
        environment.lifecycle().manage(localStorage);

        final AzureFileStorage azureFileStorage = new AzureFileStorage(
                configuration.getOffsiteConfiguration().getStorageConfiguration(), "");
        final FileStorage offsiteStorage = new InstrumentedFileStorage("azure-files", azureFileStorage, environment.metrics());
        environment.lifecycle().manage(offsiteStorage);

        final CodecFactory codecFactory = new BackupCodecFactory(
                configuration.getEncryptionConfiguration(), configuration.getCompressionConfiguration().getCodec()
        );

        final int offsiteUploadExecutorThreads = configuration.getOffsiteConfiguration().getUploaderThreadPoolSize();
        final ExecutorService offsiteUploadWorkers = environment.lifecycle().executorService("offsite-uploader-worker-%s")
                .minThreads(offsiteUploadExecutorThreads).maxThreads(offsiteUploadExecutorThreads).build();

        final MetadataStorage<BackupMetadata> backupMetadataStorage = new AzureTableMetadataStorage<>(BackupMetadata.class, configuration.getOffsiteConfiguration().getStorageConfiguration(), "backups", environment.metrics());
        final MetadataStorage<VerificationMetadata> verificationMetadataStorage = new AzureTableMetadataStorage<>(VerificationMetadata.class, configuration.getOffsiteConfiguration().getStorageConfiguration(), "verifications", environment.metrics());
        final MetadataStorage<ClientPermission> clientPermissionMetadataStorage = new AzureTableMetadataStorage<>(ClientPermission.class, configuration.getOffsiteConfiguration().getStorageConfiguration(),"clientpermissions", environment.metrics());
        final MetadataStorage<Node> nodeMetadataStorage = new AzureTableMetadataStorage<>(Node.class,configuration.getOffsiteConfiguration().getStorageConfiguration(),"nodes", environment.metrics());
        final MetadataStorage<ServiceMetadata> serviceMetadataStorage = new AzureTableMetadataStorage<>(ServiceMetadata.class, configuration.getOffsiteConfiguration().getStorageConfiguration(), "services", environment.metrics());

        final Node node = new Node(configuration.getNodeConfiguration().getName(),configuration.getNodeConfiguration().getUrl());
        nodeMetadataStorage.put(node);

        final DistributedLockConfiguration lockConfiguration = configuration.getLockConfiguration();
        final ScheduledExecutorService lockExecutor = environment.lifecycle().scheduledExecutorService("lock-%s").threads(lockConfiguration.getThreadPoolSize()).build();

        final CloudBlobContainer bucket = azureFileStorage.getBucket(AZURE_LOCK_BUCKET);
        final DistributedLockManager lockManager = new DistributedLockManager(
                new LockManager(lockExecutor, bucket, lockConfiguration.getRefreshFrequency().getQuantity(), lockConfiguration.getRefreshFrequency().getUnit()),
                lockConfiguration.getAcquireTimeout(), Duration.seconds(1)
        );

        final FileStorage backupLogStorage = new InstrumentedFileStorage("azure-backup-logs", new AzureFileStorage(
                configuration.getOffsiteConfiguration().getStorageConfiguration(), "backuplogs"
        ), environment.metrics());

        final ServiceRegistry serviceRegistry = new ServiceRegistry(serviceMetadataStorage);

        // Main handler for backups
        final BackupProcessor backupProcessor = new BackupProcessor(lockManager, backupMetadataStorage, localStorage, offsiteStorage,
                codecFactory, offsiteUploadWorkers, configuration.getChunkSize(), backupLogStorage, node.getName(),
                configuration.getCompressionConfiguration().getFileExtensions(), environment.metrics(), ImmutableList.<BackupProcessorListener>of(serviceRegistry));
        environment.lifecycle().manage(backupProcessor);

        // Backup timeout processing
        final TimedOutMetadataProcessor<BackupMetadata> backupTimeoutProcessor = this.createTimedOutProcessor(
                backupMetadataStorage,
                configuration.getTimeoutDuration(),
                configuration.getNodeConfiguration().getName(),
                environment);
        environment.lifecycle().manage(backupTimeoutProcessor);

        final FileStorage verificationLogStorage = new InstrumentedFileStorage("azure-verification-logs", new AzureFileStorage(
                configuration.getOffsiteConfiguration().getStorageConfiguration(), "verificationlogs"
        ), environment.metrics());

        // Main handler for verifications
        final VerificationProcessor verificationProcessor = new VerificationProcessor(lockManager, verificationMetadataStorage, verificationLogStorage, node.getName(), backupProcessor);
        environment.lifecycle().manage(verificationProcessor);

        // Verification timeout processing
        final TimedOutMetadataProcessor<VerificationMetadata> verificationTimeoutProcessor = this.createTimedOutProcessor(
                verificationMetadataStorage,
                configuration.getTimeoutDuration(),
                configuration.getNodeConfiguration().getName(),
                environment
        );
        environment.lifecycle().manage(verificationTimeoutProcessor);

        // Orphaned verifications
        final OrphanedVerificationProcessor orphanedVerificationProcessor = this.createOrphanedVerificationProcessor(
                verificationMetadataStorage, backupMetadataStorage, configuration.getNodeConfiguration().getName(), environment
        );
        environment.lifecycle().manage(orphanedVerificationProcessor);

        // Retention policy processing
        final RetentionPolicyProcessor localRetentionPolicyProcessor = this.createRetentionPolicyProcessor(
                localStorage,
                Location.LOCAL,
                backupProcessor,
                configuration.getLocalConfiguration().getRetentionPolicyConfiguration(),
                configuration.getNodeConfiguration().getName(),
                environment
        );
        environment.lifecycle().manage(localRetentionPolicyProcessor);

        final RetentionPolicyProcessor offsiteRetentionPolicyProcessor = this.createRetentionPolicyProcessor(
                offsiteStorage,
                Location.OFFSITE,
                backupProcessor,
                configuration.getOffsiteConfiguration().getRetentionPolicyConfiguration(),
                configuration.getNodeConfiguration().getName(),
                environment
        );
        environment.lifecycle().manage(offsiteRetentionPolicyProcessor);

        final ScheduledExecutorService failedRetentionPolicyExecutor = environment.lifecycle().scheduledExecutorService("failed-retention-policy-%s").build();
        final RetentionPolicyProcessor failedRetentionPolicyProcessor = new RetentionPolicyProcessor(
                localStorage,
                Location.LOCAL,
                backupProcessor,
                failedRetentionPolicyExecutor,
                new FailedRetentionPolicy(backupMetadataStorage),
                EnumSet.of(BackupMetadata.State.FAILED, BackupMetadata.State.TIMEDOUT),
                configuration.getNodeConfiguration().getName(),
                environment.metrics()
        );

        environment.lifecycle().manage(failedRetentionPolicyProcessor);

        // Map exceptions
        environment.jersey().register(new IncorrectNodeExceptionMapper(nodeMetadataStorage));
        environment.jersey().register(new NoContentExceptionMapper());
        environment.jersey().register(new MetadataNotFoundExceptionMapper());
        environment.jersey().register(new InvalidMD5ExceptionMapper());
        environment.jersey().register(new IllegalStateExceptionMapper());

        // Check if we know of any finished backups locally that are missing in storage
        this.addScheduledHealthCheck(
                environment,
                new MissingBackupsHealthCheck(
                        backupMetadataStorage,
                        localStorage,
                        Location.LOCAL,
                        configuration.getNodeConfiguration().getName(),
                        serviceRegistry
                ),
                "Backups files for " + localStorage
        );

        this.addScheduledHealthCheck(
                environment,
                new MissingBackupsHealthCheck(
                        backupMetadataStorage,
                        offsiteStorage,
                        Location.OFFSITE,
                        configuration.getNodeConfiguration().getName(),
                        serviceRegistry
                ),
                "Backups files for " + offsiteStorage
        );

        // Check we can connect to the file storage
        environment.healthChecks().register("Connectivity for " + localStorage, new FileStorageConnectivityHealthCheck(localStorage));
        environment.healthChecks().register("Connectivity for " + offsiteStorage, new FileStorageConnectivityHealthCheck(offsiteStorage));

        // Check if we know of any backups that failed
        this.addScheduledHealthCheck(
                environment,
                new FailedMetadataHealthCheck<>(backupMetadataStorage, configuration.getNodeConfiguration().getName(), serviceRegistry),
                "Backups failed"
        );

        // Check if we know of any verification that failed
        this.addScheduledHealthCheck(
                environment,
                new FailedMetadataHealthCheck<>(verificationMetadataStorage, configuration.getNodeConfiguration().getName(), serviceRegistry),
                "Verifications failed"
        );

        // Check if we know of any backups that are overdue
        this.addScheduledHealthCheck(
                environment,
                new OverdueMetadataHealthCheck<>(backupMetadataStorage, configuration.getBackupRequiredFrequency(), configuration.getNodeConfiguration().getName(), serviceRegistry),
                "Backups overdue"
        );

        this.addScheduledHealthCheck(
                environment,
                new OverdueMetadataHealthCheck<>(verificationMetadataStorage, configuration.getVerificationRequiredFrequency(), configuration.getNodeConfiguration().getName(), serviceRegistry),
                "Verifications overdue"
        );

        // Check we have 100 Gb space on storages
        this.addScheduledHealthCheck(
                environment,
                new StorageCapacityHealthCheck(localStorage, configuration.getLocalConfiguration().getStorageConfiguration().getMinCapacity()),
                "Available space for " + localStorage
        );

        this.addScheduledHealthCheck(
                environment,
                new StorageCapacityHealthCheck(offsiteStorage, configuration.getOffsiteConfiguration().getStorageConfiguration().getMinCapacity()),
                "Available space for " + offsiteStorage
        );

        // ldap auth
        final LdapAuthenticator ldapAuthenticator = new LdapAuthenticator(configuration.getLdapConfiguration());
        final ResourceAuthenticator ldapResourceAuthenticator = new ResourceAuthenticator(ldapAuthenticator);
        final BasicAuthProvider<BasicCredentials> ldapProvider = new BasicAuthProvider<>(new CachingAuthenticator<>(
                environment.metrics(), ldapResourceAuthenticator, configuration.getLdapConfiguration().getCachePolicy()
        ), "backups");

        // Token auth
        final TemporaryTokenGenerator tokenGenerator = new TemporaryTokenGenerator(configuration.getTokenConfiguration());
        final TokenAuthenticator tokenAuthenticator = new TokenAuthenticator(clientPermissionMetadataStorage, tokenGenerator);
        final TokenAuthProvider<ClientPermission> tokenProvider = new TokenAuthProvider<>(tokenAuthenticator);

        environment.jersey().register(new TypedAuthProvider(ImmutableMap.<Type, InjectableProvider<Auth, Parameter>>of(
                BasicCredentials.class, ldapProvider,
                ClientPermission.class, tokenProvider
        )));

        environment.jersey().register(new BackupMetadataProvider(backupMetadataStorage));
        environment.jersey().register(new VerificationMetadataProvider(verificationMetadataStorage));

        environment.jersey().register(new DashboardResource(tokenGenerator));
        environment.jersey().register(new StatusResource(backupProcessor, localStorage, offsiteStorage));
        environment.jersey().register(new BackupResource(
                backupProcessor, clientPermissionMetadataStorage,
                configuration.getNodeConfiguration().getName()));
        environment.jersey().register(new VerificationResource(verificationProcessor));
        environment.jersey().register(new ServicesResource(serviceRegistry));
    }

    private void addScheduledHealthCheck(Environment environment, HealthCheck healthCheck, String name) {
        final ScheduledExecutorService executor = environment.lifecycle().scheduledExecutorService("healthcheck-%s").build();
        final ScheduledHealthCheck wrappedHealthCheck = ScheduledHealthCheck.wrap(healthCheck, name, executor, HEALTH_CHECK_FREQUENCY, environment.metrics());

        environment.healthChecks().register(name, wrappedHealthCheck);
        environment.lifecycle().manage(wrappedHealthCheck);
    }

    private RetentionPolicyProcessor createRetentionPolicyProcessor(
            FileStorage fileStorage,
            Location location,
            BackupProcessor backupProcessor,
            RetentionPolicyConfiguration config,
            String nodeName,
            Environment environment) {
        final ScheduledExecutorService retentionPolicyExecutor = environment.lifecycle().scheduledExecutorService("retention-policy-%s").build();

        return new RetentionPolicyProcessor(
                fileStorage,
                location,
                backupProcessor,
                retentionPolicyExecutor,
                new CombinedRetentionPolicy<>(
                        // keep daily, weekly, monthly, yearly
                        new DWMYRetentionPolicy(
                                config.getDaily(),
                                config.getWeekly(),
                                config.getMonthly(),
                                config.getYearly()),
                        // keep all from today
                        new LastDurationRetentionPolicy<BackupMetadata>(config.getMinDuration()),
                        // keep the last 1
                        new LastCountRetentionPolicy<BackupMetadata>(config.getMinCount())
                    ),
                EnumSet.of(BackupMetadata.State.FINISHED),
                nodeName,
                environment.metrics()
        );
    }

    private <T extends AbstractMetadata<?>> TimedOutMetadataProcessor<T> createTimedOutProcessor(
            MetadataStorage<T> storage,
            Duration timeout,
            String nodeName,
            Environment environment) {
        final ScheduledExecutorService timeoutExecutor = environment.lifecycle().scheduledExecutorService("timeout-%s").build();
        return new TimedOutMetadataProcessor<>(storage, timeoutExecutor, timeout, nodeName, environment.metrics());
    }

    private OrphanedVerificationProcessor createOrphanedVerificationProcessor(
            MetadataStorage<VerificationMetadata> verificationStorage,
            MetadataStorage<BackupMetadata> backupStorage,
            String nodeName,
            Environment environment) {
        final ScheduledExecutorService orphanedVerificationsExecutor = environment.lifecycle().scheduledExecutorService("orphaned-verifications-%s").build();
        return new OrphanedVerificationProcessor(orphanedVerificationsExecutor, verificationStorage, backupStorage, nodeName, environment.metrics());
    }
}

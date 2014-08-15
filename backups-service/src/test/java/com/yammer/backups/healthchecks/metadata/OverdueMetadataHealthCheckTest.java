package com.yammer.backups.healthchecks.metadata;

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

import com.yammer.backups.api.metadata.BackupMetadata;
import com.yammer.backups.processor.ServiceRegistry;
import com.yammer.backups.service.metadata.ServiceMetadata;
import com.yammer.backups.storage.metadata.MetadataStorage;
import com.yammer.backups.storage.metadata.memory.InMemoryMetadataStorage;
import io.dropwizard.util.Duration;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OverdueMetadataHealthCheckTest {

    private static BackupMetadata createBackup(String service, BackupMetadata.State state) {
        final BackupMetadata backup = new BackupMetadata(service, UUID.randomUUID().toString(), "localhost");
        backup.setState(state, "test");
        return backup;
    }

    private static void setDateOffset(Duration duration) {
        DateTimeUtils.setCurrentMillisOffset(duration.toMilliseconds());
    }

    private MetadataStorage<BackupMetadata> storage;
    private OverdueMetadataHealthCheck<BackupMetadata> healthcheck;
    private ServiceRegistry serviceRegistry;

    @Before
    public void setUp() {
        storage = new InMemoryMetadataStorage<>();
        serviceRegistry = new ServiceRegistry(new InMemoryMetadataStorage<ServiceMetadata>());
        healthcheck = new OverdueMetadataHealthCheck<>(storage, Duration.days(1), "localhost", serviceRegistry);
    }

    @After
    public void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void testNoBackups() throws Exception {
        assertTrue(healthcheck.check().isHealthy());
    }

    @Test
    public void testCompletedBackups() throws Exception {
        storage.put(createBackup("feedie", BackupMetadata.State.FINISHED));
        storage.put(createBackup("hermes", BackupMetadata.State.FINISHED));

        assertTrue(healthcheck.check().isHealthy());
    }

    @Test
    public void testNotCompletedNotBackupsIgnored() throws Exception {
        storage.put(createBackup("feedie", BackupMetadata.State.WAITING));
        storage.put(createBackup("feedie", BackupMetadata.State.RECEIVING));
        storage.put(createBackup("feedie", BackupMetadata.State.QUEUED));
        storage.put(createBackup("feedie", BackupMetadata.State.UPLOADING));
        storage.put(createBackup("feedie", BackupMetadata.State.FINISHED));

        assertTrue(healthcheck.check().isHealthy());

        // Shift time by a week
        setDateOffset(Duration.days(7));

        assertFalse(healthcheck.check().isHealthy());

        storage.put(createBackup("feedie", BackupMetadata.State.FINISHED));
        assertTrue(healthcheck.check().isHealthy());
    }

    @Test
    public void testNoCompletedBackupsRecentlyTriggersHealthcheck() throws Exception {
        storage.put(createBackup("feedie", BackupMetadata.State.FINISHED));

        assertTrue(healthcheck.check().isHealthy());

        // Shift time by a week
        setDateOffset(Duration.days(7));

        assertFalse(healthcheck.check().isHealthy());
    }

    @Test
    public void testServiceHealthcheckDisabled() throws Exception {
        final BackupMetadata backup = createBackup("feedie", BackupMetadata.State.FINISHED);
        storage.put(backup);
        serviceRegistry.backupCreated(backup);

        assertTrue(healthcheck.check().isHealthy());

        // Shift time by a week
        setDateOffset(Duration.days(7));
        assertFalse(healthcheck.check().isHealthy());

        serviceRegistry.updateServiceIfPresent(new ServiceMetadata("feedie", true));
        assertTrue(healthcheck.check().isHealthy());
    }

    @Test
    public void testServicesAreCheckedIndividually() throws Exception {
        storage.put(createBackup("feedie", BackupMetadata.State.FINISHED));
        storage.put(createBackup("hermes", BackupMetadata.State.FINISHED));

        assertTrue(healthcheck.check().isHealthy());

        // Shift time by a week
        setDateOffset(Duration.days(7));

        storage.put(createBackup("hermes", BackupMetadata.State.FINISHED));
        assertFalse(healthcheck.check().isHealthy());

        storage.put(createBackup("feedie", BackupMetadata.State.FINISHED));
        assertTrue(healthcheck.check().isHealthy());
    }
}

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
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FailedMetadataHealthCheckTest {
    private static BackupMetadata createBackup(String service, BackupMetadata.State state) {
        final BackupMetadata backup = new BackupMetadata(service, UUID.randomUUID().toString(), "localhost");
        backup.setState(state, "test");
        return backup;
    }

    private MetadataStorage<BackupMetadata> storage;
    private ServiceRegistry serviceRegistry;
    private FailedMetadataHealthCheck<?> healthcheck;

    @Before
    public void setUp() throws Exception {
        setDateOffset(Duration.hours(0));
        storage = new InMemoryMetadataStorage<>();
        serviceRegistry = new ServiceRegistry(new InMemoryMetadataStorage<ServiceMetadata>());

        healthcheck = new FailedMetadataHealthCheck<>(storage, "localhost", serviceRegistry);
    }

    private static void setDateOffset(Duration duration) {
        DateTimeUtils.setCurrentMillisOffset(duration.toMilliseconds());
    }

    @Test
    public void testNoBackups() throws Exception {
        assertTrue(healthcheck.check().isHealthy());
    }

    @Test
    public void testOnlyCompletedBackups() throws Exception {
        storage.put(createBackup("feedie", BackupMetadata.State.FINISHED));
        setDateOffset(Duration.hours(1));
        storage.put(createBackup("feedie", BackupMetadata.State.FINISHED));
        setDateOffset(Duration.hours(2));
        storage.put(createBackup("hermes", BackupMetadata.State.FINISHED));

        assertTrue(healthcheck.check().isHealthy());
    }

    @Test
    public void testCompletedOrPendingBackups() throws Exception {
        storage.put(createBackup("feedie", BackupMetadata.State.WAITING));
        setDateOffset(Duration.hours(1));
        storage.put(createBackup("feedie", BackupMetadata.State.RECEIVING));
        setDateOffset(Duration.hours(2));
        storage.put(createBackup("feedie", BackupMetadata.State.QUEUED));
        setDateOffset(Duration.hours(3));
        storage.put(createBackup("feedie", BackupMetadata.State.UPLOADING));
        setDateOffset(Duration.hours(4));
        storage.put(createBackup("feedie", BackupMetadata.State.FINISHED));

        assertTrue(healthcheck.check().isHealthy());
    }

    @Test
    public void testRecentFailuresSuppressed() {
        storage.put(createBackup("feedie", BackupMetadata.State.FAILED));
        assertTrue(healthcheck.check().isHealthy());

        setDateOffset(Duration.hours(1));
        assertFalse(healthcheck.check().isHealthy());
    }

    @Test
    public void testServiceHealthcheckDisabled() {
        final BackupMetadata service = createBackup("feedie", BackupMetadata.State.FAILED);
        storage.put(service);
        serviceRegistry.backupCreated(service);

        setDateOffset(Duration.hours(1));
        assertFalse(healthcheck.check().isHealthy());
        serviceRegistry.updateServiceIfPresent(new ServiceMetadata("feedie", true));
        assertTrue(healthcheck.check().isHealthy());
    }

    @Test
    public void testTimeoutBackupTriggersHealthcheck() throws Exception {
        storage.put(createBackup("feedie", BackupMetadata.State.FINISHED));
        setDateOffset(Duration.hours(1));
        storage.put(createBackup("feedie", BackupMetadata.State.TIMEDOUT));

        setDateOffset(Duration.hours(2));
        assertFalse(healthcheck.check().isHealthy());
    }

    @Test
    public void testFailedBackupTriggersHealthcheck() throws Exception {
        storage.put(createBackup("feedie", BackupMetadata.State.FINISHED));
        setDateOffset(Duration.hours(1));
        storage.put(createBackup("feedie", BackupMetadata.State.FAILED));

        setDateOffset(Duration.hours(2));
        assertFalse(healthcheck.check().isHealthy());
    }

    @Test
    public void testPreviousFailedLatestFinished() throws Exception {
        storage.put(createBackup("feedie", BackupMetadata.State.FINISHED));
        setDateOffset(Duration.hours(1));
        storage.put(createBackup("feedie", BackupMetadata.State.FAILED));

        setDateOffset(Duration.hours(2));
        assertFalse(healthcheck.check().isHealthy());

        setDateOffset(Duration.hours(3));
        storage.put(createBackup("feedie", BackupMetadata.State.TIMEDOUT));

        setDateOffset(Duration.hours(4));
        assertFalse(healthcheck.check().isHealthy());

        setDateOffset(Duration.hours(5));
        storage.put(createBackup("other", BackupMetadata.State.FINISHED));

        setDateOffset(Duration.hours(6));
        assertFalse(healthcheck.check().isHealthy());

        setDateOffset(Duration.hours(7));
        storage.put(createBackup("feedie", BackupMetadata.State.FINISHED));

        setDateOffset(Duration.hours(8));
        assertTrue(healthcheck.check().isHealthy());
    }
}

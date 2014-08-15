package com.yammer.backups.healthchecks.file;

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

import com.yammer.backups.api.CompressionCodec;
import com.yammer.backups.api.Location;
import com.yammer.backups.api.metadata.BackupMetadata;
import com.yammer.backups.processor.ServiceRegistry;
import com.yammer.backups.storage.metadata.MetadataStorage;
import com.yammer.backups.storage.metadata.memory.InMemoryMetadataStorage;
import com.yammer.storage.file.FileStorage;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MissingBackupsHealthCheckTest {

    private static BackupMetadata createBackup(String service, BackupMetadata.State state) {
        final BackupMetadata backup = new BackupMetadata(service, UUID.randomUUID().toString(), "localhost");

        // Give the backup a file
        backup.setState(BackupMetadata.State.RECEIVING, "test");
        backup.addChunk("test1", "test1-id-1", 1000, 800, "hash", "localhost", CompressionCodec.SNAPPY);

        backup.addLocation(Location.LOCAL);
        backup.addLocation(Location.OFFSITE);

        backup.setState(state, "test");
        return backup;
    }

    private MetadataStorage<BackupMetadata> metadataStorage;
    private FileStorage fileStorage;
    private MissingBackupsHealthCheck healthcheck;
    private final ServiceRegistry serviceRegistry = mock(ServiceRegistry.class);

    @Before
    public void setUp() {
        metadataStorage = new InMemoryMetadataStorage<>();

        fileStorage = mock(FileStorage.class);
        healthcheck = new MissingBackupsHealthCheck(metadataStorage, fileStorage, Location.LOCAL, "localhost", serviceRegistry);
    }

    @Test
    public void testNoBackups() throws Exception {
        assertTrue(healthcheck.check().isHealthy());
    }

    @Test
    public void testMissingPendingBackups() throws IOException {
        when(fileStorage.exists(anyString(), anyString())).thenReturn(false);

        metadataStorage.put(createBackup("feedie", BackupMetadata.State.WAITING));
        metadataStorage.put(createBackup("feedie", BackupMetadata.State.RECEIVING));
        metadataStorage.put(createBackup("feedie", BackupMetadata.State.QUEUED));
        metadataStorage.put(createBackup("feedie", BackupMetadata.State.UPLOADING));

        when(serviceRegistry.healthCheckDisabled(anyString())).thenReturn(false);

        assertTrue(healthcheck.check().isHealthy());
    }

    @Test
    public void testMissingFinishedBackupsTriggersHealthcheck() throws IOException {
        when(fileStorage.exists(anyString(), anyString())).thenReturn(false);

        metadataStorage.put(createBackup("feedie", BackupMetadata.State.FINISHED));

        when(serviceRegistry.healthCheckDisabled(anyString())).thenReturn(false);

        assertFalse(healthcheck.check().isHealthy());
    }

    @Test
    public void testExistingFinishedBackups() throws IOException {
        when(fileStorage.exists(anyString(), anyString())).thenReturn(true);

        metadataStorage.put(createBackup("feedie", BackupMetadata.State.FINISHED));

        when(serviceRegistry.healthCheckDisabled(anyString())).thenReturn(false);

        assertTrue(healthcheck.check().isHealthy());
    }

    @Test
    public void testServiceHealthcheckDisabled() throws IOException {
        when(fileStorage.exists(anyString(), anyString())).thenReturn(false);

        metadataStorage.put(createBackup("feedie", BackupMetadata.State.FINISHED));

        when(serviceRegistry.healthCheckDisabled(anyString())).thenReturn(true);

        assertTrue(healthcheck.check().isHealthy());
    }
}

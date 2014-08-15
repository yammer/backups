package com.yammer.backups.policy;

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
import com.yammer.backups.storage.metadata.MetadataStorage;
import com.yammer.backups.storage.metadata.memory.InMemoryMetadataStorage;
import io.dropwizard.util.Duration;
import org.joda.time.DateTimeUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FailedRetentionPolicyTest {

    private static BackupMetadata createBackup(String service, Duration age) {
        DateTimeUtils.setCurrentMillisOffset(-age.toMilliseconds());
        final BackupMetadata backup = new BackupMetadata(service, UUID.randomUUID().toString(), "localhost");
        DateTimeUtils.setCurrentMillisOffset(0);

        backup.setState(BackupMetadata.State.FAILED, "test");
        return backup;
    }

    private MetadataStorage<BackupMetadata> metadataStorage;
    private RetentionPolicy<BackupMetadata> policy;

    @Before
    public void setUp() {
        metadataStorage = new InMemoryMetadataStorage<>();
        policy = new FailedRetentionPolicy(metadataStorage);
    }

    @Test
    public void testRetainsOnlyFailedBackup() {
        final BackupMetadata expected = createBackup("feedie", Duration.seconds(0));
        metadataStorage.put(expected);

        final Set<BackupMetadata> retained = policy.retain(metadataStorage.listAll());
        assertEquals(1, retained.size());
        assertTrue(retained.contains(expected));
    }

    @Test
    public void testRetainsLastSingleFailedBackup() {
        metadataStorage.put(createBackup("feedie", Duration.days(1)));

        final BackupMetadata expected = createBackup("feedie", Duration.days(0));
        metadataStorage.put(expected);

        final Set<BackupMetadata> retained = policy.retain(metadataStorage.listAll());
        assertEquals(1, retained.size());
        assertTrue(retained.contains(expected));
    }
}

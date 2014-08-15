package com.yammer.backups.processor.scheduled;

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

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import com.yammer.backups.api.metadata.BackupMetadata;
import com.yammer.backups.storage.metadata.MetadataStorage;
import com.yammer.backups.storage.metadata.memory.InMemoryMetadataStorage;
import io.dropwizard.util.Duration;
import org.joda.time.DateTimeUtils;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TimedOutMetadataProcessorTest {

    private static final String LOCAL_NODE = "localhost";

    private static BackupMetadata createBackup(String service, BackupMetadata.State state) {
        final BackupMetadata data = new BackupMetadata(service, UUID.randomUUID().toString(), "localhost");
        data.setState(state, "test");
        return data;
    }

    private MetadataStorage<BackupMetadata> storage;
    private TimedOutMetadataProcessor<BackupMetadata> processor;

    @Before
    public void setUp() {
        storage = new InMemoryMetadataStorage<>();
        processor = new TimedOutMetadataProcessor<>(storage, null, Duration.days(1), LOCAL_NODE, new MetricRegistry());
    }

    @Test
    public void testNoBackups() {
        processor.execute();
        assertTrue(storage.listAll().isEmpty());
    }

    @Test
    public void testCompletedBackups() {
        for (int i = 0;i < 10;i++) {
            DateTimeUtils.setCurrentMillisOffset(-Duration.days(i).toMilliseconds());
            storage.put(createBackup("feedie", BackupMetadata.State.FINISHED));
        }

        DateTimeUtils.setCurrentMillisOffset(0);
        processor.execute();

        for (BackupMetadata backup : storage.listAll()) {
            assertEquals(BackupMetadata.State.FINISHED, backup.getState());
        }
    }

    @Test
    public void testStartedBackupsTimeout() {
        for (int i = 0;i < 10;i++) {
            DateTimeUtils.setCurrentMillisOffset(-Duration.days(i).toMilliseconds());
            storage.put(createBackup("feedie", BackupMetadata.State.WAITING));
        }

        DateTimeUtils.setCurrentMillisOffset(0);
        processor.execute();

        final Set<BackupMetadata> timedout = Sets.filter(storage.listAll(), new Predicate<BackupMetadata>() {
            @Override
            public boolean apply(@Nullable BackupMetadata input) {
                return input != null && BackupMetadata.State.TIMEDOUT.equals(input.getState());
            }
        });

        assertEquals(9, timedout.size());
    }
}

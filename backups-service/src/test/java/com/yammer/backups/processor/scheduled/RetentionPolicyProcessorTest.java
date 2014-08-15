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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.yammer.backups.api.Location;
import com.yammer.backups.api.metadata.BackupMetadata;
import com.yammer.backups.policy.*;
import com.yammer.backups.processor.BackupProcessor;
import com.yammer.backups.storage.metadata.MetadataStorage;
import com.yammer.backups.storage.metadata.memory.InMemoryMetadataStorage;
import com.yammer.storage.file.FileStorage;
import io.dropwizard.util.Duration;
import org.joda.time.DateTimeUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class RetentionPolicyProcessorTest {

    private static final Random RANDOM = new Random();
    private static final String LOCAL_NODE = "localhost";

    private static BackupMetadata createBackup(String service) {
        final BackupMetadata backup = new BackupMetadata(service, UUID.randomUUID().toString(), "localhost");
        backup.addLocation(Location.OFFSITE);
        backup.setState(BackupMetadata.State.FINISHED, "test");

        return backup;
    }

    private FileStorage fileStorage;
    private MetadataStorage<BackupMetadata> storage;
    private BackupProcessor backupProcessor;
    private RetentionPolicy<BackupMetadata> retentionPolicy;
    private RetentionPolicyProcessor processor;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws IOException, URISyntaxException {
        storage = new InMemoryMetadataStorage<>();
        backupProcessor = mock(BackupProcessor.class);

        // Pass list requests through to our storage
        when(backupProcessor.listServices()).thenAnswer(new Answer<Set<String>>() {
            @Override
            public Set<String> answer(InvocationOnMock invocation) throws Throwable {
                return storage.listAllRows();
            }
        });

        when(backupProcessor.listMetadata()).thenAnswer(new Answer<Set<BackupMetadata>>() {
            @Override
            public Set<BackupMetadata> answer(InvocationOnMock invocation) throws Throwable {
                return storage.listAll();
            }
        });

        when(backupProcessor.listMetadata(anyString())).then(new Answer<Set<BackupMetadata>>() {
            @Override
            public Set<BackupMetadata> answer(InvocationOnMock invocation) throws Throwable {
                return storage.listAll((String) invocation.getArguments()[0]);
            }
        });

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final Location location = (Location) invocation.getArguments()[1];
                final BackupMetadata backup = (BackupMetadata) invocation.getArguments()[2];
                backup.removeLocation(location);

                if (!backup.existsAtLocation()) {
                    RetentionPolicyProcessorTest.this.storage.delete(backup);
                }

                return null;
            }
        }).when(backupProcessor).deleteFromFileStorage(any(FileStorage.class), any(Location.class), any(BackupMetadata.class));

        retentionPolicy = mock(RetentionPolicy.class);

        fileStorage = mock(FileStorage.class);

        processor = new RetentionPolicyProcessor(
                fileStorage,
                Location.OFFSITE,
                backupProcessor,
                null,
                retentionPolicy,
                EnumSet.of(BackupMetadata.State.FINISHED),
                LOCAL_NODE,
                new MetricRegistry()
        );

        for (int i = 0;i < 365;i++) {
            DateTimeUtils.setCurrentMillisOffset(-Duration.days(i).toMilliseconds());
            storage.put(createBackup("feedie"));
            storage.put(createBackup("hermes"));
        }

        DateTimeUtils.setCurrentMillisOffset(0);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRetainExpectedNumberOfBackups() throws IOException {
        final AtomicInteger retainedCount = new AtomicInteger(0);

        // Always retain a random 5 items
        when(retentionPolicy.retain(anySet())).thenAnswer(new Answer<Set<BackupMetadata>>() {
            @Override
            public Set<BackupMetadata> answer(InvocationOnMock invocation) throws Throwable {
                final List<BackupMetadata> items = ImmutableList.copyOf((Set<BackupMetadata>) invocation.getArguments()[0]);
                final Set<BackupMetadata> retained = Sets.newHashSet();

                for (int i = 0;i < 5;i++) {
                    final int pos = RANDOM.nextInt(items.size());
                    retained.add(items.get(pos));
                }

                retainedCount.addAndGet(retained.size());
                return retained;
            }
        });

        final int numBackups = storage.listAll().size();

        processor.execute();

        assertEquals(retainedCount.get(), storage.listAll().size());

        final int expected = numBackups - retainedCount.get();
        verify(backupProcessor, times(expected)).deleteFromFileStorage(eq(fileStorage), any(Location.class), any(BackupMetadata.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFailedDeleteDoesntEffectOthers() throws IOException {
        // We expect to retain all
        final int expected = storage.listAll().size();

        when(retentionPolicy.retain(anySet())).thenReturn(Collections.<BackupMetadata>emptySet());

        final BackupMetadata backup = storage.listAll().iterator().next();
        doThrow(new IOException("test")).when(backupProcessor).deleteFromFileStorage(eq(fileStorage), any(Location.class), eq(backup));

        processor.execute();

        verify(backupProcessor, times(expected)).deleteFromFileStorage(eq(fileStorage), any(Location.class), any(BackupMetadata.class));
    }

    @Test
    public void testLastCountRetentionPolicy() throws IOException {
        // We expect the retain all but the last 2 of each service
        final int expected = storage.listAll().size() - (2 * storage.listAllRows().size());

        final LastCountRetentionPolicy<BackupMetadata> retentionPolicy = new LastCountRetentionPolicy<>(2);
        final RetentionPolicyProcessor processor = new RetentionPolicyProcessor(
                fileStorage,
                Location.OFFSITE,
                backupProcessor,
                null,
                retentionPolicy,
                EnumSet.of(BackupMetadata.State.FINISHED),
                LOCAL_NODE,
                new MetricRegistry()
        );
        processor.execute();

        verify(backupProcessor, times(expected)).deleteFromFileStorage(eq(fileStorage), any(Location.class), any(BackupMetadata.class));
    }

    @Test
    public void testLastDurationRetentionPolicy() throws IOException {
        // We expect to retain all but the last 3 days worth of each service
        final int expected = storage.listAll().size() - (3 * storage.listAllRows().size());

        final LastDurationRetentionPolicy<BackupMetadata> retentionPolicy = new LastDurationRetentionPolicy<>(Duration.days(3));
        final RetentionPolicyProcessor processor = new RetentionPolicyProcessor(
                fileStorage,
                Location.OFFSITE,
                backupProcessor,
                null,
                retentionPolicy,
                EnumSet.of(BackupMetadata.State.FINISHED),
                LOCAL_NODE,
                new MetricRegistry()
        );
        processor.execute();

        verify(backupProcessor, times(expected)).deleteFromFileStorage(eq(fileStorage), any(Location.class), any(BackupMetadata.class));
    }

    @Test
    public void testCombinedRetentionPolicy() throws IOException {
        // We expect to delete all but 19 copies (16 for DWMY, +3 for last duration) of each service
        final int expected = storage.listAll().size() - (19 * storage.listAllRows().size());

        final CombinedRetentionPolicy<BackupMetadata> retentionPolicy = new CombinedRetentionPolicy<>(
                new LastCountRetentionPolicy<BackupMetadata>(2),
                new LastDurationRetentionPolicy<BackupMetadata>(Duration.days(10)),
                new DWMYRetentionPolicy(7, 4, 6, 0)
        );
        final RetentionPolicyProcessor processor = new RetentionPolicyProcessor(
                fileStorage,
                Location.OFFSITE,
                backupProcessor,
                null,
                retentionPolicy,
                EnumSet.of(BackupMetadata.State.FINISHED),
                LOCAL_NODE,
                new MetricRegistry()
        );
        processor.execute();

        verify(backupProcessor, times(expected)).deleteFromFileStorage(eq(fileStorage), any(Location.class), any(BackupMetadata.class));
    }

    @Test
    public void testRepeatingDWMYRetentionPolicy() {
        final DWMYRetentionPolicy retentionPolicy = new DWMYRetentionPolicy(7, 4, 6, 0);
        final RetentionPolicyProcessor processor = new RetentionPolicyProcessor(
                fileStorage,
                Location.OFFSITE,
                backupProcessor,
                null,
                retentionPolicy,
                EnumSet.of(BackupMetadata.State.FINISHED),
                LOCAL_NODE,
                new MetricRegistry()
        );

        storage.clear();

        for (int i = 0;i < 365;i++) {
            DateTimeUtils.setCurrentMillisOffset(Duration.days(i).toMilliseconds());

            // Add todays backup
            storage.put(createBackup("feedie"));

            // Run the retention policy
            processor.execute();
        }

        assertEquals(retentionPolicy.getExpectedRetentionCount(), storage.listAll().size());
    }
}

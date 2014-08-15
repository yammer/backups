package com.yammer.backups.processor;

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

import com.yammer.backups.MockDistributedLockManager;
import com.yammer.backups.api.metadata.BackupMetadata;
import com.yammer.backups.api.metadata.VerificationMetadata;
import com.yammer.backups.error.MetadataNotFoundException;
import com.yammer.backups.lock.DistributedLockManager;
import com.yammer.backups.storage.metadata.MetadataStorage;
import com.yammer.backups.storage.metadata.memory.InMemoryMetadataStorage;
import com.yammer.storage.file.FileStorage;
import com.yammer.storage.file.local.LocalFileStorage;
import com.yammer.storage.file.local.LocalFileStorageConfiguration;
import io.dropwizard.util.Duration;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public abstract class VerificationProcessorTest<T extends MetadataStorage<VerificationMetadata>> {

    private static final String IP = "127.0.0.1";

    @Rule
    public final TemporaryFolder localLogFolder = new TemporaryFolder();

    private String namespace;
    private T metadataStorage;
    private BackupProcessor backupProcessor;
    private VerificationProcessor processor;

    protected abstract T getMetadataStorage();
    protected abstract void clearMetadataStorage(T storage);

    @Before
    public void setUp() throws Exception {
        namespace = UUID.randomUUID().toString();
        metadataStorage = spy(getMetadataStorage());
        final FileStorage logStorage = spy(new LocalFileStorage(new LocalFileStorageConfiguration(localLogFolder.getRoot())));

        final DistributedLockManager distributedLockManager = new MockDistributedLockManager(Duration.seconds(10));

        backupProcessor = mock(BackupProcessor.class);

        final MetadataStorage<BackupMetadata> backupStorage = new InMemoryMetadataStorage<>();
        when(backupProcessor.create(anyString(), anyString())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final String service = (String) invocation.getArguments()[0];
                final String id = (String) invocation.getArguments()[1];

                final BackupMetadata backup = new BackupMetadata(service, id, "localhost");
                backupStorage.put(backup);
                return backup;
            }
        });

        when(backupProcessor.get(anyString(), anyString())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final String service = (String) invocation.getArguments()[0];
                final String id = (String) invocation.getArguments()[1];

                return backupStorage.get(service, id);
            }
        });

        processor = new VerificationProcessor(distributedLockManager, metadataStorage, logStorage, "localhost", backupProcessor);
        processor.start();
    }

    @After
    public void tearDown() throws Exception {
        try {
            processor.stop();
        }
        finally {
            this.clearMetadataStorage(metadataStorage);
        }
    }

    @Test
    public void testCreateStartedVerification() throws MetadataNotFoundException {
        final BackupMetadata backup = backupProcessor.create(namespace, IP);
        final VerificationMetadata verification = processor.create(namespace, backup.getId(), IP);

        assertEquals(verification.getState(),VerificationMetadata.State.STARTED);
        verify(metadataStorage,times(1)).put(eq(verification));
    }

    @Test
    public void testDeleteVerification() throws IOException {
        final BackupMetadata backup = backupProcessor.create(namespace, IP);
        final VerificationMetadata verification = processor.create(namespace, backup.getId(), IP);

        processor.delete(verification);
        verify(metadataStorage,times(1)).delete(eq(verification));
    }

    @Test
    public void testGetLatestVerification() throws MetadataNotFoundException{
        DateTimeUtils.setCurrentMillisOffset(-Duration.days(1).toMilliseconds());

        processor.create(namespace, backupProcessor.create(namespace, IP).getId(), IP);

        DateTimeUtils.setCurrentMillisOffset(0);


        final VerificationMetadata verification = processor.create(namespace, backupProcessor.create(namespace, IP).getId(), IP);
        verification.setState(VerificationMetadata.State.FINISHED, "Finished");
        metadataStorage.update(verification);

        assertEquals(2,processor.listMetadata().size());

        final VerificationMetadata result = metadataStorage.get(verification.getService(),verification.getId()).get();
        assertEquals(verification,result);
    }

    @Test
    public void testFinishStartedVerification() throws MetadataNotFoundException {
        final BackupMetadata backup = backupProcessor.create(namespace, IP);
        final VerificationMetadata verification = processor.create(namespace, backup.getId(), IP);

        verification.setState(VerificationMetadata.State.STARTED, "Started...");
        processor.finish(verification, "Success", true);

        final VerificationMetadata result = metadataStorage.get(verification.getService(),verification.getId()).get();
        assertEquals(result.getState(),VerificationMetadata.State.FINISHED);
    }

    @Test(expected = IllegalStateException.class)
    public void testFinishAlreadyFinishedVerification() throws MetadataNotFoundException {
        final BackupMetadata backup = backupProcessor.create(namespace, IP);
        final VerificationMetadata verification = processor.create(namespace, backup.getId(), IP);

        verification.setState(VerificationMetadata.State.FINISHED, "Finished...");
        metadataStorage.update(verification);

        processor.finish(verification, "Success", true);
    }

    @Test(expected = IllegalStateException.class)
    public void testFinishFailedVerification() throws MetadataNotFoundException {
        final BackupMetadata backup = backupProcessor.create(namespace, IP);
        final VerificationMetadata verification = processor.create(namespace, backup.getId(), IP);

        verification.setState(VerificationMetadata.State.FAILED , "Failed...");

        metadataStorage.update(verification);
        processor.finish(verification, "Success", true);
    }
}

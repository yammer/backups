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

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.hash.Hashing;
import com.google.common.io.CharStreams;
import com.google.common.util.concurrent.MoreExecutors;
import com.yammer.backups.MockDistributedLockManager;
import com.yammer.backups.api.Chunk;
import com.yammer.backups.api.CompressionCodec;
import com.yammer.backups.api.Location;
import com.yammer.backups.api.metadata.BackupMetadata;
import com.yammer.backups.codec.CodecFactory;
import com.yammer.backups.config.BackupConfiguration;
import com.yammer.backups.error.InvalidMD5Exception;
import com.yammer.backups.error.NoContentException;
import com.yammer.backups.lock.DistributedLockManager;
import com.yammer.backups.storage.metadata.MetadataStorage;
import com.yammer.io.codec.noop.NullStreamCodec;
import com.yammer.storage.file.FileStorage;
import com.yammer.storage.file.local.LocalFileStorage;
import com.yammer.storage.file.local.LocalFileStorageConfiguration;
import io.dropwizard.util.Duration;
import io.dropwizard.util.Size;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public abstract class BackupProcessorTest<K extends MetadataStorage<BackupMetadata>, V extends FileStorage> {

    private static final byte[] BYTES = "Now this is the story all about how, My life got flipped, turned upside down, And I'd like to take a minute just sit right there, I'll tell you how I became the prince of a town called Bel-air.".getBytes();
    private static final Random RANDOM = new Random();

    @Rule
    public final TemporaryFolder localTestFolder = new TemporaryFolder();

    @Rule
    public final TemporaryFolder localLogFolder = new TemporaryFolder();

    private String namespace;
    private K metadataStorage;
    private FileStorage localStorage;
    private V offsiteStorage;
    private BackupProcessor processor;
    private BackupProcessorListener listener;

    protected abstract K getMetadataStorage() throws Exception;
    protected abstract void clearMetadataStorage(K storage);

    protected abstract V getOffsiteStorage() throws Exception;

    @Before
    public void setUp() throws Exception {
        namespace = UUID.randomUUID().toString();
        metadataStorage = spy(getMetadataStorage());
        localStorage = spy(new LocalFileStorage(new LocalFileStorageConfiguration(localTestFolder.getRoot())));
        offsiteStorage = spy(getOffsiteStorage());
        final FileStorage logStorage = spy(new LocalFileStorage(new LocalFileStorageConfiguration(localLogFolder.getRoot())));
        final ExecutorService workerExecutor = MoreExecutors.sameThreadExecutor();
        listener = mock(BackupProcessorListener.class);

        final DistributedLockManager distributedLockManager = new MockDistributedLockManager(Duration.seconds(10));

        final CodecFactory codecFactory = mock(CodecFactory.class);
        when(codecFactory.get(any(CompressionCodec.class), anyBoolean())).thenReturn(new NullStreamCodec());

        processor = new BackupProcessor(distributedLockManager, metadataStorage, localStorage, offsiteStorage, codecFactory,
            workerExecutor, BackupConfiguration.DEFAULT_CHUNK_SIZE, logStorage, "localhost", ImmutableSet.of(".gz"), new MetricRegistry(), ImmutableList.of(listener));
        processor.start();
    }

    @After
    public void tearDown() throws IOException {
        try {
            processor.stop();
        }
        finally {
            this.clearMetadataStorage(metadataStorage);
            localStorage.delete(namespace);
            offsiteStorage.delete(namespace);
        }
    }

    @Test
    public void testAppendLog() throws IOException {
        final BackupMetadata backup = processor.create(namespace, "127.0.0.1");
        processor.appendLog(backup, "test1");
        processor.appendLog(backup, "test2");

        try (final Reader reader = new InputStreamReader(processor.getLog(backup))) {
            final String log = CharStreams.toString(reader);
            assertEquals("test1\ntest2\n", log);
        }
    }

    @Test
    public void testAppendNullLog() throws IOException {
        final BackupMetadata backup = processor.create(namespace, "127.0.0.1");
        processor.appendLog(backup, null);

        try (final Reader logReader = new InputStreamReader(processor.getLog(backup))) {
            assertEquals("", CharStreams.toString(logReader));
        }
    }

    @Test
    public void testCreatesStartedBackup() {
        final BackupMetadata backup = processor.create(namespace, "127.0.0.1");
        assertEquals(BackupMetadata.State.WAITING, backup.getState());

        verify(metadataStorage, times(1)).put(eq(backup));
        verify(listener, times(1)).backupCreated(eq(backup));
    }

    @Test(expected = NoContentException.class)
    public void testStoreEmptyBackup() throws IOException {
        final BackupMetadata backup = processor.create(namespace, "127.0.0.1");

        try (final InputStream in = new ByteArrayInputStream(new byte[0])) {
            processor.store(backup, Optional.<String>absent(), in, "testfile");
        }
    }

    @Test(expected = InvalidMD5Exception.class)
    public void testStoreWithInvalidContentMD5() throws IOException {
        final BackupMetadata backup = processor.create(namespace, "127.0.0.1");
        final String contentMD5 = "invalid";

        try (final InputStream in = new ByteArrayInputStream(BYTES)) {
            processor.store(backup, Optional.of(contentMD5), in, "testfile");
        }

        verify(localStorage, never()).upload(eq(backup.getService()), anyString());
    }

    @Test
    public void testStoreWithContentMD5() throws IOException {
        final BackupMetadata backup = processor.create(namespace, "127.0.0.1");
        final String contentMD5 = Hashing.md5().hashBytes(BYTES).toString();

        try (final InputStream in = new ByteArrayInputStream(BYTES)) {
            processor.store(backup, Optional.of(contentMD5), in, "testfile");
        }

        // Re-fetch the backup since we aren't necessarily using an inmemory reference
        final BackupMetadata backupResult = metadataStorage.get(backup.getService(),backup.getId()).get();
        assertFalse(backupResult.getChunks().isEmpty());
        for (Chunk chunk : backupResult.getChunks()) {
            verify(localStorage, times(1)).upload(eq(backupResult.getService()), eq(chunk.getPath()));
        }
        verify(listener, times(1)).backupUploaded(eq(backup), eq("testfile"));
    }

    @Test
    public void testStoreBackupInLocalStorage() throws IOException {
        final BackupMetadata backup = processor.create(namespace, "127.0.0.1");

        try (final InputStream in = new ByteArrayInputStream(BYTES)) {
            processor.store(backup, Optional.<String>absent(), in, "testfile");
        }

        // Re-fetch the backup since we aren't necessarily using an inmemory reference
        final BackupMetadata backupResult = metadataStorage.get(backup.getService(),backup.getId()).get();
        assertFalse(backupResult.getChunks().isEmpty());
        for (Chunk chunk : backupResult.getChunks()) {
            verify(localStorage, times(1)).upload(eq(backupResult.getService()), eq(chunk.getPath()));
        }
    }

    @Test
    public void testStoreMultipleFiles() throws IOException {
        final BackupMetadata backup = processor.create(namespace, "127.0.0.1");

        try (final InputStream in = new ByteArrayInputStream("test file 1".getBytes())) {
            processor.store(backup, Optional.<String>absent(), in, "testfile1");
        }

        try (final InputStream in = new ByteArrayInputStream("test file 2".getBytes())) {
            processor.store(backup, Optional.<String>absent(), in, "testfile2");
        }

        // Re-fetch the backup since we aren't necessarily using an inmemory reference
        final BackupMetadata backupResult = metadataStorage.get(backup.getService(),backup.getId()).get();
        assertFalse(backupResult.getChunks("testfile1").isEmpty());
        assertFalse(backupResult.getChunks("testfile2").isEmpty());
        for (Chunk chunk: backupResult.getChunks()) {
            verify(localStorage, times(1)).upload(eq(backupResult.getService()), eq(chunk.getPath()));
        }
    }

    @Test // This uploads 1MB, but never calls finish so they never make it as far as Azure
    public void testStoreMultipleSimultaneousFiles() throws IOException, InterruptedException {
        final BackupMetadata backup = processor.create(namespace, "127.0.0.1");
        assertEquals(BackupMetadata.State.WAITING, backup.getState());

        final int threads = 10;
        final ExecutorService executor = Executors.newFixedThreadPool(threads);
        final CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0;i < threads;i++) {
            final String filename = String.format("testfile-%d", i);

            executor.submit(new Runnable() {
                @Override
                public void run() {
                    final byte[] bytes = new byte[(int) Size.kilobytes(100).toBytes()];
                    RANDOM.nextBytes(bytes);

                    try (final InputStream in = new ByteArrayInputStream(bytes)) {
                        processor.store(backup, Optional.<String>absent(), in, filename);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        throw Throwables.propagate(e);
                    }
                    finally {
                        latch.countDown();
                    }
                }
            });
        }

        // Wait for all threads to complete
        latch.await();

        // Re-fetch the backup since we aren't necessarily using an inmemory reference
        final BackupMetadata backupResult = metadataStorage.get(backup.getService(), backup.getId()).get();
        assertEquals(BackupMetadata.State.WAITING, backupResult.getState());

        assertFalse(backupResult.getChunks().isEmpty());
        for (Chunk chunk : backupResult.getChunks()) {
            verify(localStorage, times(1)).upload(eq(backupResult.getService()), eq(chunk.getPath()));
        }
    }

    @Test
    public void testExceptioningStoreMarksBackupFailed() throws IOException {
        final BackupMetadata backup = processor.create(namespace, "127.0.0.1");

        doThrow(new IOException("test")).when(localStorage).upload(eq(backup.getService()), anyString());

        try (final InputStream in = new ByteArrayInputStream(BYTES)) {
            processor.store(backup, Optional.<String>absent(), in, "testfile");
        }
        catch (IOException e) {
            /* ignored */
        }

        final BackupMetadata backupResult = metadataStorage.get(backup.getService(),backup.getId()).get();
        assertEquals(BackupMetadata.State.FAILED, backupResult.getState());
    }

    @Test
    public void testListBackups() throws IOException {
        final BackupMetadata feedie = processor.create("feedie", "127.0.0.1");
        final BackupMetadata hermes = processor.create("hermes", "127.0.0.1");

        try {
            assertEquals(2, processor.listMetadata().size());

            assertEquals(1, processor.listMetadata("feedie").size());
            assertEquals(1, processor.listMetadata("hermes").size());
        }
        finally {
            processor.delete(feedie);
            processor.delete(hermes);
        }
    }

    @Test
    public void testListFilteredBackups() throws IOException {
        final BackupMetadata feedie = processor.create("feedie", "127.0.0.1");
        feedie.setState(BackupMetadata.State.FAILED, "test");
        metadataStorage.update(feedie);

        final BackupMetadata hermes = processor.create("hermes", "127.0.0.1");
        hermes.setState(BackupMetadata.State.FINISHED, "test");
        metadataStorage.update(hermes);

        try {
            assertEquals(1, processor.listMetadata(Optional.of(BackupMetadata.State.FAILED)).size());

            assertEquals(1, processor.listMetadata("feedie", Optional.of(BackupMetadata.State.FAILED)).size());
            assertEquals(0, processor.listMetadata("hermes", Optional.of(BackupMetadata.State.FAILED)).size());
        }
        finally {
            processor.delete(feedie);
            processor.delete(hermes);
        }
    }

    @Test
    public void testListServices() throws IOException {
        final BackupMetadata feedie = processor.create("feedie", "127.0.0.1");
        final BackupMetadata hermes = processor.create("hermes", "127.0.0.1");

        try {
            final Set<String> services = processor.listServices();
            assertEquals(2, services.size());

            assertTrue(services.contains("feedie"));
            assertTrue(services.contains("hermes"));
        }
        finally {
            processor.delete(feedie);
            processor.delete(hermes);
        }
    }

    @Test
    public void testFinishStartedBackup() throws IOException {
        final BackupMetadata backup = processor.create(namespace, "127.0.0.1");
        backup.setState(BackupMetadata.State.WAITING, "test");

        processor.finish(backup, "log...", true);

        final BackupMetadata backupResult = metadataStorage.get(backup.getService(),backup.getId()).get();
        assertEquals(BackupMetadata.State.FINISHED, backupResult.getState());

        verify(listener, times(1)).backupFinished(eq(backup), eq(true));
    }

    @Test(expected = IllegalStateException.class)
    public void testFinishAlreadyFailedBackup() throws IOException {
        final BackupMetadata backup = processor.create(namespace, "127.0.0.1");
        backup.setState(BackupMetadata.State.FAILED, "test");
        metadataStorage.update(backup);

        processor.finish(backup, "log...", true);
    }

    @Test
    public void testFinishFailedBackupWithoutFile() throws IOException {
        final BackupMetadata backup = processor.create(namespace, "127.0.0.1");
        backup.setState(BackupMetadata.State.WAITING, "test");
        metadataStorage.update(backup);

        processor.finish(backup, "log...", false);

        final BackupMetadata backupResult = metadataStorage.get(backup.getService(),backup.getId()).get();
        assertEquals(BackupMetadata.State.FAILED, backupResult.getState());

        // Ensure the failed file was deleted (if it existed)
        assertFalse(localStorage.exists(backupResult.getService(), backupResult.getId()));

        verify(listener, times(1)).backupFinished(eq(backup), eq(false));
    }

    @Test
    public void testFinishFailedBackup() throws IOException {
        final BackupMetadata backup = processor.create(namespace, "127.0.0.1");

        try (final InputStream in = new ByteArrayInputStream(BYTES)) {
            processor.store(backup, Optional.<String>absent(), in, "testfile");
        }

        processor.finish(backup, "log...", false);

        final BackupMetadata backupResult = metadataStorage.get(backup.getService(),backup.getId()).get();
        assertEquals(BackupMetadata.State.FAILED, backupResult.getState());

        // Ensure the failed file was deleted (if it existed)
        assertFalse(localStorage.exists(backupResult.getService(), backupResult.getId()));
    }

    @Test
    public void testFinishSuccessfulBackupCopiesOffsite() throws IOException {
        BackupMetadata backup = processor.create(namespace, "127.0.0.1");

        try (final InputStream in = new ByteArrayInputStream(BYTES)) {
            processor.store(backup, Optional.<String>absent(), in, "testfile");
        }

        backup = metadataStorage.get(backup.getService(), backup.getId()).get();
        processor.finish(backup, "log...", true);

        backup = metadataStorage.get(backup.getService(), backup.getId()).get();
        assertEquals(BackupMetadata.State.FINISHED, backup.getState());

        assertFalse(backup.getChunks().isEmpty());
        for (Chunk chunk : backup.getChunks()) {
            verify(offsiteStorage, times(1)).upload(eq(backup.getService()), eq(chunk.getPath()));
        }
    }

    @Test
    public void testExceptioningUploadMarksBackupFailed() throws Exception {
        BackupMetadata backup = processor.create(namespace, "127.0.0.1");

        doThrow(new IOException("test")).when(localStorage).download(eq(backup.getService()), anyString());

        try {
            try (final InputStream in = new ByteArrayInputStream(BYTES)) {
                processor.store(backup, Optional.<String>absent(), in, "testfile");
            }

            backup = metadataStorage.get(backup.getService(),backup.getId()).get();
            processor.finish(backup, "hello world", true);
        }
        catch (IllegalStateException e) {
            /* ignored */
        }

        backup = metadataStorage.get(backup.getService(),backup.getId()).get();
        assertEquals(BackupMetadata.State.FAILED, backup.getState());
    }

    @Test(expected = IllegalStateException.class)
    public void testDownloadPendingBackup() throws IOException {
        final BackupMetadata backup = processor.create(namespace, "127.0.0.1");

        try (final OutputStream out = new ByteArrayOutputStream()) {
            processor.download(backup, "testfile", out);
        }
    }

    @Test
    public void testDownloadFinishedBackupFromLocal() throws IOException {
        final BackupMetadata backup = processor.create(namespace, "127.0.0.1");

        try (final InputStream in = new ByteArrayInputStream(BYTES)) {
            processor.store(backup, Optional.<String>absent(), in, "testfile");
        }

        processor.finish(backup, "log...", true);

        final BackupMetadata backupResult = metadataStorage.get(backup.getService(),backup.getId()).get();
        assertEquals(BackupMetadata.State.FINISHED, backupResult.getState());

        reset(localStorage, offsiteStorage);

        try (final OutputStream out = new ByteArrayOutputStream()) {
            processor.download(backupResult, "testfile", out);
        }

        assertFalse(backupResult.getChunks().isEmpty());
        for (Chunk chunk : backupResult.getChunks()) {
            final String path = chunk.getPath();
            verify(localStorage, times(1)).download(eq(backup.getService()), eq(path));
            verify(offsiteStorage, times(0)).download(eq(backup.getService()), eq(path));
        }
    }

    @Test
    public void testDownloadFinishedBackupFromRemote() throws IOException {
        BackupMetadata backup = processor.create(namespace, "127.0.0.1");

        try (final InputStream in = new ByteArrayInputStream(BYTES)) {
            processor.store(backup, Optional.<String>absent(), in, "testfile");
        }

        backup = metadataStorage.get(backup.getService(), backup.getId()).get();
        processor.finish(backup, "log...", true);

        backup = metadataStorage.get(backup.getService(), backup.getId()).get();
        assertEquals(BackupMetadata.State.FINISHED, backup.getState());

        assertTrue(backup.existsAtLocation(Location.LOCAL));
        assertTrue(backup.existsAtLocation(Location.OFFSITE));

        for (Chunk chunk : backup.getChunks()) {
            final String path = chunk.getPath();
            assertTrue(localStorage.exists(backup.getService(), path));
            assertTrue(offsiteStorage.exists(backup.getService(), path));
        }

        // Delete the local copy so we have to fallback to the remote copy
        for (Chunk chunk : backup.getChunks()) {
            localStorage.delete(backup.getService(), chunk.getPath());
        }

        reset(localStorage, offsiteStorage);

        backup = metadataStorage.get(backup.getService(), backup.getId()).get();
        try (final OutputStream out = new ByteArrayOutputStream()) {
            processor.download(backup, "testfile", out);
        }

        assertFalse(backup.getChunks().isEmpty());
        for (Chunk chunk : backup.getChunks()) {
            final String path = chunk.getPath();
            verify(localStorage, times(0)).download(eq(backup.getService()), eq(path));
            verify(offsiteStorage, times(1)).download(eq(backup.getService()), eq(path));
        }
    }

    @Test(expected = FileNotFoundException.class)
    public void testDownloadMissingBackup() throws IOException {
        final BackupMetadata backup = processor.create(namespace, "127.0.0.1");

        try (final InputStream in = new ByteArrayInputStream(BYTES)) {
            processor.store(backup, Optional.<String>absent(), in, "testfile");
        }

        processor.finish(backup, "log...", true);

        final BackupMetadata backupResult = metadataStorage.get(backup.getService(),backup.getId()).get();
        assertEquals(BackupMetadata.State.FINISHED, backupResult.getState());

        // Delete the backups
        for (Chunk chunk : backupResult.getChunks()) {
            final String path = chunk.getPath();
            localStorage.delete(backup.getService(), path);
            offsiteStorage.delete(backup.getService(), path);
        }

        try (final OutputStream out = new ByteArrayOutputStream()) {
            processor.download(backupResult, "testfile", out);
        }
    }

    @Test
    public void testDeleteBackup() throws IOException {
        final BackupMetadata backup = processor.create(namespace, "127.0.0.1");
        backup.setState(BackupMetadata.State.RECEIVING, "test");
        backup.addChunk("test", "test-id-1", 100, 80, "hash", "localhost", CompressionCodec.SNAPPY);

        processor.delete(backup);
        verify(listener, times(1)).backupDeleted(eq(backup));
    }


}

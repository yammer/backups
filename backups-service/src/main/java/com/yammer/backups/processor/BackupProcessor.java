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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.google.common.hash.HashingOutputStream;
import com.google.common.io.ByteStreams;
import com.google.common.io.CountingOutputStream;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.yammer.backups.api.Chunk;
import com.yammer.backups.api.CompressionCodec;
import com.yammer.backups.api.Location;
import com.yammer.backups.api.metadata.BackupMetadata;
import com.yammer.backups.codec.CodecFactory;
import com.yammer.backups.error.InvalidMD5Exception;
import com.yammer.backups.error.MetadataNotFoundException;
import com.yammer.backups.error.NoContentException;
import com.yammer.backups.lock.DistributedLockManager;
import com.yammer.backups.storage.metadata.MetadataStorage;
import com.yammer.io.codec.StreamCodec;
import com.yammer.storage.file.FileStorage;
import io.dropwizard.util.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class BackupProcessor extends AbstractMetadataProcessor<BackupMetadata, BackupMetadata.State> {

    private static final int CHUNK_FILENAME_PART_RESOLUTION = 3;
    private static final String CHUNK_FILENAME_TEMPLATE = "%s-%s-part-%s";
    private static final Logger LOG = LoggerFactory.getLogger(BackupProcessor.class);

    private static String formatChunkFilename(String id, String filename, int chunk) {
        return String.format(CHUNK_FILENAME_TEMPLATE, id, filename, Strings.padStart(String.valueOf(chunk), CHUNK_FILENAME_PART_RESOLUTION, '0'));
    }

    private final Counter ACTIVE_STORES;
    private final Histogram STORE_SIZES;
    private final Timer STORE_TIMES;

    private final Counter ACTIVE_UPLOADS;
    private final Histogram UPLOAD_SIZES;
    private final Timer UPLOAD_TIMES;

    private final Counter ACTIVE_DOWNLOADS;
    private final Histogram DOWNLOAD_SIZES;
    private final Timer DOWNLOAD_TIMES;

    private final FileStorage localStorage;
    private final FileStorage offsiteStorage;
    private final CodecFactory codecFactory;
    private final ListeningExecutorService offsiteUploadWorkers;
    private final Size chunkSize;
    private final Set<String> compressedFileExtensions;
    private final List<BackupProcessorListener> listeners;

    public BackupProcessor(DistributedLockManager lockManager, MetadataStorage<BackupMetadata> metadataStorage, FileStorage localStorage, FileStorage offsiteStorage,
                           CodecFactory codecFactory, ExecutorService offsiteUploadWorkers, Size chunkSize,
                           FileStorage logStorage, String nodeName, Set<String> compressedFileExtensions, MetricRegistry metricRegistry,
                           List<BackupProcessorListener> listeners) {
        super (lockManager, metadataStorage, logStorage, nodeName);

        this.localStorage = localStorage;
        this.offsiteStorage = offsiteStorage;
        this.codecFactory = codecFactory;
        this.offsiteUploadWorkers = MoreExecutors.listeningDecorator(offsiteUploadWorkers);
        this.chunkSize = chunkSize;
        this.compressedFileExtensions = compressedFileExtensions;
        this.listeners = listeners;

        ACTIVE_STORES = metricRegistry.counter("active-stores");
        STORE_SIZES = metricRegistry.histogram("store-sizes");
        STORE_TIMES = metricRegistry.timer("store-times");

        ACTIVE_UPLOADS = metricRegistry.counter("active-uploads");
        UPLOAD_SIZES = metricRegistry.histogram("upload-sizes");
        UPLOAD_TIMES = metricRegistry.timer("upload-times");

        ACTIVE_DOWNLOADS = metricRegistry.counter("active-downloads");
        DOWNLOAD_SIZES = metricRegistry.histogram("download-sizes");
        DOWNLOAD_TIMES = metricRegistry.timer("download-times");
    }

    public long getActiveStoresCount() {
        return ACTIVE_STORES.getCount();
    }

    public long getActiveUploadsCount() {
        return ACTIVE_UPLOADS.getCount();
    }

    public long getActiveDownloadsCount() {
        return ACTIVE_DOWNLOADS.getCount();
    }

    public BackupMetadata create(String service, String remoteAddress) {
        final BackupMetadata backupMetadata = super.create(new BackupMetadata(service, remoteAddress, getNodeName()));
        fireBackupCreated(backupMetadata);
        return backupMetadata;
    }

    private void incrementPendingStores(BackupMetadata backup, final String filename) throws MetadataNotFoundException {
        this.update(backup, new Function<BackupMetadata, BackupMetadata>() {
            @Override
            public BackupMetadata apply(BackupMetadata input) {
                final int pendingStores = input.incrementAndGetPendingStores();
                final BackupMetadata.State expected = (pendingStores == 1) ? BackupMetadata.State.WAITING : BackupMetadata.State.RECEIVING;
                input.transitionState(expected, BackupMetadata.State.RECEIVING, String.format("Started receiving %s, pending stores = %d", filename, pendingStores));
                return input;
            }
        });
    }

    private void incrementPendingUploads(BackupMetadata backup, final String path) throws MetadataNotFoundException {
        this.update(backup, new Function<BackupMetadata, BackupMetadata>() {
            @Override
            public BackupMetadata apply(BackupMetadata input) {
                final int pendingUploads = input.incrementAndGetPendingUploads();

                // We don't actually want to change the state, just log a transition
                final BackupMetadata.State state = input.getState();
                input.transitionState(state, state, String.format("Starting uploading %s, pending uploads = %d", path, pendingUploads));

                return input;
            }
        });
    }

    private void decrementPendingStores(BackupMetadata backup, final String filename) throws MetadataNotFoundException {
        this.update(backup, new Function<BackupMetadata, BackupMetadata>() {
            @Override
            public BackupMetadata apply(BackupMetadata input) {
                final int pendingStores = input.decrementAndGetPendingStores();
                final BackupMetadata.State update = (pendingStores == 0) ? BackupMetadata.State.WAITING : BackupMetadata.State.RECEIVING;
                input.transitionState(BackupMetadata.State.RECEIVING, update, String.format("Finished receiving %s, pending stores = %d", filename, pendingStores));
                return input;
            }
        });
    }

    private void decrementPendingUploads(BackupMetadata backup, final String path) throws MetadataNotFoundException {
        this.update(backup, new Function<BackupMetadata, BackupMetadata>() {
            @Override
            public BackupMetadata apply(BackupMetadata input) {
                final int pendingUploads = input.decrementAndGetPendingUploads();

                // We don't actually want to change the state, just log a transition
                final BackupMetadata.State state = input.getState();
                input.transitionState(state, state, String.format("Finished uploading %s, pending uploads = %d", path, pendingUploads));

                // If this was the last pending upload and we were only uploading
                if (pendingUploads == 0 && BackupMetadata.State.UPLOADING.equals(state)) {
                    input.addLocation(Location.OFFSITE);
                    input.transitionState(BackupMetadata.State.UPLOADING, BackupMetadata.State.FINISHED, "Last pending upload finished and already marked as finished");
                }

                return input;
            }
        });
    }

    private boolean isCompressed(String filename) {
        for (String compressedFileExtension : compressedFileExtensions) {
            if (filename.toLowerCase().endsWith(compressedFileExtension)) {
                return true;
            }
        }

        return false;
    }

    public void store(BackupMetadata backup, Optional<String> contentMD5, InputStream in, String filename) throws IOException {
        final Timer.Context context = STORE_TIMES.time();

        try {
            ACTIVE_STORES.inc();
            incrementPendingStores(backup, filename);

            // If the file is already compressed then don't bother compressing it again
            final boolean originallyCompressed = isCompressed(filename);
            final CompressionCodec compressionCodec = originallyCompressed ? CompressionCodec.NONE : codecFactory.getDefaultCompressionCodec();

            LOG.trace("Compressing {} file {} using {}", backup, filename, compressionCodec);

            try (final HashingInputStream md5in = new HashingInputStream(Hashing.md5(), in)) {
                // Store all chunks
                final long size = this.storeChunks(backup, md5in, filename, compressionCodec);
                if (size == 0) {
                    throw new NoContentException(String.format("No content received for %s (service: %s, id: %s)", filename, backup.getService(), backup.getId()));
                }

                STORE_SIZES.update(size);

                final String calculatedMD5 = md5in.hash().toString();
                if (contentMD5.isPresent() && !calculatedMD5.equalsIgnoreCase(contentMD5.get())) {
                    throw new InvalidMD5Exception(contentMD5.get(), calculatedMD5);
                }
            }

            decrementPendingStores(backup, filename);
            fireBackupUploaded(backup, filename);
        }
        catch (final Exception e) {
            this.update(backup, new Function<BackupMetadata, BackupMetadata>() {
                @Override
                public BackupMetadata apply(BackupMetadata input) {
                    LOG.error("Failed to store backup: " + input, e);
                    input.setFailed("Failed to store backup: " + e.getMessage());
                    return input;
                }
            });

            throw e;
        }
        finally {
            ACTIVE_STORES.dec();
            context.stop();
        }
    }

    private long storeChunks(BackupMetadata backup, InputStream in, String filename, CompressionCodec compressionCodec) throws IOException {
        long size = 0;

        for (int i = 0; true; i++) {
            final String path = formatChunkFilename(backup.getId(), filename, i);
            final InputStream limitedIn = ByteStreams.limit(in, chunkSize.toBytes());

            final long resultSize = this.storeChunk(backup, limitedIn, path, filename, compressionCodec);
            if (resultSize <= 0) {
                LOG.debug("Attempted to store {} byte chunk ({} {} stream was finished)", resultSize, backup, filename);

                // If we wrote a 0 byte chunk, delete it
                if (localStorage.exists(backup.getService(), path)) {
                    localStorage.delete(backup.getService(), path);
                }

                break;
            }

            size += resultSize;
        }

        return size;
    }

    private long storeChunk(BackupMetadata backup, InputStream in, final String path, final String filename, final CompressionCodec compressionCodec) throws IOException {
        final CountingOutputStream countingOut = new CountingOutputStream(localStorage.upload(backup.getService(), path));

        final StreamCodec codec = codecFactory.get(compressionCodec, backup.getModelVersion() < 1);

        try (final OutputStream out = codec.output(countingOut)) {
            final HashingInputStream md5in = new HashingInputStream(Hashing.md5(), in);

            LOG.debug("Storing {} chunk {} locally", backup, path);

            final long originalSize = ByteStreams.copy(md5in, out);
            if (originalSize > 0) {
                final long size = countingOut.getCount();
                final String hash = md5in.hash().toString();

                this.update(backup, new Function<BackupMetadata, BackupMetadata>() {
                    @Override
                    public BackupMetadata apply(BackupMetadata input) {
                        input.addChunk(filename, path, originalSize, size, hash, getNodeName(), compressionCodec);
                        return input;
                    }
                });

                LOG.debug("Stored {} chunk {} (originalSize: {}, size: {}) locally", backup, path, originalSize, size);

                // Upload this chunk offsite in another thread
                this.uploadChunk(backup, path);
            }

            return originalSize;
        }
    }

    private void uploadChunk(final BackupMetadata backup, final String path) throws IOException {
        if (offsiteStorage.exists(backup.getService(), path)) {
            LOG.debug("{} chunk {} already exists offsite, skipping upload", backup, path);
            return;
        }

        incrementPendingUploads(backup, path);

        offsiteUploadWorkers.submit(new Runnable() {
            @Override
            public void run() {
                final Timer.Context context = UPLOAD_TIMES.time();

                try {
                    try (final InputStream in = localStorage.download(backup.getService(), path);
                         final OutputStream out = offsiteStorage.upload(backup.getService(), path)) {
                        ACTIVE_UPLOADS.inc();

                        LOG.debug("Uploading {} chunk {} offsite", backup, path);

                        final long size = ByteStreams.copy(in, out);
                        UPLOAD_SIZES.update(size);

                        LOG.debug("Uploaded {} chunk {} offsite", backup, path);
                    } catch (final Exception e) {
                        update(backup, new Function<BackupMetadata, BackupMetadata>() {
                            @Override
                            public BackupMetadata apply(BackupMetadata input) {
                                LOG.error("Failed copying chunk offsite", e);
                                input.setFailed("Failed copying chunk offsite: " + e.getMessage());
                                return input;
                            }
                        });

                        throw Throwables.propagate(e);
                    } finally {
                        decrementPendingUploads(backup, path);
                        ACTIVE_UPLOADS.dec();
                    }
                }
                catch (MetadataNotFoundException e) {
                    LOG.warn("Failed to find metadata for: " + backup, e);
                    throw Throwables.propagate(e);
                }
                finally {
                    context.stop();
                }
            }
        });
    }

    public void finish(final BackupMetadata backup, final String log, final boolean success) throws MetadataNotFoundException {
        // If this was a failure then mark it as so
        LOG.debug("Marking backup as finished: {}", backup);

        this.update(backup, new Function<BackupMetadata, BackupMetadata>() {
            @Override
            public BackupMetadata apply(BackupMetadata input) {
                input.addLocation(Location.LOCAL);
                appendLog(input, log);

                if (success) {
                    final int pendingUploads = input.getPendingUploads();
                    if (pendingUploads > 0) {
                        input.transitionState(BackupMetadata.State.WAITING, BackupMetadata.State.UPLOADING, "Backup finished but still uploading");
                    }
                    else {
                        input.addLocation(Location.OFFSITE);
                        input.transitionState(BackupMetadata.State.WAITING, BackupMetadata.State.FINISHED, "Marked as finished and no pending uploads left");
                    }
                } else {
                    input.setFailed("Marked as failed by client");
                }

                return input;
            }
        });
        fireBackupFinished(backup, success);
    }

    public void download(BackupMetadata backup, String filename, OutputStream out) throws IOException {
        if (!(backup.isSuccessful() || backup.isFailed())) {
            throw new IllegalStateException("Attempted to download a non-complete backup.");
        }

        final Timer.Context context = DOWNLOAD_TIMES.time();

        try {
            ACTIVE_DOWNLOADS.inc();

            for (Chunk chunk : backup.getChunks(filename)) {
                final CompressionCodec compressionCodec = chunk.getCompressionCodec();
                final StreamCodec codec = codecFactory.get(compressionCodec, backup.getModelVersion() < 1);

                try (final InputStream in = codec.input(this.openStreamFromStorage(backup.getService(), chunk.getPath()))) {
                    final HashingOutputStream md5out = new HashingOutputStream(Hashing.md5(), out);
                    final long size = ByteStreams.copy(in, md5out);

                    final String hash = md5out.hash().toString();
                    if (!hash.equalsIgnoreCase(chunk.getHash())) {
                        throw new InvalidMD5Exception(chunk.getHash(), hash);
                    }

                    DOWNLOAD_SIZES.update(size);
                }
            }
        }
        catch (IOException e) {
            LOG.error("Failed to download backup: " + backup, e);
            throw e;
        }
        finally {
            ACTIVE_DOWNLOADS.dec();
            context.stop();
        }
    }

    @Override
    public void delete(BackupMetadata backup) {
        try {
            this.deleteFromFileStorage(localStorage, Location.LOCAL, backup);
            LOG.debug("Deleted backup local: {}", backup);
        }
        catch (IOException e) {
            LOG.warn("Failed to delete backup from local: " + backup, e);
        }

        try {
            this.deleteFromFileStorage(offsiteStorage, Location.OFFSITE, backup);
            LOG.debug("Deleted backup offsite: {}", backup);
        }
        catch (IOException e) {
            LOG.warn("Failed to delete backup from offsite: " + backup, e);
        }
        fireBackupDeleted(backup);
    }

    public void deleteFromMetadataStorage(BackupMetadata backup) throws IOException {
        super.delete(backup);
    }

    public void deleteFromFileStorage(FileStorage storage, final Location location, BackupMetadata backup) throws IOException {
        backup = this.update(backup, new Function<BackupMetadata, BackupMetadata>() {
            @Override
            public BackupMetadata apply(BackupMetadata input) {
                input.removeLocation(location);
                return input;
            }
        });

        // If the backup exists here, delete it. Attempt this even if it isn't marked as existing here,
        // it's possible the backup failed, in which case it isn't marked as existing even though part
        // of it does.
        final String namespace = backup.getService();
        for (Chunk chunk : backup.getChunks()) {
            final String path = chunk.getPath();

            if (storage.exists(namespace, path)) {
                storage.delete(namespace, path);
                LOG.debug("Deleted {} backup chunk: {}", location, chunk);
            }
        }

        // If the backup doesn't exist anywhere, delete the metadata
        if (!backup.existsAtLocation()) {
            // Doesn't exist in any locations we remove the entire thing
            this.deleteFromMetadataStorage(backup);

            LOG.debug("Deleted backup metadata, files no longer stored: {}", backup);
        }
    }

    private InputStream openStreamFromStorage(String namespace, String path) throws IOException {
        // First check locally for the file
        if (localStorage.exists(namespace, path)) {
            return localStorage.download(namespace, path);
        }

        // If we don't have it locally, check offsite
        if (offsiteStorage.exists(namespace, path)) {
            return offsiteStorage.download(namespace, path);
        }

        throw new FileNotFoundException(String.format("%s/%s not found either locally or offsite", namespace, path));
    }

    private void fireBackupCreated(BackupMetadata backup) {
        for (BackupProcessorListener listener : listeners) {
            listener.backupCreated(backup);
        }
    }

    private void fireBackupUploaded(BackupMetadata backup, String filename) {
        for (BackupProcessorListener listener : listeners) {
            listener.backupUploaded(backup, filename);
        }
    }

    private void fireBackupFinished(BackupMetadata backup, boolean success) {
        for (BackupProcessorListener listener : listeners) {
            listener.backupFinished(backup, success);
        }
    }

    private void fireBackupDeleted(BackupMetadata backup) {
        for (BackupProcessorListener listener : listeners) {
            listener.backupDeleted(backup);
        }
    }
}

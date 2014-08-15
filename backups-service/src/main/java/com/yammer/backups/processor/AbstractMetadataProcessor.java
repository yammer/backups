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

import com.google.common.base.*;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.yammer.backups.api.metadata.AbstractMetadata;
import com.yammer.backups.error.MetadataNotFoundException;
import com.yammer.backups.lock.DistributedLock;
import com.yammer.backups.lock.DistributedLockManager;
import com.yammer.backups.storage.metadata.MetadataStorage;
import com.yammer.backups.util.MetadataStatePredicate;
import com.yammer.storage.file.FileStorage;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

public abstract class AbstractMetadataProcessor<T extends AbstractMetadata<S>, S> implements Managed {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractMetadataProcessor.class);
    private static final String LOG_FILENAME_TEMPLATE = "%s.log";

    private final DistributedLockManager lockManager;
    private final MetadataStorage<T> metadataStorage;
    private final FileStorage logStorage;
    private final String nodeName;

    public AbstractMetadataProcessor(DistributedLockManager lockManager, MetadataStorage<T> metadataStorage, FileStorage logStorage, String nodeName) {
        this.lockManager = lockManager;
        this.metadataStorage = metadataStorage;
        this.logStorage = logStorage;
        this.nodeName = nodeName;
    }

    public Optional<T> get(String service, String id) {
        return metadataStorage.get(service, id);
    }

    public InputStream getLog(T metadata) throws IOException {
        final String path = String.format(LOG_FILENAME_TEMPLATE, metadata.getId());
        if (logStorage.exists(metadata.getService(), path)) {
            return logStorage.download(metadata.getService(), path);
        }

        // We had no log, respond with an empty result
        return new ByteArrayInputStream(new byte[0]);
    }

    protected void appendLog(T metadata, String log) {
        if (Strings.isNullOrEmpty(log)) {
            return;
        }

        final String path = String.format(LOG_FILENAME_TEMPLATE, metadata.getId());

        try (final InputStream in = new ByteArrayInputStream(log.trim().concat("\n").getBytes(Charsets.UTF_8));
             final OutputStream out = logStorage.append(metadata.getService(), path)) {
            ByteStreams.copy(in, out);
        }
        catch (IOException e) {
            LOG.warn("Failed to append log", e);
        }
    }

    public String getNodeName() {
        return nodeName;
    }

    @Override
    public void start() throws MetadataNotFoundException {
        LOG.info("Started {}, with metadataStorage: {}", this.getClass().getSimpleName(), metadataStorage);

        final Set<T> runningMetadata = Sets.filter(listMetadata(), new Predicate<T>() {
            @Override
            public boolean apply(T input) {
                return input.isAtNode(nodeName) && input.isRunning();
            }
        });

        for (T metadata : runningMetadata) {
            this.processStalledMetadata(metadata);
        }
    }

    protected void processStalledMetadata(T metadata) throws MetadataNotFoundException {
        this.update(metadata, new Function<T, T>() {
            @Override
            public T apply(T input) {
                LOG.warn("{} was running when we started, marking as failed", input);
                input.setFailed("Service restarted while operation was in progress");
                appendLog(input, "----\nService restarted while operation was in progress :(\n----");
                return input;
            }
        });
    }

    @Override
    public void stop() {
        LOG.info("Stopped {}", this.getClass().getSimpleName());
    }

    public T create(T item) {
        metadataStorage.put(item);
        return item;
    }

    protected T update(T item, Function<T, T> function) throws MetadataNotFoundException {
        try (final DistributedLock lock = lockManager.lock(item.getId())) {
            lockManager.acquire(lock);

            final Optional<T> mutable = metadataStorage.get(item.getRowKey(), item.getColumnKey());
            if (!mutable.isPresent()) {
                throw new MetadataNotFoundException(item.getService(), item.getId());
            }

            final T result = function.apply(mutable.get());
            metadataStorage.update(result);
            return result;
        }
    }

    public Set<String> listServices() {
        return metadataStorage.listAllRows();
    }

    public Set<T> listMetadata() {
        return this.listMetadata(Optional.<S>absent());
    }

    public Set<T> listMetadata(Optional<S> state) {
        final Set<T> backups = metadataStorage.listAll();
        return this.filterState(backups, state);
    }

    public Set<T> listMetadata(String service) {
        return this.listMetadata(service, Optional.<S>absent());
    }

    public Set<T> listMetadata(String service, Optional<S> state) {
        final Set<T> backups = metadataStorage.listAll(service);
        return this.filterState(backups, state);
    }

    protected Set<T> filterState(Set<T> items, Optional<S> state) {
        // No state filter so return the lot
        if (!state.isPresent()) {
            return items;
        }

        // Filter on state
        return Sets.filter(items, new MetadataStatePredicate<T, S>(state.get()));
    }

    public void delete(T metadata) {
        metadataStorage.delete(metadata);
        LOG.debug("Deleted metadata: {}", metadata);

        // Purposely don't delete the logs in-case we want them later
    }
}

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

import com.codahale.metrics.health.HealthCheck;
import com.google.common.base.Predicates;
import com.google.common.collect.Sets;
import com.yammer.backups.api.Chunk;
import com.yammer.backups.api.Location;
import com.yammer.backups.api.metadata.BackupMetadata;
import com.yammer.backups.processor.ServiceRegistry;
import com.yammer.backups.storage.metadata.MetadataStorage;
import com.yammer.backups.util.MetadataStatePredicate;
import com.yammer.storage.file.FileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

public class MissingBackupsHealthCheck extends HealthCheck {

    private static final Logger LOG = LoggerFactory.getLogger(MissingBackupsHealthCheck.class);

    private final MetadataStorage<BackupMetadata> metadataStorage;
    private final FileStorage fileStorage;
    private final Location location;
    private final String nodeName;
    private final ServiceRegistry serviceRegistry;

    public MissingBackupsHealthCheck(
            MetadataStorage<BackupMetadata> metadataStorage,
            FileStorage fileStorage,
            Location location,
            String nodeName,
            ServiceRegistry serviceRegistry) {
        this.metadataStorage = metadataStorage;
        this.fileStorage = fileStorage;
        this.location = location;
        this.nodeName = nodeName;
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    protected Result check() throws IOException {
        final Set<BackupMetadata> missingBackups = Sets.newHashSet();

        // Only care about backups that we think are completed and in this location
        @SuppressWarnings("unchecked")
        final Collection<BackupMetadata> backups = Sets.filter(metadataStorage.listAll(), Predicates.and(
                new MetadataStatePredicate<BackupMetadata, BackupMetadata.State>(BackupMetadata.State.FINISHED),
                BackupMetadata.IN_LOCATION_PREDICATE(location),
                BackupMetadata.IN_NODE_PREDICATE(nodeName)
        ));

        for (BackupMetadata backup : backups) {
            if (!serviceRegistry.healthCheckDisabled(backup.getService())) {
                for (Chunk chunk : backup.getChunks()) {
                    final String path = chunk.getPath();
                    if (!fileStorage.exists(backup.getService(), path)) {
                        LOG.warn("Found missing backup {} file {}", backup, path);
                        missingBackups.add(backup);
                    } else {
                        LOG.trace("Found file {} for backup {}, skipping", path, backup);
                    }
                }
            }
        }

        if (!missingBackups.isEmpty()) {
            return Result.unhealthy("Missing backups: %s ", missingBackups);
        }

        return Result.healthy();
    }
}

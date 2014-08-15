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

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import com.yammer.backups.api.metadata.BackupMetadata;
import com.yammer.backups.storage.metadata.MetadataStorage;
import com.yammer.backups.util.MetadataStatePredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class FailedRetentionPolicy implements RetentionPolicy<BackupMetadata> {

    private static final Logger LOG = LoggerFactory.getLogger(FailedRetentionPolicy.class);
    private static final Predicate<BackupMetadata> COMPLETED_OR_FAILED_PREDICATE =
            new MetadataStatePredicate<>(
                    BackupMetadata.State.FINISHED, BackupMetadata.State.FAILED, BackupMetadata.State.TIMEDOUT);

    private final MetadataStorage<BackupMetadata> metadataStorage;

    public FailedRetentionPolicy(MetadataStorage<BackupMetadata> metadataStorage) {
        this.metadataStorage = metadataStorage;
    }

    @Override
    public Set<BackupMetadata> retain(Set<BackupMetadata> items) {
        return Sets.filter(items, new Predicate<BackupMetadata>() {
            @Override
            public boolean apply(BackupMetadata input) {
                final Set<BackupMetadata> backups = Sets.filter(metadataStorage.listAll(input.getService()), COMPLETED_OR_FAILED_PREDICATE);
                for (BackupMetadata backup : backups) {
                    // If this backup is after the failed one, we don't need to retain the failed one
                    if (backup.getStartedDate().isAfter(input.getStartedDate())) {
                        LOG.debug("Not retaining {} because {} is more recent", input, backup);
                        return false;
                    }
                }

                return true;
            }
        });
    }

    @Override
    public String toString() {
        return "FailedRetentionPolicy{}";
    }
}

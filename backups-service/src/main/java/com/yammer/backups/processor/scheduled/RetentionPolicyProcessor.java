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
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.yammer.backups.api.Location;
import com.yammer.backups.api.metadata.BackupMetadata;
import com.yammer.backups.policy.RetentionPolicy;
import com.yammer.backups.processor.BackupProcessor;
import com.yammer.backups.util.MetadataStatePredicate;
import com.yammer.storage.file.FileStorage;
import io.dropwizard.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

public class RetentionPolicyProcessor extends AbstractScheduledProcessor {

    private static final Duration FREQUENCY = Duration.hours(1);
    private static final Duration INITIAL_DELAY = Duration.minutes(5);
    private static final Logger LOG = LoggerFactory.getLogger(RetentionPolicyProcessor.class);

    private final FileStorage fileStorage;
    private final Location location;
    private final BackupProcessor backupProcessor;
    private final RetentionPolicy<BackupMetadata> retentionPolicy;
    private final EnumSet<BackupMetadata.State> states;
    private final String nodeName;

    public RetentionPolicyProcessor(
            FileStorage fileStorage,
            Location location,
            BackupProcessor backupProcessor,
            ScheduledExecutorService executor,
            RetentionPolicy<BackupMetadata> retentionPolicy,
            EnumSet<BackupMetadata.State> states,
            String nodeName,
            MetricRegistry metricRegistry) {
        super(executor, FREQUENCY, INITIAL_DELAY, "retention-policy-processor", metricRegistry);

        this.fileStorage = fileStorage;
        this.location = location;
        this.backupProcessor = backupProcessor;
        this.retentionPolicy = retentionPolicy;
        this.states = states;
        this.nodeName = nodeName;
    }

    private void applyRetentionPolicy(String service) {
        // We only care about completed backups that exist in the storage we are applying to
        final List<Predicate<BackupMetadata>> predicates = ImmutableList.of(
            BackupMetadata.IN_NODE_PREDICATE(nodeName),
            BackupMetadata.IN_LOCATION_PREDICATE(location),
            new MetadataStatePredicate<BackupMetadata, BackupMetadata.State>(states)
        );

        final Set<BackupMetadata> backups = Sets.filter(backupProcessor.listMetadata(service), Predicates.and(predicates));
        final Set<BackupMetadata> retained = retentionPolicy.retain(backups);
        final Set<BackupMetadata> extras = Sets.difference(backups, retained);

        if (!extras.isEmpty()) {
            LOG.debug("Applied retention policy {} to {} for {}. Retaining {}/{} backups, deleting {}.",
                    retentionPolicy, fileStorage, service, retained.size(), backups.size(), extras);
        }

        for (BackupMetadata backup : extras) {
            try {
                backupProcessor.deleteFromFileStorage(fileStorage, location, backup);
                LOG.trace("Deleted unretained backup from {}: {}", fileStorage, backup);
            }
            catch (IOException e) {
                LOG.warn("Failed to delete extra backup from " + fileStorage, e);
            }
        }
    }

    @Override
    public void execute() {
        final Collection<String> services = backupProcessor.listServices();
        for (String service : services) {
            this.applyRetentionPolicy(service);
        }
    }
}

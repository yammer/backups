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
import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.yammer.backups.api.metadata.BackupMetadata;
import com.yammer.backups.api.metadata.VerificationMetadata;
import com.yammer.backups.storage.metadata.MetadataStorage;
import io.dropwizard.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

public class OrphanedVerificationProcessor extends AbstractScheduledProcessor {

    private static final Duration FREQUENCY = Duration.hours(1);
    private static final Duration INITIAL_DELAY = Duration.minutes(5);
    private static final Logger LOG = LoggerFactory.getLogger(OrphanedVerificationProcessor.class);

    private final MetadataStorage<VerificationMetadata> verificationStorage;
    private final MetadataStorage<BackupMetadata> backupStorage;
    private final String nodeName;

    public OrphanedVerificationProcessor(ScheduledExecutorService executor,
                                         MetadataStorage<VerificationMetadata> verificationStorage,
                                         MetadataStorage<BackupMetadata> backupStorage,
                                         String nodeName,
                                         MetricRegistry metricRegistry) {
        super(executor, FREQUENCY, INITIAL_DELAY, "orphaned-verifications", metricRegistry);

        this.verificationStorage = verificationStorage;
        this.backupStorage = backupStorage;
        this.nodeName = nodeName;
    }

    private void handleOrphanedVerification(VerificationMetadata verification) {
        LOG.info("Deleting orphaned verification {}, no associated backup", verification);

        // Delete the verification
        verificationStorage.delete(verification);
    }

    @Override
    public void execute() {
        final Set<VerificationMetadata> verifications = Sets.filter(verificationStorage.listAll(),
                VerificationMetadata.IN_NODE_PREDICATE(nodeName));

        for (VerificationMetadata verification : verifications) {
            final String service = verification.getService();
            final String backupId = verification.getBackupId();

            final Optional<BackupMetadata> backup = backupStorage.get(service, backupId);
            if (backup.isPresent()) {
                LOG.trace("Keeping {}, it has an associated backup", verification);
            }
            else {
                this.handleOrphanedVerification(verification);
            }
        }
    }
}

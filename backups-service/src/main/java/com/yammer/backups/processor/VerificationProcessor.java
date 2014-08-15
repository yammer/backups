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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.yammer.backups.api.metadata.BackupMetadata;
import com.yammer.backups.api.metadata.VerificationMetadata;
import com.yammer.backups.error.MetadataNotFoundException;
import com.yammer.backups.lock.DistributedLockManager;
import com.yammer.backups.storage.metadata.MetadataStorage;
import com.yammer.storage.file.FileStorage;

public class VerificationProcessor extends AbstractMetadataProcessor<VerificationMetadata, VerificationMetadata.State> {

    private final BackupProcessor backupProcessor;

    public VerificationProcessor(DistributedLockManager lockManager, MetadataStorage<VerificationMetadata> metadataStorage,
                                 FileStorage logStorage, String nodeName, BackupProcessor backupProcessor) {
        super (lockManager, metadataStorage, logStorage, nodeName);

        this.backupProcessor = backupProcessor;
    }

    public VerificationMetadata create(String service, String backupId, String remoteAddress) throws MetadataNotFoundException {
        // Fetch the backup we're trying to verify
        final Optional<BackupMetadata> backup = backupProcessor.get(service, backupId);
        if (!backup.isPresent()) {
            throw new MetadataNotFoundException(service, backupId);
        }

        final VerificationMetadata verification = super.create(new VerificationMetadata(service, backupId, remoteAddress, getNodeName()));

        // Update the backup to point to this verification
        backupProcessor.update(backup.get(), new Function<BackupMetadata, BackupMetadata>() {
            @Override
            public BackupMetadata apply(BackupMetadata input) {
                input.setVerificationId(verification.getId());
                return input;
            }
        });

        return verification;
    }

    public void finish(VerificationMetadata verification, final String log, final boolean success) throws MetadataNotFoundException {
        this.update(verification, new Function<VerificationMetadata, VerificationMetadata>() {
            @Override
            public VerificationMetadata apply(VerificationMetadata input) {
                appendLog(input, log);

                // If this was a failure then mark it as so
                if (success) {
                    input.transitionState(VerificationMetadata.State.STARTED, VerificationMetadata.State.FINISHED, "Marked as success by client");
                }
                else {
                    input.setFailed("Marked as failed by client");
                }

                return input;
            }
        });
    }
}

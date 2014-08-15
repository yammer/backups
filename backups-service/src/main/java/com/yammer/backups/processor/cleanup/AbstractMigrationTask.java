package com.yammer.backups.processor.cleanup;

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

import com.google.common.collect.Sets;
import com.yammer.backups.api.metadata.BackupMetadata;
import com.yammer.backups.processor.BackupProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public abstract class AbstractMigrationTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractMigrationTask.class);

    private final BackupProcessor processor;

    protected AbstractMigrationTask(BackupProcessor processor) {
        this.processor = processor;
    }

    protected abstract void migrate(BackupMetadata backup);

    @Override
    public void run() {
        final Set<BackupMetadata> backups = Sets.filter(processor.listMetadata(), BackupMetadata.IN_NODE_PREDICATE(processor.getNodeName()));
        for (BackupMetadata backup : backups) {
            try {
                migrate(backup);
            }
            catch (RuntimeException e) {
                LOG.warn("Failed to cleanup backup metadata: " + backup, e.getCause());
            }
        }
    }
}

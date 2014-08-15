package com.yammer.backups.policy.util;

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

import com.google.common.base.Optional;
import com.yammer.backups.api.metadata.BackupMetadata;
import org.joda.time.DateTime;

public class RetainedBackupSet extends RetainedSet<BackupMetadata> {

    public RetainedBackupSet(int size, DateTime now, int dayRange) {
        super(size, now, dayRange);
    }

    @Override
    protected void retain(BackupMetadata item, int bucket) {
        final Optional<BackupMetadata> existingItem = this.getBucket(bucket);

        // We've already retained an item for this period
        if (existingItem.isPresent()) {
            // This item isn't verified, or we already have one that is
            if (!item.hasVerification() || existingItem.get().hasVerification()) {
                return;
            }
        }

        this.setBucket(bucket, item);
    }
}

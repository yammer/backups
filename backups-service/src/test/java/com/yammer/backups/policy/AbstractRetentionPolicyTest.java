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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.yammer.backups.api.metadata.BackupMetadata;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.Before;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Set;

public class AbstractRetentionPolicyTest {

    protected static Set<DateTime> backupsToDateTimes(Collection<BackupMetadata> backups) {
        return ImmutableSet.copyOf(Collections2.transform(backups, BACKUP_TO_DATETIME));
    }

    private static final Function<BackupMetadata, DateTime> BACKUP_TO_DATETIME = new Function<BackupMetadata, DateTime>() {
        @Nullable
        @Override
        public DateTime apply(@Nullable BackupMetadata input) {
            if (input == null) {
                return null;
            }

            return input.getStartedDate();
        }
    };

    protected DateTime now;
    protected Set<BackupMetadata> items;

    protected BackupMetadata createBackup(DateTime date) {
        try {
            DateTimeUtils.setCurrentMillisFixed(date.getMillis());
            return new BackupMetadata("test", "test", "localhost");
        }
        finally {
            DateTimeUtils.setCurrentMillisSystem();
        }
    }

    @Before
    public void setUp() {
        now = DateTime.now();

        items = Sets.newHashSet();
        for (int i = 0;i < (365 * 2);i++) {
            items.add(this.createBackup(now.minusDays(i)));
        }
    }
}

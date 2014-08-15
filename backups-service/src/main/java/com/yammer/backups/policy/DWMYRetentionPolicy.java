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

import com.google.common.collect.ImmutableSet;
import com.yammer.backups.api.metadata.BackupMetadata;
import com.yammer.backups.policy.util.RetainedBackupSet;
import com.yammer.backups.policy.util.RetainedSet;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Set;

public class DWMYRetentionPolicy extends SortedRetentionPolicy<BackupMetadata> {

    private static final int DAYS_PER_DAY = 1;
    private static final int DAYS_PER_WEEK = 7;
    private static final int DAYS_PER_MONTH = 30;
    private static final int DAYS_PER_YEAR = 365;

    private final int daily;
    private final int weekly;
    private final int monthly;
    private final int yearly;

    public DWMYRetentionPolicy(int daily, int weekly, int monthly, int yearly) {
        // Always take the oldest entry
        super (false);

        this.daily = daily;
        this.weekly = weekly;
        this.monthly = monthly;
        this.yearly = yearly;
    }

    public int getExpectedRetentionCount() {
        int expected = daily + weekly + monthly + yearly;
        if (weekly > 0) {
            expected -= 1;
        }

        if (monthly > 0) {
            expected -= 1;
        }

        if (yearly > 0) {
            expected -= 1;
        }

        return expected;
    }

    @Override
    public Set<BackupMetadata> retain(List<BackupMetadata> items) {
        final DateTime now = DateTime.now();

        final ImmutableSet<RetainedBackupSet> retainers = ImmutableSet.of(
                new RetainedBackupSet(daily, now, DAYS_PER_DAY),
                new RetainedBackupSet(weekly, now, DAYS_PER_WEEK),
                new RetainedBackupSet(monthly, now, DAYS_PER_MONTH),
                new RetainedBackupSet(yearly, now, DAYS_PER_YEAR)
        );

        // We sort this first so that we always take the most recent backup in a time range
        for (BackupMetadata item : items) {
            for (RetainedBackupSet retainer : retainers) {
                retainer.retainIfRequired(item);
            }
        }

        final ImmutableSet.Builder<BackupMetadata> retained = ImmutableSet.builder();

        for (RetainedSet<BackupMetadata> retainer : retainers) {
            retained.addAll(retainer.get());
        }

        return retained.build();
    }

    @Override
    public String toString() {
        return "DWMYRetentionPolicy{" +
                "daily=" + daily +
                ", weekly=" + weekly +
                ", monthly=" + monthly +
                ", yearly=" + yearly +
                '}';
    }
}

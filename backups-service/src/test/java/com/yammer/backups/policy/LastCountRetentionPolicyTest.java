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

import com.google.common.collect.Sets;
import com.yammer.backups.api.metadata.BackupMetadata;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;

public class LastCountRetentionPolicyTest extends AbstractRetentionPolicyTest {

    protected RetentionPolicy<BackupMetadata> policy;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        policy = new LastCountRetentionPolicy<>(3);
    }

    @Test
    public void testCorrectNumberRetained() {
        final Set<BackupMetadata> retained = policy.retain(items);
        assertEquals(3, retained.size());
    }

    @Test
    public void testRetainedMostRecent() {
        final Set<DateTime> retained = backupsToDateTimes(policy.retain(items));

        final Set<DateTime> expected = Sets.newHashSetWithExpectedSize(3);
        for (int i = 0;i < 3;i++) {
            expected.add(now.minusDays(i));
        }

        assertEquals(expected, retained);
    }
}

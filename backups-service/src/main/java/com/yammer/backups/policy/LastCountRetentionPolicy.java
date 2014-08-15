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
import com.yammer.backups.api.metadata.AbstractMetadata;

import java.util.List;
import java.util.Set;

public class LastCountRetentionPolicy<T extends AbstractMetadata<?>> extends SortedRetentionPolicy<T> {

    private final int count;

    public LastCountRetentionPolicy(int count) {
        super (true);

        this.count = count;
    }

    @Override
    protected Set<T> retain(List<T> items) {
        final int toIndex = Math.min(count, items.size());
        return ImmutableSet.copyOf(items.subList(0, toIndex));
    }

    @Override
    public String toString() {
        return "LastCountRetentionPolicy{" +
                "count=" + count +
                '}';
    }
}

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

import com.google.common.collect.Lists;
import com.yammer.backups.api.metadata.AbstractMetadata;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public abstract class SortedRetentionPolicy<T extends AbstractMetadata<?>> implements RetentionPolicy<T> {

    private static final Comparator<AbstractMetadata<?>> COMPARATOR = AbstractMetadata.COMPARATOR;

    protected abstract Set<T> retain(List<T> items);

    private final boolean ascending;

    protected SortedRetentionPolicy(boolean ascending) {
        this.ascending = ascending;
    }

    private Comparator<AbstractMetadata<?>> getComparator() {
        if (ascending) {
            return COMPARATOR;
        }

        return Collections.reverseOrder(COMPARATOR);
    }

    @Override
    public Set<T> retain(Set<T> items) {
        final List<T> sorted = Lists.newArrayList(items);
        Collections.sort(sorted, this.getComparator());
        return this.retain(sorted);
    }
}

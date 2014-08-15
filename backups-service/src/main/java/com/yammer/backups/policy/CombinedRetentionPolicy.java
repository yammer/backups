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

import java.util.Arrays;
import java.util.Set;

public class CombinedRetentionPolicy<T extends AbstractMetadata<?>> implements RetentionPolicy<T> {

    private final RetentionPolicy<T>[] policies;

    @SafeVarargs
    public CombinedRetentionPolicy(RetentionPolicy<T> ... policies) {
        this.policies = policies;
    }

    @Override
    public Set<T> retain(Set<T> items) {
        final ImmutableSet.Builder<T> retained = ImmutableSet.builder();

        for (RetentionPolicy<T> policy : policies) {
            retained.addAll(policy.retain(items));
        }

        return retained.build();
    }

    @Override
    public String toString() {
        return "CombinedRetentionPolicy{" +
                "policies=" + Arrays.toString(policies) +
                '}';
    }
}

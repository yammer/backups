package com.yammer.backups.util;

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

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import com.yammer.backups.api.metadata.AbstractMetadata;

import javax.annotation.Nullable;
import java.util.Set;

public class MetadataStatePredicate<T extends AbstractMetadata<S>, S> implements Predicate<T> {

    private final Set<S> required;

    public MetadataStatePredicate(Set<S> required) {
        this.required = required;
    }

    @SafeVarargs
    public MetadataStatePredicate(S... required) {
        this.required = Sets.newHashSet(required);
    }

    @Override
    public boolean apply(@Nullable T input) {
        return input != null && required.contains(input.getState());
    }
}

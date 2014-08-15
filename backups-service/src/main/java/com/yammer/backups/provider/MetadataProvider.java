package com.yammer.backups.provider;

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

import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;
import com.yammer.backups.api.metadata.AbstractMetadata;
import com.yammer.backups.storage.metadata.MetadataStorage;

import java.lang.reflect.Type;

public abstract class MetadataProvider<T extends AbstractMetadata<?>> implements InjectableProvider<Metadata, Type> {

    private final Class<T> clazz;
    private final MetadataStorage<T> storage;

    public MetadataProvider(MetadataStorage<T> storage, Class<T> clazz) {
        this.storage = storage;
        this.clazz = clazz;
    }

    @Override
    public ComponentScope getScope() {
        return ComponentScope.PerRequest;
    }

    @Override
    public Injectable<T> getInjectable(ComponentContext ic, Metadata annotation, Type c) {
        if (c.equals(clazz)) {
            return new MetadataInjectable<>(storage, annotation.service(), annotation.id());
        }

        return null;
    }
}

package com.yammer.backups.storage.metadata.memory;

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

import com.google.common.collect.HashBasedTable;
import com.yammer.backups.api.Storable;
import com.yammer.backups.storage.metadata.TableMetadataStorage;

public class InMemoryMetadataStorage<T extends Storable> extends TableMetadataStorage<T> {

    public InMemoryMetadataStorage() {
        super (HashBasedTable.<String, String, T>create(), HashBasedTable.<String, String, T>create());
    }

    @Override
    public void update(T metadata) {
        // NO OP, this is in memory...
    }

    @Override
    public String toString() {
        return "InMemoryMetadataStorage{}";
    }
}

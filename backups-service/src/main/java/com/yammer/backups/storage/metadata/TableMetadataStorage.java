package com.yammer.backups.storage.metadata;

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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import com.yammer.backups.api.Storable;

import java.util.Set;

public abstract class TableMetadataStorage<T extends Storable> implements MetadataStorage<T> {

    private final Table<String, String, T> table;
    private final Table<String, String, T> deleted;

    public TableMetadataStorage(Table<String, String, T> table, Table<String, String, T> deleted) {
        this.table = table;
        this.deleted = deleted;
    }

    @Override
    public void put(T data) {
        table.put(data.getRowKey(),data.getColumnKey(), data);
    }

    @Override
    public synchronized void delete(T data) {
        final String rowKey = data.getRowKey();
        final String columnKey = data.getColumnKey();

        final T item = table.remove(rowKey, columnKey);

        // If we removed something, then add it to the deleted list
        if (item != null) {
            deleted.put(item.getRowKey(), item.getColumnKey(), item);
        }
    }

    @Override
    public Set<String> listAllRows() {
        return ImmutableSet.copyOf(table.rowKeySet());
    }

    @Override
    public Set<T> listAll() {
        return ImmutableSet.copyOf(table.values());
    }

    @Override
    public Set<T> listAll(String rowKey) {
        return ImmutableSet.copyOf(table.row(rowKey).values());
    }

    @Override
    public Optional<T> get(String rowKey, String columnKey) {
        return Optional.fromNullable(table.get(rowKey, columnKey));
    }

    @VisibleForTesting
    @Override
    public void clear() {
        // Visible only for testing, this doesn't move things in to the deleted table!
        table.clear();
    }
}

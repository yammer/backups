package com.yammer.backups.storage.metadata.azure;

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
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import com.yammer.backups.api.Storable;
import com.yammer.backups.storage.metadata.MetadataStorage;
import com.yammer.backups.util.CompressedJsonDeserializationFunction;
import com.yammer.backups.util.CompressedJsonSerializationFunction;
import com.yammer.collections.azure.Bytes;

import java.util.Set;

// This serializes and deserializes values as azure does to ensure we can't rely on references
public class AzureTablelikeMetadataStorage<T extends Storable> implements MetadataStorage<T> {

    private final Table<String, String, Bytes> table;
    private final Table<String, String, Bytes> deleted;
    private final Function<T, Bytes> encoder;
    private final Function<Bytes, T> decoder;

    public AzureTablelikeMetadataStorage(Class<T> type) {
        table = HashBasedTable.create();
        deleted = HashBasedTable.create();

        encoder = new CompressedJsonSerializationFunction<>();
        decoder = new CompressedJsonDeserializationFunction<>(type);
    }

    @Override
    public void update(T metadata) {
        this.put(metadata);
    }

    @Override
    public void put(T data) {
        table.put(data.getRowKey(), data.getColumnKey(), encoder.apply(data));
    }

    @Override
    public void delete(T data) {
        final Bytes itemBytes = table.remove(data.getRowKey(), data.getColumnKey());
        if (itemBytes != null) {
            final T item = decoder.apply(itemBytes);
            if (item != null) {
                deleted.put(item.getRowKey(), item.getColumnKey(), encoder.apply(item));
            }
        }
    }

    @Override
    public Set<String> listAllRows() {
        return ImmutableSet.copyOf(table.rowKeySet());
    }

    @Override
    public Set<T> listAll() {
        return ImmutableSet.copyOf(Collections2.transform(table.values(), decoder));
    }

    @Override
    public Set<T> listAll(String service) {
        return ImmutableSet.copyOf(Collections2.transform(table.row(service).values(), decoder));
    }

    @Override
    public Optional<T> get(String service, String id) {
        final Optional<Bytes> result = Optional.fromNullable(table.get(service, id));
        if (!result.isPresent()) {
            return Optional.absent();
        }

        return Optional.of(decoder.apply(result.get()));
    }

    @Override
    public void clear() {
        table.clear();
    }
}

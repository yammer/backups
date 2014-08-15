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

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Table;
import com.microsoft.windowsazure.services.core.storage.StorageException;
import com.yammer.backups.api.Storable;
import com.yammer.backups.storage.metadata.TableMetadataStorage;
import com.yammer.backups.util.CompressedJsonDeserializationFunction;
import com.yammer.backups.util.CompressedJsonSerializationFunction;
import com.yammer.backups.util.StringDeserializationFunction;
import com.yammer.backups.util.StringSerializationFunction;
import com.yammer.collections.azure.util.AzureTables;
import com.yammer.storage.file.azure.AzureAccountConfiguration;

import java.io.IOException;

public class AzureTableMetadataStorage<T extends Storable> extends TableMetadataStorage<T> {

    private static <T extends Storable> Table<String, String, T> createAzureTable(Class<T> type, AzureAccountConfiguration config, String table, MetricRegistry metricRegistry) throws IOException {
        try {
            return AzureTables.clientForAccount(config.getName(), config.getKey())
                    .tableWithName(table)
                    .createIfDoesNotExist()
                    .andAddMetrics(metricRegistry)
                    .buildUsingCustomSerialization(
                            StringSerializationFunction.INSTANCE, StringDeserializationFunction.INSTANCE,
                            StringSerializationFunction.INSTANCE, StringDeserializationFunction.INSTANCE,
                            new CompressedJsonSerializationFunction<T>(), new CompressedJsonDeserializationFunction<>(type)
                    );
        }
        catch (StorageException e) {
            throw new IOException(e);
        }
    }

    private final AzureAccountConfiguration config;

    public AzureTableMetadataStorage(Class<T> type, AzureAccountConfiguration config, String table, MetricRegistry metricRegistry) throws IOException {
        super (createAzureTable(type, config, table, metricRegistry), createAzureTable(type, config, String.format("%sdeleted", table), metricRegistry));

        this.config = config;
    }

    @Override
    public void update(T metadata) {
        super.put(metadata);
    }

    @Override
    public String toString() {
        return "AzureTableMetadataStorage{" +
                "account=" + config.getName() +
                '}';
    }
}

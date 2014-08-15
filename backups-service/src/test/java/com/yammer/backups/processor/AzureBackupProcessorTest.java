package com.yammer.backups.processor;

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
import com.microsoft.windowsazure.services.core.storage.StorageException;
import com.yammer.backups.ConfigurationTestUtil;
import com.yammer.backups.api.metadata.BackupMetadata;
import com.yammer.backups.storage.metadata.azure.AzureTableMetadataStorage;
import com.yammer.storage.file.azure.AzureFileStorage;
import com.yammer.storage.file.azure.AzureFileStorageConfiguration;
import io.dropwizard.configuration.ConfigurationException;
import org.junit.Before;
import org.junit.Ignore;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

@Ignore
public class AzureBackupProcessorTest extends BackupProcessorTest<AzureTableMetadataStorage<BackupMetadata>, AzureFileStorage> {

    private String azureNamespace;
    private AzureFileStorageConfiguration configuration;

    @Before
    public void setUp() throws Exception {
        azureNamespace = String.format("test%d", System.currentTimeMillis()); // Azure is picky about table names
        configuration = ConfigurationTestUtil.loadConfiguration().getOffsiteConfiguration().getStorageConfiguration();
        super.setUp();
    }

    @Override
    protected AzureTableMetadataStorage<BackupMetadata> getMetadataStorage() throws IOException, ConfigurationException {
        return new AzureTableMetadataStorage<>(BackupMetadata.class, configuration, azureNamespace, new MetricRegistry());
    }

    @Override
    protected void clearMetadataStorage(AzureTableMetadataStorage<BackupMetadata> storage) {
        storage.clear();
    }

    @Override
    protected AzureFileStorage getOffsiteStorage() throws StorageException, InvalidKeyException, URISyntaxException {
        return new AzureFileStorage(configuration, azureNamespace);
    }
}

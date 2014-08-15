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
import com.yammer.backups.ConfigurationTestUtil;
import com.yammer.backups.api.metadata.BackupMetadata;
import com.yammer.backups.config.BackupConfiguration;
import com.yammer.backups.storage.metadata.MetadataStorage;
import com.yammer.backups.storage.metadata.MetadataStorageTest;
import io.dropwizard.configuration.ConfigurationException;
import org.junit.Before;
import org.junit.Ignore;

import java.io.IOException;

@Ignore
public class AzureTableMetadataStorageTest extends MetadataStorageTest {

    private String azureNamespace;

    @Override
    @Before
    public void setUp() throws Exception {
        azureNamespace = String.format("test%d", System.currentTimeMillis()); // Azure is picky about table names
        super.setUp();
    }

    @Override
    protected MetadataStorage<BackupMetadata> getMetadataStorage() throws IOException, ConfigurationException {
        final BackupConfiguration configuration = ConfigurationTestUtil.loadConfiguration();
        return new AzureTableMetadataStorage<>(BackupMetadata.class, configuration.getOffsiteConfiguration().getStorageConfiguration(), azureNamespace, new MetricRegistry());
    }

    @Override
    protected void clearMetadataStorage(MetadataStorage<BackupMetadata> storage) {
        storage.clear();
    }
}

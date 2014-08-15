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
import com.google.common.base.Throwables;
import com.yammer.backups.ConfigurationTestUtil;
import com.yammer.backups.api.metadata.VerificationMetadata;
import com.yammer.backups.storage.metadata.azure.AzureTableMetadataStorage;
import com.yammer.storage.file.azure.AzureFileStorageConfiguration;
import org.junit.Before;
import org.junit.Ignore;

import java.io.IOException;

@Ignore
public class AzureVerificationProcessorTest extends VerificationProcessorTest<AzureTableMetadataStorage<VerificationMetadata>> {

    private String azureNamespace;
    private AzureFileStorageConfiguration configuration;

    @Override
    @Before
    public void setUp() throws Exception {
        azureNamespace = String.format("test%d", System.currentTimeMillis()); // Azure is picky about table names
        configuration = ConfigurationTestUtil.loadConfiguration().getOffsiteConfiguration().getStorageConfiguration();
        super.setUp();
    }

    @Override
    protected AzureTableMetadataStorage<VerificationMetadata> getMetadataStorage() {
        try {
            return new AzureTableMetadataStorage<>(VerificationMetadata.class, configuration, azureNamespace, new MetricRegistry());
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected void clearMetadataStorage(AzureTableMetadataStorage<VerificationMetadata> storage) {
        storage.clear();
    }
}

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

import com.microsoft.windowsazure.services.core.storage.StorageException;
import com.yammer.backups.api.metadata.BackupMetadata;
import com.yammer.backups.storage.metadata.azure.AzureTablelikeMetadataStorage;
import com.yammer.storage.file.azure.AzurelikeFileStorage;
import com.yammer.storage.file.local.LocalFileStorageConfiguration;
import io.dropwizard.configuration.ConfigurationException;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

public class AzurelikeBackupProcessorTest extends BackupProcessorTest<AzureTablelikeMetadataStorage<BackupMetadata>, AzurelikeFileStorage> {

    @Rule
    public final TemporaryFolder testFolder = new TemporaryFolder();

    @Override
    protected AzureTablelikeMetadataStorage<BackupMetadata> getMetadataStorage() throws IOException, ConfigurationException {
        return new AzureTablelikeMetadataStorage<>(BackupMetadata.class);
    }

    @Override
    protected void clearMetadataStorage(AzureTablelikeMetadataStorage<BackupMetadata> storage) {
        storage.clear();
    }

    @Override
    protected AzurelikeFileStorage getOffsiteStorage() throws StorageException, InvalidKeyException, URISyntaxException, IOException {
        return new AzurelikeFileStorage(new LocalFileStorageConfiguration(testFolder.getRoot()));
    }
}

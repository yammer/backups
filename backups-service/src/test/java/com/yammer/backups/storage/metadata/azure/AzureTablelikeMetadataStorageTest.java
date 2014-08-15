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

import com.yammer.backups.api.metadata.BackupMetadata;
import com.yammer.backups.storage.metadata.MetadataStorage;
import com.yammer.backups.storage.metadata.MetadataStorageTest;

public class AzureTablelikeMetadataStorageTest extends MetadataStorageTest {

    @Override
    protected MetadataStorage<BackupMetadata> getMetadataStorage() {
        return new AzureTablelikeMetadataStorage<>(BackupMetadata.class);
    }

    @Override
    protected void clearMetadataStorage(MetadataStorage<BackupMetadata> storage) {
        storage.clear();
    }
}

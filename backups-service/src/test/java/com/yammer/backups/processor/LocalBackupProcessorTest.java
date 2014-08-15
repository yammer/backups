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

import com.yammer.backups.api.metadata.BackupMetadata;
import com.yammer.backups.storage.metadata.memory.InMemoryMetadataStorage;
import com.yammer.storage.file.local.LocalFileStorage;
import com.yammer.storage.file.local.LocalFileStorageConfiguration;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

public class LocalBackupProcessorTest extends BackupProcessorTest<InMemoryMetadataStorage<BackupMetadata>, LocalFileStorage> {

    @Rule
    public final TemporaryFolder offsiteTestFolder = new TemporaryFolder();

    @Override
    protected InMemoryMetadataStorage<BackupMetadata> getMetadataStorage() {
        return new InMemoryMetadataStorage<>();
    }

    @Override
    protected void clearMetadataStorage(InMemoryMetadataStorage<BackupMetadata> storage) {
        storage.clear();
    }

    @Override
    protected LocalFileStorage getOffsiteStorage() throws IOException {
        return new LocalFileStorage(new LocalFileStorageConfiguration(offsiteTestFolder.getRoot()));
    }
}

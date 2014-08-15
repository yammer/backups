package com.yammer.storage.file.local;

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

import com.google.common.io.Files;
import com.yammer.storage.file.FileStorage;
import com.yammer.storage.file.FileStorageTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class LocalFileStorageTest extends FileStorageTest<LocalFileStorage> {

    @Rule
    public final TemporaryFolder testFolder = new TemporaryFolder();

    @Override
    protected LocalFileStorage getFileStorage() throws IOException {
        return new LocalFileStorage(new LocalFileStorageConfiguration(testFolder.getRoot()));
    }

    @Test
    public void testWorksWithExistingDirectory() throws IOException {
        final File root = Files.createTempDir();
        assertTrue(root.exists());

        final FileStorage storage = new LocalFileStorage(new LocalFileStorageConfiguration(root));
        assertNotNull(storage);
        assertTrue(root.exists());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testWorksWithoutExistingDirectory() throws IOException {
        final File root = Files.createTempDir();
        root.delete();
        assertFalse(root.exists());

        final FileStorage storage = new LocalFileStorage(new LocalFileStorageConfiguration(root));
        assertNotNull(storage);
        assertTrue(root.exists());
    }
}

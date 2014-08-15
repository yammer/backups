package com.yammer.storage.file;

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

import com.google.common.io.ByteStreams;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.*;

public abstract class FileStorageTest<T extends FileStorage> {

    private static final byte[] BYTES = "Now this is the story all about how, My life got flipped, turned upside down, And I'd like to take a minute just sit right there, I'll tell you how I became the prince of a town called Bel-air.".getBytes();

    protected T storage;
    protected String namespace;
    protected String path;

    protected abstract T getFileStorage() throws Exception;

    @Before
    public void setUp() throws Exception {
        storage = this.getFileStorage();
        storage.start();

        namespace = String.format("test%s", UUID.randomUUID());
        path = UUID.randomUUID().toString();
    }

    @After
    public void tearDown() throws Exception {
        storage.delete(namespace);
        storage.stop();
    }

    private void createFile(String namespace, String path, byte[] content) throws IOException {
        try (final OutputStream out = storage.upload(namespace, path)) {
            out.write(content);
        }
    }

    private void appendFile(String namespace, String path, byte[] content) throws IOException {
        try (final OutputStream out = storage.append(namespace, path)) {
            out.write(content);
        }
    }

    private byte[] readFile(String namespace, String path) throws IOException {
        try (final InputStream in = storage.download(namespace, path)) {
            return ByteStreams.toByteArray(in);
        }
    }

    @Test
    public void testFileExists() throws IOException {
        assertFalse(storage.exists(namespace, path));

        this.createFile(namespace, path, BYTES);
        assertTrue(storage.exists(namespace, path));
    }

    @Test
    public void testDelete() throws IOException {
        this.createFile(namespace, path, BYTES);
        assertTrue(storage.exists(namespace, path));

        assertTrue(storage.delete(namespace, path));
        assertFalse(storage.exists(namespace, path));
    }

    @Test
    public void testAppend() throws IOException {
        this.appendFile(namespace, path, BYTES);
        assertEquals(BYTES.length, readFile(namespace, path).length);

        this.appendFile(namespace, path, BYTES);
        assertEquals(BYTES.length * 2, readFile(namespace, path).length);
    }

    @Test
    public void testDeleteInvalidFile() throws IOException {
        assertFalse(storage.delete(namespace, path));
    }

    @Test
    public void testUploadDownload() throws IOException {
        this.createFile(namespace, path, BYTES);

        final byte[] downloaded = this.readFile(namespace, path);
        assertTrue(Arrays.equals(BYTES, downloaded));
    }

    @Test
    public void testUploadMultipleDistinctFiles() throws IOException {
        this.createFile(namespace, UUID.randomUUID().toString(), BYTES);
        this.createFile(namespace, UUID.randomUUID().toString(), BYTES);
    }

    @Test(expected = IOException.class)
    public void testOverwriteFile() throws IOException {
        this.createFile(namespace, path, BYTES);
        this.createFile(namespace, path, BYTES);
    }

    @Test
    public void testPing() throws IOException {
        assertTrue(storage.ping());
    }
}

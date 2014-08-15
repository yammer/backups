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

import com.google.common.annotations.VisibleForTesting;
import com.yammer.storage.file.FileStorage;
import io.dropwizard.util.Size;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;

public class LocalFileStorage implements FileStorage {

    private static final Logger LOG = LoggerFactory.getLogger(LocalFileStorage.class);

    private final File root;

    public LocalFileStorage(LocalFileStorageConfiguration configuration) throws IOException {
        root = configuration.getRoot();

        if (!root.isDirectory() && !root.mkdirs()) {
            throw new IOException("Unable to find/create root directory: " + root);
        }
    }

    @Override
    public void start() {
        LOG.info("Created new {} with {} capacity, {} used, {} free", this, getTotalSpace(), getUsedSpace(), getFreeSpace());
    }

    @Override
    public void stop() {

    }

    private File getBucket(String namespace) throws IOException {
        final File file = new File(root, namespace);
        if (!file.isDirectory() && !file.mkdirs()) {
            throw new IOException("Unable to find/create bucket: " + namespace);
        }

        return file;
    }

    private File getLocalFile(String namespace, String path) throws IOException {
        final File bucket = this.getBucket(namespace);
        return new File(bucket, path);
    }

    @Override
    public OutputStream upload(String namespace, String path) throws IOException {
        final File file = this.getLocalFile(namespace, path);
        if (file.exists()) {
            throw new FileAlreadyExistsException("File already exists");
        }

        return new FileOutputStream(file);
    }

    @Override
    public InputStream download(String namespace, String path) throws IOException {
        final File file = this.getLocalFile(namespace, path);
        return new FileInputStream(file);
    }

    @Override
    public OutputStream append(String namespace, String path) throws IOException {
        final File file = this.getLocalFile(namespace, path);
        return new FileOutputStream(file, true);
    }

    @Override
    public boolean exists(String namespace, String path) throws IOException {
        final File file = this.getLocalFile(namespace, path);
        return file.exists();
    }

    @Override
    public boolean delete(String namespace, String path) throws IOException {
        final File file = this.getLocalFile(namespace, path);
        return file.delete();
    }

    @VisibleForTesting
    @Override
    public boolean delete(String namespace) throws IOException {
        final File bucket = this.getBucket(namespace);
        FileUtils.deleteDirectory(bucket);
        return true;
    }

    @Override
    public boolean ping() throws IOException {
        return root.isDirectory();
    }

    @Override
    public String toString() {
        return "LocalFileStorage{" +
                "root=" + root +
                '}';
    }

    @Override
    public Size getTotalSpace() {
        return Size.bytes(root.getTotalSpace());
    }

    @Override
    public Size getUsedSpace() {
        final Size totalSpace = this.getTotalSpace();
        final Size freeSpace = this.getFreeSpace();

        return Size.bytes(totalSpace.toBytes() - freeSpace.toBytes());
    }

    @Override
    public Size getFreeSpace() {
        return Size.bytes(root.getFreeSpace());
    }
}

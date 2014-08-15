package com.yammer.storage.file.instrumented;

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
import com.yammer.storage.file.FileStorage;
import com.yammer.storage.file.instrumented.metrics.FreeSpaceGauge;
import com.yammer.storage.file.instrumented.metrics.TotalSpaceGauge;
import com.yammer.storage.file.instrumented.metrics.UsedSpaceGauge;
import io.dropwizard.util.Size;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class InstrumentedFileStorage implements FileStorage {

    private final FileStorage delegate;

    public InstrumentedFileStorage(String name, FileStorage delegate, MetricRegistry registry) {
        this.delegate = delegate;

        registry.register(String.format("%s-used-space", name), new UsedSpaceGauge(delegate));
        registry.register(String.format("%s-total-space", name), new TotalSpaceGauge(delegate));
        registry.register(String.format("%s-free-space", name), new FreeSpaceGauge(delegate));
    }

    @Override
    @SuppressWarnings("SignatureDeclareThrowsException")
    public void start() throws Exception {
        delegate.start();
    }

    @Override
    @SuppressWarnings("SignatureDeclareThrowsException")
    public void stop() throws Exception {
        delegate.stop();
    }

    @Override
    public OutputStream upload(String namespace, String path) throws IOException {
        return delegate.upload(namespace, path);
    }

    @Override
    public InputStream download(String namespace, String path) throws IOException {
        return delegate.download(namespace, path);
    }

    @Override
    public OutputStream append(String namespace, String path) throws IOException {
        return delegate.append(namespace, path);
    }

    @Override
    public boolean exists(String namespace, String path) throws IOException {
        return delegate.exists(namespace, path);
    }

    @Override
    public boolean delete(String namespace, String path) throws IOException {
        return delegate.delete(namespace, path);
    }

    @Override
    public boolean ping() throws IOException {
        return delegate.ping();
    }

    @Override
    public Size getTotalSpace() throws IOException {
        return delegate.getTotalSpace();
    }

    @Override
    public Size getUsedSpace() throws IOException {
        return delegate.getUsedSpace();
    }

    @Override
    public Size getFreeSpace() throws IOException {
        return delegate.getFreeSpace();
    }

    @Override
    public boolean delete(String namespace) throws IOException {
        return delegate.delete(namespace);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}

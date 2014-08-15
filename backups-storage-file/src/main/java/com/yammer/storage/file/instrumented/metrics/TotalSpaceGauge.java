package com.yammer.storage.file.instrumented.metrics;

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

import com.codahale.metrics.Gauge;
import com.google.common.base.Throwables;
import com.yammer.storage.file.FileStorage;

import java.io.IOException;

public class TotalSpaceGauge implements Gauge<Long> {

    private final FileStorage delegate;

    public TotalSpaceGauge(FileStorage delegate) {
        this.delegate = delegate;
    }

    @Override
    public Long getValue() {
        try {
            return delegate.getTotalSpace().toBytes();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}

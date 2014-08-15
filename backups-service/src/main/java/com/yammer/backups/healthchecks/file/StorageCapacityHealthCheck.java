package com.yammer.backups.healthchecks.file;

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

import com.codahale.metrics.health.HealthCheck;
import com.yammer.storage.file.FileStorage;
import io.dropwizard.util.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class StorageCapacityHealthCheck extends HealthCheck {

    private static final Logger LOG = LoggerFactory.getLogger(StorageCapacityHealthCheck.class);

    private final FileStorage storage;
    private final Size minStorage;

    public StorageCapacityHealthCheck(FileStorage storage, Size minStorage) {
        this.storage = storage;
        this.minStorage = minStorage;
    }

    @Override
    protected Result check() throws IOException {
        final Size freeSpace = storage.getFreeSpace();
        if (freeSpace.toBytes() > minStorage.toBytes()) {
            LOG.trace("Free space {} is greater than threshold {}, skipping", freeSpace, minStorage);
            return Result.healthy();
        }

        LOG.warn("Free space {} is less than threshold {}", freeSpace, minStorage);
        return Result.unhealthy(freeSpace.toString());
    }
}

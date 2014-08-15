package com.yammer.backups.healthchecks;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class FileStorageConnectivityHealthCheck extends HealthCheck {

    private static final Logger LOG = LoggerFactory.getLogger(FileStorageConnectivityHealthCheck.class);

    private final FileStorage storage;

    public FileStorageConnectivityHealthCheck(FileStorage storage) {
        this.storage = storage;
    }

    @Override
    protected Result check() throws IOException {
        if (storage.ping()) {
            LOG.trace("Success PING for {}", storage);
            return Result.healthy();
        }

        LOG.warn("Failed to PING {}", storage);
        return Result.unhealthy("No response to PING");
    }
}

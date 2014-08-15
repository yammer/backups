package com.yammer.backups.lock;

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

import com.microsoft.windowsazure.services.blob.client.CloudBlobContainer;
import com.microsoft.windowsazure.services.core.storage.StorageException;

import java.net.URISyntaxException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LockManager {

    private final ScheduledExecutorService threadPool;
    private final CloudBlobContainer container;
    private final long timeoutInSeconds;

    public LockManager(ScheduledExecutorService threadPool,
                       CloudBlobContainer container,
                       long timeout,
                       TimeUnit timeoutUnit) {
        this.threadPool = threadPool;
        this.container = container;
        this.timeoutInSeconds = timeoutUnit.toSeconds(timeout);
        if (timeoutInSeconds < 15 || timeoutInSeconds > 60) {
            throw new IllegalArgumentException("Timeout must be between 15 and 60 seconds.");
        }
    }

    public Lock getLock(String id) throws StorageException, URISyntaxException {
        return new Lock(threadPool,
                        container.getPageBlobReference(id),
                        id,
                        (int) timeoutInSeconds);
    }
}
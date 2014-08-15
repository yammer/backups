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

import com.microsoft.windowsazure.services.core.storage.StorageException;
import io.dropwizard.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;

public class DistributedLockManager {

    private static final Logger LOG = LoggerFactory.getLogger(DistributedLockManager.class);
    private static final String LOCK_KEY_TEMPLATE = "%s.lock";

    private final LockManager lockManager;
    private final Duration timeout;
    private final Duration retryDelay;

    public DistributedLockManager(LockManager lockManager, Duration timeout, Duration retryDelay) {
        this.lockManager = lockManager;
        this.timeout = timeout;
        this.retryDelay = retryDelay;
    }

    public DistributedLock lock(String id) {
        try {
            final String key = String.format(LOCK_KEY_TEMPLATE, id);
            return new DistributedLock(lockManager.getLock(key));
        }
        catch (StorageException | URISyntaxException e) {
            LOG.error("Failed to acquire lock for: " + id, e);
            throw new LockException(e);
        }
    }

    public void acquire(DistributedLock lock) {
        lock.acquire(timeout, retryDelay);
    }
}

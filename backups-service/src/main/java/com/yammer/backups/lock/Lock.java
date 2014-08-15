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

import com.microsoft.windowsazure.services.blob.client.CloudPageBlob;
import com.microsoft.windowsazure.services.core.storage.AccessCondition;
import com.microsoft.windowsazure.services.core.storage.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Lock implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(LockManager.class);

    private final ScheduledExecutorService threadPool;
    private final CloudPageBlob blob;
    private final String name;
    private final int timeoutInSeconds;
    private final int refreshPeriodInSeconds;
    private final String leaseId;
    private final AccessCondition leaseCondition;

    private ScheduledFuture<?> future;

    Lock(ScheduledExecutorService threadPool,
         CloudPageBlob blob,
         String name, int timeoutInSeconds) {
        this.threadPool = threadPool;
        this.blob = blob;
        this.name = name;
        this.timeoutInSeconds = timeoutInSeconds;
        this.refreshPeriodInSeconds = timeoutInSeconds / 2; // renew lease halfway through
        this.leaseId = UUID.randomUUID().toString();
        this.leaseCondition = AccessCondition.generateLeaseCondition(leaseId);
    }

    @Override
    public void close() {
        if (future != null) {
            future.cancel(true);

            LOG.trace("Releasing {}", this);

            try {
                blob.releaseLease(leaseCondition);
            } catch (StorageException e) {
                LOG.warn("Error releasing {}", this, e);
            }
        }
    }

    @Override
    public String toString() {
        return name + '/' + leaseId;
    }

    public boolean tryAcquire() {
        try {
            blob.create(0);
            LOG.debug("Created empty blob {}", name);
        } catch (StorageException ignored) {
            // don't care if we can't create it
        }

        try {
            LOG.trace("Acquiring {}", this);
            final String actualLeaseId = blob.acquireLease(timeoutInSeconds, leaseId);
            if (leaseId.equals(actualLeaseId)) {
                LOG.trace("Acquired {}", this);
                scheduleAutoRenewTask();
                return true;
            }
        } catch (StorageException e) {
            if (e.getMessage().contains("already a lease")) {
                LOG.debug("Error acquiring {}", this, e);
            } else {
                LOG.warn("Error acquiring {}", this, e);
            }
        }
        LOG.info("Failed to acquire {}", this);
        return false;
    }

    private void scheduleAutoRenewTask() {
        if (future == null) {
            this.future = threadPool.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try {
                        LOG.trace("Renewing {}", this);
                        blob.renewLease(leaseCondition);
                    } catch (StorageException e) {
                        LOG.warn("Error renewing {}", this, e);
                    }
                }
            }, refreshPeriodInSeconds, refreshPeriodInSeconds, TimeUnit.SECONDS);
        }
    }
}
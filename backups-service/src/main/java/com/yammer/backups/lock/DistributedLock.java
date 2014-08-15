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

import io.dropwizard.util.Duration;

import java.util.concurrent.TimeoutException;

public class DistributedLock implements AutoCloseable {

    private final Lock lock;

    public DistributedLock(Lock lock) {
        this.lock = lock;
    }

    public void acquire(Duration timeout, Duration retryDelay) {
        final long stopTime = System.nanoTime() + timeout.toNanoseconds();
        final long retryDelayMillis = retryDelay.toMilliseconds();

        try {
            while (System.nanoTime() < stopTime) {
                if (lock.tryAcquire()) {
                    return;
                }

                Thread.sleep(retryDelayMillis);
            }

            throw new TimeoutException("Unable to acquire " + this);
        }
        catch (Exception e) {
            throw new LockException(e);
        }
    }

    @Override
    public void close() {
        lock.close();
    }

    @Override
    public String toString() {
        return "DistributedLock{" +
                "lock=" + lock +
                '}';
    }
}

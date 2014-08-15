package com.yammer.backups;

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

import com.google.common.collect.Maps;
import com.microsoft.windowsazure.services.core.storage.StorageException;
import com.yammer.backups.lock.DistributedLockManager;
import com.yammer.backups.lock.Lock;
import com.yammer.backups.lock.LockManager;
import io.dropwizard.util.Duration;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import static org.mockito.Mockito.*;

public class MockDistributedLockManager extends DistributedLockManager {

    private static final Map<String, Lock> locks = Maps.newConcurrentMap();

    private static LockManager mockLockManager() throws URISyntaxException, StorageException {
        final LockManager lockManager = mock(LockManager.class);
        when(lockManager.getLock(anyString())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final String key = (String) invocation.getArguments()[0];

                synchronized (locks) {
                    if (!locks.containsKey(key)) {
                        final Lock lock = mock(Lock.class);
                        final ReentrantLock actualLock = new ReentrantLock();

                        doAnswer(new Answer<Object>() {
                            @Override
                            public Object answer(InvocationOnMock invocation) throws Throwable {
                                return actualLock.tryLock();
                            }
                        }).when(lock).tryAcquire();

                        doAnswer(new Answer<Object>() {
                            @Override
                            public Object answer(InvocationOnMock invocation) throws Throwable {
                                actualLock.unlock();
                                return null;
                            }
                        }).when(lock).close();

                        locks.put(key, lock);
                    }

                    return locks.get(key);
                }
            }
        });

        return lockManager;
    }

    public MockDistributedLockManager(Duration timeout) throws URISyntaxException, StorageException {
        super(mockLockManager(), timeout, Duration.milliseconds(10));
    }
}

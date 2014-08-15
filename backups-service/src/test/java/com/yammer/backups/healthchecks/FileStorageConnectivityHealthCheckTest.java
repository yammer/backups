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
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileStorageConnectivityHealthCheckTest {

    private FileStorage storage;
    private FileStorageConnectivityHealthCheck healthcheck;

    @Before
    public void setUp() {
        storage = mock(FileStorage.class);
        healthcheck = new FileStorageConnectivityHealthCheck(storage);
    }

    @Test
    public void testHealthyStorage() throws IOException {
        when(storage.ping()).thenReturn(true);

        final HealthCheck.Result result = healthcheck.check();
        assertTrue(result.isHealthy());
    }

    @Test
    public void testUnhealthyStorage() throws IOException {
        when(storage.ping()).thenReturn(false);

        final HealthCheck.Result result = healthcheck.check();
        assertFalse(result.isHealthy());
    }
}

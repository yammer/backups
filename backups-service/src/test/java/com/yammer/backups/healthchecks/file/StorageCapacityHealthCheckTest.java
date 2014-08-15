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

import com.yammer.storage.file.FileStorage;
import io.dropwizard.util.Size;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StorageCapacityHealthCheckTest {
    private StorageCapacityHealthCheck healthCheck;
    private FileStorage storage;

    @Before
    public void setUp() {
        storage = mock(FileStorage.class);
        healthCheck = new StorageCapacityHealthCheck(storage, Size.megabytes(10));
    }

    @Test
    public void testInsufficientStorage() throws IOException {
        when(storage.getFreeSpace()).thenReturn(Size.megabytes(9));
        assertFalse(healthCheck.check().isHealthy());
    }

    @Test
    public void testSufficientStorage() throws IOException{
        when(storage.getFreeSpace()).thenReturn(Size.megabytes(11));
        assertTrue(healthCheck.check().isHealthy());
    }

}

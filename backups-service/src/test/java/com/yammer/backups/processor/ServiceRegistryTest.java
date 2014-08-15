package com.yammer.backups.processor;

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

import com.yammer.backups.api.metadata.BackupMetadata;
import com.yammer.backups.service.metadata.ServiceMetadata;
import com.yammer.backups.storage.metadata.MetadataStorage;
import com.yammer.backups.storage.metadata.azure.AzureTablelikeMetadataStorage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ServiceRegistryTest {

    private static final String SERVICE_NAME = "test";
    private static final String SOURCE_ADDRESS = "127.0.0.1";
    private static final String LOCAL_NODE = "localhost";

    private MetadataStorage<ServiceMetadata> storage;
    private ServiceRegistry manager;

    @Before
    public void setUp() throws Exception {
        storage = new AzureTablelikeMetadataStorage<>(ServiceMetadata.class);
        manager = new ServiceRegistry(storage);
    }

    @Test
    public void test() throws Exception {
        testNewService();
        testHealthcheckDisabled();
        testMetadataNotOverwritten();
    }

    @Test
    public void testHealthcheckDisabledOnMissingData() {
        /* If there is no metadata the healthcheck should be considered enabled */
        assertFalse(manager.healthCheckDisabled("anyOldService"));
    }

    private void testNewService() {
        manager.backupCreated(new BackupMetadata(SERVICE_NAME, SOURCE_ADDRESS, LOCAL_NODE));

        final ServiceMetadata serviceMetadata = extractServiceMetadata();
        assertEquals(SERVICE_NAME, serviceMetadata.getId());
        assertFalse(serviceMetadata.isDisableHealthcheck());
    }

    private void testHealthcheckDisabled() {
        manager.disableHealthcheck(SERVICE_NAME);

        final ServiceMetadata serviceMetadata = extractServiceMetadata();
        assertTrue(serviceMetadata.isDisableHealthcheck());
    }

    private void testMetadataNotOverwritten() {
        manager.backupCreated(new BackupMetadata(SERVICE_NAME, SOURCE_ADDRESS, LOCAL_NODE));

        final ServiceMetadata serviceMetadata = extractServiceMetadata();
        assertTrue(serviceMetadata.isDisableHealthcheck());
    }

    private ServiceMetadata extractServiceMetadata() {
        return manager.getServiceMetadata(SERVICE_NAME).get();
    }

    @After
    public void tearDown() {
        storage.clear();
    }
}
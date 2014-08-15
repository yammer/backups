package com.yammer.backups.resources.api;

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

import com.yammer.backups.processor.ServiceRegistry;
import com.yammer.backups.service.metadata.ServiceMetadata;
import com.yammer.backups.storage.metadata.MetadataStorage;
import com.yammer.backups.storage.metadata.memory.InMemoryMetadataStorage;
import io.dropwizard.auth.basic.BasicCredentials;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class ServicesResourceTest {

    public static final String SERVICE_NAME = "test_service";

    private MetadataStorage<ServiceMetadata> metadataStorage;
    ServiceRegistry registry;
    private ServicesResource resource;

    @Before
    public void setup() throws Exception {
        metadataStorage = new InMemoryMetadataStorage<>();
        registry = new ServiceRegistry(metadataStorage);
        resource = new ServicesResource(registry);
    }

    @Test
    public void testGet() {
        metadataStorage.put(new ServiceMetadata("test_service", false));
        Collection<ServiceMetadata> services = resource.get();
        assertEquals(1, services.size());
        assertEquals(SERVICE_NAME, services.iterator().next().getId());
    }

    @Test
    public void testPut() {
        metadataStorage.put(new ServiceMetadata(SERVICE_NAME, false));
        resource.put(mock(BasicCredentials.class), SERVICE_NAME, new ServiceMetadata(SERVICE_NAME, true));
        Collection<ServiceMetadata> services = resource.get();
        assertEquals(1, services.size());
        assertEquals(true, services.iterator().next().isDisableHealthcheck());
    }

    @Test(expected = WebApplicationException.class)
    public void testPutInvalid() {
        resource.put(mock(BasicCredentials.class), SERVICE_NAME, new ServiceMetadata("some other service name", true));
    }

    @Test(expected = WebApplicationException.class)
    public void testPutInvalidService() {
        resource.put(mock(BasicCredentials.class), SERVICE_NAME, new ServiceMetadata(SERVICE_NAME, true));
    }
}

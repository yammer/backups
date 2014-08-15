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
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.yammer.backups.AbstractBackupServiceResourceTest;
import com.yammer.backups.api.metadata.VerificationMetadata;
import com.yammer.backups.error.MetadataNotFoundException;
import com.yammer.backups.processor.VerificationProcessor;
import com.yammer.backups.provider.VerificationMetadataProvider;
import com.yammer.backups.storage.metadata.MetadataStorage;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


public class VerificationResourceTest extends AbstractBackupServiceResourceTest {

    private static final String SERVICE_NAME = "test";
    private static final String API_PATH = "/api/verification";
    private static final String API_SERVICE_PATH=API_PATH + "/" + SERVICE_NAME;
    private static final String SOURCE_ADDRESS = "127.0.0.1";
    private static final String LOCAL_NODE = "localhost";

    private VerificationProcessor processor;
    private MetadataStorage<VerificationMetadata> verificationStorage;

    @SuppressWarnings("unchecked")
    @Override
    protected ResourceTestRule.Builder setUpResources(ResourceTestRule.Builder builder) {
        verificationStorage = mock(MetadataStorage.class);
        processor = mock(VerificationProcessor.class);

        return super.setUpResources(builder)
                .addResource(new VerificationResource(processor))
                .addProvider(new VerificationMetadataProvider(verificationStorage));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testListAllVerifications(){
        when (processor.listMetadata(any(Optional.class))).thenAnswer(new Answer<Collection<VerificationMetadata>>() {
            @Override
            public Collection<VerificationMetadata> answer(InvocationOnMock invocation) throws Throwable {
                return ImmutableSet.of(
                    new VerificationMetadata(SERVICE_NAME, "test", SOURCE_ADDRESS, LOCAL_NODE),
                    new VerificationMetadata("other_service", "test", SOURCE_ADDRESS, LOCAL_NODE)) ;
            }
        });

        final ClientResponse response = resources.client().resource(API_PATH).get(ClientResponse.class);
        try {
            assertEquals(ClientResponse.Status.OK,response.getStatusInfo());
            verify(processor, times(1)).listMetadata(any(Optional.class));

            final Collection<VerificationMetadata> verificationList = response.getEntity(new GenericType<Collection<VerificationMetadata>>(){});
            assertEquals(2, verificationList.size());

            assertTrue(verificationList.toString().contains(SERVICE_NAME));
            assertTrue(verificationList.toString().contains("other_service"));
        }
        finally {
            response.close();
        }
    }

    @Test
    public void testCreateVerification() throws MetadataNotFoundException {
        when(processor.create(anyString(), anyString(), anyString())).thenAnswer(new Answer<VerificationMetadata>() {
            @Override
            public VerificationMetadata answer(InvocationOnMock invocation) throws Throwable {
                final String serviceName = (String) invocation.getArguments()[0];
                final String backupId = (String) invocation.getArguments()[1];
                final String remoteAddr = (String) invocation.getArguments()[2];
                return new VerificationMetadata(serviceName, backupId, remoteAddr, LOCAL_NODE);
            }
        });

        final ClientResponse response = resources.client()
                .resource(API_SERVICE_PATH)
                .queryParam("backupId", "test")
                .post(ClientResponse.class);
        try {
            assertEquals(ClientResponse.Status.OK, response.getStatusInfo());

            final String verificationId = response.getEntity(String.class);
            assertNotNull(verificationId);

            verify(processor, times(1)).create(eq(SERVICE_NAME), eq("test"), eq(SOURCE_ADDRESS));
        }
        finally {
            response.close();
        }
    }

    @Test
    public void testFinishVerification() throws MetadataNotFoundException {
        final String id = UUID.randomUUID().toString();
        final VerificationMetadata verificationMetadata = new VerificationMetadata(SERVICE_NAME, "test", id, LOCAL_NODE);

        when(verificationStorage.get(eq(SERVICE_NAME), eq(id))).thenReturn(
            Optional.of(verificationMetadata));

        final String path = String.format(API_SERVICE_PATH + "/%s/finish", id);

        final ClientResponse response = resources.client().resource(path).post(ClientResponse.class);
        try {
            assertEquals(ClientResponse.Status.NO_CONTENT, response.getStatusInfo());
        }
        finally {
            response.close();
        }
    }

    @Test
    public void testFinishUnknownVerification() throws MetadataNotFoundException {
        final String id = UUID.randomUUID().toString();
        when(verificationStorage.get(eq(SERVICE_NAME), eq(id))).thenReturn(
            Optional.<VerificationMetadata>absent());

        final String path = String.format(API_SERVICE_PATH + "/%s/finish", id);
        final ClientResponse response = resources.client().resource(path).post(ClientResponse.class);
        try {
            assertEquals(ClientResponse.Status.NOT_FOUND, response.getStatusInfo());
        }
        finally {
            response.close();
        }
    }

    @Test
    public void testFinishSuccess() throws MetadataNotFoundException {
        final String id = UUID.randomUUID().toString();
        final VerificationMetadata verificationMetadata = new VerificationMetadata(SERVICE_NAME,id, SOURCE_ADDRESS, LOCAL_NODE);

        when(verificationStorage.get(eq(SERVICE_NAME), eq(id))).thenReturn(
            Optional.of(verificationMetadata));

        final String path = String.format(API_SERVICE_PATH + "/%s/finish", id);

        final ClientResponse response = resources.client().resource(path).queryParam("success", "true").post(ClientResponse.class, "hello world");
        try {
            assertEquals(ClientResponse.Status.NO_CONTENT, response.getStatusInfo());
            verify(processor, times(1)).finish(any(VerificationMetadata.class), eq("hello world"), eq(true));
        }
        finally {
            response.close();
        }
    }

    @Test
    public void testFinishFailed() throws MetadataNotFoundException {
        final String id = UUID.randomUUID().toString();
        final VerificationMetadata verificationMetadata = new VerificationMetadata(SERVICE_NAME, "test", id, LOCAL_NODE);

        when(verificationStorage.get(eq(SERVICE_NAME), eq(id))).thenReturn(
            Optional.of(verificationMetadata));

        final String path = String.format(API_SERVICE_PATH + "/%s/finish", id);

        final ClientResponse response = resources.client().resource(path).queryParam("success", "false").post(ClientResponse.class, "hello world");
        try {
            assertEquals(ClientResponse.Status.NO_CONTENT, response.getStatusInfo());
            verify(processor, times(1)).finish(any(VerificationMetadata.class), eq("hello world"), eq(false));
        }
        finally {
            response.close();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testListVerifications() {
        final ClientResponse response = resources.client().resource(API_SERVICE_PATH).get(ClientResponse.class);
        try {
            assertEquals(ClientResponse.Status.OK, response.getStatusInfo());
            verify(processor, times(1)).listMetadata(eq(SERVICE_NAME), any(Optional.class));
        }
        finally {
            response.close();
        }
    }

    @Test
    public void testGetVerification() throws MetadataNotFoundException {
        final String id = UUID.randomUUID().toString();
        final VerificationMetadata verificationMetadata = new VerificationMetadata(SERVICE_NAME, id, SOURCE_ADDRESS, LOCAL_NODE);

        when(verificationStorage.get(eq(SERVICE_NAME), eq(id))).thenReturn(Optional.of(verificationMetadata));

        final String path = String.format(API_SERVICE_PATH + "/%s", id);

        final ClientResponse response = resources.client().resource(path).get(ClientResponse.class);
        try {
            assertEquals(ClientResponse.Status.OK, response.getStatusInfo());
            verify(verificationStorage).get(eq(SERVICE_NAME), eq(id));
        }
        finally {
            response.close();
        }
    }

    @Test
    public void testGetUnknownVerification() throws MetadataNotFoundException {
        final String id = UUID.randomUUID().toString();
        final String path = String.format(API_SERVICE_PATH + "/%s", id);

        when(verificationStorage.get(anyString(), anyString())).thenReturn(Optional.<VerificationMetadata>absent());

        final ClientResponse response = resources.client().resource(path).get(ClientResponse.class);
        try {
            assertEquals(ClientResponse.Status.NOT_FOUND, response.getStatusInfo());
        }
        finally {
            response.close();
        }
    }

    @Test
    public void testDeleteVerification() throws IOException {
        final String id = UUID.randomUUID().toString();
        final VerificationMetadata verificationMetadata = new VerificationMetadata(SERVICE_NAME, "test", id, LOCAL_NODE);

        when(verificationStorage.get(eq(SERVICE_NAME), eq(id))).thenReturn(Optional.of(verificationMetadata));

        final String path = String.format(API_SERVICE_PATH + "/%s", id);

        final ClientResponse response = resources.client().resource(path).delete(ClientResponse.class);
        try {
            assertEquals(ClientResponse.Status.NO_CONTENT, response.getStatusInfo());
            verify(processor, times(1)).delete(any(VerificationMetadata.class));
        }
        finally {
            response.close();
        }
    }

    @Test
    public void testDeleteUnknownVerification() throws IOException {
        final String id = UUID.randomUUID().toString();

        when(verificationStorage.get(eq(SERVICE_NAME), eq(id))).thenReturn(Optional.<VerificationMetadata>absent());

        final String path = String.format(API_SERVICE_PATH + "/%s", id);

        final ClientResponse response = resources.client().resource(path).delete(ClientResponse.class);
        try {
            assertEquals(ClientResponse.Status.NOT_FOUND, response.getStatusInfo());
        }
        finally {
            response.close();
        }
    }
}

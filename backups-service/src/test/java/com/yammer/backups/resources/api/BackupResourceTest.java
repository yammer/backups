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
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.yammer.backups.AbstractBackupServiceResourceTest;
import com.yammer.backups.api.ClientPermission;
import com.yammer.backups.api.CompressionCodec;
import com.yammer.backups.api.Node;
import com.yammer.backups.api.metadata.BackupMetadata;
import com.yammer.backups.auth.TemporaryTokenGenerator;
import com.yammer.backups.auth.TokenAuthProvider;
import com.yammer.backups.auth.TokenAuthenticator;
import com.yammer.backups.auth.TokenCredentials;
import com.yammer.backups.error.IncorrectNodeExceptionMapper;
import com.yammer.backups.error.InvalidMD5Exception;
import com.yammer.backups.error.MetadataNotFoundException;
import com.yammer.backups.error.NoContentException;
import com.yammer.backups.processor.BackupProcessor;
import com.yammer.backups.provider.BackupMetadataProvider;
import com.yammer.backups.storage.metadata.MetadataStorage;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.util.Collection;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BackupResourceTest extends AbstractBackupServiceResourceTest {

    private static final String SERVICE_NAME = "test";
    private static final String API_PATH = "/api/backup";
    private static final String API_SERVICE_PATH = API_PATH + "/" + SERVICE_NAME;
    private static final String TOKEN = Strings.padEnd("token1", TokenCredentials.MIN_TOKEN_LENGTH, '*');
    private static final String TOKEN_AUTH_HEADER = "Token "+ TOKEN;
    private static final String UNKNOWN_TOKEN = Strings.padEnd("token2", TokenCredentials.MIN_TOKEN_LENGTH, '*');
    private static final String UNKNOWN_TOKEN_AUTH_HEADER = "Token "+ UNKNOWN_TOKEN;
    private static final String SOURCE_ADDRESS = "127.0.0.1";
    private static final String LOCAL_NODE = "localhost";
    private static final String LOCAL_NODE_URL = "http://localhost";
    private static final String OTHER_NODE = "otherNode";
    private static final String OTHER_NODE_URL = "http://othernode";

    private BackupProcessor processor;
    private MetadataStorage<ClientPermission> clientPermissionStorage;
    private MetadataStorage<BackupMetadata> backupStorage;

    @Override
    @SuppressWarnings("unchecked")
    protected ResourceTestRule.Builder setUpResources(ResourceTestRule.Builder builder) {
        processor = mock(BackupProcessor.class);
        clientPermissionStorage = mock(MetadataStorage.class);
        backupStorage = mock(MetadataStorage.class);
        final MetadataStorage<Node> nodeStorage = mock(MetadataStorage.class);

        when(clientPermissionStorage.get(anyString(), anyString()))
            .thenReturn(Optional.<ClientPermission>absent());
        when(clientPermissionStorage.get(eq(SERVICE_NAME), eq(TOKEN)))
            .thenReturn(Optional.of(new ClientPermission(TOKEN,SERVICE_NAME)));
        try {
            when(nodeStorage.get(eq(OTHER_NODE), eq(OTHER_NODE)))
                .thenReturn(Optional.of(new Node(OTHER_NODE, new URL(OTHER_NODE_URL))));
            when(nodeStorage.get(eq(LOCAL_NODE), eq(LOCAL_NODE)))
                .thenReturn(Optional.of(new Node(LOCAL_NODE, new URL(LOCAL_NODE_URL))));
        }
        catch (MalformedURLException e) {
            throw Throwables.propagate(e);
        }

        final TemporaryTokenGenerator tokenGenerator = mock(TemporaryTokenGenerator.class);
        return super.setUpResources(builder)
                .addProvider(new TokenAuthProvider<>(new TokenAuthenticator(clientPermissionStorage, tokenGenerator)))
                .addResource(new BackupResource(processor, clientPermissionStorage, LOCAL_NODE))
                .addProvider(new BackupMetadataProvider(backupStorage))
                .addProvider(new IncorrectNodeExceptionMapper(nodeStorage));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testListAllBackups(){
        when(processor.listMetadata(any(Optional.class))).thenAnswer(new Answer<Collection<BackupMetadata>>() {
            @Override
            public Collection<BackupMetadata> answer(InvocationOnMock invocation) throws Throwable {
                return ImmutableSet.of(
                    new BackupMetadata(SERVICE_NAME, SOURCE_ADDRESS, LOCAL_NODE),
                    new BackupMetadata("other_service", SOURCE_ADDRESS, LOCAL_NODE)) ;
            }
        });

        final ClientResponse response = resources.client().resource(API_PATH).get(ClientResponse.class);
        try {
            assertEquals(ClientResponse.Status.OK,response.getStatusInfo());
            verify(processor, times(1)).listMetadata(any(Optional.class));

            final Collection<BackupMetadata> backupList = response.getEntity(new GenericType<Collection<BackupMetadata>>(){});
            assertEquals(2, backupList.size());

            assertTrue(backupList.toString().contains(SERVICE_NAME));
            assertTrue(backupList.toString().contains("other_service"));
        }
        finally {
            response.close();
        }
    }

    @Test
    public void testCreateBackup() {
        when(processor.create(anyString(), anyString())).thenAnswer(new Answer<BackupMetadata>() {
            @Override
            public BackupMetadata answer(InvocationOnMock invocation) throws Throwable {
                final String serviceName = (String) invocation.getArguments()[0];
                final String remoteAddr = (String) invocation.getArguments()[1];
                return new BackupMetadata(serviceName, remoteAddr, LOCAL_NODE);
            }
        });

        final ClientResponse response = resources.client()
            .resource(API_SERVICE_PATH)
            .header(HttpHeaders.AUTHORIZATION, TOKEN_AUTH_HEADER)
            .post(ClientResponse.class);
        try {
            assertEquals(ClientResponse.Status.OK, response.getStatusInfo());

            final String backupId = response.getEntity(String.class);
            assertNotNull(backupId);
            verify(processor, times(1)).create(eq(SERVICE_NAME), eq(SOURCE_ADDRESS));
        }
        finally {
            response.close();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testStoreBackup() throws IOException {
        final String filename = "testfile";

        final BackupMetadata backup = new BackupMetadata(SERVICE_NAME, SOURCE_ADDRESS, LOCAL_NODE);
        when(backupStorage.get(eq(SERVICE_NAME),eq(backup.getId()))).thenReturn(Optional.of(backup));

        final String path = String.format(API_SERVICE_PATH+"/%s/%s", backup.getId(), filename);

        final ClientResponse response = resources.client()
            .resource(path)
            .header(HttpHeaders.AUTHORIZATION, TOKEN_AUTH_HEADER)
            .put(ClientResponse.class, "hello world");
        try {
            assertEquals(ClientResponse.Status.NO_CONTENT, response.getStatusInfo());
            verify(processor, times(1)).store(any(BackupMetadata.class), any(Optional.class), any(InputStream.class), eq(filename));
        }
        finally {
            response.close();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testStoreEmptyBackup() throws IOException {
        final String filename = "testfile";
        final BackupMetadata backup = new BackupMetadata(SERVICE_NAME,SOURCE_ADDRESS,LOCAL_NODE);
        final String path = String.format(API_SERVICE_PATH+"/%s/%s", backup.getId(), filename);

        when(backupStorage.get(eq(SERVICE_NAME),eq(backup.getId()))).thenReturn(Optional.of(backup));

        doThrow(new NoContentException("test")).when(processor).store(any(BackupMetadata.class), any(Optional.class), any(InputStream.class), eq(filename));

        final ClientResponse response = resources.client()
            .resource(path)
            .header(HttpHeaders.AUTHORIZATION, TOKEN_AUTH_HEADER)
            .put(ClientResponse.class, "hello world");
        try {
            assertEquals(ClientResponse.Status.NOT_ACCEPTABLE, response.getStatusInfo());
        }
        finally {
            response.close();
        }
    }

    @Test
    public void testStoreBackupWithContentMD5() throws IOException {
        final String filename = "testfile";

        final BackupMetadata backup = new BackupMetadata(SERVICE_NAME, SOURCE_ADDRESS, LOCAL_NODE);
        when(backupStorage.get(eq(SERVICE_NAME),eq(backup.getId()))).thenReturn(Optional.of(backup));

        final String path = String.format(API_SERVICE_PATH + "/%s/%s", backup.getId(), filename);
        final String contentMD5 = "test";

        final ClientResponse response = resources.client()
            .resource(path)
            .header(HttpHeaders.CONTENT_MD5, contentMD5)
            .header(HttpHeaders.AUTHORIZATION, TOKEN_AUTH_HEADER)
            .put(ClientResponse.class, "hello world");
        try {
            assertEquals(ClientResponse.Status.NO_CONTENT, response.getStatusInfo());
            verify(processor, times(1)).store(any(BackupMetadata.class), eq(Optional.of(contentMD5)), any(InputStream.class), eq(filename));
        }
        finally {
            response.close();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testStoreBackupWithInvalidContentMD5() throws IOException {
        final String filename = "testfile";
        doThrow(new InvalidMD5Exception("expected", "actual")).when(processor).store(any(BackupMetadata.class), any(Optional.class), any(InputStream.class), eq(filename));

        final BackupMetadata backup = new BackupMetadata(SERVICE_NAME, SOURCE_ADDRESS, LOCAL_NODE);
        when(backupStorage.get(eq(SERVICE_NAME),eq(backup.getId()))).thenReturn(Optional.of(backup));

        final String path = String.format(API_SERVICE_PATH + "/%s/%s", backup.getId(), filename);
        final String contentMD5 = "test";

        final ClientResponse response = resources.client()
            .resource(path)
            .header(HttpHeaders.CONTENT_MD5, contentMD5)
            .header(HttpHeaders.AUTHORIZATION, TOKEN_AUTH_HEADER)
            .put(ClientResponse.class, "hello world");
        try {
            assertEquals(ClientResponse.Status.BAD_REQUEST, response.getStatusInfo());
        }
        finally {
            response.close();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testStoreUnknownBackup() throws IOException {
        final String id = UUID.randomUUID().toString();
        final String filename = "testfile";

        when(backupStorage.get(eq(SERVICE_NAME),anyString())).thenReturn(Optional.<BackupMetadata>absent());

        doThrow(new MetadataNotFoundException(SERVICE_NAME, id)).when(processor).store(any(BackupMetadata.class), any(Optional.class), any(InputStream.class), eq(filename));

        final String path = String.format(API_SERVICE_PATH + "/%s/%s", id, filename);

        final ClientResponse response = resources.client()
            .resource(path)
            .header(HttpHeaders.AUTHORIZATION, TOKEN_AUTH_HEADER)
            .put(ClientResponse.class, "hello world");
        try {
            assertEquals(ClientResponse.Status.NOT_FOUND, response.getStatusInfo());
        }
        finally {
            response.close();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testStoreFinishedBackup() throws IOException {
        final String filename = "testfile";

        final BackupMetadata backup = new BackupMetadata(SERVICE_NAME, SOURCE_ADDRESS, LOCAL_NODE);
        when(backupStorage.get(eq(SERVICE_NAME),eq(backup.getId()))).thenReturn(Optional.of(backup));

        doThrow(new IllegalStateException("test")).when(processor).store(any(BackupMetadata.class), any(Optional.class), any(InputStream.class), eq(filename));

        final String path = String.format(API_SERVICE_PATH + "/%s/%s", backup.getId(), filename);

        final ClientResponse response = resources.client()
            .resource(path)
            .header(HttpHeaders.AUTHORIZATION, TOKEN_AUTH_HEADER)
            .put(ClientResponse.class, "hello world");
        try {
            assertEquals(ClientResponse.Status.BAD_REQUEST, response.getStatusInfo());
        }
        finally {
            response.close();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testStoreMultipleSameFiles() throws IOException {
        final String filename = "testfile";

        final BackupMetadata backup = new BackupMetadata(SERVICE_NAME, SOURCE_ADDRESS, LOCAL_NODE);
        when(backupStorage.get(eq(SERVICE_NAME),eq(backup.getId()))).thenReturn(Optional.of(backup));

        final String path = String.format(API_SERVICE_PATH + "/%s/%s", backup.getId(), filename);

        final ClientResponse response1 = resources.client()
            .resource(path)
            .header(HttpHeaders.AUTHORIZATION, TOKEN_AUTH_HEADER)
            .put(ClientResponse.class, "hello world");
        try {
            assertEquals(ClientResponse.Status.NO_CONTENT, response1.getStatusInfo());
        }
        finally {
            response1.close();
        }

        doThrow(new FileAlreadyExistsException("test")).when(processor).store(any(BackupMetadata.class), any(Optional.class), any(InputStream.class), eq(filename));

        final ClientResponse response2 = resources.client()
            .resource(path)
            .header(HttpHeaders.AUTHORIZATION, TOKEN_AUTH_HEADER)
            .put(ClientResponse.class, "hello world");
        try {
            assertEquals(ClientResponse.Status.BAD_REQUEST, response2.getStatusInfo());
        }
        finally {
            response2.close();
        }
    }

    @Test
    public void testFinishBackupWithoutSpecifiedSuccess() throws IOException {
        final BackupMetadata backup = new BackupMetadata(SERVICE_NAME,SOURCE_ADDRESS,LOCAL_NODE);
        final String path = String.format(API_SERVICE_PATH + "/%s/finish", backup.getId());

        when(backupStorage.get(eq(SERVICE_NAME), eq(backup.getId()))).thenReturn(Optional.of(backup));

        final ClientResponse response = resources.client()
            .resource(path)
            .header(HttpHeaders.AUTHORIZATION, TOKEN_AUTH_HEADER)
            .post(ClientResponse.class, "hello world");
        try {
            assertEquals(ClientResponse.Status.NO_CONTENT, response.getStatusInfo());
            verify(processor, times(1)).finish(any(BackupMetadata.class), eq("hello world"), eq(true));
        }
        finally {
            response.close();
        }
    }

    @Test
    public void testFinishSuccessfulBackup() throws IOException {
        final BackupMetadata backup = new BackupMetadata(SERVICE_NAME, SOURCE_ADDRESS, LOCAL_NODE);
        when(backupStorage.get(eq(SERVICE_NAME),eq(backup.getId()))).thenReturn(Optional.of(backup));

        final String path = String.format(API_SERVICE_PATH + "/%s/finish", backup.getId());

        final ClientResponse response = resources.client()
            .resource(path)
            .queryParam("success", "true")
            .header(HttpHeaders.AUTHORIZATION, TOKEN_AUTH_HEADER)
            .post(ClientResponse.class, "hello world");
        try {
            assertEquals(ClientResponse.Status.NO_CONTENT, response.getStatusInfo());
            verify(processor, times(1)).finish(any(BackupMetadata.class), eq("hello world"), eq(true));
        }
        finally {
            response.close();
        }
    }

    @Test
    public void testFinishFailedBackup() throws IOException {
        final BackupMetadata backup = new BackupMetadata(SERVICE_NAME,SOURCE_ADDRESS,LOCAL_NODE);

        when(backupStorage.get(eq(SERVICE_NAME),eq(backup.getId()))).thenReturn(
                Optional.of(backup));

        final String path = String.format(API_SERVICE_PATH + "/%s/finish", backup.getId());

        final ClientResponse response = resources.client()
            .resource(path)
            .queryParam("success", "false")
            .header(HttpHeaders.AUTHORIZATION, TOKEN_AUTH_HEADER)
            .post(ClientResponse.class, "hello world");
        try {
            assertEquals(ClientResponse.Status.NO_CONTENT, response.getStatusInfo());
            verify(processor, times(1)).finish(any(BackupMetadata.class), eq("hello world"), eq(false));
        }
        finally {
            response.close();
        }
    }

    @Test
    public void testFinishFinishedBackup() throws IOException {
        final BackupMetadata backup = new BackupMetadata(SERVICE_NAME,SOURCE_ADDRESS,LOCAL_NODE);
        final String path = String.format(API_SERVICE_PATH + "/%s/finish", backup.getId());

        when(backupStorage.get(eq(SERVICE_NAME),eq(backup.getId()))).thenReturn(Optional.of(backup));

        doThrow(new IllegalStateException()).when(processor).finish(any(BackupMetadata.class), any(String.class), anyBoolean());

        final ClientResponse response = resources.client()
            .resource(path)
            .queryParam("success", "true")
            .header(HttpHeaders.AUTHORIZATION, TOKEN_AUTH_HEADER)
            .post(ClientResponse.class, "hello world");
        try {
            assertEquals(ClientResponse.Status.BAD_REQUEST, response.getStatusInfo());
        }
        finally {
            response.close();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testListBackups() {
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
    public void testDownloadBackup() throws IOException {
        final String filename = "testfile";

        final BackupMetadata backup = new BackupMetadata(SERVICE_NAME, SOURCE_ADDRESS, LOCAL_NODE);
        backup.setState(BackupMetadata.State.RECEIVING, "test");
        backup.addChunk(filename, "test", 100, 80, "hash", LOCAL_NODE, CompressionCodec.SNAPPY);
        backup.setState(BackupMetadata.State.FINISHED, "test");

        when(backupStorage.get(eq(SERVICE_NAME),eq(backup.getId()))).thenReturn(Optional.of(backup));

        final String path = String.format(API_SERVICE_PATH + "/%s/%s", backup.getId(), filename);
        final ClientResponse response = resources.client()
            .resource(path)
            .header(HttpHeaders.AUTHORIZATION, TOKEN_AUTH_HEADER)
            .get(ClientResponse.class);
        try {
            assertEquals(ClientResponse.Status.OK, response.getStatusInfo());
            verify(processor).download(eq(backup), eq(filename), any(OutputStream.class));
        }
        finally {
            response.close();
        }
    }


    @Test
    public void testDownloadPendingBackup() throws IOException {
        final BackupMetadata backup = new BackupMetadata(SERVICE_NAME, SOURCE_ADDRESS, LOCAL_NODE);
        final String filename = "testfile";
        final String path = String.format(API_SERVICE_PATH + "/%s/%s", backup.getId(), filename);

        when(backupStorage.get(eq(SERVICE_NAME),eq(backup.getId()))).thenReturn(Optional.of(backup));

        doThrow(new IllegalStateException("test")).when(processor).download(any(BackupMetadata.class), eq(filename), any(OutputStream.class));


        final ClientResponse response = resources.client()
            .resource(path)
            .header(HttpHeaders.AUTHORIZATION, TOKEN_AUTH_HEADER)
            .get(ClientResponse.class);
        try {
            assertEquals(ClientResponse.Status.BAD_REQUEST, response.getStatusInfo());
        }
        finally {
            response.close();
        }
    }

    @Test
    public void testDownloadEmptyBackup() throws IOException {
        final BackupMetadata backup = new BackupMetadata(SERVICE_NAME, SOURCE_ADDRESS, LOCAL_NODE);
        backup.setState(BackupMetadata.State.FINISHED, "test");
        final String filename = "testfile";

        when(backupStorage.get(eq(SERVICE_NAME),eq(backup.getId()))).thenReturn(Optional.of(backup));

        final String path = String.format(API_SERVICE_PATH + "/%s/%s", backup.getId(), filename);

        final ClientResponse response = resources.client()
            .resource(path)
            .header(HttpHeaders.AUTHORIZATION, TOKEN_AUTH_HEADER)
            .get(ClientResponse.class);
        try {
            assertEquals(ClientResponse.Status.NOT_FOUND, response.getStatusInfo());
        }
        finally {
            response.close();
        }
    }

    @Test
    public void testGetBackup() throws MetadataNotFoundException {

        final BackupMetadata backup = new BackupMetadata(SERVICE_NAME, SOURCE_ADDRESS, LOCAL_NODE);
        when(backupStorage.get(eq(SERVICE_NAME),eq(backup.getId()))).thenReturn(Optional.of(backup));

        final String path = String.format(API_SERVICE_PATH + "/%s", backup.getId());

        final ClientResponse response = resources.client().resource(path).get(ClientResponse.class);
        try {
            assertEquals(ClientResponse.Status.OK, response.getStatusInfo());
            verify(backupStorage).get(eq(backup.getService()),eq(backup.getId()));
        }
        finally {
            response.close();
        }
    }

    @Test
    public void testGetUnknownBackup() throws IOException {
        final String id = UUID.randomUUID().toString();
        final String path = String.format(API_SERVICE_PATH + "/%s", id);

        when(backupStorage.get(eq(SERVICE_NAME),anyString())).thenReturn(Optional.<BackupMetadata>absent());

        final ClientResponse response = resources.client().resource(path).get(ClientResponse.class);
        try {
            assertEquals(ClientResponse.Status.NOT_FOUND, response.getStatusInfo());
        }
        finally {
            response.close();
        }
    }

    @Test
    public void testDeleteBackup() throws IOException {

        final BackupMetadata backup = new BackupMetadata(SERVICE_NAME, SOURCE_ADDRESS, LOCAL_NODE);
        when(backupStorage.get(eq(SERVICE_NAME),eq(backup.getId()))).thenReturn(Optional.of(backup));

        final String path = String.format(API_SERVICE_PATH + "/%s", backup.getId());

        final ClientResponse response = resources.client()
            .resource(path)
            .header(HttpHeaders.AUTHORIZATION, TOKEN_AUTH_HEADER)
            .delete(ClientResponse.class);
        try {
            assertEquals(ClientResponse.Status.NO_CONTENT, response.getStatusInfo());
            verify(processor, times(1)).delete(any(BackupMetadata.class));
        }
        finally {
            response.close();
        }
    }

    // Authorization related tests
    @Test
    public void testNoHeaderCreateBackup() {
        final ClientResponse response = resources.client()
            .resource(API_SERVICE_PATH)
            .post(ClientResponse.class);
        try {
            assertEquals(ClientResponse.Status.UNAUTHORIZED, response.getStatusInfo());
        }
        finally {
            response.close();
        }
    }

    @Test
    public void testNoHeaderDeleteBackup() {
        final String id = UUID.randomUUID().toString();
        final String path = String.format(API_SERVICE_PATH + "/%s", id);
        final ClientResponse response = resources.client()
            .resource(path)
            .delete(ClientResponse.class);
        try {
            assertEquals(ClientResponse.Status.UNAUTHORIZED, response.getStatusInfo());
        }
        finally {
            response.close();
        }
    }

    @Test
    public void testNoHeaderFinishBackup() {
        final String id = UUID.randomUUID().toString();
        final String path = String.format(API_SERVICE_PATH + "/%s/finish", id);
        final ClientResponse response = resources.client()
            .resource(path)
            .queryParam("success", "false")
            .post(ClientResponse.class, "hello world");
        try {
            assertEquals(ClientResponse.Status.UNAUTHORIZED, response.getStatusInfo());
        }
        finally {
            response.close();
        }
    }


    @Test
    public void testNoHeaderStoreBackup() {
        final String id = UUID.randomUUID().toString();
        final String filename = "testfile";
        final String path = String.format(API_SERVICE_PATH+"/%s/%s", id, filename);

        final ClientResponse response = resources.client()
            .resource(path)
            .put(ClientResponse.class, "hello world");
        try {
            assertEquals(ClientResponse.Status.UNAUTHORIZED, response.getStatusInfo());
        }
        finally {
            response.close();
        }
    }

    @Test
    public void testNoHeaderDownloadBackup() {
        final String filename = "testfile";
        final String path = String.format(API_SERVICE_PATH+"/1234/%s", filename);

        final ClientResponse response = resources.client()
            .resource(path)
            .get(ClientResponse.class);
        try {
            assertEquals(ClientResponse.Status.UNAUTHORIZED, response.getStatusInfo());
        }
        finally {
            response.close();
        }
    }


    @Test
    public void testTooShortAuthorizationHeader() {
        final String shortHeader = Strings.padEnd("*", TokenCredentials.MIN_TOKEN_LENGTH - 1 , '*');
        final ClientResponse response = resources.client()
            .resource(API_SERVICE_PATH)
            .header(HttpHeaders.AUTHORIZATION, shortHeader)
            .post(ClientResponse.class);
        try {
            assertEquals(ClientResponse.Status.BAD_REQUEST, response.getStatusInfo());
        }
        finally {
            response.close();
        }
    }

    @Test
    public void testUnauthorizedCreateBackup() {
        when(processor.create(anyString(), anyString())).thenAnswer(new Answer<BackupMetadata>() {
            @Override
            public BackupMetadata answer(InvocationOnMock invocation) throws Throwable {
                final String serviceName = (String) invocation.getArguments()[0];
                final String remoteAddr = (String) invocation.getArguments()[1];
                return new BackupMetadata(serviceName, remoteAddr, LOCAL_NODE);
            }
        });
        when(clientPermissionStorage.listAll(eq(SERVICE_NAME)))
            .thenReturn(ImmutableSet.<ClientPermission>of())
            .thenReturn(ImmutableSet.of(new ClientPermission(UNKNOWN_TOKEN,SERVICE_NAME)));

        when(clientPermissionStorage.get(eq(SERVICE_NAME), eq(TOKEN)))
            .thenReturn(Optional.<ClientPermission>absent());

        when(clientPermissionStorage.get(eq(SERVICE_NAME), eq(UNKNOWN_TOKEN)))
            .thenReturn(Optional.<ClientPermission>absent())
            .thenReturn(Optional.of(new ClientPermission(UNKNOWN_TOKEN,SERVICE_NAME)));


        // The first backup must be allowed and register the token
        final ClientResponse response = resources.client()
            .resource(API_SERVICE_PATH)
            .header(HttpHeaders.AUTHORIZATION, UNKNOWN_TOKEN_AUTH_HEADER)
            .post(ClientResponse.class);
        try {
            verify(clientPermissionStorage, times(1)).put(eq(new ClientPermission(UNKNOWN_TOKEN,SERVICE_NAME)));
            assertEquals(ClientResponse.Status.OK, response.getStatusInfo());
        }
        finally {
            response.close();
        }

        // A second backup for the same service with a different token must fail.
        final ClientResponse secondResponse = resources.client()
            .resource(API_SERVICE_PATH)
            .header(HttpHeaders.AUTHORIZATION, TOKEN_AUTH_HEADER)
            .post(ClientResponse.class);
        try {
            assertEquals(ClientResponse.Status.UNAUTHORIZED, secondResponse.getStatusInfo());
        }
        finally {
            secondResponse.close();
        }

        // A third request with the original token must succeed
        final ClientResponse thirdResponse = resources.client()
            .resource(API_SERVICE_PATH)
            .header(HttpHeaders.AUTHORIZATION, UNKNOWN_TOKEN_AUTH_HEADER)
            .post(ClientResponse.class);
        try {
            assertEquals(ClientResponse.Status.OK, thirdResponse.getStatusInfo());
        }
        finally {
            thirdResponse.close();
        }

    }

    @Test
    public void testUnauthorizedDeleteBackup(){
        final String id = UUID.randomUUID().toString();
        final String path = String.format(API_SERVICE_PATH + "/%s", id);
        final ClientResponse response = resources.client()
            .resource(path)
            .header(HttpHeaders.AUTHORIZATION, UNKNOWN_TOKEN_AUTH_HEADER)
            .delete(ClientResponse.class);
        try {
            assertEquals(ClientResponse.Status.UNAUTHORIZED, response.getStatusInfo());
        }
        finally {
            response.close();
        }
    }

    @Test
    public void testUnauthorizedFinishBackup() {
        final String id = UUID.randomUUID().toString();
        final String path = String.format(API_SERVICE_PATH + "/%s/finish", id);
        final ClientResponse response = resources.client()
            .resource(path)
            .queryParam("success", "false")
            .header(HttpHeaders.AUTHORIZATION, UNKNOWN_TOKEN_AUTH_HEADER)
            .post(ClientResponse.class, "hello world");
        try {
            assertEquals(ClientResponse.Status.UNAUTHORIZED, response.getStatusInfo());
        }
        finally {
            response.close();
        }
    }


    @Test
    public void testUnauthorizedStoreBackup() {
        final String id = UUID.randomUUID().toString();
        final String filename = "testfile";
        final String path = String.format(API_SERVICE_PATH+"/%s/%s", id, filename);

        final ClientResponse response = resources.client()
            .resource(path)
            .header(HttpHeaders.AUTHORIZATION, UNKNOWN_TOKEN_AUTH_HEADER)
            .put(ClientResponse.class, "hello world");
        try {
            assertEquals(ClientResponse.Status.UNAUTHORIZED, response.getStatusInfo());
        }
        finally {
            response.close();
        }
    }

    @Test
    public void testUnauthorizedDownloadBackup() {
        final String filename = "testfile";
        final String path = String.format(API_SERVICE_PATH+"/1234/%s", filename);

        final ClientResponse response = resources.client()
            .resource(path)
            .header(HttpHeaders.AUTHORIZATION, UNKNOWN_TOKEN_AUTH_HEADER)
            .get(ClientResponse.class);
        try {
            assertEquals(ClientResponse.Status.UNAUTHORIZED, response.getStatusInfo());
        }
        finally {
            response.close();
        }
    }

    @Test
    public void testDownloadBackupOtherNode() throws IOException {
        final String filename = "testfile";

        final BackupMetadata backup = new BackupMetadata(SERVICE_NAME, SOURCE_ADDRESS, OTHER_NODE);
        backup.setState(BackupMetadata.State.RECEIVING, "test");
        backup.addChunk(filename, "test", 100, 80, "hash", LOCAL_NODE, CompressionCodec.SNAPPY);
        backup.setState(BackupMetadata.State.FINISHED, "test");

        when(backupStorage.get(eq(SERVICE_NAME),eq(backup.getId()))).thenReturn(Optional.of(backup));

        final String path = String.format(API_SERVICE_PATH + "/%s/%s", backup.getId(), filename);
        final ClientResponse response = resources.client()
            .resource(path)
            .header(HttpHeaders.AUTHORIZATION, TOKEN_AUTH_HEADER)
            .get(ClientResponse.class);
        try {
            assertEquals(ClientResponse.Status.OK, response.getStatusInfo());
            verify(processor).download(eq(backup), eq(filename), any(OutputStream.class));
        }
        finally {
            response.close();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testStoreBackupOtherNode() throws IOException {
        final String filename = "testfile";

        final BackupMetadata backup = new BackupMetadata(SERVICE_NAME, SOURCE_ADDRESS, OTHER_NODE);
        when(backupStorage.get(eq(SERVICE_NAME),eq(backup.getId()))).thenReturn(Optional.of(backup));

        final String path = String.format(API_SERVICE_PATH+"/%s/%s", backup.getId(), filename);

        final ClientResponse response = resources.client()
            .resource(path)
            .header(HttpHeaders.AUTHORIZATION, TOKEN_AUTH_HEADER)
            .put(ClientResponse.class, "hello world");
        try {
            assertEquals(ClientResponse.Status.TEMPORARY_REDIRECT,response.getStatusInfo());
            verify(processor, times(0)).store(any(BackupMetadata.class), any(Optional.class), any(InputStream.class), eq(filename));
        }
        finally {
            response.close();
        }
    }

    @Test
    public void testFinishSuccessfulBackupOtherNode() throws IOException {
        final BackupMetadata backup = new BackupMetadata(SERVICE_NAME, SOURCE_ADDRESS, OTHER_NODE);
        when(backupStorage.get(eq(SERVICE_NAME),eq(backup.getId()))).thenReturn(Optional.of(backup));

        final String path = String.format(API_SERVICE_PATH + "/%s/finish", backup.getId());

        final ClientResponse response = resources.client()
            .resource(path)
            .queryParam("success", "true")
            .header(HttpHeaders.AUTHORIZATION, TOKEN_AUTH_HEADER)
            .post(ClientResponse.class, "hello world");
        try {
            assertEquals(ClientResponse.Status.TEMPORARY_REDIRECT, response.getStatusInfo());
            verify(processor, times(0)).finish(any(BackupMetadata.class), eq("hello world"), eq(true));
        }
        finally {
            response.close();
        }
    }

    @Test
    public void testDeleteBackupOtherNode() throws IOException {
        final BackupMetadata backup = new BackupMetadata(SERVICE_NAME, SOURCE_ADDRESS, OTHER_NODE);
        when(backupStorage.get(eq(SERVICE_NAME),eq(backup.getId()))).thenReturn(Optional.of(backup));

        final String path = String.format(API_SERVICE_PATH + "/%s", backup.getId());

        final ClientResponse response = resources.client()
            .resource(path)
            .header(HttpHeaders.AUTHORIZATION, TOKEN_AUTH_HEADER)
            .delete(ClientResponse.class);
        try {
            assertEquals(ClientResponse.Status.TEMPORARY_REDIRECT, response.getStatusInfo());
            verify(processor, times(0)).delete(any(BackupMetadata.class));
        }
        finally {
            response.close();
        }
    }





}

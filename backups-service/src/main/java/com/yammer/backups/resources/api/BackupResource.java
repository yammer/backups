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
import com.google.common.io.ByteStreams;
import com.google.common.net.HttpHeaders;
import com.sun.jersey.core.header.ContentDisposition;
import com.yammer.backups.api.Chunk;
import com.yammer.backups.api.ClientPermission;
import com.yammer.backups.api.metadata.BackupMetadata;
import com.yammer.backups.auth.TokenAuthProvider;
import com.yammer.backups.auth.TokenCredentials;
import com.yammer.backups.error.IncorrectNodeException;
import com.yammer.backups.error.MetadataNotFoundException;
import com.yammer.backups.processor.BackupProcessor;
import com.yammer.backups.provider.Metadata;
import com.yammer.backups.storage.metadata.MetadataStorage;
import io.dropwizard.auth.Auth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Path("/api/backup")
public class BackupResource {

    private static final Logger LOG = LoggerFactory.getLogger(BackupResource.class);

    private final BackupProcessor backupProcessor;
    private final MetadataStorage<ClientPermission> clientPermissionStorage;
    private final String nodeName;

    public BackupResource(
            BackupProcessor backupProcessor,
            MetadataStorage<ClientPermission> clientPermissionStorage,
            String nodeName) {
        this.backupProcessor = backupProcessor;
        this.clientPermissionStorage = clientPermissionStorage;
        this.nodeName = nodeName;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<BackupMetadata> get(
            @QueryParam("state") final BackupMetadata.State state) {
        return backupProcessor.listMetadata(Optional.fromNullable(state));
    }

    @POST
    @Path("/{service}")
    @Produces(MediaType.APPLICATION_JSON)
    public String create(
            @Auth(required = false) ClientPermission clientPermission,
            @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @SuppressWarnings("TypeMayBeWeakened") @Context HttpServletRequest req,
            @PathParam("service") final String service) {

        // We always need an authorization header
        if (authorization == null) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }

        // The authroization header must be always long enough.
        final String token = authorization.replace(TokenAuthProvider.SCHEMA, "").trim();
        if (token.length() < TokenCredentials.MIN_TOKEN_LENGTH) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        // Auto-register on first backup
        if (clientPermission == null) {
            // If there are already any tokens for this service, then we don't auto-register because
            // there is already a token for this service

            final Set<ClientPermission> servicePermittedKeys = clientPermissionStorage.listAll(service);
            if (!servicePermittedKeys.isEmpty()) {
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }

            clientPermissionStorage.put(new ClientPermission(token,service));
        }

        final BackupMetadata backup = backupProcessor.create(service, req.getRemoteAddr());
        return backup.getId();
    }

    @PUT
    @Path("/{service}/{id}/{filename}")
    public void upload(
            @Auth ClientPermission clientPermission,
            @HeaderParam(HttpHeaders.CONTENT_MD5) final String contentMD5,
            @Metadata BackupMetadata backup,
            @PathParam("filename") final String filename,
            final InputStream in) throws IOException, IncorrectNodeException {

        if (!backup.isAtNode(nodeName)) {
            throw new IncorrectNodeException(nodeName, backup.getNodeName());
        }

        try {
            LOG.trace("Receiving payload for {}", backup);
            backupProcessor.store(backup, Optional.fromNullable(contentMD5), in, filename);
        }
        catch (FileAlreadyExistsException e) {
            LOG.info("Attempted to overwrite existing file for backup " + backup, e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }
        finally {
            in.close();
        }
    }

    @POST
    @Path("/{service}/{id}/finish")
    public void finish(
            @Auth ClientPermission clientPermission,
            @Metadata BackupMetadata backup,
            @QueryParam("success") @DefaultValue("true") final boolean success,
            final String log) throws IOException, IncorrectNodeException {

        if(!backup.isAtNode(nodeName)) {
            throw new IncorrectNodeException(nodeName, backup.getNodeName());
        }
        backupProcessor.finish(backup, log, success);
        LOG.trace("Backup {}/{} completed with success={}", backup.getService(), backup.getId(), success);
    }

    @GET
    @Path("/{service}")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<BackupMetadata> list(
            @PathParam("service") final String service,
            @QueryParam("state") final BackupMetadata.State state) {
        return backupProcessor.listMetadata(service, Optional.fromNullable(state));
    }

    @DELETE
    @Path("/{service}/{id}")
    public void delete(
            @Auth ClientPermission clientPermission,
            @SuppressWarnings("TypeMayBeWeakened") @Context HttpServletRequest req,
            @Metadata BackupMetadata backup) throws IOException, IncorrectNodeException {
        if (!backup.isAtNode(nodeName)) {
            throw new IncorrectNodeException(nodeName, backup.getNodeName());
        }

        LOG.info("Deleting backup {}, requested by {}", backup, req.getRemoteAddr());

        backupProcessor.delete(backup);
    }

    @GET
    @Path("/{service}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public BackupMetadata get(
            @Metadata BackupMetadata backup) throws MetadataNotFoundException {
        return backup;
    }

    @GET
    @Path("/logs/{service}/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public StreamingOutput getLog(
            @Metadata final BackupMetadata backup) {
        return new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException {
                try (final InputStream in = backupProcessor.getLog(backup)) {
                    ByteStreams.copy(in, output);
                }
            }
        };
    }

    @GET
    @Path("/{service}/{id}/{filename}")
    public Response download(
            @Auth ClientPermission clientPermission,
            @SuppressWarnings("TypeMayBeWeakened") @Context HttpServletRequest req,
            @Metadata final BackupMetadata backup,
            @PathParam("filename") final String filename) throws MetadataNotFoundException {

        if (!(backup.isSuccessful() || backup.isFailed())) {
            throw new IllegalStateException("Attempted to download a non-complete backup.");
        }

        final List<Chunk> chunks = backup.getChunks(filename);
        if (chunks.isEmpty()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        LOG.info("SecEvent: Downloading backup {}, requested by {}", backup, req.getRemoteAddr());

        long size = 0;
        for (Chunk chunk : chunks) {
            size += chunk.getOriginalSize();
        }

        final ContentDisposition disposition = ContentDisposition
                .type("attachment")
                .fileName(filename)
                .build();

        final Response.ResponseBuilder builder = Response
                .status(Response.Status.OK)
                .type(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .entity(new StreamingOutput() {
                    @Override
                    public void write(OutputStream output) throws IOException {
                        try {
                            backupProcessor.download(backup, filename, output);
                        }
                        finally {
                            output.close();
                        }
                    }
                });

        // Only add a content length if we have one
        if (size > 0) {
            builder.header(HttpHeaders.CONTENT_LENGTH, size);
        }

        return builder.build();
    }
}

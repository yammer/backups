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
import com.yammer.backups.api.metadata.VerificationMetadata;
import com.yammer.backups.error.MetadataNotFoundException;
import com.yammer.backups.processor.VerificationProcessor;
import com.yammer.backups.provider.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

@Path("/api/verification")
public class VerificationResource {

    private static final Logger LOG = LoggerFactory.getLogger(VerificationResource.class);

    private final VerificationProcessor verificationProcessor;

    public VerificationResource(VerificationProcessor verificationProcessor) {
        this.verificationProcessor = verificationProcessor;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<VerificationMetadata> get(
            @QueryParam("state") final VerificationMetadata.State state) {
        return verificationProcessor.listMetadata(Optional.fromNullable(state));
    }

    @POST
    @Path("/{service}")
    @Produces(MediaType.APPLICATION_JSON)
    public String start(
            @SuppressWarnings("TypeMayBeWeakened") @Context HttpServletRequest req,
            @PathParam("service") String service,
            @QueryParam("backupId") String backupId) throws MetadataNotFoundException {

        return verificationProcessor.create(service, backupId, req.getRemoteAddr()).getId();
    }

    @POST
    @Path("/{service}/{id}/finish")
    public void finish(
            @Metadata VerificationMetadata verification,
            @QueryParam("success") @DefaultValue("true") final boolean success,
            final String log) throws MetadataNotFoundException {
        verificationProcessor.finish(verification, log, success);
        LOG.trace("Verification {}/{} completed with success={}", verification.getService(), verification.getId(), success);
    }

    @GET
    @Path("/{service}")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<VerificationMetadata> list(
            @PathParam("service") final String service,
            @QueryParam("state") final VerificationMetadata.State state) {
        return verificationProcessor.listMetadata(service, Optional.fromNullable(state));
    }

    @DELETE
    @Path("/{service}/{id}")
    public void delete(
            @Metadata VerificationMetadata verification) throws IOException {
        verificationProcessor.delete(verification);
    }

    @GET
    @Path("/{service}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public VerificationMetadata get(
            @Metadata VerificationMetadata verification) throws MetadataNotFoundException {
        return verification;
    }

    @GET
    @Path("/logs/{service}/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public StreamingOutput getLog(
            @Metadata final VerificationMetadata verification) {
        return new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException {
                try (final InputStream in = verificationProcessor.getLog(verification)) {
                    ByteStreams.copy(in, output);
                }
            }
        };
    }
}

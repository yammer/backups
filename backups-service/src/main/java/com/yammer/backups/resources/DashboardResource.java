package com.yammer.backups.resources;

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
import com.yammer.backups.auth.TemporaryTokenGenerator;
import com.yammer.backups.provider.Metadata;
import com.yammer.backups.views.DetailView;
import com.yammer.backups.views.DownloadView;
import com.yammer.backups.views.HistoryView;
import com.yammer.backups.views.StatusView;
import io.dropwizard.auth.Auth;
import io.dropwizard.auth.basic.BasicCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;

@Path("/")
public class DashboardResource {

    private static final Logger LOG = LoggerFactory.getLogger(DashboardResource.class);
    private static final URI STATUS_URI = URI.create("/status");

    private final TemporaryTokenGenerator tokenGenerator;

    public DashboardResource(TemporaryTokenGenerator tokenGenerator) {
        this.tokenGenerator = tokenGenerator;
    }

    @GET
    @Path("/")
    public Response getIndex() {
        return Response
                .temporaryRedirect(STATUS_URI)
                .build();
    }

    @GET
    @Path("/status")
    @Produces(MediaType.TEXT_HTML)
    public StatusView getStatus() {
        return new StatusView();
    }

    @GET
    @Path("/history")
    @Produces(MediaType.TEXT_HTML)
    public HistoryView getHistory() {
        return new HistoryView();
    }

    @GET
    @Path("/detail/{service}/{id}")
    @Produces(MediaType.TEXT_HTML)
    public DetailView getDetail(
            @Metadata BackupMetadata backup) {
        return new DetailView(backup);
    }

    @GET
    @Path("/download/{service}/{id}/{filename}")
    @Produces(MediaType.TEXT_PLAIN)
    public DownloadView downloadBackup(
            @Context UriInfo context,
            @Auth BasicCredentials credentials,
            @PathParam("service") String service,
            @PathParam("id") String id,
            @PathParam("filename") String filename) throws URISyntaxException {

        final String temporaryToken = tokenGenerator.create(service);

        LOG.info("SecEvent: Created temporary token for {} to download {} backups", credentials.getUsername(), service);

        final URI requestURI = context.getRequestUri();
        final String root = new URI(requestURI.getScheme(), null, requestURI.getHost(), requestURI.getPort(), null, null, null).toString();

        return new DownloadView(root, service, id, filename, temporaryToken);
    }
}

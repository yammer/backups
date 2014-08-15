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
import io.dropwizard.auth.Auth;
import io.dropwizard.auth.basic.BasicCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;

@Path("/api/services")
public class ServicesResource {
    private static final Logger LOG = LoggerFactory.getLogger(ServicesResource.class);

    private final ServiceRegistry serviceRegistry;

    public ServicesResource(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<ServiceMetadata> get() {
        return serviceRegistry.listAll();
    }

    @PUT
    @Path("/{service}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void put(
            @Auth BasicCredentials user,
            @PathParam("service") final String service,
            ServiceMetadata serviceMetadata) {
        if (service.equals(serviceMetadata.getId())) {
            if (serviceRegistry.updateServiceIfPresent(serviceMetadata)) {
                LOG.info("Service settings updated by " + user.getUsername());
            } else {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
        } else {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }
}

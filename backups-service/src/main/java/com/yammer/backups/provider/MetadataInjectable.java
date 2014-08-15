package com.yammer.backups.provider;

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
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.server.impl.inject.AbstractHttpContextInjectable;
import com.yammer.backups.api.metadata.AbstractMetadata;
import com.yammer.backups.storage.metadata.MetadataStorage;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

public class MetadataInjectable<T extends AbstractMetadata<?>> extends AbstractHttpContextInjectable<T> {

    private static final String LATEST_ID = "latest";

    private final MetadataStorage<T> storage;
    private final String serviceField;
    private final String idField;

    public MetadataInjectable(MetadataStorage<T> storage, String serviceField, String idField) {
        this.storage = storage;
        this.serviceField = serviceField;
        this.idField = idField;
    }

    @Override
    public T getValue(HttpContext context) {
        // Get the service
        PathSegment serviceSegment = null;
        if (context.getUriInfo().getPathSegments(serviceField).size() > 0) {
            serviceSegment = context.getUriInfo().getPathSegments(serviceField).get(0);
        }

        // Get the id
        PathSegment idSegment = null;
        if (context.getUriInfo().getPathSegments(idField).size() > 0) {
            idSegment = context.getUriInfo().getPathSegments(idField).get(0);
        }

        // Ensure we have service and id
        if (idSegment == null || serviceSegment == null) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        final String service = serviceSegment.getPath();
        final String id = idSegment.getPath();

        final Optional<T> metadata;
        if (LATEST_ID.equals(id)) {
            metadata = getLatestValue(service);
        }
        else {
            metadata = storage.get(service, id);
        }

        if (!metadata.isPresent()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        return metadata.get();
    }

    protected Optional<T> getLatestValue(String service) {
        final List<T> metadataList = Lists.newArrayList(Sets.filter(storage.listAll(service), new Predicate<T>() {
            @Override
            public boolean apply(T input) {
                return input.isFinished();
            }
        }));

        if (!metadataList.isEmpty()) {
            Collections.sort(metadataList, AbstractMetadata.COMPARATOR);

            final T metadata = metadataList.get(0);
            return Optional.of(metadata);
        }

        return Optional.absent();
    }
}

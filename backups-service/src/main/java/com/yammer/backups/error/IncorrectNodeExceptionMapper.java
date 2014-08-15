package com.yammer.backups.error;

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
import com.yammer.backups.api.Node;
import com.yammer.backups.storage.metadata.MetadataStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.net.URI;
import java.net.URISyntaxException;

public class IncorrectNodeExceptionMapper implements ExceptionMapper<IncorrectNodeException>{

    private static final Logger LOG = LoggerFactory.getLogger(IncorrectNodeExceptionMapper.class);

    private final MetadataStorage<Node> nodeStorage;

    public IncorrectNodeExceptionMapper(MetadataStorage<Node> nodeStorage) {
        this.nodeStorage = nodeStorage;
    }

    @Override
    public Response toResponse(IncorrectNodeException exception) {
        final Optional<Node> node = nodeStorage.get(exception.getCorrectNode(), exception.getCorrectNode());
        if (!node.isPresent()) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }

        try {
            final URI location = node.get().getUrl().toURI();
            return Response.temporaryRedirect(location).build();
        }
        catch (URISyntaxException e) {
            LOG.warn("Failed to redirect to correct node", e);
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}

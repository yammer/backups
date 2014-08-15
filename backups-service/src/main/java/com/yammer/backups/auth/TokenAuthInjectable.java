package com.yammer.backups.auth;

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
import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.core.ExtendedUriInfo;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.api.core.HttpRequestContext;
import com.sun.jersey.server.impl.inject.AbstractHttpContextInjectable;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import java.util.List;

public class TokenAuthInjectable<T> extends AbstractHttpContextInjectable<T> {

    private static final Logger LOG = LoggerFactory.getLogger(TokenAuthInjectable.class);
    private static final String SERVICE_PATH_SEGMENT = "service";

    private final Authenticator<TokenCredentials, T> authenticator;
    private final boolean required;

    public TokenAuthInjectable(Authenticator<TokenCredentials, T> authenticator, boolean required) {
        this.authenticator = authenticator;
        this.required = required;
    }

    private Optional<String> getServiceFromPath(ExtendedUriInfo uriInfo) {
        final List<PathSegment> segments = uriInfo.getPathSegments(SERVICE_PATH_SEGMENT);
        if (segments.isEmpty()) {
            return Optional.absent();
        }

        return Optional.fromNullable(segments.get(0).getPath());
    }

    private Optional<String> getToken(HttpRequestContext request) {
        // Token in an Authorization header
        final String header = request.getHeaderValue(HttpHeaders.AUTHORIZATION);
        if (!Strings.isNullOrEmpty(header) && header.startsWith(TokenAuthProvider.SCHEMA)) {
            return Optional.of(header.replace(TokenAuthProvider.SCHEMA, "").trim());
        }

        // Token in URL
        final MultivaluedMap<String, String> query = request.getQueryParameters();
        final Optional<String> queryToken = Optional.fromNullable(query.getFirst("token"));
        if (queryToken.isPresent()) {
            return queryToken;
        }

        // No token provided
        return Optional.absent();
    }

    private Optional<T> authenticateToken(String service, String token, boolean readRequest) {
        try {
            return authenticator.authenticate(new TokenCredentials(token, service, readRequest));
        }
        catch (IllegalArgumentException e) {
            LOG.debug("Error decoding credentials", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }
        catch (AuthenticationException e) {
            LOG.warn("Error authenticating credentials", e);
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public T getValue(HttpContext context) {
        final Optional<String> service = getServiceFromPath(context.getUriInfo());
        if (!service.isPresent()) {
            return null;
        }

        final Optional<String> token = getToken(context.getRequest());
        if (token.isPresent()) {
            final String method = context.getRequest().getMethod();
            final boolean readRequest = HttpMethod.GET.equals(method);

            final Optional<T> result = authenticateToken(service.get(), token.get(), readRequest);
            if (result.isPresent()) {
                return result.get();
            }
        }

        if (required) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                .entity("Credentials are required to access this resource.")
                .type(MediaType.TEXT_PLAIN_TYPE).build());
        }

        return null;
    }
}

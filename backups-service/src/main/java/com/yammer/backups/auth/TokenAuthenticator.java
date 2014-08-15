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
import com.yammer.backups.api.ClientPermission;
import com.yammer.backups.storage.metadata.MetadataStorage;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;

public class TokenAuthenticator implements Authenticator<TokenCredentials, ClientPermission> {

    private final MetadataStorage<ClientPermission> serviceTokens;
    private final TemporaryTokenGenerator tokenGenerator;

    public TokenAuthenticator(MetadataStorage<ClientPermission> serviceTokens, TemporaryTokenGenerator tokenGenerator) {
        this.serviceTokens = serviceTokens;
        this.tokenGenerator = tokenGenerator;
    }

    private Optional<ClientPermission> getFromTemporaryToken(String service, String token) {
        if (!tokenGenerator.validate(service, token)) {
            return Optional.absent();
        }

        return Optional.of(new ClientPermission(service, token));
    }

    private Optional<ClientPermission> getFromServiceToken(String service, String token) {
        return serviceTokens.get(service, token);
    }

    @Override
    public Optional<ClientPermission> authenticate(TokenCredentials tokenCredential) throws AuthenticationException {
        // Check for a service token that matches
        final Optional<ClientPermission> servicePermission = getFromServiceToken(tokenCredential.getService(), tokenCredential.getToken());
        if (servicePermission.isPresent()) {
            return servicePermission;
        }

        // Check for a temporary token that matches, only if this is a read request
        if (tokenCredential.isReadRequest()) {
            final Optional<ClientPermission> temporaryPermission = getFromTemporaryToken(tokenCredential.getService(), tokenCredential.getToken());
            if (temporaryPermission.isPresent()) {
                return temporaryPermission;
            }
        }

        // No permission
        return Optional.absent();
    }
}

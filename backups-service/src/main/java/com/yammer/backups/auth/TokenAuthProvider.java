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

import com.sun.jersey.api.model.Parameter;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;
import io.dropwizard.auth.Auth;
import io.dropwizard.auth.Authenticator;

public class TokenAuthProvider<T> implements InjectableProvider<Auth, Parameter> {

    public static final String SCHEMA = "Token";

    private final Authenticator<TokenCredentials, T> authenticator;

    public TokenAuthProvider(Authenticator<TokenCredentials, T> authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    public Injectable<?> getInjectable(ComponentContext ic, Auth a, Parameter c) {
        return new TokenAuthInjectable<>(authenticator, a.required());
    }

    @Override
    public ComponentScope getScope() {
        return ComponentScope.PerRequest;
    }

}

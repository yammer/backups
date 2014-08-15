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

import java.lang.reflect.Type;
import java.util.Map;

public class TypedAuthProvider implements InjectableProvider<Auth, Parameter> {

    private final Map<Type, InjectableProvider<Auth, Parameter>> map;

    public TypedAuthProvider(Map<Type, InjectableProvider<Auth, Parameter>> map) {
        this.map = map;
    }

    @Override
    public ComponentScope getScope() {
        return ComponentScope.PerRequest;
    }

    @Override
    public Injectable getInjectable(ComponentContext ic, Auth auth, Parameter parameter) {
        final Type type = parameter.getParameterType();

        final InjectableProvider<Auth, Parameter> delegate = map.get(type);
        if (delegate == null) {
            return null;
        }

        return delegate.getInjectable(ic, auth, parameter);
    }
}

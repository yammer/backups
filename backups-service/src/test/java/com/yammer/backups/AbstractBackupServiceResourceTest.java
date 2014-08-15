package com.yammer.backups;

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

import com.sun.jersey.spi.inject.SingletonTypeInjectableProvider;
import com.yammer.backups.error.IllegalStateExceptionMapper;
import com.yammer.backups.error.InvalidMD5ExceptionMapper;
import com.yammer.backups.error.MetadataNotFoundExceptionMapper;
import com.yammer.backups.error.NoContentExceptionMapper;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.Before;
import org.junit.Rule;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractBackupServiceResourceTest {

    private static final HttpServletRequest REQUEST = mock(HttpServletRequest.class);

    @Rule
    public final ResourceTestRule resources = setUpResources(new ResourceTestRule.Builder()).build();

    protected ResourceTestRule.Builder setUpResources(ResourceTestRule.Builder builder) {
        return builder.addProvider(new SingletonTypeInjectableProvider<Context, HttpServletRequest>(HttpServletRequest.class, REQUEST) {})
                .addProvider(new NoContentExceptionMapper())
                .addProvider(new MetadataNotFoundExceptionMapper())
                .addProvider(new InvalidMD5ExceptionMapper())
                .addProvider(new IllegalStateExceptionMapper());
    }

    @Before
    public void setUp() {
        when(REQUEST.getRemoteAddr()).thenReturn("127.0.0.1");
    }
}

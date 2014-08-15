package com.yammer.backups.api.metadata;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jackson.Jackson;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public abstract class AbstractMetadataTest<T extends AbstractMetadata<?>> {

    private static final ObjectMapper JSON = Jackson.newObjectMapper();

    private final Class<T> type;

    protected T data;

    protected AbstractMetadataTest(Class<T> type) {
        this.type = type;
    }

    protected abstract T createMetadata();

    @Before
    public void setUp() throws MalformedURLException {
        data = this.createMetadata();
    }

    @Test
    public void testSerialize() throws JsonProcessingException {
        data.setCompletedDate(DateTime.now());

        final String result = JSON.writeValueAsString(data);
        assertNotNull(result);
    }

    @Test
    public void testSerializeDeserialize() throws IOException {
        data.setCompletedDate(DateTime.now());

        final String result = JSON.writeValueAsString(data);
        final T deserialized = JSON.readValue(result, type);
        assertEquals(data, deserialized);
    }
}

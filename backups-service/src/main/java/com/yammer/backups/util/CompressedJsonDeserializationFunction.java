package com.yammer.backups.util;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import com.yammer.collections.azure.Bytes;
import com.yammer.io.codec.StreamCodec;
import com.yammer.io.codec.compression.GZIPCompressionCodec;
import io.dropwizard.jackson.Jackson;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CompressedJsonDeserializationFunction<T> implements Function<Bytes, T> {

    private static final StreamCodec CODEC = new GZIPCompressionCodec();
    private static final ObjectMapper JSON = Jackson.newObjectMapper();

    private final Class<T> type;

    public CompressedJsonDeserializationFunction(Class<T> type) {
        this.type = type;
    }

    @Override
    public T apply(Bytes input) {
        try (final InputStream in = CODEC.input(new ByteArrayInputStream(input.getBytes()))) {
            final byte[] deflated = ByteStreams.toByteArray(in);
            return JSON.readValue(deflated, type);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}

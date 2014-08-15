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

import com.yammer.backups.api.CompressionCodec;
import com.yammer.backups.api.metadata.BackupMetadata;
import com.yammer.collections.azure.Bytes;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JsonDeSerializationFunctionTest {

    private static BackupMetadata createBackup(String service) {
        return new BackupMetadata(service, UUID.randomUUID().toString(), "localhost");
    }

    private static class NotJsonSerializable {

        @SuppressWarnings("UnusedParameters")
        private NotJsonSerializable(int value) {
        }
    }

    private CompressedJsonSerializationFunction<BackupMetadata> serializer;
    private CompressedJsonDeserializationFunction<BackupMetadata> deserializer;

    @Before
    public void setUp() {
        serializer = new CompressedJsonSerializationFunction<>();
        deserializer = new CompressedJsonDeserializationFunction<>(BackupMetadata.class);
    }

    @Test
    public void testSerialize() {
        final BackupMetadata backup = createBackup("feedie");
        final Bytes result = serializer.apply(backup);
        assertNotNull(result);
    }

    @Test
    public void testSerializeDeserialize() {
        final BackupMetadata backup = createBackup("feedie");
        backup.setState(BackupMetadata.State.RECEIVING, "test");
        backup.addChunk("filename", "path", 100, 80, "hash", "localhost", CompressionCodec.SNAPPY);

        final Bytes result = serializer.apply(backup);
        final BackupMetadata deserialized = deserializer.apply(result);
        assertEquals(backup, deserialized);
    }

    @Test(expected = NullPointerException.class)
    public void testDeserializeNull() {
        deserializer.apply(null);
    }

    @Test(expected = RuntimeException.class)
    public void testSerializeNotSerializable() {
        new CompressedJsonSerializationFunction<NotJsonSerializable>().apply(new NotJsonSerializable(7));
    }

    @Test(expected = RuntimeException.class)
    public void testDeserializeNotJson() throws UnsupportedEncodingException {
        new CompressedJsonDeserializationFunction<>(NotJsonSerializable.class).apply(Bytes.of("bla".getBytes("UTF-8")));
    }
}

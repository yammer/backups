package com.yammer.backups.codec;

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

import com.google.common.collect.Maps;
import com.yammer.backups.api.CompressionCodec;
import com.yammer.io.codec.CombinedStreamCodec;
import com.yammer.io.codec.StreamCodec;
import com.yammer.io.codec.compression.GZIPCompressionCodec;
import com.yammer.io.codec.compression.SnappyCompressionCodec;
import com.yammer.io.codec.encryption.AESCipherEncryptionCodec;
import com.yammer.io.codec.encryption.AESCipherEncryptionConfiguration;
import com.yammer.io.codec.noop.NullStreamCodec;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.EnumMap;

public class BackupCodecFactory implements CodecFactory {

    private final CompressionCodec defaultCompressionCodec;
    private final EnumMap<CompressionCodec, StreamCodec> compressionCodecs;
    private final StreamCodec encryptionCodec;

    public BackupCodecFactory(AESCipherEncryptionConfiguration encryptionConfiguration, CompressionCodec defaultCompressionCodec) throws InvalidKeySpecException, NoSuchAlgorithmException, UnsupportedEncodingException {
        this.defaultCompressionCodec = defaultCompressionCodec;

        compressionCodecs = Maps.newEnumMap(CompressionCodec.class);

        compressionCodecs.put(CompressionCodec.GZIP, new GZIPCompressionCodec());
        compressionCodecs.put(CompressionCodec.SNAPPY, new SnappyCompressionCodec());
        compressionCodecs.put(CompressionCodec.NONE, new NullStreamCodec());

        encryptionCodec = new AESCipherEncryptionCodec(encryptionConfiguration);
    }

    @Override
    public CompressionCodec getDefaultCompressionCodec() {
        return defaultCompressionCodec;
    }

    // Flip is for backwards compatibility
    @Override
    public StreamCodec get(CompressionCodec compressionCodec, boolean flip) {
        if (flip) {
            return new CombinedStreamCodec(compressionCodecs.get(compressionCodec), encryptionCodec);
        }
        else {
            return new CombinedStreamCodec(encryptionCodec, compressionCodecs.get(compressionCodec));
        }
    }
}

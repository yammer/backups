package com.yammer.io.codec;

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

import com.yammer.io.codec.compression.SnappyCompressionCodec;
import com.yammer.io.codec.encoding.Base64EncodingCodec;
import com.yammer.io.codec.encryption.AESCipherEncryptionCodec;
import com.yammer.io.codec.encryption.AESCipherEncryptionConfiguration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

public class CombinedStreamCodecTest {

    private static final byte[] BYTES = "Now this is the story all about how, My life got flipped, turned upside down, And I'd like to take a minute just sit right there, I'll tell you how I became the prince of a town called Bel-air.".getBytes();

    private StreamCodec compressionCodec;
    private StreamCodec encryptionCodec;
    private StreamCodec encodingCodec;

    @Before
    public void setUp() throws InvalidKeySpecException, NoSuchAlgorithmException, UnsupportedEncodingException {
        compressionCodec = new SnappyCompressionCodec();
        encryptionCodec = new AESCipherEncryptionCodec(new AESCipherEncryptionConfiguration("secret", "salt"));
        encodingCodec = new Base64EncodingCodec();
    }

    @Test
    public void testStreamsCombinedEncodeAndDecode() throws IOException {
        final StreamCodecTestWrapper combinedWrapper = new StreamCodecTestWrapper(new CombinedStreamCodec(compressionCodec, encryptionCodec, encodingCodec));

        final byte[] encoded = combinedWrapper.encode(BYTES);
        Assert.assertFalse(Arrays.equals(BYTES, encoded));

        final byte[] decoded = combinedWrapper.decode(encoded);
        Assert.assertTrue(Arrays.equals(BYTES, decoded));
    }
}

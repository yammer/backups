package com.yammer.io.codec.encryption;

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

import com.yammer.io.codec.StreamCodec;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CipherEncryptionCodecTest {

    private static final String KEY_ALGORITHM = "DES";
    private static final byte[] KEY_BYTES = new byte[] { 0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab, (byte) 0xcd, (byte) 0xef };

    private SecretKey key;

    @Before
    public void setUp() {
        key = new SecretKeySpec(KEY_BYTES, KEY_ALGORITHM);
    }

    @Test(expected = IOException.class)
    public void testInvalidAlgorithm() throws IOException {
        final StreamCodec codec = new CipherEncryptionCodec("invalid", key);
        codec.output(new ByteArrayOutputStream());
    }
}

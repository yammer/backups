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

import com.yammer.io.codec.StreamCodecTestWrapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

public class AESCipherEncryptionCodecTest {

    private static final byte[] BYTES = "Now this is the story all about how, My life got flipped, turned upside down, And I'd like to take a minute just sit right there, I'll tell you how I became the prince of a town called Bel-air.".getBytes();

    @Test
    public void testEncryptionDecryption() throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        final StreamCodecTestWrapper codec = new StreamCodecTestWrapper(new AESCipherEncryptionCodec(new AESCipherEncryptionConfiguration("secret", "salt")));

        final byte[] encrypted = codec.encode(BYTES);
        Assert.assertFalse(Arrays.equals(BYTES, encrypted));

        final byte[] decrypted = codec.decode(encrypted);
        Assert.assertTrue(Arrays.equals(BYTES, decrypted));
    }

    @Test
    public void testDifferentSecretsEncryptDifferently() throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        final StreamCodecTestWrapper codec1 = new StreamCodecTestWrapper(new AESCipherEncryptionCodec(new AESCipherEncryptionConfiguration("test1", "salt")));
        final StreamCodecTestWrapper codec2 = new StreamCodecTestWrapper(new AESCipherEncryptionCodec(new AESCipherEncryptionConfiguration("test2", "salt")));

        final byte[] encrypted1 = codec1.encode(BYTES);
        final byte[] encrypted2 = codec2.encode(BYTES);
        Assert.assertFalse(Arrays.equals(encrypted1, encrypted2));
    }

    @Test
    public void testCantDecryptWithWrongSecret() throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        final StreamCodecTestWrapper codec1 = new StreamCodecTestWrapper(new AESCipherEncryptionCodec(new AESCipherEncryptionConfiguration("test1", "salt")));
        final StreamCodecTestWrapper codec2 = new StreamCodecTestWrapper(new AESCipherEncryptionCodec(new AESCipherEncryptionConfiguration("test2", "salt")));

        final byte[] encrypted = codec1.encode(BYTES);

        Assert.assertTrue(Arrays.equals(BYTES, codec1.decode(encrypted)));
        Assert.assertFalse(Arrays.equals(BYTES, codec2.decode(encrypted)));
    }
}

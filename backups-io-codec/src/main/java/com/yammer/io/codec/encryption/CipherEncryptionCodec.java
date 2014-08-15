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

import com.google.common.io.ByteStreams;
import com.yammer.io.codec.StreamCodec;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

public class CipherEncryptionCodec implements StreamCodec {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int IV_LENGTH = 16;

    private final String algorithm;
    private final SecretKey key;

    public CipherEncryptionCodec(String algorithm, SecretKey key) {
        this.algorithm = algorithm;
        this.key = key;
    }

    protected byte[] generateIV() {
        final byte[] iv = new byte[IV_LENGTH];
        RANDOM.nextBytes(iv);
        return iv;
    }

    private Cipher getCipher(int mode, byte[] iv) throws IOException {
        try {
            final Cipher cipher = Cipher.getInstance(algorithm);
            final AlgorithmParameterSpec ivSpec = new IvParameterSpec(iv);

            cipher.init(mode, key, ivSpec);
            return cipher;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public OutputStream output(OutputStream out) throws IOException {
        final byte[] iv = this.generateIV();
        final Cipher cipher = this.getCipher(Cipher.ENCRYPT_MODE, iv);

        out.write(iv);
        return new CipherOutputStream(out, cipher);
    }

    @Override
    public InputStream input(InputStream in) throws IOException {
        final byte[] iv = new byte[IV_LENGTH];
        ByteStreams.readFully(in, iv);

        final Cipher cipher = this.getCipher(Cipher.DECRYPT_MODE, iv);
        return new CipherInputStream(in, cipher);
    }
}

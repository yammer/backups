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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class AESCipherEncryptionConfiguration {

    private static final String HASH_ALGORITHM = "PBKDF2WithHmacSHA1";
    private static final String KEY_ALGORITHM = "AES";

    public static final int DEFAULT_ITERATIONS = 10_000;
    public static final int DEFAULT_LENGTH = 128;

    @JsonProperty
    @NotEmpty
    private final String secret;

    @JsonProperty
    @NotEmpty
    private final String salt;

    @JsonProperty
    @Min(1_000)
    @Max(1_000_000)
    private int iterations = DEFAULT_ITERATIONS;

    @JsonProperty
    @Min(128)
    @Max(256)
    private int length = DEFAULT_LENGTH;

    public AESCipherEncryptionConfiguration(String secret, String salt) {
        this(secret, salt, DEFAULT_ITERATIONS, DEFAULT_LENGTH);
    }

    @JsonCreator
    public AESCipherEncryptionConfiguration(
            @JsonProperty("secret") String secret,
            @JsonProperty("salt") String salt,
            @JsonProperty("iterations") int iterations,
            @JsonProperty("length") int length) {
        this.secret = secret;
        this.salt = salt;
        this.iterations = iterations;
        this.length = length;
    }

    protected SecretKey getKey() throws NoSuchAlgorithmException, InvalidKeySpecException, UnsupportedEncodingException {
        final SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(HASH_ALGORITHM);
        final SecretKey intermediateKey = keyFactory.generateSecret(new PBEKeySpec(secret.toCharArray(), salt.getBytes("UTF-8"), iterations, length));
        return new SecretKeySpec(intermediateKey.getEncoded(), KEY_ALGORITHM);
    }
}

package com.yammer.backups.auth;

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
import com.google.common.base.Throwables;
import com.yammer.backups.config.TokenConfiguration;
import com.yammer.io.codec.CombinedStreamCodec;
import com.yammer.io.codec.StreamCodec;
import com.yammer.io.codec.encoding.Base64EncodingCodec;
import com.yammer.io.codec.encryption.AESCipherEncryptionCodec;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Objects;

public class TemporaryTokenGenerator {

    private static final ObjectMapper JSON = Jackson.newObjectMapper();
    private static final Logger LOG = LoggerFactory.getLogger(TemporaryTokenGenerator.class);

    private final Duration tokenValidity;
    private final StreamCodec codec;

    public TemporaryTokenGenerator(TokenConfiguration config) throws InvalidKeySpecException, NoSuchAlgorithmException, UnsupportedEncodingException {
        tokenValidity = config.getTemporaryTokenValidity();

        codec = new CombinedStreamCodec(
                new Base64EncodingCodec(),
                new AESCipherEncryptionCodec(config.getEncryptionConfiguration())
        );
    }

    private String encode(TemporaryToken token) {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try (final OutputStream out = codec.output(bytes)) {
            JSON.writeValue(out, token);
        }
        catch (IOException e) {
            LOG.warn("Failed to encode temporary token", e);
            throw Throwables.propagate(e);
        }

        try {
            return bytes.toString("UTF-8").replaceAll("[\\r\\n]", "");
        }
        catch (UnsupportedEncodingException e) {
            throw Throwables.propagate(e);
        }
    }

    private TemporaryToken decode(String token) {
        try (final InputStream in = codec.input(new ByteArrayInputStream(token.getBytes("UTF-8")))) {
            return JSON.readValue(in, TemporaryToken.class);
        }
        catch (IOException e) {
            LOG.warn("Failed to validate temporary token", e);
            throw new IllegalArgumentException(e);
        }
    }

    public String create(String namespace) {
        final long now = System.currentTimeMillis();
        return this.encode(new TemporaryToken(namespace, now));
    }

    private boolean validate(String namespace, TemporaryToken token) {
        if (!Objects.equals(namespace, token.getNamespace())) {
            return false;
        }

        final long now = System.currentTimeMillis();
        final long diff = now - token.getTimestamp();
        return tokenValidity.toMilliseconds() > diff;
    }

    public boolean validate(String namespace, String token) {
        return this.validate(namespace, this.decode(token));
    }
}

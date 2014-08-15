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

import com.yammer.backups.ConfigurationTestUtil;
import com.yammer.backups.config.TokenConfiguration;
import io.dropwizard.configuration.ConfigurationException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TemporaryTokenGeneratorTest {

    private TemporaryTokenGenerator generator;

    @Before
    public void setUp() throws InvalidKeySpecException, NoSuchAlgorithmException, IOException, ConfigurationException {
        final TokenConfiguration config = ConfigurationTestUtil.loadConfiguration().getTokenConfiguration();
        generator = new TemporaryTokenGenerator(config);
    }

    @Test
    public void testGeneratesValidToken() {
        final String token = generator.create("test");

        assertTrue(generator.validate("test", token));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidTokenFailsValidation() {
        final String token = "invalid";

        assertFalse(generator.validate("test", token));
    }

    @Test
    public void testTokenForWrongNamespaceFailsValidation() {
        final String token = generator.create("test1");

        assertFalse(generator.validate("test2", token));
    }
}

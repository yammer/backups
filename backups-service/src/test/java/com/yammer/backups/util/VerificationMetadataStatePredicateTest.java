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

import com.yammer.backups.api.metadata.VerificationMetadata;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VerificationMetadataStatePredicateTest {

    private MetadataStatePredicate<VerificationMetadata, VerificationMetadata.State> predicate;

    @Before
    public void setUp() {
        predicate = new MetadataStatePredicate<>(VerificationMetadata.State.FAILED);
    }

    @Test
    public void testDesiredState() {
        final VerificationMetadata verification = new VerificationMetadata("test", "test", "127.0.0.1", "localhost");
        verification.setState(VerificationMetadata.State.FAILED, "test");

        assertTrue(predicate.apply(verification));
    }

    @Test
    public void testUndesiredState() {
        final VerificationMetadata verification = new VerificationMetadata("test", "test", "127.0.0.1", "localhost");
        verification.setState(VerificationMetadata.State.STARTED, "test");

        assertFalse(predicate.apply(verification));
    }

    @Test
    public void testNull() {
        assertFalse(predicate.apply(null));
    }
}

package com.yammer.backups.api.metadata;

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

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class VerificationMetadataTest extends AbstractMetadataTest<VerificationMetadata> {

    public VerificationMetadataTest() {
        super(VerificationMetadata.class);
    }

    @Override
    protected VerificationMetadata createMetadata() {
        return new VerificationMetadata("service", "test", UUID.randomUUID().toString(), "localhost");
    }

    @Test
    public void testNewVerificationNotCompleted() {
        assertEquals(VerificationMetadata.State.STARTED, data.getState());
        assertFalse(data.getCompletedDate().isPresent());
    }

    @Test
    public void testCompleteVerification() {
        data.setState(VerificationMetadata.State.FINISHED, "test");
        assertTrue(data.getCompletedDate().isPresent());
    }
}

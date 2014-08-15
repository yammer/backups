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

import com.yammer.backups.api.CompressionCodec;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class BackupMetadataTest extends AbstractMetadataTest<BackupMetadata> {

    public BackupMetadataTest() {
        super(BackupMetadata.class);
    }

    @Override
    protected BackupMetadata createMetadata() {
        return new BackupMetadata("service", UUID.randomUUID().toString(), "localhost");
    }

    @Test
    public void testNewBackupNotCompleted() {
        assertEquals(BackupMetadata.State.WAITING, data.getState());
        assertFalse(data.getCompletedDate().isPresent());
    }

    @Test
    public void testCompleteBackup() {
        data.setState(BackupMetadata.State.FINISHED, "test");
        assertTrue(data.getCompletedDate().isPresent());
    }

    @Test(expected = IllegalStateException.class)
    public void testAddChunkInvalidState() {
        data.setState(BackupMetadata.State.WAITING, "test");
        data.addChunk("test1", "test1-id-1", 700, 500, "hash", "localhost", CompressionCodec.SNAPPY);
    }

    @Test
    public void testAddChunksSize() {
        data.setState(BackupMetadata.State.RECEIVING, "test");
        assertEquals(0, data.getOriginalSize());

        data.addChunk("test1", "test1-id-1", 700, 500, "hash", "localhost", CompressionCodec.SNAPPY);
        assertEquals(700, data.getOriginalSize());

        data.addChunk("test2", "test2-id-1", 300, 500, "hash", "localhost", CompressionCodec.SNAPPY);
        assertEquals(1000, data.getOriginalSize());
    }
}

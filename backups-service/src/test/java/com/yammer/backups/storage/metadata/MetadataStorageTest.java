package com.yammer.backups.storage.metadata;

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

import com.google.common.base.Optional;
import com.yammer.backups.api.metadata.BackupMetadata;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.*;

public abstract class MetadataStorageTest {

    private BackupMetadata createBackup(String service)  {
        return new BackupMetadata(service, UUID.randomUUID().toString(), "localhost");
    }

    private MetadataStorage<BackupMetadata> storage;

    protected abstract MetadataStorage<BackupMetadata> getMetadataStorage() throws Exception;
    protected abstract void clearMetadataStorage(MetadataStorage<BackupMetadata> storage);

    @Before
    public void setUp() throws Exception {
        storage = this.getMetadataStorage();
    }

    @After
    public void tearDown() throws Exception {
        this.clearMetadataStorage(storage);
    }

    @Test
    public void testGet() {
        final BackupMetadata data = this.createBackup("test");
        storage.put(data);

        final Optional<BackupMetadata> result = storage.get(data.getService(), data.getId());
        assertTrue(result.isPresent());
    }

    @Test
    public void testModify() {
        final BackupMetadata data = this.createBackup("test");
        storage.put(data);

        final Optional<BackupMetadata> result1 = storage.get(data.getService(), data.getId());
        assertTrue(result1.isPresent());
        assertEquals(BackupMetadata.State.WAITING, result1.get().getState());

        data.setFailed("test");
        storage.update(data);

        final Optional<BackupMetadata> result2 = storage.get(data.getService(), data.getId());
        assertTrue(result2.isPresent());
        assertEquals(BackupMetadata.State.FAILED, result2.get().getState());
    }

    @Test
    public void testGetInvalid() {
        final Optional<BackupMetadata> result = storage.get("invalid", "id");
        assertFalse(result.isPresent());
    }

    @Test
    public void testDelete() {
        final BackupMetadata data = this.createBackup("test");
        storage.put(data);

        assertTrue(storage.get(data.getService(), data.getId()).isPresent());
        assertTrue(storage.listAll().contains(data));

        storage.delete(data);
        assertFalse(storage.get(data.getService(), data.getId()).isPresent());
        assertFalse(storage.listAll().contains(data));
    }

    @Test
    public void testDeleteNonExistantData() {
        final BackupMetadata data = this.createBackup("test");
        storage.delete(data);
    }

    @Test
    public void testList() {
        storage.put(this.createBackup("feedie"));
        storage.put(this.createBackup("feedie"));
        storage.put(this.createBackup("feedie"));
        storage.put(this.createBackup("hermes"));

        assertEquals(4, storage.listAll().size());
        assertEquals(3, storage.listAll("feedie").size());
        assertEquals(1, storage.listAll("hermes").size());
    }

    @Test
    public void testListKnownServices() {
        storage.put(this.createBackup("feedie"));
        storage.put(this.createBackup("feedie"));
        storage.put(this.createBackup("hermes"));

        final Set<String> services = storage.listAllRows();
        assertEquals(2, services.size());

        assertTrue(services.contains("feedie"));
        assertTrue(services.contains("hermes"));
    }

    @Test
    public void testListUnknownService() {
        assertTrue(storage.listAll("invalid").isEmpty());
    }
}

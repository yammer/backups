package com.yammer.storage.file.azure;

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

import com.microsoft.windowsazure.services.core.storage.StorageException;
import com.yammer.backups.ConfigurationTestUtil;
import com.yammer.backups.config.BackupConfiguration;
import com.yammer.storage.file.FileStorageTest;
import io.dropwizard.configuration.ConfigurationException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import static org.junit.Assert.assertEquals;

@Ignore
public class AzureFileStorageTest extends FileStorageTest<AzureFileStorage> {

    private String azureNamespace;

    @Before
    public void setUp() throws Exception {
        azureNamespace = String.format("test%d", System.currentTimeMillis()); // Azure is picky about table names
        super.setUp();
    }

    @Override
    protected AzureFileStorage getFileStorage() throws IOException, ConfigurationException, StorageException, InvalidKeyException, URISyntaxException {
        final BackupConfiguration configuration = ConfigurationTestUtil.loadConfiguration();
        return new AzureFileStorage(configuration.getOffsiteConfiguration().getStorageConfiguration(), azureNamespace);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSanitizeEmptyBucketName() {
        storage.getSanitizedBucketName("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSanitizeNullBucketName() {
        storage.getSanitizedBucketName(null);
    }

    @Test
    public void testSanitizeValidBucketName() {
        final String name = "test";
        assertEquals(azureNamespace + name, storage.getSanitizedBucketName(name));
    }

    @Test
    public void testSanitizeStripsPeriods() {
        assertEquals(azureNamespace + "stageprankiebn1yammercom", storage.getSanitizedBucketName("stageprankie.bn1.yammer.com"));
    }
}

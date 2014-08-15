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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.ByteStreams;
import com.microsoft.windowsazure.services.blob.client.CloudBlobClient;
import com.microsoft.windowsazure.services.blob.client.CloudBlobContainer;
import com.microsoft.windowsazure.services.blob.client.CloudBlockBlob;
import com.microsoft.windowsazure.services.core.storage.CloudStorageAccount;
import com.microsoft.windowsazure.services.core.storage.StorageException;
import com.microsoft.windowsazure.services.table.client.CloudTableClient;
import com.microsoft.windowsazure.services.table.client.TableOperation;
import com.yammer.storage.file.FileStorage;
import io.dropwizard.util.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class AzureFileStorage implements FileStorage {

    private static final Logger LOG = LoggerFactory.getLogger(AzureFileStorage.class);

    private static final Pattern NON_ALPHANUMERIC_REGEX = Pattern.compile("[^a-z0-9]");

    private static final String AZURE_DATE_FORMAT = "yyyyMMdd'T'0000";
    private static final String AZURE_METRICS_KEY = "data";
    private static final String AZURE_METRICS_TABLE = "$MetricsCapacityBlob";

    private static final Size AZURE_STORAGE_SIZE = Size.terabytes(200);

    private static String getAzureMetricsPartitionKey(int dayOffset) {
        final DateFormat dateFormat = new SimpleDateFormat(AZURE_DATE_FORMAT);

        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, dayOffset);

        return dateFormat.format(cal.getTime());
    }

    private final CloudStorageAccount account;
    private final CloudBlobClient blobClient;
    private final CloudTableClient tableClient;
    private final Cache<String, AzureCapacityEntity> capacityCache;
    private final String prefix;

    public AzureFileStorage(AzureFileStorageConfiguration config, String prefix) throws URISyntaxException, InvalidKeyException {
        account = CloudStorageAccount.parse(config.getConnectionString());

        blobClient = account.createCloudBlobClient();
        tableClient = account.createCloudTableClient();

        capacityCache = CacheBuilder
                .newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();

        this.prefix = prefix;
    }

    @Override
    public void start() {
        try {
            LOG.info("Created new {} with {} capacity, {} used, {} free", this, getTotalSpace(), getUsedSpace(), getFreeSpace());
        }
        catch (IOException e) {
            LOG.warn("Failed to fetch capacity", e);
        }
    }

    @Override
    public void stop() {

    }

    protected String getSanitizedBucketName(String namespace) {
        if (!Strings.isNullOrEmpty(namespace)) {
            // Prefix
            namespace = String.format("%s%s", prefix, namespace);

            // Lowercase
            namespace = namespace.toLowerCase();

            // Alphanumeric only
            namespace = NON_ALPHANUMERIC_REGEX.matcher(namespace).replaceAll("");
        }

        if (Strings.isNullOrEmpty(namespace)) {
            throw new IllegalArgumentException("Azure namespaces is empty after sanitizing. Azure only allows alphanumeric bucket names.");
        }

        return namespace;
    }

    public CloudBlobContainer getBucket(String namespace) throws IOException {
        try {
            final String containerName = this.getSanitizedBucketName(namespace);
            final CloudBlobContainer bucket = blobClient.getContainerReference(containerName);
            bucket.createIfNotExist();

            return bucket;
        } catch (URISyntaxException | StorageException e) {
            throw new IOException(e);
        }
    }

    private CloudBlockBlob getBlob(String namespace, String path) throws IOException {
        try {
            final CloudBlobContainer bucket = this.getBucket(namespace);
            return bucket.getBlockBlobReference(path);
        } catch (URISyntaxException | StorageException e) {
            throw new IOException(e);
        }
    }

    @Override
    public OutputStream upload(String namespace, String path) throws IOException {
        final CloudBlockBlob blob = this.getBlob(namespace, path);
        try {
            if (blob.exists()) {
                throw new IOException("File already exists");
            }

            return blob.openOutputStream();
        } catch (StorageException e) {
            throw new IOException(e);
        }
    }

    @Override
    public InputStream download(String namespace, String path) throws IOException {
        final CloudBlockBlob blob = this.getBlob(namespace, path);
        try {
            return blob.openInputStream();
        } catch (StorageException e) {
            throw new IOException(e);
        }
    }

    @Override
    public OutputStream append(String namespace, String path) throws IOException {
        final CloudBlockBlob blob = this.getBlob(namespace, path);
        try {
            final OutputStream out = blob.openOutputStream();

            if (blob.exists()) {
                // rewrite the entire existing contents. Please don't do this on big files!
                try (final InputStream in = blob.openInputStream()) {
                    ByteStreams.copy(in, out);
                }
            }

            return out;
        } catch (StorageException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean exists(String namespace, String path) throws IOException {
        final CloudBlockBlob blob = this.getBlob(namespace, path);
        try {
            return blob.exists();
        } catch (StorageException e) {
            throw new IOException(e);
        }
    }

    @VisibleForTesting
    @Override
    public boolean delete(String namespace) throws IOException {
        final CloudBlobContainer bucket = this.getBucket(namespace);
        try {
            return bucket.deleteIfExists();
        } catch (StorageException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean delete(String namespace, String path) throws IOException {
        final CloudBlockBlob blob = this.getBlob(namespace, path);
        try {
            return blob.deleteIfExists();
        } catch (StorageException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean ping() throws IOException {
        try {
            blobClient.downloadServiceProperties();
            return true;
        } catch (StorageException e) {
            throw new IOException(e);
        }
    }

    @Override
    public String toString() {
        return "AzureFileStorage{" + "account=" + account.getCredentials().getAccountName() + '}';
    }

    @Override
    public Size getTotalSpace() throws IOException {
        return AZURE_STORAGE_SIZE;
    }

    @Override
    public Size getUsedSpace() throws IOException {
        for (int i = 0;i < 2;i++) {
            try {
                return this.getUsedSpace(-i);
            }
            catch (StorageException | IOException e) {
                LOG.debug("Failed to fetch storage space for day offset: " + (-i), e);
            }
        }

        throw new IOException("There is no capacity data available");
    }

    private Size getUsedSpace(int daysOffset) throws StorageException, IOException {
        final String partitionKey = getAzureMetricsPartitionKey(daysOffset);

        final AzureCapacityEntity capacity;
        try {
            capacity = capacityCache.get(partitionKey, new Callable<AzureCapacityEntity>() {
                @Override
                public AzureCapacityEntity call() throws StorageException, IOException {
                    final TableOperation readCapacityOperation = TableOperation.retrieve(partitionKey, AZURE_METRICS_KEY, AzureCapacityEntity.class);
                    final AzureCapacityEntity result = tableClient.execute(AZURE_METRICS_TABLE, readCapacityOperation).getResultAsType();
                    if (result == null) {
                        throw new IOException("No capacity details for " + partitionKey);
                    }

                    return result;
                }
            });
        }
        catch (ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof StorageException) {
                throw (StorageException) cause;
            }

            if (cause instanceof IOException) {
                throw (IOException) cause;
            }

            LOG.warn("Failed to fetch azure storage capacity", e);
            return Size.bytes(0);
        }

        return Size.bytes(capacity.getCapacity());
    }

    @Override
    public Size getFreeSpace() throws IOException {
        final Size totalSpace = this.getTotalSpace();
        final Size usedSpace = this.getUsedSpace();

        return Size.bytes(totalSpace.toBytes() - usedSpace.toBytes());
    }
}

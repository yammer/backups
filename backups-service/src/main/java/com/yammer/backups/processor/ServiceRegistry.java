package com.yammer.backups.processor;

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
import com.yammer.backups.service.metadata.ServiceMetadata;
import com.yammer.backups.storage.metadata.MetadataStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class ServiceRegistry implements BackupProcessorListener {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceRegistry.class);

    private final MetadataStorage<ServiceMetadata> metadataStorage;

    public ServiceRegistry(MetadataStorage<ServiceMetadata> metadataStorage) {
        this.metadataStorage = metadataStorage;
    }

    @Override
    public void backupCreated(BackupMetadata backup) {
        LOG.debug(String.format("Service registry notified of backup for service %s", backup.getService()));
        Optional<ServiceMetadata> existingMetadata = metadataStorage.get(backup.getService(), ServiceMetadata.COLUMN_KEY);
        if (!existingMetadata.isPresent()) {
            registerNewService(backup);
        }
    }

    private void registerNewService(BackupMetadata backup) {
        Optional<ServiceMetadata> existingMetadata = metadataStorage.get(backup.getService(), ServiceMetadata.COLUMN_KEY);
        if (!existingMetadata.isPresent()) {
            LOG.debug(String.format("Registering new service %s", backup.getService()));
            ServiceMetadata service = new ServiceMetadata(backup.getService(), false);
            metadataStorage.put(service);
        }
    }

    @Override
    public void backupUploaded(BackupMetadata backup, String filename) {

    }

    @Override
    public void backupFinished(BackupMetadata backup, boolean success) {

    }

    @Override
    public void backupDeleted(BackupMetadata backup) {

    }

    public void disableHealthcheck(String serviceName) {
        Optional<ServiceMetadata> metadata = metadataStorage.get(serviceName, ServiceMetadata.COLUMN_KEY);
        if (metadata.isPresent()) {
            metadata.get().setDisableHealthcheck(true);
            metadataStorage.update(metadata.get());
        }
    }

    public boolean healthCheckDisabled(String service) {
        final Optional<ServiceMetadata> serviceMetadata = getServiceMetadata(service);
        return serviceMetadata.isPresent() && serviceMetadata.get().isDisableHealthcheck();
    }

    public Optional<ServiceMetadata> getServiceMetadata(String serviceName) {
        return metadataStorage.get(serviceName, ServiceMetadata.COLUMN_KEY);
    }

    public Collection<ServiceMetadata> listAll() {
        return metadataStorage.listAll();
    }

    public boolean updateServiceIfPresent(ServiceMetadata serviceMetadata) {
        final Optional<ServiceMetadata> currentMetadata = metadataStorage.get(serviceMetadata.getId(), ServiceMetadata.COLUMN_KEY);
        if (currentMetadata.isPresent()) {
            metadataStorage.put(serviceMetadata);
            return true;
        } else {
            return false;
        }
    }
}

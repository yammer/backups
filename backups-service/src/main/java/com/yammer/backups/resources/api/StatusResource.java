package com.yammer.backups.resources.api;

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

import com.yammer.backups.api.status.ActiveStatus;
import com.yammer.backups.api.status.StorageStatus;
import com.yammer.backups.processor.BackupProcessor;
import com.yammer.storage.file.FileStorage;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

@Path("/api/status")
public class StatusResource {

    private final BackupProcessor backupProcessor;
    private final FileStorage localStorage;
    private final FileStorage offsiteStorage;

    public StatusResource(final BackupProcessor backupProcessor, FileStorage localStorage, FileStorage offsiteStorage) {
        this.backupProcessor = backupProcessor;
        this.localStorage = localStorage;
        this.offsiteStorage = offsiteStorage;
    }

    @GET
    @Path("/active")
    @Produces(MediaType.APPLICATION_JSON)
    public ActiveStatus getActiveStatus() {
        return new ActiveStatus(
                backupProcessor.getActiveUploadsCount(),
                backupProcessor.getActiveDownloadsCount(),
                backupProcessor.getActiveStoresCount()
        );
    }

    @GET
    @Path("/storage")
    @Produces(MediaType.APPLICATION_JSON)
    public StorageStatus getStorageStatus() throws IOException {
        return new StorageStatus(
                localStorage.getTotalSpace(), localStorage.getUsedSpace(), localStorage.getFreeSpace(),
                offsiteStorage.getTotalSpace(), offsiteStorage.getUsedSpace(), offsiteStorage.getFreeSpace()
        );
    }
}

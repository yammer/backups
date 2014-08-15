package com.yammer.backups.api.status;

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

public class ActiveStatus {

    private final long uploads;
    private final long downloads;
    private final long stores;

    public ActiveStatus(long uploads, long downloads, long stores) {
        this.uploads = uploads;
        this.downloads = downloads;
        this.stores = stores;
    }

    public long getUploads() {
        return uploads;
    }

    public long getDownloads() {
        return downloads;
    }

    public long getStores() {
        return stores;
    }

}

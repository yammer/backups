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

import com.microsoft.windowsazure.services.table.client.TableServiceEntity;

public class AzureCapacityEntity extends TableServiceEntity {

    private long capacity;
    private long containerCount;
    private long objectCount;

    public AzureCapacityEntity(String partitionKey, String rowKey) {
        this.partitionKey = partitionKey;
        this.rowKey = rowKey;
    }

    public AzureCapacityEntity() {

    }

    public long getCapacity() {
        return capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    public long getContainerCount() {
        return containerCount;
    }

    public void setContainerCount(long containerCount) {
        this.containerCount = containerCount;
    }

    public long getObjectCount() {
        return objectCount;
    }

    public void setObjectCount(long objectCount) {
        this.objectCount = objectCount;
    }

    @Override
    public String getPartitionKey() {
        return partitionKey;
    }

    @Override
    public String getRowKey() {
        return rowKey;
    }
}

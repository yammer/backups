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

import io.dropwizard.util.Size;

public class StorageStatus {

    private final Size localCapacity;
    private final Size localUsedCapacity;
    private final Size localFreeCapacity;
    private final Size offsiteCapacity;
    private final Size offsiteUsedCapacity;
    private final Size offsiteFreeCapacity;

    public StorageStatus(Size localCapacity, Size localUsedCapacity, Size localFreeCapacity,
            Size offsiteCapacity, Size offsiteUsedCapacity, Size offsiteFreeCapacity) {
        this.localCapacity = localCapacity;
        this.localUsedCapacity = localUsedCapacity;
        this.localFreeCapacity = localFreeCapacity;
        this.offsiteCapacity = offsiteCapacity;
        this.offsiteUsedCapacity = offsiteUsedCapacity;
        this.offsiteFreeCapacity = offsiteFreeCapacity;
    }

    public Size getLocalCapacity() {
        return localCapacity;
    }

    public Size getLocalUsedCapacity() {
        return localUsedCapacity;
    }

    public Size getLocalFreeCapacity() {
        return localFreeCapacity;
    }

    public Size getOffsiteCapacity() {
        return offsiteCapacity;
    }

    public Size getOffsiteUsedCapacity() {
        return offsiteUsedCapacity;
    }

    public Size getOffsiteFreeCapacity() {
        return offsiteFreeCapacity;
    }
}

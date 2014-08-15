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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.util.Size;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class AzureFileStorageConfiguration extends AzureAccountConfiguration {

    @Valid
    @NotNull
    private Size minCapacity = Size.gigabytes(100);

    public AzureFileStorageConfiguration(
            @JsonProperty("name") String name,
            @JsonProperty("key") String key) {
        super(name, key);
    }

    public Size getMinCapacity() {
        return minCapacity;
    }

    public void setMinCapacity(Size minCapacity) {
        this.minCapacity = minCapacity;
    }
}

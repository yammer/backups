package com.yammer.backups.api;

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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientPermission implements Storable {

    private final String token;
    private final String service;

    @JsonCreator
    public ClientPermission(
            @JsonProperty("token") String token,
            @JsonProperty("service") String service) {
        this.token = token;
        this.service = service;
    }

    @SuppressWarnings("unused")
    public String getToken() {
        return token;
    }

    @SuppressWarnings("unused")
    public String getService() {
        return service;
    }

    @JsonIgnore
    @Override
    public String getRowKey() {
        return service;
    }

    @JsonIgnore
    @Override
    public String getColumnKey() {
        return token;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClientPermission that = (ClientPermission) o;

        if (service != null ? !service.equals(that.service) : that.service != null) return false;
        if (token != null ? !token.equals(that.token) : that.token != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = token != null ? token.hashCode() : 0;
        result = 31 * result + (service != null ? service.hashCode() : 0);
        return result;
    }
}

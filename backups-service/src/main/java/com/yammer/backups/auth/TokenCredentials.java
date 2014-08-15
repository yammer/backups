package com.yammer.backups.auth;

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

public class TokenCredentials {

    public static final int MIN_TOKEN_LENGTH = 10;

    private final String token;
    private final String service;
    private final boolean readRequest;

    public TokenCredentials(String token, String service, boolean readRequest) {
        this.token = token;
        this.service = service;
        this.readRequest = readRequest;
    }

    public String getToken() {
        return token;
    }

    public String getService() {
        return service;
    }

    public boolean isReadRequest() {
        return readRequest;
    }

    @Override
    public String toString() {
        return "TokenCredentials{" +
                "token='" + token + '\'' +
                ", service='" + service + '\'' +
                ", readRequest='" + readRequest + '\'' +
                '}';
    }
}

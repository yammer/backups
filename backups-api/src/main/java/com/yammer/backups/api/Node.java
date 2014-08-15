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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.InetAddress;
import java.net.URL;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Node implements Storable {

    private final String name;
    private final URL url;
    private final String hostname;

    @JsonCreator
    public Node(
        @JsonProperty("name") String name,
        @JsonProperty("url") URL url,
        @JsonProperty("hostname") String hostname) {
        this.name = name;
        this.url = url;
        this.hostname = hostname;
    }

    public Node(String name, URL url) {
        this (name, url, InetAddress.getLoopbackAddress().getHostName());
    }

    @SuppressWarnings("unused")
    public String getName() {
        return name;
    }

    @SuppressWarnings("unused")
    public URL getUrl() {
        return url;
    }

    @SuppressWarnings("unused")
    public String getHostname() {
        return hostname;
    }

    @Override
    public String getRowKey() {
        return name;
    }

    @Override
    public String getColumnKey() {
        return name;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Node node = (Node) o;

        if (name != null ? !name.equals(node.name) : node.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}

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
import org.joda.time.DateTime;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Chunk {

    private final String path;
    private final long originalSize;
    private final long size;
    private final String hash;
    private final DateTime storedDate;
    private final String nodeName;
    private final CompressionCodec compressionCodec;

    @JsonCreator
    public Chunk(
            @JsonProperty("path") String path,
            @JsonProperty("originalSize") long originalSize,
            @JsonProperty("size") long size,
            @JsonProperty("hash") String hash,
            @JsonProperty("storedDate") DateTime storedDate,
            @JsonProperty("nodeName") String nodeName,
            @JsonProperty("compressionCodec") CompressionCodec compressionCodec) {
        this.path = path;
        this.originalSize = originalSize;
        this.size = size;
        this.hash = hash;
        this.storedDate = storedDate;
        this.nodeName = nodeName;

        // Before this was added we only used Snappy, so null must = Snappy
        this.compressionCodec = compressionCodec == null ? CompressionCodec.SNAPPY : compressionCodec;
    }

    public Chunk(String path, long originalSize, long size, String hash, String nodeName, CompressionCodec compressionCodec) {
        this (path, originalSize, size, hash, DateTime.now(), nodeName, compressionCodec);
    }

    public String getPath() {
        return path;
    }

    public long getOriginalSize() {
        return originalSize;
    }

    public long getSize() {
        return size;
    }

    public String getHash() {
        return hash;
    }

    public DateTime getStoredDate() {
        return storedDate;
    }

    public String getNodeName() {
        return nodeName;
    }

    public boolean isAtNode(String nodeName) {
        return Objects.equals(this.nodeName, nodeName);
    }

    public CompressionCodec getCompressionCodec() {
        return compressionCodec;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Chunk chunk = (Chunk) o;

        if (path != null ? !path.equals(chunk.path) : chunk.path != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return path != null ? path.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Chunk{" +
                "path='" + path + '\'' +
                ", originalSize=" + originalSize +
                ", size=" + size +
                ", hash='" + hash + '\'' +
                ", storedDate=" + storedDate +
                '}';
    }
}

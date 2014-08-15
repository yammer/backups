package com.yammer.backups.api.metadata;

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
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.yammer.backups.api.Chunk;
import com.yammer.backups.api.CompressionCodec;
import com.yammer.backups.api.Location;
import com.yammer.backups.api.Transition;
import org.joda.time.DateTime;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BackupMetadata extends AbstractMetadata<BackupMetadata.State> {

    private static final int MODEL_VERSION = 1;

    public static enum State { WAITING, RECEIVING, QUEUED, UPLOADING, FINISHED, FAILED, TIMEDOUT }

    public static Predicate<BackupMetadata> IN_LOCATION_PREDICATE(final Location location) {
        return new Predicate<BackupMetadata>() {
            @Override
            public boolean apply(BackupMetadata input) {
                return input != null && input.existsAtLocation(location);
            }
        };
    }

    public static Predicate<BackupMetadata> IN_NODE_PREDICATE(final String nodeName) {
        return new Predicate<BackupMetadata>() {
            @Override
            public boolean apply(BackupMetadata input) {
                return input != null && input.isAtNode(nodeName);
            }
        };
    }

    private long originalSize;
    private long size;
    private Optional<String> verificationId;

    @JsonProperty
    private final EnumSet<Location> locations;

    @JsonProperty
    private final ListMultimap<String, Chunk> chunks;

    @JsonProperty
    private final AtomicInteger pendingStores;

    @JsonProperty
    private final AtomicInteger pendingUploads;

    @JsonCreator
    public BackupMetadata(
            @JsonProperty("service") String service,
            @JsonProperty("id") String id,
            @JsonProperty("sourceAddress") String sourceAddress,
            @JsonProperty("startedDate") DateTime startedDate,
            @JsonProperty("completedDate") Optional<DateTime> completedDate,
            @JsonProperty("nodeName") String nodeName,
            @JsonProperty("state") State state,
            @JsonProperty("originalSize") long originalSize,
            @JsonProperty("size") long size,
            @JsonProperty("locations") EnumSet<Location> locations,
            @JsonProperty("chunks") ListMultimap<String, Chunk> chunks,
            @JsonProperty("pendingStores") AtomicInteger pendingStores,
            @JsonProperty("pendingUploads") AtomicInteger pendingUploads,
            @JsonProperty("transitions") List<Transition<State>> transitions,
            @JsonProperty("modelVersion") int modelVersion,
            @JsonProperty("verificationId") Optional<String> verificationId) {
        super (service, id, state, sourceAddress, startedDate, completedDate, nodeName, transitions, modelVersion);

        this.originalSize = originalSize;
        this.size = size;
        this.locations = locations == null ? EnumSet.noneOf(Location.class) : locations;
        this.chunks = chunks == null ? LinkedListMultimap.<String, Chunk>create() : chunks;
        this.pendingStores = pendingStores == null ? new AtomicInteger(0) : pendingStores;
        this.pendingUploads = pendingUploads == null ? new AtomicInteger(0) : pendingUploads;
        this.verificationId = verificationId;
    }

    public BackupMetadata(String service, String sourceAddress, String nodeName) {
        this(service, randomID(), sourceAddress, DateTime.now(), Optional.<DateTime>absent(), nodeName,
                State.WAITING, 0, 0, null, null, null, null, null, BackupMetadata.MODEL_VERSION, Optional.<String>absent());
    }

    @JsonIgnore
    public boolean hasVerification() {
        return verificationId.isPresent();
    }

    public Optional<String> getVerificationId() {
        return verificationId;
    }

    public void setVerificationId(String id) {
        verificationId = Optional.fromNullable(id);
    }

    @JsonIgnore
    public int getPendingStores() {
        return pendingStores.get();
    }

    public int incrementAndGetPendingStores() {
        return pendingStores.incrementAndGet();
    }

    public int decrementAndGetPendingStores() {
        return pendingStores.decrementAndGet();
    }

    @JsonIgnore
    public int getPendingUploads() {
        return pendingUploads.get();
    }

    public int incrementAndGetPendingUploads() {
        return pendingUploads.incrementAndGet();
    }

    public int decrementAndGetPendingUploads() {
        return pendingUploads.decrementAndGet();
    }

    public long getOriginalSize() {
        return originalSize;
    }

    public long getSize() {
        return size;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean existsAtLocation() {
        return !locations.isEmpty();
    }

    public boolean existsAtLocation(Location location) {
        return locations.contains(location);
    }

    public void addLocation(Location location) {
        locations.add(location);
    }

    public void removeLocation(Location location) {
        locations.remove(location);
    }

    public synchronized void addChunk(String filename, String path, long originalSize, long size, String hash, String nodeName, CompressionCodec compressionCodec) {
        if (!this.isState(State.RECEIVING)) {
            throw new IllegalStateException(String.format("Cannot add file when not in RECEIVING state."));
        }

        chunks.put(filename, new Chunk(path, originalSize, size, hash, nodeName, compressionCodec));

        this.originalSize += originalSize;
        this.size += size;
    }

    @JsonIgnore
    public List<Chunk> getChunks(String filename) {
        return chunks.get(filename);
    }

    @JsonIgnore
    public Collection<Chunk> getChunks() {
        return chunks.values();
    }

    @Override
    public void setFailed(String comment) {
        this.setState(State.FAILED, comment);
    }

    @Override
    public void setTimedOut(String comment) {
        this.setState(State.TIMEDOUT, comment);
    }

    @JsonIgnore
    @Override
    public boolean isSuccessful() {
        return this.isState(State.UPLOADING, State.FINISHED);
    }

    @JsonIgnore
    @Override
    public boolean isRunning() {
        return this.isState(State.QUEUED, State.RECEIVING, State.UPLOADING, State.WAITING);
    }

    @JsonIgnore
    @Override
    public boolean isFailed() {
        return this.isState(State.FAILED, State.TIMEDOUT);
    }

    @JsonIgnore
    @Override
    public boolean isFinished() {
        return !isRunning();
    }

    @Override
    public String toString() {
        final ToStringHelper helper = Objects.toStringHelper(this.getClass());

        helper.add("service", getService());
        helper.add("id", getId());
        helper.add("verificationId", verificationId.or("none"));
        helper.add("state", getState());
        helper.add("startedDate",getStartedDate());

        if (getCompletedDate().isPresent()) {
            helper.add("completed", getCompletedDate().get());
        }
        else {
            helper.add("completed", "running");
        }

        return helper.toString();
    }
}

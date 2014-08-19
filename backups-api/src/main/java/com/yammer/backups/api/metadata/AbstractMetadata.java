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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.yammer.backups.api.Storable;
import com.yammer.backups.api.Transition;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractMetadata<T> implements Storable {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractMetadata.class);

    public static final Comparator<AbstractMetadata<?>> COMPARATOR = new Comparator<AbstractMetadata<?>>() {
        @Override
        public int compare(AbstractMetadata<?> o1, AbstractMetadata<?> o2) {
            return o1.getStartedDate().compareTo(o2.getStartedDate()) * -1;
        }
    };

    protected static String randomID() {
        return UUID.randomUUID().toString();
    }

    private final String service;
    private final String id;
    private final String sourceAddress;
    private final DateTime startedDate;
    private final int modelVersion;

    @JsonProperty
    private final String nodeName;

    @JsonProperty
    private final List<Transition<T>> transitions;

    private T state;
    private Optional<DateTime> completedDate;

    @JsonCreator
    protected AbstractMetadata(String service, String id, T state, String sourceAddress, DateTime startedDate,
                               Optional<DateTime> completedDate, String nodeName, List<Transition<T>> transitions,
                               int modelVersion) {
        this.service = service;
        this.id = id == null ? AbstractMetadata.randomID() : id;
        this.state = state;
        this.sourceAddress = sourceAddress;
        this.startedDate = startedDate;
        this.completedDate = completedDate;
        this.nodeName = nodeName;
        this.transitions = transitions == null ? Lists.<Transition<T>>newArrayList() : transitions;
        this.modelVersion = modelVersion;
    }

    public synchronized T getState() {
        return state;
    }

    public synchronized void transitionState(T expect, T update, String comment) {
        if (!expect.equals(state)) {
            throw new IllegalStateException(String.format("Expected: %s, actual: %s", expect, state));
        }

        this.setState(update, comment);
    }

    @VisibleForTesting
    public synchronized void setState(T update, String comment) {
        LOG.trace("Changing state {} -> {} for {} because: {}", state, update, this, comment);

        transitions.add(new Transition<>(state, update, comment));
        state = update;

        if (this.isSuccessful() || this.isFailed()) {
            this.setCompletedDate(DateTime.now());
        }
    }

    public String getSourceAddress() {
        return sourceAddress;
    }

    public DateTime getStartedDate() {
        return startedDate;
    }

    protected void setCompletedDate(DateTime completedDate) {
        this.completedDate = Optional.fromNullable(completedDate);
    }

    public Optional<DateTime> getCompletedDate() {
        return completedDate;
    }

    public String getService() {
        return service;
    }

    public String getId() {
        return id;
    }

    public boolean isAtNode(String nodeName) {
        return Objects.equals(this.getNodeName(), nodeName);
    }

    public abstract void setFailed(String comment);
    public abstract void setTimedOut(String comment);

    @SuppressWarnings("unchecked")
    public synchronized boolean isState(T ... options) {
        for (T option : options) {
            if (option.equals(state)) {
                return true;
            }
        }

        return false;
    }

    @JsonIgnore
    public abstract boolean isSuccessful();

    @JsonIgnore
    public abstract boolean isFailed();

    @JsonIgnore
    public abstract boolean isRunning();

    @JsonIgnore
    public abstract boolean isFinished();

    public int getModelVersion() {
        return modelVersion;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractMetadata<?> metadata = (AbstractMetadata<?>) o;

        if (id != null ? !id.equals(metadata.id) : metadata.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @JsonIgnore
    @Override
    public String getRowKey() {
       return this.service;
    }

    @JsonIgnore
    @Override
    public String getColumnKey() {
        return this.id;
    }

    @JsonIgnore
    public String getNodeName() {
        return nodeName;
    }
}

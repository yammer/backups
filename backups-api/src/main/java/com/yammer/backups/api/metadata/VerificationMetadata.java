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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.yammer.backups.api.Transition;
import org.joda.time.DateTime;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VerificationMetadata extends AbstractMetadata<VerificationMetadata.State> {

    private static final int MODEL_VERSION = 1;

    public static enum State { STARTED, FINISHED, FAILED, TIMEDOUT }

    public static Predicate<VerificationMetadata> IN_NODE_PREDICATE(final String nodeName) {
        return new Predicate<VerificationMetadata>() {
            @Override
            public boolean apply(VerificationMetadata input) {
                return input != null && input.isAtNode(nodeName);
            }
        };
    }

    private final String backupId;

    public VerificationMetadata(
            @JsonProperty("service") String service,
            @JsonProperty("id") String id,
            @JsonProperty("backupId") String backupId,
            @JsonProperty("sourceAddress") String sourceAddress,
            @JsonProperty("startedDate") DateTime startedDate,
            @JsonProperty("completedDate") Optional<DateTime> completedDate,
            @JsonProperty("nodeName") String nodeName,
            @JsonProperty("state") State state,
            @JsonProperty("transitions") List<Transition<State>> transitions,
            @JsonProperty("modelVersion") int modelVersion) {
        super (service, id, state, sourceAddress, startedDate, completedDate, nodeName, transitions, modelVersion);

        this.backupId = backupId;
    }

    public VerificationMetadata(String service, String backupId, String sourceAddress, String nodeName) {
        this(service, randomID(), backupId, sourceAddress, DateTime.now(), Optional.<DateTime>absent(), nodeName
            , State.STARTED, Lists.<Transition<State>>newLinkedList(), VerificationMetadata.MODEL_VERSION);
    }

    public String getBackupId() {
        return backupId;
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
        return this.isState(State.FINISHED);
    }

    @JsonIgnore
    @Override
    public boolean isRunning() {
        return this.isState(State.STARTED);
    }

    @JsonIgnore
    @Override
    public boolean isFailed() {
        return this.isState(State.FAILED, State.TIMEDOUT);
    }

    @JsonIgnore
    @Override
    public boolean isFinished() {
        return this.isState(VerificationMetadata.State.FINISHED, VerificationMetadata.State.FAILED, VerificationMetadata.State.TIMEDOUT);
    }
    @Override
    public String toString() {
        final ToStringHelper helper = Objects.toStringHelper(this.getClass());

        helper.add("service", getService());
        helper.add("id", getId());
        helper.add("backupId", backupId);
        helper.add("state", getState());
        helper.add("startedDate", getStartedDate());

        if (getCompletedDate().isPresent()){
            helper.add("completedDate", getCompletedDate().get());
        }
        else {
            helper.add("completedDate", "running");
        }

        return helper.toString();
    }
}

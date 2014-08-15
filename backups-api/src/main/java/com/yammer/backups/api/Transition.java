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

@JsonIgnoreProperties(ignoreUnknown = true)
public class Transition<T> {

    private final T oldState;
    private final T newState;
    private final DateTime time;
    private final String comment;

    @JsonCreator
    public Transition(
            @JsonProperty("oldState") T oldState,
            @JsonProperty("newState") T newState,
            @JsonProperty("time") DateTime time,
            @JsonProperty("comment") String comment) {
        this.oldState = oldState;
        this.newState = newState;
        this.time = time;
        this.comment = comment;
    }

    public Transition(T oldState, T newState, String comment) {
        this(oldState, newState, DateTime.now(), comment);
    }

    public T getOldState() {
        return oldState;
    }

    public T getNewState() {
        return newState;
    }

    public DateTime getTime() {
        return time;
    }

    public String getComment() {
        return comment;
    }
}

package com.yammer.backups.config;

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
import io.dropwizard.util.Duration;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@SuppressWarnings("FieldCanBeLocal")
public class DistributedLockConfiguration {

    private static final int DEFAULT_THREAD_POOL_SIZE = 4;
    private static final Duration DEFAULT_ACQUIRE_TIMEOUT = Duration.minutes(1);
    private static final Duration DEFAULT_REFRESH_FREQUENCY = Duration.seconds(15);

    @Valid
    @Min(1)
    @Max(100)
    private int threadPoolSize = DEFAULT_THREAD_POOL_SIZE;

    @Valid
    @NotNull
    @JsonProperty
    private Duration acquireTimeout = DEFAULT_ACQUIRE_TIMEOUT;

    @Valid
    @NotNull
    @JsonProperty
    private Duration refreshFrequency = DEFAULT_REFRESH_FREQUENCY;

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public Duration getAcquireTimeout() {
        return acquireTimeout;
    }

    public Duration getRefreshFrequency() {
        return refreshFrequency;
    }
}

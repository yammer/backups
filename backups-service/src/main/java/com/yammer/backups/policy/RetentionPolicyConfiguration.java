package com.yammer.backups.policy;

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
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.util.Duration;

import javax.validation.constraints.Min;

public class RetentionPolicyConfiguration {

    @JsonProperty
    private final Duration minDuration;

    @JsonProperty
    @Min(0)
    private final int minCount;

    @JsonProperty
    @Min(0)
    private final int daily;

    @JsonProperty
    @Min(0)
    private final int weekly;

    @JsonProperty
    @Min(0)
    private final int monthly;

    @JsonProperty
    @Min(0)
    private final int yearly;

    @JsonCreator
    public RetentionPolicyConfiguration(
            @JsonProperty("minDuration") Duration minDuration,
            @JsonProperty("minCount") int minCount,
            @JsonProperty("daily") int daily,
            @JsonProperty("weekly") int weekly,
            @JsonProperty("monthly") int monthly,
            @JsonProperty("yearly") int yearly) {
        this.minDuration = minDuration;
        this.minCount = minCount;
        this.daily = daily;
        this.weekly = weekly;
        this.monthly = monthly;
        this.yearly = yearly;
    }

    public Duration getMinDuration() {
        return minDuration;
    }

    public int getMinCount() {
        return minCount;
    }

    public int getDaily() {
        return daily;
    }

    public int getWeekly() {
        return weekly;
    }

    public int getMonthly() {
        return monthly;
    }

    public int getYearly() {
        return yearly;
    }
}

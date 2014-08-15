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

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import com.yammer.backups.api.metadata.AbstractMetadata;
import io.dropwizard.util.Duration;
import org.joda.time.DateTime;
import org.joda.time.Seconds;

import java.util.Set;

public class LastDurationRetentionPolicy<T extends AbstractMetadata<?>> implements RetentionPolicy<T> {

    private final Duration duration;

    public LastDurationRetentionPolicy(Duration duration) {
        this.duration = duration;
    }

    protected long getAgeInSeconds(T item, DateTime now) {
        final DateTime date = item.getStartedDate();
        return Seconds.secondsBetween(date, now).getSeconds();
    }

    @Override
    public Set<T> retain(Set<T> items) {
        final DateTime now = DateTime.now();
        final long durationInSeconds = duration.toSeconds();

        return Sets.filter(items, new Predicate<T>() {
            @Override
            public boolean apply(T item) {
                final long ageInSeconds = getAgeInSeconds(item, now);
                return ageInSeconds < durationInSeconds;
            }
        });
    }

    @Override
    public String toString() {
        return "LastDurationRetentionPolicy{" +
                "duration=" + duration +
                '}';
    }
}

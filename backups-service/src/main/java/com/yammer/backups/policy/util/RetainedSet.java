package com.yammer.backups.policy.util;

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

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.yammer.backups.api.metadata.AbstractMetadata;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Seconds;

import java.util.Arrays;

public class RetainedSet<T extends AbstractMetadata<?>> {

    private final DateTime now;
    private final long secsRange;
    private final T[] items;

    @SuppressWarnings("unchecked")
    public RetainedSet(int size, DateTime now, int dayRange) {
        this.now = now;

        secsRange = Days.days(dayRange).toStandardSeconds().getSeconds();
        items = (T[]) new AbstractMetadata[size];
    }

    protected int getAge(T item, DateTime now) {
        final DateTime date = item.getStartedDate();
        final int difference = Seconds.secondsBetween(date, now).getSeconds();
        return (int) (difference / secsRange);
    }

    protected Optional<T> getBucket(int index) {
        return Optional.fromNullable(items[index]);
    }

    protected void setBucket(int index, T value) {
        items[index] = value;
    }

    protected void retain(T item, int bucket) {
        final Optional<T> existingItem = this.getBucket(bucket);

        // We've already retained an item for this period
        if (existingItem.isPresent()) {
            return;
        }

        this.setBucket(bucket, item);
    }

    public void retainIfRequired(T item) {
        if (items.length == 0) {
            return;
        }

        final int age = this.getAge(item, now);

        // This item is older than the retention policy
        if (age >= items.length) {
            return;
        }

        retain(item, age);
    }

    public Iterable<T> get() {
        return Collections2.filter(Arrays.asList(items), Predicates.notNull());
    }
}

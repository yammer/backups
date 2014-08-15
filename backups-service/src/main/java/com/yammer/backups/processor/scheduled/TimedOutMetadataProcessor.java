package com.yammer.backups.processor.scheduled;

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

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import com.yammer.backups.api.metadata.AbstractMetadata;
import com.yammer.backups.storage.metadata.MetadataStorage;
import io.dropwizard.util.Duration;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;

public class TimedOutMetadataProcessor<T extends AbstractMetadata<?>> extends AbstractScheduledProcessor {

    private static final Duration FREQUENCY = Duration.hours(1);
    private static final Duration INITIAL_DELAY = Duration.minutes(5);
    private static final Logger LOG = LoggerFactory.getLogger(TimedOutMetadataProcessor.class);

    private final MetadataStorage<T> metadataStorage;
    private final Duration expiration;
    private final String nodeName;

    public TimedOutMetadataProcessor(
            MetadataStorage<T> metadataStorage,
            ScheduledExecutorService executor,
            Duration expiration,
            String nodeName,
            MetricRegistry metricRegistry) {
        super(executor, FREQUENCY, INITIAL_DELAY, "timed-out-metadata", metricRegistry);

        this.metadataStorage = metadataStorage;
        this.expiration = expiration;
        this.nodeName = nodeName;
    }

    private void timeoutIfRequired(T item, DateTime now) {
        final DateTime started = item.getStartedDate();
        final int difference = Seconds.secondsBetween(started, now).getSeconds();
        if (difference >= expiration.toSeconds()) {
            LOG.debug("Timedout {}, started at {}", item, started);
            item.setTimedOut(String.format("Expired after %s seconds", difference));
            metadataStorage.update(item);
        }
    }

    @Override
    public void execute() {
        final DateTime now = DateTime.now();

        // Filter to only running items in current node
        final Collection<T> item = Sets.filter(metadataStorage.listAll(), new Predicate<T>() {
            @Override
            public boolean apply(T input) {
                return input.isRunning() && input.isAtNode(nodeName);
            }
        });

        for (T backup : item) {
            this.timeoutIfRequired(backup, now);
        }
    }
}

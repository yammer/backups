package com.yammer.backups.healthchecks.metadata;

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

import com.codahale.metrics.health.HealthCheck;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.yammer.backups.api.metadata.AbstractMetadata;
import com.yammer.backups.processor.ServiceRegistry;
import com.yammer.backups.storage.metadata.MetadataStorage;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class FailedMetadataHealthCheck<T extends AbstractMetadata<?>> extends HealthCheck {

    private static final Logger LOG = LoggerFactory.getLogger(FailedMetadataHealthCheck.class);
    private static final Duration ALERT_LAG = Duration.standardMinutes(10);

    private final MetadataStorage<T> metadataStorage;
    private final String nodeName;
    private final ServiceRegistry serviceRegistry;

    public FailedMetadataHealthCheck(MetadataStorage<T> metadataStorage, String nodeName, ServiceRegistry serviceRegistry) {
        this.metadataStorage = metadataStorage;
        this.nodeName = nodeName;
        this.serviceRegistry = serviceRegistry;
    }

    private Optional<T> findLatest(Iterable<T> items) {
        DateTime latestDate = new DateTime(0);
        T latestItem = null;

        for (T item : items) {
            final DateTime date = item.getStartedDate();
            if (date.isAfter(latestDate)) {
                latestDate = date;
                latestItem = item;
            }
        }

        return Optional.fromNullable(latestItem);
    }

    @Override
    protected Result check() {
        final Set<T> failedItems = Sets.newHashSet();

        // We are interested only in the status of the last (finished) backup for each service
        for (String service : metadataStorage.listAllRows()) {
            if (serviceRegistry.healthCheckDisabled(service)) {
                LOG.trace("{} has healthchecks disabled, skipping", service);
                continue;
            }
            final Set<T> items = metadataStorage.listAll(service);

            final Optional<T> optionalItem = findLatest(items);
            // There are no items that are finished
            if (!optionalItem.isPresent()) {
                LOG.trace("{} has no finished backups, skipping", service);
                continue;
            }

            final T item = optionalItem.get();

            // If the latest one isn't from this node, then it isn't our responsibility
            if (!item.isAtNode(nodeName)) {
                LOG.trace("{} isn't our responsibility, skipping", item);
                continue;
            }

            // The latest one passed, so don't alert
            if (!item.isFailed()) {
                LOG.trace("{} isn't failed, skipping", item);
                continue;
            }

            // If this backup completed within the last ALERT_LAG duration, then don't alert yet.
            // This is to avoid flapping alerts in between retry attempts. Really it should  be done
            // by the monitoring system not us...
            final Optional<DateTime> completedDate = item.getCompletedDate();
            if (completedDate.isPresent()) {
                final Duration duration = new Interval(completedDate.get(), DateTime.now()).toDuration();
                if (duration.isShorterThan(ALERT_LAG)) {
                    LOG.debug("Found failed item {}, but suppressing as it's only {} old", item, duration);
                    continue;
                }
            }

            LOG.warn("Found failed item {}", item);
            failedItems.add(item);
        }

        if (!failedItems.isEmpty()) {
            return Result.unhealthy("Failed: %s ", failedItems);
        }

        return Result.healthy();
    }
}

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
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import com.yammer.backups.api.metadata.AbstractMetadata;
import com.yammer.backups.processor.ServiceRegistry;
import com.yammer.backups.storage.metadata.MetadataStorage;
import io.dropwizard.util.Duration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class OverdueMetadataHealthCheck<T extends AbstractMetadata<?>> extends HealthCheck {

    private static final Logger LOG = LoggerFactory.getLogger(OverdueMetadataHealthCheck.class);

    private final MetadataStorage<T> metadataStorage;
    private final Duration requiredFrequency;
    private final String nodeName;
    private final ServiceRegistry serviceRegistry;

    public OverdueMetadataHealthCheck(
            MetadataStorage<T> metadataStorage,
            Duration requiredFrequency,
            String nodeName, ServiceRegistry serviceRegistry) {
        this.metadataStorage = metadataStorage;
        this.requiredFrequency = requiredFrequency;
        this.nodeName = nodeName;
        this.serviceRegistry = serviceRegistry;
    }

    private DateTime getDueDate() {
        final DateTime date = DateTime.now();
        return date.minusSeconds((int) requiredFrequency.toSeconds());
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

    private boolean isOverdue(Iterable<T> items, DateTime dueDate) {
        for (T item : items) {
            final DateTime startedDate = item.getStartedDate();
            if (startedDate.isAfter(dueDate)) {
                // started since the due date, so we're not overdue
                return false;
            }
        }

        return true;
    }

    private boolean isOverdue(String service, DateTime dueDate) {
        // Only look at finished items
        final Set<T> items =  Sets.filter(metadataStorage.listAll(service), new Predicate<T>() {
            @Override
            public boolean apply(T input) {
                return input != null && (input.isSuccessful() || input.isFailed());
            }
        });

        final boolean overdue = isOverdue(items, dueDate);
        // It isn't overdue anyway so don't bother the below checks
        if (!overdue) {
            return false;
        }

        // If we got this far then we have finished backups that aren't in the past due duration

        // Sort by date and take the most recent one
        final Optional<T> item = findLatest(items);

        // If there are no items then it can't be overdue
        if (!item.isPresent()) {
            return false;
        }

        // If we're responsible for it, then alert
        return item.get().isAtNode(nodeName);
    }

    @Override
    protected Result check() {
        final DateTime dueDate = getDueDate();
        final Set<String> overdueItems = Sets.newHashSet();

        final Set<String> services = metadataStorage.listAllRows();
        for (String service : services) {
            if (serviceRegistry.healthCheckDisabled(service)) {
                LOG.trace("{} has healthchecks disabled, skipping", service);
                continue;
            }
            if (!this.isOverdue(service, dueDate)) {
                // skip not overdue services
                LOG.trace("{} isn't overdue, skipping", service);
                continue;
            }

            LOG.warn("Found overdue service {}, was due {}", service, dueDate);
            overdueItems.add(service);
        }

        if (!overdueItems.isEmpty()) {
            return Result.unhealthy("Overdue: %s (were due by: %s)", overdueItems, dueDate);
        }

        return Result.healthy();
    }
}

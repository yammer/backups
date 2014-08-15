package com.yammer.backups.healthchecks;

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
import com.codahale.metrics.health.HealthCheck;
import com.google.common.base.Supplier;
import com.yammer.backups.processor.scheduled.AbstractScheduledProcessor;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

public final class ScheduledHealthCheck extends HealthCheck implements Managed {

    private static final Duration INITIAL_DELAY = Duration.seconds(0);
    private static final Logger LOG = LoggerFactory.getLogger(ScheduledHealthCheck.class);

    public static ScheduledHealthCheck wrap(HealthCheck delegate, String name, ScheduledExecutorService executorService, Duration frequency, MetricRegistry metricRegistry) {
        return new ScheduledHealthCheck(delegate, name, executorService, frequency, metricRegistry);
    }

    private final AtomicReference<Result> result;
    private final Supplier<Result> supplier;
    private final AbstractScheduledProcessor processor;

    private ScheduledHealthCheck(final HealthCheck delegate, final String name, final ScheduledExecutorService executor, final Duration frequency, final MetricRegistry metricRegistry) {
        result = new AtomicReference<>(Result.unhealthy("Scheduled health check not yet checked."));
        supplier = new Supplier<Result>() {
            @Override
            public Result get() {
                LOG.trace("Running scheduled health check: {}", name);
                return delegate.execute();
            }
        };

        processor = new AbstractScheduledProcessor(executor, frequency, INITIAL_DELAY, name, metricRegistry) {
            @Override
            public void execute() {
                result.set(supplier.get());
            }
        };
    }

    @Override
    public void start() {
        processor.start();
    }

    @Override
    public void stop() {
        processor.stop();
    }

    @Override
    protected Result check() {
        return result.get();
    }
}

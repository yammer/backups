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
import com.codahale.metrics.Timer;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.util.Duration;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class AbstractScheduledProcessor implements Managed {

    private final ScheduledExecutorService executor;
    private final Duration frequency;
    private final Duration initialDelay;
    private final Timer timer;

    private ScheduledFuture<?> future;

    protected AbstractScheduledProcessor(ScheduledExecutorService executor, Duration frequency, Duration initialDelay, String name, MetricRegistry metricRegistry) {
        this.executor = executor;
        this.frequency = frequency;
        this.initialDelay = initialDelay;

        timer = metricRegistry.timer(String.format("scheduled-processor-%s", name));
    }

    protected abstract void execute();

    @Override
    public void start() {
        future = executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                final Timer.Context context = timer.time();
                try {
                    execute();
                }
                finally {
                    context.stop();
                }
            }
        }, initialDelay.toSeconds(), frequency.toSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        if (future != null) {
            future.cancel(false);
        }
    }
}

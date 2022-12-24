/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.provisioning.java.pushpull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;
import org.apache.syncope.core.persistence.api.entity.task.ProvisioningTask;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningActions;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopeResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public abstract class SyncopeResultHandlerDispatcher<
        T extends ProvisioningTask<?>, A extends ProvisioningActions, RA extends SyncopeResultHandler<T, A>> {

    protected static final Logger LOG = LoggerFactory.getLogger(SyncopeResultHandlerDispatcher.class);

    protected final Optional<ThreadPoolTaskExecutor> tpte;

    protected final Optional<ExecutorCompletionService<Void>> ecs;

    protected final Map<String, Supplier<RA>> suppliers = new ConcurrentHashMap<>();

    protected final Map<String, RA> handlers = new ConcurrentHashMap<>();

    protected final List<Future<Void>> futures = new ArrayList<>();

    public SyncopeResultHandlerDispatcher(final ProvisioningProfile<T, A> profile) {
        if (profile.getTask().getConcurrentSettings() == null) {
            tpte = Optional.empty();
            ecs = Optional.empty();
        } else {
            ThreadPoolTaskExecutor t = new ThreadPoolTaskExecutor();
            t.setCorePoolSize(profile.getTask().getConcurrentSettings().getCorePoolSize());
            t.setMaxPoolSize(profile.getTask().getConcurrentSettings().getMaxPoolSize());
            t.setQueueCapacity(profile.getTask().getConcurrentSettings().getQueueCapacity());
            t.setWaitForTasksToCompleteOnShutdown(true);
            t.setThreadNamePrefix("provisioningTask-" + profile.getTask().getKey() + "-");
            t.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            t.setTaskDecorator(d -> () -> {
                SecurityContextHolder.getContext().setAuthentication(authentication);
                d.run();
            });

            t.initialize();
            this.tpte = Optional.of(t);

            this.ecs = Optional.of(new ExecutorCompletionService<>(t.getThreadPoolExecutor()));
        }
    }

    public void addHandlerSupplier(final String key, final Supplier<RA> supplier) {
        suppliers.put(key, supplier);
    }

    protected RA nonConcurrentHandler(final String key) {
        return Optional.ofNullable(handlers.get(key)).orElseGet(() -> {
            RA h = suppliers.get(key).get();
            handlers.put(key, h);
            return h;
        });
    }

    protected void submit(final Runnable runnable) {
        if (ecs.isPresent()) {
            futures.add(ecs.get().submit(runnable, null));
        }
    }

    protected void cleanup() {
        for (Future<Void> f : this.futures) {
            try {
                f.get();
            } catch (ExecutionException | InterruptedException e) {
                LOG.error("Unexpected error when waiting for completion", e);
            }
        }

        tpte.ifPresent(ThreadPoolTaskExecutor::shutdown);
    }
}

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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;
import org.apache.syncope.core.persistence.api.entity.task.ProvisioningTask;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningActions;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopeResultHandler;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.SyncopeAuthenticationDetails;
import org.apache.syncope.core.spring.security.SyncopeGrantedAuthority;
import org.apache.syncope.core.spring.task.VirtualThreadPoolTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

public abstract class SyncopeResultHandlerDispatcher<
        T extends ProvisioningTask<?>, A extends ProvisioningActions, RA extends SyncopeResultHandler<T, A>> {

    protected static final Logger LOG = LoggerFactory.getLogger(SyncopeResultHandlerDispatcher.class);

    private static final String PLACEHOLDER_PWD = "PLACEHOLDER_PWD";

    protected final Optional<VirtualThreadPoolTaskExecutor> tpte;

    protected final Map<String, Supplier<RA>> suppliers = new ConcurrentHashMap<>();

    protected final Map<String, RA> handlers = new ConcurrentHashMap<>();

    protected final List<Future<?>> futures = new ArrayList<>();

    protected SyncopeResultHandlerDispatcher(final ProvisioningProfile<T, A> profile) {
        if (profile.getTask().getConcurrentSettings() == null) {
            tpte = Optional.empty();
        } else {
            VirtualThreadPoolTaskExecutor t = new VirtualThreadPoolTaskExecutor();
            t.setPoolSize(profile.getTask().getConcurrentSettings().getPoolSize());
            t.setWaitForTasksToCompleteOnShutdown(true);
            t.setThreadNamePrefix("provisioningTask-" + profile.getTask().getKey() + "-");
            t.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());

            String domain = AuthContextUtils.getDomain();
            String delegatedBy = AuthContextUtils.getDelegatedBy().orElse(null);
            Set<SyncopeGrantedAuthority> authorities = AuthContextUtils.getAuthorities();
            t.setTaskDecorator(d -> () -> {
                // set placeholder authentication object by creating fresh and copying data from caller's
                UsernamePasswordAuthenticationToken placeHolderAuth = new UsernamePasswordAuthenticationToken(
                        new User(profile.getExecutor(), PLACEHOLDER_PWD, authorities), PLACEHOLDER_PWD, authorities);
                placeHolderAuth.setDetails(new SyncopeAuthenticationDetails(domain, delegatedBy));
                SecurityContextHolder.getContext().setAuthentication(placeHolderAuth);

                d.run();
            });

            t.initialize();

            tpte = Optional.of(t);
        }
    }

    public void addHandlerSupplier(final String key, final Supplier<RA> supplier) {
        suppliers.put(key, supplier);
    }

    protected RA nonConcurrentHandler(final String key) {
        return handlers.computeIfAbsent(key, k -> suppliers.get(k).get());
    }

    protected void submit(final Runnable runnable) {
        tpte.ifPresent(executor -> futures.add(executor.submit(runnable)));
    }

    public void stop() {
        handlers.values().forEach(SyncopeResultHandler::stop);
    }

    protected void shutdown() {
        for (Future<?> f : this.futures) {
            try {
                f.get();
            } catch (ExecutionException | InterruptedException e) {
                LOG.error("Unexpected error when waiting for completion", e);
            }
        }

        tpte.ifPresent(VirtualThreadPoolTaskExecutor::shutdown);
    }
}

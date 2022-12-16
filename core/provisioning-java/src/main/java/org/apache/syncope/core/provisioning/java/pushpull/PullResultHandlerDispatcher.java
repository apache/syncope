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

import java.util.Optional;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.PullActions;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePullExecutor;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePullResultHandler;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class PullResultHandlerDispatcher implements SyncResultsHandler {

    protected static final Logger LOG = LoggerFactory.getLogger(PullResultHandlerDispatcher.class);

    protected final SyncopePullExecutor executor;

    protected SyncopePullResultHandler handler;

    protected final Optional<ThreadPoolTaskExecutor> tpte;

    public PullResultHandlerDispatcher(
            final ProvisioningProfile<PullTask, PullActions> profile,
            final SyncopePullExecutor executor) {

        this.executor = executor;

        this.tpte = Optional.ofNullable(profile.getTask().getConcurrentSettings()).map(s -> {
            ThreadPoolTaskExecutor t = new ThreadPoolTaskExecutor();
            t.setCorePoolSize(s.getCorePoolSize());
            t.setMaxPoolSize(s.getMaxPoolSize());
            t.setKeepAliveSeconds(s.getKeepAliveSeconds());
            t.setQueueCapacity(s.getQueueCapacity());
            t.setAllowCoreThreadTimeOut(s.isAllowCoreThreadTimeOut());
            t.setPrestartAllCoreThreads(s.isPrestartAllCoreThreads());
            t.setWaitForTasksToCompleteOnShutdown(s.isWaitForTasksToCompleteOnShutdown());
            t.setAwaitTerminationSeconds(s.getAwaitTerminationSeconds());
            t.setThreadNamePrefix("pullTask-" + profile.getTask().getKey() + "-");
            t.setRejectedExecutionHandler(s.getRejectionPolicy().getHandler());
            t.initialize();
            return t;
        });
    }

    public void setHandler(final SyncopePullResultHandler handler) {
        this.handler = handler;
    }

    @Override
    public boolean handle(final SyncDelta delta) {
        if (executor.wasInterruptRequested()) {
            LOG.debug("Pull interrupted");
            executor.setInterrupted();
            return false;
        }

        if (tpte.isEmpty()) {
            boolean result = handler.handle(delta);

            executor.reportHandled(delta.getObjectClass().getObjectClassValue(), delta.getObject().getName());
            if (result) {
                executor.setLatestSyncToken(delta.getObjectClass().getObjectClassValue(), delta.getToken());
            }

            return result;
        }

        tpte.get().submit(() -> {
            executor.setLatestSyncToken(delta.getObjectClass().getObjectClassValue(), delta.getToken());

            handler.handle(delta);
            executor.reportHandled(delta.getObjectClass().getObjectClassValue(), delta.getObject().getName());
        });
        return true;
    }

    public void cleanup() {
        tpte.ifPresent(ThreadPoolTaskExecutor::shutdown);
    }
}

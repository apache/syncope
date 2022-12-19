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
import org.apache.syncope.core.persistence.api.entity.task.ProvisioningTask;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningActions;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopeResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public abstract class SyncopeResultHandlerDispatcher<
        T extends ProvisioningTask<?>, A extends ProvisioningActions, SRA extends SyncopeResultHandler<T, A>> {

    protected static final Logger LOG = LoggerFactory.getLogger(SyncopeResultHandlerDispatcher.class);

    protected final Optional<ThreadPoolTaskExecutor> tpte;

    protected SRA handler;

    public SyncopeResultHandlerDispatcher(final ProvisioningProfile<T, A> profile) {
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
            t.setThreadNamePrefix("provisioningTask-" + profile.getTask().getKey() + "-");
            t.setRejectedExecutionHandler(s.getRejectionPolicy().getHandler());
            t.initialize();
            return t;
        });
    }

    public void setHandler(final SRA handler) {
        this.handler = handler;
    }

    public void cleanup() {
        tpte.ifPresent(ThreadPoolTaskExecutor::shutdown);
    }
}

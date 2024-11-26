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
package org.apache.syncope.core.provisioning.java.propagation;

import java.util.Collection;
import java.util.stream.Collectors;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskCallable;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class DefaultPropagationTaskCallable implements PropagationTaskCallable {

    protected static final Logger LOG = LoggerFactory.getLogger(PropagationTaskCallable.class);

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    protected final String domain;

    protected final Collection<String> authorities;

    protected PropagationTaskInfo taskInfo;

    protected PropagationReporter reporter;

    protected String executor;

    public DefaultPropagationTaskCallable() {
        SecurityContext ctx = SecurityContextHolder.getContext();
        domain = AuthContextUtils.getDomain();
        authorities = ctx.getAuthentication().getAuthorities().stream().
                map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }

    @Override
    public void setTaskInfo(final PropagationTaskInfo taskInfo) {
        this.taskInfo = taskInfo;
    }

    @Override
    public void setReporter(final PropagationReporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public void setExecutor(final String executor) {
        this.executor = executor;
    }

    @Override
    public TaskExec<PropagationTask> call() {
        return AuthContextUtils.callAs(domain, executor, authorities, () -> {
            LOG.debug("Execution started for {}", taskInfo);

            TaskExec<PropagationTask> execution = taskExecutor.execute(taskInfo, reporter, executor);

            LOG.debug("Execution completed for {}, {}", taskInfo, execution);

            return execution;
        });
    }
}

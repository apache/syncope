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

import java.util.concurrent.RejectedExecutionException;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.PushActions;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePushExecutor;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePushResultHandler;

public class PushResultHandlerDispatcher
        extends SyncopeResultHandlerDispatcher<PushTask, PushActions, SyncopePushResultHandler> {

    protected final SyncopePushExecutor executor;

    public PushResultHandlerDispatcher(
            final ProvisioningProfile<PushTask, PushActions> profile,
            final SyncopePushExecutor executor) {

        super(profile);
        this.executor = executor;
    }

    public boolean handle(final String anyType, final String anyKey) {
        if (tpte.isEmpty()) {
            boolean result = nonConcurrentHandler(anyType).handle(anyKey);

            executor.reportHandled(anyType, anyKey);

            return result;
        }

        try {
            submit(() -> {
                suppliers.get(anyType).get().handle(anyKey);

                executor.reportHandled(anyType, anyKey);
            });
            return true;
        } catch (RejectedExecutionException e) {
            LOG.error("Could not submit push handler for {} {}", anyType, anyKey);
            return false;
        }
    }
}

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
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.provisioning.api.pushpull.InboundActions;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePullExecutor;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePullResultHandler;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;

public class PullResultHandlerDispatcher
        extends SyncopeResultHandlerDispatcher<PullTask, InboundActions, SyncopePullResultHandler>
        implements SyncResultsHandler {

    protected final SyncopePullExecutor executor;

    public PullResultHandlerDispatcher(
            final ProvisioningProfile<PullTask, InboundActions> profile,
            final SyncopePullExecutor executor) {

        super(profile);
        this.executor = executor;
    }

    @Override
    public boolean handle(final SyncDelta delta) {
        if (tpte.isEmpty()) {
            boolean result = nonConcurrentHandler(delta.getObjectClass().getObjectClassValue()).handle(delta);

            executor.reportHandled(delta.getObjectClass().getObjectClassValue(), delta.getObject().getName());
            if (result) {
                executor.setLatestSyncToken(delta.getObjectClass().getObjectClassValue(), delta.getToken());
            }

            return result;
        }

        try {
            submit(() -> {
                executor.setLatestSyncToken(delta.getObjectClass().getObjectClassValue(), delta.getToken());

                suppliers.get(delta.getObjectClass().getObjectClassValue()).get().handle(delta);

                executor.reportHandled(delta.getObjectClass().getObjectClassValue(), delta.getObject().getName());
            });
            return true;
        } catch (RejectedExecutionException e) {
            LOG.error("Could not submit pull handler for {} {}",
                    delta.getObjectClass().getObjectClassValue(), delta.getObject().getName());
            return false;
        }
    }
}

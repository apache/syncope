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
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.PushActions;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopeAnyPushResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePushExecutor;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePushResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopeRealmPushResultHandler;
import org.springframework.transaction.annotation.Transactional;

public class PushResultHandlerDispatcher
        extends SyncopeResultHandlerDispatcher<PushTask, PushActions, SyncopePushResultHandler> {

    protected SyncopePushExecutor executor;

    public PushResultHandlerDispatcher init(
            final ProvisioningProfile<PushTask, PushActions> profile,
            final SyncopePushExecutor executor) {

        init(profile);
        this.executor = executor;

        return this;
    }

    @Transactional(readOnly = true)
    public boolean handle(final Any any) {
        if (tpte.isEmpty()) {
            boolean result = ((SyncopeAnyPushResultHandler) nonConcurrentHandler(any.getType().getKey())).handle(any);

            executor.reportHandled(any.getType().getKey(), any.getKey());

            return result;
        }

        try {
            submit(() -> {
                ((SyncopeAnyPushResultHandler) suppliers.get(any.getType().getKey()).get()).handle(any);

                executor.reportHandled(any.getType().getKey(), any.getKey());
            });
            return true;
        } catch (RejectedExecutionException e) {
            LOG.error("Could not submit push handler for {} {}", any.getType().getKey(), any.getKey());
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean handle(final Realm realm) {
        if (tpte.isEmpty()) {
            boolean result = ((SyncopeRealmPushResultHandler) nonConcurrentHandler(SyncopeConstants.REALM_ANYTYPE)).
                    handle(realm);

            executor.reportHandled(SyncopeConstants.REALM_ANYTYPE, realm.getKey());

            return result;
        }

        try {
            submit(() -> {
                ((SyncopeRealmPushResultHandler) suppliers.get(SyncopeConstants.REALM_ANYTYPE).get()).handle(realm);

                executor.reportHandled(SyncopeConstants.REALM_ANYTYPE, realm.getKey());
            });
            return true;
        } catch (RejectedExecutionException e) {
            LOG.error("Could not submit push handler for {} {}", SyncopeConstants.REALM_ANYTYPE, realm.getKey());
            return false;
        }
    }
}

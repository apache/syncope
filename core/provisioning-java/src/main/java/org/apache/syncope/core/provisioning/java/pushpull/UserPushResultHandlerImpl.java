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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningReport;
import org.apache.syncope.core.provisioning.api.pushpull.UserPushResultHandler;

public class UserPushResultHandlerImpl extends AbstractPushResultHandler implements UserPushResultHandler {

    @Override
    protected AnyUtils getAnyUtils() {
        return anyUtilsFactory.getInstance(AnyTypeKind.USER);
    }

    @Override
    protected void provision(final Any<?> any, final Boolean enabled, final ProvisioningReport result) {
        AnyTO before = getAnyTO(any.getKey());

        List<String> noPropResources = new ArrayList<>(before.getResources());
        noPropResources.remove(profile.getTask().getResource().getKey());

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.add(ResourceOperation.CREATE, profile.getTask().getResource().getKey());

        PropagationReporter reporter = taskExecutor.execute(propagationManager.getUserCreateTasks(
                before.getKey(),
                null,
                enabled,
                propByRes,
                before.getVirAttrs(),
                noPropResources),
                false);
        reportPropagation(result, reporter);
    }

    @Override
    protected String getName(final Any<?> any) {
        return User.class.cast(any).getUsername();
    }

    @Override
    protected Any<?> getAny(final String key) {
        try {
            return userDAO.authFind(key);
        } catch (Exception e) {
            LOG.warn("Error retrieving user {}", key, e);
            return null;
        }
    }

    @Override
    protected AnyTO getAnyTO(final String key) {
        return userDataBinder.getUserTO(key);
    }

    @Override
    protected AnyPatch newPatch(final String key) {
        UserPatch patch = new UserPatch();
        patch.setKey(key);
        return patch;
    }

    @Override
    protected WorkflowResult<? extends AnyPatch> update(final AnyPatch patch) {
        WorkflowResult<Pair<UserPatch, Boolean>> update = uwfAdapter.update((UserPatch) patch);
        return new WorkflowResult<>(update.getResult().getLeft(), update.getPropByRes(), update.getPerformedTasks());
    }

}

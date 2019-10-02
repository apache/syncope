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
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningReport;
import org.apache.syncope.core.provisioning.api.pushpull.UserPushResultHandler;
import org.apache.syncope.core.provisioning.java.propagation.DefaultPropagationReporter;
import org.identityconnectors.framework.common.objects.ConnectorObject;

public class DefaultUserPushResultHandler extends AbstractPushResultHandler implements UserPushResultHandler {

    @Override
    protected AnyUtils getAnyUtils() {
        return anyUtilsFactory.getInstance(AnyTypeKind.USER);
    }

    @Override
    protected String getName(final Any<?> any) {
        return User.class.cast(any).getUsername();
    }

    @Override
    protected AnyTO getAnyTO(final String key) {
        return userDataBinder.getUserTO(key);
    }

    @Override
    protected void provision(final Any<?> any, final Boolean enabled, final ProvisioningReport result) {
        AnyTO before = getAnyTO(any.getKey());

        List<String> noPropResources = new ArrayList<>(before.getResources());
        noPropResources.remove(profile.getTask().getResource().getKey());

        PropagationByResource<String> propByRes = new PropagationByResource<>();
        propByRes.add(ResourceOperation.CREATE, profile.getTask().getResource().getKey());

        PropagationByResource<Pair<String, String>> propByLinkedAccount = new PropagationByResource<>();
        ((User) any).getLinkedAccounts(profile.getTask().getResource().getKey()).
                forEach(account -> propByLinkedAccount.add(
                ResourceOperation.CREATE,
                Pair.of(account.getResource().getKey(), account.getConnObjectKeyValue())));

        PropagationReporter reporter = taskExecutor.execute(propagationManager.getUserCreateTasks(
                before.getKey(),
                null,
                enabled,
                propByRes,
                propByLinkedAccount,
                before.getVirAttrs(),
                noPropResources),
                false);
        reportPropagation(result, reporter);
    }

    @Override
    protected void update(
            final Any<?> any,
            final Boolean enable,
            final ConnectorObject beforeObj,
            final ProvisioningReport result) {

        List<String> ownedResources = getAnyUtils().getAllResources(any).stream().
                map(Entity::getKey).collect(Collectors.toList());

        List<String> noPropResources = new ArrayList<>(ownedResources);
        noPropResources.remove(profile.getTask().getResource().getKey());

        PropagationByResource<String> propByRes = new PropagationByResource<>();
        propByRes.add(ResourceOperation.UPDATE, profile.getTask().getResource().getKey());
        propByRes.addOldConnObjectKey(profile.getTask().getResource().getKey(), beforeObj.getUid().getUidValue());

        PropagationByResource<Pair<String, String>> propByLinkedAccount = new PropagationByResource<>();
        ((User) any).getLinkedAccounts(profile.getTask().getResource().getKey()).
                forEach(account -> propByLinkedAccount.add(
                ResourceOperation.UPDATE,
                Pair.of(account.getResource().getKey(), account.getConnObjectKeyValue())));

        List<PropagationTaskInfo> taskInfos = propagationManager.getUpdateTasks(
                any.getType().getKind(),
                any.getKey(),
                true,
                enable,
                propByRes,
                propByLinkedAccount,
                null,
                noPropResources);
        if (!taskInfos.isEmpty()) {
            taskInfos.get(0).setBeforeObj(Optional.of(beforeObj));
            PropagationReporter reporter = new DefaultPropagationReporter();
            taskExecutor.execute(taskInfos.get(0), reporter);
            reportPropagation(result, reporter);
        }
    }

    @Override
    protected void deprovision(final Any<?> any, final ConnectorObject beforeObj, final ProvisioningReport result) {
        AnyTO before = getAnyTO(any.getKey());

        List<String> noPropResources = new ArrayList<>(before.getResources());
        noPropResources.remove(profile.getTask().getResource().getKey());

        PropagationByResource<String> propByRes = new PropagationByResource<>();
        propByRes.add(ResourceOperation.DELETE, profile.getTask().getResource().getKey());
        propByRes.addOldConnObjectKey(profile.getTask().getResource().getKey(), beforeObj.getUid().getUidValue());

        PropagationByResource<Pair<String, String>> propByLinkedAccount = new PropagationByResource<>();
        ((User) any).getLinkedAccounts(profile.getTask().getResource().getKey()).
                forEach(account -> propByLinkedAccount.add(
                ResourceOperation.DELETE,
                Pair.of(account.getResource().getKey(), account.getConnObjectKeyValue())));

        List<PropagationTaskInfo> taskInfos = propagationManager.getDeleteTasks(
                any.getType().getKind(),
                any.getKey(),
                propByRes,
                propByLinkedAccount,
                noPropResources);
        if (!taskInfos.isEmpty()) {
            taskInfos.get(0).setBeforeObj(Optional.of(beforeObj));
            PropagationReporter reporter = new DefaultPropagationReporter();
            taskExecutor.execute(taskInfos.get(0), reporter);
            reportPropagation(result, reporter);
        }
    }

    @Override
    protected WorkflowResult<? extends AnyPatch> update(final AnyPatch patch) {
        WorkflowResult<Pair<UserPatch, Boolean>> update = uwfAdapter.update((UserPatch) patch);
        return new WorkflowResult<>(update.getResult().getLeft(), update.getPropByRes(), update.getPerformedTasks());
    }
}

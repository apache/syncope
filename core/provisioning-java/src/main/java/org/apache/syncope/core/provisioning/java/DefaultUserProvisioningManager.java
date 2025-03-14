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
package org.apache.syncope.core.provisioning.java;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.StatusR;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.StatusRType;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.UserWorkflowResult;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;
import org.identityconnectors.framework.common.objects.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class DefaultUserProvisioningManager implements UserProvisioningManager {

    protected static final Logger LOG = LoggerFactory.getLogger(UserProvisioningManager.class);

    protected final UserWorkflowAdapter uwfAdapter;

    protected final PropagationManager propagationManager;

    protected final PropagationTaskExecutor taskExecutor;

    protected final UserDAO userDAO;

    protected final VirAttrHandler virtAttrHandler;

    public DefaultUserProvisioningManager(
            final UserWorkflowAdapter uwfAdapter,
            final PropagationManager propagationManager,
            final PropagationTaskExecutor taskExecutor,
            final UserDAO userDAO,
            final VirAttrHandler virtAttrHandler) {

        this.uwfAdapter = uwfAdapter;
        this.propagationManager = propagationManager;
        this.taskExecutor = taskExecutor;
        this.userDAO = userDAO;
        this.virtAttrHandler = virtAttrHandler;
    }

    @Override
    public Pair<String, List<PropagationStatus>> create(
            final UserCR userCR, final boolean nullPriorityAsync, final String creator, final String context) {

        return create(userCR, false, null, Set.of(), nullPriorityAsync, creator, context);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public Pair<String, List<PropagationStatus>> create(
            final UserCR userCR,
            final boolean disablePwdPolicyCheck,
            final Boolean enabled,
            final Set<String> excludedResources,
            final boolean nullPriorityAsync,
            final String creator,
            final String context) {

        UserWorkflowResult<Pair<String, Boolean>> created =
                uwfAdapter.create(userCR, disablePwdPolicyCheck, enabled, creator, context);

        List<PropagationTaskInfo> taskInfos = propagationManager.getUserCreateTasks(
                created.getResult().getLeft(),
                userCR.getPassword(),
                created.getResult().getRight(),
                created.getPropByRes(),
                created.getPropByLinkedAccount(),
                userCR.getVirAttrs(),
                excludedResources);
        PropagationReporter propagationReporter = taskExecutor.execute(taskInfos, nullPriorityAsync, creator);

        return Pair.of(created.getResult().getLeft(), propagationReporter.getStatuses());
    }

    @Override
    public Pair<UserUR, List<PropagationStatus>> update(
            final UserUR userUR, final boolean nullPriorityAsync, final String updater, final String context) {

        Map<Pair<String, String>, Set<Attribute>> beforeAttrs = propagationManager.prepareAttrs(
                AnyTypeKind.USER,
                userUR.getKey(),
                Optional.ofNullable(userUR.getPassword()).map(PasswordPatch::getValue).orElse(null),
                userUR.getPassword() == null ? List.of() : userUR.getPassword().getResources(),
                null,
                Set.of());

        UserWorkflowResult<Pair<UserUR, Boolean>> updated = uwfAdapter.update(userUR, null, updater, context);

        List<PropagationTaskInfo> taskInfos = propagationManager.setAttributeDeltas(
                propagationManager.getUserUpdateTasks(updated),
                beforeAttrs);
        PropagationReporter propagationReporter = taskExecutor.execute(taskInfos, nullPriorityAsync, updater);

        return Pair.of(updated.getResult().getLeft(), propagationReporter.getStatuses());
    }

    @Override
    public Pair<UserUR, List<PropagationStatus>> update(
            final UserUR userUR,
            final Set<String> excludedResources,
            final boolean nullPriorityAsync,
            final String updater,
            final String context) {

        return update(userUR, new ProvisioningReport(), null, excludedResources, nullPriorityAsync, updater, context);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public Pair<UserUR, List<PropagationStatus>> update(
            final UserUR userUR,
            final ProvisioningReport result,
            final Boolean enabled,
            final Set<String> excludedResources,
            final boolean nullPriorityAsync,
            final String updater,
            final String context) {

        Map<Pair<String, String>, Set<Attribute>> beforeAttrs = propagationManager.prepareAttrs(
                AnyTypeKind.USER,
                userUR.getKey(),
                Optional.ofNullable(userUR.getPassword()).map(PasswordPatch::getValue).orElse(null),
                userUR.getPassword() == null ? List.of() : userUR.getPassword().getResources(),
                enabled,
                excludedResources);

        UserWorkflowResult<Pair<UserUR, Boolean>> updated;
        try {
            updated = uwfAdapter.update(userUR, enabled, updater, context);
        } catch (Exception e) {
            LOG.error("Update of user {} failed, trying to pull its status anyway (if configured)",
                    userUR.getKey(), e);

            result.setStatus(ProvisioningReport.Status.FAILURE);
            result.setMessage("Update failed, trying to pull status anyway (if configured)\n" + e.getMessage());

            updated = new UserWorkflowResult<>(
                    Pair.of(userUR, false),
                    new PropagationByResource<>(),
                    new PropagationByResource<>(),
                    new HashSet<>());
        }

        List<PropagationTaskInfo> taskInfos = propagationManager.setAttributeDeltas(
                propagationManager.getUserUpdateTasks(
                        updated,
                        updated.getResult().getLeft().getPassword() != null
                                ? updated.getResult().getLeft().getPassword().getResources()
                                : List.of(),
                        excludedResources),
                beforeAttrs);
        PropagationReporter propagationReporter = taskExecutor.execute(taskInfos, nullPriorityAsync, updater);

        return Pair.of(updated.getResult().getLeft(), propagationReporter.getStatuses());
    }

    @Override
    public List<PropagationStatus> delete(
            final String key, final boolean nullPriorityAsync, final String eraser, final String context) {

        return delete(key, Set.of(), nullPriorityAsync, eraser, context);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public List<PropagationStatus> delete(
            final String key,
            final Set<String> excludedResources,
            final boolean nullPriorityAsync,
            final String eraser,
            final String context) {

        PropagationByResource<String> propByRes = new PropagationByResource<>();
        propByRes.set(ResourceOperation.DELETE, userDAO.findAllResourceKeys(key));

        PropagationByResource<Pair<String, String>> propByLinkedAccount = new PropagationByResource<>();
        userDAO.findLinkedAccounts(key).forEach(account -> propByLinkedAccount.add(
                ResourceOperation.DELETE,
                Pair.of(account.getResource().getKey(), account.getConnObjectKeyValue())));

        // Note here that we can only notify about "delete", not any other
        // task defined in workflow process definition: this because this
        // information could only be available after uwfAdapter.delete(), which
        // will also effectively remove user from db, thus making virtually
        // impossible by NotificationManager to fetch required user information
        List<PropagationTaskInfo> taskInfos = propagationManager.getDeleteTasks(
                AnyTypeKind.USER,
                key,
                propByRes,
                propByLinkedAccount,
                excludedResources);
        PropagationReporter propagationReporter = taskExecutor.execute(taskInfos, nullPriorityAsync, eraser);

        uwfAdapter.delete(key, eraser, context);

        return propagationReporter.getStatuses();
    }

    @Override
    public String unlink(final UserUR userUR, final String updater, final String context) {
        UserWorkflowResult<Pair<UserUR, Boolean>> updated = uwfAdapter.update(userUR, null, updater, context);
        return updated.getResult().getLeft().getKey();
    }

    @Override
    public String link(final UserUR userUR, final String updater, final String context) {
        return uwfAdapter.update(userUR, null, updater, context).getResult().getLeft().getKey();
    }

    @Override
    public Pair<String, List<PropagationStatus>> activate(
            final StatusR statusR, final boolean nullPriorityAsync, final String updater, final String context) {

        UserWorkflowResult<String> updated = statusR.isOnSyncope()
                ? uwfAdapter.activate(statusR.getKey(), statusR.getToken(), updater, context)
                : new UserWorkflowResult<>(statusR.getKey(), null, null, statusR.getType().name().toLowerCase());

        return Pair.of(updated.getResult(), propagateStatus(statusR, nullPriorityAsync, updater));
    }

    @Override
    public Pair<String, List<PropagationStatus>> reactivate(
            final StatusR statusR, final boolean nullPriorityAsync, final String updater, final String context) {

        UserWorkflowResult<String> updated = statusR.isOnSyncope()
                ? uwfAdapter.reactivate(statusR.getKey(), updater, context)
                : new UserWorkflowResult<>(statusR.getKey(), null, null, statusR.getType().name().toLowerCase());

        return Pair.of(updated.getResult(), propagateStatus(statusR, nullPriorityAsync, updater));
    }

    @Override
    public Pair<String, List<PropagationStatus>> suspend(
            final StatusR statusR, final boolean nullPriorityAsync, final String updater, final String context) {

        UserWorkflowResult<String> updated = statusR.isOnSyncope()
                ? uwfAdapter.suspend(statusR.getKey(), updater, context)
                : new UserWorkflowResult<>(statusR.getKey(), null, null, statusR.getType().name().toLowerCase());

        return Pair.of(updated.getResult(), propagateStatus(statusR, nullPriorityAsync, updater));
    }

    protected List<PropagationStatus> propagateStatus(
            final StatusR statusR, final boolean nullPriorityAsync, final String updater) {

        PropagationByResource<String> propByRes = new PropagationByResource<>();
        propByRes.addAll(ResourceOperation.UPDATE, statusR.getResources());
        List<PropagationTaskInfo> taskInfos = propagationManager.getUpdateTasks(
                null,
                AnyTypeKind.USER,
                statusR.getKey(),
                List.of(),
                statusR.getType() != StatusRType.SUSPEND,
                propByRes,
                null,
                null,
                null);
        PropagationReporter propagationReporter = taskExecutor.execute(taskInfos, nullPriorityAsync, updater);

        return propagationReporter.getStatuses();
    }

    @Override
    public void internalSuspend(final String key, final String updater, final String context) {
        Pair<UserWorkflowResult<String>, Boolean> updated = uwfAdapter.internalSuspend(key, updater, context);

        // propagate suspension if and only if it is required by policy
        if (updated != null && updated.getRight()) {
            UserUR userUR = new UserUR();
            userUR.setKey(updated.getLeft().getResult());

            List<PropagationTaskInfo> taskInfos = propagationManager.getUserUpdateTasks(new UserWorkflowResult<>(
                    Pair.of(userUR, Boolean.FALSE),
                    updated.getLeft().getPropByRes(),
                    updated.getLeft().getPropByLinkedAccount(),
                    updated.getLeft().getPerformedTasks()));
            taskExecutor.execute(taskInfos, false, updater);
        }
    }

    @Override
    public List<PropagationStatus> provision(
            final String key,
            final boolean changePwd,
            final String password,
            final Collection<String> resources,
            final boolean nullPriorityAsync,
            final String executor) {

        UserUR userUR = new UserUR();
        userUR.setKey(key);
        userUR.getResources().addAll(resources.stream().
                map(r -> new StringPatchItem.Builder().operation(PatchOperation.ADD_REPLACE).value(r).build()).
                collect(Collectors.toSet()));

        if (changePwd) {
            PasswordPatch passwordPatch = new PasswordPatch();
            passwordPatch.setOnSyncope(false);
            passwordPatch.getResources().addAll(resources);
            passwordPatch.setValue(password);
            userUR.setPassword(passwordPatch);
        }

        PropagationByResource<String> propByRes = new PropagationByResource<>();
        propByRes.addAll(ResourceOperation.UPDATE, resources);

        UserWorkflowResult<Pair<UserUR, Boolean>> wfResult = new UserWorkflowResult<>(
                Pair.of(userUR, null), propByRes, null, "update");

        List<PropagationTaskInfo> taskInfos = propagationManager.getUserUpdateTasks(wfResult,
                userUR.getPassword() == null ? List.of() : userUR.getPassword().getResources(), null);
        PropagationReporter propagationReporter = taskExecutor.execute(taskInfos, nullPriorityAsync, executor);

        return propagationReporter.getStatuses();
    }

    @Override
    public List<PropagationStatus> deprovision(
            final String key,
            final Collection<String> resources,
            final boolean nullPriorityAsync,
            final String executor) {

        PropagationByResource<String> propByRes = new PropagationByResource<>();
        propByRes.set(ResourceOperation.DELETE, resources);

        PropagationByResource<Pair<String, String>> propByLinkedAccount = new PropagationByResource<>();
        userDAO.findLinkedAccounts(key).stream().
                filter(account -> resources.contains(account.getResource().getKey())).
                forEach(account -> propByLinkedAccount.add(
                ResourceOperation.DELETE,
                Pair.of(account.getResource().getKey(), account.getConnObjectKeyValue())));

        List<PropagationTaskInfo> taskInfos = propagationManager.getDeleteTasks(
                AnyTypeKind.USER,
                key,
                propByRes,
                propByLinkedAccount,
                userDAO.findAllResourceKeys(key).stream().
                        filter(resource -> !resources.contains(resource)).
                        toList());
        PropagationReporter propagationReporter = taskExecutor.execute(taskInfos, nullPriorityAsync, executor);

        return propagationReporter.getStatuses();
    }

    @Override
    public void requestPasswordReset(final String key, final String updater, final String context) {
        uwfAdapter.requestPasswordReset(key, updater, context);
    }

    @Override
    public void confirmPasswordReset(
            final String key, final String token, final String password, final String updater, final String context) {

        UserWorkflowResult<Pair<UserUR, Boolean>> updated =
                uwfAdapter.confirmPasswordReset(key, token, password, updater, context);

        List<PropagationTaskInfo> taskInfos = propagationManager.getUserUpdateTasks(updated);
        taskExecutor.execute(taskInfos, false, updater);
    }
}

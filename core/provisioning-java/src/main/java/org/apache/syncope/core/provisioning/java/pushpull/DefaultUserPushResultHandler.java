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
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.MatchType;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.UserWorkflowResult;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.provisioning.api.pushpull.IgnoreProvisionException;
import org.apache.syncope.core.provisioning.api.pushpull.PushActions;
import org.apache.syncope.core.provisioning.api.pushpull.UserPushResultHandler;
import org.apache.syncope.core.provisioning.java.propagation.DefaultPropagationReporter;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class DefaultUserPushResultHandler extends AbstractPushResultHandler implements UserPushResultHandler {

    @Override
    protected AnyUtils getAnyUtils() {
        return anyUtilsFactory.getInstance(AnyTypeKind.USER);
    }

    @Override
    protected String getName(final Any any) {
        return User.class.cast(any).getUsername();
    }

    @Override
    protected AnyTO getAnyTO(final Any any) {
        return userDataBinder.getUserTO((User) any, true);
    }

    @Override
    protected void provision(final Any any, final Boolean enabled, final ProvisioningReport result) {
        AnyTO before = getAnyTO(any);

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
                false,
                profile.getExecutor());
        reportPropagation(result, reporter);
    }

    @Override
    protected void update(
            final Any any,
            final Boolean enable,
            final ConnectorObject beforeObj,
            final ProvisioningReport result) {

        List<String> ownedResources = getAnyUtils().getAllResources(any).stream().
                map(ExternalResource::getKey).toList();

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
                null,
                any.getType().getKind(),
                any.getKey(),
                List.of(profile.getTask().getResource().getKey()),
                enable,
                propByRes,
                propByLinkedAccount,
                null,
                noPropResources);
        if (!taskInfos.isEmpty()) {
            taskInfos.getFirst().setBeforeObj(Optional.of(beforeObj));
            PropagationReporter reporter = new DefaultPropagationReporter();
            taskExecutor.execute(taskInfos.getFirst(), reporter, profile.getExecutor());
            reportPropagation(result, reporter);
        }
    }

    @Override
    protected void deprovision(final Any any, final ConnectorObject beforeObj, final ProvisioningReport result) {
        AnyTO before = getAnyTO(any);

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
            taskInfos.getFirst().setBeforeObj(Optional.of(beforeObj));
            PropagationReporter reporter = new DefaultPropagationReporter();
            taskExecutor.execute(taskInfos.getFirst(), reporter, profile.getExecutor());
            reportPropagation(result, reporter);
        }
    }

    @Override
    protected WorkflowResult<? extends AnyUR> update(final AnyUR req) {
        WorkflowResult<Pair<UserUR, Boolean>> update =
                uwfAdapter.update((UserUR) req, null, profile.getExecutor(), profile.getContext());
        return new WorkflowResult<>(update.getResult().getLeft(), update.getPropByRes(), update.getPerformedTasks());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public boolean handle(final LinkedAccount account, final Provision provision) {
        try {
            doHandle(account, provision);
            return true;
        } catch (IgnoreProvisionException e) {
            ProvisioningReport ignoreResult = profile.getResults().stream().
                    filter(report -> account.getKey().equalsIgnoreCase(report.getKey())).
                    findFirst().
                    orElse(null);
            if (ignoreResult == null) {
                ignoreResult = new ProvisioningReport();
                ignoreResult.setKey(account.getKey());
                ignoreResult.setAnyType(MatchType.LINKED_ACCOUNT.name());
                ignoreResult.setUidValue(account.getConnObjectKeyValue());

                profile.getResults().add(ignoreResult);
            }

            ignoreResult.setOperation(ResourceOperation.NONE);
            ignoreResult.setStatus(ProvisioningReport.Status.IGNORE);
            ignoreResult.setMessage(e.getMessage());

            LOG.warn("Ignoring during push", e);
            return true;
        } catch (JobExecutionException e) {
            LOG.error("Push failed", e);
            return false;
        }
    }

    protected void doHandle(final LinkedAccount account, final Provision provision) throws JobExecutionException {
        ProvisioningReport result = new ProvisioningReport();
        profile.getResults().add(result);

        result.setKey(account.getKey());
        result.setAnyType(MatchType.LINKED_ACCOUNT.name());
        result.setUidValue(account.getConnObjectKeyValue());
        result.setName(account.getConnObjectKeyValue());

        LOG.debug("Pushing linked account {} towards {}", account.getKey(), profile.getTask().getResource());

        // Try to read remote object BEFORE any actual operation
        Optional<ConnectorObject> connObj = MappingUtils.getConnObjectKeyItem(provision).
                flatMap(connObjectKeyItem -> outboundMatcher.matchByConnObjectKeyValue(
                profile.getConnector(),
                connObjectKeyItem,
                account.getConnObjectKeyValue(),
                profile.getTask().getResource(),
                provision,
                Optional.empty(),
                Optional.empty()));
        LOG.debug("Match found for linked account {} as {}: {}", account, provision.getObjectClass(), connObj);

        ConnectorObject beforeObj = connObj.orElse(null);

        if (profile.isDryRun()) {
            if (beforeObj == null) {
                result.setOperation(toResourceOperation(profile.getTask().getUnmatchingRule()));
            } else {
                result.setOperation(toResourceOperation(profile.getTask().getMatchingRule()));
            }
            result.setStatus(ProvisioningReport.Status.SUCCESS);
        } else {
            Boolean enable = profile.getTask().isSyncStatus()
                    ? BooleanUtils.negate(account.isSuspended())
                    : null;
            try {
                if (beforeObj == null) {
                    result.setOperation(toResourceOperation(profile.getTask().getUnmatchingRule()));

                    switch (profile.getTask().getUnmatchingRule()) {
                        case ASSIGN:
                        case PROVISION:
                            for (PushActions action : profile.getActions()) {
                                if (profile.getTask().getUnmatchingRule() == UnmatchingRule.ASSIGN) {
                                    action.beforeAssign(profile, account);
                                } else {
                                    action.beforeProvision(profile, account);
                                }
                            }

                            if (!profile.getTask().isPerformCreate()) {
                                LOG.debug("PushTask not configured for create");
                                result.setStatus(ProvisioningReport.Status.IGNORE);
                            } else {
                                provision(account, enable, result);
                            }
                            break;

                        case UNLINK:
                            LOG.warn("{} not applicable to linked accounts, ignoring",
                                    profile.getTask().getUnmatchingRule());
                            break;

                        case IGNORE:
                            result.setStatus(ProvisioningReport.Status.IGNORE);
                            break;

                        default:
                        // do nothing
                    }
                } else {
                    result.setOperation(toResourceOperation(profile.getTask().getMatchingRule()));

                    switch (profile.getTask().getMatchingRule()) {
                        case UPDATE:
                            for (PushActions action : profile.getActions()) {
                                action.beforeUpdate(profile, account);
                            }
                            if (!profile.getTask().isPerformUpdate()) {
                                LOG.debug("PushTask not configured for update");
                                result.setStatus(ProvisioningReport.Status.IGNORE);
                            } else {
                                update(account, enable, beforeObj, ResourceOperation.UPDATE, result);
                            }
                            break;

                        case UNASSIGN:
                        case DEPROVISION:
                            for (PushActions action : profile.getActions()) {
                                if (profile.getTask().getMatchingRule() == MatchingRule.UNASSIGN) {
                                    action.beforeUnassign(profile, account);
                                } else {
                                    action.beforeDeprovision(profile, account);
                                }
                            }

                            if (!profile.getTask().isPerformDelete()) {
                                LOG.debug("PushTask not configured for delete");
                                result.setStatus(ProvisioningReport.Status.IGNORE);
                            } else {
                                update(account, enable, beforeObj, ResourceOperation.DELETE, result);
                            }
                            break;

                        case LINK:
                        case UNLINK:
                            LOG.warn("{} not applicable to linked accounts, ignoring",
                                    profile.getTask().getMatchingRule());
                            break;

                        case IGNORE:
                            result.setStatus(ProvisioningReport.Status.IGNORE);
                            break;

                        default:
                        // do nothing
                    }
                }

                for (PushActions action : profile.getActions()) {
                    action.after(profile, account, result);
                }

                if (result.getStatus() == null) {
                    result.setStatus(ProvisioningReport.Status.SUCCESS);
                }
            } catch (IgnoreProvisionException e) {
                throw e;
            } catch (Exception e) {
                result.setStatus(ProvisioningReport.Status.FAILURE);
                result.setMessage(ExceptionUtils.getRootCauseMessage(e));

                LOG.warn("Error pushing linked account {} towards {}", account, profile.getTask().getResource(), e);

                for (PushActions action : profile.getActions()) {
                    action.onError(profile, account, result, e);
                }

                throw new JobExecutionException(e);
            }
        }
    }

    protected void provision(
            final LinkedAccount account,
            final Boolean enable,
            final ProvisioningReport result) {

        PropagationByResource<Pair<String, String>> propByLinkedAccount = new PropagationByResource<>();
        propByLinkedAccount.add(
                ResourceOperation.CREATE,
                Pair.of(account.getResource().getKey(), account.getConnObjectKeyValue()));

        List<PropagationTaskInfo> taskInfos = propagationManager.getUserCreateTasks(
                account.getOwner().getKey(),
                null,
                enable,
                new PropagationByResource<>(),
                propByLinkedAccount,
                null,
                null);
        if (!taskInfos.isEmpty()) {
            taskInfos.getFirst().setBeforeObj(Optional.empty());
            PropagationReporter reporter = new DefaultPropagationReporter();
            taskExecutor.execute(taskInfos.getFirst(), reporter, profile.getExecutor());
            reportPropagation(result, reporter);
        }
    }

    protected void update(
            final LinkedAccount account,
            final Boolean enable,
            final ConnectorObject beforeObj,
            final ResourceOperation operation,
            final ProvisioningReport result) {

        UserUR req = new UserUR();
        req.setKey(account.getOwner().getKey());

        PropagationByResource<Pair<String, String>> propByLinkedAccount = new PropagationByResource<>();
        propByLinkedAccount.add(operation, Pair.of(account.getResource().getKey(), account.getConnObjectKeyValue()));

        List<PropagationTaskInfo> taskInfos = propagationManager.getUserUpdateTasks(
                new UserWorkflowResult<>(
                        Pair.of(req, enable),
                        new PropagationByResource<>(),
                        propByLinkedAccount,
                        ""));
        if (!taskInfos.isEmpty()) {
            taskInfos.getFirst().setBeforeObj(Optional.empty());
            PropagationReporter reporter = new DefaultPropagationReporter();
            taskExecutor.execute(taskInfos.getFirst(), reporter, profile.getExecutor());
            reportPropagation(result, reporter);
        }
    }
}

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
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.patch.StringPatchItem;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.Result;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.provisioning.api.propagation.PropagationException;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.ProvisioningManager;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCache;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCacheValue;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.provisioning.api.pushpull.IgnoreProvisionException;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningReport;
import org.apache.syncope.core.provisioning.api.pushpull.PullActions;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePullExecutor;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePullResultHandler;
import org.apache.syncope.core.provisioning.java.utils.ConnObjectUtils;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = Throwable.class)
public abstract class AbstractPullResultHandler extends AbstractSyncopeResultHandler<PullTask, PullActions>
        implements SyncopePullResultHandler {

    @Autowired
    protected PullUtils pullUtils;

    @Autowired
    private NotificationManager notificationManager;

    @Autowired
    private AuditManager auditManager;

    @Autowired
    private ConnObjectUtils connObjectUtils;

    @Autowired
    private VirSchemaDAO virSchemaDAO;

    @Autowired
    private VirAttrCache virAttrCache;

    private SyncopePullExecutor executor;

    private Result latestResult;

    protected abstract String getName(AnyTO anyTO);

    protected abstract ProvisioningManager<?, ?> getProvisioningManager();

    protected abstract AnyTO doCreate(AnyTO anyTO, SyncDelta delta);

    protected abstract AnyPatch doUpdate(AnyTO before, AnyPatch anyPatch, SyncDelta delta, ProvisioningReport result);

    protected void doDelete(final AnyTypeKind kind, final String key) {
        PropagationByResource propByRes = new PropagationByResource();
        propByRes.add(ResourceOperation.DELETE, profile.getTask().getResource().getKey());
        try {
            taskExecutor.execute(propagationManager.getDeleteTasks(
                    kind,
                    key,
                    propByRes,
                    null),
                    false);
        } catch (Exception e) {
            // A propagation failure doesn't imply a pull failure.
            // The propagation exception status will be reported into the propagation task execution.
            LOG.error("Could not propagate anyObject " + key, e);
        }

        getProvisioningManager().delete(key, Collections.singleton(profile.getTask().getResource().getKey()), true);
    }

    @Override
    public void setPullExecutor(final SyncopePullExecutor executor) {
        this.executor = executor;
    }

    @Override
    public boolean handle(final SyncDelta delta) {
        Provision provision = null;
        try {
            provision = profile.getTask().getResource().getProvision(delta.getObject().getObjectClass()).orElse(null);
            if (provision == null) {
                throw new JobExecutionException("No provision found on " + profile.getTask().getResource() + " for "
                        + delta.getObject().getObjectClass());
            }

            doHandle(delta, provision);

            LOG.debug("Successfully handled {}", delta);

            if (profile.getTask().getPullMode() != PullMode.INCREMENTAL) {
                return true;
            }

            boolean shouldContinue;
            synchronized (this) {
                shouldContinue = latestResult == Result.SUCCESS;
                this.latestResult = null;
            }
            if (shouldContinue) {
                executor.setLatestSyncToken(delta.getObjectClass(), delta.getToken());
            }
            return shouldContinue;
        } catch (IgnoreProvisionException e) {
            ProvisioningReport ignoreResult = new ProvisioningReport();
            ignoreResult.setOperation(ResourceOperation.NONE);
            ignoreResult.setAnyType(provision == null
                    ? getAnyUtils().getAnyTypeKind().name() : provision.getAnyType().getKey());
            ignoreResult.setStatus(ProvisioningReport.Status.IGNORE);
            ignoreResult.setKey(null);
            ignoreResult.setName(delta.getObject().getName().getNameValue());
            profile.getResults().add(ignoreResult);

            LOG.warn("Ignoring during pull", e);

            executor.setLatestSyncToken(delta.getObjectClass(), delta.getToken());

            return true;
        } catch (JobExecutionException e) {
            LOG.error("Pull failed", e);

            return false;
        }
    }

    protected List<ProvisioningReport> assign(
            final SyncDelta delta, final Provision provision, final AnyUtils anyUtils)
            throws JobExecutionException {

        if (!profile.getTask().isPerformCreate()) {
            LOG.debug("PullTask not configured for create");
            finalize(UnmatchingRule.toEventName(UnmatchingRule.ASSIGN), Result.SUCCESS, null, null, delta);
            return Collections.<ProvisioningReport>emptyList();
        }

        AnyTO anyTO = connObjectUtils.getAnyTO(delta.getObject(), profile.getTask(), provision, anyUtils);
        anyTO.getResources().add(profile.getTask().getResource().getKey());

        ProvisioningReport result = new ProvisioningReport();
        result.setOperation(ResourceOperation.CREATE);
        result.setAnyType(provision.getAnyType().getKey());
        result.setStatus(ProvisioningReport.Status.SUCCESS);
        result.setName(getName(anyTO));

        if (profile.isDryRun()) {
            result.setKey(null);
            finalize(UnmatchingRule.toEventName(UnmatchingRule.ASSIGN), Result.SUCCESS, null, null, delta);
        } else {
            SyncDelta actionedDelta = delta;
            for (PullActions action : profile.getActions()) {
                actionedDelta = action.beforeAssign(profile, actionedDelta, anyTO);
            }

            create(anyTO, actionedDelta, UnmatchingRule.toEventName(UnmatchingRule.ASSIGN), result);
        }

        return Collections.singletonList(result);
    }

    protected List<ProvisioningReport> provision(
            final SyncDelta delta, final Provision provision, final AnyUtils anyUtils)
            throws JobExecutionException {

        if (!profile.getTask().isPerformCreate()) {
            LOG.debug("PullTask not configured for create");
            finalize(UnmatchingRule.toEventName(UnmatchingRule.PROVISION), Result.SUCCESS, null, null, delta);
            return Collections.<ProvisioningReport>emptyList();
        }

        AnyTO anyTO = connObjectUtils.getAnyTO(delta.getObject(), profile.getTask(), provision, anyUtils);

        ProvisioningReport result = new ProvisioningReport();
        result.setOperation(ResourceOperation.CREATE);
        result.setAnyType(provision.getAnyType().getKey());
        result.setStatus(ProvisioningReport.Status.SUCCESS);
        result.setName(getName(anyTO));

        if (profile.isDryRun()) {
            result.setKey(null);
            finalize(UnmatchingRule.toEventName(UnmatchingRule.PROVISION), Result.SUCCESS, null, null, delta);
        } else {
            SyncDelta actionedDelta = delta;
            for (PullActions action : profile.getActions()) {
                actionedDelta = action.beforeProvision(profile, actionedDelta, anyTO);
            }

            create(anyTO, actionedDelta, UnmatchingRule.toEventName(UnmatchingRule.PROVISION), result);
        }

        return Collections.singletonList(result);
    }

    protected void throwIgnoreProvisionException(final SyncDelta delta, final Exception exception)
            throws JobExecutionException {

        if (exception instanceof IgnoreProvisionException) {
            throw IgnoreProvisionException.class.cast(exception);
        }

        IgnoreProvisionException ipe = null;
        for (PullActions action : profile.getActions()) {
            if (ipe == null) {
                ipe = action.onError(profile, delta, exception);
            }
        }
        if (ipe != null) {
            throw ipe;
        }
    }

    protected void create(
            final AnyTO anyTO,
            final SyncDelta delta,
            final String operation,
            final ProvisioningReport result)
            throws JobExecutionException {

        Object output;
        Result resultStatus;

        try {
            AnyTO created = doCreate(anyTO, delta);
            output = created;
            result.setKey(created.getKey());
            result.setName(getName(created));
            resultStatus = Result.SUCCESS;

            for (PullActions action : profile.getActions()) {
                action.after(profile, delta, created, result);
            }

            LOG.debug("{} {} successfully created", created.getType(), created.getKey());
        } catch (PropagationException e) {
            // A propagation failure doesn't imply a pull failure.
            // The propagation exception status will be reported into the propagation task execution.
            LOG.error("Could not propagate {} {}", anyTO.getType(), delta.getUid().getUidValue(), e);
            output = e;
            resultStatus = Result.FAILURE;
        } catch (Exception e) {
            throwIgnoreProvisionException(delta, e);

            result.setStatus(ProvisioningReport.Status.FAILURE);
            result.setMessage(ExceptionUtils.getRootCauseMessage(e));
            LOG.error("Could not create {} {} ", anyTO.getType(), delta.getUid().getUidValue(), e);
            output = e;
            resultStatus = Result.FAILURE;
        }

        finalize(operation, resultStatus, null, output, delta);
    }

    protected List<ProvisioningReport> update(
            final SyncDelta delta, final List<String> anys, final Provision provision) throws JobExecutionException {

        if (!profile.getTask().isPerformUpdate()) {
            LOG.debug("PullTask not configured for update");
            finalize(MatchingRule.toEventName(MatchingRule.UPDATE), Result.SUCCESS, null, null, delta);
            return Collections.<ProvisioningReport>emptyList();
        }

        LOG.debug("About to update {}", anys);

        List<ProvisioningReport> results = new ArrayList<>();

        SyncDelta workingDelta = delta;
        for (String key : anys) {
            LOG.debug("About to update {}", key);

            ProvisioningReport result = new ProvisioningReport();
            result.setOperation(ResourceOperation.UPDATE);
            result.setAnyType(provision.getAnyType().getKey());
            result.setStatus(ProvisioningReport.Status.SUCCESS);
            result.setKey(key);

            AnyTO before = getAnyTO(key);
            if (before == null) {
                result.setStatus(ProvisioningReport.Status.FAILURE);
                result.setMessage(String.format("Any '%s(%s)' not found", provision.getAnyType().getKey(), key));
            } else {
                result.setName(getName(before));
            }

            if (!profile.isDryRun()) {
                Result resultStatus;
                Object output;
                AnyPatch effectivePatch = null;

                if (before == null) {
                    resultStatus = Result.FAILURE;
                    output = null;
                } else {
                    try {
                        AnyPatch anyPatch = connObjectUtils.getAnyPatch(
                                before.getKey(),
                                workingDelta.getObject(),
                                before,
                                profile.getTask(),
                                provision,
                                getAnyUtils());

                        for (PullActions action : profile.getActions()) {
                            workingDelta = action.beforeUpdate(profile, workingDelta, before, anyPatch);
                        }

                        effectivePatch = doUpdate(before, anyPatch, workingDelta, result);
                        AnyTO updated = AnyOperations.patch(before, effectivePatch);

                        for (PullActions action : profile.getActions()) {
                            action.after(profile, workingDelta, updated, result);
                        }

                        output = updated;
                        resultStatus = Result.SUCCESS;
                        result.setName(getName(updated));

                        LOG.debug("{} {} successfully updated", provision.getAnyType().getKey(), key);
                    } catch (PropagationException e) {
                        // A propagation failure doesn't imply a pull failure.
                        // The propagation exception status will be reported into the propagation task execution.
                        LOG.error("Could not propagate {} {}",
                                provision.getAnyType().getKey(), workingDelta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = Result.FAILURE;
                    } catch (Exception e) {
                        throwIgnoreProvisionException(delta, e);

                        result.setStatus(ProvisioningReport.Status.FAILURE);
                        result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error("Could not update {} {}",
                                provision.getAnyType().getKey(), workingDelta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = Result.FAILURE;
                    }
                }
                finalize(MatchingRule.toEventName(MatchingRule.UPDATE),
                        resultStatus, before, output, workingDelta, effectivePatch);
            }
            results.add(result);
        }
        return results;
    }

    protected List<ProvisioningReport> deprovision(
            final SyncDelta delta,
            final List<String> anys,
            final Provision provision,
            final boolean unlink)
            throws JobExecutionException {

        if (!profile.getTask().isPerformUpdate()) {
            LOG.debug("PullTask not configured for update");
            finalize(unlink
                    ? MatchingRule.toEventName(MatchingRule.UNASSIGN)
                    : MatchingRule.toEventName(MatchingRule.DEPROVISION), Result.SUCCESS, null, null, delta);
            return Collections.<ProvisioningReport>emptyList();
        }

        LOG.debug("About to deprovision {}", anys);

        final List<ProvisioningReport> results = new ArrayList<>();

        for (String key : anys) {
            LOG.debug("About to unassign resource {}", key);

            ProvisioningReport result = new ProvisioningReport();
            result.setOperation(ResourceOperation.DELETE);
            result.setAnyType(provision.getAnyType().getKey());
            result.setStatus(ProvisioningReport.Status.SUCCESS);
            result.setKey(key);

            AnyTO before = getAnyTO(key);

            if (before == null) {
                result.setStatus(ProvisioningReport.Status.FAILURE);
                result.setMessage(String.format("Any '%s(%s)' not found", provision.getAnyType().getKey(), key));
            }

            if (!profile.isDryRun()) {
                Object output;
                Result resultStatus;

                if (before == null) {
                    resultStatus = Result.FAILURE;
                    output = null;
                } else {
                    result.setName(getName(before));

                    try {
                        if (unlink) {
                            for (PullActions action : profile.getActions()) {
                                action.beforeUnassign(profile, delta, before);
                            }
                        } else {
                            for (PullActions action : profile.getActions()) {
                                action.beforeDeprovision(profile, delta, before);
                            }
                        }

                        PropagationByResource propByRes = new PropagationByResource();
                        propByRes.add(ResourceOperation.DELETE, profile.getTask().getResource().getKey());
                        taskExecutor.execute(propagationManager.getDeleteTasks(
                                provision.getAnyType().getKind(),
                                key,
                                propByRes,
                                null),
                                false);

                        AnyPatch anyPatch = null;
                        if (unlink) {
                            anyPatch = newPatch(key);
                            anyPatch.getResources().add(new StringPatchItem.Builder().
                                    operation(PatchOperation.DELETE).
                                    value(profile.getTask().getResource().getKey()).build());
                        }
                        if (anyPatch == null) {
                            output = getAnyTO(key);
                        } else {
                            output = doUpdate(before, anyPatch, delta, result);
                        }

                        for (PullActions action : profile.getActions()) {
                            action.after(profile, delta, AnyTO.class.cast(output), result);
                        }

                        resultStatus = Result.SUCCESS;

                        LOG.debug("{} {} successfully updated", provision.getAnyType().getKey(), key);
                    } catch (PropagationException e) {
                        // A propagation failure doesn't imply a pull failure.
                        // The propagation exception status will be reported into the propagation task execution.
                        LOG.error("Could not propagate {} {}",
                                provision.getAnyType().getKey(), delta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = Result.FAILURE;
                    } catch (Exception e) {
                        throwIgnoreProvisionException(delta, e);

                        result.setStatus(ProvisioningReport.Status.FAILURE);
                        result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error("Could not update {} {}",
                                provision.getAnyType().getKey(), delta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = Result.FAILURE;
                    }
                }
                finalize(unlink
                        ? MatchingRule.toEventName(MatchingRule.UNASSIGN)
                        : MatchingRule.toEventName(MatchingRule.DEPROVISION), resultStatus, before, output, delta);
            }
            results.add(result);
        }

        return results;
    }

    protected List<ProvisioningReport> link(
            final SyncDelta delta,
            final List<String> anys,
            final Provision provision,
            final boolean unlink)
            throws JobExecutionException {

        if (!profile.getTask().isPerformUpdate()) {
            LOG.debug("PullTask not configured for update");
            finalize(unlink
                    ? MatchingRule.toEventName(MatchingRule.UNLINK)
                    : MatchingRule.toEventName(MatchingRule.LINK), Result.SUCCESS, null, null, delta);
            return Collections.<ProvisioningReport>emptyList();
        }

        LOG.debug("About to update {}", anys);

        final List<ProvisioningReport> results = new ArrayList<>();

        for (String key : anys) {
            LOG.debug("About to unassign resource {}", key);

            ProvisioningReport result = new ProvisioningReport();
            result.setOperation(ResourceOperation.NONE);
            result.setAnyType(provision.getAnyType().getKey());
            result.setStatus(ProvisioningReport.Status.SUCCESS);
            result.setKey(key);

            AnyTO before = getAnyTO(key);

            if (before == null) {
                result.setStatus(ProvisioningReport.Status.FAILURE);
                result.setMessage(String.format("Any '%s(%s)' not found", provision.getAnyType().getKey(), key));
            }

            if (!profile.isDryRun()) {
                Result resultStatus;
                Object output;
                AnyPatch effectivePatch = null;

                if (before == null) {
                    resultStatus = Result.FAILURE;
                    output = null;
                } else {
                    result.setName(getName(before));

                    try {
                        if (unlink) {
                            for (PullActions action : profile.getActions()) {
                                action.beforeUnlink(profile, delta, before);
                            }
                        } else {
                            for (PullActions action : profile.getActions()) {
                                action.beforeLink(profile, delta, before);
                            }
                        }

                        AnyPatch anyPatch = newPatch(before.getKey());
                        anyPatch.getResources().add(new StringPatchItem.Builder().
                                operation(unlink ? PatchOperation.DELETE : PatchOperation.ADD_REPLACE).
                                value(profile.getTask().getResource().getKey()).build());

                        effectivePatch = update(anyPatch).getResult();
                        output = AnyOperations.patch(before, effectivePatch);

                        for (PullActions action : profile.getActions()) {
                            action.after(profile, delta, AnyTO.class.cast(output), result);
                        }

                        resultStatus = Result.SUCCESS;

                        LOG.debug("{} {} successfully updated", provision.getAnyType().getKey(), key);
                    } catch (PropagationException e) {
                        // A propagation failure doesn't imply a pull failure.
                        // The propagation exception status will be reported into the propagation task execution.
                        LOG.error("Could not propagate {} {}",
                                provision.getAnyType().getKey(), delta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = Result.FAILURE;
                    } catch (Exception e) {
                        throwIgnoreProvisionException(delta, e);

                        result.setStatus(ProvisioningReport.Status.FAILURE);
                        result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error("Could not update {} {}",
                                provision.getAnyType().getKey(), delta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = Result.FAILURE;
                    }
                }
                finalize(unlink
                        ? MatchingRule.toEventName(MatchingRule.UNLINK)
                        : MatchingRule.toEventName(MatchingRule.LINK),
                        resultStatus, before, output, delta, effectivePatch);
            }
            results.add(result);
        }

        return results;
    }

    protected List<ProvisioningReport> delete(
            final SyncDelta delta,
            final List<String> anys,
            final Provision provision)
            throws JobExecutionException {

        if (!profile.getTask().isPerformDelete()) {
            LOG.debug("PullTask not configured for delete");
            finalize(ResourceOperation.DELETE.name().toLowerCase(), Result.SUCCESS, null, null, delta);
            return Collections.<ProvisioningReport>emptyList();
        }

        LOG.debug("About to delete {}", anys);

        List<ProvisioningReport> results = new ArrayList<>();

        SyncDelta workingDelta = delta;
        for (String key : anys) {
            Object output;
            Result resultStatus = Result.FAILURE;

            ProvisioningReport result = new ProvisioningReport();

            try {
                AnyTO before = getAnyTO(key);

                result.setKey(key);
                result.setName(getName(before));
                result.setOperation(ResourceOperation.DELETE);
                result.setAnyType(provision.getAnyType().getKey());
                result.setStatus(ProvisioningReport.Status.SUCCESS);

                if (!profile.isDryRun()) {
                    for (PullActions action : profile.getActions()) {
                        workingDelta = action.beforeDelete(profile, workingDelta, before);
                    }

                    try {
                        doDelete(provision.getAnyType().getKind(), key);
                        output = null;
                        resultStatus = Result.SUCCESS;

                        for (PullActions action : profile.getActions()) {
                            action.after(profile, workingDelta, before, result);
                        }
                    } catch (Exception e) {
                        throwIgnoreProvisionException(delta, e);

                        result.setStatus(ProvisioningReport.Status.FAILURE);
                        result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error("Could not delete {} {}", provision.getAnyType().getKey(), key, e);
                        output = e;
                    }

                    finalize(ResourceOperation.DELETE.name().toLowerCase(), resultStatus, before, output, workingDelta);
                }

                results.add(result);
            } catch (NotFoundException e) {
                LOG.error("Could not find {} {}", provision.getAnyType().getKey(), key, e);
            } catch (DelegatedAdministrationException e) {
                LOG.error("Not allowed to read {} {}", provision.getAnyType().getKey(), key, e);
            } catch (Exception e) {
                LOG.error("Could not delete {} {}", provision.getAnyType().getKey(), key, e);
            }
        }

        return results;
    }

    protected ProvisioningReport ignore(
            final SyncDelta delta,
            final Provision provision,
            final boolean matching)
            throws JobExecutionException {

        LOG.debug("Any to ignore {}", delta.getObject().getUid().getUidValue());

        ProvisioningReport result = new ProvisioningReport();

        result.setKey(null);
        result.setName(delta.getObject().getUid().getUidValue());
        result.setOperation(ResourceOperation.NONE);
        result.setAnyType(provision.getAnyType().getKey());
        result.setStatus(ProvisioningReport.Status.SUCCESS);

        finalize(matching
                ? MatchingRule.toEventName(MatchingRule.IGNORE)
                : UnmatchingRule.toEventName(UnmatchingRule.IGNORE), Result.SUCCESS, null, null, delta);

        return result;
    }

    /**
     * Look into SyncDelta and take necessary profile.getActions() (create / update / delete) on any object(s).
     *
     * @param delta returned by the underlying profile.getConnector()
     * @param provision provisioning info
     * @throws JobExecutionException in case of pull failure.
     */
    protected void doHandle(final SyncDelta delta, final Provision provision) throws JobExecutionException {
        AnyUtils anyUtils = getAnyUtils();

        LOG.debug("Process {} for {} as {}",
                delta.getDeltaType(), delta.getUid().getUidValue(), delta.getObject().getObjectClass());

        String uid = delta.getPreviousUid() == null
                ? delta.getUid().getUidValue()
                : delta.getPreviousUid().getUidValue();

        try {
            List<String> anyKeys = pullUtils.findExisting(uid, delta.getObject(), provision, anyUtils);
            LOG.debug("Match(es) found for {} as {}: {}",
                    delta.getUid().getUidValue(), delta.getObject().getObjectClass(), anyKeys);

            if (anyKeys.size() > 1) {
                switch (profile.getResAct()) {
                    case IGNORE:
                        throw new IllegalStateException("More than one match " + anyKeys);

                    case FIRSTMATCH:
                        anyKeys = anyKeys.subList(0, 1);
                        break;

                    case LASTMATCH:
                        anyKeys = anyKeys.subList(anyKeys.size() - 1, anyKeys.size());
                        break;

                    default:
                    // keep anyKeys unmodified
                }
            }

            if (SyncDeltaType.CREATE_OR_UPDATE == delta.getDeltaType()) {
                if (anyKeys.isEmpty()) {
                    switch (profile.getTask().getUnmatchingRule()) {
                        case ASSIGN:
                            profile.getResults().addAll(assign(delta, provision, anyUtils));
                            break;

                        case PROVISION:
                            profile.getResults().addAll(provision(delta, provision, anyUtils));
                            break;

                        case IGNORE:
                            profile.getResults().add(ignore(delta, provision, false));
                            break;

                        default:
                        // do nothing
                    }
                } else {
                    // update VirAttrCache
                    for (VirSchema virSchema : virSchemaDAO.findByProvision(provision)) {
                        Attribute attr = delta.getObject().getAttributeByName(virSchema.getExtAttrName());
                        for (String anyKey : anyKeys) {
                            if (attr == null) {
                                virAttrCache.expire(
                                        provision.getAnyType().getKey(),
                                        anyKey,
                                        virSchema.getKey());
                            } else {
                                VirAttrCacheValue cacheValue = new VirAttrCacheValue();
                                cacheValue.setValues(attr.getValue());
                                virAttrCache.put(
                                        provision.getAnyType().getKey(),
                                        anyKey,
                                        virSchema.getKey(),
                                        cacheValue);
                            }
                        }
                    }

                    switch (profile.getTask().getMatchingRule()) {
                        case UPDATE:
                            profile.getResults().addAll(update(delta, anyKeys, provision));
                            break;

                        case DEPROVISION:
                            profile.getResults().addAll(deprovision(delta, anyKeys, provision, false));
                            break;

                        case UNASSIGN:
                            profile.getResults().addAll(deprovision(delta, anyKeys, provision, true));
                            break;

                        case LINK:
                            profile.getResults().addAll(link(delta, anyKeys, provision, false));
                            break;

                        case UNLINK:
                            profile.getResults().addAll(link(delta, anyKeys, provision, true));
                            break;

                        case IGNORE:
                            profile.getResults().add(ignore(delta, provision, true));
                            break;

                        default:
                        // do nothing
                    }
                }
            } else if (SyncDeltaType.DELETE == delta.getDeltaType()) {
                if (anyKeys.isEmpty()) {
                    finalize(ResourceOperation.DELETE.name().toLowerCase(), Result.SUCCESS, null, null, delta);
                    LOG.debug("No match found for deletion");
                } else {
                    profile.getResults().addAll(delete(delta, anyKeys, provision));
                }
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            LOG.warn(e.getMessage());
        }
    }

    protected void finalize(
            final String event,
            final Result result,
            final Object before,
            final Object output,
            final SyncDelta delta,
            final Object... furtherInput) {

        synchronized (this) {
            this.latestResult = result;
        }

        notificationManager.createTasks(AuditElements.EventCategoryType.PULL,
                getAnyUtils().getAnyTypeKind().name().toLowerCase(),
                profile.getTask().getResource().getKey(),
                event,
                result,
                before,
                output,
                delta,
                furtherInput);

        auditManager.audit(AuditElements.EventCategoryType.PULL,
                getAnyUtils().getAnyTypeKind().name().toLowerCase(),
                profile.getTask().getResource().getKey(),
                event,
                result,
                before,
                output,
                delta,
                furtherInput);
    }
}

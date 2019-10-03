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
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.request.StringPatchItem;
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
import org.apache.syncope.core.persistence.api.dao.RemediationDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.provisioning.api.propagation.PropagationException;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Remediation;
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
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractPullResultHandler extends AbstractSyncopeResultHandler<PullTask, PullActions>
        implements SyncopePullResultHandler {

    @Autowired
    protected PullUtils pullUtils;

    @Autowired
    protected NotificationManager notificationManager;

    @Autowired
    protected AuditManager auditManager;

    @Autowired
    protected ConnObjectUtils connObjectUtils;

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected RemediationDAO remediationDAO;

    @Autowired
    protected VirSchemaDAO virSchemaDAO;

    @Autowired
    protected VirAttrCache virAttrCache;

    @Autowired
    protected EntityFactory entityFactory;

    protected SyncopePullExecutor executor;

    protected Result latestResult;

    protected abstract String getName(AnyTO anyTO);

    protected abstract String getName(AnyCR anyCR);

    protected abstract ProvisioningManager<?, ?, ?> getProvisioningManager();

    protected abstract AnyTO doCreate(AnyCR anyCR, SyncDelta delta);

    protected abstract AnyUR doUpdate(AnyTO before, AnyUR anyUR, SyncDelta delta, ProvisioningReport result);

    @Override
    public void setPullExecutor(final SyncopePullExecutor executor) {
        this.executor = executor;
    }

    @Transactional
    @Override
    public boolean handle(final SyncDelta delta) {
        Provision provision = null;
        try {
            provision = profile.getTask().getResource().getProvision(delta.getObject().getObjectClass()).
                    orElseThrow(() -> new JobExecutionException(
                    "No provision found on " + profile.getTask().getResource() + " for "
                    + delta.getObject().getObjectClass()));

            doHandle(delta, provision);
            executor.reportHandled(delta.getObjectClass(), delta.getObject().getName());

            LOG.debug("Successfully handled {}", delta);

            if (profile.getTask().getPullMode() != PullMode.INCREMENTAL) {
                if (executor.wasInterruptRequested()) {
                    LOG.debug("Pull interrupted");
                    executor.setInterrupted();
                    return false;
                }
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
            if (executor.wasInterruptRequested()) {
                LOG.debug("Pull interrupted");
                executor.setInterrupted();
                return false;
            }
            return shouldContinue;
        } catch (IgnoreProvisionException e) {
            ProvisioningReport ignoreResult = new ProvisioningReport();
            ignoreResult.setOperation(ResourceOperation.NONE);
            ignoreResult.setAnyType(provision == null
                    ? getAnyUtils().anyTypeKind().name() : provision.getAnyType().getKey());
            ignoreResult.setStatus(ProvisioningReport.Status.IGNORE);
            ignoreResult.setMessage(e.getMessage());
            ignoreResult.setKey(null);
            ignoreResult.setName(delta.getObject().getName().getNameValue());
            profile.getResults().add(ignoreResult);

            LOG.warn("Ignoring during pull", e);

            executor.setLatestSyncToken(delta.getObjectClass(), delta.getToken());
            executor.reportHandled(delta.getObjectClass(), delta.getObject().getName());

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
            return List.of();
        }

        AnyCR anyCR = connObjectUtils.getAnyCR(delta.getObject(), profile.getTask(), provision, anyUtils);
        anyCR.getResources().add(profile.getTask().getResource().getKey());

        ProvisioningReport result = new ProvisioningReport();
        result.setOperation(ResourceOperation.CREATE);
        result.setAnyType(provision.getAnyType().getKey());
        result.setStatus(ProvisioningReport.Status.SUCCESS);
        result.setName(getName(anyCR));
        result.setUidValue(delta.getUid().getUidValue());

        if (profile.isDryRun()) {
            result.setKey(null);
            finalize(UnmatchingRule.toEventName(UnmatchingRule.ASSIGN), Result.SUCCESS, null, null, delta);
        } else {
            for (PullActions action : profile.getActions()) {
                action.beforeAssign(profile, delta, anyCR);
            }

            create(anyCR, delta, UnmatchingRule.toEventName(UnmatchingRule.ASSIGN), provision, result);
        }

        return List.of(result);
    }

    protected List<ProvisioningReport> provision(
            final SyncDelta delta, final Provision provision, final AnyUtils anyUtils)
            throws JobExecutionException {

        if (!profile.getTask().isPerformCreate()) {
            LOG.debug("PullTask not configured for create");
            finalize(UnmatchingRule.toEventName(UnmatchingRule.PROVISION), Result.SUCCESS, null, null, delta);
            return List.of();
        }

        AnyCR anyCR = connObjectUtils.getAnyCR(delta.getObject(), profile.getTask(), provision, anyUtils);

        ProvisioningReport result = new ProvisioningReport();
        result.setOperation(ResourceOperation.CREATE);
        result.setAnyType(provision.getAnyType().getKey());
        result.setStatus(ProvisioningReport.Status.SUCCESS);
        result.setName(getName(anyCR));
        result.setUidValue(delta.getUid().getUidValue());

        if (profile.isDryRun()) {
            result.setKey(null);
            finalize(UnmatchingRule.toEventName(UnmatchingRule.PROVISION), Result.SUCCESS, null, null, delta);
        } else {
            for (PullActions action : profile.getActions()) {
                action.beforeProvision(profile, delta, anyCR);
            }

            create(anyCR, delta, UnmatchingRule.toEventName(UnmatchingRule.PROVISION), provision, result);
        }

        return List.of(result);
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
            final AnyCR anyCR,
            final SyncDelta delta,
            final String operation,
            final Provision provision,
            final ProvisioningReport result)
            throws JobExecutionException {

        Object output;
        Result resultStatus;

        try {
            AnyTO created = doCreate(anyCR, delta);
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
            LOG.error("Could not propagate {} {}", provision.getAnyType().getKey(), delta.getUid().getUidValue(), e);
            output = e;
            resultStatus = Result.FAILURE;
        } catch (Exception e) {
            throwIgnoreProvisionException(delta, e);

            result.setStatus(ProvisioningReport.Status.FAILURE);
            result.setMessage(ExceptionUtils.getRootCauseMessage(e));
            LOG.error("Could not create {} {} ", provision.getAnyType().getKey(), delta.getUid().getUidValue(), e);
            output = e;
            resultStatus = Result.FAILURE;

            if (profile.getTask().isRemediation()) {
                Remediation entity = entityFactory.newEntity(Remediation.class);
                entity.setAnyType(provision.getAnyType());
                entity.setOperation(ResourceOperation.CREATE);
                entity.setPayload(anyCR);
                entity.setError(result.getMessage());
                entity.setInstant(new Date());
                entity.setRemoteName(delta.getObject().getName().getNameValue());
                entity.setPullTask(profile.getTask());

                remediationDAO.save(entity);
            }
        }

        finalize(operation, resultStatus, null, output, delta);
    }

    protected List<ProvisioningReport> update(
            final SyncDelta delta, final List<String> anyKeys, final Provision provision) throws JobExecutionException {

        if (!profile.getTask().isPerformUpdate()) {
            LOG.debug("PullTask not configured for update");
            finalize(MatchingRule.toEventName(MatchingRule.UPDATE), Result.SUCCESS, null, null, delta);
            return List.of();
        }

        LOG.debug("About to update {}", anyKeys);

        List<ProvisioningReport> results = new ArrayList<>();

        for (String key : anyKeys) {
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
                AnyUR effectiveReq = null;

                if (before == null) {
                    resultStatus = Result.FAILURE;
                    output = null;
                } else {
                    AnyUR anyUR = null;
                    try {
                        anyUR = connObjectUtils.getAnyUR(
                                before.getKey(),
                                delta.getObject(),
                                before,
                                profile.getTask(),
                                provision,
                                getAnyUtils());

                        for (PullActions action : profile.getActions()) {
                            action.beforeUpdate(profile, delta, before, anyUR);
                        }

                        effectiveReq = doUpdate(before, anyUR, delta, result);
                        AnyTO updated = AnyOperations.patch(before, effectiveReq);

                        for (PullActions action : profile.getActions()) {
                            action.after(profile, delta, updated, result);
                        }

                        output = updated;
                        resultStatus = Result.SUCCESS;
                        result.setName(getName(updated));

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

                        if (profile.getTask().isRemediation()) {
                            Remediation entity = entityFactory.newEntity(Remediation.class);
                            entity.setAnyType(provision.getAnyType());
                            entity.setOperation(ResourceOperation.UPDATE);
                            entity.setPayload(anyUR);
                            entity.setError(result.getMessage());
                            entity.setInstant(new Date());
                            entity.setRemoteName(delta.getObject().getName().getNameValue());
                            entity.setPullTask(profile.getTask());

                            remediationDAO.save(entity);
                        }
                    }
                }
                finalize(MatchingRule.toEventName(MatchingRule.UPDATE),
                        resultStatus, before, output, delta, effectiveReq);
            }
            results.add(result);
        }
        return results;
    }

    protected List<ProvisioningReport> deprovision(
            final SyncDelta delta,
            final List<String> anyKeys,
            final Provision provision,
            final boolean unlink)
            throws JobExecutionException {

        if (!profile.getTask().isPerformUpdate()) {
            LOG.debug("PullTask not configured for update");
            finalize(unlink
                    ? MatchingRule.toEventName(MatchingRule.UNASSIGN)
                    : MatchingRule.toEventName(MatchingRule.DEPROVISION), Result.SUCCESS, null, null, delta);
            return List.of();
        }

        LOG.debug("About to deprovision {}", anyKeys);

        final List<ProvisioningReport> results = new ArrayList<>();

        for (String key : anyKeys) {
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

                        PropagationByResource<String> propByRes = new PropagationByResource<>();
                        propByRes.add(ResourceOperation.DELETE, profile.getTask().getResource().getKey());

                        PropagationByResource<Pair<String, String>> propByLinkedAccount = new PropagationByResource<>();
                        if (getAnyUtils().anyTypeKind() == AnyTypeKind.USER) {
                            userDAO.findLinkedAccounts(key).forEach(account -> propByLinkedAccount.add(
                                    ResourceOperation.DELETE,
                                    Pair.of(account.getResource().getKey(), account.getConnObjectKeyValue())));
                        }

                        taskExecutor.execute(propagationManager.getDeleteTasks(
                                provision.getAnyType().getKind(),
                                key,
                                propByRes,
                                propByLinkedAccount,
                                null),
                                false);

                        AnyUR anyUR = null;
                        if (unlink) {
                            anyUR = getAnyUtils().newAnyUR(key);
                            anyUR.getResources().add(new StringPatchItem.Builder().
                                    operation(PatchOperation.DELETE).
                                    value(profile.getTask().getResource().getKey()).build());
                        }
                        if (anyUR == null) {
                            output = getAnyTO(key);
                        } else {
                            output = doUpdate(before, anyUR, delta, result);
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
            final List<String> anyKeys,
            final Provision provision,
            final boolean unlink)
            throws JobExecutionException {

        if (!profile.getTask().isPerformUpdate()) {
            LOG.debug("PullTask not configured for update");
            finalize(unlink
                    ? MatchingRule.toEventName(MatchingRule.UNLINK)
                    : MatchingRule.toEventName(MatchingRule.LINK), Result.SUCCESS, null, null, delta);
            return List.of();
        }

        LOG.debug("About to update {}", anyKeys);

        final List<ProvisioningReport> results = new ArrayList<>();

        for (String key : anyKeys) {
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
                AnyUR effectiveReq = null;

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

                        AnyUR anyUR = getAnyUtils().newAnyUR(before.getKey());
                        anyUR.getResources().add(new StringPatchItem.Builder().
                                operation(unlink ? PatchOperation.DELETE : PatchOperation.ADD_REPLACE).
                                value(profile.getTask().getResource().getKey()).build());

                        effectiveReq = update(anyUR).getResult();
                        output = AnyOperations.patch(before, effectiveReq);

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
                        resultStatus, before, output, delta, effectiveReq);
            }
            results.add(result);
        }

        return results;
    }

    protected List<ProvisioningReport> delete(
            final SyncDelta delta,
            final List<String> anyKeys,
            final Provision provision)
            throws JobExecutionException {

        if (!profile.getTask().isPerformDelete()) {
            LOG.debug("PullTask not configured for delete");
            finalize(ResourceOperation.DELETE.name().toLowerCase(), Result.SUCCESS, null, null, delta);
            return List.of();
        }

        LOG.debug("About to delete {}", anyKeys);

        List<ProvisioningReport> results = new ArrayList<>();

        anyKeys.forEach(key -> {
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
                        action.beforeDelete(profile, delta, before);
                    }

                    try {
                        getProvisioningManager().
                                delete(key, Set.of(profile.getTask().getResource().getKey()), true);
                        output = null;
                        resultStatus = Result.SUCCESS;

                        for (PullActions action : profile.getActions()) {
                            action.after(profile, delta, before, result);
                        }
                    } catch (Exception e) {
                        throwIgnoreProvisionException(delta, e);

                        result.setStatus(ProvisioningReport.Status.FAILURE);
                        result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error("Could not delete {} {}", provision.getAnyType().getKey(), key, e);
                        output = e;

                        if (profile.getTask().isRemediation()) {
                            Remediation entity = entityFactory.newEntity(Remediation.class);
                            entity.setAnyType(provision.getAnyType());
                            entity.setOperation(ResourceOperation.DELETE);
                            entity.setPayload(key);
                            entity.setError(result.getMessage());
                            entity.setInstant(new Date());
                            entity.setRemoteName(delta.getObject().getName().getNameValue());
                            entity.setPullTask(profile.getTask());

                            remediationDAO.save(entity);
                        }
                    }

                    finalize(ResourceOperation.DELETE.name().toLowerCase(), resultStatus, before, output, delta);
                }

                results.add(result);
            } catch (NotFoundException e) {
                LOG.error("Could not find {} {}", provision.getAnyType().getKey(), key, e);
            } catch (DelegatedAdministrationException e) {
                LOG.error("Not allowed to read {} {}", provision.getAnyType().getKey(), key, e);
            } catch (Exception e) {
                LOG.error("Could not delete {} {}", provision.getAnyType().getKey(), key, e);
            }
        });

        return results;
    }

    protected List<ProvisioningReport> ignore(
            final SyncDelta delta,
            final List<String> anyKeys,
            final Provision provision,
            final boolean matching,
            final String... message)
            throws JobExecutionException {

        LOG.debug("Any to ignore {}", delta.getObject().getUid().getUidValue());

        List<ProvisioningReport> results = new ArrayList<>();

        if (anyKeys == null) {
            ProvisioningReport report = new ProvisioningReport();
            report.setKey(null);
            report.setName(delta.getObject().getUid().getUidValue());
            report.setOperation(ResourceOperation.NONE);
            report.setAnyType(provision.getAnyType().getKey());
            report.setStatus(ProvisioningReport.Status.SUCCESS);
            if (message != null && message.length >= 1) {
                report.setMessage(message[0]);
            }

            results.add(report);
        } else {
            for (String anyKey : anyKeys) {
                ProvisioningReport report = new ProvisioningReport();
                report.setKey(anyKey);
                report.setName(delta.getObject().getUid().getUidValue());
                report.setOperation(ResourceOperation.NONE);
                report.setAnyType(provision.getAnyType().getKey());
                report.setStatus(ProvisioningReport.Status.SUCCESS);
                if (message != null && message.length >= 1) {
                    report.setMessage(message[0]);
                }

                results.add(report);
            }
        }

        finalize(matching
                ? MatchingRule.toEventName(MatchingRule.IGNORE)
                : UnmatchingRule.toEventName(UnmatchingRule.IGNORE), Result.SUCCESS, null, null, delta);

        return results;
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

        SyncDelta processed = delta;
        for (PullActions action : profile.getActions()) {
            processed = action.preprocess(profile, processed);
        }

        LOG.debug("Transformed {} for {} as {}",
                processed.getDeltaType(), processed.getUid().getUidValue(), processed.getObject().getObjectClass());

        try {
            List<String> keys = pullUtils.match(processed, provision, anyUtils);
            LOG.debug("Match(es) found for {} as {}: {}",
                    processed.getUid().getUidValue(), processed.getObject().getObjectClass(), keys);

            if (keys.size() > 1) {
                switch (profile.getConflictResolutionAction()) {
                    case IGNORE:
                        throw new IgnoreProvisionException("More than one match found for "
                                + processed.getObject().getUid().getUidValue() + ": " + keys);

                    case FIRSTMATCH:
                        keys = keys.subList(0, 1);
                        break;

                    case LASTMATCH:
                        keys = keys.subList(keys.size() - 1, keys.size());
                        break;

                    default:
                    // keep anyKeys unmodified
                }
            }

            if (SyncDeltaType.CREATE_OR_UPDATE == processed.getDeltaType()) {
                if (keys.isEmpty()) {
                    switch (profile.getTask().getUnmatchingRule()) {
                        case ASSIGN:
                            profile.getResults().addAll(assign(processed, provision, anyUtils));
                            break;

                        case PROVISION:
                            profile.getResults().addAll(provision(processed, provision, anyUtils));
                            break;

                        case IGNORE:
                            profile.getResults().addAll(ignore(processed, null, provision, false));
                            break;

                        default:
                        // do nothing
                    }
                } else {
                    // update VirAttrCache
                    for (VirSchema virSchema : virSchemaDAO.findByProvision(provision)) {
                        Attribute attr = processed.getObject().getAttributeByName(virSchema.getExtAttrName());
                        for (String anyKey : keys) {
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
                            profile.getResults().addAll(update(processed, keys, provision));
                            break;

                        case DEPROVISION:
                            profile.getResults().addAll(deprovision(processed, keys, provision, false));
                            break;

                        case UNASSIGN:
                            profile.getResults().addAll(deprovision(processed, keys, provision, true));
                            break;

                        case LINK:
                            profile.getResults().addAll(link(processed, keys, provision, false));
                            break;

                        case UNLINK:
                            profile.getResults().addAll(link(processed, keys, provision, true));
                            break;

                        case IGNORE:
                            profile.getResults().addAll(ignore(processed, keys, provision, true));
                            break;

                        default:
                        // do nothing
                    }
                }
            } else if (SyncDeltaType.DELETE == processed.getDeltaType()) {
                if (keys.isEmpty()) {
                    finalize(ResourceOperation.DELETE.name().toLowerCase(), Result.SUCCESS, null, null, processed);
                    LOG.debug("No match found for deletion");
                } else {
                    profile.getResults().addAll(delete(processed, keys, provision));
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

        AnyUtils anyUtils = getAnyUtils();

        notificationManager.createTasks(
                AuthContextUtils.getUsername(),
                AuditElements.EventCategoryType.PULL,
                anyUtils.anyTypeKind().name().toLowerCase(),
                profile.getTask().getResource().getKey(),
                event,
                result,
                before,
                output,
                delta,
                furtherInput);

        auditManager.audit(
                AuthContextUtils.getUsername(),
                AuditElements.EventCategoryType.PULL,
                anyUtils.anyTypeKind().name().toLowerCase(),
                profile.getTask().getResource().getKey(),
                event,
                result,
                before,
                output,
                delta,
                furtherInput);
    }
}

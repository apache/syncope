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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.patch.StringPatchItem;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.Result;
import org.apache.syncope.common.lib.types.MatchType;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PullMatch;
import org.apache.syncope.core.persistence.api.dao.RemediationDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Remediation;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.ProvisioningManager;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCache;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCacheKey;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCacheValue;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationException;
import org.apache.syncope.core.provisioning.api.pushpull.IgnoreProvisionException;
import org.apache.syncope.core.provisioning.api.pushpull.PullActions;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePullExecutor;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePullResultHandler;
import org.apache.syncope.core.provisioning.java.utils.ConnObjectUtils;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.types.TaskType;

@Transactional(rollbackFor = Throwable.class)
public abstract class AbstractPullResultHandler extends AbstractSyncopeResultHandler<PullTask, PullActions>
        implements SyncopePullResultHandler {

    @Autowired
    protected InboundMatcher inboundMatcher;

    @Autowired
    protected NotificationManager notificationManager;

    @Autowired
    protected AuditManager auditManager;

    @Autowired
    protected ConnObjectUtils connObjectUtils;

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected AnyTypeDAO anyTypeDAO;

    @Autowired
    protected TaskDAO taskDAO;

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

    protected abstract ProvisioningManager<?, ?> getProvisioningManager();

    protected abstract AnyTO doCreate(AnyTO anyTO, SyncDelta delta);

    protected abstract AnyPatch doUpdate(AnyTO before, AnyPatch anyPatch, SyncDelta delta, ProvisioningReport result);

    @Override
    public void setPullExecutor(final SyncopePullExecutor executor) {
        this.executor = executor;
    }

    @Override
    public boolean handle(final SyncDelta delta) {
        Provision provision = null;
        try {
            provision = profile.getTask().getResource().getProvision(delta.getObject().getObjectClass()).
                    orElseThrow(() -> new JobExecutionException(
                    "No provision found on " + profile.getTask().getResource()
                    + " for " + delta.getObject().getObjectClass()));

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
            ignoreResult.setUidValue(delta.getUid().getUidValue());
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

    protected List<ProvisioningReport> provision(
            final UnmatchingRule rule,
            final SyncDelta delta,
            final Provision provision) throws JobExecutionException {

        if (!profile.getTask().isPerformCreate()) {
            LOG.debug("PullTask not configured for create");
            end(provision.getAnyType().getKind(), UnmatchingRule.toEventName(rule), Result.SUCCESS, null, null, delta);
            return Collections.<ProvisioningReport>emptyList();
        }

        AnyTO anyTO = connObjectUtils.getAnyTO(delta.getObject(), profile.getTask(), provision, true);
        if (rule == UnmatchingRule.ASSIGN) {
            anyTO.getResources().add(profile.getTask().getResource().getKey());
        }

        ProvisioningReport result = new ProvisioningReport();
        result.setOperation(ResourceOperation.CREATE);
        result.setAnyType(provision.getAnyType().getKey());
        result.setStatus(ProvisioningReport.Status.SUCCESS);
        result.setName(getName(anyTO));
        result.setUidValue(delta.getUid().getUidValue());

        if (profile.isDryRun()) {
            result.setKey(null);
            end(provision.getAnyType().getKind(), UnmatchingRule.toEventName(rule), Result.SUCCESS, null, null, delta);
        } else {
            Object output;
            Result resultStatus;
            try {
                for (PullActions action : profile.getActions()) {
                    if (rule == UnmatchingRule.ASSIGN) {
                        action.beforeAssign(profile, delta, anyTO);
                    } else if (rule == UnmatchingRule.PROVISION) {
                        action.beforeProvision(profile, delta, anyTO);
                    }
                }
                result.setName(getName(anyTO));

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

                if (profile.getTask().isRemediation()) {
                    // set to SUCCESS to let the incremental flow go on in case of errors
                    resultStatus = Result.SUCCESS;
                    createRemediation(
                            provision.getAnyType(),
                            null,
                            anyTO,
                            null,
                            taskDAO.exists(TaskType.PULL, profile.getTask().getKey())
                            ? profile.getTask() : null,
                            result,
                            delta);
                } else {
                    resultStatus = Result.FAILURE;
                }
            }

            end(provision.getAnyType().getKind(), UnmatchingRule.toEventName(rule), resultStatus, null, output, delta);
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

    protected List<ProvisioningReport> update(
            final SyncDelta delta,
            final List<PullMatch> matches,
            final Provision provision) throws JobExecutionException {

        if (!profile.getTask().isPerformUpdate()) {
            LOG.debug("PullTask not configured for update");
            end(provision.getAnyType().getKind(),
                    MatchingRule.toEventName(MatchingRule.UPDATE), Result.SUCCESS, null, null, delta);
            return Collections.<ProvisioningReport>emptyList();
        }

        LOG.debug("About to update {}", matches);

        List<ProvisioningReport> results = new ArrayList<>();

        for (PullMatch match : matches) {
            LOG.debug("About to update {}", match);

            ProvisioningReport result = new ProvisioningReport();
            result.setOperation(ResourceOperation.UPDATE);
            result.setAnyType(provision.getAnyType().getKey());
            result.setStatus(ProvisioningReport.Status.SUCCESS);
            result.setKey(match.getAny().getKey());
            result.setUidValue(delta.getUid().getUidValue());

            AnyTO before = getAnyTO(match.getAny());
            if (before == null) {
                result.setStatus(ProvisioningReport.Status.FAILURE);
                result.setMessage(String.format("Any '%s(%s)' not found", provision.getAnyType().getKey(), match));
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
                    AnyPatch anyPatch = null;
                    try {
                        anyPatch = connObjectUtils.getAnyPatch(
                                before.getKey(),
                                delta.getObject(),
                                before,
                                profile.getTask(),
                                provision);

                        for (PullActions action : profile.getActions()) {
                            action.beforeUpdate(profile, delta, before, anyPatch);
                        }

                        effectivePatch = doUpdate(before, anyPatch, delta, result);
                        AnyTO updated = AnyOperations.patch(before, effectivePatch);

                        for (PullActions action : profile.getActions()) {
                            action.after(profile, delta, updated, result);
                        }

                        output = updated;
                        resultStatus = Result.SUCCESS;
                        result.setName(getName(updated));

                        LOG.debug("{} {} successfully updated", provision.getAnyType().getKey(), match);
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

                        if (profile.getTask().isRemediation()) {
                            // set to SUCCESS to let the incremental flow go on in case of errors
                            resultStatus = Result.SUCCESS;
                            createRemediation(
                                    provision.getAnyType(),
                                    anyPatch,
                                    taskDAO.exists(TaskType.PULL, profile.getTask().getKey())
                                    ? profile.getTask() : null,
                                    result,
                                    delta);
                        } else {
                            resultStatus = Result.FAILURE;
                        }
                    }
                }
                end(provision.getAnyType().getKind(),
                        MatchingRule.toEventName(MatchingRule.UPDATE),
                        resultStatus, before, output, delta, effectivePatch);
            }
            results.add(result);
        }
        return results;
    }

    protected List<ProvisioningReport> deprovision(
            final MatchingRule matchingRule,
            final SyncDelta delta,
            final List<PullMatch> matches,
            final Provision provision)
            throws JobExecutionException {

        if (!profile.getTask().isPerformUpdate()) {
            LOG.debug("PullTask not configured for update");
            end(provision.getAnyType().getKind(),
                    MatchingRule.toEventName(matchingRule), Result.SUCCESS, null, null, delta);
            return Collections.<ProvisioningReport>emptyList();
        }

        LOG.debug("About to deprovision {}", matches);

        List<ProvisioningReport> results = new ArrayList<>();

        for (PullMatch match : matches) {
            LOG.debug("About to unassign resource {}", match);

            ProvisioningReport result = new ProvisioningReport();
            result.setOperation(ResourceOperation.DELETE);
            result.setAnyType(provision.getAnyType().getKey());
            result.setStatus(ProvisioningReport.Status.SUCCESS);
            result.setKey(match.getAny().getKey());
            result.setUidValue(delta.getUid().getUidValue());

            AnyTO before = getAnyTO(match.getAny());

            if (before == null) {
                result.setStatus(ProvisioningReport.Status.FAILURE);
                result.setMessage(String.format("Any '%s(%s)' not found", provision.getAnyType().getKey(), match));
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
                        if (matchingRule == MatchingRule.UNASSIGN) {
                            for (PullActions action : profile.getActions()) {
                                action.beforeUnassign(profile, delta, before);
                            }
                        } else if (matchingRule == MatchingRule.DEPROVISION) {
                            for (PullActions action : profile.getActions()) {
                                action.beforeDeprovision(profile, delta, before);
                            }
                        }

                        PropagationByResource<String> propByRes = new PropagationByResource<>();
                        propByRes.add(ResourceOperation.DELETE, provision.getResource().getKey());

                        taskExecutor.execute(propagationManager.getDeleteTasks(
                                provision.getAnyType().getKind(),
                                match.getAny().getKey(),
                                propByRes,
                                null,
                                null),
                                false);

                        AnyPatch anyPatch = null;
                        if (matchingRule == MatchingRule.UNASSIGN) {
                            anyPatch = getAnyUtils().newAnyPatch(match.getAny().getKey());
                            anyPatch.getResources().add(new StringPatchItem.Builder().
                                    operation(PatchOperation.DELETE).
                                    value(profile.getTask().getResource().getKey()).build());
                        }
                        if (anyPatch == null) {
                            output = getAnyTO(match.getAny());
                        } else {
                            output = doUpdate(before, anyPatch, delta, result);
                        }

                        for (PullActions action : profile.getActions()) {
                            action.after(profile, delta, AnyTO.class.cast(output), result);
                        }

                        resultStatus = Result.SUCCESS;

                        LOG.debug("{} {} successfully updated", provision.getAnyType().getKey(), match);
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
                end(provision.getAnyType().getKind(),
                        MatchingRule.toEventName(matchingRule), resultStatus, before, output, delta);
            }
            results.add(result);
        }

        return results;
    }

    protected List<ProvisioningReport> link(
            final SyncDelta delta,
            final List<PullMatch> matches,
            final Provision provision,
            final boolean unlink)
            throws JobExecutionException {

        if (!profile.getTask().isPerformUpdate()) {
            LOG.debug("PullTask not configured for update");
            end(provision.getAnyType().getKind(),
                    unlink
                            ? MatchingRule.toEventName(MatchingRule.UNLINK)
                            : MatchingRule.toEventName(MatchingRule.LINK),
                    Result.SUCCESS, null, null, delta);
            return Collections.<ProvisioningReport>emptyList();
        }

        LOG.debug("About to update {}", matches);

        final List<ProvisioningReport> results = new ArrayList<>();

        for (PullMatch match : matches) {
            LOG.debug("About to unassign resource {}", match);

            ProvisioningReport result = new ProvisioningReport();
            result.setOperation(ResourceOperation.NONE);
            result.setAnyType(provision.getAnyType().getKey());
            result.setStatus(ProvisioningReport.Status.SUCCESS);
            result.setKey(match.getAny().getKey());
            result.setUidValue(delta.getUid().getUidValue());

            AnyTO before = getAnyTO(match.getAny());

            if (before == null) {
                result.setStatus(ProvisioningReport.Status.FAILURE);
                result.setMessage(String.format("Any '%s(%s)' not found", provision.getAnyType().getKey(), match));
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

                        AnyPatch anyPatch = getAnyUtils().newAnyPatch(before.getKey());
                        anyPatch.getResources().add(new StringPatchItem.Builder().
                                operation(unlink ? PatchOperation.DELETE : PatchOperation.ADD_REPLACE).
                                value(profile.getTask().getResource().getKey()).build());

                        effectivePatch = update(anyPatch).getResult();
                        output = AnyOperations.patch(before, effectivePatch);

                        for (PullActions action : profile.getActions()) {
                            action.after(profile, delta, AnyTO.class.cast(output), result);
                        }

                        resultStatus = Result.SUCCESS;

                        LOG.debug("{} {} successfully updated", provision.getAnyType().getKey(), match);
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
                end(provision.getAnyType().getKind(),
                        unlink
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
            final List<PullMatch> matches,
            final Provision provision)
            throws JobExecutionException {

        if (!profile.getTask().isPerformDelete()) {
            LOG.debug("PullTask not configured for delete");
            end(provision.getAnyType().getKind(),
                    ResourceOperation.DELETE.name().toLowerCase(), Result.SUCCESS, null, null, delta);
            return Collections.<ProvisioningReport>emptyList();
        }

        LOG.debug("About to delete {}", matches);

        List<ProvisioningReport> results = new ArrayList<>();

        matches.forEach(match -> {
            Object output;
            Result resultStatus = Result.FAILURE;

            ProvisioningReport result = new ProvisioningReport();

            try {
                AnyTO before = getAnyTO(match.getAny());

                result.setKey(match.getAny().getKey());
                result.setName(getName(before));
                result.setOperation(ResourceOperation.DELETE);
                result.setAnyType(provision.getAnyType().getKey());
                result.setStatus(ProvisioningReport.Status.SUCCESS);
                result.setUidValue(delta.getUid().getUidValue());

                if (!profile.isDryRun()) {
                    for (PullActions action : profile.getActions()) {
                        action.beforeDelete(profile, delta, before);
                    }

                    try {
                        getProvisioningManager().delete(
                                match.getAny().getKey(),
                                Collections.singleton(profile.getTask().getResource().getKey()),
                                true);
                        output = null;
                        resultStatus = Result.SUCCESS;

                        for (PullActions action : profile.getActions()) {
                            action.after(profile, delta, before, result);
                        }
                    } catch (Exception e) {
                        throwIgnoreProvisionException(delta, e);

                        result.setStatus(ProvisioningReport.Status.FAILURE);
                        result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error("Could not delete {} {}", provision.getAnyType().getKey(), match, e);
                        output = e;

                        if (profile.getTask().isRemediation()) {
                            // set to SUCCESS to let the incremental flow go on in case of errors
                            resultStatus = Result.SUCCESS;
                            createRemediation(
                                    provision.getAnyType(),
                                    match.getAny().getKey(),
                                    null,
                                    null,
                                    taskDAO.exists(TaskType.PULL, profile.getTask().getKey())
                                    ? profile.getTask() : null,
                                    result,
                                    delta);
                        }
                    }

                    end(provision.getAnyType().getKind(),
                            ResourceOperation.DELETE.name().toLowerCase(), resultStatus, before, output, delta);
                }

                results.add(result);
            } catch (NotFoundException e) {
                LOG.error("Could not find {} {}", provision.getAnyType().getKey(), match, e);
            } catch (DelegatedAdministrationException e) {
                LOG.error("Not allowed to read {} {}", provision.getAnyType().getKey(), match, e);
            } catch (Exception e) {
                LOG.error("Could not delete {} {}", provision.getAnyType().getKey(), match, e);
            }
        });

        return results;
    }

    protected List<ProvisioningReport> ignore(
            final SyncDelta delta,
            final List<PullMatch> matches,
            final Provision provision,
            final boolean matching,
            final String... message)
            throws JobExecutionException {

        LOG.debug("Any to ignore {}", delta.getObject().getUid().getUidValue());

        List<ProvisioningReport> results = new ArrayList<>();

        if (matches == null) {
            ProvisioningReport report = new ProvisioningReport();
            report.setKey(null);
            report.setName(delta.getObject().getUid().getUidValue());
            report.setOperation(ResourceOperation.NONE);
            report.setAnyType(provision.getAnyType().getKey());
            report.setStatus(ProvisioningReport.Status.SUCCESS);
            report.setUidValue(delta.getUid().getUidValue());
            if (message != null && message.length >= 1) {
                report.setMessage(message[0]);
            }

            results.add(report);
        } else {
            matches.forEach(match -> {
                ProvisioningReport report = new ProvisioningReport();
                report.setKey(match.getAny().getKey());
                report.setName(delta.getObject().getUid().getUidValue());
                report.setOperation(ResourceOperation.NONE);
                report.setAnyType(provision.getAnyType().getKey());
                report.setStatus(ProvisioningReport.Status.SUCCESS);
                report.setUidValue(delta.getUid().getUidValue());
                if (message != null && message.length >= 1) {
                    report.setMessage(message[0]);
                }

                results.add(report);
            });
        }

        end(provision.getAnyType().getKind(),
                matching
                        ? MatchingRule.toEventName(MatchingRule.IGNORE)
                        : UnmatchingRule.toEventName(UnmatchingRule.IGNORE), Result.SUCCESS, null, null, delta);

        return results;
    }

    protected void handleAnys(
            final SyncDelta delta,
            final List<PullMatch> matches,
            final Provision provision) throws JobExecutionException {

        if (matches.isEmpty()) {
            LOG.debug("Nothing to do");
            return;
        }

        switch (delta.getDeltaType()) {
            case CREATE:
            case UPDATE:
            case CREATE_OR_UPDATE:
                if (matches.get(0).getAny() == null) {
                    switch (profile.getTask().getUnmatchingRule()) {
                        case ASSIGN:
                        case PROVISION:
                            profile.getResults().addAll(
                                    provision(profile.getTask().getUnmatchingRule(), delta, provision));
                            break;

                        case IGNORE:
                            profile.getResults().addAll(ignore(delta, null, provision, false));
                            break;

                        default:
                        // do nothing
                    }
                } else {
                    // update VirAttrCache
                    virSchemaDAO.findByProvision(provision).forEach(schema -> {
                        Attribute attr = delta.getObject().getAttributeByName(schema.getExtAttrName());
                        matches.forEach(match -> {
                            VirAttrCacheKey cacheKey = new VirAttrCacheKey(
                                    provision.getAnyType().getKey(), match.getAny().getKey(),
                                    schema.getKey());
                            if (attr == null) {
                                virAttrCache.expire(cacheKey);
                            } else {
                                virAttrCache.put(cacheKey, new VirAttrCacheValue(attr.getValue()));
                            }
                        });
                    });

                    switch (profile.getTask().getMatchingRule()) {
                        case UPDATE:
                            profile.getResults().addAll(update(delta, matches, provision));
                            break;

                        case DEPROVISION:
                        case UNASSIGN:
                            profile.getResults().addAll(
                                    deprovision(profile.getTask().getMatchingRule(), delta, matches, provision));
                            break;

                        case LINK:
                            profile.getResults().addAll(link(delta, matches, provision, false));
                            break;

                        case UNLINK:
                            profile.getResults().addAll(link(delta, matches, provision, true));
                            break;

                        case IGNORE:
                            profile.getResults().addAll(ignore(delta, matches, provision, true));
                            break;

                        default:
                        // do nothing
                    }
                }
                break;

            case DELETE:
                // Skip DELETE in case of PullCorrelationRule.NO_MATCH
                if (matches.get(0).getAny() != null) {
                    profile.getResults().addAll(delete(delta, matches, provision));
                }
                break;

            default:
        }
    }

    protected void handleLinkedAccounts(
            final SyncDelta delta,
            final List<PullMatch> matches,
            final Provision provision) throws JobExecutionException {

        if (matches.isEmpty()) {
            LOG.debug("Nothing to do");
            return;
        }

        // nothing to do in the general case
        LOG.warn("Unexpected linked accounts found for {}: {}", provision.getAnyType().getKind(), matches);
    }

    /**
     * Look into SyncDelta and take necessary profile.getActions() (create / update / delete) on any object(s).
     *
     * @param delta returned by the underlying profile.getConnector()
     * @param provision provisioning info
     * @throws JobExecutionException in case of pull failure.
     */
    protected void doHandle(final SyncDelta delta, final Provision provision) throws JobExecutionException {
        LOG.debug("Process {} for {} as {}",
                delta.getDeltaType(), delta.getUid().getUidValue(), delta.getObject().getObjectClass());

        SyncDelta finalDelta = delta;
        for (PullActions action : profile.getActions()) {
            finalDelta = action.preprocess(profile, finalDelta);
        }

        LOG.debug("Transformed {} for {} as {}",
                finalDelta.getDeltaType(), finalDelta.getUid().getUidValue(), finalDelta.getObject().getObjectClass());

        try {
            List<PullMatch> matches = inboundMatcher.match(finalDelta, provision);
            LOG.debug("Match(es) found for {} as {}: {}",
                    finalDelta.getUid().getUidValue(), finalDelta.getObject().getObjectClass(), matches);

            if (matches.size() > 1) {
                switch (profile.getConflictResolutionAction()) {
                    case IGNORE:
                        throw new IgnoreProvisionException("More than one match found for "
                                + finalDelta.getObject().getUid().getUidValue() + ": " + matches);

                    case FIRSTMATCH:
                        matches = matches.subList(0, 1);
                        break;

                    case LASTMATCH:
                        matches = matches.subList(matches.size() - 1, matches.size());
                        break;

                    default:
                    // keep matches unmodified
                }
            }

            // users, groups and any objects
            handleAnys(
                    finalDelta,
                    matches.stream().
                            filter(match -> match.getMatchTarget() == MatchType.ANY).
                            collect(Collectors.toList()), provision);

            // linked accounts
            handleLinkedAccounts(
                    finalDelta,
                    matches.stream().
                            filter(match -> match.getMatchTarget() == MatchType.LINKED_ACCOUNT).
                            collect(Collectors.toList()), provision);
        } catch (IllegalStateException | IllegalArgumentException e) {
            LOG.warn(e.getMessage());
        }
    }

    protected void end(
            final AnyTypeKind anyTypeKind,
            final String event,
            final Result result,
            final Object before,
            final Object output,
            final SyncDelta delta,
            final Object... furtherInput) {

        synchronized (this) {
            this.latestResult = result;
        }

        notificationManager.createTasks(
                AuthContextUtils.getWho(),
                AuditElements.EventCategoryType.PULL,
                anyTypeKind.name().toLowerCase(),
                profile.getTask().getResource().getKey(),
                event,
                result,
                before,
                output,
                delta,
                furtherInput);

        auditManager.audit(
                AuthContextUtils.getWho(),
                AuditElements.EventCategoryType.PULL,
                anyTypeKind.name().toLowerCase(),
                profile.getTask().getResource().getKey(),
                event,
                result,
                before,
                output,
                delta,
                furtherInput);
    }

    protected void createRemediationIfNeeded(
            final AnyPatch anyPatch,
            final SyncDelta delta,
            final ProvisioningReport result) {

        if (ProvisioningReport.Status.FAILURE == result.getStatus() && profile.getTask().isRemediation()) {
            createRemediation(
                    anyTypeDAO.find(result.getAnyType()),
                    null,
                    null,
                    anyPatch,
                    taskDAO.exists(TaskType.PULL, profile.getTask().getKey()) ? profile.getTask() : null,
                    result,
                    delta);
        }
    }

    protected void createRemediation(
            final AnyType anyType,
            final AnyPatch anyPatch,
            final PullTask pullTask,
            final ProvisioningReport result,
            final SyncDelta delta) {

        createRemediation(anyType, null, null, anyPatch, pullTask, result, delta);
    }

    protected void createRemediation(
            final AnyType anyType,
            final String anyKey,
            final AnyTO anyTO,
            final AnyPatch anyPatch,
            final PullTask pullTask,
            final ProvisioningReport result,
            final SyncDelta delta) {

        Remediation remediation = entityFactory.newEntity(Remediation.class);

        remediation.setAnyType(anyType);
        remediation.setOperation(anyPatch == null ? ResourceOperation.CREATE : ResourceOperation.UPDATE);
        if (StringUtils.isNotBlank(anyKey)) {
            remediation.setPayload(anyKey);
        } else if (anyTO != null) {
            remediation.setPayload(anyTO);
        } else if (anyPatch != null) {
            remediation.setPayload(anyPatch);
        }
        remediation.setError(result.getMessage());
        remediation.setInstant(new Date());
        remediation.setRemoteName(delta.getObject().getName().getNameValue());
        remediation.setPullTask(pullTask);

        remediation = remediationDAO.save(remediation);

        ProvisioningReport remediationResult = new ProvisioningReport();
        remediationResult.setOperation(remediation.getOperation());
        remediationResult.setAnyType(anyType.getKey());
        remediationResult.setStatus(ProvisioningReport.Status.FAILURE);
        remediationResult.setMessage(remediation.getError());
        if (StringUtils.isNotBlank(anyKey)) {
            remediationResult.setKey(anyKey);
        } else if (anyTO != null) {
            remediationResult.setKey(anyTO.getKey());
        } else if (anyPatch != null) {
            remediationResult.setKey(anyPatch.getKey());
        }
        remediationResult.setUidValue(delta.getUid().getUidValue());
        remediationResult.setName(remediation.getRemoteName());
        profile.getResults().add(remediationResult);
    }
}

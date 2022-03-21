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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.Result;
import org.apache.syncope.common.lib.types.MatchType;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RemediationDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.provisioning.api.propagation.PropagationException;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Remediation;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.ProvisioningManager;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCache;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCacheValue;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.provisioning.api.pushpull.IgnoreProvisionException;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.provisioning.api.pushpull.PullActions;
import org.apache.syncope.core.persistence.api.dao.PullMatch;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCacheKey;
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

    protected abstract String getName(AnyCR anyCR);

    protected abstract ProvisioningManager<?, ?> getProvisioningManager();

    protected abstract AnyTO doCreate(AnyCR anyCR, SyncDelta delta);

    protected abstract AnyUR doUpdate(AnyTO before, AnyUR anyUR, SyncDelta delta, ProvisioningReport result);

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
            return List.of();
        }

        AnyCR anyCR = connObjectUtils.getAnyCR(delta.getObject(), profile.getTask(), provision, true);
        if (rule == UnmatchingRule.ASSIGN) {
            anyCR.getResources().add(profile.getTask().getResource().getKey());
        }

        ProvisioningReport result = new ProvisioningReport();
        result.setOperation(ResourceOperation.CREATE);
        result.setAnyType(provision.getAnyType().getKey());
        result.setStatus(ProvisioningReport.Status.SUCCESS);
        result.setName(getName(anyCR));
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
                        action.beforeAssign(profile, delta, anyCR);
                    } else if (rule == UnmatchingRule.PROVISION) {
                        action.beforeProvision(profile, delta, anyCR);
                    }
                }
                result.setName(getName(anyCR));

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
                LOG.error("Could not propagate {} {}",
                        provision.getAnyType().getKey(), delta.getUid().getUidValue(), e);
                output = e;
                resultStatus = Result.FAILURE;
            } catch (Exception e) {
                throwIgnoreProvisionException(delta, e);

                result.setStatus(ProvisioningReport.Status.FAILURE);
                result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                LOG.error("Could not create {} {} ", provision.getAnyType().getKey(), delta.getUid().getUidValue(), e);
                output = e;

                if (profile.getTask().isRemediation()) {
                    // set to SUCCESS to let the incremental flow go on in case of errors
                    resultStatus = Result.SUCCESS;
                    createRemediation(
                            provision.getAnyType(),
                            null,
                            anyCR,
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

    protected List<ProvisioningReport> update(
            final SyncDelta delta,
            final List<PullMatch> matches,
            final Provision provision) throws JobExecutionException {

        if (!profile.getTask().isPerformUpdate()) {
            LOG.debug("PullTask not configured for update");
            end(provision.getAnyType().getKind(),
                    MatchingRule.toEventName(MatchingRule.UPDATE), Result.SUCCESS, null, null, delta);
            return List.of();
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
                                provision);

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
                                    anyUR,
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
                        resultStatus, before, output, delta, effectiveReq);
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
            return List.of();
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
                                false,
                                securityProperties.getAdminUser());

                        AnyUR anyUR = null;
                        if (matchingRule == MatchingRule.UNASSIGN) {
                            anyUR = getAnyUtils().newAnyUR(match.getAny().getKey());
                            anyUR.getResources().add(new StringPatchItem.Builder().
                                    operation(PatchOperation.DELETE).
                                    value(profile.getTask().getResource().getKey()).build());
                        }
                        if (anyUR == null) {
                            output = getAnyTO(match.getAny());
                        } else {
                            output = doUpdate(before, anyUR, delta, result);
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
            return List.of();
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
                        resultStatus, before, output, delta, effectiveReq);
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
            return List.of();
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
                                Set.of(profile.getTask().getResource().getKey()),
                                true,
                                profile.getExecutor(),
                                getContext());
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

        if (SyncDeltaType.CREATE_OR_UPDATE == delta.getDeltaType()) {
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
        } else if (SyncDeltaType.DELETE == delta.getDeltaType()) {
            profile.getResults().addAll(delete(delta, matches, provision));
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
            final AnyUR anyUR,
            final SyncDelta delta,
            final ProvisioningReport result) {

        if (ProvisioningReport.Status.FAILURE == result.getStatus() && profile.getTask().isRemediation()) {
            createRemediation(
                    anyTypeDAO.find(result.getAnyType()),
                    null,
                    null,
                    anyUR,
                    taskDAO.exists(TaskType.PULL, profile.getTask().getKey()) ? profile.getTask() : null,
                    result,
                    delta);
        }
    }

    protected void createRemediation(
            final AnyType anyType,
            final AnyCR anyCR,
            final PullTask pullTask,
            final ProvisioningReport result,
            final SyncDelta delta) {

        createRemediation(anyType, null, anyCR, null, pullTask, result, delta);
    }

    protected void createRemediation(
            final AnyType anyType,
            final AnyUR anyUR,
            final PullTask pullTask,
            final ProvisioningReport result,
            final SyncDelta delta) {

        createRemediation(anyType, null, null, anyUR, pullTask, result, delta);
    }

    protected void createRemediation(
            final AnyType anyType,
            final String anyKey,
            final AnyCR anyCR,
            final AnyUR anyUR,
            final PullTask pullTask,
            final ProvisioningReport result,
            final SyncDelta delta) {

        Remediation remediation = entityFactory.newEntity(Remediation.class);

        remediation.setAnyType(anyType);
        remediation.setOperation(anyUR == null ? ResourceOperation.CREATE : ResourceOperation.UPDATE);
        if (StringUtils.isNotBlank(anyKey)) {
            remediation.setPayload(anyKey);
        } else if (anyCR != null) {
            remediation.setPayload(anyCR);
        } else if (anyUR != null) {
            remediation.setPayload(anyUR);
        }
        remediation.setError(result.getMessage());
        remediation.setInstant(OffsetDateTime.now());
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
        } else if (anyUR != null) {
            remediationResult.setKey(anyUR.getKey());
        }
        remediationResult.setUidValue(delta.getUid().getUidValue());
        remediationResult.setName(remediation.getRemoteName());
        profile.getResults().add(remediationResult);
    }
}

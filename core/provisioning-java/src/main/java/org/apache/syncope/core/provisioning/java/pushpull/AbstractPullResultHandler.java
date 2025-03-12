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
import java.util.List;
import java.util.Set;
import javax.cache.Cache;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.MatchType;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RemediationDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Remediation;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.ProvisioningManager;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationException;
import org.apache.syncope.core.provisioning.api.pushpull.IgnoreProvisionException;
import org.apache.syncope.core.provisioning.api.pushpull.InboundActions;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePullResultHandler;
import org.apache.syncope.core.provisioning.api.rules.InboundMatch;
import org.apache.syncope.core.provisioning.java.cache.VirAttrCacheKey;
import org.apache.syncope.core.provisioning.java.cache.VirAttrCacheValue;
import org.apache.syncope.core.provisioning.java.utils.ConnObjectUtils;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractPullResultHandler
        extends AbstractSyncopeResultHandler<PullTask, InboundActions>
        implements SyncopePullResultHandler {

    protected static OpEvent.Outcome and(final OpEvent.Outcome left, final OpEvent.Outcome right) {
        return left == OpEvent.Outcome.SUCCESS && right == OpEvent.Outcome.SUCCESS
                ? OpEvent.Outcome.SUCCESS
                : OpEvent.Outcome.FAILURE;
    }

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
    protected Cache<VirAttrCacheKey, VirAttrCacheValue> virAttrCache;

    @Autowired
    protected EntityFactory entityFactory;

    protected abstract String getName(AnyTO anyTO);

    protected abstract String getName(AnyCR anyCR);

    protected abstract ProvisioningManager<?, ?> getProvisioningManager();

    protected abstract AnyTO doCreate(AnyCR anyCR, SyncDelta delta);

    protected abstract AnyUR doUpdate(AnyTO before, AnyUR anyUR, SyncDelta delta, ProvisioningReport result);

    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRES_NEW)
    @Override
    public boolean handle(final SyncDelta delta) {
        Provision provision = null;
        try {
            provision = profile.getTask().getResource().
                    getProvisionByObjectClass(delta.getObject().getObjectClass().getObjectClassValue()).
                    orElseThrow(() -> new JobExecutionException(
                    "No provision found on " + profile.getTask().getResource()
                    + " for " + delta.getObject().getObjectClass()));

            String anyType = provision.getAnyType();
            OpEvent.Outcome latestResult = doHandle(
                    delta,
                    provision,
                    anyTypeDAO.findById(anyType).
                            orElseThrow(() -> new NotFoundException("AnyType " + anyType)).
                            getKind());

            LOG.debug("Successfully handled {}", delta);

            if (stopRequested) {
                LOG.debug("Stop was requested");
                return false;
            }

            if (profile.getTask().getPullMode() != PullMode.INCREMENTAL) {
                return true;
            }
            return latestResult == OpEvent.Outcome.SUCCESS;
        } catch (IgnoreProvisionException e) {
            ProvisioningReport ignoreResult = new ProvisioningReport();
            ignoreResult.setOperation(ResourceOperation.NONE);
            ignoreResult.setAnyType(provision == null
                    ? getAnyUtils().anyTypeKind().name() : provision.getAnyType());
            ignoreResult.setStatus(ProvisioningReport.Status.IGNORE);
            ignoreResult.setMessage(e.getMessage());
            ignoreResult.setKey(null);
            ignoreResult.setUidValue(delta.getUid().getUidValue());
            ignoreResult.setName(delta.getObject().getName().getNameValue());
            profile.getResults().add(ignoreResult);

            LOG.warn("Ignoring during pull", e);

            return true;
        } catch (JobExecutionException e) {
            LOG.error("Pull failed", e);

            return false;
        }
    }

    protected void throwIgnoreProvisionException(final SyncDelta delta, final Exception exception) {

        if (exception instanceof IgnoreProvisionException) {
            throw IgnoreProvisionException.class.cast(exception);
        }

        IgnoreProvisionException ipe = null;
        for (InboundActions action : profile.getActions()) {
            if (ipe == null) {
                ipe = action.onError(profile, delta, exception);
            }
        }
        if (ipe != null) {
            throw ipe;
        }
    }

    protected OpEvent.Outcome provision(
            final UnmatchingRule rule,
            final SyncDelta delta,
            final AnyTypeKind anyTypeKind,
            final Provision provision) throws JobExecutionException {

        if (!profile.getTask().isPerformCreate()) {
            LOG.debug("PullTask not configured for create");
            end(provision.getAnyType(), UnmatchingRule.toOp(rule), OpEvent.Outcome.SUCCESS, null, null, delta);
            return OpEvent.Outcome.SUCCESS;
        }

        AnyCR anyCR = connObjectUtils.getAnyCR(
                delta.getObject(),
                profile.getTask(),
                anyTypeKind,
                provision,
                true);
        if (rule == UnmatchingRule.ASSIGN) {
            anyCR.getResources().add(profile.getTask().getResource().getKey());
        }

        ProvisioningReport result = new ProvisioningReport();
        result.setOperation(ResourceOperation.CREATE);
        result.setAnyType(provision.getAnyType());
        result.setStatus(ProvisioningReport.Status.SUCCESS);
        result.setName(getName(anyCR));
        result.setUidValue(delta.getUid().getUidValue());

        if (profile.isDryRun()) {
            result.setKey(null);
            end(provision.getAnyType(), UnmatchingRule.toOp(rule), OpEvent.Outcome.SUCCESS, null, null, delta);
            return OpEvent.Outcome.SUCCESS;
        }

        Object output;
        OpEvent.Outcome resultStatus;
        try {
            for (InboundActions action : profile.getActions()) {
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
            resultStatus = OpEvent.Outcome.SUCCESS;

            for (InboundActions action : profile.getActions()) {
                action.after(profile, delta, created, result);
            }

            LOG.debug("{} {} successfully created", created.getType(), created.getKey());
        } catch (PropagationException e) {
            // A propagation failure doesn't imply a pull failure.
            // The propagation exception status will be reported into the propagation task execution.
            LOG.error("Could not propagate {} {}",
                    provision.getAnyType(), delta.getUid().getUidValue(), e);
            output = e;
            resultStatus = OpEvent.Outcome.FAILURE;
        } catch (Exception e) {
            throwIgnoreProvisionException(delta, e);

            result.setStatus(ProvisioningReport.Status.FAILURE);
            result.setMessage(ExceptionUtils.getRootCauseMessage(e));
            LOG.error("Could not create {} {} ", provision.getAnyType(), delta.getUid().getUidValue(), e);
            output = e;

            if (profile.getTask().isRemediation()) {
                // set to SUCCESS to let the incremental flow go on in case of errors
                resultStatus = OpEvent.Outcome.SUCCESS;
                createRemediation(provision.getAnyType(), null, anyCR, null, result, delta);
            } else {
                resultStatus = OpEvent.Outcome.FAILURE;
            }
        }

        end(provision.getAnyType(), UnmatchingRule.toOp(rule), resultStatus, null, output, delta);
        profile.getResults().add(result);
        return resultStatus;
    }

    protected OpEvent.Outcome update(
            final SyncDelta delta,
            final List<InboundMatch> matches,
            final Provision provision) throws JobExecutionException {

        if (!profile.getTask().isPerformUpdate()) {
            LOG.debug("PullTask not configured for update");
            end(provision.getAnyType(),
                    MatchingRule.toOp(MatchingRule.UPDATE), OpEvent.Outcome.SUCCESS, null, null, delta);
            return OpEvent.Outcome.SUCCESS;
        }

        LOG.debug("About to update {}", matches);

        OpEvent.Outcome global = OpEvent.Outcome.SUCCESS;
        for (InboundMatch match : matches) {
            LOG.debug("About to update {}", match);

            ProvisioningReport result = new ProvisioningReport();
            result.setOperation(ResourceOperation.UPDATE);
            result.setAnyType(provision.getAnyType());
            result.setStatus(ProvisioningReport.Status.SUCCESS);
            result.setKey(match.getAny().getKey());
            result.setUidValue(delta.getUid().getUidValue());

            AnyTO before = getAnyTO(match.getAny());
            if (before == null) {
                result.setStatus(ProvisioningReport.Status.FAILURE);
                result.setMessage(String.format("Any '%s(%s)' not found", provision.getAnyType(), match));
            } else {
                result.setName(getName(before));
            }

            if (!profile.isDryRun()) {
                OpEvent.Outcome resultStatus;
                Object output;
                AnyUR effectiveReq = null;

                if (before == null) {
                    resultStatus = OpEvent.Outcome.FAILURE;
                    output = null;
                } else {
                    AnyUR anyUR = null;
                    try {
                        anyUR = connObjectUtils.getAnyUR(
                                before.getKey(),
                                delta.getObject(),
                                before,
                                profile.getTask(),
                                match.getAny().getType().getKind(),
                                provision);

                        for (InboundActions action : profile.getActions()) {
                            action.beforeUpdate(profile, delta, before, anyUR);
                        }

                        effectiveReq = doUpdate(before, anyUR, delta, result);
                        AnyTO updated = AnyOperations.patch(before, effectiveReq);

                        for (InboundActions action : profile.getActions()) {
                            action.after(profile, delta, updated, result);
                        }

                        output = updated;
                        resultStatus = OpEvent.Outcome.SUCCESS;
                        result.setName(getName(updated));

                        LOG.debug("{} {} successfully updated", provision.getAnyType(), match);
                    } catch (PropagationException e) {
                        // A propagation failure doesn't imply a pull failure.
                        // The propagation exception status will be reported into the propagation task execution.
                        LOG.error("Could not propagate {} {}",
                                provision.getAnyType(), delta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = OpEvent.Outcome.FAILURE;
                    } catch (Exception e) {
                        throwIgnoreProvisionException(delta, e);

                        result.setStatus(ProvisioningReport.Status.FAILURE);
                        result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error("Could not update {} {}",
                                provision.getAnyType(), delta.getUid().getUidValue(), e);
                        output = e;

                        if (profile.getTask().isRemediation()) {
                            // set to SUCCESS to let the incremental flow go on in case of errors
                            resultStatus = OpEvent.Outcome.SUCCESS;
                            createRemediation(provision.getAnyType(), null, null, anyUR, result, delta);
                        } else {
                            resultStatus = OpEvent.Outcome.FAILURE;
                        }
                    }
                }
                end(provision.getAnyType(),
                        MatchingRule.toOp(MatchingRule.UPDATE),
                        resultStatus, before, output, delta, effectiveReq);
                global = and(global, resultStatus);
            }

            profile.getResults().add(result);
        }

        return global;
    }

    protected OpEvent.Outcome deprovision(
            final MatchingRule matchingRule,
            final SyncDelta delta,
            final List<InboundMatch> matches,
            final Provision provision)
            throws JobExecutionException {

        if (!profile.getTask().isPerformUpdate()) {
            LOG.debug("PullTask not configured for update");
            end(provision.getAnyType(),
                    MatchingRule.toOp(matchingRule), OpEvent.Outcome.SUCCESS, null, null, delta);
            return OpEvent.Outcome.SUCCESS;
        }

        LOG.debug("About to deprovision {}", matches);

        OpEvent.Outcome global = OpEvent.Outcome.SUCCESS;
        for (InboundMatch match : matches) {
            LOG.debug("About to unassign resource {}", match);

            ProvisioningReport result = new ProvisioningReport();
            result.setOperation(ResourceOperation.DELETE);
            result.setAnyType(provision.getAnyType());
            result.setStatus(ProvisioningReport.Status.SUCCESS);
            result.setKey(match.getAny().getKey());
            result.setUidValue(delta.getUid().getUidValue());

            AnyTO before = getAnyTO(match.getAny());

            if (before == null) {
                result.setStatus(ProvisioningReport.Status.FAILURE);
                result.setMessage(String.format("Any '%s(%s)' not found", provision.getAnyType(), match));
            }

            if (!profile.isDryRun()) {
                Object output;
                OpEvent.Outcome resultStatus;

                if (before == null) {
                    resultStatus = OpEvent.Outcome.FAILURE;
                    output = null;
                } else {
                    result.setName(getName(before));

                    try {
                        if (matchingRule == MatchingRule.UNASSIGN) {
                            for (InboundActions action : profile.getActions()) {
                                action.beforeUnassign(profile, delta, before);
                            }
                        } else if (matchingRule == MatchingRule.DEPROVISION) {
                            for (InboundActions action : profile.getActions()) {
                                action.beforeDeprovision(profile, delta, before);
                            }
                        }

                        PropagationByResource<String> propByRes = new PropagationByResource<>();
                        propByRes.add(ResourceOperation.DELETE, profile.getTask().getResource().getKey());

                        taskExecutor.execute(propagationManager.getDeleteTasks(
                                match.getAny().getType().getKind(),
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

                        for (InboundActions action : profile.getActions()) {
                            action.after(profile, delta, AnyTO.class.cast(output), result);
                        }

                        resultStatus = OpEvent.Outcome.SUCCESS;

                        LOG.debug("{} {} successfully updated", provision.getAnyType(), match);
                    } catch (PropagationException e) {
                        // A propagation failure doesn't imply a pull failure.
                        // The propagation exception status will be reported into the propagation task execution.
                        LOG.error("Could not propagate {} {}",
                                provision.getAnyType(), delta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = OpEvent.Outcome.FAILURE;
                    } catch (Exception e) {
                        throwIgnoreProvisionException(delta, e);

                        result.setStatus(ProvisioningReport.Status.FAILURE);
                        result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error("Could not update {} {}",
                                provision.getAnyType(), delta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = OpEvent.Outcome.FAILURE;
                    }
                }
                end(provision.getAnyType(),
                        MatchingRule.toOp(matchingRule),
                        resultStatus, before, output, delta);
                global = and(global, resultStatus);
            }

            profile.getResults().add(result);
        }

        return global;
    }

    protected OpEvent.Outcome link(
            final SyncDelta delta,
            final List<InboundMatch> matches,
            final Provision provision,
            final boolean unlink)
            throws JobExecutionException {

        if (!profile.getTask().isPerformUpdate()) {
            LOG.debug("PullTask not configured for update");
            end(provision.getAnyType(),
                    unlink
                            ? MatchingRule.toOp(MatchingRule.UNLINK)
                            : MatchingRule.toOp(MatchingRule.LINK),
                    OpEvent.Outcome.SUCCESS, null, null, delta);
            return OpEvent.Outcome.SUCCESS;
        }

        LOG.debug("About to update {}", matches);

        OpEvent.Outcome global = OpEvent.Outcome.SUCCESS;
        for (InboundMatch match : matches) {
            LOG.debug("About to unassign resource {}", match);

            ProvisioningReport result = new ProvisioningReport();
            result.setOperation(ResourceOperation.NONE);
            result.setAnyType(provision.getAnyType());
            result.setStatus(ProvisioningReport.Status.SUCCESS);
            result.setKey(match.getAny().getKey());
            result.setUidValue(delta.getUid().getUidValue());

            AnyTO before = getAnyTO(match.getAny());

            if (before == null) {
                result.setStatus(ProvisioningReport.Status.FAILURE);
                result.setMessage(String.format("Any '%s(%s)' not found", provision.getAnyType(), match));
            }

            if (!profile.isDryRun()) {
                OpEvent.Outcome resultStatus;
                Object output;
                AnyUR effectiveReq = null;

                if (before == null) {
                    resultStatus = OpEvent.Outcome.FAILURE;
                    output = null;
                } else {
                    result.setName(getName(before));

                    try {
                        if (unlink) {
                            for (InboundActions action : profile.getActions()) {
                                action.beforeUnlink(profile, delta, before);
                            }
                        } else {
                            for (InboundActions action : profile.getActions()) {
                                action.beforeLink(profile, delta, before);
                            }
                        }

                        AnyUR anyUR = getAnyUtils().newAnyUR(before.getKey());
                        anyUR.getResources().add(new StringPatchItem.Builder().
                                operation(unlink ? PatchOperation.DELETE : PatchOperation.ADD_REPLACE).
                                value(profile.getTask().getResource().getKey()).build());

                        effectiveReq = update(anyUR).getResult();
                        output = AnyOperations.patch(before, effectiveReq);

                        for (InboundActions action : profile.getActions()) {
                            action.after(profile, delta, AnyTO.class.cast(output), result);
                        }

                        resultStatus = OpEvent.Outcome.SUCCESS;

                        LOG.debug("{} {} successfully updated", provision.getAnyType(), match);
                    } catch (PropagationException e) {
                        // A propagation failure doesn't imply a pull failure.
                        // The propagation exception status will be reported into the propagation task execution.
                        LOG.error("Could not propagate {} {}",
                                provision.getAnyType(), delta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = OpEvent.Outcome.FAILURE;
                    } catch (Exception e) {
                        throwIgnoreProvisionException(delta, e);

                        result.setStatus(ProvisioningReport.Status.FAILURE);
                        result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error("Could not update {} {}",
                                provision.getAnyType(), delta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = OpEvent.Outcome.FAILURE;
                    }
                }
                end(provision.getAnyType(),
                        unlink
                                ? MatchingRule.toOp(MatchingRule.UNLINK)
                                : MatchingRule.toOp(MatchingRule.LINK),
                        resultStatus, before, output, delta, effectiveReq);
                global = and(global, resultStatus);
            }

            profile.getResults().add(result);
        }

        return global;
    }

    protected OpEvent.Outcome delete(
            final SyncDelta delta,
            final List<InboundMatch> matches,
            final Provision provision) {

        if (!profile.getTask().isPerformDelete()) {
            LOG.debug("PullTask not configured for delete");
            end(provision.getAnyType(),
                    ResourceOperation.DELETE.name().toLowerCase(), OpEvent.Outcome.SUCCESS, null, null, delta);
            return OpEvent.Outcome.SUCCESS;
        }

        LOG.debug("About to delete {}", matches);

        OpEvent.Outcome global = OpEvent.Outcome.SUCCESS;
        for (InboundMatch match : matches) {
            Object output;
            OpEvent.Outcome resultStatus = OpEvent.Outcome.FAILURE;

            ProvisioningReport result = new ProvisioningReport();

            try {
                AnyTO before = getAnyTO(match.getAny());

                result.setKey(match.getAny().getKey());
                result.setName(getName(before));
                result.setOperation(ResourceOperation.DELETE);
                result.setAnyType(provision.getAnyType());
                result.setStatus(ProvisioningReport.Status.SUCCESS);
                result.setUidValue(delta.getUid().getUidValue());

                if (!profile.isDryRun()) {
                    for (InboundActions action : profile.getActions()) {
                        action.beforeDelete(profile, delta, before);
                    }

                    try {
                        getProvisioningManager().delete(
                                match.getAny().getKey(),
                                Set.of(profile.getTask().getResource().getKey()),
                                true,
                                profile.getExecutor(),
                                profile.getContext());
                        output = null;
                        resultStatus = OpEvent.Outcome.SUCCESS;

                        for (InboundActions action : profile.getActions()) {
                            action.after(profile, delta, before, result);
                        }
                    } catch (Exception e) {
                        throwIgnoreProvisionException(delta, e);

                        result.setStatus(ProvisioningReport.Status.FAILURE);
                        result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error("Could not delete {} {}", provision.getAnyType(), match, e);
                        output = e;

                        if (profile.getTask().isRemediation()) {
                            // set to SUCCESS to let the incremental flow go on in case of errors
                            resultStatus = OpEvent.Outcome.SUCCESS;
                            createRemediation(
                                    provision.getAnyType(), match.getAny().getKey(), null, null, result, delta);
                        }
                    }

                    end(provision.getAnyType(),
                            ResourceOperation.DELETE.name().toLowerCase(),
                            resultStatus, before, output, delta);
                    global = and(global, resultStatus);
                }

                profile.getResults().add(result);
            } catch (NotFoundException e) {
                LOG.error("Could not find {} {}", provision.getAnyType(), match, e);
            } catch (DelegatedAdministrationException e) {
                LOG.error("Not allowed to read {} {}", provision.getAnyType(), match, e);
            } catch (Exception e) {
                LOG.error("Could not delete {} {}", provision.getAnyType(), match, e);
            }
        }

        return global;
    }

    protected OpEvent.Outcome ignore(
            final SyncDelta delta,
            final List<InboundMatch> matches,
            final Provision provision,
            final boolean matching,
            final String... message) {

        LOG.debug("Any to ignore {}", delta.getObject().getUid().getUidValue());

        if (matches == null) {
            ProvisioningReport report = new ProvisioningReport();
            report.setKey(null);
            report.setName(delta.getObject().getUid().getUidValue());
            report.setOperation(ResourceOperation.NONE);
            report.setAnyType(provision.getAnyType());
            report.setStatus(ProvisioningReport.Status.SUCCESS);
            report.setUidValue(delta.getUid().getUidValue());
            if (message != null && message.length >= 1) {
                report.setMessage(message[0]);
            }

            profile.getResults().add(report);
        } else {
            matches.forEach(match -> {
                ProvisioningReport report = new ProvisioningReport();
                report.setKey(match.getAny().getKey());
                report.setName(delta.getObject().getUid().getUidValue());
                report.setOperation(ResourceOperation.NONE);
                report.setAnyType(provision.getAnyType());
                report.setStatus(ProvisioningReport.Status.SUCCESS);
                report.setUidValue(delta.getUid().getUidValue());
                if (message != null && message.length >= 1) {
                    report.setMessage(message[0]);
                }

                profile.getResults().add(report);
            });
        }

        end(provision.getAnyType(),
                matching
                        ? MatchingRule.toOp(MatchingRule.IGNORE)
                        : UnmatchingRule.toOp(UnmatchingRule.IGNORE),
                OpEvent.Outcome.SUCCESS, null, null, delta);

        return OpEvent.Outcome.SUCCESS;
    }

    protected OpEvent.Outcome handleAnys(
            final SyncDelta delta,
            final List<InboundMatch> matches,
            final AnyTypeKind anyTypeKind,
            final Provision provision) throws JobExecutionException {

        if (matches.isEmpty()) {
            LOG.debug("Nothing to do");
            return OpEvent.Outcome.SUCCESS;
        }

        OpEvent.Outcome result = OpEvent.Outcome.SUCCESS;
        switch (delta.getDeltaType()) {
            case CREATE:
            case UPDATE:
            case CREATE_OR_UPDATE:
                if (matches.getFirst().getAny() == null) {
                    switch (profile.getTask().getUnmatchingRule()) {
                        case ASSIGN:
                        case PROVISION:
                            result = provision(profile.getTask().getUnmatchingRule(), delta, anyTypeKind, provision);
                            break;

                        case IGNORE:
                            result = ignore(delta, null, provision, false);
                            break;

                        default:
                        // do nothing
                    }
                } else {
                    // update VirAttrCache
                    virSchemaDAO.findByResourceAndAnyType(
                            profile.getTask().getResource().getKey(),
                            matches.getFirst().getAny().getType().getKey()).
                            forEach(vs -> {
                                Attribute attr = delta.getObject().getAttributeByName(vs.getExtAttrName());
                                matches.forEach(match -> {
                                    VirAttrCacheKey cacheKey = VirAttrCacheKey.of(
                                            provision.getAnyType(), match.getAny().getKey(), vs.getKey());
                                    if (attr == null) {
                                        virAttrCache.remove(cacheKey);
                                    } else {
                                        virAttrCache.put(cacheKey, VirAttrCacheValue.of(attr.getValue()));
                                    }
                                });
                            });

                    switch (profile.getTask().getMatchingRule()) {
                        case UPDATE:
                            result = update(delta, matches, provision);
                            break;

                        case DEPROVISION:
                        case UNASSIGN:
                            result = deprovision(profile.getTask().getMatchingRule(), delta, matches, provision);
                            break;

                        case LINK:
                            result = link(delta, matches, provision, false);
                            break;

                        case UNLINK:
                            result = link(delta, matches, provision, true);
                            break;

                        case IGNORE:
                            result = ignore(delta, matches, provision, true);
                            break;

                        default:
                        // do nothing
                    }
                }
                break;

            case DELETE:
                // Skip DELETE in case of InboundCorrelationRule.NO_MATCH
                result = matches.getFirst().getAny() == null
                    ? OpEvent.Outcome.SUCCESS : delete(delta, matches, provision);
                break;

            default:
        }

        return result;
    }

    protected OpEvent.Outcome handleLinkedAccounts(
            final SyncDelta delta,
            final List<InboundMatch> matches,
            final Provision provision) throws JobExecutionException {

        if (matches.isEmpty()) {
            LOG.debug("Nothing to do");
            return OpEvent.Outcome.SUCCESS;
        }

        // nothing to do in the general case
        LOG.warn("Unexpected linked accounts found for {}: {}", provision.getAnyType(), matches);
        return OpEvent.Outcome.SUCCESS;
    }

    /**
     * Look into SyncDelta and take necessary profile.getActions() (create / update / delete) on any object(s).
     *
     * @param delta returned by the underlying profile.getConnector()
     * @param provision provisioning info
     * @param anyTypeKind any type kind
     * @return if handle was successful or not
     * @throws JobExecutionException in case of pull failure.
     */
    protected OpEvent.Outcome doHandle(
            final SyncDelta delta,
            final Provision provision,
            final AnyTypeKind anyTypeKind) throws JobExecutionException {

        LOG.debug("Process {} for {} as {}",
                delta.getDeltaType(), delta.getUid().getUidValue(), delta.getObject().getObjectClass());

        SyncDelta finalDelta = delta;
        for (InboundActions action : profile.getActions()) {
            finalDelta = action.preprocess(profile, finalDelta);
        }

        LOG.debug("Transformed {} for {} as {}",
                finalDelta.getDeltaType(), finalDelta.getUid().getUidValue(), finalDelta.getObject().getObjectClass());

        OpEvent.Outcome result = OpEvent.Outcome.SUCCESS;
        try {
            List<InboundMatch> matches = inboundMatcher.match(
                    finalDelta,
                    profile.getTask().getResource(),
                    provision,
                    anyTypeKind);
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
            OpEvent.Outcome anys = handleAnys(
                    finalDelta,
                    matches.stream().
                            filter(match -> match.getMatchTarget() == MatchType.ANY).
                            toList(),
                    anyTypeKind,
                    provision);

            // linked accounts
            OpEvent.Outcome linkedAccounts = handleLinkedAccounts(
                    finalDelta,
                    matches.stream().
                            filter(match -> match.getMatchTarget() == MatchType.LINKED_ACCOUNT).
                            toList(), provision);

            result = and(anys, linkedAccounts);
        } catch (IllegalStateException | IllegalArgumentException e) {
            LOG.warn(e.getMessage());
        }

        return result;
    }

    protected void end(
            final String anyType,
            final String event,
            final OpEvent.Outcome result,
            final Object before,
            final Object output,
            final SyncDelta delta,
            final Object... furtherInput) {

        notificationManager.createTasks(
                profile.getExecutor(),
                OpEvent.CategoryType.PULL,
                anyType,
                profile.getTask().getResource().getKey(),
                event,
                result,
                before,
                output,
                delta,
                furtherInput);

        auditManager.audit(
                AuthContextUtils.getDomain(),
                profile.getExecutor(),
                OpEvent.CategoryType.PULL,
                anyType,
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
            createRemediation(result.getAnyType(), null, null, anyUR, result, delta);
        }
    }

    protected void createRemediation(
            final String anyType,
            final String anyKey,
            final AnyCR anyCR,
            final AnyUR anyUR,
            final ProvisioningReport result,
            final SyncDelta delta) {

        Remediation remediation = entityFactory.newEntity(Remediation.class);

        remediation.setAnyType(anyTypeDAO.findById(anyType).
                orElseThrow(() -> new NotFoundException("AnyType " + anyType)));
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
        remediation.setPullTask(taskDAO.findById(TaskType.PULL, profile.getTask().getKey()).
                map(PullTask.class::cast).orElse(null));

        remediation = remediationDAO.save(remediation);

        ProvisioningReport remediationResult = new ProvisioningReport();
        remediationResult.setOperation(remediation.getOperation());
        remediationResult.setAnyType(anyType);
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

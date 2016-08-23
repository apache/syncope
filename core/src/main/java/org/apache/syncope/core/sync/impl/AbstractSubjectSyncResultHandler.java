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
package org.apache.syncope.core.sync.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.syncope.common.mod.AbstractSubjectMod;
import org.apache.syncope.common.to.AbstractSubjectTO;
import org.apache.syncope.common.types.AuditElements;
import org.apache.syncope.common.types.AuditElements.Result;
import org.apache.syncope.common.types.MatchingRule;
import org.apache.syncope.common.types.ResourceOperation;
import org.apache.syncope.common.types.UnmatchingRule;
import org.apache.syncope.core.persistence.beans.SyncTask;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.propagation.PropagationException;
import org.apache.syncope.core.rest.controller.UnauthorizedRoleException;
import org.apache.syncope.core.rest.data.AttributableTransformer;
import org.apache.syncope.core.sync.IgnoreProvisionException;
import org.apache.syncope.core.sync.SyncActions;
import org.apache.syncope.core.sync.SyncResult;
import org.apache.syncope.core.sync.SyncUtilities;
import org.apache.syncope.core.util.AttributableUtil;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractSubjectSyncResultHandler extends AbstractSyncopeResultHandler<SyncTask, SyncActions>
        implements SyncResultsHandler {

    @Autowired
    protected SyncUtilities syncUtilities;

    @Autowired
    protected AttributableTransformer attrTransformer;

    @Autowired
    protected UserDAO userDAO;

    protected SyncJob syncJob;

    protected Result latestResult = null;

    protected abstract String getName(AbstractSubjectTO subjectTO);

    protected abstract AbstractSubjectMod getSubjectMod(AbstractSubjectTO subjectTO, SyncDelta delta);

    protected abstract AbstractSubjectTO doCreate(AbstractSubjectTO subjectTO, SyncDelta _delta, SyncResult result);

    protected abstract AbstractSubjectTO doLink(AbstractSubjectTO before, SyncResult result, boolean unlink)
            throws Exception;

    protected abstract AbstractSubjectTO doUpdate(AbstractSubjectTO before, AbstractSubjectMod subjectMod,
            SyncDelta delta, SyncResult result)
            throws Exception;

    protected abstract void doDeprovision(Long id, boolean unlink) throws Exception;

    protected abstract void doDelete(Long id);

    public void setSyncJob(final SyncJob syncJob) {
        this.syncJob = syncJob;
    }

    protected void setLatestSyncToken(final SyncDelta delta) {
        if (ObjectClass.ACCOUNT.equals(delta.getObjectClass())) {
            syncJob.setLatestUSyncToken(delta.getToken());
        } else if (ObjectClass.GROUP.equals(delta.getObjectClass())) {
            syncJob.setLatestRSyncToken(delta.getToken());
        }
    }

    @Override
    public boolean handle(final SyncDelta delta) {
        try {
            doHandle(delta);

            LOG.debug("Successfully handled {}", delta);

            if (profile.getSyncTask().isFullReconciliation()) {
                return true;
            }

            boolean shouldContinue;
            synchronized (this) {
                shouldContinue = latestResult == Result.SUCCESS;
                this.latestResult = null;
            }
            if (shouldContinue) {
                setLatestSyncToken(delta);
            }
            return shouldContinue;
        } catch (IgnoreProvisionException e) {
            SyncResult ignoreResult = new SyncResult();
            ignoreResult.setOperation(ResourceOperation.NONE);
            ignoreResult.setSubjectType(getAttributableUtil().getType());
            ignoreResult.setStatus(SyncResult.Status.IGNORE);
            ignoreResult.setId(0L);
            ignoreResult.setName(delta.getObject().getName().getNameValue());
            profile.getResults().add(ignoreResult);

            LOG.warn("Ignoring during synchronization", e);

            setLatestSyncToken(delta);

            return true;
        } catch (JobExecutionException e) {
            LOG.error("Synchronization failed", e);

            return false;
        }
    }

    protected List<SyncResult> assign(final SyncDelta delta, final AttributableUtil attrUtil)
            throws JobExecutionException {

        if (!profile.getSyncTask().isPerformCreate()) {
            LOG.debug("SyncTask not configured for create");
            finalize(UnmatchingRule.toEventName(UnmatchingRule.ASSIGN), Result.SUCCESS, null, null, delta);
            return Collections.<SyncResult>emptyList();
        }

        final AbstractSubjectTO subjectTO =
                connObjectUtil.getSubjectTO(delta.getObject(), profile.getSyncTask(), attrUtil);

        subjectTO.getResources().add(profile.getSyncTask().getResource().getName());

        final SyncResult result = new SyncResult();
        result.setOperation(ResourceOperation.CREATE);
        result.setSubjectType(attrUtil.getType());
        result.setStatus(SyncResult.Status.SUCCESS);

        // Attributable transformation (if configured)
        AbstractSubjectTO transformed = attrTransformer.transform(subjectTO);
        LOG.debug("Transformed: {}", transformed);

        result.setName(getName(transformed));

        if (profile.isDryRun()) {
            result.setId(0L);
            finalize(UnmatchingRule.toEventName(UnmatchingRule.ASSIGN), Result.SUCCESS, null, null, delta);
        } else {
            SyncDelta _delta = delta;
            for (SyncActions action : profile.getActions()) {
                _delta = action.beforeAssign(profile, _delta, transformed);
            }

            create(transformed, _delta, attrUtil, UnmatchingRule.toEventName(UnmatchingRule.ASSIGN), result);
        }

        return Collections.singletonList(result);
    }

    protected List<SyncResult> provision(final SyncDelta delta, final AttributableUtil attrUtil)
            throws JobExecutionException {

        if (!profile.getSyncTask().isPerformCreate()) {
            LOG.debug("SyncTask not configured for create");
            finalize(UnmatchingRule.toEventName(UnmatchingRule.PROVISION), Result.SUCCESS, null, null, delta);
            return Collections.<SyncResult>emptyList();
        }

        final AbstractSubjectTO subjectTO =
                connObjectUtil.getSubjectTO(delta.getObject(), profile.getSyncTask(), attrUtil);

        // Attributable transformation (if configured)
        AbstractSubjectTO transformed = attrTransformer.transform(subjectTO);
        LOG.debug("Transformed: {}", transformed);

        final SyncResult result = new SyncResult();
        result.setOperation(ResourceOperation.CREATE);
        result.setSubjectType(attrUtil.getType());
        result.setStatus(SyncResult.Status.SUCCESS);

        result.setName(getName(transformed));

        if (profile.isDryRun()) {
            result.setId(0L);
            finalize(UnmatchingRule.toEventName(UnmatchingRule.PROVISION), Result.SUCCESS, null, null, delta);
        } else {
            SyncDelta _delta = delta;
            for (SyncActions action : profile.getActions()) {
                _delta = action.beforeProvision(profile, _delta, transformed);
            }

            create(transformed, _delta, attrUtil, UnmatchingRule.toEventName(UnmatchingRule.PROVISION), result);
        }

        return Collections.<SyncResult>singletonList(result);
    }

    protected void throwIgnoreProvisionException(final SyncDelta delta, final Exception exception)
            throws JobExecutionException {

        if (exception instanceof IgnoreProvisionException) {
            throw IgnoreProvisionException.class.cast(exception);
        }

        IgnoreProvisionException ipe = null;
        for (SyncActions action : profile.getActions()) {
            if (ipe == null) {
                ipe = action.onError(profile, delta, exception);
            }
        }
        if (ipe != null) {
            throw ipe;
        }
    }

    protected void create(
            final AbstractSubjectTO subjectTO,
            final SyncDelta delta,
            final AttributableUtil attrUtil,
            final String operation,
            final SyncResult result)
            throws JobExecutionException {

        Object output;
        Result resultStatus;

        try {
            AbstractSubjectTO actual = doCreate(subjectTO, delta, result);
            result.setName(getName(actual));
            output = actual;
            resultStatus = Result.SUCCESS;

            for (SyncActions action : profile.getActions()) {
                action.after(profile, delta, actual, result);
            }
        } catch (PropagationException e) {
            // A propagation failure doesn't imply a synchronization failure.
            // The propagation exception status will be reported into the propagation task execution.
            LOG.error("Could not propagate {} {}", attrUtil.getType(), delta.getUid().getUidValue(), e);
            output = e;
            resultStatus = Result.FAILURE;
        } catch (Exception e) {
            throwIgnoreProvisionException(delta, e);

            result.setStatus(SyncResult.Status.FAILURE);
            result.setMessage(ExceptionUtils.getRootCauseMessage(e));
            LOG.error("Could not create {} {} ", attrUtil.getType(), delta.getUid().getUidValue(), e);
            output = e;
            resultStatus = Result.FAILURE;
        }

        finalize(operation, resultStatus, null, output, delta);
    }

    protected List<SyncResult> update(SyncDelta delta, final List<Long> subjects, final AttributableUtil attrUtil)
            throws JobExecutionException {

        if (!profile.getSyncTask().isPerformUpdate()) {
            LOG.debug("SyncTask not configured for update");
            finalize(MatchingRule.toEventName(MatchingRule.UPDATE), Result.SUCCESS, null, null, delta);
            return Collections.<SyncResult>emptyList();
        }

        LOG.debug("About to update {}", subjects);

        List<SyncResult> updResults = new ArrayList<SyncResult>();

        for (Long id : subjects) {
            LOG.debug("About to update {}", id);

            Object output;
            AbstractSubjectTO before;
            Result resultStatus;

            final SyncResult result = new SyncResult();
            result.setOperation(ResourceOperation.UPDATE);
            result.setSubjectType(attrUtil.getType());
            result.setStatus(SyncResult.Status.SUCCESS);
            result.setId(id);

            before = getSubjectTO(id);

            if (before == null) {
                result.setStatus(SyncResult.Status.FAILURE);
                result.setMessage(String.format("Subject '%s(%d)' not found", attrUtil.getType().name(), id));
            } else {
                result.setName(getName(before));
            }

            if (!profile.isDryRun()) {
                if (before == null) {
                    resultStatus = Result.FAILURE;
                    output = null;
                } else {
                    try {
                        final AbstractSubjectMod attributableMod = getSubjectMod(before, delta);

                        // Attribute value transformation (if configured)
                        final AbstractSubjectMod actual = attrTransformer.transform(attributableMod);
                        LOG.debug("Transformed: {}", actual);

                        for (SyncActions action : profile.getActions()) {
                            delta = action.beforeUpdate(profile, delta, before, attributableMod);
                        }

                        final AbstractSubjectTO updated = doUpdate(before, attributableMod, delta, result);

                        for (SyncActions action : profile.getActions()) {
                            action.after(profile, delta, updated, result);
                        }

                        output = updated;
                        resultStatus = Result.SUCCESS;
                        result.setName(getName(updated));
                        LOG.debug("{} {} successfully updated", attrUtil.getType(), id);
                    } catch (PropagationException e) {
                        // A propagation failure doesn't imply a synchronization failure.
                        // The propagation exception status will be reported into the propagation task execution.
                        LOG.error("Could not propagate {} {}", attrUtil.getType(), delta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = Result.FAILURE;
                    } catch (Exception e) {
                        throwIgnoreProvisionException(delta, e);

                        result.setStatus(SyncResult.Status.FAILURE);
                        result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error("Could not update {} {}", attrUtil.getType(), delta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = Result.FAILURE;
                    }
                }
                finalize(MatchingRule.toEventName(MatchingRule.UPDATE), resultStatus, before, output, delta);
            }
            updResults.add(result);
        }
        return updResults;
    }

    protected List<SyncResult> deprovision(
            SyncDelta delta,
            final List<Long> subjects,
            final AttributableUtil attrUtil,
            final boolean unlink)
            throws JobExecutionException {

        if (!profile.getSyncTask().isPerformUpdate()) {
            LOG.debug("SyncTask not configured for update");
            finalize(unlink
                    ? MatchingRule.toEventName(MatchingRule.UNASSIGN)
                    : MatchingRule.toEventName(MatchingRule.DEPROVISION), Result.SUCCESS, null, null, delta);
            return Collections.<SyncResult>emptyList();
        }

        LOG.debug("About to update {}", subjects);

        final List<SyncResult> updResults = new ArrayList<SyncResult>();

        for (Long id : subjects) {
            LOG.debug("About to unassign resource {}", id);

            Object output;
            Result resultStatus;

            final SyncResult result = new SyncResult();
            result.setOperation(ResourceOperation.DELETE);
            result.setSubjectType(attrUtil.getType());
            result.setStatus(SyncResult.Status.SUCCESS);
            result.setId(id);

            final AbstractSubjectTO before = getSubjectTO(id);

            if (before == null) {
                result.setStatus(SyncResult.Status.FAILURE);
                result.setMessage(String.format("Subject '%s(%d)' not found", attrUtil.getType().name(), id));
            }

            if (!profile.isDryRun()) {
                if (before == null) {
                    resultStatus = Result.FAILURE;
                    output = null;
                } else {
                    result.setName(getName(before));

                    try {
                        if (unlink) {
                            for (SyncActions action : profile.getActions()) {
                                action.beforeUnassign(profile, delta, before);
                            }
                        } else {
                            for (SyncActions action : profile.getActions()) {
                                action.beforeDeprovision(profile, delta, before);
                            }
                        }

                        doDeprovision(id, unlink);
                        output = getSubjectTO(id);

                        for (SyncActions action : profile.getActions()) {
                            action.after(profile, delta, AbstractSubjectTO.class.cast(output), result);
                        }

                        resultStatus = Result.SUCCESS;
                        LOG.debug("{} {} successfully updated", attrUtil.getType(), id);
                    } catch (PropagationException e) {
                        // A propagation failure doesn't imply a synchronization failure.
                        // The propagation exception status will be reported into the propagation task execution.
                        LOG.error("Could not propagate {} {}", attrUtil.getType(), delta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = Result.FAILURE;
                    } catch (Exception e) {
                        throwIgnoreProvisionException(delta, e);

                        result.setStatus(SyncResult.Status.FAILURE);
                        result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error("Could not update {} {}", attrUtil.getType(), delta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = Result.FAILURE;
                    }
                }
                finalize(unlink
                        ? MatchingRule.toEventName(MatchingRule.UNASSIGN)
                        : MatchingRule.toEventName(MatchingRule.DEPROVISION), resultStatus, before, output, delta);
            }
            updResults.add(result);
        }

        return updResults;
    }

    protected List<SyncResult> link(
            final SyncDelta delta,
            final List<Long> subjects,
            final AttributableUtil attrUtil,
            final boolean unlink)
            throws JobExecutionException {

        if (!profile.getSyncTask().isPerformUpdate()) {
            LOG.debug("SyncTask not configured for update");
            finalize(unlink
                    ? MatchingRule.toEventName(MatchingRule.UNLINK)
                    : MatchingRule.toEventName(MatchingRule.LINK), Result.SUCCESS, null, null, delta);
            return Collections.<SyncResult>emptyList();
        }

        LOG.debug("About to update {}", subjects);

        final List<SyncResult> updResults = new ArrayList<SyncResult>();

        for (Long id : subjects) {
            LOG.debug("About to unassign resource {}", id);

            Object output;
            Result resultStatus;

            final SyncResult result = new SyncResult();
            result.setOperation(ResourceOperation.NONE);
            result.setSubjectType(attrUtil.getType());
            result.setStatus(SyncResult.Status.SUCCESS);
            result.setId(id);

            final AbstractSubjectTO before = getSubjectTO(id);

            if (before == null) {
                result.setStatus(SyncResult.Status.FAILURE);
                result.setMessage(String.format("Subject '%s(%d)' not found", attrUtil.getType().name(), id));
            }

            if (!profile.isDryRun()) {
                if (before == null) {
                    resultStatus = Result.FAILURE;
                    output = null;
                } else {
                    result.setName(getName(before));

                    try {
                        if (unlink) {
                            for (SyncActions action : profile.getActions()) {
                                action.beforeUnlink(profile, delta, before);
                            }
                        } else {
                            for (SyncActions action : profile.getActions()) {
                                action.beforeLink(profile, delta, before);
                            }
                        }

                        output = doLink(before, result, unlink);

                        for (SyncActions action : profile.getActions()) {
                            action.after(profile, delta, AbstractSubjectTO.class.cast(output), result);
                        }

                        resultStatus = Result.SUCCESS;
                        LOG.debug("{} {} successfully updated", attrUtil.getType(), id);
                    } catch (PropagationException e) {
                        // A propagation failure doesn't imply a synchronization failure.
                        // The propagation exception status will be reported into the propagation task execution.
                        LOG.error("Could not propagate {} {}", attrUtil.getType(), delta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = Result.FAILURE;
                    } catch (Exception e) {
                        throwIgnoreProvisionException(delta, e);

                        result.setStatus(SyncResult.Status.FAILURE);
                        result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error("Could not update {} {}", attrUtil.getType(), delta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = Result.FAILURE;
                    }
                }
                finalize(unlink
                        ? MatchingRule.toEventName(MatchingRule.UNLINK)
                        : MatchingRule.toEventName(MatchingRule.LINK), resultStatus, before, output, delta);
            }
            updResults.add(result);
        }

        return updResults;
    }

    protected List<SyncResult> delete(SyncDelta delta, final List<Long> subjects, final AttributableUtil attrUtil)
            throws JobExecutionException {

        if (!profile.getSyncTask().isPerformDelete()) {
            LOG.debug("SyncTask not configured for delete");
            finalize(ResourceOperation.DELETE.name().toLowerCase(), Result.SUCCESS, null, null, delta);
            return Collections.<SyncResult>emptyList();
        }

        LOG.debug("About to delete {}", subjects);

        List<SyncResult> delResults = new ArrayList<SyncResult>();

        for (Long id : subjects) {
            Object output;
            Result resultStatus = Result.FAILURE;

            AbstractSubjectTO before;
            final SyncResult result = new SyncResult();

            try {
                before = getSubjectTO(id);

                result.setId(id);
                result.setName(getName(before));
                result.setOperation(ResourceOperation.DELETE);
                result.setSubjectType(attrUtil.getType());
                result.setStatus(SyncResult.Status.SUCCESS);

                if (!profile.isDryRun()) {
                    for (SyncActions action : profile.getActions()) {
                        delta = action.beforeDelete(profile, delta, before);
                    }

                    try {
                        doDelete(id);
                        output = null;
                        resultStatus = Result.SUCCESS;
                    } catch (Exception e) {
                        throwIgnoreProvisionException(delta, e);

                        result.setStatus(SyncResult.Status.FAILURE);
                        result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error("Could not delete {} {}", attrUtil.getType(), id, e);
                        output = e;
                    }

                    for (SyncActions action : profile.getActions()) {
                        action.after(profile, delta, before, result);
                    }

                    finalize(ResourceOperation.DELETE.name().toLowerCase(), resultStatus, before, output, delta);
                }

                delResults.add(result);
            } catch (NotFoundException e) {
                LOG.error("Could not find {} {}", attrUtil.getType(), id, e);
            } catch (UnauthorizedRoleException e) {
                LOG.error("Not allowed to read {} {}", attrUtil.getType(), id, e);
            } catch (Exception e) {
                LOG.error("Could not delete {} {}", attrUtil.getType(), id, e);
            }
        }

        return delResults;
    }

    protected List<SyncResult> ignore(SyncDelta delta, final AttributableUtil attrUtil, final boolean matching)
            throws JobExecutionException {

        LOG.debug("Subject to ignore {}", delta.getObject().getUid().getUidValue());

        final List<SyncResult> ignoreResults = new ArrayList<SyncResult>();
        final SyncResult result = new SyncResult();

        result.setId(null);
        result.setName(delta.getObject().getUid().getUidValue());
        result.setOperation(ResourceOperation.NONE);
        result.setSubjectType(attrUtil.getType());
        result.setStatus(SyncResult.Status.SUCCESS);
        ignoreResults.add(result);

        finalize(matching
                ? MatchingRule.toEventName(MatchingRule.IGNORE)
                : UnmatchingRule.toEventName(UnmatchingRule.IGNORE), Result.SUCCESS, null, null, delta);

        return ignoreResults;
    }

    /**
     * Look into SyncDelta and take necessary profile.getActions() (create / update / delete) on user(s)/role(s).
     *
     * @param delta returned by the underlying profile.getConnector()
     * @throws JobExecutionException in case of synchronization failure.
     */
    protected void doHandle(final SyncDelta delta) throws JobExecutionException {
        final AttributableUtil attrUtil = getAttributableUtil();

        LOG.debug("Process {} for {} as {}",
                delta.getDeltaType(), delta.getUid().getUidValue(), delta.getObject().getObjectClass());

        final String uid = delta.getPreviousUid() == null
                ? delta.getUid().getUidValue()
                : delta.getPreviousUid().getUidValue();

        try {
            List<Long> subjectIds = syncUtilities.findExisting(
                    uid, delta.getObject(), profile.getSyncTask().getResource(), attrUtil);

            if (subjectIds.size() > 1) {
                switch (profile.getResAct()) {
                    case IGNORE:
                        throw new IllegalStateException("More than one match " + subjectIds);

                    case FIRSTMATCH:
                        subjectIds = subjectIds.subList(0, 1);
                        break;

                    case LASTMATCH:
                        subjectIds = subjectIds.subList(subjectIds.size() - 1, subjectIds.size());
                        break;

                    case ALL:
                    default:
                    // keep subjectIds as is
                }
            }

            if (SyncDeltaType.CREATE_OR_UPDATE == delta.getDeltaType()) {
                if (subjectIds.isEmpty()) {
                    switch (profile.getSyncTask().getUnmatchingRule()) {
                        case ASSIGN:
                            profile.getResults().addAll(assign(delta, attrUtil));
                            break;
                        case PROVISION:
                            profile.getResults().addAll(provision(delta, attrUtil));
                            break;
                        case IGNORE:
                            profile.getResults().addAll(ignore(delta, attrUtil, false));
                            break;
                        default:
                        // do nothing
                    }
                } else {
                    switch (profile.getSyncTask().getMatchingRule()) {
                        case UPDATE:
                            profile.getResults().addAll(update(delta, subjectIds, attrUtil));
                            break;
                        case DEPROVISION:
                            profile.getResults().addAll(deprovision(delta, subjectIds, attrUtil, false));
                            break;
                        case UNASSIGN:
                            profile.getResults().addAll(deprovision(delta, subjectIds, attrUtil, true));
                            break;
                        case LINK:
                            profile.getResults().addAll(link(delta, subjectIds, attrUtil, false));
                            break;
                        case UNLINK:
                            profile.getResults().addAll(link(delta, subjectIds, attrUtil, true));
                            break;
                        case IGNORE:
                            profile.getResults().addAll(ignore(delta, attrUtil, true));
                            break;
                        default:
                        // do nothing
                    }
                }
            } else if (SyncDeltaType.DELETE == delta.getDeltaType()) {
                if (subjectIds.isEmpty()) {
                    finalize(ResourceOperation.DELETE.name().toLowerCase(), Result.SUCCESS, null, null, delta);
                    LOG.debug("No match found for deletion");
                } else {
                    profile.getResults().addAll(delete(delta, subjectIds, attrUtil));
                }
            }
        } catch (IllegalStateException e) {
            LOG.warn(e.getMessage());
        } catch (IllegalArgumentException e) {
            LOG.warn(e.getMessage());
        }
    }

    protected void finalize(
            final String event,
            final Result result,
            final Object before,
            final Object output,
            final SyncDelta delta) {

        synchronized (this) {
            this.latestResult = result;
        }

        notificationManager.createTasks(
                AuditElements.EventCategoryType.SYNCHRONIZATION,
                getAttributableUtil().getType().name().toLowerCase(),
                profile.getSyncTask().getResource().getName(),
                event,
                result,
                before,
                output,
                delta);

        auditManager.audit(
                AuditElements.EventCategoryType.SYNCHRONIZATION,
                getAttributableUtil().getType().name().toLowerCase(),
                profile.getSyncTask().getResource().getName(),
                event,
                result,
                before,
                output,
                delta);
    }
}

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
package org.apache.syncope.core.provisioning.java.sync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.syncope.common.lib.mod.AbstractSubjectMod;
import org.apache.syncope.common.lib.to.AbstractSubjectTO;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.Result;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.AttributableUtil;
import org.apache.syncope.core.persistence.api.entity.task.SyncTask;
import org.apache.syncope.core.provisioning.api.AttributableTransformer;
import org.apache.syncope.core.provisioning.api.propagation.PropagationException;
import org.apache.syncope.core.provisioning.api.sync.SyncActions;
import org.apache.syncope.core.provisioning.api.sync.ProvisioningResult;
import org.apache.syncope.core.misc.security.UnauthorizedRoleException;
import org.apache.syncope.core.provisioning.api.sync.SyncopeSyncResultHandler;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractSyncResultHandler extends AbstractSyncopeResultHandler<SyncTask, SyncActions>
        implements SyncopeSyncResultHandler {

    @Autowired
    protected SyncUtilities syncUtilities;

    @Autowired
    protected AttributableTransformer attrTransformer;

    protected abstract AttributableUtil getAttributableUtil();

    protected abstract String getName(AbstractSubjectTO subjectTO);

    protected abstract AbstractSubjectTO getSubjectTO(long key);

    protected abstract AbstractSubjectMod getSubjectMod(AbstractSubjectTO subjectTO, SyncDelta delta);

    protected abstract AbstractSubjectTO create(AbstractSubjectTO subjectTO, SyncDelta _delta, ProvisioningResult result);

    protected abstract AbstractSubjectTO link(AbstractSubjectTO before, ProvisioningResult result, boolean unlink);

    protected abstract AbstractSubjectTO update(AbstractSubjectTO before, AbstractSubjectMod subjectMod,
            SyncDelta delta, ProvisioningResult result);

    protected abstract void deprovision(Long key, boolean unlink);

    protected abstract void delete(Long key);

    @Override
    public boolean handle(final SyncDelta delta) {
        try {
            doHandle(delta);
            return true;
        } catch (JobExecutionException e) {
            LOG.error("Synchronization failed", e);
            return false;
        }
    }

    protected List<ProvisioningResult> assign(final SyncDelta delta, final AttributableUtil attrUtil)
            throws JobExecutionException {
        if (!profile.getTask().isPerformCreate()) {
            LOG.debug("SyncTask not configured for create");
            return Collections.<ProvisioningResult>emptyList();
        }

        final AbstractSubjectTO subjectTO =
                connObjectUtil.getSubjectTO(delta.getObject(), profile.getTask(), attrUtil);

        subjectTO.getResources().add(profile.getTask().getResource().getKey());

        final ProvisioningResult result = new ProvisioningResult();
        result.setOperation(ResourceOperation.CREATE);
        result.setSubjectType(attrUtil.getType());
        result.setStatus(ProvisioningResult.Status.SUCCESS);

        // Attributable transformation (if configured)
        AbstractSubjectTO transformed = attrTransformer.transform(subjectTO);
        LOG.debug("Transformed: {}", transformed);

        result.setName(getName(transformed));

        if (profile.isDryRun()) {
            result.setId(0L);
        } else {
            SyncDelta _delta = delta;
            for (SyncActions action : profile.getActions()) {
                _delta = action.beforeAssign(this.getProfile(), _delta, transformed);
            }

            create(transformed, _delta, attrUtil, "assign", result);
        }

        return Collections.singletonList(result);
    }

    protected List<ProvisioningResult> create(final SyncDelta delta, final AttributableUtil attrUtil)
            throws JobExecutionException {

        if (!profile.getTask().isPerformCreate()) {
            LOG.debug("SyncTask not configured for create");
            return Collections.<ProvisioningResult>emptyList();
        }

        final AbstractSubjectTO subjectTO =
                connObjectUtil.getSubjectTO(delta.getObject(), profile.getTask(), attrUtil);

        // Attributable transformation (if configured)
        AbstractSubjectTO transformed = attrTransformer.transform(subjectTO);
        LOG.debug("Transformed: {}", transformed);

        final ProvisioningResult result = new ProvisioningResult();
        result.setOperation(ResourceOperation.CREATE);
        result.setSubjectType(attrUtil.getType());
        result.setStatus(ProvisioningResult.Status.SUCCESS);

        result.setName(getName(transformed));

        if (profile.isDryRun()) {
            result.setId(0L);
        } else {
            SyncDelta _delta = delta;
            for (SyncActions action : profile.getActions()) {
                _delta = action.beforeProvision(this.getProfile(), _delta, transformed);
            }

            create(transformed, _delta, attrUtil, "provision", result);
        }

        return Collections.<ProvisioningResult>singletonList(result);
    }

    private void create(
            final AbstractSubjectTO subjectTO,
            final SyncDelta delta,
            final AttributableUtil attrUtil,
            final String operation,
            final ProvisioningResult result)
            throws JobExecutionException {

        Object output;
        Result resultStatus;

        try {
            AbstractSubjectTO actual = create(subjectTO, delta, result);
            result.setName(getName(actual));
            output = actual;
            resultStatus = Result.SUCCESS;

            for (SyncActions action : profile.getActions()) {
                action.after(this.getProfile(), delta, actual, result);
            }
        } catch (PropagationException e) {
            // A propagation failure doesn't imply a synchronization failure.
            // The propagation exception status will be reported into the propagation task execution.
            LOG.error("Could not propagate {} {}", attrUtil.getType(), delta.getUid().getUidValue(), e);
            output = e;
            resultStatus = Result.FAILURE;
        } catch (Exception e) {
            result.setStatus(ProvisioningResult.Status.FAILURE);
            result.setMessage(ExceptionUtils.getRootCauseMessage(e));
            LOG.error("Could not create {} {} ", attrUtil.getType(), delta.getUid().getUidValue(), e);
            output = e;
            resultStatus = Result.FAILURE;
        }

        audit(operation, resultStatus, null, output, delta);
    }

    protected List<ProvisioningResult> update(SyncDelta delta, final List<Long> subjects,
            final AttributableUtil attrUtil)
            throws JobExecutionException {

        if (!profile.getTask().isPerformUpdate()) {
            LOG.debug("SyncTask not configured for update");
            return Collections.<ProvisioningResult>emptyList();
        }

        LOG.debug("About to update {}", subjects);

        List<ProvisioningResult> results = new ArrayList<>();

        for (Long key : subjects) {
            LOG.debug("About to update {}", key);

            final ProvisioningResult result = new ProvisioningResult();
            result.setOperation(ResourceOperation.UPDATE);
            result.setSubjectType(attrUtil.getType());
            result.setStatus(ProvisioningResult.Status.SUCCESS);
            result.setId(key);

            AbstractSubjectTO before = getSubjectTO(key);
            if (before == null) {
                result.setStatus(ProvisioningResult.Status.FAILURE);
                result.setMessage(String.format("Subject '%s(%d)' not found", attrUtil.getType().name(), key));
            } else {
                result.setName(getName(before));
            }

            Result resultStatus;
            Object output;
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
                            delta = action.beforeUpdate(this.getProfile(), delta, before, attributableMod);
                        }

                        final AbstractSubjectTO updated = update(before, attributableMod, delta, result);

                        for (SyncActions action : profile.getActions()) {
                            action.after(this.getProfile(), delta, updated, result);
                        }

                        output = updated;
                        resultStatus = Result.SUCCESS;
                        result.setName(getName(updated));
                        LOG.debug("{} {} successfully updated", attrUtil.getType(), key);
                    } catch (PropagationException e) {
                        // A propagation failure doesn't imply a synchronization failure.
                        // The propagation exception status will be reported into the propagation task execution.
                        LOG.error("Could not propagate {} {}", attrUtil.getType(), delta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = Result.FAILURE;
                    } catch (Exception e) {
                        result.setStatus(ProvisioningResult.Status.FAILURE);
                        result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error("Could not update {} {}", attrUtil.getType(), delta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = Result.FAILURE;
                    }
                }
                audit("update", resultStatus, before, output, delta);
            }
            results.add(result);
        }
        return results;
    }

    protected List<ProvisioningResult> deprovision(
            SyncDelta delta,
            final List<Long> subjects,
            final AttributableUtil attrUtil,
            final boolean unlink)
            throws JobExecutionException {

        if (!profile.getTask().isPerformUpdate()) {
            LOG.debug("SyncTask not configured for update");
            return Collections.<ProvisioningResult>emptyList();
        }

        LOG.debug("About to update {}", subjects);

        final List<ProvisioningResult> updResults = new ArrayList<>();

        for (Long id : subjects) {
            LOG.debug("About to unassign resource {}", id);

            Object output;
            Result resultStatus;

            final ProvisioningResult result = new ProvisioningResult();
            result.setOperation(ResourceOperation.DELETE);
            result.setSubjectType(attrUtil.getType());
            result.setStatus(ProvisioningResult.Status.SUCCESS);
            result.setId(id);

            final AbstractSubjectTO before = getSubjectTO(id);

            if (before == null) {
                result.setStatus(ProvisioningResult.Status.FAILURE);
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
                                action.beforeUnassign(this.getProfile(), delta, before);
                            }
                        } else {
                            for (SyncActions action : profile.getActions()) {
                                action.beforeDeprovision(this.getProfile(), delta, before);
                            }
                        }

                        deprovision(id, unlink);
                        output = getSubjectTO(id);

                        for (SyncActions action : profile.getActions()) {
                            action.after(this.getProfile(), delta, AbstractSubjectTO.class.cast(output), result);
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
                        result.setStatus(ProvisioningResult.Status.FAILURE);
                        result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error("Could not update {} {}", attrUtil.getType(), delta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = Result.FAILURE;
                    }
                }
                audit(unlink ? "unassign" : "deprovision", resultStatus, before, output, delta);
            }
            updResults.add(result);
        }

        return updResults;
    }

    protected List<ProvisioningResult> link(
            SyncDelta delta,
            final List<Long> subjects,
            final AttributableUtil attrUtil,
            final boolean unlink)
            throws JobExecutionException {

        if (!profile.getTask().isPerformUpdate()) {
            LOG.debug("SyncTask not configured for update");
            return Collections.<ProvisioningResult>emptyList();
        }

        LOG.debug("About to update {}", subjects);

        final List<ProvisioningResult> updResults = new ArrayList<>();

        for (Long id : subjects) {
            LOG.debug("About to unassign resource {}", id);

            Object output;
            Result resultStatus;

            final ProvisioningResult result = new ProvisioningResult();
            result.setOperation(ResourceOperation.NONE);
            result.setSubjectType(attrUtil.getType());
            result.setStatus(ProvisioningResult.Status.SUCCESS);
            result.setId(id);

            final AbstractSubjectTO before = getSubjectTO(id);

            if (before == null) {
                result.setStatus(ProvisioningResult.Status.FAILURE);
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
                                action.beforeUnlink(this.getProfile(), delta, before);
                            }
                        } else {
                            for (SyncActions action : profile.getActions()) {
                                action.beforeLink(this.getProfile(), delta, before);
                            }
                        }

                        output = link(before, result, unlink);

                        for (SyncActions action : profile.getActions()) {
                            action.after(this.getProfile(), delta, AbstractSubjectTO.class.cast(output), result);
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
                        result.setStatus(ProvisioningResult.Status.FAILURE);
                        result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error("Could not update {} {}", attrUtil.getType(), delta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = Result.FAILURE;
                    }
                }
                audit(unlink ? "unlink" : "link", resultStatus, before, output, delta);
            }
            updResults.add(result);
        }

        return updResults;
    }

    protected List<ProvisioningResult> delete(
            SyncDelta delta, final List<Long> subjects, final AttributableUtil attrUtil)
            throws JobExecutionException {

        if (!profile.getTask().isPerformDelete()) {
            LOG.debug("SyncTask not configured for delete");
            return Collections.<ProvisioningResult>emptyList();
        }

        LOG.debug("About to delete {}", subjects);

        List<ProvisioningResult> delResults = new ArrayList<>();

        for (Long id : subjects) {
            Object output;
            Result resultStatus = Result.FAILURE;

            AbstractSubjectTO before = null;
            final ProvisioningResult result = new ProvisioningResult();

            try {
                before = getSubjectTO(id);

                result.setId(id);
                result.setName(getName(before));
                result.setOperation(ResourceOperation.DELETE);
                result.setSubjectType(attrUtil.getType());
                result.setStatus(ProvisioningResult.Status.SUCCESS);

                if (!profile.isDryRun()) {
                    for (SyncActions action : profile.getActions()) {
                        delta = action.beforeDelete(this.getProfile(), delta, before);
                    }

                    try {
                        delete(id);
                        output = null;
                        resultStatus = Result.SUCCESS;
                    } catch (Exception e) {
                        result.setStatus(ProvisioningResult.Status.FAILURE);
                        result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error("Could not delete {} {}", attrUtil.getType(), id, e);
                        output = e;
                    }

                    for (SyncActions action : profile.getActions()) {
                        action.after(this.getProfile(), delta, before, result);
                    }

                    audit("delete", resultStatus, before, output, delta);
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

    /**
     * Look into SyncDelta and take necessary profile.getActions() (create / update / delete) on user(s)/role(s).
     *
     * @param delta returned by the underlying profile.getConnector()
     * @throws JobExecutionException in case of synchronization failure.
     */
    protected final void doHandle(final SyncDelta delta)
            throws JobExecutionException {

        final AttributableUtil attrUtil = getAttributableUtil();

        LOG.debug("Process {} for {} as {}",
                delta.getDeltaType(), delta.getUid().getUidValue(), delta.getObject().getObjectClass());

        final String uid = delta.getPreviousUid() == null
                ? delta.getUid().getUidValue()
                : delta.getPreviousUid().getUidValue();

        try {
            List<Long> subjectKeys = syncUtilities.findExisting(
                    uid, delta.getObject(), profile.getTask().getResource(), attrUtil);

            if (subjectKeys.size() > 1) {
                switch (profile.getResAct()) {
                    case IGNORE:
                        throw new IllegalStateException("More than one match " + subjectKeys);

                    case FIRSTMATCH:
                        subjectKeys = subjectKeys.subList(0, 1);
                        break;

                    case LASTMATCH:
                        subjectKeys = subjectKeys.subList(subjectKeys.size() - 1, subjectKeys.size());
                        break;

                    default:
                    // keep subjectIds as is
                }
            }

            if (SyncDeltaType.CREATE_OR_UPDATE == delta.getDeltaType()) {
                if (subjectKeys.isEmpty()) {
                    switch (profile.getTask().getUnmatchingRule()) {
                        case ASSIGN:
                            profile.getResults().addAll(assign(delta, attrUtil));
                            break;
                        case PROVISION:
                            profile.getResults().addAll(create(delta, attrUtil));
                            break;
                        default:
                        // do nothing
                    }
                } else {
                    switch (profile.getTask().getMatchingRule()) {
                        case UPDATE:
                            profile.getResults().addAll(update(delta, subjectKeys, attrUtil));
                            break;
                        case DEPROVISION:
                            profile.getResults().addAll(deprovision(delta, subjectKeys, attrUtil, false));
                            break;
                        case UNASSIGN:
                            profile.getResults().addAll(deprovision(delta, subjectKeys, attrUtil, true));
                            break;
                        case LINK:
                            profile.getResults().addAll(link(delta, subjectKeys, attrUtil, false));
                            break;
                        case UNLINK:
                            profile.getResults().addAll(link(delta, subjectKeys, attrUtil, true));
                            break;
                        default:
                        // do nothing
                    }
                }
            } else if (SyncDeltaType.DELETE == delta.getDeltaType()) {
                if (subjectKeys.isEmpty()) {
                    LOG.debug("No match found for deletion");
                } else {
                    profile.getResults().addAll(delete(delta, subjectKeys, attrUtil));
                }
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            LOG.warn(e.getMessage());
        }
    }

    private void audit(
            final String event,
            final Result result,
            final Object before,
            final Object output,
            final Object... input) {

        notificationManager.createTasks(
                AuditElements.EventCategoryType.SYNCHRONIZATION,
                getAttributableUtil().getType().name().toLowerCase(),
                profile.getTask().getResource().getKey(),
                event,
                result,
                before,
                output,
                input);

        auditManager.audit(
                AuditElements.EventCategoryType.SYNCHRONIZATION,
                getAttributableUtil().getType().name().toLowerCase(),
                profile.getTask().getResource().getKey(),
                event,
                result,
                before,
                output,
                input);
    }
}

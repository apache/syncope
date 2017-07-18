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
import java.util.Set;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.Result;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.resource.OrgUnit;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.provisioning.api.propagation.PropagationException;
import org.apache.syncope.core.provisioning.api.pushpull.IgnoreProvisionException;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningReport;
import org.apache.syncope.core.provisioning.api.pushpull.PullActions;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePullExecutor;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePullResultHandler;
import org.apache.syncope.core.provisioning.java.utils.ConnObjectUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = Throwable.class)
public class RealmPullResultHandlerImpl
        extends AbstractRealmResultHandler<PullTask, PullActions>
        implements SyncopePullResultHandler {

    @Autowired
    private PullUtils pullUtils;

    @Autowired
    private ConnObjectUtils connObjectUtils;

    @Autowired
    private AnySearchDAO searchDAO;

    private SyncopePullExecutor executor;

    private Result latestResult;

    @Override
    public void setPullExecutor(final SyncopePullExecutor executor) {
        this.executor = executor;
    }

    @Override
    public boolean handle(final SyncDelta delta) {
        try {
            OrgUnit orgUnit = profile.getTask().getResource().getOrgUnit();
            if (orgUnit == null) {
                throw new JobExecutionException("No orgUnit found on " + profile.getTask().getResource() + " for "
                        + delta.getObject().getObjectClass());
            }

            doHandle(delta, orgUnit);

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
            ignoreResult.setStatus(ProvisioningReport.Status.IGNORE);
            ignoreResult.setAnyType(REALM_TYPE);
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

    private List<ProvisioningReport> assign(final SyncDelta delta, final OrgUnit orgUnit) throws JobExecutionException {
        if (!profile.getTask().isPerformCreate()) {
            LOG.debug("PullTask not configured for create");
            finalize(UnmatchingRule.toEventName(UnmatchingRule.ASSIGN), Result.SUCCESS, null, null, delta);
            return Collections.<ProvisioningReport>emptyList();
        }

        RealmTO realmTO = connObjectUtils.getRealmTO(delta.getObject(), profile.getTask(), orgUnit);
        if (realmTO.getFullPath() == null) {
            if (realmTO.getParent() == null) {
                realmTO.setParent(profile.getTask().getDestinatioRealm().getFullPath());
            }

            realmTO.setFullPath(realmTO.getParent() + "/" + realmTO.getName());
        }
        realmTO.getResources().add(profile.getTask().getResource().getKey());

        ProvisioningReport result = new ProvisioningReport();
        result.setOperation(ResourceOperation.CREATE);
        result.setAnyType(REALM_TYPE);
        result.setStatus(ProvisioningReport.Status.SUCCESS);
        result.setName(realmTO.getFullPath());

        if (profile.isDryRun()) {
            result.setKey(null);
            finalize(UnmatchingRule.toEventName(UnmatchingRule.ASSIGN), Result.SUCCESS, null, null, delta);
        } else {
            SyncDelta actionedDelta = delta;
            for (PullActions action : profile.getActions()) {
                actionedDelta = action.beforeAssign(profile, actionedDelta, realmTO);
            }

            create(realmTO, actionedDelta, UnmatchingRule.toEventName(UnmatchingRule.ASSIGN), result);
        }

        return Collections.singletonList(result);
    }

    private List<ProvisioningReport> provision(final SyncDelta delta, final OrgUnit orgUnit)
            throws JobExecutionException {

        if (!profile.getTask().isPerformCreate()) {
            LOG.debug("PullTask not configured for create");
            finalize(UnmatchingRule.toEventName(UnmatchingRule.PROVISION), Result.SUCCESS, null, null, delta);
            return Collections.<ProvisioningReport>emptyList();
        }

        RealmTO realmTO = connObjectUtils.getRealmTO(delta.getObject(), profile.getTask(), orgUnit);
        if (realmTO.getFullPath() == null) {
            if (realmTO.getParent() == null) {
                realmTO.setParent(profile.getTask().getDestinatioRealm().getFullPath());
            }

            realmTO.setFullPath(realmTO.getParent() + "/" + realmTO.getName());
        }

        ProvisioningReport result = new ProvisioningReport();
        result.setOperation(ResourceOperation.CREATE);
        result.setAnyType(REALM_TYPE);
        result.setStatus(ProvisioningReport.Status.SUCCESS);
        result.setName(realmTO.getFullPath());

        if (profile.isDryRun()) {
            result.setKey(null);
            finalize(UnmatchingRule.toEventName(UnmatchingRule.PROVISION), Result.SUCCESS, null, null, delta);
        } else {
            SyncDelta actionedDelta = delta;
            for (PullActions action : profile.getActions()) {
                actionedDelta = action.beforeProvision(profile, actionedDelta, realmTO);
            }

            create(realmTO, actionedDelta, UnmatchingRule.toEventName(UnmatchingRule.PROVISION), result);
        }

        return Collections.singletonList(result);
    }

    private void throwIgnoreProvisionException(final SyncDelta delta, final Exception exception)
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

    private void create(
            final RealmTO realmTO,
            final SyncDelta delta,
            final String operation,
            final ProvisioningReport result)
            throws JobExecutionException {

        Object output;
        Result resultStatus;

        try {
            Realm realm = realmDAO.save(binder.create(profile.getTask().getDestinatioRealm().getFullPath(), realmTO));

            PropagationByResource propByRes = new PropagationByResource();
            for (String resource : realm.getResourceKeys()) {
                propByRes.add(ResourceOperation.CREATE, resource);
            }
            List<PropagationTask> tasks = propagationManager.createTasks(realm, propByRes, null);
            taskExecutor.execute(tasks, false);

            RealmTO actual = binder.getRealmTO(realm, true);

            result.setKey(actual.getKey());
            result.setName(profile.getTask().getDestinatioRealm().getFullPath() + "/" + actual.getName());

            output = actual;
            resultStatus = Result.SUCCESS;

            for (PullActions action : profile.getActions()) {
                action.after(profile, delta, actual, result);
            }

            LOG.debug("Realm {} successfully created", actual.getKey());
        } catch (PropagationException e) {
            // A propagation failure doesn't imply a pull failure.
            // The propagation exception status will be reported into the propagation task execution.
            LOG.error("Could not propagate Realm {}", delta.getUid().getUidValue(), e);
            output = e;
            resultStatus = Result.FAILURE;
        } catch (Exception e) {
            throwIgnoreProvisionException(delta, e);

            result.setStatus(ProvisioningReport.Status.FAILURE);
            result.setMessage(ExceptionUtils.getRootCauseMessage(e));
            LOG.error("Could not create Realm {} ", delta.getUid().getUidValue(), e);
            output = e;
            resultStatus = Result.FAILURE;
        }

        finalize(operation, resultStatus, null, output, delta);
    }

    private List<ProvisioningReport> update(final SyncDelta delta, final List<String> keys)
            throws JobExecutionException {

        if (!profile.getTask().isPerformUpdate()) {
            LOG.debug("PullTask not configured for update");
            finalize(MatchingRule.toEventName(MatchingRule.UPDATE), Result.SUCCESS, null, null, delta);
            return Collections.<ProvisioningReport>emptyList();
        }

        LOG.debug("About to update {}", keys);

        List<ProvisioningReport> results = new ArrayList<>();

        SyncDelta workingDelta = delta;
        for (String key : keys) {
            LOG.debug("About to update {}", key);

            ProvisioningReport result = new ProvisioningReport();
            result.setOperation(ResourceOperation.UPDATE);
            result.setAnyType(REALM_TYPE);
            result.setStatus(ProvisioningReport.Status.SUCCESS);
            result.setKey(key);

            Realm realm = realmDAO.find(key);
            RealmTO before = binder.getRealmTO(realm, true);
            if (before == null) {
                result.setStatus(ProvisioningReport.Status.FAILURE);
                result.setMessage(String.format("Realm '%s' not found", key));
            } else {
                result.setName(before.getFullPath());
            }

            if (!profile.isDryRun()) {
                Result resultStatus;
                Object output;

                if (before == null) {
                    resultStatus = Result.FAILURE;
                    output = null;
                } else {
                    try {
                        for (PullActions action : profile.getActions()) {
                            workingDelta = action.beforeUpdate(profile, workingDelta, before, null);
                        }

                        PropagationByResource propByRes = binder.update(realm, before);
                        realm = realmDAO.save(realm);
                        RealmTO updated = binder.getRealmTO(realm, true);

                        List<PropagationTask> tasks = propagationManager.createTasks(realm, propByRes, null);
                        taskExecutor.execute(tasks, false);

                        for (PullActions action : profile.getActions()) {
                            action.after(profile, workingDelta, updated, result);
                        }

                        output = updated;
                        resultStatus = Result.SUCCESS;
                        result.setName(updated.getFullPath());

                        LOG.debug("{} successfully updated", updated);
                    } catch (PropagationException e) {
                        // A propagation failure doesn't imply a pull failure.
                        // The propagation exception status will be reported into the propagation task execution.
                        LOG.error("Could not propagate Realm {}", workingDelta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = Result.FAILURE;
                    } catch (Exception e) {
                        throwIgnoreProvisionException(workingDelta, e);

                        result.setStatus(ProvisioningReport.Status.FAILURE);
                        result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error("Could not update Realm {}", workingDelta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = Result.FAILURE;
                    }
                }
                finalize(MatchingRule.toEventName(MatchingRule.UPDATE), resultStatus, before, output, workingDelta);
            }
            results.add(result);
        }

        return results;
    }

    private List<ProvisioningReport> deprovision(final SyncDelta delta, final List<String> keys, final boolean unlink)
            throws JobExecutionException {

        if (!profile.getTask().isPerformUpdate()) {
            LOG.debug("PullTask not configured for update");
            finalize(unlink
                    ? MatchingRule.toEventName(MatchingRule.UNASSIGN)
                    : MatchingRule.toEventName(MatchingRule.DEPROVISION), Result.SUCCESS, null, null, delta);
            return Collections.<ProvisioningReport>emptyList();
        }

        LOG.debug("About to deprovision {}", keys);

        final List<ProvisioningReport> results = new ArrayList<>();

        SyncDelta workingDelta = delta;
        for (String key : keys) {
            LOG.debug("About to unassign resource {}", key);

            ProvisioningReport result = new ProvisioningReport();
            result.setOperation(ResourceOperation.DELETE);
            result.setAnyType(REALM_TYPE);
            result.setStatus(ProvisioningReport.Status.SUCCESS);
            result.setKey(key);

            Realm realm = realmDAO.find(key);
            RealmTO before = binder.getRealmTO(realm, true);
            if (before == null) {
                result.setStatus(ProvisioningReport.Status.FAILURE);
                result.setMessage(String.format("Realm '%s' not found", key));
            } else {
                result.setName(before.getFullPath());
            }

            if (!profile.isDryRun()) {
                Object output;
                Result resultStatus;

                if (before == null) {
                    resultStatus = Result.FAILURE;
                    output = null;
                } else {
                    try {
                        if (unlink) {
                            for (PullActions action : profile.getActions()) {
                                workingDelta = action.beforeUnassign(profile, workingDelta, before);
                            }
                        } else {
                            for (PullActions action : profile.getActions()) {
                                workingDelta = action.beforeDeprovision(profile, workingDelta, before);
                            }
                        }

                        PropagationByResource propByRes = new PropagationByResource();
                        propByRes.add(ResourceOperation.DELETE, profile.getTask().getResource().getKey());
                        taskExecutor.execute(propagationManager.createTasks(realm, propByRes, null), false);

                        if (unlink) {
                            realm.getResources().remove(profile.getTask().getResource());
                            output = binder.getRealmTO(realmDAO.save(realm), true);
                        } else {
                            output = binder.getRealmTO(realm, true);
                        }

                        for (PullActions action : profile.getActions()) {
                            action.after(profile, workingDelta, RealmTO.class.cast(output), result);
                        }

                        resultStatus = Result.SUCCESS;

                        LOG.debug("{} successfully updated", realm);
                    } catch (PropagationException e) {
                        // A propagation failure doesn't imply a pull failure.
                        // The propagation exception status will be reported into the propagation task execution.
                        LOG.error("Could not propagate Realm {}", workingDelta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = Result.FAILURE;
                    } catch (Exception e) {
                        throwIgnoreProvisionException(workingDelta, e);

                        result.setStatus(ProvisioningReport.Status.FAILURE);
                        result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error("Could not update Realm {}", delta.getUid().getUidValue(), e);
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

    private List<ProvisioningReport> link(final SyncDelta delta, final List<String> keys, final boolean unlink)
            throws JobExecutionException {

        if (!profile.getTask().isPerformUpdate()) {
            LOG.debug("PullTask not configured for update");
            finalize(unlink
                    ? MatchingRule.toEventName(MatchingRule.UNLINK)
                    : MatchingRule.toEventName(MatchingRule.LINK), Result.SUCCESS, null, null, delta);
            return Collections.<ProvisioningReport>emptyList();
        }

        LOG.debug("About to link {}", keys);

        final List<ProvisioningReport> results = new ArrayList<>();

        SyncDelta workingDelta = delta;
        for (String key : keys) {
            LOG.debug("About to unassign resource {}", key);

            ProvisioningReport result = new ProvisioningReport();
            result.setOperation(ResourceOperation.NONE);
            result.setAnyType(REALM_TYPE);
            result.setStatus(ProvisioningReport.Status.SUCCESS);
            result.setKey(key);

            Realm realm = realmDAO.find(key);
            RealmTO before = binder.getRealmTO(realm, true);
            if (before == null) {
                result.setStatus(ProvisioningReport.Status.FAILURE);
                result.setMessage(String.format("Realm '%s' not found", key));
            } else {
                result.setName(before.getFullPath());
            }

            Object output;
            Result resultStatus;
            if (!profile.isDryRun()) {
                if (before == null) {
                    resultStatus = Result.FAILURE;
                    output = null;
                } else {
                    try {
                        if (unlink) {
                            for (PullActions action : profile.getActions()) {
                                workingDelta = action.beforeUnlink(profile, workingDelta, before);
                            }
                        } else {
                            for (PullActions action : profile.getActions()) {
                                workingDelta = action.beforeLink(profile, workingDelta, before);
                            }
                        }

                        if (unlink) {
                            realm.getResources().remove(profile.getTask().getResource());
                        } else {
                            realm.add(profile.getTask().getResource());
                        }
                        output = update(workingDelta, Collections.singletonList(key));

                        for (PullActions action : profile.getActions()) {
                            action.after(profile, workingDelta, RealmTO.class.cast(output), result);
                        }

                        resultStatus = Result.SUCCESS;

                        LOG.debug("{} successfully updated", realm);
                    } catch (PropagationException e) {
                        // A propagation failure doesn't imply a pull failure.
                        // The propagation exception status will be reported into the propagation task execution.
                        LOG.error("Could not propagate Realm {}", workingDelta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = Result.FAILURE;
                    } catch (Exception e) {
                        throwIgnoreProvisionException(workingDelta, e);

                        result.setStatus(ProvisioningReport.Status.FAILURE);
                        result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error("Could not update Realm {}", workingDelta.getUid().getUidValue(), e);
                        output = e;
                        resultStatus = Result.FAILURE;
                    }
                }
                finalize(unlink
                        ? MatchingRule.toEventName(MatchingRule.UNLINK)
                        : MatchingRule.toEventName(MatchingRule.LINK), resultStatus, before, output, workingDelta);
            }
            results.add(result);
        }

        return results;
    }

    private List<ProvisioningReport> delete(final SyncDelta delta, final List<String> keys)
            throws JobExecutionException {

        if (!profile.getTask().isPerformDelete()) {
            LOG.debug("PullTask not configured for delete");
            finalize(ResourceOperation.DELETE.name().toLowerCase(), Result.SUCCESS, null, null, delta);
            return Collections.<ProvisioningReport>emptyList();
        }

        LOG.debug("About to delete {}", keys);

        List<ProvisioningReport> results = new ArrayList<>();

        SyncDelta workingDelta = delta;
        for (String key : keys) {
            Object output;
            Result resultStatus = Result.FAILURE;

            ProvisioningReport result = new ProvisioningReport();

            try {
                result.setKey(key);
                result.setOperation(ResourceOperation.DELETE);
                result.setAnyType(REALM_TYPE);
                result.setStatus(ProvisioningReport.Status.SUCCESS);

                Realm realm = realmDAO.find(key);
                RealmTO before = binder.getRealmTO(realm, true);
                if (before == null) {
                    result.setStatus(ProvisioningReport.Status.FAILURE);
                    result.setMessage(String.format("Realm '%s' not found", key));
                } else {
                    result.setName(before.getFullPath());
                }

                if (!profile.isDryRun()) {
                    for (PullActions action : profile.getActions()) {
                        workingDelta = action.beforeDelete(profile, workingDelta, before);
                    }

                    try {
                        if (!realmDAO.findChildren(realm).isEmpty()) {
                            throw SyncopeClientException.build(ClientExceptionType.HasChildren);
                        }

                        Set<String> adminRealms = Collections.singleton(realm.getFullPath());
                        AnyCond keyCond = new AnyCond(AttributeCond.Type.ISNOTNULL);
                        keyCond.setSchema("key");
                        SearchCond allMatchingCond = SearchCond.getLeafCond(keyCond);
                        int users = searchDAO.count(adminRealms, allMatchingCond, AnyTypeKind.USER);
                        int groups = searchDAO.count(adminRealms, allMatchingCond, AnyTypeKind.GROUP);
                        int anyObjects = searchDAO.count(adminRealms, allMatchingCond, AnyTypeKind.ANY_OBJECT);

                        if (users + groups + anyObjects > 0) {
                            SyncopeClientException containedAnys = SyncopeClientException.build(
                                    ClientExceptionType.AssociatedAnys);
                            containedAnys.getElements().add(users + " user(s)");
                            containedAnys.getElements().add(groups + " group(s)");
                            containedAnys.getElements().add(anyObjects + " anyObject(s)");
                            throw containedAnys;
                        }

                        PropagationByResource propByRes = new PropagationByResource();
                        for (String resource : realm.getResourceKeys()) {
                            propByRes.add(ResourceOperation.DELETE, resource);
                        }
                        List<PropagationTask> tasks = propagationManager.createTasks(realm, propByRes, null);
                        taskExecutor.execute(tasks, false);

                        realmDAO.delete(realm);

                        output = null;
                        resultStatus = Result.SUCCESS;

                        for (PullActions action : profile.getActions()) {
                            action.after(profile, workingDelta, before, result);
                        }
                    } catch (Exception e) {
                        throwIgnoreProvisionException(workingDelta, e);

                        result.setStatus(ProvisioningReport.Status.FAILURE);
                        result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error("Could not delete {}", realm, e);
                        output = e;
                    }

                    finalize(ResourceOperation.DELETE.name().toLowerCase(), resultStatus, before, output, workingDelta);
                }

                results.add(result);
            } catch (DelegatedAdministrationException e) {
                LOG.error("Not allowed to read Realm {}", key, e);
            } catch (Exception e) {
                LOG.error("Could not delete Realm {}", key, e);
            }
        }

        return results;
    }

    private ProvisioningReport ignore(
            final SyncDelta delta,
            final boolean matching)
            throws JobExecutionException {

        LOG.debug("Any to ignore {}", delta.getObject().getUid().getUidValue());

        ProvisioningReport result = new ProvisioningReport();

        result.setKey(null);
        result.setName(delta.getObject().getUid().getUidValue());
        result.setOperation(ResourceOperation.NONE);
        result.setAnyType(REALM_TYPE);
        result.setStatus(ProvisioningReport.Status.SUCCESS);

        if (!profile.isDryRun()) {
            finalize(matching
                    ? MatchingRule.toEventName(MatchingRule.IGNORE)
                    : UnmatchingRule.toEventName(UnmatchingRule.IGNORE), Result.SUCCESS, null, null, delta);
        }

        return result;
    }

    private void doHandle(final SyncDelta delta, final OrgUnit orgUnit) throws JobExecutionException {
        LOG.debug("Process {} for {} as {}",
                delta.getDeltaType(), delta.getUid().getUidValue(), delta.getObject().getObjectClass());

        String uid = delta.getPreviousUid() == null
                ? delta.getUid().getUidValue()
                : delta.getPreviousUid().getUidValue();

        List<String> keys = pullUtils.findExisting(uid, delta.getObject(), orgUnit);
        LOG.debug("Match found for {} as {}: {}",
                delta.getUid().getUidValue(), delta.getObject().getObjectClass(), keys);

        if (keys.size() > 1) {
            switch (profile.getResAct()) {
                case IGNORE:
                    throw new IllegalStateException("More than one match " + keys);

                case FIRSTMATCH:
                    keys = keys.subList(0, 1);
                    break;

                case LASTMATCH:
                    keys = keys.subList(keys.size() - 1, keys.size());
                    break;

                default:
                // keep keys unmodified
                }
        }

        try {
            if (SyncDeltaType.CREATE_OR_UPDATE == delta.getDeltaType()) {
                if (keys.isEmpty()) {
                    switch (profile.getTask().getUnmatchingRule()) {
                        case ASSIGN:
                            profile.getResults().addAll(assign(delta, orgUnit));
                            break;

                        case PROVISION:
                            profile.getResults().addAll(provision(delta, orgUnit));
                            break;

                        case IGNORE:
                            profile.getResults().add(ignore(delta, false));
                            break;

                        default:
                        // do nothing
                    }
                } else {
                    switch (profile.getTask().getMatchingRule()) {
                        case UPDATE:
                            profile.getResults().addAll(update(delta, keys));
                            break;

                        case DEPROVISION:
                            profile.getResults().addAll(deprovision(delta, keys, false));
                            break;

                        case UNASSIGN:
                            profile.getResults().addAll(deprovision(delta, keys, true));
                            break;

                        case LINK:
                            profile.getResults().addAll(link(delta, keys, false));
                            break;

                        case UNLINK:
                            profile.getResults().addAll(link(delta, keys, true));
                            break;

                        case IGNORE:
                            profile.getResults().add(ignore(delta, true));
                            break;

                        default:
                        // do nothing
                    }
                }
            } else if (SyncDeltaType.DELETE == delta.getDeltaType()) {
                if (keys.isEmpty()) {
                    finalize(ResourceOperation.DELETE.name().toLowerCase(), Result.SUCCESS, null, null, delta);
                    LOG.debug("No match found for deletion");
                } else {
                    profile.getResults().addAll(delete(delta, keys));
                }
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            LOG.warn(e.getMessage());
        }
    }

    private void finalize(
            final String event,
            final Result result,
            final Object before,
            final Object output,
            final SyncDelta delta) {

        synchronized (this) {
            this.latestResult = result;
        }

        notificationManager.createTasks(AuditElements.EventCategoryType.PULL,
                REALM_TYPE.toLowerCase(),
                profile.getTask().getResource().getKey(),
                event,
                result,
                before,
                output,
                delta);

        auditManager.audit(AuditElements.EventCategoryType.PULL,
                REALM_TYPE.toLowerCase(),
                profile.getTask().getResource().getKey(),
                event,
                result,
                before,
                output,
                delta);
    }
}

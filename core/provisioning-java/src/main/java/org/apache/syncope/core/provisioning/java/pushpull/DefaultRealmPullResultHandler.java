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
import java.util.Set;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.Result;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.resource.OrgUnit;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.propagation.PropagationException;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.provisioning.api.pushpull.IgnoreProvisionException;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.core.provisioning.api.pushpull.PullActions;
import org.apache.syncope.core.provisioning.api.pushpull.RealmPullResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePullExecutor;
import org.apache.syncope.core.provisioning.java.utils.ConnObjectUtils;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = Throwable.class)
public class DefaultRealmPullResultHandler
        extends AbstractRealmResultHandler<PullTask, PullActions>
        implements RealmPullResultHandler {

    @Autowired
    private InboundMatcher inboundMatcher;

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
            executor.reportHandled(delta.getObjectClass(), delta.getObject().getName());

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
            ignoreResult.setAnyType(SyncopeConstants.REALM_ANYTYPE);
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

    private List<ProvisioningReport> assign(final SyncDelta delta, final OrgUnit orgUnit) throws JobExecutionException {
        if (!profile.getTask().isPerformCreate()) {
            LOG.debug("PullTask not configured for create");
            finalize(UnmatchingRule.toEventName(UnmatchingRule.ASSIGN), Result.SUCCESS, null, null, delta);
            return List.of();
        }

        RealmTO realmTO = connObjectUtils.getRealmTO(delta.getObject(), orgUnit);
        if (realmTO.getFullPath() == null) {
            if (realmTO.getParent() == null) {
                realmTO.setParent(profile.getTask().getDestinationRealm().getFullPath());
            }

            realmTO.setFullPath(realmTO.getParent() + '/' + realmTO.getName());
        }
        realmTO.getResources().add(profile.getTask().getResource().getKey());

        ProvisioningReport result = new ProvisioningReport();
        result.setOperation(ResourceOperation.CREATE);
        result.setAnyType(SyncopeConstants.REALM_ANYTYPE);
        result.setStatus(ProvisioningReport.Status.SUCCESS);
        result.setName(realmTO.getFullPath());

        if (profile.isDryRun()) {
            result.setKey(null);
            finalize(UnmatchingRule.toEventName(UnmatchingRule.ASSIGN), Result.SUCCESS, null, null, delta);
        } else {
            for (PullActions action : profile.getActions()) {
                action.beforeAssign(profile, delta, realmTO);
            }

            create(realmTO, delta, UnmatchingRule.ASSIGN, result);
        }

        return List.of(result);
    }

    private List<ProvisioningReport> provision(final SyncDelta delta, final OrgUnit orgUnit)
            throws JobExecutionException {

        if (!profile.getTask().isPerformCreate()) {
            LOG.debug("PullTask not configured for create");
            finalize(UnmatchingRule.toEventName(UnmatchingRule.PROVISION), Result.SUCCESS, null, null, delta);
            return List.of();
        }

        RealmTO realmTO = connObjectUtils.getRealmTO(delta.getObject(), orgUnit);
        if (realmTO.getFullPath() == null) {
            if (realmTO.getParent() == null) {
                realmTO.setParent(profile.getTask().getDestinationRealm().getFullPath());
            }

            realmTO.setFullPath(realmTO.getParent() + '/' + realmTO.getName());
        }

        ProvisioningReport result = new ProvisioningReport();
        result.setOperation(ResourceOperation.CREATE);
        result.setAnyType(SyncopeConstants.REALM_ANYTYPE);
        result.setStatus(ProvisioningReport.Status.SUCCESS);
        result.setName(realmTO.getFullPath());

        if (profile.isDryRun()) {
            result.setKey(null);
            finalize(UnmatchingRule.toEventName(UnmatchingRule.PROVISION), Result.SUCCESS, null, null, delta);
        } else {
            for (PullActions action : profile.getActions()) {
                action.beforeProvision(profile, delta, realmTO);
            }

            create(realmTO, delta, UnmatchingRule.PROVISION, result);
        }

        return List.of(result);
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
            final UnmatchingRule unmatchingRule,
            final ProvisioningReport result)
            throws JobExecutionException {

        Object output;
        Result resultStatus;

        try {
            Realm realm = realmDAO.save(binder.create(profile.getTask().getDestinationRealm(), realmTO));

            PropagationByResource<String> propByRes = new PropagationByResource<>();
            propByRes.addAll(ResourceOperation.CREATE, realm.getResourceKeys());
            if (unmatchingRule == UnmatchingRule.ASSIGN) {
                List<PropagationTaskInfo> taskInfos = propagationManager.createTasks(realm, propByRes, null);
                taskExecutor.execute(taskInfos, false, securityProperties.getAdminUser());
            }

            RealmTO actual = binder.getRealmTO(realm, true);

            result.setKey(actual.getKey());
            result.setName(profile.getTask().getDestinationRealm().getFullPath() + '/' + actual.getName());

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

        finalize(UnmatchingRule.toEventName(unmatchingRule), resultStatus, null, output, delta);
    }

    private List<ProvisioningReport> update(final SyncDelta delta, final List<Realm> realms, final boolean inLink)
            throws JobExecutionException {

        if (!profile.getTask().isPerformUpdate()) {
            LOG.debug("PullTask not configured for update");
            finalize(MatchingRule.toEventName(MatchingRule.UPDATE), Result.SUCCESS, null, null, delta);
            return List.of();
        }

        LOG.debug("About to update {}", realms);

        List<ProvisioningReport> results = new ArrayList<>();

        for (Realm realm : realms) {
            LOG.debug("About to update {}", realm);

            ProvisioningReport result = new ProvisioningReport();
            result.setOperation(ResourceOperation.UPDATE);
            result.setAnyType(SyncopeConstants.REALM_ANYTYPE);
            result.setStatus(ProvisioningReport.Status.SUCCESS);
            result.setKey(realm.getKey());
            result.setName(realm.getFullPath());

            if (!profile.isDryRun()) {
                Result resultStatus;
                Object output;

                RealmTO before = binder.getRealmTO(realm, true);
                try {
                    if (!inLink) {
                        for (PullActions action : profile.getActions()) {
                            action.beforeUpdate(profile, delta, before, null);
                        }
                    }

                    PropagationByResource<String> propByRes = binder.update(realm, before);
                    realm = realmDAO.save(realm);
                    RealmTO updated = binder.getRealmTO(realm, true);

                    List<PropagationTaskInfo> taskInfos = propagationManager.createTasks(realm, propByRes, null);
                    taskExecutor.execute(taskInfos, false, securityProperties.getAdminUser());

                    for (PullActions action : profile.getActions()) {
                        action.after(profile, delta, updated, result);
                    }

                    output = updated;
                    resultStatus = Result.SUCCESS;
                    result.setName(updated.getFullPath());

                    LOG.debug("{} successfully updated", updated);
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
                    LOG.error("Could not update Realm {}", delta.getUid().getUidValue(), e);
                    output = e;
                    resultStatus = Result.FAILURE;
                }

                finalize(MatchingRule.toEventName(MatchingRule.UPDATE), resultStatus, before, output, delta);
            }

            results.add(result);
        }

        return results;
    }

    private List<ProvisioningReport> deprovision(final SyncDelta delta, final List<Realm> realms, final boolean unlink)
            throws JobExecutionException {

        if (!profile.getTask().isPerformUpdate()) {
            LOG.debug("PullTask not configured for update");
            finalize(unlink
                    ? MatchingRule.toEventName(MatchingRule.UNASSIGN)
                    : MatchingRule.toEventName(MatchingRule.DEPROVISION), Result.SUCCESS, null, null, delta);
            return List.of();
        }

        LOG.debug("About to deprovision {}", realms);

        final List<ProvisioningReport> results = new ArrayList<>();

        for (Realm realm : realms) {
            LOG.debug("About to unassign resource {}", realm);

            ProvisioningReport result = new ProvisioningReport();
            result.setOperation(ResourceOperation.DELETE);
            result.setAnyType(SyncopeConstants.REALM_ANYTYPE);
            result.setStatus(ProvisioningReport.Status.SUCCESS);
            result.setKey(realm.getKey());
            result.setName(realm.getFullPath());

            if (!profile.isDryRun()) {
                Object output;
                Result resultStatus;

                RealmTO before = binder.getRealmTO(realm, true);
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
                    taskExecutor.execute(
                            propagationManager.createTasks(realm, propByRes, null),
                            false, securityProperties.getAdminUser());

                    RealmTO realmTO;
                    if (unlink) {
                        realm.getResources().remove(profile.getTask().getResource());
                        realmTO = binder.getRealmTO(realmDAO.save(realm), true);
                    } else {
                        realmTO = binder.getRealmTO(realm, true);
                    }
                    output = realmTO;

                    for (PullActions action : profile.getActions()) {
                        action.after(profile, delta, realmTO, result);
                    }

                    resultStatus = Result.SUCCESS;

                    LOG.debug("{} successfully updated", realm);
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
                    LOG.error("Could not update Realm {}", delta.getUid().getUidValue(), e);
                    output = e;
                    resultStatus = Result.FAILURE;
                }

                finalize(unlink
                        ? MatchingRule.toEventName(MatchingRule.UNASSIGN)
                        : MatchingRule.toEventName(MatchingRule.DEPROVISION), resultStatus, before, output, delta);
            }

            results.add(result);
        }

        return results;
    }

    private List<ProvisioningReport> link(final SyncDelta delta, final List<Realm> realms, final boolean unlink)
            throws JobExecutionException {

        if (!profile.getTask().isPerformUpdate()) {
            LOG.debug("PullTask not configured for update");
            finalize(unlink
                    ? MatchingRule.toEventName(MatchingRule.UNLINK)
                    : MatchingRule.toEventName(MatchingRule.LINK), Result.SUCCESS, null, null, delta);
            return List.of();
        }

        LOG.debug("About to link {}", realms);

        final List<ProvisioningReport> results = new ArrayList<>();

        for (Realm realm : realms) {
            LOG.debug("About to unassign resource {}", realm);

            ProvisioningReport result = new ProvisioningReport();
            result.setOperation(ResourceOperation.NONE);
            result.setAnyType(SyncopeConstants.REALM_ANYTYPE);
            result.setStatus(ProvisioningReport.Status.SUCCESS);
            result.setKey(realm.getKey());
            result.setName(realm.getFullPath());

            if (!profile.isDryRun()) {
                Object output;
                Result resultStatus;

                RealmTO before = binder.getRealmTO(realm, true);
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

                    if (unlink) {
                        realm.getResources().remove(profile.getTask().getResource());
                    } else {
                        realm.add(profile.getTask().getResource());
                    }
                    output = update(delta, List.of(realm), true);

                    resultStatus = Result.SUCCESS;

                    LOG.debug("{} successfully updated", realm);
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
                    LOG.error("Could not update Realm {}", delta.getUid().getUidValue(), e);
                    output = e;
                    resultStatus = Result.FAILURE;
                }

                finalize(unlink
                        ? MatchingRule.toEventName(MatchingRule.UNLINK)
                        : MatchingRule.toEventName(MatchingRule.LINK), resultStatus, before, output, delta);
            }
            results.add(result);
        }

        return results;
    }

    private List<ProvisioningReport> delete(final SyncDelta delta, final List<Realm> realms)
            throws JobExecutionException {

        if (!profile.getTask().isPerformDelete()) {
            LOG.debug("PullTask not configured for delete");
            finalize(ResourceOperation.DELETE.name().toLowerCase(), Result.SUCCESS, null, null, delta);
            return List.of();
        }

        LOG.debug("About to delete {}", realms);

        List<ProvisioningReport> results = new ArrayList<>();

        realms.forEach(realm -> {
            Object output;
            Result resultStatus = Result.FAILURE;

            ProvisioningReport result = new ProvisioningReport();

            RealmTO before = binder.getRealmTO(realm, true);
            try {
                result.setKey(realm.getKey());
                result.setName(realm.getFullPath());
                result.setOperation(ResourceOperation.DELETE);
                result.setAnyType(SyncopeConstants.REALM_ANYTYPE);
                result.setStatus(ProvisioningReport.Status.SUCCESS);

                if (!profile.isDryRun()) {
                    for (PullActions action : profile.getActions()) {
                        action.beforeDelete(profile, delta, before);
                    }

                    try {
                        if (!realmDAO.findChildren(realm).isEmpty()) {
                            throw SyncopeClientException.build(ClientExceptionType.HasChildren);
                        }

                        Set<String> adminRealms = Set.of(realm.getFullPath());
                        AnyCond keyCond = new AnyCond(AttrCond.Type.ISNOTNULL);
                        keyCond.setSchema("key");
                        SearchCond allMatchingCond = SearchCond.getLeaf(keyCond);
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

                        PropagationByResource<String> propByRes = new PropagationByResource<>();
                        propByRes.addAll(ResourceOperation.DELETE, realm.getResourceKeys());
                        List<PropagationTaskInfo> taskInfos = propagationManager.createTasks(realm, propByRes, null);
                        taskExecutor.execute(taskInfos, false, securityProperties.getAdminUser());

                        realmDAO.delete(realm);

                        output = null;
                        resultStatus = Result.SUCCESS;

                        for (PullActions action : profile.getActions()) {
                            action.after(profile, delta, before, result);
                        }
                    } catch (Exception e) {
                        throwIgnoreProvisionException(delta, e);

                        result.setStatus(ProvisioningReport.Status.FAILURE);
                        result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error("Could not delete {}", realm, e);
                        output = e;
                    }

                    finalize(ResourceOperation.DELETE.name().toLowerCase(), resultStatus, before, output, delta);
                }

                results.add(result);
            } catch (DelegatedAdministrationException e) {
                LOG.error("Not allowed to read Realm {}", realm, e);
            } catch (Exception e) {
                LOG.error("Could not delete Realm {}", realm, e);
            }
        });

        return results;
    }

    private ProvisioningReport ignore(final SyncDelta delta, final boolean matching) throws JobExecutionException {
        LOG.debug("Any to ignore {}", delta.getObject().getUid().getUidValue());

        ProvisioningReport result = new ProvisioningReport();

        result.setKey(null);
        result.setName(delta.getObject().getUid().getUidValue());
        result.setOperation(ResourceOperation.NONE);
        result.setAnyType(SyncopeConstants.REALM_ANYTYPE);
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

        SyncDelta finalDelta = delta;
        for (PullActions action : profile.getActions()) {
            finalDelta = action.preprocess(profile, finalDelta);
        }

        LOG.debug("Transformed {} for {} as {}",
                finalDelta.getDeltaType(), finalDelta.getUid().getUidValue(), finalDelta.getObject().getObjectClass());

        List<Realm> realms = inboundMatcher.match(finalDelta, orgUnit);
        LOG.debug("Match found for {} as {}: {}",
                finalDelta.getUid().getUidValue(), finalDelta.getObject().getObjectClass(), realms);

        if (realms.size() > 1) {
            switch (profile.getConflictResolutionAction()) {
                case IGNORE:
                    throw new IgnoreProvisionException("More than one match found for "
                            + finalDelta.getObject().getUid().getUidValue() + ": " + realms);

                case FIRSTMATCH:
                    realms = realms.subList(0, 1);
                    break;

                case LASTMATCH:
                    realms = realms.subList(realms.size() - 1, realms.size());
                    break;

                default:
                // keep keys unmodified
                }
        }

        try {
            if (SyncDeltaType.CREATE_OR_UPDATE == finalDelta.getDeltaType()) {
                if (realms.isEmpty()) {
                    switch (profile.getTask().getUnmatchingRule()) {
                        case ASSIGN:
                            profile.getResults().addAll(assign(finalDelta, orgUnit));
                            break;

                        case PROVISION:
                            profile.getResults().addAll(provision(finalDelta, orgUnit));
                            break;

                        case IGNORE:
                            profile.getResults().add(ignore(finalDelta, false));
                            break;

                        default:
                        // do nothing
                    }
                } else {
                    switch (profile.getTask().getMatchingRule()) {
                        case UPDATE:
                            profile.getResults().addAll(update(finalDelta, realms, false));
                            break;

                        case DEPROVISION:
                            profile.getResults().addAll(deprovision(finalDelta, realms, false));
                            break;

                        case UNASSIGN:
                            profile.getResults().addAll(deprovision(finalDelta, realms, true));
                            break;

                        case LINK:
                            profile.getResults().addAll(link(finalDelta, realms, false));
                            break;

                        case UNLINK:
                            profile.getResults().addAll(link(finalDelta, realms, true));
                            break;

                        case IGNORE:
                            profile.getResults().add(ignore(finalDelta, true));
                            break;

                        default:
                        // do nothing
                    }
                }
            } else if (SyncDeltaType.DELETE == finalDelta.getDeltaType()) {
                if (realms.isEmpty()) {
                    finalize(ResourceOperation.DELETE.name().toLowerCase(), Result.SUCCESS, null, null, finalDelta);
                    LOG.debug("No match found for deletion");
                } else {
                    profile.getResults().addAll(delete(finalDelta, realms));
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

        notificationManager.createTasks(
                AuthContextUtils.getWho(),
                AuditElements.EventCategoryType.PULL,
                SyncopeConstants.REALM_ANYTYPE.toLowerCase(),
                profile.getTask().getResource().getKey(),
                event,
                result,
                before,
                output,
                delta);

        auditManager.audit(
                AuthContextUtils.getWho(),
                AuditElements.EventCategoryType.PULL,
                SyncopeConstants.REALM_ANYTYPE.toLowerCase(),
                profile.getTask().getResource().getKey(),
                event,
                result,
                before,
                output,
                delta);
    }
}

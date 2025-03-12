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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.OrgUnit;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.CASSPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.OIDCRPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.SAML2SPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.api.propagation.PropagationException;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.provisioning.api.pushpull.IgnoreProvisionException;
import org.apache.syncope.core.provisioning.api.pushpull.InboundActions;
import org.apache.syncope.core.provisioning.api.pushpull.RealmPullResultHandler;
import org.apache.syncope.core.provisioning.java.utils.ConnObjectUtils;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = Throwable.class)
public class DefaultRealmPullResultHandler
        extends AbstractRealmResultHandler<PullTask, InboundActions>
        implements RealmPullResultHandler {

    protected static OpEvent.Outcome and(final OpEvent.Outcome left, final OpEvent.Outcome right) {
        return left == OpEvent.Outcome.SUCCESS && right == OpEvent.Outcome.SUCCESS
                ? OpEvent.Outcome.SUCCESS
                : OpEvent.Outcome.FAILURE;
    }

    @Autowired
    protected InboundMatcher inboundMatcher;

    @Autowired
    protected ConnObjectUtils connObjectUtils;

    @Autowired
    protected AnySearchDAO searchDAO;

    @Autowired
    protected TaskDAO taskDAO;

    @Autowired
    protected CASSPClientAppDAO casSPClientAppDAO;

    @Autowired
    protected OIDCRPClientAppDAO oidcRPClientAppDAO;

    @Autowired
    protected SAML2SPClientAppDAO saml2SPClientAppDAO;

    @Override
    public boolean handle(final SyncDelta delta) {
        try {
            OrgUnit orgUnit = Optional.ofNullable(profile.getTask().getResource().getOrgUnit()).
                    orElseThrow(() -> new JobExecutionException(
                    "No orgUnit found on " + profile.getTask().getResource() + " for "
                    + delta.getObject().getObjectClass()));

            OpEvent.Outcome latestResult = doHandle(delta, orgUnit);

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
            ignoreResult.setStatus(ProvisioningReport.Status.IGNORE);
            ignoreResult.setAnyType(SyncopeConstants.REALM_ANYTYPE);
            ignoreResult.setKey(null);
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

    protected OpEvent.Outcome assign(final SyncDelta delta, final OrgUnit orgUnit)
            throws JobExecutionException {

        if (!profile.getTask().isPerformCreate()) {
            LOG.debug("PullTask not configured for create");
            end(UnmatchingRule.toOp(UnmatchingRule.ASSIGN), OpEvent.Outcome.SUCCESS, null, null, delta);
            return OpEvent.Outcome.SUCCESS;
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
            end(UnmatchingRule.toOp(UnmatchingRule.ASSIGN), OpEvent.Outcome.SUCCESS, null, null, delta);
            return OpEvent.Outcome.SUCCESS;
        }

        for (InboundActions action : profile.getActions()) {
            action.beforeAssign(profile, delta, realmTO);
        }

        profile.getResults().add(result);

        return create(realmTO, delta, UnmatchingRule.ASSIGN, result);
    }

    protected OpEvent.Outcome provision(final SyncDelta delta, final OrgUnit orgUnit)
            throws JobExecutionException {

        if (!profile.getTask().isPerformCreate()) {
            LOG.debug("PullTask not configured for create");
            end(UnmatchingRule.toOp(UnmatchingRule.PROVISION), OpEvent.Outcome.SUCCESS, null, null, delta);
            return OpEvent.Outcome.SUCCESS;
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
            end(UnmatchingRule.toOp(UnmatchingRule.PROVISION), OpEvent.Outcome.SUCCESS, null, null, delta);
            return OpEvent.Outcome.SUCCESS;
        }

        for (InboundActions action : profile.getActions()) {
            action.beforeProvision(profile, delta, realmTO);
        }

        profile.getResults().add(result);

        return create(realmTO, delta, UnmatchingRule.PROVISION, result);
    }

    protected OpEvent.Outcome create(
            final RealmTO realmTO,
            final SyncDelta delta,
            final UnmatchingRule unmatchingRule,
            final ProvisioningReport result)
            throws JobExecutionException {

        Object output;
        OpEvent.Outcome resultStatus;

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
            resultStatus = OpEvent.Outcome.SUCCESS;

            for (InboundActions action : profile.getActions()) {
                action.after(profile, delta, actual, result);
            }

            LOG.debug("Realm {} successfully created", actual.getKey());
        } catch (PropagationException e) {
            // A propagation failure doesn't imply a pull failure.
            // The propagation exception status will be reported into the propagation task execution.
            LOG.error("Could not propagate Realm {}", delta.getUid().getUidValue(), e);
            output = e;
            resultStatus = OpEvent.Outcome.FAILURE;
        } catch (Exception e) {
            throwIgnoreProvisionException(delta, e);

            result.setStatus(ProvisioningReport.Status.FAILURE);
            result.setMessage(ExceptionUtils.getRootCauseMessage(e));
            LOG.error("Could not create Realm {} ", delta.getUid().getUidValue(), e);
            output = e;
            resultStatus = OpEvent.Outcome.FAILURE;
        }

        end(UnmatchingRule.toOp(unmatchingRule), resultStatus, null, output, delta);
        return resultStatus;
    }

    protected OpEvent.Outcome update(final SyncDelta delta, final List<Realm> realms, final boolean inLink)
            throws JobExecutionException {

        if (!profile.getTask().isPerformUpdate()) {
            LOG.debug("PullTask not configured for update");
            end(MatchingRule.toOp(MatchingRule.UPDATE), OpEvent.Outcome.SUCCESS, null, null, delta);
            return OpEvent.Outcome.SUCCESS;
        }

        LOG.debug("About to update {}", realms);

        OpEvent.Outcome global = OpEvent.Outcome.SUCCESS;
        for (Realm realm : realms) {
            LOG.debug("About to update {}", realm);

            ProvisioningReport result = new ProvisioningReport();
            result.setOperation(ResourceOperation.UPDATE);
            result.setAnyType(SyncopeConstants.REALM_ANYTYPE);
            result.setStatus(ProvisioningReport.Status.SUCCESS);
            result.setKey(realm.getKey());
            result.setName(realm.getFullPath());

            if (!profile.isDryRun()) {
                OpEvent.Outcome resultStatus;
                Object output;

                RealmTO before = binder.getRealmTO(realm, true);
                try {
                    if (!inLink) {
                        for (InboundActions action : profile.getActions()) {
                            action.beforeUpdate(profile, delta, before, null);
                        }
                    }

                    Map<Pair<String, String>, Set<Attribute>> beforeAttrs = propagationManager.prepareAttrs(realm);

                    PropagationByResource<String> propByRes = binder.update(realm, before);
                    realm = realmDAO.save(realm);
                    RealmTO updated = binder.getRealmTO(realm, true);

                    List<PropagationTaskInfo> taskInfos = propagationManager.setAttributeDeltas(
                            propagationManager.createTasks(realm, propByRes, null),
                            beforeAttrs);
                    taskExecutor.execute(taskInfos, false, securityProperties.getAdminUser());

                    for (InboundActions action : profile.getActions()) {
                        action.after(profile, delta, updated, result);
                    }

                    output = updated;
                    resultStatus = OpEvent.Outcome.SUCCESS;
                    result.setName(updated.getFullPath());

                    LOG.debug("{} successfully updated", updated);
                } catch (PropagationException e) {
                    // A propagation failure doesn't imply a pull failure.
                    // The propagation exception status will be reported into the propagation task execution.
                    LOG.error("Could not propagate Realm {}", delta.getUid().getUidValue(), e);
                    output = e;
                    resultStatus = OpEvent.Outcome.FAILURE;
                } catch (Exception e) {
                    throwIgnoreProvisionException(delta, e);

                    result.setStatus(ProvisioningReport.Status.FAILURE);
                    result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                    LOG.error("Could not update Realm {}", delta.getUid().getUidValue(), e);
                    output = e;
                    resultStatus = OpEvent.Outcome.FAILURE;
                }

                end(MatchingRule.toOp(MatchingRule.UPDATE), resultStatus, before, output, delta);
                global = and(global, resultStatus);
            }

            profile.getResults().add(result);
        }

        return global;
    }

    protected OpEvent.Outcome deprovision(final SyncDelta delta, final List<Realm> realms, final boolean unlink)
            throws JobExecutionException {

        if (!profile.getTask().isPerformUpdate()) {
            LOG.debug("PullTask not configured for update");
            end(unlink
                    ? MatchingRule.toOp(MatchingRule.UNASSIGN)
                    : MatchingRule.toOp(MatchingRule.DEPROVISION), OpEvent.Outcome.SUCCESS, null, null, delta);
            return OpEvent.Outcome.SUCCESS;
        }

        LOG.debug("About to deprovision {}", realms);

        OpEvent.Outcome global = OpEvent.Outcome.SUCCESS;
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
                OpEvent.Outcome resultStatus;

                RealmTO before = binder.getRealmTO(realm, true);
                try {
                    if (unlink) {
                        for (InboundActions action : profile.getActions()) {
                            action.beforeUnassign(profile, delta, before);
                        }
                    } else {
                        for (InboundActions action : profile.getActions()) {
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

                    for (InboundActions action : profile.getActions()) {
                        action.after(profile, delta, realmTO, result);
                    }

                    resultStatus = OpEvent.Outcome.SUCCESS;

                    LOG.debug("{} successfully updated", realm);
                } catch (PropagationException e) {
                    // A propagation failure doesn't imply a pull failure.
                    // The propagation exception status will be reported into the propagation task execution.
                    LOG.error("Could not propagate Realm {}", delta.getUid().getUidValue(), e);
                    output = e;
                    resultStatus = OpEvent.Outcome.FAILURE;
                } catch (Exception e) {
                    throwIgnoreProvisionException(delta, e);

                    result.setStatus(ProvisioningReport.Status.FAILURE);
                    result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                    LOG.error("Could not update Realm {}", delta.getUid().getUidValue(), e);
                    output = e;
                    resultStatus = OpEvent.Outcome.FAILURE;
                }

                end(unlink
                        ? MatchingRule.toOp(MatchingRule.UNASSIGN)
                        : MatchingRule.toOp(MatchingRule.DEPROVISION),
                        resultStatus, before, output, delta);
                global = and(global, resultStatus);
            }

            profile.getResults().add(result);
        }

        return global;
    }

    protected OpEvent.Outcome link(final SyncDelta delta, final List<Realm> realms, final boolean unlink)
            throws JobExecutionException {

        if (!profile.getTask().isPerformUpdate()) {
            LOG.debug("PullTask not configured for update");
            end(unlink
                    ? MatchingRule.toOp(MatchingRule.UNLINK)
                    : MatchingRule.toOp(MatchingRule.LINK), OpEvent.Outcome.SUCCESS, null, null, delta);
            return OpEvent.Outcome.SUCCESS;
        }

        LOG.debug("About to link {}", realms);

        OpEvent.Outcome global = OpEvent.Outcome.SUCCESS;
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
                OpEvent.Outcome resultStatus;

                RealmTO before = binder.getRealmTO(realm, true);
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

                    if (unlink) {
                        realm.getResources().remove(profile.getTask().getResource());
                    } else {
                        realm.add(profile.getTask().getResource());
                    }
                    output = update(delta, List.of(realm), true);

                    resultStatus = OpEvent.Outcome.SUCCESS;

                    LOG.debug("{} successfully updated", realm);
                } catch (PropagationException e) {
                    // A propagation failure doesn't imply a pull failure.
                    // The propagation exception status will be reported into the propagation task execution.
                    LOG.error("Could not propagate Realm {}", delta.getUid().getUidValue(), e);
                    output = e;
                    resultStatus = OpEvent.Outcome.FAILURE;
                } catch (Exception e) {
                    throwIgnoreProvisionException(delta, e);

                    result.setStatus(ProvisioningReport.Status.FAILURE);
                    result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                    LOG.error("Could not update Realm {}", delta.getUid().getUidValue(), e);
                    output = e;
                    resultStatus = OpEvent.Outcome.FAILURE;
                }

                end(unlink
                        ? MatchingRule.toOp(MatchingRule.UNLINK)
                        : MatchingRule.toOp(MatchingRule.LINK),
                        resultStatus, before, output, delta);
                global = and(global, resultStatus);
            }

            profile.getResults().add(result);
        }

        return global;
    }

    protected OpEvent.Outcome delete(final SyncDelta delta, final List<Realm> realms) {

        if (!profile.getTask().isPerformDelete()) {
            LOG.debug("PullTask not configured for delete");
            end(ResourceOperation.DELETE.name().toLowerCase(), OpEvent.Outcome.SUCCESS, null, null, delta);
            return OpEvent.Outcome.SUCCESS;
        }

        LOG.debug("About to delete {}", realms);

        OpEvent.Outcome global = OpEvent.Outcome.SUCCESS;
        for (Realm realm : realms) {
            Object output;
            OpEvent.Outcome resultStatus = OpEvent.Outcome.FAILURE;

            ProvisioningReport result = new ProvisioningReport();

            RealmTO before = binder.getRealmTO(realm, true);
            try {
                result.setKey(realm.getKey());
                result.setName(realm.getFullPath());
                result.setOperation(ResourceOperation.DELETE);
                result.setAnyType(SyncopeConstants.REALM_ANYTYPE);
                result.setStatus(ProvisioningReport.Status.SUCCESS);

                if (!profile.isDryRun()) {
                    for (InboundActions action : profile.getActions()) {
                        action.beforeDelete(profile, delta, before);
                    }

                    try {
                        if (!realmSearchDAO.findChildren(realm).isEmpty()) {
                            throw SyncopeClientException.build(ClientExceptionType.RealmContains);
                        }

                        Set<String> adminRealms = Set.of(realm.getFullPath());
                        AnyCond keyCond = new AnyCond(AttrCond.Type.ISNOTNULL);
                        keyCond.setSchema("key");
                        SearchCond allMatchingCond = SearchCond.of(keyCond);
                        long users = searchDAO.count(
                                realmDAO.getRoot(), true, adminRealms, allMatchingCond, AnyTypeKind.USER);
                        long groups = searchDAO.count(
                                realmDAO.getRoot(), true, adminRealms, allMatchingCond, AnyTypeKind.GROUP);
                        long anyObjects = searchDAO.count(
                                realmDAO.getRoot(), true, adminRealms, allMatchingCond, AnyTypeKind.ANY_OBJECT);
                        long macroTasks = taskDAO.findByRealm(realm).size();
                        long clientApps = casSPClientAppDAO.findAllByRealm(realm).size()
                                + saml2SPClientAppDAO.findAllByRealm(realm).size()
                                + oidcRPClientAppDAO.findAllByRealm(realm).size();

                        if (users + groups + anyObjects + macroTasks + clientApps > 0) {
                            SyncopeClientException realmContains =
                                    SyncopeClientException.build(ClientExceptionType.RealmContains);
                            realmContains.getElements().add(users + " user(s)");
                            realmContains.getElements().add(groups + " group(s)");
                            realmContains.getElements().add(anyObjects + " anyObject(s)");
                            realmContains.getElements().add(macroTasks + " command task(s)");
                            realmContains.getElements().add(clientApps + " client app(s)");
                            throw realmContains;
                        }

                        PropagationByResource<String> propByRes = new PropagationByResource<>();
                        propByRes.addAll(ResourceOperation.DELETE, realm.getResourceKeys());
                        List<PropagationTaskInfo> taskInfos = propagationManager.createTasks(realm, propByRes, null);
                        taskExecutor.execute(taskInfos, false, securityProperties.getAdminUser());

                        realmDAO.delete(realm);

                        output = null;
                        resultStatus = OpEvent.Outcome.SUCCESS;

                        for (InboundActions action : profile.getActions()) {
                            action.after(profile, delta, before, result);
                        }
                    } catch (Exception e) {
                        throwIgnoreProvisionException(delta, e);

                        result.setStatus(ProvisioningReport.Status.FAILURE);
                        result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                        LOG.error("Could not delete {}", realm, e);
                        output = e;
                    }

                    end(ResourceOperation.DELETE.name().toLowerCase(), resultStatus, before, output, delta);
                    global = and(global, resultStatus);
                }

                profile.getResults().add(result);
            } catch (DelegatedAdministrationException e) {
                LOG.error("Not allowed to read Realm {}", realm, e);
            } catch (Exception e) {
                LOG.error("Could not delete Realm {}", realm, e);
            }
        }

        return global;
    }

    protected OpEvent.Outcome ignore(final SyncDelta delta, final boolean matching) {
        LOG.debug("Any to ignore {}", delta.getObject().getUid().getUidValue());

        ProvisioningReport report = new ProvisioningReport();
        report.setKey(null);
        report.setName(delta.getObject().getUid().getUidValue());
        report.setOperation(ResourceOperation.NONE);
        report.setAnyType(SyncopeConstants.REALM_ANYTYPE);
        report.setStatus(ProvisioningReport.Status.SUCCESS);
        profile.getResults().add(report);

        if (!profile.isDryRun()) {
            end(matching
                    ? MatchingRule.toOp(MatchingRule.IGNORE)
                    : UnmatchingRule.toOp(UnmatchingRule.IGNORE), OpEvent.Outcome.SUCCESS, null, null, delta);
            return OpEvent.Outcome.SUCCESS;
        }

        return OpEvent.Outcome.SUCCESS;
    }

    protected OpEvent.Outcome doHandle(final SyncDelta delta, final OrgUnit orgUnit) throws JobExecutionException {
        LOG.debug("Process {} for {} as {}",
                delta.getDeltaType(), delta.getUid().getUidValue(), delta.getObject().getObjectClass());

        SyncDelta finalDelta = delta;
        for (InboundActions action : profile.getActions()) {
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

        OpEvent.Outcome result = OpEvent.Outcome.SUCCESS;
        try {
            switch (delta.getDeltaType()) {
                case CREATE:
                case UPDATE:
                case CREATE_OR_UPDATE:
                    if (realms.isEmpty()) {
                        switch (profile.getTask().getUnmatchingRule()) {
                            case ASSIGN:
                                result = assign(finalDelta, orgUnit);
                                break;

                            case PROVISION:
                                result = provision(finalDelta, orgUnit);
                                break;

                            case IGNORE:
                                result = ignore(finalDelta, false);
                                break;

                            default:
                            // do nothing
                        }
                    } else {
                        switch (profile.getTask().getMatchingRule()) {
                            case UPDATE:
                                result = update(finalDelta, realms, false);
                                break;

                            case DEPROVISION:
                                result = deprovision(finalDelta, realms, false);
                                break;

                            case UNASSIGN:
                                result = deprovision(finalDelta, realms, true);
                                break;

                            case LINK:
                                result = link(finalDelta, realms, false);
                                break;

                            case UNLINK:
                                result = link(finalDelta, realms, true);
                                break;

                            case IGNORE:
                                result = ignore(finalDelta, true);
                                break;

                            default:
                            // do nothing
                        }
                    }
                    break;

                case DELETE:
                    if (realms.isEmpty()) {
                        end(ResourceOperation.DELETE.name().toLowerCase(), OpEvent.Outcome.SUCCESS, null, null,
                                finalDelta);
                        LOG.debug("No match found for deletion");
                    } else {
                        result = delete(finalDelta, realms);
                    }
                    break;

                default:
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            LOG.warn(e.getMessage());
        }

        return result;
    }

    protected void end(
            final String event,
            final OpEvent.Outcome result,
            final Object before,
            final Object output,
            final SyncDelta delta) {

        notificationManager.createTasks(
                AuthContextUtils.getWho(),
                OpEvent.CategoryType.PULL,
                SyncopeConstants.REALM_ANYTYPE.toLowerCase(),
                profile.getTask().getResource().getKey(),
                event,
                result,
                before,
                output,
                delta);

        auditManager.audit(
                AuthContextUtils.getDomain(),
                AuthContextUtils.getWho(),
                OpEvent.CategoryType.PULL,
                SyncopeConstants.REALM_ANYTYPE.toLowerCase(),
                profile.getTask().getResource().getKey(),
                event,
                result,
                before,
                output,
                delta);
    }
}

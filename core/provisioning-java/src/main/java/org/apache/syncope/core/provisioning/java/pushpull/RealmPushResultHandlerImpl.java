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
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.Result;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.resource.OrgUnit;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.TimeoutException;
import org.apache.syncope.core.provisioning.api.pushpull.IgnoreProvisionException;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningReport;
import org.apache.syncope.core.provisioning.api.pushpull.PushActions;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePushResultHandler;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.quartz.JobExecutionException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class RealmPushResultHandlerImpl
        extends AbstractRealmResultHandler<PushTask, PushActions>
        implements SyncopePushResultHandler {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public boolean handle(final String realmKey) {
        Realm realm = null;
        try {
            realm = realmDAO.find(realmKey);
            doHandle(realm);
            return true;
        } catch (IgnoreProvisionException e) {
            ProvisioningReport result = new ProvisioningReport();
            result.setOperation(ResourceOperation.NONE);
            result.setAnyType(realm == null ? null : REALM_TYPE);
            result.setStatus(ProvisioningReport.Status.IGNORE);
            result.setKey(realmKey);
            profile.getResults().add(result);

            LOG.warn("Ignoring during push", e);
            return true;
        } catch (JobExecutionException e) {
            LOG.error("Push failed", e);
            return false;
        }
    }

    private Realm update(final RealmTO realmTO) {
        Realm realm = realmDAO.findByFullPath(realmTO.getFullPath());
        PropagationByResource propByRes = binder.update(realm, realmTO);
        realm = realmDAO.save(realm);

        List<PropagationTask> tasks = propagationManager.createTasks(realm, propByRes, null);
        taskExecutor.execute(tasks, false);

        return realm;
    }

    private void deprovision(final Realm realm) {
        List<String> noPropResources = new ArrayList<>(realm.getResourceKeys());
        noPropResources.remove(profile.getTask().getResource().getKey());

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.addAll(ResourceOperation.DELETE, realm.getResourceKeys());

        taskExecutor.execute(propagationManager.createTasks(realm, propByRes, noPropResources));
    }

    private void provision(final Realm realm) {
        List<String> noPropResources = new ArrayList<>(realm.getResourceKeys());
        noPropResources.remove(profile.getTask().getResource().getKey());

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.add(ResourceOperation.CREATE, profile.getTask().getResource().getKey());

        taskExecutor.execute(propagationManager.createTasks(realm, propByRes, noPropResources));
    }

    private void link(final Realm realm, final Boolean unlink) {
        RealmTO realmTO = binder.getRealmTO(realm, true);
        if (unlink) {
            realmTO.getResources().remove(profile.getTask().getResource().getKey());
        } else {
            realmTO.getResources().add(profile.getTask().getResource().getKey());
        }

        update(realmTO);
    }

    private void unassign(final Realm realm) {
        RealmTO realmTO = binder.getRealmTO(realm, true);
        realmTO.getResources().remove(profile.getTask().getResource().getKey());

        deprovision(update(realmTO));
    }

    private void assign(final Realm realm) {
        RealmTO realmTO = binder.getRealmTO(realm, true);
        realmTO.getResources().add(profile.getTask().getResource().getKey());

        provision(update(realmTO));
    }

    private void doHandle(final Realm realm) throws JobExecutionException {
        ProvisioningReport result = new ProvisioningReport();
        profile.getResults().add(result);

        result.setKey(realm.getKey());
        result.setAnyType(REALM_TYPE);
        result.setName(realm.getFullPath());

        LOG.debug("Propagating Realm with key {} towards {}", realm.getKey(), profile.getTask().getResource());

        Object output = null;
        Result resultStatus = null;
        String operation = null;

        // Try to read remote object BEFORE any actual operation
        ConnectorObject beforeObj = getRemoteObject(
                realm.getName(),
                profile.getConnector(),
                profile.getTask().getResource().getOrgUnit());

        if (profile.isDryRun()) {
            if (beforeObj == null) {
                result.setOperation(getResourceOperation(profile.getTask().getUnmatchingRule()));
            } else {
                result.setOperation(getResourceOperation(profile.getTask().getMatchingRule()));
            }
            result.setStatus(ProvisioningReport.Status.SUCCESS);
        } else {
            try {
                if (beforeObj == null) {
                    operation = UnmatchingRule.toEventName(profile.getTask().getUnmatchingRule());
                    result.setOperation(getResourceOperation(profile.getTask().getUnmatchingRule()));

                    switch (profile.getTask().getUnmatchingRule()) {
                        case ASSIGN:
                            for (PushActions action : profile.getActions()) {
                                action.beforeAssign(profile, realm);
                            }

                            if (!profile.getTask().isPerformCreate()) {
                                LOG.debug("PushTask not configured for create");
                            } else {
                                assign(realm);
                            }

                            break;

                        case PROVISION:
                            for (PushActions action : profile.getActions()) {
                                action.beforeProvision(profile, realm);
                            }

                            if (!profile.getTask().isPerformCreate()) {
                                LOG.debug("PushTask not configured for create");
                            } else {
                                provision(realm);
                            }

                            break;

                        case UNLINK:
                            for (PushActions action : profile.getActions()) {
                                action.beforeUnlink(profile, realm);
                            }

                            if (!profile.getTask().isPerformUpdate()) {
                                LOG.debug("PushTask not configured for update");
                            } else {
                                link(realm, true);
                            }

                            break;

                        case IGNORE:
                            LOG.debug("Ignored any: {}", realm);
                            break;
                        default:
                        // do nothing
                    }
                } else {
                    operation = MatchingRule.toEventName(profile.getTask().getMatchingRule());
                    result.setOperation(getResourceOperation(profile.getTask().getMatchingRule()));

                    switch (profile.getTask().getMatchingRule()) {
                        case UPDATE:
                            for (PushActions action : profile.getActions()) {
                                action.beforeUpdate(profile, realm);
                            }
                            if (!profile.getTask().isPerformUpdate()) {
                                LOG.debug("PushTask not configured for update");
                            } else {
                                update(binder.getRealmTO(realm, true));
                            }

                            break;

                        case DEPROVISION:
                            for (PushActions action : profile.getActions()) {
                                action.beforeDeprovision(profile, realm);
                            }

                            if (!profile.getTask().isPerformDelete()) {
                                LOG.debug("PushTask not configured for delete");
                            } else {
                                deprovision(realm);
                            }

                            break;

                        case UNASSIGN:
                            for (PushActions action : profile.getActions()) {
                                action.beforeUnassign(profile, realm);
                            }

                            if (!profile.getTask().isPerformDelete()) {
                                LOG.debug("PushTask not configured for delete");
                            } else {
                                unassign(realm);
                            }

                            break;

                        case LINK:
                            for (PushActions action : profile.getActions()) {
                                action.beforeLink(profile, realm);
                            }

                            if (!profile.getTask().isPerformUpdate()) {
                                LOG.debug("PushTask not configured for update");
                            } else {
                                link(realm, false);
                            }

                            break;

                        case UNLINK:
                            for (PushActions action : profile.getActions()) {
                                action.beforeUnlink(profile, realm);
                            }

                            if (!profile.getTask().isPerformUpdate()) {
                                LOG.debug("PushTask not configured for update");
                            } else {
                                link(realm, true);
                            }

                            break;

                        case IGNORE:
                            LOG.debug("Ignored any: {}", realm);
                            break;
                        default:
                        // do nothing
                    }
                }

                for (PushActions action : profile.getActions()) {
                    action.after(profile, realm, result);
                }

                result.setStatus(ProvisioningReport.Status.SUCCESS);
                resultStatus = AuditElements.Result.SUCCESS;
                output = getRemoteObject(
                        realm.getName(),
                        profile.getConnector(),
                        profile.getTask().getResource().getOrgUnit());
            } catch (IgnoreProvisionException e) {
                throw e;
            } catch (Exception e) {
                result.setStatus(ProvisioningReport.Status.FAILURE);
                result.setMessage(ExceptionUtils.getRootCauseMessage(e));
                resultStatus = AuditElements.Result.FAILURE;
                output = e;

                LOG.warn("Error pushing {} towards {}", realm, profile.getTask().getResource(), e);

                for (PushActions action : profile.getActions()) {
                    action.onError(profile, realm, result, e);
                }

                throw new JobExecutionException(e);
            } finally {
                notificationManager.createTasks(AuditElements.EventCategoryType.PUSH,
                        REALM_TYPE.toLowerCase(),
                        profile.getTask().getResource().getKey(),
                        operation,
                        resultStatus,
                        beforeObj,
                        output,
                        realm);
                auditManager.audit(AuditElements.EventCategoryType.PUSH,
                        REALM_TYPE.toLowerCase(),
                        profile.getTask().getResource().getKey(),
                        operation,
                        resultStatus,
                        beforeObj,
                        output,
                        realm);
            }
        }
    }

    private ResourceOperation getResourceOperation(final UnmatchingRule rule) {
        switch (rule) {
            case ASSIGN:
            case PROVISION:
                return ResourceOperation.CREATE;
            default:
                return ResourceOperation.NONE;
        }
    }

    private ResourceOperation getResourceOperation(final MatchingRule rule) {
        switch (rule) {
            case UPDATE:
                return ResourceOperation.UPDATE;
            case DEPROVISION:
            case UNASSIGN:
                return ResourceOperation.DELETE;
            default:
                return ResourceOperation.NONE;
        }
    }

    /**
     * Get remote object for given realm .
     *
     * @param connector connector facade proxy.
     * @param task current propagation task.
     * @param orgUnit orgUnit
     * @return remote connector object.
     */
    private ConnectorObject getRemoteObject(
            final String realmName,
            final Connector connector,
            final OrgUnit orgUnit) {

        final ConnectorObject[] obj = new ConnectorObject[1];
        try {
            connector.search(orgUnit.getObjectClass(),
                    new EqualsFilter(AttributeBuilder.build(orgUnit.getExtAttrName(), realmName)),
                    new ResultsHandler() {

                @Override
                public boolean handle(final ConnectorObject connectorObject) {
                    obj[0] = connectorObject;
                    return false;
                }
            }, MappingUtils.buildOperationOptions(orgUnit));
        } catch (TimeoutException toe) {
            LOG.debug("Request timeout", toe);
            throw toe;
        } catch (RuntimeException ignore) {
            LOG.debug("While resolving {}", realmName, ignore);
        }

        return obj[0];
    }
}

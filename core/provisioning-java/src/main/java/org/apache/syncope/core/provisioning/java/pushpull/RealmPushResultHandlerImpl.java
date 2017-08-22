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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.Result;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.resource.Item;
import org.apache.syncope.core.persistence.api.entity.resource.OrgUnit;
import org.apache.syncope.core.persistence.api.entity.resource.OrgUnitItem;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.TimeoutException;
import org.apache.syncope.core.provisioning.api.event.AfterHandlingEvent;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.pushpull.IgnoreProvisionException;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningReport;
import org.apache.syncope.core.provisioning.api.pushpull.PushActions;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePushResultHandler;
import org.apache.syncope.core.provisioning.java.job.AfterHandlingJob;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class RealmPushResultHandlerImpl
        extends AbstractRealmResultHandler<PushTask, PushActions>
        implements SyncopePushResultHandler {

    @Autowired
    private MappingManager mappingManager;

    @Autowired
    private SchedulerFactoryBean scheduler;

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

    private void reportPropagation(final ProvisioningReport result, final PropagationReporter reporter) {
        if (!reporter.getStatuses().isEmpty()) {
            result.setStatus(toProvisioningReportStatus(reporter.getStatuses().get(0).getStatus()));
            result.setMessage(reporter.getStatuses().get(0).getFailureReason());
        }
    }

    private Realm update(final RealmTO realmTO, final ProvisioningReport result) {
        Realm realm = realmDAO.findByFullPath(realmTO.getFullPath());
        PropagationByResource propByRes = binder.update(realm, realmTO);
        realm = realmDAO.save(realm);

        PropagationReporter reporter = taskExecutor.execute(
                propagationManager.createTasks(realm, propByRes, null), false);
        reportPropagation(result, reporter);

        return realm;
    }

    private void deprovision(final Realm realm, final ProvisioningReport result) {
        List<String> noPropResources = new ArrayList<>(realm.getResourceKeys());
        noPropResources.remove(profile.getTask().getResource().getKey());

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.addAll(ResourceOperation.DELETE, realm.getResourceKeys());

        PropagationReporter reporter = taskExecutor.execute(
                propagationManager.createTasks(realm, propByRes, noPropResources), false);
        reportPropagation(result, reporter);
    }

    private void provision(final Realm realm, final ProvisioningReport result) {
        List<String> noPropResources = new ArrayList<>(realm.getResourceKeys());
        noPropResources.remove(profile.getTask().getResource().getKey());

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.add(ResourceOperation.CREATE, profile.getTask().getResource().getKey());

        PropagationReporter reporter = taskExecutor.execute(
                propagationManager.createTasks(realm, propByRes, noPropResources), false);
        reportPropagation(result, reporter);
    }

    private void link(final Realm realm, final boolean unlink, final ProvisioningReport result) {
        RealmTO realmTO = binder.getRealmTO(realm, true);
        if (unlink) {
            realmTO.getResources().remove(profile.getTask().getResource().getKey());
        } else {
            realmTO.getResources().add(profile.getTask().getResource().getKey());
        }

        update(realmTO, result);
    }

    private void unassign(final Realm realm, final ProvisioningReport result) {
        RealmTO realmTO = binder.getRealmTO(realm, true);
        realmTO.getResources().remove(profile.getTask().getResource().getKey());

        deprovision(update(realmTO, result), result);
    }

    private void assign(final Realm realm, final ProvisioningReport result) {
        RealmTO realmTO = binder.getRealmTO(realm, true);
        realmTO.getResources().add(profile.getTask().getResource().getKey());

        provision(update(realmTO, result), result);
    }

    protected ConnectorObject getRemoteObject(
            final ObjectClass objectClass,
            final String connObjectKey,
            final String connObjectKeyValue,
            final Iterator<? extends Item> iterator) {

        ConnectorObject obj = null;
        try {
            obj = profile.getConnector().getObject(
                    objectClass,
                    AttributeBuilder.build(connObjectKey, connObjectKeyValue),
                    MappingUtils.buildOperationOptions(iterator));
        } catch (TimeoutException toe) {
            LOG.debug("Request timeout", toe);
            throw toe;
        } catch (RuntimeException ignore) {
            LOG.debug("While resolving {}", connObjectKeyValue, ignore);
        }

        return obj;
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

        // Try to read remote object BEFORE any actual operation
        OrgUnit orgUnit = profile.getTask().getResource().getOrgUnit();
        Optional<? extends OrgUnitItem> connObjectKey = orgUnit.getConnObjectKeyItem();
        String connObjecKeyValue = mappingManager.getConnObjectKeyValue(realm, orgUnit);

        ConnectorObject beforeObj = getRemoteObject(
                orgUnit.getObjectClass(),
                connObjectKey.get().getExtAttrName(),
                connObjecKeyValue,
                orgUnit.getItems().iterator());

        if (profile.isDryRun()) {
            if (beforeObj == null) {
                result.setOperation(toResourceOperation(profile.getTask().getUnmatchingRule()));
            } else {
                result.setOperation(toResourceOperation(profile.getTask().getMatchingRule()));
            }
            result.setStatus(ProvisioningReport.Status.SUCCESS);
        } else {
            String operation = beforeObj == null
                    ? UnmatchingRule.toEventName(profile.getTask().getUnmatchingRule())
                    : MatchingRule.toEventName(profile.getTask().getMatchingRule());

            boolean notificationsAvailable = notificationManager.notificationsAvailable(
                    AuditElements.EventCategoryType.PUSH,
                    REALM_TYPE.toLowerCase(),
                    profile.getTask().getResource().getKey(),
                    operation);
            boolean auditRequested = auditManager.auditRequested(
                    AuditElements.EventCategoryType.PUSH,
                    REALM_TYPE.toLowerCase(),
                    profile.getTask().getResource().getKey(),
                    operation);
            try {
                if (beforeObj == null) {
                    result.setOperation(toResourceOperation(profile.getTask().getUnmatchingRule()));

                    switch (profile.getTask().getUnmatchingRule()) {
                        case ASSIGN:
                            for (PushActions action : profile.getActions()) {
                                action.beforeAssign(profile, realm);
                            }

                            if (!profile.getTask().isPerformCreate()) {
                                LOG.debug("PushTask not configured for create");
                                result.setStatus(ProvisioningReport.Status.IGNORE);
                            } else {
                                assign(realm, result);
                            }

                            break;

                        case PROVISION:
                            for (PushActions action : profile.getActions()) {
                                action.beforeProvision(profile, realm);
                            }

                            if (!profile.getTask().isPerformCreate()) {
                                LOG.debug("PushTask not configured for create");
                                result.setStatus(ProvisioningReport.Status.IGNORE);
                            } else {
                                provision(realm, result);
                            }

                            break;

                        case UNLINK:
                            for (PushActions action : profile.getActions()) {
                                action.beforeUnlink(profile, realm);
                            }

                            if (!profile.getTask().isPerformUpdate()) {
                                LOG.debug("PushTask not configured for update");
                                result.setStatus(ProvisioningReport.Status.IGNORE);
                            } else {
                                link(realm, true, result);
                            }

                            break;

                        case IGNORE:
                            LOG.debug("Ignored any: {}", realm);
                            result.setStatus(ProvisioningReport.Status.IGNORE);
                            break;

                        default:
                        // do nothing
                    }
                } else {
                    result.setOperation(toResourceOperation(profile.getTask().getMatchingRule()));

                    switch (profile.getTask().getMatchingRule()) {
                        case UPDATE:
                            for (PushActions action : profile.getActions()) {
                                action.beforeUpdate(profile, realm);
                            }
                            if (!profile.getTask().isPerformUpdate()) {
                                LOG.debug("PushTask not configured for update");
                                result.setStatus(ProvisioningReport.Status.IGNORE);
                            } else {
                                update(binder.getRealmTO(realm, true), result);
                            }

                            break;

                        case DEPROVISION:
                            for (PushActions action : profile.getActions()) {
                                action.beforeDeprovision(profile, realm);
                            }

                            if (!profile.getTask().isPerformDelete()) {
                                LOG.debug("PushTask not configured for delete");
                                result.setStatus(ProvisioningReport.Status.IGNORE);
                            } else {
                                deprovision(realm, result);
                            }

                            break;

                        case UNASSIGN:
                            for (PushActions action : profile.getActions()) {
                                action.beforeUnassign(profile, realm);
                            }

                            if (!profile.getTask().isPerformDelete()) {
                                LOG.debug("PushTask not configured for delete");
                                result.setStatus(ProvisioningReport.Status.IGNORE);
                            } else {
                                unassign(realm, result);
                            }

                            break;

                        case LINK:
                            for (PushActions action : profile.getActions()) {
                                action.beforeLink(profile, realm);
                            }

                            if (!profile.getTask().isPerformUpdate()) {
                                LOG.debug("PushTask not configured for update");
                                result.setStatus(ProvisioningReport.Status.IGNORE);
                            } else {
                                link(realm, false, result);
                            }

                            break;

                        case UNLINK:
                            for (PushActions action : profile.getActions()) {
                                action.beforeUnlink(profile, realm);
                            }

                            if (!profile.getTask().isPerformUpdate()) {
                                LOG.debug("PushTask not configured for update");
                                result.setStatus(ProvisioningReport.Status.IGNORE);
                            } else {
                                link(realm, true, result);
                            }

                            break;

                        case IGNORE:
                            LOG.debug("Ignored any: {}", realm);
                            result.setStatus(ProvisioningReport.Status.IGNORE);
                            break;

                        default:
                        // do nothing
                    }
                }

                for (PushActions action : profile.getActions()) {
                    action.after(profile, realm, result);
                }

                if (result.getStatus() == null) {
                    result.setStatus(ProvisioningReport.Status.SUCCESS);
                }
                resultStatus = AuditElements.Result.SUCCESS;
                output = getRemoteObject(
                        orgUnit.getObjectClass(),
                        connObjectKey.get().getExtAttrName(),
                        connObjecKeyValue,
                        orgUnit.getItems().iterator());
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
                if (notificationsAvailable || auditRequested) {
                    Map<String, Object> jobMap = new HashMap<>();
                    jobMap.put(AfterHandlingEvent.JOBMAP_KEY, new AfterHandlingEvent(
                            AuditElements.EventCategoryType.PUSH,
                            REALM_TYPE.toLowerCase(),
                            profile.getTask().getResource().getKey(),
                            operation,
                            resultStatus,
                            beforeObj,
                            output,
                            realm));
                    AfterHandlingJob.schedule(scheduler, jobMap);
                }
            }
        }
    }

    private ResourceOperation toResourceOperation(final UnmatchingRule rule) {
        switch (rule) {
            case ASSIGN:
            case PROVISION:
                return ResourceOperation.CREATE;
            default:
                return ResourceOperation.NONE;
        }
    }

    private ResourceOperation toResourceOperation(final MatchingRule rule) {
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

    private ProvisioningReport.Status toProvisioningReportStatus(final PropagationTaskExecStatus status) {
        switch (status) {
            case FAILURE:
                return ProvisioningReport.Status.FAILURE;

            case SUCCESS:
                return ProvisioningReport.Status.SUCCESS;

            case CREATED:
            case NOT_ATTEMPTED:
            default:
                return ProvisioningReport.Status.IGNORE;
        }
    }
}

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
package org.apache.syncope.core.provisioning.java.propagation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.collections.IteratorChain;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.Result;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.ConnectorFactory;
import org.apache.syncope.core.provisioning.api.TimeoutException;
import org.apache.syncope.core.provisioning.api.propagation.PropagationActions;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.java.utils.ConnObjectUtils;
import org.apache.syncope.core.provisioning.api.utils.ExceptionUtils2;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.OrgUnit;
import org.apache.syncope.core.persistence.api.entity.resource.OrgUnitItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCache;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCacheValue;
import org.apache.syncope.core.provisioning.api.data.TaskDataBinder;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationException;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.apache.syncope.core.spring.ImplementationManager;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = { Throwable.class })
public abstract class AbstractPropagationTaskExecutor implements PropagationTaskExecutor {

    protected static final Logger LOG = LoggerFactory.getLogger(PropagationTaskExecutor.class);

    /**
     * Connector factory.
     */
    @Autowired
    protected ConnectorFactory connFactory;

    /**
     * ConnObjectUtils.
     */
    @Autowired
    protected ConnObjectUtils connObjectUtils;

    /**
     * User DAO.
     */
    @Autowired
    protected UserDAO userDAO;

    /**
     * Group DAO.
     */
    @Autowired
    protected GroupDAO groupDAO;

    /**
     * Any object DAO.
     */
    @Autowired
    protected AnyObjectDAO anyObjectDAO;

    /**
     * Task DAO.
     */
    @Autowired
    protected TaskDAO taskDAO;

    @Autowired
    protected ExternalResourceDAO resourceDAO;

    @Autowired
    protected VirSchemaDAO virSchemaDAO;

    /**
     * Notification Manager.
     */
    @Autowired
    protected NotificationManager notificationManager;

    /**
     * Audit Manager.
     */
    @Autowired
    protected AuditManager auditManager;

    /**
     * Task data binder.
     */
    @Autowired
    protected TaskDataBinder taskDataBinder;

    @Autowired
    protected AnyUtilsFactory anyUtilsFactory;

    @Autowired
    protected TaskUtilsFactory taskUtilsFactory;

    @Autowired
    protected EntityFactory entityFactory;

    @Autowired
    protected VirAttrCache virAttrCache;

    protected List<PropagationActions> getPropagationActions(final ExternalResource resource) {
        List<PropagationActions> result = new ArrayList<>();

        resource.getPropagationActions().forEach(impl -> {
            try {
                result.add(ImplementationManager.build(impl));
            } catch (Exception e) {
                LOG.error("While building {}", impl, e);
            }
        });

        return result;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected Uid createOrUpdate(
            final PropagationTask task,
            final ConnectorObject beforeObj,
            final Connector connector,
            final AtomicReference<Boolean> propagationAttempted) {

        // set of attributes to be propagated
        Set<Attribute> attributes = new HashSet<>(task.getAttributes());

        // check if there is any missing or null / empty mandatory attribute
        Set<Object> mandatoryAttrNames = new HashSet<>();
        Attribute mandatoryMissing = AttributeUtil.find(MANDATORY_MISSING_ATTR_NAME, task.getAttributes());
        if (mandatoryMissing != null) {
            attributes.remove(mandatoryMissing);

            if (beforeObj == null) {
                mandatoryAttrNames.addAll(mandatoryMissing.getValue());
            }
        }
        Attribute mandatoryNullOrEmpty = AttributeUtil.find(MANDATORY_NULL_OR_EMPTY_ATTR_NAME, task.getAttributes());
        if (mandatoryNullOrEmpty != null) {
            attributes.remove(mandatoryNullOrEmpty);

            mandatoryAttrNames.addAll(mandatoryNullOrEmpty.getValue());
        }
        if (!mandatoryAttrNames.isEmpty()) {
            throw new IllegalArgumentException(
                    "Not attempted because there are mandatory attributes without value(s): " + mandatoryAttrNames);
        }

        Uid result;
        if (beforeObj == null) {
            LOG.debug("Create {} on {}", attributes, task.getResource().getKey());
            result = connector.create(
                    new ObjectClass(task.getObjectClassName()), attributes, null, propagationAttempted);

            task.getResource().getProvision(task.getAnyType()).ifPresent(provision -> {
                if (provision.getUidOnCreate() != null) {
                    anyUtilsFactory.getInstance(task.getAnyTypeKind()).
                            addAttr(task.getEntityKey(), provision.getUidOnCreate(), result.getUidValue());
                }
            });
        } else {
            // 1. check if rename is really required
            Name newName = AttributeUtil.getNameFromAttributes(attributes);

            LOG.debug("Rename required with value {}", newName);

            if (newName != null && newName.equals(beforeObj.getName())
                    && !newName.getNameValue().equals(beforeObj.getUid().getUidValue())) {

                LOG.debug("Remote object name unchanged");
                attributes.remove(newName);
            }

            // 2. check wether anything is actually needing to be propagated, i.e. if there is attribute
            // difference between beforeObj - just read above from the connector - and the values to be propagated
            Map<String, Attribute> originalAttrMap = beforeObj.getAttributes().stream().
                    collect(Collectors.toMap(attr -> attr.getName().toUpperCase(), attr -> attr));
            Map<String, Attribute> updateAttrMap = attributes.stream().
                    collect(Collectors.toMap(attr -> attr.getName().toUpperCase(), attr -> attr));

            // Only compare attribute from beforeObj that are also being updated
            Set<String> skipAttrNames = originalAttrMap.keySet();
            skipAttrNames.removeAll(updateAttrMap.keySet());
            new HashSet<>(skipAttrNames).forEach(attrName -> {
                originalAttrMap.remove(attrName);
            });

            Set<Attribute> originalAttrs = new HashSet<>(originalAttrMap.values());

            if (originalAttrs.equals(attributes)) {
                LOG.debug("Don't need to propagate anything: {} is equal to {}", originalAttrs, attributes);
                result = AttributeUtil.getUidAttribute(attributes);
            } else {
                LOG.debug("Attributes that would be updated {}", attributes);

                Set<Attribute> strictlyModified = new HashSet<>();
                attributes.stream().filter(attr -> (!originalAttrs.contains(attr))).
                        forEachOrdered(attr -> {
                            strictlyModified.add(attr);
                        });

                // 3. provision entry
                LOG.debug("Update {} on {}", strictlyModified, task.getResource().getKey());

                result = connector.update(
                        beforeObj.getObjectClass(),
                        new Uid(beforeObj.getUid().getUidValue()),
                        strictlyModified,
                        null,
                        propagationAttempted);
            }
        }

        return result;
    }

    protected Uid delete(
            final PropagationTask task,
            final ConnectorObject beforeObj,
            final Connector connector,
            final AtomicReference<Boolean> propagationAttempted) {

        Uid result;
        if (beforeObj == null) {
            LOG.debug("{} not found on external resource: ignoring delete", task.getConnObjectKey());
            result = null;
        } else {
            /*
             * We must choose here whether to
             * a. actually delete the provided entity from the external resource
             * b. just update the provided entity data onto the external resource
             *
             * (a) happens when either there is no entity associated with the PropagationTask (this takes place
             * when the task is generated via Logic's delete()) or the provided updated
             * entity hasn't the current resource assigned (when the task is generated via
             * Logic's update()).
             *
             * (b) happens when the provided updated entity does have the current resource assigned (when the task
             * is generated via Logic's update()): this basically means that before such
             * update, this entity used to have the current resource assigned by more than one mean (for example,
             * two different memberships with the same resource).
             */
            Collection<String> resources = Collections.emptySet();
            if (task.getEntityKey() != null && task.getAnyTypeKind() != null) {
                switch (task.getAnyTypeKind()) {
                    case USER:
                        try {
                            resources = userDAO.findAllResourceKeys(task.getEntityKey());
                        } catch (Exception e) {
                            LOG.error("Could not read user {}", task.getEntityKey(), e);
                        }
                        break;

                    case GROUP:
                        try {
                            resources = groupDAO.findAllResourceKeys(task.getEntityKey());
                        } catch (Exception e) {
                            LOG.error("Could not read group {}", task.getEntityKey(), e);
                        }
                        break;

                    case ANY_OBJECT:
                    default:
                        try {
                            resources = anyObjectDAO.findAllResourceKeys(task.getEntityKey());
                        } catch (Exception e) {
                            LOG.error("Could not read any object {}", task.getEntityKey(), e);
                        }
                        break;
                }
            }
            if (task.getAnyTypeKind() == null || !resources.contains(task.getResource().getKey())) {
                LOG.debug("Delete {} on {}", beforeObj.getUid(), task.getResource().getKey());

                connector.delete(beforeObj.getObjectClass(), beforeObj.getUid(), null, propagationAttempted);
                result = beforeObj.getUid();
            } else {
                result = createOrUpdate(task, beforeObj, connector, propagationAttempted);
            }
        }

        return result;
    }

    @Override
    public TaskExec execute(final PropagationTaskInfo taskInfo) {
        return execute(taskInfo, null);
    }

    @Override
    public TaskExec execute(final PropagationTaskInfo taskInfo, final PropagationReporter reporter) {
        PropagationTask task;
        if (taskInfo.getKey() == null) {
            task = entityFactory.newEntity(PropagationTask.class);
            task.setResource(resourceDAO.find(taskInfo.getResource()));
            task.setObjectClassName(taskInfo.getObjectClassName());
            task.setAnyTypeKind(taskInfo.getAnyTypeKind());
            task.setAnyType(taskInfo.getAnyType());
            task.setEntityKey(taskInfo.getEntityKey());
            task.setOperation(taskInfo.getOperation());
            task.setConnObjectKey(taskInfo.getConnObjectKey());
            task.setOldConnObjectKey(taskInfo.getOldConnObjectKey());
        } else {
            task = taskDAO.find(taskInfo.getKey());
        }
        Set<Attribute> attributes = new HashSet<>();
        if (StringUtils.isNotBlank(taskInfo.getAttributes())) {
            attributes.addAll(List.of(POJOHelper.deserialize(taskInfo.getAttributes(), Attribute[].class)));
        }
        task.setAttributes(attributes);

        List<PropagationActions> actions = getPropagationActions(task.getResource());

        String resource = task.getResource().getKey();

        Date start = new Date();

        TaskExec execution = entityFactory.newEntity(TaskExec.class);
        execution.setStatus(ExecStatus.CREATED.name());

        String taskExecutionMessage = null;
        String failureReason = null;

        // Flag to state whether any propagation has been attempted
        AtomicReference<Boolean> propagationAttempted = new AtomicReference<>(false);

        ConnectorObject beforeObj = null;
        ConnectorObject afterObj = null;

        Provision provision = null;
        OrgUnit orgUnit = null;
        Uid uid = null;
        Connector connector = null;
        Result result;
        try {
            provision = task.getResource().getProvision(new ObjectClass(task.getObjectClassName())).orElse(null);
            orgUnit = task.getResource().getOrgUnit();
            connector = connFactory.getConnector(task.getResource());

            if (taskInfo.getBeforeObj() == null) {
                // Try to read remote object BEFORE any actual operation
                beforeObj = provision == null && orgUnit == null
                        ? null
                        : orgUnit == null
                                ? getRemoteObject(task, connector, provision, false)
                                : getRemoteObject(task, connector, orgUnit, false);
            } else if (taskInfo.getBeforeObj().isPresent()) {
                beforeObj = taskInfo.getBeforeObj().get();
            }

            for (PropagationActions action : actions) {
                action.before(task, beforeObj);
            }

            switch (task.getOperation()) {
                case CREATE:
                case UPDATE:
                    uid = createOrUpdate(task, beforeObj, connector, propagationAttempted);
                    break;

                case DELETE:
                    uid = delete(task, beforeObj, connector, propagationAttempted);
                    break;

                default:
            }

            execution.setStatus(propagationAttempted.get()
                    ? ExecStatus.SUCCESS.name()
                    : ExecStatus.NOT_ATTEMPTED.name());

            LOG.debug("Successfully propagated to {}", task.getResource());
            result = Result.SUCCESS;
        } catch (Exception e) {
            result = Result.FAILURE;
            LOG.error("Exception during provision on resource " + resource, e);

            if (e instanceof ConnectorException && e.getCause() != null) {
                taskExecutionMessage = e.getCause().getMessage();
                if (e.getCause().getMessage() == null) {
                    failureReason = e.getMessage();
                } else {
                    failureReason = e.getMessage() + "\n\n Cause: " + e.getCause().getMessage().split("\n")[0];
                }
            } else {
                taskExecutionMessage = ExceptionUtils2.getFullStackTrace(e);
                if (e.getCause() == null) {
                    failureReason = e.getMessage();
                } else {
                    failureReason = e.getMessage() + "\n\n Cause: " + e.getCause().getMessage().split("\n")[0];
                }
            }

            try {
                execution.setStatus(ExecStatus.FAILURE.name());
            } catch (Exception wft) {
                LOG.error("While executing KO action on {}", execution, wft);
            }

            propagationAttempted.set(true);

            actions.forEach(action -> {
                action.onError(task, execution, e);
            });
        } finally {
            // Try to read remote object AFTER any actual operation
            if (connector != null) {
                if (uid != null) {
                    task.setConnObjectKey(uid.getUidValue());
                }
                try {
                    afterObj = provision == null && orgUnit == null
                            ? null
                            : orgUnit == null
                                    ? getRemoteObject(task, connector, provision, true)
                                    : getRemoteObject(task, connector, orgUnit, true);
                } catch (Exception ignore) {
                    // ignore exception
                    LOG.error("Error retrieving after object", ignore);
                }
            }

            if (task.getOperation() != ResourceOperation.DELETE && afterObj == null && uid != null) {
                afterObj = new ConnectorObjectBuilder().
                        setObjectClass(new ObjectClass(task.getObjectClassName())).
                        setUid(uid).
                        setName(AttributeUtil.getNameFromAttributes(task.getAttributes())).
                        build();
            }

            execution.setStart(start);
            execution.setMessage(taskExecutionMessage);
            execution.setEnd(new Date());

            LOG.debug("Execution finished: {}", execution);

            if (hasToBeregistered(task, execution)) {
                LOG.debug("Execution to be stored: {}", execution);

                execution.setTask(task);
                task.add(execution);

                taskDAO.save(task);
            }

            if (reporter != null) {
                reporter.onSuccessOrNonPriorityResourceFailures(taskInfo,
                        ExecStatus.valueOf(execution.getStatus()),
                        failureReason,
                        beforeObj,
                        afterObj);
            }
        }

        for (PropagationActions action : actions) {
            action.after(task, execution, afterObj);
        }
        // SYNCOPE-1136
        String anyTypeKind = task.getAnyTypeKind() == null ? "realm" : task.getAnyTypeKind().name().toLowerCase();
        String operation = task.getOperation().name().toLowerCase();
        boolean notificationsAvailable = notificationManager.notificationsAvailable(
                AuditElements.EventCategoryType.PROPAGATION, anyTypeKind, resource, operation);
        boolean auditRequested = auditManager.auditRequested(
                AuthContextUtils.getUsername(),
                AuditElements.EventCategoryType.PROPAGATION,
                anyTypeKind,
                resource,
                operation);

        if (notificationsAvailable || auditRequested) {
            ExecTO execTO = taskDataBinder.getExecTO(execution);
            notificationManager.createTasks(
                    AuthContextUtils.getUsername(),
                    AuditElements.EventCategoryType.PROPAGATION,
                    anyTypeKind,
                    resource,
                    operation,
                    result,
                    beforeObj,
                    new Object[] { execTO, afterObj },
                    taskInfo);

            auditManager.audit(
                    AuthContextUtils.getUsername(),
                    AuditElements.EventCategoryType.PROPAGATION,
                    anyTypeKind,
                    resource,
                    operation,
                    result,
                    beforeObj,
                    new Object[] { execTO, afterObj },
                    taskInfo);
        }

        return execution;
    }

    protected abstract void doExecute(
            Collection<PropagationTaskInfo> taskInfos, PropagationReporter reporter, boolean nullPriorityAsync);

    @Override
    public PropagationReporter execute(
            final Collection<PropagationTaskInfo> taskInfos,
            final boolean nullPriorityAsync) {

        PropagationReporter reporter = new DefaultPropagationReporter();
        try {
            doExecute(taskInfos, reporter, nullPriorityAsync);
        } catch (PropagationException e) {
            LOG.error("Error propagation priority resource", e);
            reporter.onPriorityResourceFailure(e.getResourceName(), taskInfos);
        }

        return reporter;
    }

    /**
     * Check whether an execution has to be stored, for a given task.
     *
     * @param task propagation task
     * @param execution to be decide whether to store or not
     * @return true if execution has to be store, false otherwise
     */
    protected boolean hasToBeregistered(final PropagationTask task, final TaskExec execution) {
        boolean result;

        boolean failed = ExecStatus.valueOf(execution.getStatus()) != ExecStatus.SUCCESS;

        switch (task.getOperation()) {

            case CREATE:
                result = (failed && task.getResource().getCreateTraceLevel().ordinal() >= TraceLevel.FAILURES.ordinal())
                        || task.getResource().getCreateTraceLevel() == TraceLevel.ALL;
                break;

            case UPDATE:
                result = (failed && task.getResource().getUpdateTraceLevel().ordinal() >= TraceLevel.FAILURES.ordinal())
                        || task.getResource().getUpdateTraceLevel() == TraceLevel.ALL;
                break;

            case DELETE:
                result = (failed && task.getResource().getDeleteTraceLevel().ordinal() >= TraceLevel.FAILURES.ordinal())
                        || task.getResource().getDeleteTraceLevel() == TraceLevel.ALL;
                break;

            default:
                result = false;
        }

        return result;
    }

    /**
     * Get remote object for given task.
     *
     * @param connector connector facade proxy.
     * @param task current propagation task.
     * @param provision provision
     * @param latest 'FALSE' to retrieve object using old connObjectKey if not null.
     * @return remote connector object.
     */
    protected ConnectorObject getRemoteObject(
            final PropagationTask task,
            final Connector connector,
            final Provision provision,
            final boolean latest) {

        String connObjectKey = latest || task.getOldConnObjectKey() == null
                ? task.getConnObjectKey()
                : task.getOldConnObjectKey();

        Set<MappingItem> linkingMappingItems = virSchemaDAO.findByProvision(provision).stream().
                map(schema -> schema.asLinkingMappingItem()).collect(Collectors.toSet());

        ConnectorObject obj = null;
        Optional<? extends MappingItem> connObjectKeyItem = MappingUtils.getConnObjectKeyItem(provision);
        if (connObjectKeyItem.isPresent()) {
            try {
                obj = connector.getObject(
                        new ObjectClass(task.getObjectClassName()),
                        AttributeBuilder.build(connObjectKeyItem.get().getExtAttrName(), connObjectKey),
                        provision.isIgnoreCaseMatch(),
                        MappingUtils.buildOperationOptions(new IteratorChain<>(
                                MappingUtils.getPropagationItems(provision.getMapping().getItems()).iterator(),
                                linkingMappingItems.iterator())));

                for (MappingItem item : linkingMappingItems) {
                    Attribute attr = obj.getAttributeByName(item.getExtAttrName());
                    if (attr == null) {
                        virAttrCache.expire(task.getAnyType(), task.getEntityKey(), item.getIntAttrName());
                    } else {
                        VirAttrCacheValue cacheValue = new VirAttrCacheValue();
                        cacheValue.setValues(attr.getValue());
                        virAttrCache.put(task.getAnyType(), task.getEntityKey(), item.getIntAttrName(), cacheValue);
                    }
                }
            } catch (TimeoutException toe) {
                LOG.debug("Request timeout", toe);
                throw toe;
            } catch (RuntimeException ignore) {
                LOG.debug("While resolving {}", connObjectKey, ignore);
            }
        }

        return obj;
    }

    /**
     * Get remote object for given task.
     *
     * @param connector connector facade proxy.
     * @param task current propagation task.
     * @param orgUnit orgUnit
     * @param latest 'FALSE' to retrieve object using old connObjectKey if not null.
     * @return remote connector object.
     */
    protected ConnectorObject getRemoteObject(
            final PropagationTask task,
            final Connector connector,
            final OrgUnit orgUnit,
            final boolean latest) {

        String connObjectKey = latest || task.getOldConnObjectKey() == null
                ? task.getConnObjectKey()
                : task.getOldConnObjectKey();

        ConnectorObject obj = null;
        Optional<? extends OrgUnitItem> connObjectKeyItem = orgUnit.getConnObjectKeyItem();
        if (connObjectKeyItem.isPresent()) {
            try {
                obj = connector.getObject(new ObjectClass(task.getObjectClassName()),
                        AttributeBuilder.build(connObjectKeyItem.get().getExtAttrName(), connObjectKey),
                        orgUnit.isIgnoreCaseMatch(),
                        MappingUtils.buildOperationOptions(
                                MappingUtils.getPropagationItems(orgUnit.getItems()).iterator()));
            } catch (TimeoutException toe) {
                LOG.debug("Request timeout", toe);
                throw toe;
            } catch (RuntimeException ignore) {
                LOG.debug("While resolving {}", connObjectKey, ignore);
            }
        }

        return obj;
    }
}

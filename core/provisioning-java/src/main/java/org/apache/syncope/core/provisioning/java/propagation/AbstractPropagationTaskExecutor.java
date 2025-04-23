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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.OrgUnit;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.policy.PropagationPolicy;
import org.apache.syncope.core.persistence.api.entity.task.PropagationData;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.apache.syncope.core.persistence.api.utils.ExceptionUtils2;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.provisioning.api.TimeoutException;
import org.apache.syncope.core.provisioning.api.data.TaskDataBinder;
import org.apache.syncope.core.provisioning.api.event.AfterHandlingEvent;
import org.apache.syncope.core.provisioning.api.event.EntityLifecycleEvent;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationActions;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.provisioning.java.pushpull.OutboundMatcher;
import org.apache.syncope.core.provisioning.java.utils.ConnObjectUtils;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.apache.syncope.core.spring.implementation.ImplementationManager;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeDelta;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.retry.RetryException;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = { Throwable.class })
public abstract class AbstractPropagationTaskExecutor implements PropagationTaskExecutor {

    protected static final Logger LOG = LoggerFactory.getLogger(PropagationTaskExecutor.class);

    protected final Map<String, RetryTemplate> retryTemplates = Collections.synchronizedMap(new HashMap<>());

    protected final ConnectorManager connectorManager;

    protected final ConnObjectUtils connObjectUtils;

    protected final TaskDAO taskDAO;

    protected final ExternalResourceDAO resourceDAO;

    protected final PlainSchemaDAO plainSchemaDAO;

    protected final NotificationManager notificationManager;

    protected final AuditManager auditManager;

    protected final TaskDataBinder taskDataBinder;

    protected final AnyUtilsFactory anyUtilsFactory;

    protected final TaskUtilsFactory taskUtilsFactory;

    protected final OutboundMatcher outboundMatcher;

    protected final PlainAttrValidationManager validator;

    protected final ApplicationEventPublisher publisher;

    protected final Map<String, PropagationActions> perContextActions = new ConcurrentHashMap<>();

    public AbstractPropagationTaskExecutor(
            final ConnectorManager connectorManager,
            final ConnObjectUtils connObjectUtils,
            final TaskDAO taskDAO,
            final ExternalResourceDAO resourceDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final NotificationManager notificationManager,
            final AuditManager auditManager,
            final TaskDataBinder taskDataBinder,
            final AnyUtilsFactory anyUtilsFactory,
            final TaskUtilsFactory taskUtilsFactory,
            final OutboundMatcher outboundMatcher,
            final PlainAttrValidationManager validator,
            final ApplicationEventPublisher publisher) {

        this.connectorManager = connectorManager;
        this.connObjectUtils = connObjectUtils;
        this.taskDAO = taskDAO;
        this.resourceDAO = resourceDAO;
        this.plainSchemaDAO = plainSchemaDAO;
        this.notificationManager = notificationManager;
        this.auditManager = auditManager;
        this.taskDataBinder = taskDataBinder;
        this.anyUtilsFactory = anyUtilsFactory;
        this.taskUtilsFactory = taskUtilsFactory;
        this.outboundMatcher = outboundMatcher;
        this.validator = validator;
        this.publisher = publisher;
    }

    @Override
    public void expireRetryTemplate(final String resource) {
        retryTemplates.remove(resource);
    }

    protected List<PropagationActions> getPropagationActions(final ExternalResource resource) {
        List<PropagationActions> result = new ArrayList<>();

        resource.getPropagationActions().forEach(impl -> {
            try {
                result.add(ImplementationManager.build(
                        impl,
                        () -> perContextActions.get(impl.getKey()),
                        instance -> perContextActions.put(impl.getKey(), instance)));
            } catch (Exception e) {
                LOG.error("While building {}", impl, e);
            }
        });

        return result;
    }

    protected void checkMandatoryMissing(
            final PropagationTaskInfo taskInfo,
            final Set<Attribute> attrs,
            final boolean enablePasswordCheck) {

        // check if there is any missing or null / empty mandatory attribute
        Set<Object> mandatoryAttrNames = new HashSet<>();
        Optional.ofNullable(AttributeUtil.find(PropagationManager.MANDATORY_MISSING_ATTR_NAME, attrs)).
                ifPresent(missing -> {
                    attrs.remove(missing);

                    if (taskInfo.getOperation() == ResourceOperation.CREATE) {
                        // SYNCOPE-1751 remove __PASSWORD__ if enablePasswordCheck is false, this is needed to support
                        // LinkedAccount update propagation without password
                        mandatoryAttrNames.addAll(enablePasswordCheck
                                ? missing.getValue()
                                : missing.getValue().stream().
                                        filter(v -> !OperationalAttributes.PASSWORD_NAME.equals(v)).
                                        toList());
                    }
                });
        Optional.ofNullable(AttributeUtil.find(PropagationManager.MANDATORY_NULL_OR_EMPTY_ATTR_NAME, attrs)).
                ifPresent(nullOrEmpty -> {
                    attrs.remove(nullOrEmpty);

                    mandatoryAttrNames.addAll(nullOrEmpty.getValue());
                });
        if (!mandatoryAttrNames.isEmpty()) {
            throw new IllegalArgumentException(
                    "Not attempted because there are mandatory attributes without value(s): " + mandatoryAttrNames);
        }
    }

    protected Uid doCreate(
            final PropagationTaskInfo taskInfo,
            final Connector connector,
            final Mutable<Boolean> propagationAttempted) {

        Set<Attribute> attrs = taskInfo.getPropagationData().getAttributes();

        checkMandatoryMissing(taskInfo, attrs, true);

        LOG.debug("Create {} on {}", attrs, taskInfo.getResource().getKey());

        Uid result = connector.create(taskInfo.getObjectClass(), attrs, null, propagationAttempted);

        taskInfo.getResource().getProvisionByAnyType(taskInfo.getAnyType()).
                filter(provision -> provision.getUidOnCreate() != null).
                ifPresent(provision -> {
                    LOG.debug("Adding uidOnCreate [{}] attribute to [{}] on create",
                            provision.getUidOnCreate(), taskInfo.getEntityKey());
                    AnyUtils anyUtils = anyUtilsFactory.getInstance(taskInfo.getAnyTypeKind());
                    anyUtils.addAttr(
                            validator,
                            taskInfo.getEntityKey(),
                            plainSchemaDAO.findById(provision.getUidOnCreate()).
                                    orElseThrow(() -> new NotFoundException(
                                    "PlainSchema " + (provision.getUidOnCreate()))),
                            result.getUidValue());
                    publisher.publishEvent(new EntityLifecycleEvent<>(
                            this,
                            SyncDeltaType.UPDATE,
                            anyUtils.dao().findById(taskInfo.getEntityKey()).
                                    orElseThrow(() -> new NotFoundException(
                                    anyUtils.anyTypeKind() + "" + taskInfo.getEntityKey())),
                            AuthContextUtils.getDomain()));
                });

        return result;
    }

    protected Uid doUpdate(
            final PropagationTaskInfo taskInfo,
            final Connector connector,
            final ConnectorObject beforeObj,
            final Mutable<Boolean> propagationAttempted) {

        Set<Attribute> attrs = taskInfo.getPropagationData().getAttributes();

        checkMandatoryMissing(taskInfo, attrs, false);

        LOG.debug("Update {} on {}", attrs, taskInfo.getResource().getKey());

        // 1. check if rename is really required
        Name newName = AttributeUtil.getNameFromAttributes(attrs);

        LOG.debug("Rename required with value {}", newName);

        if (beforeObj != null
                && newName != null && newName.equals(beforeObj.getName())
                && !newName.getNameValue().equals(beforeObj.getUid().getUidValue())) {

            LOG.debug("Remote object name unchanged");
            attrs.remove(newName);
        }

        // 2. check whether anything is actually needing to be propagated, i.e. if there is attribute
        // difference between beforeObj - just read above from the connector - and the values to be propagated
        Map<String, Attribute> originalAttrMap = Map.of();
        if (beforeObj != null) {
            originalAttrMap = beforeObj.getAttributes().stream().
                    collect(Collectors.toMap(attr -> attr.getName().toUpperCase(), Function.identity()));
            Map<String, Attribute> updateAttrMap = attrs.stream().
                    collect(Collectors.toMap(attr -> attr.getName().toUpperCase(), Function.identity()));

            // Only compare attribute from beforeObj that are also being updated
            originalAttrMap.keySet().removeAll(updateAttrMap.keySet());
        }

        Uid result;
        if (!originalAttrMap.isEmpty() && originalAttrMap.values().equals(attrs)) {
            LOG.debug("Don't need to propagate anything: {} is equal to {}", originalAttrMap.values(), attrs);
            result = AttributeUtil.getUidAttribute(attrs);
        } else {
            LOG.debug("Attributes to update: {}", attrs);

            // 3. provision entry
            LOG.debug("Update {} on {}", attrs, taskInfo.getResource().getKey());

            result = connector.update(
                    Optional.ofNullable(beforeObj).
                            map(ConnectorObject::getObjectClass).
                            orElseGet(taskInfo::getObjectClass),
                    Optional.ofNullable(beforeObj).
                            map(ConnectorObject::getUid).
                            orElseGet(() -> new Uid(taskInfo.getOldConnObjectKey() == null
                            ? taskInfo.getConnObjectKey() : taskInfo.getOldConnObjectKey())),
                    attrs,
                    null,
                    propagationAttempted);
        }

        return result;
    }

    protected Uid doUpdateDelta(
            final PropagationTaskInfo taskInfo,
            final Set<AttributeDelta> modifications,
            final Connector connector,
            final Mutable<Boolean> propagationAttempted) {

        Uid uid = new Uid(taskInfo.getConnObjectKey());

        if (modifications.isEmpty()) {
            LOG.debug("Nothing to modify for {} on {}", uid, taskInfo.getResource().getKey());
        } else {
            LOG.debug("Update Delta {} for {} on {}", modifications, uid, taskInfo.getResource().getKey());

            connector.updateDelta(
                    taskInfo.getObjectClass(),
                    uid,
                    modifications,
                    null,
                    propagationAttempted);
        }

        return uid;
    }

    protected Uid createOrUpdate(
            final PropagationTaskInfo taskInfo,
            final boolean fetchRemoteObj,
            final ConnectorObject beforeObj,
            final Connector connector,
            final Mutable<Boolean> propagationAttempted) {

        PropagationData propagationData = taskInfo.getPropagationData();

        if (propagationData.getAttributeDeltas() == null) {
            if (beforeObj != null) {
                return doUpdate(taskInfo, connector, beforeObj, propagationAttempted);
            }

            if (fetchRemoteObj || taskInfo.getOperation() == ResourceOperation.CREATE) {
                return doCreate(taskInfo, connector, propagationAttempted);
            }

            return doUpdate(taskInfo, connector, beforeObj, propagationAttempted);
        }

        return doUpdateDelta(taskInfo, propagationData.getAttributeDeltas(), connector, propagationAttempted);
    }

    protected Uid delete(
            final PropagationTaskInfo taskInfo,
            final boolean fetchRemoteObj,
            final ConnectorObject beforeObj,
            final Connector connector,
            final Mutable<Boolean> propagationAttempted) {

        Uid result;

        if (fetchRemoteObj && beforeObj == null) {
            LOG.debug("{} not found on {}: ignoring delete",
                    taskInfo.getConnObjectKey(), taskInfo.getResource().getKey());
            result = null;
        } else {
            ObjectClass objectClass = Optional.ofNullable(beforeObj).
                    map(ConnectorObject::getObjectClass).
                    orElseGet(taskInfo::getObjectClass);
            Uid uid = Optional.ofNullable(beforeObj).
                    map(ConnectorObject::getUid).
                    orElseGet(() -> new Uid(taskInfo.getConnObjectKey()));

            LOG.debug("Delete {} on {}", uid, taskInfo.getResource().getKey());

            connector.delete(objectClass, uid, null, propagationAttempted);
            result = uid;
            taskInfo.getResource().getProvisionByAnyType(taskInfo.getAnyType()).
                    filter(provision -> provision.getUidOnCreate() != null).
                    ifPresent(provision -> {
                        LOG.debug("Removing uidOnCreate [{}] attribute from [{}] on delete",
                                provision.getUidOnCreate(), taskInfo.getEntityKey());
                        AnyUtils anyUtils = anyUtilsFactory.getInstance(taskInfo.getAnyTypeKind());
                        anyUtils.removeAttr(taskInfo.getEntityKey(),
                                plainSchemaDAO.findById(provision.getUidOnCreate())
                                        .orElseThrow(() -> new NotFoundException(
                                        "PlainSchema " + (provision.getUidOnCreate()))));
                        publisher.publishEvent(new EntityLifecycleEvent<>(
                                this,
                                SyncDeltaType.UPDATE,
                                anyUtils.dao().findById(taskInfo.getEntityKey()).
                                        orElseThrow(() -> new NotFoundException(
                                        anyUtils.anyTypeKind() + "" + taskInfo.getEntityKey())),
                                AuthContextUtils.getDomain()));
                    });
        }

        return result;
    }

    protected Optional<RetryTemplate> retryTemplate(final ExternalResource resource) {
        RetryTemplate retryTemplate = null;

        if (resource.getPropagationPolicy() != null) {
            retryTemplate = retryTemplates.get(resource.getKey());
            if (retryTemplate == null) {
                retryTemplate = new RetryTemplate();

                SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
                retryPolicy.setMaxAttempts(resource.getPropagationPolicy().getMaxAttempts());
                retryTemplate.setRetryPolicy(retryPolicy);

                String[] params = resource.getPropagationPolicy().getBackOffParams().split(";");

                switch (resource.getPropagationPolicy().getBackOffStrategy()) {
                    case EXPONENTIAL:
                        ExponentialBackOffPolicy eBackOffPolicy = new ExponentialBackOffPolicy();
                        if (params.length > 0) {
                            try {
                                eBackOffPolicy.setInitialInterval(Long.parseLong(params[0]));
                            } catch (NumberFormatException e) {
                                LOG.error("Could not convert to long: {}", params[0], e);
                            }
                        }
                        if (params.length > 1) {
                            try {
                                eBackOffPolicy.setMaxInterval(Long.parseLong(params[1]));
                            } catch (NumberFormatException e) {
                                LOG.error("Could not convert to long: {}", params[1], e);
                            }
                        }
                        if (params.length > 2) {
                            try {
                                eBackOffPolicy.setMultiplier(Double.parseDouble(params[2]));
                            } catch (NumberFormatException e) {
                                LOG.error("Could not convert to double: {}", params[2], e);
                            }
                        }
                        retryTemplate.setBackOffPolicy(eBackOffPolicy);
                        break;

                    case RANDOM:
                        ExponentialRandomBackOffPolicy erBackOffPolicy = new ExponentialRandomBackOffPolicy();
                        if (params.length > 0) {
                            try {
                                erBackOffPolicy.setInitialInterval(Long.parseLong(params[0]));
                            } catch (NumberFormatException e) {
                                LOG.error("Could not convert to long: {}", params[0], e);
                            }
                        }
                        if (params.length > 1) {
                            try {
                                erBackOffPolicy.setMaxInterval(Long.parseLong(params[1]));
                            } catch (NumberFormatException e) {
                                LOG.error("Could not convert to long: {}", params[1], e);
                            }
                        }
                        if (params.length > 2) {
                            try {
                                erBackOffPolicy.setMultiplier(Double.parseDouble(params[2]));
                            } catch (NumberFormatException e) {
                                LOG.error("Could not convert to double: {}", params[2], e);
                            }
                        }
                        retryTemplate.setBackOffPolicy(erBackOffPolicy);
                        break;

                    case FIXED:
                    default:
                        FixedBackOffPolicy fBackOffPolicy = new FixedBackOffPolicy();
                        if (params.length > 0) {
                            try {
                                fBackOffPolicy.setBackOffPeriod(Long.parseLong(params[0]));
                            } catch (NumberFormatException e) {
                                LOG.error("Could not convert to long: {}", params[0], e);
                            }

                        }
                        retryTemplate.setBackOffPolicy(fBackOffPolicy);
                }

                retryTemplates.put(resource.getKey(), retryTemplate);
            }
        }

        return Optional.ofNullable(retryTemplate);
    }

    @Override
    public TaskExec<PropagationTask> execute(
            final PropagationTaskInfo taskInfo,
            final PropagationReporter reporter,
            final String executor) {

        return retryTemplate(taskInfo.getResource()).map(rt -> rt.execute(context -> {
            LOG.debug("#{} Propagation attempt", context.getRetryCount());

            TaskExec<PropagationTask> exec = doExecute(taskInfo, reporter, executor);
            if (context.getRetryCount() < taskInfo.getResource().getPropagationPolicy().getMaxAttempts() - 1
                    && !ExecStatus.SUCCESS.name().equals(exec.getStatus())) {

                throw new RetryException("Attempt #" + context.getRetryCount() + " failed");
            }
            return exec;
        })).orElseGet(() -> doExecute(taskInfo, reporter, executor));
    }

    protected boolean isFetchRemoteObj(final PropagationTaskInfo taskInfo) {
        return Optional.ofNullable(taskInfo.getResource().getPropagationPolicy()).
                map(PropagationPolicy::isFetchAroundProvisioning).
                orElse(true);
    }

    protected TaskExec<PropagationTask> doExecute(
            final PropagationTaskInfo taskInfo,
            final PropagationReporter reporter,
            final String executor) {

        Connector connector = taskInfo.getConnector() == null
                ? connectorManager.getConnector(taskInfo.getResource())
                : taskInfo.getConnector();

        List<PropagationActions> actions = getPropagationActions(taskInfo.getResource());

        OffsetDateTime start = OffsetDateTime.now();

        TaskExec<PropagationTask> exec = taskUtilsFactory.getInstance(TaskType.PROPAGATION).newTaskExec();
        exec.setStatus(ExecStatus.CREATED.name());
        exec.setExecutor(executor);

        String taskExecutionMessage = null;
        String failureReason = null;

        // Flag to state whether any propagation has been attempted
        Mutable<Boolean> propagationAttempted = new MutableObject<>(false);

        boolean fetchRemoteObj = isFetchRemoteObj(taskInfo);

        ConnectorObject beforeObj = null;
        ConnectorObject afterObj = null;

        Provision provision = null;
        OrgUnit orgUnit = null;
        Uid uid = null;
        OpEvent.Outcome result;
        try {
            provision = taskInfo.getResource().
                    getProvisionByObjectClass(taskInfo.getObjectClass().getObjectClassValue()).orElse(null);
            orgUnit = taskInfo.getResource().getOrgUnit();

            if (taskInfo.getBeforeObj().isEmpty()) {
                if (fetchRemoteObj) {
                    // Try to read remote object BEFORE any actual operation
                    beforeObj = provision == null && orgUnit == null
                            ? null
                            : orgUnit == null
                                    ? getRemoteObject(taskInfo, connector, provision, actions, false)
                                    : getRemoteObject(taskInfo, connector, orgUnit, actions, false);
                    taskInfo.setBeforeObj(Optional.ofNullable(beforeObj));
                }
            } else {
                beforeObj = taskInfo.getBeforeObj().get();
            }

            actions.forEach(action -> action.before(taskInfo));

            switch (taskInfo.getOperation()) {
                case CREATE:
                case UPDATE:
                    uid = createOrUpdate(taskInfo, fetchRemoteObj, beforeObj, connector, propagationAttempted);
                    break;

                case DELETE:
                    uid = delete(taskInfo, fetchRemoteObj, beforeObj, connector, propagationAttempted);
                    break;

                default:
            }

            exec.setStatus(propagationAttempted.getValue()
                    ? ExecStatus.SUCCESS.name()
                    : ExecStatus.NOT_ATTEMPTED.name());

            result = OpEvent.Outcome.SUCCESS;

            LOG.debug("Successfully propagated to {}", taskInfo.getResource());
        } catch (Exception e) {
            result = OpEvent.Outcome.FAILURE;

            exec.setStatus(ExecStatus.FAILURE.name());

            propagationAttempted.setValue(true);

            LOG.error("Exception during provision on resource {}", taskInfo.getResource().getKey(), e);

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

            actions.forEach(action -> action.onError(taskInfo, exec, e));
        } finally {
            // Try to read remote object AFTER any actual operation
            if (uid != null) {
                taskInfo.setConnObjectKey(uid.getUidValue());
            }
            if (fetchRemoteObj) {
                try {
                    afterObj = provision == null && orgUnit == null
                            ? null
                            : orgUnit == null
                                    ? getRemoteObject(taskInfo, connector, provision, actions, true)
                                    : getRemoteObject(taskInfo, connector, orgUnit, actions, true);
                } catch (Exception ignore) {
                    // ignore exception
                    LOG.error("Error retrieving after object", ignore);
                }
            }

            if (!ExecStatus.FAILURE.name().equals(exec.getStatus())
                    && afterObj == null
                    && uid != null
                    && taskInfo.getOperation() != ResourceOperation.DELETE) {

                afterObj = new ConnectorObjectBuilder().
                        setObjectClass(taskInfo.getObjectClass()).
                        setUid(uid).
                        setName(Optional.ofNullable(
                                AttributeUtil.getNameFromAttributes(taskInfo.getPropagationData().getAttributes())).
                                orElseGet(() -> new Name(taskInfo.getConnObjectKey()))).
                        build();
            }

            exec.setStart(start);
            exec.setMessage(taskExecutionMessage);
            exec.setEnd(OffsetDateTime.now());

            LOG.debug("Execution finished: {}", exec);

            hasToBeregistered(taskInfo, exec).ifPresent(task -> {
                LOG.debug("Execution to be stored: {}", exec);

                exec.setTask(task);
                task.add(exec);

                taskInfo.setKey(taskDAO.save(task).getKey());
            });

            String fiql = provision == null
                    ? null
                    : afterObj != null
                            ? outboundMatcher.getFIQL(afterObj, taskInfo.getResource(), provision)
                            : beforeObj != null
                                    ? outboundMatcher.getFIQL(beforeObj, taskInfo.getResource(), provision)
                                    : null;
            reporter.onSuccessOrNonPriorityResourceFailures(
                    taskInfo,
                    ExecStatus.valueOf(exec.getStatus()),
                    failureReason,
                    fiql,
                    beforeObj,
                    afterObj);
        }

        for (PropagationActions action : actions) {
            action.after(taskInfo, exec, afterObj);
        }
        // SYNCOPE-1136
        String anyTypeKind = Optional.ofNullable(taskInfo.getAnyTypeKind()).map(Enum::name).orElse("realm");
        String operation = taskInfo.getOperation().name().toLowerCase();
        boolean notificationsAvailable = notificationManager.notificationsAvailable(
                AuthContextUtils.getDomain(),
                OpEvent.CategoryType.PROPAGATION,
                anyTypeKind,
                taskInfo.getResource().getKey(),
                operation);
        boolean auditRequested = auditManager.auditRequested(
                AuthContextUtils.getDomain(),
                AuthContextUtils.getUsername(),
                OpEvent.CategoryType.PROPAGATION,
                anyTypeKind,
                taskInfo.getResource().getKey(),
                operation);

        if (notificationsAvailable || auditRequested) {
            ExecTO execTO = taskDataBinder.getExecTO(exec);

            AfterHandlingEvent event = new AfterHandlingEvent(
                    AuthContextUtils.getDomain(),
                    AuthContextUtils.getWho(),
                    OpEvent.CategoryType.PROPAGATION,
                    anyTypeKind,
                    taskInfo.getResource().getKey(),
                    operation,
                    result,
                    beforeObj,
                    new Object[] { execTO, afterObj },
                    taskInfo);

            notificationManager.createTasks(event);

            auditManager.audit(event);
        }

        return exec;
    }

    protected TaskExec<PropagationTask> rejected(
            final PropagationTaskInfo taskInfo,
            final String rejectReason,
            final PropagationReporter reporter,
            final String executor) {

        TaskExec<PropagationTask> execution = taskUtilsFactory.getInstance(TaskType.PROPAGATION).newTaskExec();
        execution.setStatus(ExecStatus.NOT_ATTEMPTED.name());
        execution.setExecutor(executor);
        execution.setStart(OffsetDateTime.now());
        execution.setMessage(rejectReason);
        execution.setEnd(execution.getStart());

        hasToBeregistered(taskInfo, execution).ifPresent(task -> {
            LOG.debug("Execution to be stored: {}", execution);

            execution.setTask(task);
            task.add(execution);

            taskDAO.save(task);
        });

        reporter.onSuccessOrNonPriorityResourceFailures(
                taskInfo,
                ExecStatus.valueOf(execution.getStatus()),
                rejectReason,
                null,
                null,
                null);

        return execution;
    }

    /**
     * Check whether an execution has to be stored, for a given task.
     *
     * @param taskInfo propagation task
     * @param execution to be decide whether to store or not
     * @return true if execution has to be store, false otherwise
     */
    protected Optional<PropagationTask> hasToBeregistered(
            final PropagationTaskInfo taskInfo, final TaskExec<PropagationTask> execution) {

        boolean failed = ExecStatus.valueOf(execution.getStatus()) != ExecStatus.SUCCESS;

        ExternalResource resource = taskInfo.getResource();

        boolean result;
        result = switch (taskInfo.getOperation()) {
            case CREATE ->
                resource.getCreateTraceLevel() == TraceLevel.ALL
                || (failed && resource.getCreateTraceLevel().ordinal() >= TraceLevel.FAILURES.ordinal());

            case UPDATE ->
                resource.getUpdateTraceLevel() == TraceLevel.ALL
                || (failed && resource.getUpdateTraceLevel().ordinal() >= TraceLevel.FAILURES.ordinal());

            case DELETE ->
                resource.getDeleteTraceLevel() == TraceLevel.ALL
                || (failed && resource.getDeleteTraceLevel().ordinal() >= TraceLevel.FAILURES.ordinal());

            default ->
                false;
        };

        if (!result) {
            return Optional.empty();
        }

        PropagationTask task = Optional.ofNullable(taskInfo.getKey()).
                flatMap(key -> taskDAO.findById(TaskType.PROPAGATION, key)).
                map(PropagationTask.class::cast).
                orElseGet(() -> {
                    PropagationTask t = taskUtilsFactory.getInstance(TaskType.PROPAGATION).newTask();
                    t.setResource(resourceDAO.findById(resource.getKey()).
                            orElseThrow(() -> new NotFoundException("Resource " + resource.getKey())));
                    t.setObjectClassName(taskInfo.getObjectClass().getObjectClassValue());
                    t.setAnyTypeKind(taskInfo.getAnyTypeKind());
                    t.setAnyType(taskInfo.getAnyType());
                    t.setEntityKey(taskInfo.getEntityKey());
                    t.setOperation(taskInfo.getOperation());
                    t.setConnObjectKey(taskInfo.getConnObjectKey());
                    t.setOldConnObjectKey(taskInfo.getOldConnObjectKey());
                    return t;
                });
        task.setPropagationData(taskInfo.getPropagationData());

        return Optional.of(task);
    }

    /**
     * Get remote object for given task.
     *
     * @param connector connector facade proxy
     * @param taskInfo current propagation task
     * @param provision provision
     * @param actions propagation actions
     * @param latest 'FALSE' to retrieve object using old connObjectKey if not null.
     * @return remote connector object.
     */
    protected ConnectorObject getRemoteObject(
            final PropagationTaskInfo taskInfo,
            final Connector connector,
            final Provision provision,
            final List<PropagationActions> actions,
            final boolean latest) {

        String connObjectKeyValue = latest || taskInfo.getOldConnObjectKey() == null
                ? taskInfo.getConnObjectKey()
                : taskInfo.getOldConnObjectKey();

        List<ConnectorObject> matches =
                outboundMatcher.match(taskInfo, connector, provision, actions, connObjectKeyValue);
        LOG.debug("Found for propagation task {}: {}", taskInfo, matches);

        return matches.isEmpty() ? null : matches.getFirst();
    }

    /**
     * Get remote object for given task.
     *
     * @param connector connector facade proxy
     * @param taskInfo current propagation task
     * @param orgUnit orgUnit
     * @param actions propagation actions
     * @param latest 'FALSE' to retrieve object using old connObjectKey if not null.
     * @return remote connector object.
     */
    protected ConnectorObject getRemoteObject(
            final PropagationTaskInfo taskInfo,
            final Connector connector,
            final OrgUnit orgUnit,
            final List<PropagationActions> actions,
            final boolean latest) {

        String connObjectKey = latest || taskInfo.getOldConnObjectKey() == null
                ? taskInfo.getConnObjectKey()
                : taskInfo.getOldConnObjectKey();

        Set<String> moreAttrsToGet = new HashSet<>();
        actions.forEach(action -> moreAttrsToGet.addAll(action.moreAttrsToGet(Optional.of(taskInfo), orgUnit)));

        ConnectorObject obj = null;
        Optional<Item> connObjectKeyItem = orgUnit.getConnObjectKeyItem();
        if (connObjectKeyItem.isPresent()) {
            try {
                obj = connector.getObject(
                        taskInfo.getObjectClass(),
                        AttributeBuilder.build(connObjectKeyItem.get().getExtAttrName(), connObjectKey),
                        orgUnit.isIgnoreCaseMatch(),
                        MappingUtils.buildOperationOptions(
                                MappingUtils.getPropagationItems(orgUnit.getItems().stream()),
                                moreAttrsToGet.toArray(String[]::new)));
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

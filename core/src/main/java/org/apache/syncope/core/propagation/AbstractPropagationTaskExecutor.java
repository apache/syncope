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
package org.apache.syncope.core.propagation;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.apache.syncope.core.init.ConnInstanceLoader;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.TaskExec;
import org.apache.syncope.core.persistence.dao.TaskDAO;
import org.apache.syncope.core.rest.data.RoleDataBinder;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.ConnObjectUtil;
import org.apache.syncope.types.PropagationMode;
import org.apache.syncope.types.PropagationTaskExecStatus;
import org.apache.syncope.types.TraceLevel;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = {Throwable.class})
public abstract class AbstractPropagationTaskExecutor implements PropagationTaskExecutor {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractPropagationTaskExecutor.class);

    /**
     * Connector instance loader.
     */
    @Autowired
    protected ConnInstanceLoader connLoader;

    /**
     * ConnObjectUtil.
     */
    @Autowired
    protected ConnObjectUtil connObjectUtil;

    /**
     * Task DAO.
     */
    @Autowired
    protected TaskDAO taskDAO;

    @Autowired
    protected UserDataBinder userDataBinder;

    @Autowired
    protected RoleDataBinder roleDataBinder;

    @Override
    public TaskExec execute(final PropagationTask task) {
        return execute(task, null);
    }

    protected PropagationActions getPropagationActions(final ExternalResource resource) {
        PropagationActions result = null;

        if (StringUtils.isNotBlank(resource.getActionsClassName())) {
            try {
                Class<?> actionsClass = Class.forName(resource.getActionsClassName());
                result = (PropagationActions) ApplicationContextProvider.getBeanFactory().
                        createBean(actionsClass, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, true);
            } catch (ClassNotFoundException e) {
                LOG.error("Invalid PropagationAction class name '{}' for resource {}",
                        new Object[]{resource, resource.getActionsClassName(), e});
            }
        }

        if (result == null) {
            result = new DefaultPropagationActions();
        }

        return result;
    }

    protected void createOrUpdate(final PropagationTask task, final ConnectorObject beforeObj,
            final ConnectorFacadeProxy connector, final Set<String> propagationAttempted) {

        // set of attributes to be propagated
        final Set<Attribute> attributes = new HashSet<Attribute>(task.getAttributes());

        if (beforeObj == null) {
            // 1. get accountId
            final String accountId = task.getAccountId();

            // 2. get name
            final Name name = (Name) AttributeUtil.find(Name.NAME, attributes);

            // 3. check if:
            //      * accountId is not blank;
            //      * accountId is not equal to Name.
            if (StringUtils.isNotBlank(accountId) && (name == null || !accountId.equals(name.getNameValue()))) {
                // 3.a retrieve uid
                final Uid uid = (Uid) AttributeUtil.find(Uid.NAME, attributes);

                // 3.b add Uid if not provided
                if (uid == null) {
                    attributes.add(AttributeBuilder.build(Uid.NAME, Collections.singleton(accountId)));
                }
            }

            // 4. provision entry
            LOG.debug("Create {} on {}", attributes, task.getResource().getName());

            connector.create(task.getPropagationMode(), new ObjectClass(task.getObjectClassName()),
                    attributes, null, propagationAttempted);
        } else {
            // 1. check if rename is really required
            final Name newName = (Name) AttributeUtil.find(Name.NAME, attributes);

            LOG.debug("Rename required with value {}", newName);

            if (newName != null && newName.equals(beforeObj.getName())
                    && !newName.getNameValue().equals(beforeObj.getUid().getUidValue())) {

                LOG.debug("Remote object name unchanged");
                attributes.remove(newName);
            }

            // 2. check wether anything is actually needing to be propagated, i.e. if there is attribute
            // difference between beforeObj - just read above from the connector - and the values to be propagated
            Map<String, Attribute> originalAttrMap = connObjectUtil.toMap(beforeObj.getAttributes());
            Map<String, Attribute> updateAttrMap = connObjectUtil.toMap(attributes);

            // Only compare attribute from beforeObj that are also being updated
            Set<String> skipAttrNames = originalAttrMap.keySet();
            skipAttrNames.removeAll(updateAttrMap.keySet());
            for (String attrName : new HashSet<String>(skipAttrNames)) {
                originalAttrMap.remove(attrName);
            }

            Set<Attribute> originalAttrs = new HashSet<Attribute>(originalAttrMap.values());

            if (originalAttrs.equals(attributes)) {
                LOG.debug("Don't need to propagate anything: {} is equal to {}", originalAttrs, attributes);
            } else {
                LOG.debug("Attributes that would be updated {}", attributes);

                Set<Attribute> strictlyModified = new HashSet<Attribute>();
                for (Attribute attr : attributes) {
                    if (!originalAttrs.contains(attr)) {
                        strictlyModified.add(attr);
                    }
                }

                // 3. provision entry
                LOG.debug("Update {} on {}", strictlyModified, task.getResource().getName());

                connector.update(task.getPropagationMode(), beforeObj.getObjectClass(),
                        beforeObj.getUid(), strictlyModified, null, propagationAttempted);
            }
        }
    }

    protected AbstractAttributable getSubject(final PropagationTask task) {
        AbstractAttributable subject = null;

        if (task.getSubjectId() != null) {
            switch (task.getSubjectType()) {
                case USER:
                    try {
                        subject = userDataBinder.getUserFromId(task.getSubjectId());
                    } catch (Exception e) {
                        LOG.error("Could not read user {}", task.getSubjectId(), e);
                    }
                    break;

                case ROLE:
                    try {
                        subject = roleDataBinder.getRoleFromId(task.getSubjectId());
                    } catch (Exception e) {
                        LOG.error("Could not read role {}", task.getSubjectId(), e);
                    }
                    break;

                case MEMBERSHIP:
                default:
            }
        }

        return subject;
    }

    protected void delete(final PropagationTask task, final ConnectorObject beforeObj,
            final ConnectorFacadeProxy connector, final Set<String> propagationAttempted) {

        if (beforeObj == null) {
            LOG.debug("{} not found on external resource: ignoring delete", task.getAccountId());
        } else {
            /*
             * We must choose here whether to
             *  a. actually delete the provided user from the external resource
             *  b. just update the provided user data onto the external resource
             *
             * (a) happens when either there is no user associated with the PropagationTask (this takes
             * place when the task is generated via UserController.delete()) or the provided updated
             * user hasn't the current resource assigned (when the task is generated via
             * UserController.update()).
             *
             * (b) happens when the provided updated user does have the current resource assigned
             * (when the task is generated via UserController.update()): this basically means that
             * before such update, this user used to have the current resource assigned by more than
             * one mean (for example, two different memberships with the same resource).
             */
            AbstractAttributable subject = getSubject(task);
            if (subject == null || !subject.getResourceNames().contains(task.getResource().getName())) {
                LOG.debug("Delete {} on {}", beforeObj.getUid(), task.getResource().getName());

                connector.delete(
                        task.getPropagationMode(),
                        beforeObj.getObjectClass(),
                        beforeObj.getUid(),
                        null,
                        propagationAttempted);
            } else {
                createOrUpdate(task, beforeObj, connector, propagationAttempted);
            }
        }
    }

    @Override
    public TaskExec execute(final PropagationTask task, final PropagationHandler handler) {
        final PropagationActions actions = getPropagationActions(task.getResource());

        final Date startDate = new Date();

        final TaskExec execution = new TaskExec();
        execution.setStatus(PropagationTaskExecStatus.CREATED.name());

        String taskExecutionMessage = null;

        // Flag to state whether any propagation has been attempted
        Set<String> propagationAttempted = new HashSet<String>();

        ConnectorObject beforeObj = null;
        ConnectorObject afterObj = null;

        ConnectorFacadeProxy connector = null;
        try {
            connector = connLoader.getConnector(task.getResource());

            // Try to read remote object (user / group) BEFORE any actual operation
            beforeObj = getRemoteObject(task, connector, false);

            actions.before(task, beforeObj);

            switch (task.getPropagationOperation()) {
                case CREATE:
                case UPDATE:
                    createOrUpdate(task, beforeObj, connector, propagationAttempted);
                    break;

                case DELETE:
                    delete(task, beforeObj, connector, propagationAttempted);
                    break;

                default:
            }

            execution.setStatus(task.getPropagationMode() == PropagationMode.ONE_PHASE
                    ? PropagationTaskExecStatus.SUCCESS.name()
                    : PropagationTaskExecStatus.SUBMITTED.name());

            LOG.debug("Successfully propagated to {}", task.getResource());
        } catch (Exception e) {
            LOG.error("Exception during provision on resource " + task.getResource().getName(), e);

            if (e instanceof ConnectorException && e.getCause() != null) {
                taskExecutionMessage = e.getCause().getMessage();
            } else {
                StringWriter exceptionWriter = new StringWriter();
                exceptionWriter.write(e.getMessage() + "\n\n");
                e.printStackTrace(new PrintWriter(exceptionWriter));
                taskExecutionMessage = exceptionWriter.toString();
            }

            try {
                execution.setStatus(task.getPropagationMode() == PropagationMode.ONE_PHASE
                        ? PropagationTaskExecStatus.FAILURE.name()
                        : PropagationTaskExecStatus.UNSUBMITTED.name());
            } catch (Exception wft) {
                LOG.error("While executing KO action on {}", execution, wft);
            }

            propagationAttempted.add(task.getPropagationOperation().name().toLowerCase());
        } finally {
            // Try to read remote object (user / group) AFTER any actual operation
            if (connector != null) {
                afterObj = getRemoteObject(task, connector, true);
            }

            LOG.debug("Update execution for {}", task);

            execution.setStartDate(startDate);
            execution.setMessage(taskExecutionMessage);
            execution.setEndDate(new Date());

            if (hasToBeregistered(task, execution)) {
                if (propagationAttempted.isEmpty()) {
                    LOG.debug("No propagation attempted for {}", execution);
                } else {
                    execution.setTask(task);
                    task.addExec(execution);

                    LOG.debug("Execution finished: {}", execution);
                }

                taskDAO.save(task);

                // This flush call is needed to generate a value for the execution id
                // An alternative to this would be the following statement that might cause troubles with
                // concurrent calls.
                // taskExecDAO.findLatestStarted(task);
                taskDAO.flush();
            }
        }

        if (handler != null) {
            handler.handle(
                    task.getResource().getName(),
                    PropagationTaskExecStatus.valueOf(execution.getStatus()),
                    beforeObj,
                    afterObj);
        }

        actions.after(task, execution, afterObj);

        return execution;
    }

    @Override
    public void execute(final Collection<PropagationTask> tasks) throws PropagationException {
        execute(tasks, null);
    }

    @Override
    public abstract void execute(Collection<PropagationTask> tasks, PropagationHandler handler)
            throws PropagationException;

    /**
     * Check whether an execution has to be stored, for a given task.
     *
     * @param task execution's task
     * @param execution to be decide whether to store or not
     * @return true if execution has to be store, false otherwise
     */
    protected boolean hasToBeregistered(final PropagationTask task, final TaskExec execution) {
        boolean result;

        final boolean failed = !PropagationTaskExecStatus.valueOf(execution.getStatus()).isSuccessful();

        switch (task.getPropagationOperation()) {

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
     * @param latest 'FALSE' to retrieve object using old accountId if not null.
     * @return remote connector object.
     */
    protected ConnectorObject getRemoteObject(final PropagationTask task, final ConnectorFacadeProxy connector,
            final boolean latest) {

        String accountId = latest || task.getOldAccountId() == null
                ? task.getAccountId()
                : task.getOldAccountId();

        ConnectorObject obj = null;
        try {
            obj = connector.getObject(task.getPropagationMode(),
                    task.getPropagationOperation(),
                    new ObjectClass(task.getObjectClassName()),
                    new Uid(accountId),
                    connector.getOperationOptions(AttributableUtil.getInstance(task.getSubjectType()).
                    getMappingItems(task.getResource())));
        } catch (RuntimeException ignore) {
            LOG.debug("While resolving {}", accountId, ignore);
        }

        return obj;
    }
}

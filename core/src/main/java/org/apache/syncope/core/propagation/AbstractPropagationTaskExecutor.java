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

import org.apache.syncope.propagation.PropagationException;
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
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.TaskExec;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.TaskDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.apache.syncope.core.util.ConnObjectUtil;
import org.apache.syncope.core.util.NotFoundException;
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

    @Autowired
    protected ConnObjectUtil connObjectUtil;

    /**
     * User DAO.
     */
    @Autowired
    protected UserDAO userDAO;

    /**
     * Task DAO.
     */
    @Autowired
    protected TaskDAO taskDAO;

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

            // Try to read user BEFORE any actual operation
            beforeObj = getRemoteObject(connector, task, false);

            actions.before(task, beforeObj);

            switch (task.getPropagationOperation()) {
                case CREATE:
                case UPDATE:
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
                        if (StringUtils.isNotBlank(accountId)
                                && (name == null || !accountId.equals(name.getNameValue()))) {

                            // 3.a retrieve uid
                            final Uid uid = (Uid) AttributeUtil.find(Uid.NAME, attributes);

                            // 3.b add Uid if not provided
                            if (uid == null) {
                                attributes.add(AttributeBuilder.build(Uid.NAME, Collections.singleton(accountId)));
                            }
                        }

                        // 4. provision entry
                        connector.create(task.getPropagationMode(), ObjectClass.ACCOUNT, attributes, null,
                                propagationAttempted);
                    } else {
                        // 1. check if rename is really required
                        final Name newName = (Name) AttributeUtil.find(Name.NAME, attributes);

                        LOG.debug("Rename required with value {}", newName);

                        if (newName != null && newName.equals(beforeObj.getName())
                                && !beforeObj.getUid().getUidValue().equals(newName.getNameValue())) {

                            LOG.debug("Remote object name unchanged");
                            attributes.remove(newName);
                        }

                        // 2. check wether anything is actually needing to be propagated, i.e. if there is attribute
                        // difference between beforeObj - just read above from the connector - and the values to
                        // be propagated
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

                            LOG.debug("Attributes that will be actually propagated for update {}", strictlyModified);

                            connector.update(task.getPropagationMode(), ObjectClass.ACCOUNT, beforeObj.getUid(),
                                    strictlyModified, null, propagationAttempted);
                        }
                    }
                    break;

                case DELETE:
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

                        SyncopeUser user = null;
                        if (task.getSyncopeUser() != null) {
                            try {
                                user = getSyncopeUser(task.getSyncopeUser().getId());
                            } catch (NotFoundException e) {
                                LOG.warn("Requesting to delete a non-existing user from {}",
                                        task.getResource().getName(), e);
                            }
                        }

                        if (user == null || !user.getResourceNames().contains(task.getResource().getName())) {
                            LOG.debug("Perform deprovisioning on {}", task.getResource().getName());

                            connector.delete(
                                    task.getPropagationMode(),
                                    ObjectClass.ACCOUNT,
                                    beforeObj.getUid(),
                                    null,
                                    propagationAttempted);
                        } else {
                            LOG.debug("Update remote object on {}", task.getResource().getName());

                            connector.update(
                                    task.getPropagationMode(),
                                    ObjectClass.ACCOUNT,
                                    beforeObj.getUid(),
                                    task.getAttributes(),
                                    null,
                                    propagationAttempted);
                        }
                    }

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
            // Try to read user AFTER any actual operation
            if (connector != null) {
                afterObj = getRemoteObject(connector, task, true);
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

    protected SyncopeUser getSyncopeUser(final Long userId)
            throws NotFoundException {

        SyncopeUser user = userDAO.find(userId);
        if (user == null) {
            throw new NotFoundException("User " + userId);
        }

        return user;
    }

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
    protected ConnectorObject getRemoteObject(final ConnectorFacadeProxy connector, final PropagationTask task,
            final boolean latest) {

        String accountId = latest || task.getOldAccountId() == null
                ? task.getAccountId()
                : task.getOldAccountId();

        ConnectorObject obj = null;
        try {
            obj = connector.getObject(task.getPropagationMode(), task.getPropagationOperation(), ObjectClass.ACCOUNT,
                    new Uid(accountId), connector.getOperationOptions(task.getResource()));
        } catch (RuntimeException ignore) {
            LOG.debug("While resolving {}", accountId, ignore);
        }

        return obj;
    }
}

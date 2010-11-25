/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence.propagation;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.identityconnectors.framework.common.FrameworkUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.syncope.core.persistence.ConnectorInstanceLoader;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.ConnectorInstance;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.Task;
import org.syncope.core.persistence.beans.TaskExecution;
import org.syncope.core.persistence.beans.membership.Membership;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UserAttribute;
import org.syncope.core.persistence.beans.user.UserAttributeValue;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.TaskDAO;
import org.syncope.core.rest.data.TaskDataBinder;
import org.syncope.types.PropagationMode;
import org.syncope.types.ResourceOperationType;
import org.syncope.types.SchemaType;
import org.syncope.types.SchemaValueType;
import org.syncope.types.TaskExecutionStatus;

/**
 * Manage the data propagation to target resources.
 */
@Component
public class PropagationManager {

    /**
     * Logger.
     */
    private static final Logger LOG =
            LoggerFactory.getLogger(PropagationManager.class);

    /**
     * Schema DAO.
     */
    @Autowired
    private SchemaDAO schemaDAO;

    /**
     * Task DAO.
     */
    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private TaskDataBinder taskDataBinder;

    /**
     * Create the user on every associated resource.
     * Exceptions will be ignored.
     * @param user to be created.
     * @param password to be set.
     * @throws PropagationException
     */
    public void create(final SyncopeUser user, final String password)
            throws PropagationException {

        create(user, password, Collections.EMPTY_SET);
    }

    /**
     * Create the user on every associated resource.
     * It is possible to ask for a synchronous provisioning for some resources
     * specifying a set of resource names.
     * Exceptions won't be ignored and the process will be stopped if the
     * creation fails onto a synchronous resource.
     * @param user to be created.
     * @param password to be set.
     * @param syncResourceNames to ask for a synchronous or
     * asynchronous provisioning.
     * @throws PropagationException
     */
    public void create(final SyncopeUser user,
            final String password, Set<String> syncResourceNames)
            throws PropagationException {

        if (syncResourceNames == null) {
            syncResourceNames = Collections.EMPTY_SET;
        }

        Set<TargetResource> resources = new HashSet<TargetResource>();
        for (TargetResource resource : user.getTargetResources()) {
            resources.add(resource);
        }
        for (Membership membership : user.getMemberships()) {
            resources.addAll(membership.getSyncopeRole().getTargetResources());
        }

        ResourceOperations resourceOperations = new ResourceOperations();
        resourceOperations.set(ResourceOperationType.CREATE, resources);

        provision(user, password, resourceOperations, syncResourceNames);
    }

    /**
     * Performs update on each resource associated to the user.
     * It is possible to ask for a synchronous provisioning for some resources
     * specifying a set of resource names.
     * Exceptions won't be ignored and the process will be stoppend if the
     * provisioning fails onto a synchronous resource.
     * @param user to be updated.
     * @param password to be updated.
     * @param affectedResources resources affected by this update
     * @param syncResourceNames to ask for a synchronous or asynchronous update.
     * @throws PropagationException
     */
    public void update(final SyncopeUser user,
            final String password,
            final ResourceOperations resourceOperations,
            Set<String> syncResourceNames)
            throws PropagationException {

        if (syncResourceNames == null) {
            syncResourceNames = Collections.EMPTY_SET;
        }

        provision(user, password, resourceOperations, syncResourceNames);
    }

    public void delete(SyncopeUser user, Set<String> syncResourceNames)
            throws PropagationException {

        if (syncResourceNames == null) {
            syncResourceNames = Collections.EMPTY_SET;
        }

        Set<TargetResource> resources = new HashSet<TargetResource>();
        for (TargetResource resource : user.getTargetResources()) {
            resources.add(resource);
        }
        for (Membership membership : user.getMemberships()) {
            resources.addAll(membership.getSyncopeRole().getTargetResources());
        }

        ResourceOperations resourceOperations = new ResourceOperations();
        resourceOperations.set(ResourceOperationType.DELETE, resources);

        provision(user, null, resourceOperations, syncResourceNames);
    }

    /**
     * Implementation of the provisioning feature.
     * @param user
     * @param syncResourceNames
     * @param merge
     * @throws PropagationException
     */
    private void provision(
            final SyncopeUser user,
            final String password,
            final ResourceOperations resourceOperations,
            final Set<String> syncResourceNames)
            throws PropagationException {

        LOG.debug("Provisioning with user {}:\n{}",
                user, resourceOperations);

        // Avoid duplicates - see javadoc
        resourceOperations.purge();
        LOG.debug("After purge: {}", resourceOperations);

        Task task;
        TaskExecution execution;
        for (ResourceOperationType type : ResourceOperationType.values()) {
            for (TargetResource resource : resourceOperations.get(type)) {
                Map<String, Set<Attribute>> preparedAttributes =
                        prepareAttributes(user, password, resource);
                String accountId =
                        preparedAttributes.keySet().iterator().next();

                task = new Task();
                task.setResource(resource);
                task.setResourceOperationType(type);
                task.setPropagationMode(
                        syncResourceNames.contains(resource.getName())
                        ? PropagationMode.SYNC : PropagationMode.ASYNC);
                task.setAccountId(accountId);
                task.setOldAccountId(resourceOperations.getOldAccountId());
                task.setAttributes(
                        preparedAttributes.values().iterator().next());

                LOG.debug("Execution started for {}", task);

                task = taskDAO.save(task);

                TaskExecution taskExecution = new TaskExecution();
                taskExecution.setTask(task);

                if (PropagationMode.SYNC.equals(task.getPropagationMode())) {
                    syncPropagate(taskExecution);

                    // read execution after saving
                    taskExecution =
                            task.getExecutions() != null
                            && !task.getExecutions().isEmpty()
                            ? task.getExecutions().get(0) : null;

                } else {
                    asyncPropagate(taskExecution);
                }

                LOG.debug("Execution finished for {}", task);

                if (taskExecution != null
                        && syncResourceNames.contains(resource.getName())
                        && taskExecution.getStatus()
                        != TaskExecutionStatus.SUCCESS) {

                    throw new PropagationException(resource.getName(),
                            taskExecution.getMessage());
                }
            }
        }
    }

    private Map<String, Set<Attribute>> prepareAttributes(SyncopeUser user,
            String password, TargetResource resource) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Preparing resource attributes for " + user
                    + " on resource " + resource
                    + " with attributes: " + user.getAttributes());
        }

        // set of user attributes
        Set<Attribute> attributes = new HashSet<Attribute>();

        // cast to be applied on SchemaValueType
        Class castToBeApplied;

        // account id
        String accountId = null;

        // resource field values
        Set objValues;

        // syncope user attribute
        UserAttribute userAttribute;
        // syncope user attribute schema type
        SchemaValueType schemaValueType = null;
        // syncope user attribute values
        List<UserAttributeValue> values;

        for (SchemaMapping mapping : resource.getMappings()) {
            LOG.debug("Processing schema {} ({})", mapping.getSchemaName(),
                    mapping.getSchemaType().getClassName());

            try {
                AbstractSchema schema = null;
                try {
                    // check for schema or constants (AccountId/Password)
                    mapping.getSchemaType().getSchemaClass().asSubclass(
                            AbstractSchema.class);

                    schema = schemaDAO.find(mapping.getSchemaName(),
                            mapping.getSchemaType().getSchemaClass());
                } catch (ClassCastException e) {
                    // ignore exception ... check for AccountId or Password
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Wrong schema type "
                                + mapping.getSchemaType().getClassName());
                    }
                }

                if (schema != null) {
                    // get defined type for user attribute
                    schemaValueType = schema.getType();

                    // get user attribute object
                    userAttribute = user.getAttribute(mapping.getSchemaName());

                    values = userAttribute != null
                            ? userAttribute.getValues()
                            : Collections.EMPTY_LIST;

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Retrieved attribute " + userAttribute
                                + "\n* Schema " + mapping.getSchemaName()
                                + "\n* Schema type "
                                + mapping.getSchemaType().getClassName()
                                + "\n* Attribute values " + values);
                    }
                } else {
                    schemaValueType = SchemaValueType.String;

                    UserAttributeValue userAttributeValue =
                            new UserAttributeValue();

                    if (SchemaType.AccountId.equals(mapping.getSchemaType())) {
                        userAttributeValue.setStringValue(
                                user.getId().toString());
                    }
                    if (SchemaType.Password.equals(mapping.getSchemaType())
                            && password != null) {

                        userAttributeValue.setStringValue(password);
                    }

                    values = Collections.singletonList(userAttributeValue);
                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Define mapping for: "
                            + "\n* Field " + mapping.getField()
                            + "\n* is accountId " + mapping.isAccountid()
                            + "\n* is password " + (mapping.isPassword()
                            || mapping.getSchemaType().equals(
                            SchemaType.Password))
                            + "\n* nullable condition "
                            + mapping.getMandatoryCondition()
                            + "\n* Schema " + mapping.getSchemaName()
                            + "\n* SchemaType "
                            + mapping.getSchemaType().toString()
                            + "\n* ClassType " + schemaValueType.getClassName()
                            + "\n* Values " + values);
                }

                // -----------------------------
                // Retrieve user attribute values
                // -----------------------------
                objValues = new HashSet();

                for (UserAttributeValue value : values) {
                    castToBeApplied =
                            Class.forName(schemaValueType.getClassName());

                    if (!FrameworkUtil.isSupportedAttributeType(
                            castToBeApplied)) {

                        castToBeApplied = String.class;
                        objValues.add(value.getValueAsString());
                    } else {
                        objValues.add(value.getValue());
                    }
                }
                // -----------------------------

                if (mapping.isAccountid()) {
                    accountId = objValues.iterator().next().toString();
                    attributes.add(new Name(accountId));
                }

                if (mapping.isPassword()) {
                    attributes.add(AttributeBuilder.buildPassword(
                            objValues.iterator().next().toString().
                            toCharArray()));
                }

                if (!mapping.isPassword() && !mapping.isAccountid()) {
                    if (schema != null && schema.isMultivalue()) {
                        attributes.add(AttributeBuilder.build(
                                mapping.getField(),
                                objValues));
                    } else {
                        attributes.add(objValues.isEmpty()
                                ? AttributeBuilder.build(
                                mapping.getField())
                                : AttributeBuilder.build(
                                mapping.getField(),
                                objValues.iterator().next()));
                    }
                }
            } catch (ClassNotFoundException e) {
                LOG.warn("Unsupported attribute type "
                        + schemaValueType.getClassName(), e);
            } catch (Throwable t) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Attribute '" + mapping.getSchemaName()
                            + "' processing failed", t);
                }
            }
        }

        return Collections.singletonMap(accountId, attributes);
    }

    public void propagate(final TaskExecution execution) {
        Date startDate = new Date();
        TaskExecutionStatus taskExecutionStatus = null;
        String taskExecutionMessage = null;

        Task task = execution.getTask();

        // Output parameter to verify the propagation request tryed
        final Set<String> triedPropagationRequests = new HashSet<String>();


        try {
            ConnectorInstance connectorInstance =
                    task.getResource().getConnector();

            ConnectorFacadeProxy connector =
                    ConnectorInstanceLoader.getConnector(
                    connectorInstance.getId().toString());

            if (connector == null) {
                LOG.error("Connector instance bean "
                        + connectorInstance.getId().toString()
                        + " not found");

                throw new NoSuchBeanDefinitionException(
                        "Connector instance bean not found");
            }

            switch (task.getResourceOperationType()) {
                case CREATE:
                case UPDATE:
                    Uid userUid = null;
                    try {
                        userUid = connector.resolveUsernameForUpdate(
                                task.getPropagationMode(),
                                task.getResourceOperationType(),
                                ObjectClass.ACCOUNT,
                                task.getOldAccountId() == null
                                ? task.getAccountId()
                                : task.getOldAccountId(),
                                null);
                    } catch (RuntimeException ignore) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("To be ignored, when resolving "
                                    + "username on connector", ignore);
                        }
                    }

                    if (userUid != null) {
                        connector.update(
                                task.getPropagationMode(),
                                ObjectClass.ACCOUNT,
                                userUid,
                                task.getAttributes(),
                                null,
                                triedPropagationRequests);
                    } else {
                        connector.create(
                                task.getPropagationMode(),
                                ObjectClass.ACCOUNT,
                                task.getAttributes(),
                                null,
                                triedPropagationRequests);
                    }
                    break;

                case DELETE:
                    connector.delete(task.getPropagationMode(),
                            ObjectClass.ACCOUNT,
                            new Uid(task.getAccountId()),
                            null,
                            triedPropagationRequests);
                    break;

                default:
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Succesfully propagated to resource "
                        + task.getResource().getName());
            }

            taskExecutionStatus = task.getPropagationMode()
                    == PropagationMode.SYNC
                    ? TaskExecutionStatus.SUCCESS
                    : TaskExecutionStatus.SUBMITTED;
        } catch (Throwable t) {
            LOG.error("Exception during provision on resource "
                    + task.getResource().getName(), t);

            StringWriter execeptionWriter = new StringWriter();
            t.printStackTrace(new PrintWriter(execeptionWriter));
            taskExecutionMessage = execeptionWriter.toString();

            taskExecutionStatus = task.getPropagationMode()
                    == PropagationMode.SYNC
                    ? TaskExecutionStatus.FAILURE
                    : TaskExecutionStatus.UNSUBMITTED;

            triedPropagationRequests.add(
                    task.getResourceOperationType().toString().toLowerCase());
        } finally {
            LOG.debug("Update execution for {}", task);

            if (!triedPropagationRequests.isEmpty()
                    || execution.getId() != null) {

                execution.setStartDate(startDate);

                if (taskExecutionMessage != null) {
                    execution.setMessage(taskExecutionMessage);
                }

                if (taskExecutionStatus != null) {
                    execution.setStatus(taskExecutionStatus);
                }

                execution.setEndDate(new Date());

                TaskExecution actualExecution =
                        taskDataBinder.storeTaskExecution(execution);

                task.addExecution(actualExecution);
                taskDAO.save(task);

                LOG.debug("Updated {}", actualExecution);
            }
        }
    }

    public void syncPropagate(final TaskExecution execution) {
        LOG.debug("Synchronous execution {}", execution);
        propagate(execution);
    }

    //@Async
    public void asyncPropagate(final TaskExecution execution) {
        LOG.debug("Asynchronous execution {}", execution);
        new Thread() {

            @Override
            public void run() {
                propagate(execution);
            }
        }.start();
    }
}

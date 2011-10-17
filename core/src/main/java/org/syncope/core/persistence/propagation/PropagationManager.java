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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections.keyvalue.DefaultMapEntry;
import org.identityconnectors.framework.common.FrameworkUtil;
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
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.syncope.core.init.ConnInstanceLoader;
import org.syncope.core.persistence.beans.AbstractAttrValue;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.ConnInstance;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.PropagationTask;
import org.syncope.core.persistence.beans.TaskExec;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UAttr;
import org.syncope.core.persistence.beans.user.UAttrValue;
import org.syncope.core.persistence.beans.user.UDerAttr;
import org.syncope.core.persistence.beans.user.UDerSchema;
import org.syncope.core.persistence.beans.user.USchema;
import org.syncope.core.persistence.beans.user.UVirAttr;
import org.syncope.core.persistence.beans.user.UVirSchema;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.TaskDAO;
import org.syncope.core.persistence.dao.TaskExecDAO;
import org.syncope.core.util.JexlUtil;
import org.syncope.types.PropagationMode;
import org.syncope.types.PropagationOperation;
import org.syncope.types.SourceMappingType;
import org.syncope.types.SchemaType;
import org.syncope.types.PropagationTaskExecStatus;
import org.syncope.types.TraceLevel;

/**
 * Manage the data propagation to target resources.
 */
public class PropagationManager {

    /**
     * Logger.
     */
    protected static final Logger LOG =
            LoggerFactory.getLogger(PropagationManager.class);

    /**
     * Connector instance loader.
     */
    @Autowired
    private ConnInstanceLoader connLoader;

    /**
     * Resource DAO.
     */
    @Autowired
    private ResourceDAO resourceDAO;

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

    /**
     * Task execution DAO.
     */
    @Autowired
    private TaskExecDAO taskExecDAO;

    /**
     * JEXL engine for evaluating connector's account link.
     */
    @Autowired
    private JexlUtil jexlUtil;

    /**
     * Create the user on every associated resource.
     * Exceptions will be ignored.
     * @param user to be created.
     * @param password to be set.
     * @throws PropagationException when anything goes wrong.
     */
    public void create(final SyncopeUser user, final String password)
            throws PropagationException {

        create(user, password, Collections.EMPTY_SET);
    }

    /**
     * Create the user on every associated resource.
     * It is possible to ask for a mandatory provisioning for some resources
     * specifying a set of resource names.
     * Exceptions won't be ignored and the process will be stopped if the
     * creation fails onto a mandatory resource.
     *
     * @param user to be created.
     * @param password to be set.
     * @param mandResNames to ask for mandatory or optional
     * provisioning.
     * @throws PropagationException when anything goes wrong
     */
    public void create(final SyncopeUser user,
            final String password, final Set<String> mandResNames)
            throws PropagationException {

        final PropagationByResource propByRes = new PropagationByResource();
        propByRes.set(PropagationOperation.CREATE, user.getTargetResources());

        provision(user, password, propByRes,
                mandResNames == null ? Collections.EMPTY_SET : mandResNames);
    }

    /**
     * Performs update on each resource associated to the user.
     * It is possible to ask for a mandatory provisioning for some resources
     * specifying a set of resource names.
     * Exceptions won't be ignored and the process will be stopped if the
     * creation fails onto a mandatory resource.
     *
     * @param user to be updated.
     * @param password to be updated.
     * @param propByRes operations to perform on each resource.
     * @param mandResNames to ask for mandatory or optional update.
     * @throws PropagationException if anything goes wrong
     */
    public void update(final SyncopeUser user,
            final String password,
            final PropagationByResource propByRes,
            final Set<String> mandResNames)
            throws PropagationException {

        provision(user, password, propByRes,
                mandResNames == null ? Collections.EMPTY_SET : mandResNames);
    }

    /**
     * Perform delete on each resource associated to the user.
     * It is possible to ask for a mandatory provisioning for some resources
     * specifying a set of resource names.
     * Exceptions won't be ignored and the process will be stopped if the
     * creation fails onto a mandatory resource.
     *
     * @param user to be deleted
     * @param mandResNames to ask for mandatory or optyional delete
     * @throws PropagationException if anything goes wrong
     */
    public void delete(final SyncopeUser user,
            final Set<String> mandResNames)
            throws PropagationException {

        final PropagationByResource propByRes = new PropagationByResource();
        propByRes.set(PropagationOperation.DELETE,
                user.getTargetResources());

        provision(user, null, propByRes,
                mandResNames == null ? Collections.EMPTY_SET : mandResNames);
    }

    /**
     * Implementation of the provisioning feature.
     *
     * @param user user to be provisioned
     * @param password cleartext password to be provisioned
     * @param propByRes operation to be performed per resource
     * @param mandResNames resources for mandatory propagation
     * @throws PropagationException if anything goes wrong
     */
    protected void provision(
            final SyncopeUser user,
            final String password,
            final PropagationByResource propByRes,
            final Set<String> mandResNames)
            throws PropagationException {

        LOG.debug("Provisioning with user {}:\n{}", user, propByRes);

        // Avoid duplicates - see javadoc
        propByRes.purge();
        LOG.debug("After purge: {}", propByRes);

        TargetResource resource;
        Map.Entry<String, Set<Attribute>> preparedAttrs;
        PropagationTask task;
        TaskExec execution;
        for (PropagationOperation type : PropagationOperation.values()) {
            for (String resourceName : propByRes.get(type)) {
                resource = resourceDAO.find(resourceName);
                preparedAttrs = prepareAttributes(user, password, resource);

                task = new PropagationTask();
                task.setResource(resource);
                task.setResourceOperationType(type);
                task.setPropagationMode(
                        mandResNames.contains(resource.getName())
                        ? PropagationMode.SYNC
                        : resource.getOptionalPropagationMode());
                task.setAccountId(preparedAttrs.getKey());
                task.setOldAccountId(
                        propByRes.getOldAccountId(resource.getName()));
                task.setAttributes(preparedAttrs.getValue());

                LOG.debug("Execution started for {}", task);

                execution = propagate(task, new Date());

                LOG.debug("Execution finished for {}, {}", task, execution);

                // Propagation is interrupted as soon as the result of the
                // communication with a mandatory resource is in error
                if (mandResNames.contains(resource.getName())
                        && !PropagationTaskExecStatus.SUCCESS.toString().
                        equals(execution.getStatus())) {

                    throw new PropagationException(resource.getName(),
                            execution.getMessage());
                }
            }
        }
    }

    /**
     * For given source mapping type, return the corresponding Class object.
     *
     * @param sourceMappingType source mapping type
     * @return corresponding Class object, if any (can be null)
     */
    private Class getSourceMappingTypeClass(
            final SourceMappingType sourceMappingType) {

        Class result;

        switch (sourceMappingType) {
            case UserSchema:
                result = USchema.class;
                break;

            case UserDerivedSchema:
                result = UDerSchema.class;
                break;

            case UserVirtualSchema:
                result = UVirSchema.class;
                break;

            default:
                result = null;
        }

        return result;
    }

    /**
     * Prepare an attribute for sending to a connector instance.
     * 
     * @param mapping schema mapping for the given attribute
     * @param user given user
     * @param password clear-text password
     * @return account link + prepare attributes
     * @throws ClassNotFoundException if schema type for given mapping does not
     * exists in current class loader
     */
    private Map.Entry<String, Attribute> prepareAttribute(
            final SchemaMapping mapping,
            final SyncopeUser user, final String password)
            throws ClassNotFoundException {

        AbstractSchema schema = null;
        SchemaType schemaType = null;
        List<AbstractAttrValue> values = null;
        AbstractAttrValue attrValue;
        switch (mapping.getSourceMappingType()) {
            case UserSchema:
                schema = schemaDAO.find(mapping.getSourceAttrName(),
                        getSourceMappingTypeClass(
                        mapping.getSourceMappingType()));
                schemaType = schema.getType();

                UAttr attr = user.getAttribute(mapping.getSourceAttrName());
                values = attr != null
                        ? (schema.isUniqueConstraint()
                        ? Collections.singletonList(attr.getUniqueValue())
                        : attr.getValues())
                        : Collections.EMPTY_LIST;

                LOG.debug("Retrieved attribute {}", attr
                        + "\n* SourceAttrName {}"
                        + "\n* SourceMappingType {}"
                        + "\n* Attribute values {}",
                        new Object[]{mapping.getSourceAttrName(),
                            mapping.getSourceMappingType(), values});
                break;

            case UserVirtualSchema:
                schemaType = SchemaType.String;

                UVirAttr virAttr = user.getVirtualAttribute(
                        mapping.getSourceAttrName());

                values = new ArrayList<AbstractAttrValue>();
                if (virAttr != null && virAttr.getValues() != null) {
                    for (String value : virAttr.getValues()) {
                        attrValue = new UAttrValue();
                        attrValue.setStringValue(value);
                        values.add(attrValue);
                    }
                }

                LOG.debug("Retrieved virtual attribute {}", virAttr
                        + "\n* SourceAttrName {}"
                        + "\n* SourceMappingType {}"
                        + "\n* Attribute values {}",
                        new Object[]{mapping.getSourceAttrName(),
                            mapping.getSourceMappingType(), values});
                break;

            case UserDerivedSchema:
                schemaType = SchemaType.String;

                UDerAttr derAttr = user.getDerivedAttribute(
                        mapping.getSourceAttrName());
                attrValue = new UAttrValue();
                if (derAttr != null) {
                    attrValue.setStringValue(
                            derAttr.getValue(user.getAttributes()));

                    values = Collections.singletonList(attrValue);
                } else {
                    values = Collections.EMPTY_LIST;
                }

                LOG.debug("Retrieved attribute {}", derAttr
                        + "\n* SourceAttrName {}"
                        + "\n* SourceMappingType {}"
                        + "\n* Attribute values {}",
                        new Object[]{mapping.getSourceAttrName(),
                            mapping.getSourceMappingType(), values});
                break;

            case SyncopeUserId:
            case Password:
                schema = null;
                schemaType = SchemaType.String;

                attrValue = new UAttrValue();
                if (SourceMappingType.SyncopeUserId
                        == mapping.getSourceMappingType()) {

                    attrValue.setStringValue(user.getId().toString());
                }
                if (SourceMappingType.Password
                        == mapping.getSourceMappingType() && password != null) {

                    attrValue.setStringValue(password);
                }

                values = Collections.singletonList(attrValue);
                break;

            default:
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Define mapping for: "
                    + "\n* DestAttrName " + mapping.getDestAttrName()
                    + "\n* is accountId " + mapping.isAccountid()
                    + "\n* is password " + (mapping.isPassword()
                    || mapping.getSourceMappingType().equals(
                    SourceMappingType.Password))
                    + "\n* mandatory condition "
                    + mapping.getMandatoryCondition()
                    + "\n* Schema " + mapping.getSourceAttrName()
                    + "\n* SourceMappingType "
                    + mapping.getSourceMappingType().toString()
                    + "\n* ClassType " + schemaType.getClassName()
                    + "\n* Values " + values);
        }

        List<Object> objValues = new ArrayList<Object>();
        for (AbstractAttrValue value : values) {
            if (!FrameworkUtil.isSupportedAttributeType(
                    Class.forName(schemaType.getClassName()))) {

                objValues.add(value.getValueAsString());
            } else {
                objValues.add(value.getValue());
            }
        }

        String accountId = null;
        if (mapping.isAccountid()) {
            accountId = objValues.iterator().next().toString();
        }

        Attribute attribute = null;
        if (mapping.isPassword()) {
            attribute = AttributeBuilder.buildPassword(
                    objValues.iterator().next().toString().toCharArray());
        }

        if (!mapping.isPassword() && !mapping.isAccountid()) {
            if (schema != null && schema.isMultivalue()) {
                attribute = AttributeBuilder.build(mapping.getDestAttrName(),
                        objValues);
            } else {
                attribute = objValues.isEmpty()
                        ? AttributeBuilder.build(mapping.getDestAttrName())
                        : AttributeBuilder.build(mapping.getDestAttrName(),
                        objValues.iterator().next());
            }
        }

        return new DefaultMapEntry(accountId, attribute);
    }

    /**
     * Prepare attributes for sending to a connector instance.
     *
     * @param user given user
     * @param password clear-text password
     * @param resource target resource
     * @return account link + prepared attributes
     * @throws PropagationException if anything goes wrong
     */
    private Map.Entry<String, Set<Attribute>> prepareAttributes(
            final SyncopeUser user, final String password,
            final TargetResource resource)
            throws PropagationException {

        LOG.debug("Preparing resource attributes for {}"
                + " on resource {}"
                + " with attributes {}",
                new Object[]{user, resource, user.getAttributes()});

        Set<Attribute> attributes = new HashSet<Attribute>();
        String accountId = null;

        Map.Entry<String, Attribute> preparedAttribute;
        for (SchemaMapping mapping : resource.getMappings()) {
            LOG.debug("Processing schema {}", mapping.getSourceAttrName());

            try {
                preparedAttribute = prepareAttribute(mapping, user, password);
                if (preparedAttribute.getKey() != null) {
                    accountId = preparedAttribute.getKey();
                }
                if (preparedAttribute.getValue() != null) {
                    attributes.add(preparedAttribute.getValue());
                }
            } catch (Throwable t) {
                LOG.debug("Attribute '{}' processing failed",
                        mapping.getSourceAttrName(), t);
            }
        }

        if (!StringUtils.hasText(accountId)) {
            throw new PropagationException(resource.getName(),
                    "Missing accountId specification");
        }

        // Evaluate AccountLink expression
        String evaluatedAccountLink =
                jexlUtil.evaluate(resource.getAccountLink(), user);

        // AccountId must be propagated. It could be a simple attribute for
        // the target resource or the key (depending on the accountLink)
        if (evaluatedAccountLink.isEmpty()) {
            // add accountId as __NAME__ attribute ...
            LOG.debug("Add AccountId [{}] as __NAME__", accountId);
            attributes.add(new Name(accountId));
        } else {
            LOG.debug("Add AccountLink [{}] as __NAME__", evaluatedAccountLink);
            attributes.add(new Name(evaluatedAccountLink));

            // AccountId not propagated: 
            // it will be used to set the value for __UID__ attribute
            LOG.debug("AccountId will be used just as __UID__ attribute");
        }

        return new DefaultMapEntry(accountId, attributes);
    }

    /**
     * Check wether an execution has to be stored, for a given task.
     *
     * @param task execution's task
     * @param execution to be decide wether to store or not
     * @return true if execution has to be store, false otherwise
     */
    private boolean hasToBeregistered(final PropagationTask task,
            final TaskExec execution) {

        boolean result;

        final boolean failed = !PropagationTaskExecStatus.valueOf(
                execution.getStatus()).isSuccessful();

        switch (task.getResourceOperationType()) {

            case CREATE:
                result = (failed
                        && task.getResource().getCreateTraceLevel().
                        ordinal() >= TraceLevel.FAILURES.ordinal())
                        || task.getResource().getCreateTraceLevel()
                        == TraceLevel.ALL;
                break;

            case UPDATE:
                result = (failed
                        && task.getResource().getUpdateTraceLevel().
                        ordinal() >= TraceLevel.FAILURES.ordinal())
                        || task.getResource().getUpdateTraceLevel()
                        == TraceLevel.ALL;
                break;

            case DELETE:
                result = (failed
                        && task.getResource().getDeleteTraceLevel().
                        ordinal() >= TraceLevel.FAILURES.ordinal())
                        || task.getResource().getDeleteTraceLevel()
                        == TraceLevel.ALL;
                break;

            default:
                result = false;
        }

        return result;
    }

    /**
     * Execute a propagation task.
     *
     * @param task to execute
     * @param startDate timestamp for beginning task excecution
     * @return TaskExecution
     */
    public TaskExec propagate(final PropagationTask task,
            final Date startDate) {

        TaskExec execution = new TaskExec();
        execution.setStatus(PropagationTaskExecStatus.CREATED.toString());

        String taskExecutionMessage = null;

        // Flag to state wether any propagation has been attempted
        Set<String> propagationAttempted = new HashSet<String>();

        try {
            final ConnInstance connInstance =
                    task.getResource().getConnector();

            final ConnectorFacadeProxy connector =
                    connLoader.getConnector(task.getResource());

            if (connector == null) {
                final String msg = String.format(
                        "Connector instance bean for resource %s and "
                        + "connInstance %s not found",
                        task.getResource(), connInstance);

                throw new NoSuchBeanDefinitionException(msg);
            }

            switch (task.getResourceOperationType()) {
                case CREATE:
                case UPDATE:
                    ConnectorObject remoteObject = null;
                    try {
                        remoteObject = connector.getObject(
                                task.getPropagationMode(),
                                task.getResourceOperationType(),
                                ObjectClass.ACCOUNT,
                                new Uid(task.getOldAccountId() == null
                                ? task.getAccountId()
                                : task.getOldAccountId()),
                                null);
                    } catch (RuntimeException ignore) {
                        LOG.debug("To be ignored, when resolving "
                                + "username on connector", ignore);
                    }

                    if (remoteObject != null) {
                        // 0. prepare new set of attributes
                        final Set<Attribute> attributes =
                                new HashSet<Attribute>(task.getAttributes());

                        // 1. check if rename is really required
                        final Name newName = (Name) AttributeUtil.find(
                                Name.NAME, attributes);

                        LOG.debug("Rename required with value {}", newName);

                        if (newName != null
                                && newName.equals(remoteObject.getName())) {

                            LOG.debug("Remote object name unchanged");
                            attributes.remove(newName);
                        }

                        LOG.debug("Attributes to be replaced {}", attributes);

                        // 2. update with a new "normalized" attribute set
                        connector.update(
                                task.getPropagationMode(),
                                ObjectClass.ACCOUNT,
                                remoteObject.getUid(),
                                attributes,
                                null,
                                propagationAttempted);
                    } else {
                        connector.create(
                                task.getPropagationMode(),
                                ObjectClass.ACCOUNT,
                                task.getAttributes(),
                                null,
                                propagationAttempted);
                    }
                    break;

                case DELETE:
                    connector.delete(task.getPropagationMode(),
                            ObjectClass.ACCOUNT,
                            new Uid(task.getAccountId()),
                            null,
                            propagationAttempted);
                    break;

                default:
            }

            execution.setStatus(
                    task.getPropagationMode() == PropagationMode.SYNC
                    ? PropagationTaskExecStatus.SUCCESS.toString()
                    : PropagationTaskExecStatus.SUBMITTED.toString());

            LOG.debug("Successfully propagated to resource {}",
                    task.getResource());
        } catch (Throwable t) {
            LOG.error("Exception during provision on resource "
                    + task.getResource().getName(), t);

            if (t instanceof ConnectorException && t.getCause() != null) {
                taskExecutionMessage = t.getCause().getMessage();
            } else {
                StringWriter exceptionWriter = new StringWriter();
                exceptionWriter.write(t.getMessage() + "\n\n");
                t.printStackTrace(new PrintWriter(exceptionWriter));
                taskExecutionMessage = exceptionWriter.toString();
            }

            try {
                execution.setStatus(
                        task.getPropagationMode() == PropagationMode.SYNC
                        ? PropagationTaskExecStatus.FAILURE.toString()
                        : PropagationTaskExecStatus.UNSUBMITTED.toString());
            } catch (Throwable wft) {
                LOG.error("While executing KO action on {}", execution, wft);
            }

            propagationAttempted.add(
                    task.getResourceOperationType().toString().toLowerCase());
        } finally {
            LOG.debug("Update execution for {}", task);

            if (hasToBeregistered(task, execution)) {
                PropagationTask savedTask = taskDAO.save(task);

                if (!propagationAttempted.isEmpty()) {
                    execution.setStartDate(startDate);
                    execution.setMessage(taskExecutionMessage);
                    execution.setEndDate(new Date());

                    execution.setTask(savedTask);
                    execution = taskExecDAO.save(execution);

                    LOG.debug("Execution finished: {}", execution);
                } else {
                    LOG.debug("No propagation attemped for {}", execution);
                }
            }
        }

        return execution;
    }
}

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.collections.keyvalue.DefaultMapEntry;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.MapContext;
import org.apache.commons.lang.StringUtils;
import org.apache.syncope.client.mod.AttributeMod;
import org.apache.syncope.client.to.AttributeTO;
import org.apache.syncope.core.persistence.beans.AbstractAttrValue;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractMappingItem;
import org.apache.syncope.core.persistence.beans.AbstractSchema;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.ResourceDAO;
import org.apache.syncope.core.persistence.dao.SchemaDAO;
import org.apache.syncope.core.rest.controller.UnauthorizedRoleException;
import org.apache.syncope.core.rest.data.AbstractAttributableDataBinder;
import org.apache.syncope.core.rest.data.RoleDataBinder;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.ConnObjectUtil;
import org.apache.syncope.core.util.JexlUtil;
import org.apache.syncope.core.util.MappingUtil;
import org.apache.syncope.core.util.NotFoundException;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.apache.syncope.types.AttributableType;
import org.apache.syncope.types.IntMappingType;
import org.apache.syncope.types.PropagationOperation;
import org.apache.syncope.types.SchemaType;
import org.identityconnectors.framework.common.FrameworkUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manage the data propagation to external resources.
 */
@Transactional(rollbackFor = {Throwable.class})
public class PropagationManager {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(PropagationManager.class);

    /**
     * User DataBinder.
     */
    @Autowired
    private UserDataBinder userDataBinder;

    /**
     * User DataBinder.
     */
    @Autowired
    private RoleDataBinder roleDataBinder;

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
     * ConnObjectUtil.
     */
    @Autowired
    private ConnObjectUtil connObjectUtil;

    /**
     * JEXL engine for evaluating connector's account link.
     */
    @Autowired
    private JexlUtil jexlUtil;

    /**
     * Create the user on every associated resource.
     *
     * @param wfResult user to be propagated (and info associated), as per result from workflow
     * @param password to be set
     * @param vAttrs virtual attributes to be set
     * @return list of propagation tasks
     * @throws NotFoundException if user is not found
     * @throws UnauthorizedRoleException if caller doesn't own enough entitlements to administer the given user
     */
    public List<PropagationTask> getUserCreateTaskIds(final WorkflowResult<Map.Entry<Long, Boolean>> wfResult,
            final String password, final List<AttributeTO> vAttrs)
            throws NotFoundException, UnauthorizedRoleException {

        return getUserCreateTaskIds(wfResult, password, vAttrs, null);
    }

    /**
     * Create the user on every associated resource.
     *
     * @param wfResult user to be propagated (and info associated), as per result from workflow
     * @param password to be set
     * @param vAttrs virtual attributes to be set
     * @param syncResourceNames external resources performing sync, hence not to be considered for propagation
     * @return list of propagation tasks
     * @throws NotFoundException if user is not found
     * @throws UnauthorizedRoleException if caller doesn't own enough entitlements to administer the given user
     */
    public List<PropagationTask> getUserCreateTaskIds(final WorkflowResult<Map.Entry<Long, Boolean>> wfResult,
            final String password, final List<AttributeTO> vAttrs, final Set<String> syncResourceNames)
            throws NotFoundException, UnauthorizedRoleException {

        SyncopeUser user = userDataBinder.getUserFromId(wfResult.getResult().getKey());
        if (vAttrs != null && !vAttrs.isEmpty()) {
            userDataBinder.fillVirtual(user, vAttrs, AttributableUtil.getInstance(AttributableType.USER));
        }
        return getCreateTaskIds(user, password, vAttrs,
                wfResult.getResult().getValue(), wfResult.getPropByRes(), syncResourceNames);
    }

    /**
     * Create the role on every associated resource.
     *
     * @param wfResult user to be propagated (and info associated), as per result from workflow
     * @param vAttrs virtual attributes to be set
     * @return list of propagation tasks
     * @throws NotFoundException if role is not found
     * @throws UnauthorizedRoleException if caller doesn't own enough entitlements to administer the given role
     */
    public List<PropagationTask> getRoleCreateTaskIds(final WorkflowResult<Long> wfResult,
            final List<AttributeTO> vAttrs)
            throws NotFoundException, UnauthorizedRoleException {

        return getRoleCreateTaskIds(wfResult, vAttrs, null);
    }

    /**
     * Create the role on every associated resource.
     *
     * @param wfResult role to be propagated (and info associated), as per result from workflow
     * @param vAttrs virtual attributes to be set
     * @param syncResourceNames external resources performing sync, hence not to be considered for propagation
     * @return list of propagation tasks
     * @throws NotFoundException if role is not found
     * @throws UnauthorizedRoleException if caller doesn't own enough entitlements to administer the given role
     */
    public List<PropagationTask> getRoleCreateTaskIds(final WorkflowResult<Long> wfResult,
            final List<AttributeTO> vAttrs, final Set<String> syncResourceNames)
            throws NotFoundException, UnauthorizedRoleException {

        SyncopeRole role = roleDataBinder.getRoleFromId(wfResult.getResult());
        if (vAttrs != null && !vAttrs.isEmpty()) {
            roleDataBinder.fillVirtual(role, vAttrs, AttributableUtil.getInstance(AttributableType.ROLE));
        }
        return getCreateTaskIds(role, null, vAttrs, null, wfResult.getPropByRes(), syncResourceNames);
    }

    protected List<PropagationTask> getCreateTaskIds(final AbstractAttributable attributable,
            final String password, final List<AttributeTO> vAttrs, final Boolean enable,
            final PropagationByResource propByRes, final Set<String> syncResourceNames) {

        if (propByRes == null || propByRes.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        if (syncResourceNames != null) {
            propByRes.get(PropagationOperation.CREATE).removeAll(syncResourceNames);
        }

        return createTasks(attributable, password, enable, false, propByRes);
    }

    /**
     * Performs update on each resource associated to the user excluding the specified into 'resourceNames' parameter.
     *
     * @param user to be propagated
     * @param enable whether user must be enabled or not
     * @param syncResourceNames external resource names not to be considered for propagation. Use this during sync and
     * disable/enable actions limited to the external resources only
     * @return list of propagation tasks
     * @throws NotFoundException if user is not found
     */
    public List<PropagationTask> getUserUpdateTaskIds(final SyncopeUser user, final Boolean enable,
            final Set<String> syncResourceNames)
            throws NotFoundException {

        return getUpdateTaskIds(
                user, // SyncopeUser to be updated on external resources
                null, // no propagation by resources
                enable, // status to be propagated
                null, // no password
                null, // no virtual attributes to be managed
                null, // no virtual attributes to be managed
                syncResourceNames);
    }

    /**
     * Performs update on each resource associated to the user.
     *
     * @param wfResult user to be propagated (and info associated), as per result from workflow.
     * @return list of propagation tasks
     * @throws NotFoundException if user is not found
     * @throws UnauthorizedRoleException if caller doesn't own enough entitlements to administer the given user
     */
    public List<PropagationTask> getUserUpdateTaskIds(final WorkflowResult<Map.Entry<Long, Boolean>> wfResult)
            throws NotFoundException, UnauthorizedRoleException {

        return getUserUpdateTaskIds(wfResult, null, null, null, null);
    }

    /**
     * Performs update on each resource associated to the user.
     *
     * @param wfResult user to be propagated (and info associated), as per result from workflow
     * @param password to be updated
     * @param vAttrsToBeRemoved virtual attributes to be removed
     * @param vAttrsToBeUpdated virtual attributes to be added
     * @return list of propagation tasks
     * @throws NotFoundException if user is not found
     * @throws UnauthorizedRoleException if caller doesn't own enough entitlements to administer the given user
     */
    public List<PropagationTask> getUserUpdateTaskIds(final WorkflowResult<Map.Entry<Long, Boolean>> wfResult,
            final String password, final Set<String> vAttrsToBeRemoved, final Set<AttributeMod> vAttrsToBeUpdated)
            throws NotFoundException, UnauthorizedRoleException {

        return getUserUpdateTaskIds(wfResult, password, vAttrsToBeRemoved, vAttrsToBeUpdated, null);
    }

    /**
     * Performs update on each resource associated to the user.
     *
     * @param wfResult user to be propagated (and info associated), as per result from workflow
     * @param password to be updated
     * @param vAttrsToBeRemoved virtual attributes to be removed
     * @param vAttrsToBeUpdated virtual attributes to be added
     * @param syncResourceNames external resource names not to be considered for propagation. Use this during sync and
     * disable/enable actions limited to the external resources only
     * @return list of propagation tasks
     * @throws NotFoundException if user is not found
     * @throws UnauthorizedRoleException if caller doesn't own enough entitlements to administer the given user
     */
    public List<PropagationTask> getUserUpdateTaskIds(final WorkflowResult<Map.Entry<Long, Boolean>> wfResult,
            final String password, final Set<String> vAttrsToBeRemoved, final Set<AttributeMod> vAttrsToBeUpdated,
            final Set<String> syncResourceNames)
            throws NotFoundException, UnauthorizedRoleException {

        SyncopeUser user = userDataBinder.getUserFromId(wfResult.getResult().getKey());
        return getUpdateTaskIds(user, password, wfResult.getResult().getValue(),
                vAttrsToBeRemoved, vAttrsToBeUpdated, wfResult.getPropByRes(), syncResourceNames);
    }

    /**
     * Performs update on each resource associated to the role.
     *
     * @param wfResult role to be propagated (and info associated), as per result from workflow
     * @param vAttrsToBeRemoved virtual attributes to be removed
     * @param vAttrsToBeUpdated virtual attributes to be added
     * @return list of propagation tasks
     * @throws NotFoundException if role is not found
     * @throws UnauthorizedRoleException if caller doesn't own enough entitlements to administer the given role
     */
    public List<PropagationTask> getRoleUpdateTaskIds(final WorkflowResult<Long> wfResult,
            final Set<String> vAttrsToBeRemoved, final Set<AttributeMod> vAttrsToBeUpdated)
            throws NotFoundException, UnauthorizedRoleException {

        return getRoleUpdateTaskIds(wfResult, vAttrsToBeRemoved, vAttrsToBeUpdated, null);
    }

    /**
     * Performs update on each resource associated to the role.
     *
     * @param wfResult role to be propagated (and info associated), as per result from workflow
     * @param vAttrsToBeRemoved virtual attributes to be removed
     * @param vAttrsToBeUpdated virtual attributes to be added
     * @param syncResourceNames external resource names not to be considered for propagation. Use this during sync and
     * disable/enable actions limited to the external resources only
     * @return list of propagation tasks
     * @throws NotFoundException if role is not found
     * @throws UnauthorizedRoleException if caller doesn't own enough entitlements to administer the given role
     */
    public List<PropagationTask> getRoleUpdateTaskIds(final WorkflowResult<Long> wfResult,
            final Set<String> vAttrsToBeRemoved, final Set<AttributeMod> vAttrsToBeUpdated,
            final Set<String> syncResourceNames)
            throws NotFoundException, UnauthorizedRoleException {

        SyncopeRole role = roleDataBinder.getRoleFromId(wfResult.getResult());
        return getUpdateTaskIds(role, null, null,
                vAttrsToBeRemoved, vAttrsToBeUpdated, wfResult.getPropByRes(), syncResourceNames);
    }

    protected List<PropagationTask> getUpdateTaskIds(final AbstractAttributable attributable,
            final String password, final Boolean enable,
            final Set<String> vAttrsToBeRemoved, final Set<AttributeMod> vAttrsToBeUpdated,
            final PropagationByResource propByRes, final Set<String> syncResourceNames)
            throws NotFoundException {

        AbstractAttributableDataBinder binder = attributable instanceof SyncopeUser
                ? userDataBinder : roleDataBinder;

        PropagationByResource localPropByRes = binder.fillVirtual(attributable, vAttrsToBeRemoved == null
                ? Collections.EMPTY_SET
                : vAttrsToBeRemoved, vAttrsToBeUpdated == null
                ? Collections.EMPTY_SET
                : vAttrsToBeUpdated, AttributableUtil.getInstance(AttributableType.USER));

        if (propByRes == null || propByRes.isEmpty()) {
            localPropByRes.addAll(PropagationOperation.UPDATE, attributable.getResourceNames());
        } else {
            localPropByRes.merge(propByRes);
        }

        if (syncResourceNames != null) {
            localPropByRes.get(PropagationOperation.CREATE).removeAll(syncResourceNames);
            localPropByRes.get(PropagationOperation.UPDATE).removeAll(syncResourceNames);
            localPropByRes.get(PropagationOperation.DELETE).removeAll(syncResourceNames);
        }

        return createTasks(attributable, password, enable, false, localPropByRes);
    }

    /**
     * Perform delete on each resource associated to the user. It is possible to ask for a mandatory provisioning for
     * some resources specifying a set of resource names. Exceptions won't be ignored and the process will be stopped if
     * the creation fails onto a mandatory resource.
     *
     * @param userId to be deleted
     * @return list of propagation tasks
     * @throws NotFoundException if user is not found
     * @throws UnauthorizedRoleException if caller doesn't own enough entitlements to administer the given user
     */
    public List<PropagationTask> getUserDeleteTaskIds(final Long userId)
            throws NotFoundException, UnauthorizedRoleException {

        return getUserDeleteTaskIds(userId, null);
    }

    /**
     * Perform delete on each resource associated to the user. It is possible to ask for a mandatory provisioning for
     * some resources specifying a set of resource names. Exceptions won't be ignored and the process will be stopped if
     * the creation fails onto a mandatory resource.
     *
     * @param userId to be deleted
     * @param syncResourceName name of external resource performing sync, hence not to be considered for propagation
     * @return list of propagation tasks
     * @throws NotFoundException if user is not found
     * @throws UnauthorizedRoleException if caller doesn't own enough entitlements to administer the given user
     */
    public List<PropagationTask> getUserDeleteTaskIds(final Long userId, final String syncResourceName)
            throws NotFoundException, UnauthorizedRoleException {

        SyncopeUser user = userDataBinder.getUserFromId(userId);
        return getDeleteTaskIds(user, syncResourceName);
    }

    /**
     * Perform delete on each resource associated to the role. It is possible to ask for a mandatory provisioning for
     * some resources specifying a set of resource names. Exceptions won't be ignored and the process will be stopped if
     * the creation fails onto a mandatory resource.
     *
     * @param roleId to be deleted
     * @return list of propagation tasks
     * @throws NotFoundException if role is not found
     * @throws UnauthorizedRoleException if caller doesn't own enough entitlements to administer the given role
     */
    public List<PropagationTask> getRoleDeleteTaskIds(final Long roleId)
            throws NotFoundException, UnauthorizedRoleException {

        return getRoleDeleteTaskIds(roleId, null);
    }

    /**
     * Perform delete on each resource associated to the user. It is possible to ask for a mandatory provisioning for
     * some resources specifying a set of resource names. Exceptions won't be ignored and the process will be stopped if
     * the creation fails onto a mandatory resource.
     *
     * @param roleId to be deleted
     * @param syncResourceName name of external resource performing sync, hence not to be considered for propagation
     * @return list of propagation tasks
     * @throws NotFoundException if role is not found
     * @throws UnauthorizedRoleException if caller doesn't own enough entitlements to administer the given role
     */
    public List<PropagationTask> getRoleDeleteTaskIds(final Long roleId, final String syncResourceName)
            throws NotFoundException, UnauthorizedRoleException {

        SyncopeRole role = roleDataBinder.getRoleFromId(roleId);
        return getDeleteTaskIds(role, syncResourceName);
    }

    protected List<PropagationTask> getDeleteTaskIds(final AbstractAttributable attributable,
            final String syncResourceName) {

        final PropagationByResource propByRes = new PropagationByResource();
        propByRes.set(PropagationOperation.DELETE, attributable.getResourceNames());
        if (syncResourceName != null) {
            propByRes.get(PropagationOperation.DELETE).remove(syncResourceName);
        }
        return createTasks(attributable, null, false, true, propByRes);
    }

    /**
     * Prepare an attribute to be sent to a connector instance.
     *
     * @param <T> user / role
     * @param mapItem mapping item for the given attribute
     * @param subject given user
     * @param password clear-text password
     * @return account link + prepared attribute
     * @throws ClassNotFoundException if schema type for given mapping does not exists in current class loader
     */
    private <T extends AbstractAttributable> Map.Entry<String, Attribute> prepareAttribute(
            final AbstractMappingItem mapItem, final T subject, final String password)
            throws ClassNotFoundException {

        final List<AbstractAttributable> attributables = new ArrayList<AbstractAttributable>();

        switch (mapItem.getIntMappingType().getAttributableType()) {
            case USER:
                if (subject instanceof SyncopeUser) {
                    attributables.addAll(Collections.singleton(subject));
                }
                break;

            case ROLE:
                if (subject instanceof SyncopeUser) {
                    attributables.addAll(((SyncopeUser) subject).getRoles());
                }
                if (subject instanceof SyncopeRole) {
                    attributables.addAll(Collections.singleton(subject));
                }
                break;

            case MEMBERSHIP:
                if (subject instanceof SyncopeUser) {
                    attributables.addAll(((SyncopeUser) subject).getMemberships());
                }
                break;

            default:
        }

        final Entry<AbstractSchema, List<AbstractAttrValue>> entry =
                MappingUtil.getIntValues(mapItem, attributables, password, schemaDAO);

        final List<AbstractAttrValue> values = entry.getValue();
        final AbstractSchema schema = entry.getKey();
        final SchemaType schemaType = schema == null ? SchemaType.String : schema.getType();

        final String extAttrName = mapItem.getExtAttrName();

        LOG.debug("Define mapping for: "
                + "\n* ExtAttrName " + extAttrName
                + "\n* is accountId " + mapItem.isAccountid()
                + "\n* is password "
                + (mapItem.isPassword() || mapItem.getIntMappingType().equals(IntMappingType.Password))
                + "\n* mandatory condition " + mapItem.getMandatoryCondition()
                + "\n* Schema " + mapItem.getIntAttrName()
                + "\n* IntMappingType " + mapItem.getIntMappingType().toString()
                + "\n* ClassType " + schemaType.getClassName()
                + "\n* Values " + values);

        List<Object> objValues = new ArrayList<Object>();

        for (AbstractAttrValue value : values) {
            if (FrameworkUtil.isSupportedAttributeType(Class.forName(schemaType.getClassName()))) {
                objValues.add(value.getValue());
            } else {
                objValues.add(value.getValueAsString());
            }
        }

        Map.Entry<String, Attribute> result;

        if (mapItem.isAccountid()) {
            result = new DefaultMapEntry(objValues.iterator().next().toString(), null);
        } else if (mapItem.isPassword()) {
            result = new DefaultMapEntry(null,
                    AttributeBuilder.buildPassword(objValues.iterator().next().toString().toCharArray()));
        } else {
            if (schema != null && schema.isMultivalue()) {
                result = new DefaultMapEntry(null, AttributeBuilder.build(extAttrName, objValues));
            } else {
                result = new DefaultMapEntry(null, objValues.isEmpty()
                        ? AttributeBuilder.build(extAttrName)
                        : AttributeBuilder.build(extAttrName, objValues.iterator().next()));
            }
        }

        return result;
    }

    /**
     * Prepare attributes for sending to a connector instance.
     *
     * @param <T> user / role
     * @param subject given user / role
     * @param password clear-text password
     * @param enable whether user must be enabled or not
     * @param resource target resource
     * @param attrUtil attributable util to get info about subject
     * @return account link + prepared attributes
     */
    private <T extends AbstractAttributable> Map.Entry<String, Set<Attribute>> prepareAttributes(final T subject,
            final String password, final Boolean enable, final ExternalResource resource,
            final AttributableUtil attrUtil) {

        LOG.debug("Preparing resource attributes for {} on resource {} with attributes {}",
                new Object[]{subject, resource, subject.getAttributes()});

        Set<Attribute> attributes = new HashSet<Attribute>();
        String accountId = null;

        for (AbstractMappingItem mapping : attrUtil.getMappingItems(resource)) {
            LOG.debug("Processing schema {}", mapping.getIntAttrName());

            try {
                Map.Entry<String, Attribute> preparedAttribute = prepareAttribute(mapping, subject, password);

                if (preparedAttribute.getKey() != null) {
                    accountId = preparedAttribute.getKey();
                }

                if (preparedAttribute.getValue() != null) {
                    final Attribute alreadyAdded = AttributeUtil.find(preparedAttribute.getValue().getName(),
                            attributes);

                    if (alreadyAdded == null) {
                        attributes.add(preparedAttribute.getValue());
                    } else {
                        attributes.remove(alreadyAdded);

                        Set values = new HashSet(alreadyAdded.getValue());
                        values.addAll(preparedAttribute.getValue().getValue());

                        attributes.add(AttributeBuilder.build(preparedAttribute.getValue().getName(), values));
                    }
                }
            } catch (Exception e) {
                LOG.debug("Attribute '{}' processing failed", mapping.getIntAttrName(), e);
            }
        }

        if (StringUtils.isBlank(accountId)) {
            // LOG error but avoid to throw exception: leave it to the external resource
            LOG.error("Missing accountId for '{}': ", resource.getName());
        }

        // Evaluate AccountLink expression
        String evalAccountLink = null;
        if (StringUtils.isNotBlank(attrUtil.getAccountLink(resource))) {
            final JexlContext jexlContext = new MapContext();
            jexlUtil.addFieldsToContext(subject, jexlContext);
            jexlUtil.addAttrsToContext(subject.getAttributes(), jexlContext);
            jexlUtil.addDerAttrsToContext(subject.getDerivedAttributes(), subject.getAttributes(), jexlContext);
            evalAccountLink = jexlUtil.evaluate(attrUtil.getAccountLink(resource), jexlContext);
        }

        // If AccountLink evaluates to an empty string, just use the provided AccountId as Name(),
        // otherwise evaluated AccountLink expression is taken as Name().
        if (StringUtils.isBlank(evalAccountLink)) {
            // add AccountId as __NAME__ attribute ...
            LOG.debug("Add AccountId [{}] as __NAME__", accountId);
            attributes.add(new Name(accountId));
        } else {
            LOG.debug("Add AccountLink [{}] as __NAME__", evalAccountLink);
            attributes.add(new Name(evalAccountLink));

            // AccountId not propagated: it will be used to set the value for __UID__ attribute
            LOG.debug("AccountId will be used just as __UID__ attribute");
        }

        if (enable != null) {
            attributes.add(AttributeBuilder.buildEnabled(enable));
        }

        return new DefaultMapEntry(accountId, attributes);
    }

    /**
     * Create propagation tasks.
     *
     * @param <T> user / role
     * @param subject user / role to be provisioned
     * @param password cleartext password to be provisioned
     * @param enable whether user must be enabled or not
     * @param deleteOnResource whether user / role must be deleted anyway from external resource or not
     * @param propByRes operation to be performed per resource
     * @return list of propagation tasks created
     */
    protected <T extends AbstractAttributable> List<PropagationTask> createTasks(final T subject,
            final String password, final Boolean enable, final boolean deleteOnResource,
            final PropagationByResource propByRes) {

        LOG.debug("Provisioning subject {}:\n{}", subject, propByRes);

        AttributableUtil attrUtil = AttributableUtil.getInstance(subject);

        // Avoid duplicates - see javadoc
        propByRes.purge();
        LOG.debug("After purge: {}", propByRes);

        final List<PropagationTask> tasks = new ArrayList<PropagationTask>();

        for (PropagationOperation operation : PropagationOperation.values()) {
            for (String resourceName : propByRes.get(operation)) {
                final ExternalResource resource = resourceDAO.find(resourceName);
                if (resource == null) {
                    LOG.error("Invalid resource name specified: {}, ignoring...", resourceName);
                } else {
                    PropagationTask task = new PropagationTask();
                    task.setResource(resource);
                    task.setObjectClassName(connObjectUtil.fromAttributable(subject).getObjectClassValue());
                    task.setSubjectType(AttributableUtil.getInstance(subject).getType());
                    if (!deleteOnResource) {
                        task.setSubjectId(subject.getId());
                    }
                    task.setPropagationOperation(operation);
                    task.setPropagationMode(resource.getPropagationMode());
                    task.setOldAccountId(propByRes.getOldAccountId(resource.getName()));

                    Map.Entry<String, Set<Attribute>> preparedAttrs =
                            prepareAttributes(subject, password, enable, resource, attrUtil);
                    task.setAccountId(preparedAttrs.getKey());
                    task.setAttributes(preparedAttrs.getValue());

                    tasks.add(task);

                    LOG.debug("PropagationTask created: {}", task);
                }
            }
        }

        return tasks;
    }
}

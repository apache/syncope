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
package org.apache.syncope.core.propagation.impl;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.common.mod.AttributeMod;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.IntMappingType;
import org.apache.syncope.common.types.MappingPurpose;
import org.apache.syncope.common.types.ResourceOperation;
import org.apache.syncope.core.connid.ConnObjectUtil;
import org.apache.syncope.core.connid.PasswordGenerator;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractMappingItem;
import org.apache.syncope.core.persistence.beans.AbstractVirAttr;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.dao.ResourceDAO;
import org.apache.syncope.core.propagation.PropagationByResource;
import org.apache.syncope.core.rest.controller.UnauthorizedRoleException;
import org.apache.syncope.core.rest.data.AbstractAttributableDataBinder;
import org.apache.syncope.core.rest.data.RoleDataBinder;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.MappingUtil;
import org.apache.syncope.core.util.VirAttrCache;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
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
     * ConnObjectUtil.
     */
    @Autowired
    private ConnObjectUtil connObjectUtil;

    @Autowired
    private PasswordGenerator passwordGenerator;

    /**
     * Virtual attribute cache.
     */
    @Autowired
    private VirAttrCache virAttrCache;

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
     * @param noPropResourceNames external resources not to be considered for propagation
     * @return list of propagation tasks
     * @throws NotFoundException if user is not found
     * @throws UnauthorizedRoleException if caller doesn't own enough entitlements to administer the given user
     */
    public List<PropagationTask> getUserCreateTaskIds(final WorkflowResult<Map.Entry<Long, Boolean>> wfResult,
            final String password, final List<AttributeTO> vAttrs, final Set<String> noPropResourceNames)
            throws NotFoundException, UnauthorizedRoleException {

        SyncopeUser user = userDataBinder.getUserFromId(wfResult.getResult().getKey());
        if (vAttrs != null && !vAttrs.isEmpty()) {
            userDataBinder.fillVirtual(user, vAttrs, AttributableUtil.getInstance(AttributableType.USER));
        }
        return getCreateTaskIds(user, password, vAttrs,
                wfResult.getResult().getValue(), wfResult.getPropByRes(), noPropResourceNames);
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
     * @param noPropResourceNames external resources performing not to be considered for propagation
     * @return list of propagation tasks
     * @throws NotFoundException if role is not found
     * @throws UnauthorizedRoleException if caller doesn't own enough entitlements to administer the given role
     */
    public List<PropagationTask> getRoleCreateTaskIds(final WorkflowResult<Long> wfResult,
            final List<AttributeTO> vAttrs, final Set<String> noPropResourceNames)
            throws NotFoundException, UnauthorizedRoleException {

        SyncopeRole role = roleDataBinder.getRoleFromId(wfResult.getResult());
        if (vAttrs != null && !vAttrs.isEmpty()) {
            roleDataBinder.fillVirtual(role, vAttrs, AttributableUtil.getInstance(AttributableType.ROLE));
        }
        return getCreateTaskIds(role, null, vAttrs, null, wfResult.getPropByRes(), noPropResourceNames);
    }

    protected List<PropagationTask> getCreateTaskIds(final AbstractAttributable attributable,
            final String password, final List<AttributeTO> vAttrs, final Boolean enable,
            final PropagationByResource propByRes, final Set<String> noPropResourceNames) {

        if (propByRes == null || propByRes.isEmpty()) {
            return Collections.<PropagationTask>emptyList();
        }

        if (noPropResourceNames != null) {
            propByRes.get(ResourceOperation.CREATE).removeAll(noPropResourceNames);
        }

        return createTasks(attributable, password, null, null, enable, false, propByRes);
    }

    /**
     * Performs update on each resource associated to the user excluding the specified into 'resourceNames' parameter.
     *
     * @param user to be propagated
     * @param enable whether user must be enabled or not
     * @param noPropResourceNames external resource names not to be considered for propagation
     * @return list of propagation tasks
     * @throws NotFoundException if user is not found
     */
    public List<PropagationTask> getUserUpdateTaskIds(final SyncopeUser user, final Boolean enable,
            final Set<String> noPropResourceNames)
            throws NotFoundException {

        return getUpdateTaskIds(
                user, // SyncopeUser to be updated on external resources
                null, // no password
                enable, // status to be propagated
                Collections.<String>emptySet(), // no virtual attributes to be managed
                Collections.<AttributeMod>emptySet(), // no virtual attributes to be managed
                null, // no propagation by resources
                noPropResourceNames);
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

        return getUserUpdateTaskIds(
                wfResult, null, Collections.<String>emptySet(), Collections.<AttributeMod>emptySet(), null);
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
     * @param noPropResourceNames external resources not to be considered for propagation
     * @return list of propagation tasks
     * @throws NotFoundException if user is not found
     * @throws UnauthorizedRoleException if caller doesn't own enough entitlements to administer the given user
     */
    public List<PropagationTask> getUserUpdateTaskIds(final WorkflowResult<Map.Entry<Long, Boolean>> wfResult,
            final String password, final Set<String> vAttrsToBeRemoved, final Set<AttributeMod> vAttrsToBeUpdated,
            final Set<String> noPropResourceNames)
            throws NotFoundException, UnauthorizedRoleException {

        SyncopeUser user = userDataBinder.getUserFromId(wfResult.getResult().getKey());
        return getUpdateTaskIds(user, password, wfResult.getResult().getValue(),
                vAttrsToBeRemoved, vAttrsToBeUpdated, wfResult.getPropByRes(), noPropResourceNames);
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
     * @param noPropResourceNames external resource names not to be considered for propagation
     * @return list of propagation tasks
     * @throws NotFoundException if role is not found
     * @throws UnauthorizedRoleException if caller doesn't own enough entitlements to administer the given role
     */
    public List<PropagationTask> getRoleUpdateTaskIds(final WorkflowResult<Long> wfResult,
            final Set<String> vAttrsToBeRemoved, final Set<AttributeMod> vAttrsToBeUpdated,
            final Set<String> noPropResourceNames)
            throws NotFoundException, UnauthorizedRoleException {

        SyncopeRole role = roleDataBinder.getRoleFromId(wfResult.getResult());
        return getUpdateTaskIds(role, null, null,
                vAttrsToBeRemoved, vAttrsToBeUpdated, wfResult.getPropByRes(), noPropResourceNames);
    }

    protected List<PropagationTask> getUpdateTaskIds(final AbstractAttributable attributable,
            final String password, final Boolean enable,
            final Set<String> vAttrsToBeRemoved, final Set<AttributeMod> vAttrsToBeUpdated,
            final PropagationByResource propByRes, final Set<String> noPropResourceNames)
            throws NotFoundException {

        AbstractAttributableDataBinder binder = attributable instanceof SyncopeUser
                ? userDataBinder : roleDataBinder;

        PropagationByResource localPropByRes = binder.fillVirtual(attributable, vAttrsToBeRemoved == null
                ? Collections.<String>emptySet()
                : vAttrsToBeRemoved, vAttrsToBeUpdated == null
                ? Collections.<AttributeMod>emptySet()
                : vAttrsToBeUpdated, AttributableUtil.getInstance(attributable));

        if (propByRes == null || propByRes.isEmpty()) {
            localPropByRes.addAll(ResourceOperation.UPDATE, attributable.getResourceNames());
        } else {
            localPropByRes.merge(propByRes);
        }

        if (noPropResourceNames != null) {
            localPropByRes.removeAll(noPropResourceNames);
        }

        Map<String, AttributeMod> vAttrsToBeUpdatedMap = null;
        if (vAttrsToBeUpdated != null) {
            vAttrsToBeUpdatedMap = new HashMap<String, AttributeMod>();
            for (AttributeMod attrMod : vAttrsToBeUpdated) {
                vAttrsToBeUpdatedMap.put(attrMod.getSchema(), attrMod);
            }
        }

        return createTasks(attributable, password,
                vAttrsToBeRemoved, vAttrsToBeUpdatedMap, enable, false, localPropByRes);
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

        return getUserDeleteTaskIds(userId, Collections.<String>emptySet());
    }

    /**
     * Perform delete on each resource associated to the user. It is possible to ask for a mandatory provisioning for
     * some resources specifying a set of resource names. Exceptions won't be ignored and the process will be stopped if
     * the creation fails onto a mandatory resource.
     *
     * @param userId to be deleted
     * @param noPropResourceName name of external resource not to be considered for propagation
     * @return list of propagation tasks
     * @throws NotFoundException if user is not found
     * @throws UnauthorizedRoleException if caller doesn't own enough entitlements to administer the given user
     */
    public List<PropagationTask> getUserDeleteTaskIds(final Long userId, final String noPropResourceName)
            throws NotFoundException, UnauthorizedRoleException {

        SyncopeUser user = userDataBinder.getUserFromId(userId);
        return getDeleteTaskIds(user, Collections.<String>singleton(noPropResourceName));
    }

    /**
     * Perform delete on each resource associated to the user. It is possible to ask for a mandatory provisioning for
     * some resources specifying a set of resource names. Exceptions won't be ignored and the process will be stopped if
     * the creation fails onto a mandatory resource.
     *
     * @param userId to be deleted
     * @param noPropResourceNames name of external resources not to be considered for propagation
     * @return list of propagation tasks
     * @throws NotFoundException if user is not found
     * @throws UnauthorizedRoleException if caller doesn't own enough entitlements to administer the given user
     */
    public List<PropagationTask> getUserDeleteTaskIds(final Long userId, final Collection<String> noPropResourceNames)
            throws NotFoundException, UnauthorizedRoleException {

        SyncopeUser user = userDataBinder.getUserFromId(userId);
        return getDeleteTaskIds(user, noPropResourceNames);
    }

    /**
     * Perform delete on each resource associated to the user. It is possible to ask for a mandatory provisioning for
     * some resources specifying a set of resource names. Exceptions won't be ignored and the process will be stopped if
     * the creation fails onto a mandatory resource.
     *
     * @param wfResult user to be propagated (and info associated), as per result from workflow
     * @return list of propagation tasks
     */
    public List<PropagationTask> getUserDeleteTaskIds(final WorkflowResult<Long> wfResult) {
        SyncopeUser user = userDataBinder.getUserFromId(wfResult.getResult());
        return createTasks(user, null, null, null, false, true, wfResult.getPropByRes());
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

        return getRoleDeleteTaskIds(roleId, Collections.<String>emptySet());
    }

    /**
     * Perform delete on each resource associated to the user. It is possible to ask for a mandatory provisioning for
     * some resources specifying a set of resource names. Exceptions won't be ignored and the process will be stopped if
     * the creation fails onto a mandatory resource.
     *
     * @param roleId to be deleted
     * @param noPropResourceName name of external resource not to be considered for propagation
     * @return list of propagation tasks
     * @throws NotFoundException if role is not found
     * @throws UnauthorizedRoleException if caller doesn't own enough entitlements to administer the given role
     */
    public List<PropagationTask> getRoleDeleteTaskIds(final Long roleId, final String noPropResourceName)
            throws NotFoundException, UnauthorizedRoleException {

        return getRoleDeleteTaskIds(roleId, Collections.<String>singleton(noPropResourceName));
    }

    /**
     * Perform delete on each resource associated to the user. It is possible to ask for a mandatory provisioning for
     * some resources specifying a set of resource names. Exceptions won't be ignored and the process will be stopped if
     * the creation fails onto a mandatory resource.
     *
     * @param roleId to be deleted
     * @param noPropResourceNames name of external resources not to be considered for propagation
     * @return list of propagation tasks
     * @throws NotFoundException if role is not found
     * @throws UnauthorizedRoleException if caller doesn't own enough entitlements to administer the given role
     */
    public List<PropagationTask> getRoleDeleteTaskIds(final Long roleId, final Collection<String> noPropResourceNames)
            throws NotFoundException, UnauthorizedRoleException {

        SyncopeRole role = roleDataBinder.getRoleFromId(roleId);
        return getDeleteTaskIds(role, noPropResourceNames);
    }

    protected List<PropagationTask> getDeleteTaskIds(final AbstractAttributable attributable,
            final Collection<String> noPropResourceNames) {

        final PropagationByResource propByRes = new PropagationByResource();
        propByRes.set(ResourceOperation.DELETE, attributable.getResourceNames());
        if (noPropResourceNames != null && !noPropResourceNames.isEmpty()) {
            propByRes.get(ResourceOperation.DELETE).removeAll(noPropResourceNames);
        }
        return createTasks(attributable, null, null, null, false, true, propByRes);
    }

    /**
     * Prepare attributes for sending to a connector instance.
     *
     * @param <T> user / role
     * @param subject given user / role
     * @param password clear-text password
     * @param vAttrsToBeRemoved virtual attributes to be removed
     * @param vAttrsToBeUpdated virtual attributes to be added
     * @param enable whether user must be enabled or not
     * @param resource target resource
     * @return account link + prepared attributes
     */
    protected <T extends AbstractAttributable> Map.Entry<String, Set<Attribute>> prepareAttributes(final T subject,
            final String password, final Set<String> vAttrsToBeRemoved,
            final Map<String, AttributeMod> vAttrsToBeUpdated, final Boolean enable, final ExternalResource resource) {

        LOG.debug("Preparing resource attributes for {} on resource {} with attributes {}",
                subject, resource, subject.getAttributes());

        Set<Attribute> attributes = new HashSet<Attribute>();
        String accountId = null;

        final AttributableUtil attrUtil = AttributableUtil.getInstance(subject);
        for (AbstractMappingItem mapping : attrUtil.getMappingItems(resource, MappingPurpose.PROPAGATION)) {
            LOG.debug("Processing schema {}", mapping.getIntAttrName());

            try {
                if ((attrUtil.getType() == AttributableType.USER
                        && mapping.getIntMappingType() == IntMappingType.UserVirtualSchema)
                        || (attrUtil.getType() == AttributableType.ROLE
                        && mapping.getIntMappingType() == IntMappingType.RoleVirtualSchema)) {
                    LOG.debug("Expire entry cache {}-{}", subject.getId(), mapping.getIntAttrName());
                    virAttrCache.expire(attrUtil.getType(), subject.getId(), mapping.getIntAttrName());
                }

                Map.Entry<String, Attribute> preparedAttribute = MappingUtil.prepareAttribute(
                        resource, mapping, subject, password, passwordGenerator, vAttrsToBeRemoved, vAttrsToBeUpdated);

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

                        Set<Object> values = new HashSet<Object>(alreadyAdded.getValue());
                        values.addAll(preparedAttribute.getValue().getValue());

                        attributes.add(AttributeBuilder.build(preparedAttribute.getValue().getName(), values));
                    }
                }
            } catch (Exception e) {
                LOG.debug("Attribute '{}' processing failed", mapping.getIntAttrName(), e);
            }
        }

        attributes.add(MappingUtil.evaluateNAME(subject, resource, accountId));

        if (enable != null) {
            attributes.add(AttributeBuilder.buildEnabled(enable));
        }

        return new SimpleEntry<String, Set<Attribute>>(accountId, attributes);
    }

    /**
     * Create propagation tasks.
     *
     * @param <T> user / role
     * @param subject user / role to be provisioned
     * @param password cleartext password to be provisioned
     * @param vAttrsToBeRemoved virtual attributes to be removed
     * @param vAttrsToBeUpdated virtual attributes to be added
     * @param enable whether user must be enabled or not
     * @param deleteOnResource whether user / role must be deleted anyway from external resource or not
     * @param propByRes operation to be performed per resource
     * @return list of propagation tasks created
     */
    protected <T extends AbstractAttributable> List<PropagationTask> createTasks(final T subject, final String password,
            final Set<String> vAttrsToBeRemoved, final Map<String, AttributeMod> vAttrsToBeUpdated,
            final Boolean enable, final boolean deleteOnResource,
            final PropagationByResource propByRes) {

        LOG.debug("Provisioning subject {}:\n{}", subject, propByRes);

        final AttributableUtil attrUtil = AttributableUtil.getInstance(subject);

        if (!propByRes.get(ResourceOperation.CREATE).isEmpty()
                && vAttrsToBeRemoved != null && vAttrsToBeUpdated != null) {

            connObjectUtil.retrieveVirAttrValues(subject, attrUtil);

            // update vAttrsToBeUpdated as well
            for (AbstractVirAttr virAttr : subject.getVirtualAttributes()) {
                final String schema = virAttr.getVirtualSchema().getName();

                final AttributeMod attributeMod = new AttributeMod();
                attributeMod.setSchema(schema);
                attributeMod.getValuesToBeAdded().addAll(virAttr.getValues());

                vAttrsToBeUpdated.put(schema, attributeMod);
            }
        }

        // Avoid duplicates - see javadoc
        propByRes.purge();
        LOG.debug("After purge: {}", propByRes);

        final List<PropagationTask> tasks = new ArrayList<PropagationTask>();

        for (ResourceOperation operation : ResourceOperation.values()) {
            for (String resourceName : propByRes.get(operation)) {
                final ExternalResource resource = resourceDAO.find(resourceName);
                if (resource == null) {
                    LOG.error("Invalid resource name specified: {}, ignoring...", resourceName);
                } else if (attrUtil.getMappingItems(resource, MappingPurpose.PROPAGATION).isEmpty()) {
                    LOG.warn("Requesting propagation for {} but no propagation mapping provided for {}",
                            attrUtil.getType(), resource);
                } else {
                    PropagationTask task = new PropagationTask();
                    task.setResource(resource);
                    task.setObjectClassName(connObjectUtil.fromAttributable(subject).getObjectClassValue());
                    task.setSubjectType(attrUtil.getType());
                    if (!deleteOnResource) {
                        task.setSubjectId(subject.getId());
                    }
                    task.setPropagationOperation(operation);
                    task.setPropagationMode(resource.getPropagationMode());
                    task.setOldAccountId(propByRes.getOldAccountId(resource.getName()));

                    Map.Entry<String, Set<Attribute>> preparedAttrs = prepareAttributes(subject, password,
                            vAttrsToBeRemoved, vAttrsToBeUpdated, enable, resource);
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

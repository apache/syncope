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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.mod.AttrMod;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.VirAttr;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.java.VirAttrHandler;
import org.apache.syncope.core.misc.ConnObjectUtils;
import org.apache.syncope.core.misc.MappingUtils;
import org.apache.syncope.core.misc.jexl.JexlUtils;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
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
@Transactional(rollbackFor = { Throwable.class })
public class PropagationManagerImpl implements PropagationManager {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(PropagationManager.class);

    protected AnyObjectDAO anyObjectDAO;

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
     * Resource DAO.
     */
    @Autowired
    protected ExternalResourceDAO resourceDAO;

    @Autowired
    protected EntityFactory entityFactory;

    /**
     * ConnObjectUtils.
     */
    @Autowired
    protected ConnObjectUtils connObjectUtils;

    @Autowired
    protected AnyUtilsFactory anyUtilsFactory;

    @Autowired
    protected VirAttrHandler virAttrHandler;

    @Override
    public List<PropagationTask> getAnyObjectCreateTasks(
            final WorkflowResult<Long> wfResult,
            final Collection<AttrTO> vAttrs,
            final Collection<String> noPropResourceNames) {

        return getAnyObjectCreateTasks(wfResult.getResult(), vAttrs, wfResult.getPropByRes(), noPropResourceNames);
    }

    @Override
    public List<PropagationTask> getAnyObjectCreateTasks(
            final Long key,
            final Collection<AttrTO> vAttrs,
            final PropagationByResource propByRes,
            final Collection<String> noPropResourceNames) {

        AnyObject anyObject = anyObjectDAO.authFind(key);
        if (vAttrs != null && !vAttrs.isEmpty()) {
            virAttrHandler.fillVirtual(anyObject, vAttrs, anyUtilsFactory.getInstance(AnyTypeKind.ANY_OBJECT));
        }

        return getCreateTaskIds(anyObject, null, null, propByRes, noPropResourceNames);
    }

    @Override
    public List<PropagationTask> getUserCreateTasks(
            final Long key,
            final Boolean enable,
            final PropagationByResource propByRes,
            final String password,
            final Collection<AttrTO> vAttrs,
            final Collection<String> noPropResourceNames) {

        User user = userDAO.authFind(key);
        if (vAttrs != null && !vAttrs.isEmpty()) {
            virAttrHandler.fillVirtual(user, vAttrs, anyUtilsFactory.getInstance(AnyTypeKind.USER));
        }
        return getCreateTaskIds(user, password, enable, propByRes, noPropResourceNames);
    }

    @Override
    public List<PropagationTask> getGroupCreateTasks(
            final WorkflowResult<Long> wfResult,
            final Collection<AttrTO> vAttrs,
            final Collection<String> noPropResourceNames) {

        return getGroupCreateTasks(wfResult.getResult(), vAttrs, wfResult.getPropByRes(), noPropResourceNames);
    }

    @Override
    public List<PropagationTask> getGroupCreateTasks(
            final Long key,
            final Collection<AttrTO> vAttrs,
            final PropagationByResource propByRes,
            final Collection<String> noPropResourceNames) {

        Group group = groupDAO.authFind(key);
        if (vAttrs != null && !vAttrs.isEmpty()) {
            virAttrHandler.fillVirtual(group, vAttrs, anyUtilsFactory.getInstance(AnyTypeKind.GROUP));
        }

        return getCreateTaskIds(group, null, null, propByRes, noPropResourceNames);
    }

    protected List<PropagationTask> getCreateTaskIds(final Any<?, ?, ?> any,
            final String password, final Boolean enable,
            final PropagationByResource propByRes,
            final Collection<String> noPropResourceNames) {

        if (propByRes == null || propByRes.isEmpty()) {
            return Collections.<PropagationTask>emptyList();
        }

        if (noPropResourceNames != null) {
            propByRes.get(ResourceOperation.CREATE).removeAll(noPropResourceNames);
        }

        return createTasks(any, password, true, null, null, enable, false, propByRes);
    }

    @Override
    public List<PropagationTask> getAnyObjectUpdateTasks(
            final WorkflowResult<Long> wfResult,
            final Set<String> vAttrsToBeRemoved, final Set<AttrMod> vAttrsToBeUpdated,
            final Set<String> noPropResourceNames) {

        AnyObject anyObject = anyObjectDAO.authFind(wfResult.getResult());
        return getUpdateTasks(anyObject, null, false, null,
                vAttrsToBeRemoved, vAttrsToBeUpdated, wfResult.getPropByRes(), noPropResourceNames);
    }

    @Override
    public List<PropagationTask> getUserUpdateTasks(final User user, final Boolean enable,
            final Collection<String> noPropResourceNames) {

        return getUpdateTasks(
                user, // user to be updated on external resources
                null, // no password
                false,
                enable, // status to be propagated
                Collections.<String>emptySet(), // no virtual attributes to be managed
                Collections.<AttrMod>emptySet(), // no virtual attributes to be managed
                null, // no propagation by resources
                noPropResourceNames);
    }

    @Override
    public List<PropagationTask> getUserUpdateTasks(final WorkflowResult<Pair<UserMod, Boolean>> wfResult,
            final boolean changePwd, final Collection<String> noPropResourceNames) {

        User user = userDAO.authFind(wfResult.getResult().getKey().getKey());
        return getUpdateTasks(user,
                wfResult.getResult().getKey().getPassword(),
                changePwd,
                wfResult.getResult().getValue(),
                wfResult.getResult().getKey().getVirAttrsToRemove(),
                wfResult.getResult().getKey().getVirAttrsToUpdate(),
                wfResult.getPropByRes(),
                noPropResourceNames);
    }

    @Override
    public List<PropagationTask> getUserUpdateTasks(final WorkflowResult<Pair<UserMod, Boolean>> wfResult) {
        UserMod userMod = wfResult.getResult().getKey();

        // Propagate password update only to requested resources
        List<PropagationTask> tasks = new ArrayList<>();
        if (userMod.getPwdPropRequest() == null) {
            // a. no specific password propagation request: generate propagation tasks for any resource associated
            tasks = getUserUpdateTasks(wfResult, false, null);
        } else {
            // b. generate the propagation task list in two phases: first the ones containing password,
            // the the rest (with no password)
            final PropagationByResource origPropByRes = new PropagationByResource();
            origPropByRes.merge(wfResult.getPropByRes());

            Set<String> pwdResourceNames = new HashSet<>(userMod.getPwdPropRequest().getResourceNames());
            Collection<String> currentResourceNames = userDAO.findAllResourceNames(userDAO.authFind(userMod.getKey()));
            pwdResourceNames.retainAll(currentResourceNames);
            PropagationByResource pwdPropByRes = new PropagationByResource();
            pwdPropByRes.addAll(ResourceOperation.UPDATE, pwdResourceNames);
            if (!pwdPropByRes.isEmpty()) {
                Set<String> toBeExcluded = new HashSet<>(currentResourceNames);
                toBeExcluded.addAll(userMod.getResourcesToAdd());
                toBeExcluded.removeAll(pwdResourceNames);
                tasks.addAll(getUserUpdateTasks(wfResult, true, toBeExcluded));
            }

            PropagationByResource nonPwdPropByRes = new PropagationByResource();
            nonPwdPropByRes.merge(origPropByRes);
            nonPwdPropByRes.removeAll(pwdResourceNames);
            nonPwdPropByRes.purge();
            if (!nonPwdPropByRes.isEmpty()) {
                tasks.addAll(getUserUpdateTasks(wfResult, false, pwdResourceNames));
            }
        }

        return tasks;
    }

    @Override
    public List<PropagationTask> getGroupUpdateTasks(final WorkflowResult<Long> wfResult,
            final Set<String> vAttrsToBeRemoved, final Set<AttrMod> vAttrsToBeUpdated,
            final Set<String> noPropResourceNames) {

        Group group = groupDAO.authFind(wfResult.getResult());
        return getUpdateTasks(group, null, false, null,
                vAttrsToBeRemoved, vAttrsToBeUpdated, wfResult.getPropByRes(), noPropResourceNames);
    }

    @Override
    public List<PropagationTask> getUpdateTasks(final Any<?, ?, ?> any,
            final String password, final boolean changePwd, final Boolean enable,
            final Set<String> vAttrsToBeRemoved, final Set<AttrMod> vAttrsToBeUpdated,
            final PropagationByResource propByRes, final Collection<String> noPropResourceNames) {

        PropagationByResource localPropByRes = virAttrHandler.fillVirtual(any, vAttrsToBeRemoved == null
                ? Collections.<String>emptySet()
                : vAttrsToBeRemoved, vAttrsToBeUpdated == null
                        ? Collections.<AttrMod>emptySet()
                        : vAttrsToBeUpdated, anyUtilsFactory.getInstance(any));

        if (propByRes == null || propByRes.isEmpty()) {
            localPropByRes.addAll(ResourceOperation.UPDATE, any.getResourceNames());
        } else {
            localPropByRes.merge(propByRes);
        }

        if (noPropResourceNames != null) {
            localPropByRes.removeAll(noPropResourceNames);
        }

        Map<String, AttrMod> vAttrsToBeUpdatedMap = null;
        if (vAttrsToBeUpdated != null) {
            vAttrsToBeUpdatedMap = new HashMap<>();
            for (AttrMod attrMod : vAttrsToBeUpdated) {
                vAttrsToBeUpdatedMap.put(attrMod.getSchema(), attrMod);
            }
        }

        return createTasks(
                any, password, changePwd, vAttrsToBeRemoved, vAttrsToBeUpdatedMap, enable, false, localPropByRes);
    }

    @Override
    public List<PropagationTask> getAnyObjectDeleteTasks(final Long anyObjectKey) {
        return getAnyObjectDeleteTasks(anyObjectKey, Collections.<String>emptySet());
    }

    @Override
    public List<PropagationTask> getAnyObjectDeleteTasks(final Long anyObjectKey, final String noPropResourceName) {
        return getAnyObjectDeleteTasks(anyObjectKey, Collections.<String>singleton(noPropResourceName));
    }

    @Override
    public List<PropagationTask> getAnyObjectDeleteTasks(
            final Long anyObjectKey, final Collection<String> noPropResourceNames) {

        AnyObject anyObject = anyObjectDAO.authFind(anyObjectKey);
        return getDeleteTaskIds(anyObject, anyObject.getResourceNames(), noPropResourceNames);
    }

    @Override
    public List<PropagationTask> getAnyObjectDeleteTasks(
            final Long groupKey, final Set<String> resourceNames, final Collection<String> noPropResourceNames) {

        AnyObject anyObject = anyObjectDAO.authFind(groupKey);
        return getDeleteTaskIds(anyObject, resourceNames, noPropResourceNames);
    }

    @Override
    public List<PropagationTask> getUserDeleteTasks(final Long userKey, final Collection<String> noPropResourceNames) {
        User user = userDAO.authFind(userKey);
        return getDeleteTaskIds(user, userDAO.findAllResourceNames(user), noPropResourceNames);
    }

    @Override
    public List<PropagationTask> getUserDeleteTasks(
            final Long userKey, final Set<String> resourceNames, final Collection<String> noPropResourceNames) {

        User user = userDAO.authFind(userKey);
        return getDeleteTaskIds(user, resourceNames, noPropResourceNames);
    }

    @Override
    public List<PropagationTask> getGroupDeleteTasks(final Long groupKey) {
        return getGroupDeleteTasks(groupKey, Collections.<String>emptySet());
    }

    @Override
    public List<PropagationTask> getGroupDeleteTasks(final Long groupKey, final String noPropResourceName) {
        return getGroupDeleteTasks(groupKey, Collections.<String>singleton(noPropResourceName));
    }

    @Override
    public List<PropagationTask> getGroupDeleteTasks(
            final Long groupKey, final Collection<String> noPropResourceNames) {

        Group group = groupDAO.authFind(groupKey);
        return getDeleteTaskIds(group, group.getResourceNames(), noPropResourceNames);
    }

    @Override
    public List<PropagationTask> getGroupDeleteTasks(
            final Long groupKey, final Set<String> resourceNames, final Collection<String> noPropResourceNames) {

        Group group = groupDAO.authFind(groupKey);
        return getDeleteTaskIds(group, resourceNames, noPropResourceNames);
    }

    protected List<PropagationTask> getDeleteTaskIds(
            final Any<?, ?, ?> any,
            final Collection<String> resourceNames,
            final Collection<String> noPropResourceNames) {

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.set(ResourceOperation.DELETE, resourceNames);
        if (noPropResourceNames != null && !noPropResourceNames.isEmpty()) {
            propByRes.get(ResourceOperation.DELETE).removeAll(noPropResourceNames);
        }
        return createTasks(any, null, false, null, null, false, true, propByRes);
    }

    /**
     * Create propagation tasks.
     *
     * @param any user / group to be provisioned
     * @param password cleartext password to be provisioned
     * @param changePwd whether password should be included for propagation attributes or not
     * @param vAttrsToBeRemoved virtual attributes to be removed
     * @param vAttrsToBeUpdated virtual attributes to be added
     * @param enable whether user must be enabled or not
     * @param deleteOnResource whether user / group must be deleted anyway from external resource or not
     * @param propByRes operation to be performed per resource
     * @return list of propagation tasks created
     */
    protected List<PropagationTask> createTasks(final Any<?, ?, ?> any,
            final String password, final boolean changePwd,
            final Set<String> vAttrsToBeRemoved, final Map<String, AttrMod> vAttrsToBeUpdated,
            final Boolean enable, final boolean deleteOnResource, final PropagationByResource propByRes) {

        LOG.debug("Provisioning any {}:\n{}", any, propByRes);

        AnyUtils anyUtils = anyUtilsFactory.getInstance(any);

        if (!propByRes.get(ResourceOperation.CREATE).isEmpty()
                && vAttrsToBeRemoved != null && vAttrsToBeUpdated != null) {

            connObjectUtils.retrieveVirAttrValues(any);

            // update vAttrsToBeUpdated as well
            for (VirAttr<?> virAttr : any.getVirAttrs()) {
                final String schema = virAttr.getSchema().getKey();

                final AttrMod attributeMod = new AttrMod();
                attributeMod.setSchema(schema);
                attributeMod.getValuesToBeAdded().addAll(virAttr.getValues());

                vAttrsToBeUpdated.put(schema, attributeMod);
            }
        }

        // Avoid duplicates - see javadoc
        propByRes.purge();
        LOG.debug("After purge: {}", propByRes);

        List<PropagationTask> tasks = new ArrayList<>();

        for (ResourceOperation operation : ResourceOperation.values()) {
            for (String resourceName : propByRes.get(operation)) {
                ExternalResource resource = resourceDAO.find(resourceName);
                Provision provision = resource == null ? null : resource.getProvision(any.getType());
                if (resource == null) {
                    LOG.error("Invalid resource name specified: {}, ignoring...", resourceName);
                } else if (provision == null) {
                    LOG.error("No provision specified on resource {} for type {}, ignoring...",
                            resource, any.getType());
                } else if (anyUtils.getMappingItems(provision, MappingPurpose.PROPAGATION).isEmpty()) {
                    LOG.warn("Requesting propagation for {} but no propagation mapping provided for {}",
                            any.getType(), resource);
                } else {
                    PropagationTask task = entityFactory.newEntity(PropagationTask.class);
                    task.setResource(resource);
                    task.setObjectClassName(
                            resource.getProvision(any.getType()).getObjectClass().getObjectClassValue());
                    task.setAnyTypeKind(anyUtils.getAnyTypeKind());
                    if (!deleteOnResource) {
                        task.setAnyKey(any.getKey());
                    }
                    task.setPropagationOperation(operation);
                    task.setPropagationMode(resource.getPropagationMode());
                    task.setOldConnObjectKey(propByRes.getOldConnObjectKey(resource.getKey()));

                    Pair<String, Set<Attribute>> preparedAttrs = MappingUtils.prepareAttributes(anyUtils, any,
                            password, changePwd, vAttrsToBeRemoved, vAttrsToBeUpdated, enable, provision);
                    task.setConnObjectKey(preparedAttrs.getKey());

                    // Check if any of mandatory attributes (in the mapping) is missing or not received any value: 
                    // if so, add special attributes that will be evaluated by PropagationTaskExecutor
                    List<String> mandatoryMissing = new ArrayList<>();
                    List<String> mandatoryNullOrEmpty = new ArrayList<>();
                    for (MappingItem item : anyUtils.getMappingItems(provision, MappingPurpose.PROPAGATION)) {
                        if (!item.isConnObjectKey()
                                && JexlUtils.evaluateMandatoryCondition(item.getMandatoryCondition(), any)) {

                            Attribute attr = AttributeUtil.find(item.getExtAttrName(), preparedAttrs.getValue());
                            if (attr == null) {
                                mandatoryMissing.add(item.getExtAttrName());
                            } else if (attr.getValue() == null || attr.getValue().isEmpty()) {
                                mandatoryNullOrEmpty.add(item.getExtAttrName());
                            }
                        }
                    }
                    if (!mandatoryMissing.isEmpty()) {
                        preparedAttrs.getValue().add(AttributeBuilder.build(
                                PropagationTaskExecutor.MANDATORY_MISSING_ATTR_NAME, mandatoryMissing));
                    }
                    if (!mandatoryNullOrEmpty.isEmpty()) {
                        preparedAttrs.getValue().add(AttributeBuilder.build(
                                PropagationTaskExecutor.MANDATORY_NULL_OR_EMPTY_ATTR_NAME, mandatoryNullOrEmpty));
                    }

                    task.setAttributes(preparedAttrs.getValue());
                    tasks.add(task);

                    LOG.debug("PropagationTask created: {}", task);
                }
            }
        }

        return tasks;
    }
}

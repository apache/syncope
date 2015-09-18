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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.patch.AttrPatch;
import org.apache.syncope.common.lib.patch.StringPatchItem;
import org.apache.syncope.common.lib.patch.UserPatch;
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
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.misc.ConnObjectUtils;
import org.apache.syncope.core.misc.MappingUtils;
import org.apache.syncope.core.misc.jexl.JexlUtils;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
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

    protected static final Logger LOG = LoggerFactory.getLogger(PropagationManager.class);

    @Autowired
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
    protected VirAttrHandler virAttrHandler;

    protected Any<?, ?, ?> find(final AnyTypeKind kind, final Long key) {
        AnyDAO<? extends Any<?, ?, ?>> dao;
        switch (kind) {
            case ANY_OBJECT:
                dao = anyObjectDAO;
                break;

            case GROUP:
                dao = groupDAO;
                break;

            case USER:
            default:
                dao = userDAO;
        }

        return dao.authFind(key);
    }

    @Override
    public List<PropagationTask> getCreateTasks(
            final AnyTypeKind kind,
            final Long key,
            final PropagationByResource propByRes,
            final Collection<AttrTO> vAttrs,
            final Collection<String> noPropResourceNames) {

        Any<?, ?, ?> any = find(kind, key);
        if (vAttrs != null && !vAttrs.isEmpty()) {
            virAttrHandler.createVirtual(any, vAttrs);
        }

        return getCreateTasks(any, null, null, propByRes, noPropResourceNames);
    }

    @Override
    public List<PropagationTask> getUserCreateTasks(
            final Long key,
            final String password,
            final Boolean enable,
            final PropagationByResource propByRes,
            final Collection<AttrTO> vAttrs,
            final Collection<String> noPropResourceNames) {

        User user = userDAO.authFind(key);
        if (vAttrs != null && !vAttrs.isEmpty()) {
            virAttrHandler.createVirtual(user, vAttrs);
        }

        return getCreateTasks(user, password, enable, propByRes, noPropResourceNames);
    }

    protected List<PropagationTask> getCreateTasks(
            final Any<?, ?, ?> any,
            final String password,
            final Boolean enable,
            final PropagationByResource propByRes,
            final Collection<String> noPropResourceNames) {

        if (propByRes == null || propByRes.isEmpty()) {
            return Collections.<PropagationTask>emptyList();
        }

        if (noPropResourceNames != null) {
            propByRes.get(ResourceOperation.CREATE).removeAll(noPropResourceNames);
        }

        return createTasks(any, password, true, null, enable, false, propByRes);
    }

    @Override
    public List<PropagationTask> getUpdateTasks(
            final AnyTypeKind kind,
            final Long key,
            final boolean changePwd,
            final Boolean enable,
            final PropagationByResource propByRes,
            final Collection<AttrPatch> vAttrs,
            final Collection<String> noPropResourceNames) {

        return getUpdateTasks(find(kind, key), null, changePwd, enable, propByRes, vAttrs, noPropResourceNames);
    }

    @Override
    public List<PropagationTask> getUserUpdateTasks(
            final WorkflowResult<Pair<UserPatch, Boolean>> wfResult,
            final boolean changePwd,
            final Collection<String> noPropResourceNames) {

        return getUpdateTasks(
                userDAO.authFind(wfResult.getResult().getKey().getKey()),
                wfResult.getResult().getKey().getPassword() == null
                        ? null
                        : wfResult.getResult().getKey().getPassword().getValue(),
                changePwd,
                wfResult.getResult().getValue(),
                wfResult.getPropByRes(),
                wfResult.getResult().getKey().getVirAttrs(),
                noPropResourceNames);
    }

    @Override
    public List<PropagationTask> getUserUpdateTasks(final WorkflowResult<Pair<UserPatch, Boolean>> wfResult) {
        UserPatch userPatch = wfResult.getResult().getKey();

        // Propagate password update only to requested resources
        List<PropagationTask> tasks = new ArrayList<>();
        if (userPatch.getPassword() == null) {
            // a. no specific password propagation request: generate propagation tasks for any resource associated
            tasks = getUserUpdateTasks(wfResult, false, null);
        } else {
            // b. generate the propagation task list in two phases: first the ones containing password,
            // the the rest (with no password)
            PropagationByResource origPropByRes = new PropagationByResource();
            origPropByRes.merge(wfResult.getPropByRes());

            Set<String> pwdResourceNames = new HashSet<>(userPatch.getPassword().getResources());
            Collection<String> currentResourceNames =
                    userDAO.findAllResourceNames(userDAO.authFind(userPatch.getKey()));
            pwdResourceNames.retainAll(currentResourceNames);
            PropagationByResource pwdPropByRes = new PropagationByResource();
            pwdPropByRes.addAll(ResourceOperation.UPDATE, pwdResourceNames);
            if (!pwdPropByRes.isEmpty()) {
                Set<String> toBeExcluded = new HashSet<>(currentResourceNames);
                toBeExcluded.addAll(CollectionUtils.collect(userPatch.getResources(),
                        new Transformer<StringPatchItem, String>() {

                            @Override
                            public String transform(final StringPatchItem input) {
                                return input.getValue();
                            }
                        }));
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

    protected List<PropagationTask> getUpdateTasks(
            final Any<?, ?, ?> any,
            final String password,
            final boolean changePwd,
            final Boolean enable,
            final PropagationByResource propByRes,
            final Collection<AttrPatch> vAttrs,
            final Collection<String> noPropResourceNames) {

        PropagationByResource localPropByRes = virAttrHandler.updateVirtual(
                any,
                vAttrs == null ? Collections.<AttrPatch>emptySet() : vAttrs);

        if (propByRes == null || propByRes.isEmpty()) {
            localPropByRes.addAll(ResourceOperation.UPDATE, any.getResourceNames());
        } else {
            localPropByRes.merge(propByRes);
        }

        if (noPropResourceNames != null) {
            localPropByRes.removeAll(noPropResourceNames);
        }

        Map<String, AttrPatch> vAttrsMap = null;
        if (vAttrs != null) {
            vAttrsMap = new HashMap<>();
            for (AttrPatch attrPatch : vAttrs) {
                vAttrsMap.put(attrPatch.getAttrTO().getSchema(), attrPatch);
            }
        }

        return createTasks(any, password, changePwd, vAttrsMap, enable, false, localPropByRes);
    }

    @Override
    public List<PropagationTask> getDeleteTasks(
            final AnyTypeKind kind,
            final Long key,
            final PropagationByResource propByRes,
            final Collection<String> noPropResourceNames) {

        Any<?, ?, ?> any = find(kind, key);

        PropagationByResource localPropByRes = new PropagationByResource();

        if (propByRes == null || propByRes.isEmpty()) {
            localPropByRes.addAll(ResourceOperation.DELETE, any.getResourceNames());
        } else {
            localPropByRes.merge(propByRes);
        }

        if (noPropResourceNames != null) {
            localPropByRes.removeAll(noPropResourceNames);
        }

        return getDeleteTasks(any, localPropByRes, noPropResourceNames);
    }

    protected List<PropagationTask> getDeleteTasks(
            final Any<?, ?, ?> any,
            final PropagationByResource propByRes,
            final Collection<String> noPropResourceNames) {

        return createTasks(any, null, false, null, false, true, propByRes);
    }

    /**
     * Create propagation tasks.
     *
     * @param any user / group to be provisioned
     * @param password cleartext password to be provisioned
     * @param changePwd whether password should be included for propagation attributes or not
     * @param vAttrs virtual attributes to be maaged
     * @param enable whether user must be enabled or not
     * @param deleteOnResource whether user / group must be deleted anyway from external resource or not
     * @param propByRes operation to be performed per resource
     * @return list of propagation tasks created
     */
    protected List<PropagationTask> createTasks(final Any<?, ?, ?> any,
            final String password, final boolean changePwd,
            final Map<String, AttrPatch> vAttrs,
            final Boolean enable, final boolean deleteOnResource, final PropagationByResource propByRes) {

        LOG.debug("Provisioning any {}:\n{}", any, propByRes);

        if (!propByRes.get(ResourceOperation.CREATE).isEmpty() && vAttrs != null) {
            virAttrHandler.retrieveVirAttrValues(any);

            // update vAttrsToBeUpdated as well
            for (VirAttr<?> virAttr : any.getVirAttrs()) {
                String schema = virAttr.getSchema().getKey();

                vAttrs.put(schema, new AttrPatch.Builder().
                        attrTO(new AttrTO.Builder().schema(schema).values(virAttr.getValues()).build()).
                        build());
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
                } else if (MappingUtils.getMappingItems(provision, MappingPurpose.PROPAGATION).isEmpty()) {
                    LOG.warn("Requesting propagation for {} but no propagation mapping provided for {}",
                            any.getType(), resource);
                } else {
                    PropagationTask task = entityFactory.newEntity(PropagationTask.class);
                    task.setResource(resource);
                    task.setObjectClassName(
                            resource.getProvision(any.getType()).getObjectClass().getObjectClassValue());
                    task.setAnyTypeKind(any.getType().getKind());
                    if (!deleteOnResource) {
                        task.setAnyKey(any.getKey());
                    }
                    task.setPropagationOperation(operation);
                    task.setPropagationMode(resource.getPropagationMode());
                    task.setOldConnObjectKey(propByRes.getOldConnObjectKey(resource.getKey()));

                    Pair<String, Set<Attribute>> preparedAttrs = MappingUtils.prepareAttrs(
                            any, password, changePwd, vAttrs, enable, provision);
                    task.setConnObjectKey(preparedAttrs.getKey());

                    // Check if any of mandatory attributes (in the mapping) is missing or not received any value: 
                    // if so, add special attributes that will be evaluated by PropagationTaskExecutor
                    List<String> mandatoryMissing = new ArrayList<>();
                    List<String> mandatoryNullOrEmpty = new ArrayList<>();
                    for (MappingItem item : MappingUtils.getMappingItems(provision, MappingPurpose.PROPAGATION)) {
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

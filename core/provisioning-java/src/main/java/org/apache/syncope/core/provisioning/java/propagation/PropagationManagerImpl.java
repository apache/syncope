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
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.mod.AttrMod;
import org.apache.syncope.common.lib.mod.MembershipMod;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AttributableUtils;
import org.apache.syncope.core.persistence.api.entity.AttributableUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.MappingItem;
import org.apache.syncope.core.persistence.api.entity.Subject;
import org.apache.syncope.core.persistence.api.entity.VirAttr;
import org.apache.syncope.core.persistence.api.entity.membership.Membership;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.java.VirAttrHandler;
import org.apache.syncope.core.misc.security.UnauthorizedException;
import org.apache.syncope.core.misc.ConnObjectUtils;
import org.apache.syncope.core.misc.MappingUtils;
import org.apache.syncope.core.misc.jexl.JexlUtils;
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
    protected AttributableUtilsFactory attrUtilsFactory;

    @Autowired
    protected VirAttrHandler virAttrHandler;

    @Override
    public List<PropagationTask> getUserCreateTasks(
            final Long key,
            final Boolean enable,
            final PropagationByResource propByRes,
            final String password,
            final Collection<AttrTO> vAttrs,
            final Collection<MembershipTO> membershipTOs,
            final Collection<String> noPropResourceNames)
            throws NotFoundException, UnauthorizedException {

        User user = userDAO.authFetch(key);
        if (vAttrs != null && !vAttrs.isEmpty()) {
            virAttrHandler.fillVirtual(user, vAttrs, attrUtilsFactory.getInstance(AttributableType.USER));
        }
        for (final Membership membership : user.getMemberships()) {
            if (membership.getVirAttrs() != null && !membership.getVirAttrs().isEmpty()) {
                MembershipTO membershipTO = CollectionUtils.find(membershipTOs, new Predicate<MembershipTO>() {

                    @Override
                    public boolean evaluate(final MembershipTO membershipTO) {
                        return membershipTO.getGroupKey() == membership.getGroup().getKey();
                    }
                });
                if (membershipTO != null) {
                    virAttrHandler.fillVirtual(membership,
                            membershipTO.getVirAttrs(), attrUtilsFactory.getInstance(AttributableType.MEMBERSHIP));
                }
            }
        }
        return getCreateTaskIds(user, password, enable, propByRes, noPropResourceNames);
    }

    /**
     * Create the group on every associated resource.
     *
     * @param wfResult group to be propagated (and info associated), as per result from workflow
     * @param vAttrs virtual attributes to be set
     * @param noPropResourceNames external resources performing not to be considered for propagation
     * @return list of propagation tasks
     * @throws NotFoundException if group is not found
     * @throws UnauthorizedException if caller doesn't own enough entitlements to administer the given group
     */
    @Override
    public List<PropagationTask> getGroupCreateTasks(
            final WorkflowResult<Long> wfResult,
            final Collection<AttrTO> vAttrs,
            final Collection<String> noPropResourceNames)
            throws NotFoundException, UnauthorizedException {

        return getGroupCreateTasks(wfResult.getResult(), vAttrs, wfResult.getPropByRes(), noPropResourceNames);
    }

    /**
     * Create the group on every associated resource.
     *
     * @param key group key
     * @param vAttrs virtual attributes to be set
     * @param propByRes operation to be performed per resource
     * @param noPropResourceNames external resources performing not to be considered for propagation
     * @return list of propagation tasks
     * @throws NotFoundException if group is not found
     * @throws UnauthorizedException if caller doesn't own enough entitlements to administer the given group
     */
    @Override
    public List<PropagationTask> getGroupCreateTasks(
            final Long key,
            final Collection<AttrTO> vAttrs,
            final PropagationByResource propByRes,
            final Collection<String> noPropResourceNames)
            throws NotFoundException, UnauthorizedException {

        Group group = groupDAO.authFetch(key);
        if (vAttrs != null && !vAttrs.isEmpty()) {
            virAttrHandler.fillVirtual(group, vAttrs, attrUtilsFactory.getInstance(AttributableType.GROUP));
        }

        return getCreateTaskIds(group, null, null, propByRes, noPropResourceNames);
    }

    protected List<PropagationTask> getCreateTaskIds(final Subject<?, ?, ?> subject,
            final String password, final Boolean enable,
            final PropagationByResource propByRes,
            final Collection<String> noPropResourceNames) {

        if (propByRes == null || propByRes.isEmpty()) {
            return Collections.<PropagationTask>emptyList();
        }

        if (noPropResourceNames != null) {
            propByRes.get(ResourceOperation.CREATE).removeAll(noPropResourceNames);
        }

        return createTasks(subject, password, true, null, null, null, null, enable, false, propByRes);
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
    @Override
    public List<PropagationTask> getUserUpdateTasks(final User user, final Boolean enable,
            final Collection<String> noPropResourceNames) throws NotFoundException {

        return getUpdateTasks(
                user, // user to be updated on external resources
                null, // no password
                false,
                enable, // status to be propagated
                Collections.<String>emptySet(), // no virtual attributes to be managed
                Collections.<AttrMod>emptySet(), // no virtual attributes to be managed
                null, // no propagation by resources
                noPropResourceNames,
                Collections.<MembershipMod>emptySet());
    }

    /**
     * Performs update on each resource associated to the user.
     *
     * @param wfResult user to be propagated (and info associated), as per result from workflow
     * @param changePwd whether password should be included for propagation attributes or not
     * @param noPropResourceNames external resources not to be considered for propagation
     * @return list of propagation tasks
     * @throws NotFoundException if user is not found
     * @throws UnauthorizedException if caller doesn't own enough entitlements to administer the given user
     */
    @Override
    public List<PropagationTask> getUserUpdateTasks(final WorkflowResult<Pair<UserMod, Boolean>> wfResult,
            final boolean changePwd, final Collection<String> noPropResourceNames)
            throws NotFoundException, UnauthorizedException {

        User user = userDAO.authFetch(wfResult.getResult().getKey().getKey());
        return getUpdateTasks(user,
                wfResult.getResult().getKey().getPassword(),
                changePwd,
                wfResult.getResult().getValue(),
                wfResult.getResult().getKey().getVirAttrsToRemove(),
                wfResult.getResult().getKey().getVirAttrsToUpdate(),
                wfResult.getPropByRes(),
                noPropResourceNames,
                wfResult.getResult().getKey().getMembershipsToAdd());
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
            Set<String> currentResourceNames = userDAO.authFetch(userMod.getKey()).getResourceNames();
            pwdResourceNames.retainAll(currentResourceNames);
            PropagationByResource pwdPropByRes = new PropagationByResource();
            pwdPropByRes.addAll(ResourceOperation.UPDATE, pwdResourceNames);
            if (!pwdPropByRes.isEmpty()) {
                Set<String> toBeExcluded = new HashSet<>(currentResourceNames);
                toBeExcluded.addAll(userMod.getResourcesToAdd());
                toBeExcluded.removeAll(pwdResourceNames);
                tasks.addAll(getUserUpdateTasks(wfResult, true, toBeExcluded));
            }

            final PropagationByResource nonPwdPropByRes = new PropagationByResource();
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
            final Set<String> noPropResourceNames)
            throws NotFoundException, UnauthorizedException {

        Group group = groupDAO.authFetch(wfResult.getResult());
        return getUpdateTasks(group, null, false, null,
                vAttrsToBeRemoved, vAttrsToBeUpdated, wfResult.getPropByRes(), noPropResourceNames,
                Collections.<MembershipMod>emptySet());
    }

    @Override
    public List<PropagationTask> getUpdateTasks(final Subject<?, ?, ?> subject,
            final String password, final boolean changePwd, final Boolean enable,
            final Set<String> vAttrsToBeRemoved, final Set<AttrMod> vAttrsToBeUpdated,
            final PropagationByResource propByRes, final Collection<String> noPropResourceNames,
            final Set<MembershipMod> membershipsToAdd)
            throws NotFoundException {

        PropagationByResource localPropByRes = virAttrHandler.fillVirtual(subject, vAttrsToBeRemoved == null
                ? Collections.<String>emptySet()
                : vAttrsToBeRemoved, vAttrsToBeUpdated == null
                        ? Collections.<AttrMod>emptySet()
                        : vAttrsToBeUpdated, attrUtilsFactory.getInstance(subject));

        // SYNCOPE-458 fill membership virtual attributes
        if (subject instanceof User) {
            final User user = (User) subject;
            for (final Membership membership : user.getMemberships()) {
                if (membership.getVirAttrs() != null && !membership.getVirAttrs().isEmpty()) {
                    final MembershipMod membershipMod = CollectionUtils.find(membershipsToAdd,
                            new Predicate<MembershipMod>() {

                                @Override
                                public boolean evaluate(final MembershipMod membershipMod) {
                                    return membershipMod.getGroup() == membership.getGroup().getKey();
                                }
                            });
                    if (membershipMod != null) {
                        virAttrHandler.fillVirtual(membership, membershipMod.getVirAttrsToRemove() == null
                                ? Collections.<String>emptySet()
                                : membershipMod.getVirAttrsToRemove(),
                                membershipMod.getVirAttrsToUpdate() == null ? Collections.<AttrMod>emptySet()
                                        : membershipMod.getVirAttrsToUpdate(), attrUtilsFactory.getInstance(
                                        AttributableType.MEMBERSHIP));
                    }
                }
            }
        }

        if (propByRes == null || propByRes.isEmpty()) {
            localPropByRes.addAll(ResourceOperation.UPDATE, subject.getResourceNames());
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

        // SYNCOPE-458 fill membership virtual attributes to be updated map
        Map<String, AttrMod> membVAttrsToBeUpdatedMap = new HashMap<>();
        for (MembershipMod membershipMod : membershipsToAdd) {
            for (AttrMod attrMod : membershipMod.getVirAttrsToUpdate()) {
                membVAttrsToBeUpdatedMap.put(attrMod.getSchema(), attrMod);
            }
        }

        // SYNCOPE-458 fill membership virtual attributes to be removed set
        final Set<String> membVAttrsToBeRemoved = new HashSet<>();
        for (MembershipMod membershipMod : membershipsToAdd) {
            membVAttrsToBeRemoved.addAll(membershipMod.getVirAttrsToRemove());
        }

        return createTasks(subject, password, changePwd,
                vAttrsToBeRemoved, vAttrsToBeUpdatedMap, membVAttrsToBeRemoved, membVAttrsToBeUpdatedMap, enable, false,
                localPropByRes);
    }

    @Override
    public List<PropagationTask> getUserDeleteTasks(final Long userKey, final Collection<String> noPropResourceNames)
            throws NotFoundException, UnauthorizedException {

        User user = userDAO.authFetch(userKey);
        return getDeleteTaskIds(user, user.getResourceNames(), noPropResourceNames);
    }

    @Override
    public List<PropagationTask> getUserDeleteTasks(
            final Long userKey, final Set<String> resourceNames, final Collection<String> noPropResourceNames)
            throws NotFoundException, UnauthorizedException {

        User user = userDAO.authFetch(userKey);
        return getDeleteTaskIds(user, resourceNames, noPropResourceNames);
    }

    @Override
    public List<PropagationTask> getUserDeleteTasks(final WorkflowResult<Long> wfResult) {
        User user = userDAO.authFetch(wfResult.getResult());
        return createTasks(user, null, false, null, null, null, null, false, true, wfResult.getPropByRes());
    }

    @Override
    public List<PropagationTask> getGroupDeleteTasks(final Long groupKey)
            throws NotFoundException, UnauthorizedException {

        return getGroupDeleteTasks(groupKey, Collections.<String>emptySet());
    }

    @Override
    public List<PropagationTask> getGroupDeleteTasks(final Long groupKey, final String noPropResourceName)
            throws NotFoundException, UnauthorizedException {

        return getGroupDeleteTasks(groupKey, Collections.<String>singleton(noPropResourceName));
    }

    @Override
    public List<PropagationTask> getGroupDeleteTasks(
            final Long groupKey, final Collection<String> noPropResourceNames)
            throws NotFoundException, UnauthorizedException {

        Group group = groupDAO.authFetch(groupKey);
        return getDeleteTaskIds(group, group.getResourceNames(), noPropResourceNames);
    }

    @Override
    public List<PropagationTask> getGroupDeleteTasks(
            final Long groupKey, final Set<String> resourceNames, final Collection<String> noPropResourceNames)
            throws NotFoundException, UnauthorizedException {

        Group group = groupDAO.authFetch(groupKey);
        return getDeleteTaskIds(group, resourceNames, noPropResourceNames);
    }

    protected List<PropagationTask> getDeleteTaskIds(
            final Subject<?, ?, ?> subject,
            final Set<String> resourceNames,
            final Collection<String> noPropResourceNames) {

        final PropagationByResource propByRes = new PropagationByResource();
        propByRes.set(ResourceOperation.DELETE, resourceNames);
        if (noPropResourceNames != null && !noPropResourceNames.isEmpty()) {
            propByRes.get(ResourceOperation.DELETE).removeAll(noPropResourceNames);
        }
        return createTasks(subject, null, false, null, null, null, null, false, true, propByRes);
    }

    /**
     * Create propagation tasks.
     *
     * @param subject user / group to be provisioned
     * @param password cleartext password to be provisioned
     * @param changePwd whether password should be included for propagation attributes or not
     * @param vAttrsToBeRemoved virtual attributes to be removed
     * @param vAttrsToBeUpdated virtual attributes to be added
     * @param membVAttrsToBeRemoved membership virtual attributes to be removed
     * @param membVAttrsToBeUpdatedMap membership virtual attributes to be added
     * @param enable whether user must be enabled or not
     * @param deleteOnResource whether user / group must be deleted anyway from external resource or not
     * @param propByRes operation to be performed per resource
     * @return list of propagation tasks created
     */
    protected List<PropagationTask> createTasks(final Subject<?, ?, ?> subject,
            final String password, final boolean changePwd,
            final Set<String> vAttrsToBeRemoved, final Map<String, AttrMod> vAttrsToBeUpdated,
            final Set<String> membVAttrsToBeRemoved, final Map<String, AttrMod> membVAttrsToBeUpdatedMap,
            final Boolean enable, final boolean deleteOnResource, final PropagationByResource propByRes) {

        LOG.debug("Provisioning subject {}:\n{}", subject, propByRes);

        final AttributableUtils attrUtils = attrUtilsFactory.getInstance(subject);

        if (!propByRes.get(ResourceOperation.CREATE).isEmpty()
                && vAttrsToBeRemoved != null && vAttrsToBeUpdated != null) {

            connObjectUtils.retrieveVirAttrValues(subject, attrUtils);

            // update vAttrsToBeUpdated as well
            for (VirAttr virAttr : subject.getVirAttrs()) {
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

        final List<PropagationTask> tasks = new ArrayList<>();

        for (ResourceOperation operation : ResourceOperation.values()) {
            for (String resourceName : propByRes.get(operation)) {
                final ExternalResource resource = resourceDAO.find(resourceName);
                if (resource == null) {
                    LOG.error("Invalid resource name specified: {}, ignoring...", resourceName);
                } else if (attrUtils.getMappingItems(resource, MappingPurpose.PROPAGATION).isEmpty()) {
                    LOG.warn("Requesting propagation for {} but no propagation mapping provided for {}",
                            attrUtils.getType(), resource);
                } else {
                    PropagationTask task = entityFactory.newEntity(PropagationTask.class);
                    task.setResource(resource);
                    task.setObjectClassName(connObjectUtils.fromSubject(subject).getObjectClassValue());
                    task.setSubjectType(attrUtils.getType());
                    if (!deleteOnResource) {
                        task.setSubjectKey(subject.getKey());
                    }
                    task.setPropagationOperation(operation);
                    task.setPropagationMode(resource.getPropagationMode());
                    task.setOldAccountId(propByRes.getOldAccountId(resource.getKey()));

                    Pair<String, Set<Attribute>> preparedAttrs = MappingUtils.prepareAttributes(attrUtils, subject,
                            password, changePwd, vAttrsToBeRemoved, vAttrsToBeUpdated, membVAttrsToBeRemoved,
                            membVAttrsToBeUpdatedMap, enable, resource);
                    task.setAccountId(preparedAttrs.getKey());

                    // Check if any of mandatory attributes (in the mapping) is missing or not received any value: 
                    // if so, add special attributes that will be evaluated by PropagationTaskExecutor
                    List<String> mandatoryMissing = new ArrayList<>();
                    List<String> mandatoryNullOrEmpty = new ArrayList<>();
                    for (MappingItem item : attrUtils.getMappingItems(resource, MappingPurpose.PROPAGATION)) {
                        if (!item.isAccountid()
                                && JexlUtils.evaluateMandatoryCondition(item.getMandatoryCondition(), subject)) {

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

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
package org.apache.syncope.core.sync.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.syncope.common.mod.AbstractAttributableMod;
import org.apache.syncope.common.mod.MembershipMod;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.core.notification.NotificationManager;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.SyncTask;
import org.apache.syncope.core.persistence.beans.membership.Membership;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.propagation.ConnectorFactory;
import org.apache.syncope.core.propagation.PropagationException;
import org.apache.syncope.core.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.propagation.SyncopeConnector;
import org.apache.syncope.core.propagation.impl.PropagationManager;
import org.apache.syncope.core.sync.DefaultSyncActions;
import org.apache.syncope.core.sync.SyncResult;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.apache.syncope.core.workflow.user.UserWorkflowAdapter;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Simple action for synchronizing LDAP groups memberships to Syncope role memberships, when the same resource is
 * configured for both users and roles.
 *
 * @see org.apache.syncope.core.propagation.impl.LDAPMembershipPropagationActions
 */
public class LDAPMembershipSyncActions extends DefaultSyncActions {

    protected static final Logger LOG = LoggerFactory.getLogger(LDAPMembershipSyncActions.class);

    @Autowired
    protected RoleDAO roleDAO;

    @Autowired
    protected ConnectorFactory connInstanceLoader;

    @Autowired
    protected UserWorkflowAdapter uwfAdapter;

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    private PropagationTaskExecutor taskExecutor;

    @Autowired
    private NotificationManager notificationManager;

    protected Map<Long, Long> membersBeforeRoleUpdate = Collections.<Long, Long>emptyMap();

    /**
     * Allows easy subclassing for the ConnId AD connector bundle.
     *
     * @return the name of the attribute used to keep track of group memberships
     */
    protected String getGroupMembershipAttrName() {
        return "uniquemember";
    }

    /**
     * Keep track of members of the role being updated <b>before</b> actual update takes place. This is not needed on
     * <ul> <li>beforeCreate() - because the synchronizing role does not exist yet on Syncope</li> <li>beforeDelete() -
     * because role delete cascades as membership removal for all users involved</li> </ul>
     *
     * {@inheritDoc}
     */
    @Transactional(readOnly = true)
    @Override
    public <T extends AbstractAttributableTO, K extends AbstractAttributableMod> SyncDelta beforeUpdate(
            final SyncResultsHandler handler, final SyncDelta delta, final T subject, final K subjectMod)
            throws JobExecutionException {

        if (subject instanceof RoleTO) {
            // search for all users assigned to given role
            SyncopeRole role = roleDAO.find(subject.getId());
            if (role != null) {
                List<Membership> membs = roleDAO.findMemberships(role);
                // save memberships before role update takes place
                membersBeforeRoleUpdate = new HashMap<Long, Long>(membs.size());
                for (Membership memb : membs) {
                    membersBeforeRoleUpdate.put(memb.getSyncopeUser().getId(), memb.getId());
                }
            }
        }

        return super.beforeUpdate(handler, delta, subject, subjectMod);
    }

    /**
     * Build UserMod for adding membership to given user, for given role.
     *
     * @param userId user to be assigned membership to given role
     * @param roleTO role for adding membership
     * @return UserMod for user update
     */
    protected UserMod getUserMod(final Long userId, final RoleTO roleTO) {
        UserMod userMod = new UserMod();
        // no actual modification takes place when user has already the role assigned
        if (membersBeforeRoleUpdate.containsKey(userId)) {
            membersBeforeRoleUpdate.remove(userId);
        } else {
            userMod.setId(userId);

            MembershipMod membershipMod = new MembershipMod();
            membershipMod.setRole(roleTO.getId());
            userMod.addMembershipToBeAdded(membershipMod);
        }

        return userMod;
    }

    /**
     * Read values of attribute returned by getGroupMembershipAttrName(); if not present in the given delta, perform an
     * additioanl read on the underlying connector.
     *
     * @param delta representing the synchronizing role
     * @param connector associated to the current resource
     * @return value of attribute returned by getGroupMembershipAttrName()
     * @see getGroupMembershipAttrName()
     */
    protected List<Object> getMembAttrValues(final SyncDelta delta, final SyncopeConnector connector) {
        List<Object> result = Collections.<Object>emptyList();

        // first, try to read the configured attribute from delta, returned by the ongoing synchronization
        Attribute membAttr = delta.getObject().getAttributeByName(getGroupMembershipAttrName());
        // if not found, perform an additional read on the underlying connector for the same connector object
        if (membAttr == null) {
            final OperationOptionsBuilder oob = new OperationOptionsBuilder();
            oob.setAttributesToGet(getGroupMembershipAttrName());
            membAttr = connector.getObjectAttribute(
                    ObjectClass.GROUP, delta.getUid(), oob.build(), getGroupMembershipAttrName());
        }
        if (membAttr != null && membAttr.getValue() != null) {
            result = membAttr.getValue();
        }

        return result;
    }

    /**
     * Perform actual modifications (i.e. membership add / remove) for the given role on the given resource.
     *
     * @param userMod modifications to perform on the user
     * @param resourceName resource to be propagated for changes
     */
    protected void userUpdate(final UserMod userMod, final String resourceName) {
        if (userMod.getId() == 0) {
            return;
        }

        try {
            WorkflowResult<Map.Entry<Long, Boolean>> updated = uwfAdapter.update(userMod);

            List<PropagationTask> tasks = propagationManager.getUserUpdateTaskIds(updated,
                    userMod.getPassword(), userMod.getVirtualAttributesToBeRemoved(),
                    userMod.getVirtualAttributesToBeUpdated(),
                    Collections.singleton(resourceName));

            taskExecutor.execute(tasks);

            notificationManager.createTasks(updated.getResult().getKey(), updated.getPerformedTasks());
        } catch (PropagationException e) {
            LOG.error("Could not propagate {}", userMod, e);
        } catch (Exception e) {
            LOG.error("Could not perform update {}", userMod, e);
        }
    }

    /**
     * Synchronize Syncope memberships with the situation read on the external resource's group.
     *
     * @param handler syncope sync result handler
     * @param delta representing the synchronizing role
     * @param roleTO role after modification performed by the handler
     * @throws JobExecutionException if anything goes wrong
     */
    protected void synchronizeMemberships(final SyncopeSyncResultHandler handler, final SyncDelta delta,
            final RoleTO roleTO) throws JobExecutionException {

        final SyncTask task = handler.getSyncTask();
        final ExternalResource resource = task.getResource();

        SyncopeConnector connector;
        try {
            connector = connInstanceLoader.getConnector(resource);
        } catch (Exception e) {
            final String msg = String.format("Connector instance bean for resource %s and connInstance %s not found",
                    resource, resource.getConnector());

            throw new JobExecutionException(msg, e);
        }

        for (Object membValue : getMembAttrValues(delta, connector)) {

            final List<ConnectorObject> found = connector.search(ObjectClass.ACCOUNT,
                    new EqualsFilter(new Name(membValue.toString())),
                    connector.getOperationOptions(resource.getUmapping().getItems()));

            if (found.isEmpty()) {
                LOG.debug("No account found on {} with __NAME__ {}", resource, membValue.toString());
            } else {
                if (found.size() > 1) {
                    LOG.warn("More than one account found on {} with __NAME__ {} - taking first only",
                            resource, membValue.toString());
                }

                ConnectorObject externalAccount = found.iterator().next();
                final List<Long> userIds = handler.findExisting(externalAccount.getUid().getUidValue(),
                        externalAccount, AttributableUtil.getInstance(AttributableType.USER));
                if (userIds.isEmpty()) {
                    LOG.debug("No matching user found for {}, aborting", externalAccount);
                } else {
                    if (userIds.size() > 1) {
                        LOG.warn("More than one user found {} - taking first only", userIds);
                    }

                    UserMod userMod = getUserMod(userIds.iterator().next(), roleTO);
                    userUpdate(userMod, resource.getName());
                }
            }
        }

        // finally remove any residual membership that was present before role update but not any more
        for (Map.Entry<Long, Long> member : membersBeforeRoleUpdate.entrySet()) {
            UserMod userMod = new UserMod();
            userMod.setId(member.getKey());
            userMod.addMembershipToBeRemoved(member.getValue());
            userUpdate(userMod, resource.getName());
        }
    }

    /**
     * Synchronize membership at role synchronization time (because SyncJob first synchronize users then roles).
     * {@inheritDoc}
     */
    @Override
    public <T extends AbstractAttributableTO> void after(final SyncResultsHandler handler, final SyncDelta delta,
            final T subject, final SyncResult result) throws JobExecutionException {

        if (!(handler instanceof SyncopeSyncResultHandler)) {
            return;
        }

        SyncopeSyncResultHandler intHandler = (SyncopeSyncResultHandler) handler;
        if (!(subject instanceof RoleTO) || intHandler.getSyncTask().getResource().getUmapping() == null) {
            super.after(handler, delta, subject, result);
        } else {
            synchronizeMemberships(intHandler, delta, (RoleTO) subject);
        }
    }
}

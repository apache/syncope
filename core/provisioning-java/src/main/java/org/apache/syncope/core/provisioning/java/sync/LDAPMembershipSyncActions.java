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
package org.apache.syncope.core.provisioning.java.sync;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.patch.MembershipPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.AuditElements.Result;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.ProvisioningTask;
import org.apache.syncope.core.persistence.api.entity.task.SyncTask;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.propagation.PropagationException;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.api.sync.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.sync.ProvisioningReport;
import org.apache.syncope.core.misc.AuditManager;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Simple action for synchronizing LDAP groups memberships to Syncope group memberships, when the same resource is
 * configured for both users and groups.
 *
 * @see org.apache.syncope.core.provisioning.java.propagation.LDAPMembershipPropagationActions
 */
public class LDAPMembershipSyncActions extends DefaultSyncActions {

    protected static final Logger LOG = LoggerFactory.getLogger(LDAPMembershipSyncActions.class);

    @Autowired
    protected AnyTypeDAO anyTypeDAO;

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected GroupDAO groupDAO;

    @Autowired
    protected UserWorkflowAdapter uwfAdapter;

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    private PropagationTaskExecutor taskExecutor;

    @Autowired
    private NotificationManager notificationManager;

    @Autowired
    private AuditManager auditManager;

    @Autowired
    private SyncUtils syncUtils;

    protected Map<Long, Long> membersBeforeGroupUpdate = Collections.<Long, Long>emptyMap();

    /**
     * Allows easy subclassing for the ConnId AD connector bundle.
     *
     * @param connector A Connector instance to query for the groupMemberAttribute property name
     * @return the name of the attribute used to keep track of group memberships
     */
    protected String getGroupMembershipAttrName(final Connector connector) {
        ConnConfProperty groupMembership = IterableUtils.find(connector.getConnInstance().getConf(),
                new Predicate<ConnConfProperty>() {

                    @Override
                    public boolean evaluate(final ConnConfProperty property) {
                        return "groupMemberAttribute".equals(property.getSchema().getName())
                        && property.getValues() != null && !property.getValues().isEmpty();
                    }
                });

        return groupMembership == null
                ? "uniquemember"
                : (String) groupMembership.getValues().get(0);
    }

    /**
     * Keep track of members of the group being updated <b>before</b> actual update takes place. This is not needed on
     * <ul> <li>beforeProvision() - because the synchronizing group does not exist yet on Syncope</li>
     * <li>beforeDelete() - because group delete cascades as membership removal for all users involved</li> </ul>
     *
     * {@inheritDoc}
     */
    @Override
    public <A extends AnyTO, M extends AnyPatch> SyncDelta beforeUpdate(
            final ProvisioningProfile<?, ?> profile,
            final SyncDelta delta, final A any, final M anyPatch) throws JobExecutionException {

        if (any instanceof GroupTO) {
            // search for all users assigned to given group
            Group group = groupDAO.find(any.getKey());
            if (group != null) {
                List<UMembership> membs = groupDAO.findUMemberships(group);
                // save memberships before group update takes place
                membersBeforeGroupUpdate = new HashMap<>(membs.size());
                for (UMembership memb : membs) {
                    membersBeforeGroupUpdate.put(memb.getLeftEnd().getKey(), memb.getKey());
                }
            }
        }

        return super.beforeUpdate(profile, delta, any, anyPatch);
    }

    /**
     * Build UserPatch for adding membership to given user, for given group.
     *
     * @param userKey user to be assigned membership to given group
     * @param groupTO group for adding membership
     * @return UserPatch for user update
     */
    protected UserPatch getUserPatch(final Long userKey, final GroupTO groupTO) {
        UserPatch userPatch = new UserPatch();
        // no actual modification takes place when user has already the group assigned
        if (membersBeforeGroupUpdate.containsKey(userKey)) {
            membersBeforeGroupUpdate.remove(userKey);
        } else {
            userPatch.setKey(userKey);

            userPatch.getMemberships().add(
                    new MembershipPatch.Builder().
                    operation(PatchOperation.ADD_REPLACE).
                    membershipTO(new MembershipTO.Builder().group(groupTO.getKey(), null).build()).
                    build());
        }

        return userPatch;
    }

    /**
     * Read values of attribute returned by getGroupMembershipAttrName(); if not present in the given delta, perform an
     * additional read on the underlying connector.
     *
     * @param delta representing the synchronizing group
     * @param connector associated to the current resource
     * @return value of attribute returned by
     * {@link #getGroupMembershipAttrName}
     */
    protected List<Object> getMembAttrValues(final SyncDelta delta, final Connector connector) {
        List<Object> result = Collections.<Object>emptyList();
        String groupMemberName = getGroupMembershipAttrName(connector);

        // first, try to read the configured attribute from delta, returned by the ongoing synchronization
        Attribute membAttr = delta.getObject().getAttributeByName(groupMemberName);
        // if not found, perform an additional read on the underlying connector for the same connector object
        if (membAttr == null) {
            OperationOptionsBuilder oob = new OperationOptionsBuilder();
            oob.setAttributesToGet(groupMemberName);
            ConnectorObject remoteObj = connector.getObject(ObjectClass.GROUP, delta.getUid(), oob.build());
            if (remoteObj == null) {
                LOG.debug("Object for '{}' not found", delta.getUid().getUidValue());
            } else {
                membAttr = remoteObj.getAttributeByName(groupMemberName);
            }
        }
        if (membAttr != null && membAttr.getValue() != null) {
            result = membAttr.getValue();
        }

        return result;
    }

    /**
     * Perform actual modifications (i.e. membership add / remove) for the given group on the given resource.
     *
     * @param userPatch modifications to perform on the user
     * @param resourceName resource to be propagated for changes
     */
    protected void userUpdate(final UserPatch userPatch, final String resourceName) {
        if (userPatch.getKey() == 0) {
            return;
        }

        Result result;

        WorkflowResult<Pair<UserPatch, Boolean>> updated = null;

        try {
            updated = uwfAdapter.update(userPatch);

            List<PropagationTask> tasks = propagationManager.getUserUpdateTasks(
                    updated, false, Collections.singleton(resourceName));

            taskExecutor.execute(tasks);
            result = Result.SUCCESS;
        } catch (PropagationException e) {
            result = Result.FAILURE;
            LOG.error("Could not propagate {}", userPatch, e);
        } catch (Exception e) {
            result = Result.FAILURE;
            LOG.error("Could not perform update {}", userPatch, e);
        }

        notificationManager.createTasks(
                AuditElements.EventCategoryType.SYNCHRONIZATION,
                this.getClass().getSimpleName(),
                null,
                "update",
                result,
                null, // searching for before object is too much expensive ... 
                updated == null ? null : updated.getResult().getKey(),
                userPatch,
                resourceName);

        auditManager.audit(
                AuditElements.EventCategoryType.SYNCHRONIZATION,
                this.getClass().getSimpleName(),
                null,
                "update",
                result,
                null, // searching for before object is too much expensive ... 
                updated == null ? null : updated.getResult().getKey(),
                userPatch,
                resourceName);
    }

    /**
     * Synchronize Syncope memberships with the situation read on the external resource's group.
     *
     * @param profile sync profile
     * @param delta representing the synchronizing group
     * @param groupTO group after modification performed by the handler
     * @throws JobExecutionException if anything goes wrong
     */
    protected void synchronizeMemberships(
            final ProvisioningProfile<?, ?> profile, final SyncDelta delta, final GroupTO groupTO)
            throws JobExecutionException {

        ProvisioningTask task = profile.getTask();
        ExternalResource resource = task.getResource();
        Connector connector = profile.getConnector();

        for (Object membValue : getMembAttrValues(delta, connector)) {
            Long userKey = syncUtils.findMatchingAnyKey(
                    anyTypeDAO.findUser(),
                    membValue.toString(),
                    profile.getTask().getResource(),
                    profile.getConnector());
            if (userKey != null) {
                UserPatch userPatch = getUserPatch(userKey, groupTO);
                userUpdate(userPatch, resource.getKey());
            }
        }

        // finally remove any residual membership that was present before group update but not any more
        for (Map.Entry<Long, Long> member : membersBeforeGroupUpdate.entrySet()) {
            UserPatch userPatch = new UserPatch();
            userPatch.setKey(member.getKey());

            userPatch.getMemberships().add(
                    new MembershipPatch.Builder().
                    operation(PatchOperation.DELETE).
                    membershipTO(new MembershipTO.Builder().group(groupTO.getKey(), null).build()).
                    build());

            userUpdate(userPatch, resource.getKey());
        }
    }

    /**
     * Synchronize membership at group synchronization time (because SyncJob first synchronize users then groups).
     * {@inheritDoc}
     */
    @Override
    public <A extends AnyTO> void after(
            final ProvisioningProfile<?, ?> profile,
            final SyncDelta delta,
            final A any,
            final ProvisioningReport result) throws JobExecutionException {

        if (!(profile.getTask() instanceof SyncTask)) {
            return;
        }

        if (!(any instanceof GroupTO)
                || profile.getTask().getResource().getProvision(anyTypeDAO.findUser()) == null
                || profile.getTask().getResource().getProvision(anyTypeDAO.findUser()).getMapping() == null) {

            super.after(profile, delta, any, result);
        } else {
            synchronizeMemberships(profile, delta, (GroupTO) any);
        }
    }
}

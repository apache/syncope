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
package org.apache.syncope.core.provisioning.java.pushpull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningReport;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.provisioning.java.job.SetUMembershipsJob;

/**
 * Simple action for pulling LDAP groups memberships to Syncope group memberships, when the same resource is
 * configured for both users and groups.
 *
 * @see org.apache.syncope.core.provisioning.java.propagation.LDAPMembershipPropagationActions
 */
public class LDAPMembershipPullActions extends SchedulingPullActions {

    protected static final Logger LOG = LoggerFactory.getLogger(LDAPMembershipPullActions.class);

    @Autowired
    protected AnyTypeDAO anyTypeDAO;

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected GroupDAO groupDAO;

    @Autowired
    private PullUtils pullUtils;

    protected final Map<String, Set<String>> memberships = new HashMap<>();

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
     * Read values of attribute returned by getGroupMembershipAttrName(); if not present in the given delta, perform an
     * additional read on the underlying connector.
     *
     * @param delta representing the pulling group
     * @param connector associated to the current resource
     * @return value of attribute returned by
     * {@link #getGroupMembershipAttrName}
     */
    protected List<Object> getMembAttrValues(final SyncDelta delta, final Connector connector) {
        List<Object> result = Collections.<Object>emptyList();
        String groupMemberName = getGroupMembershipAttrName(connector);

        // first, try to read the configured attribute from delta, returned by the ongoing pull
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
     * Pull Syncope memberships with the situation read on the external resource's group.
     *
     * @param profile pull profile
     * @param delta representing the pullong group
     * @param groupTO group after modification performed by the handler
     * @throws JobExecutionException if anything goes wrong
     */
    protected void populateMemberships(
            final ProvisioningProfile<?, ?> profile, final SyncDelta delta, final GroupTO groupTO)
            throws JobExecutionException {

        Connector connector = profile.getConnector();
        for (Object membValue : getMembAttrValues(delta, connector)) {
            Set<String> memb = memberships.get(membValue.toString());
            if (memb == null) {
                memb = new HashSet<>();
                memberships.put(membValue.toString(), memb);
            }
            memb.add(groupTO.getKey());
        }
    }

    /**
     * Pull membership at group pull time (because PullJob first pulls users then groups).
     * {@inheritDoc}
     */
    @Override
    public void after(
            final ProvisioningProfile<?, ?> profile,
            final SyncDelta delta,
            final EntityTO entity,
            final ProvisioningReport result) throws JobExecutionException {

        if (!(profile.getTask() instanceof PullTask)) {
            return;
        }

        if (!(entity instanceof GroupTO)
                || profile.getTask().getResource().getProvision(anyTypeDAO.findUser()) == null
                || profile.getTask().getResource().getProvision(anyTypeDAO.findUser()).getMapping() == null) {

            super.after(profile, delta, entity, result);
        } else {
            populateMemberships(profile, delta, (GroupTO) entity);
        }
    }

    @Override
    public void afterAll(final ProvisioningProfile<?, ?> profile) throws JobExecutionException {
        Map<String, Set<String>> resolvedMemberships = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : this.memberships.entrySet()) {
            String userKey = pullUtils.findMatchingAnyKey(
                    anyTypeDAO.findUser(),
                    entry.getKey(),
                    profile.getTask().getResource(),
                    profile.getConnector());
            if (userKey == null) {
                LOG.warn("Could not find matching user for {}", entry.getKey());
            } else {
                resolvedMemberships.put(userKey, entry.getValue());
            }
        }

        Map<String, Object> jobMap = new HashMap<>();
        jobMap.put(SetUMembershipsJob.MEMBERSHIPS_KEY, resolvedMemberships);
        schedule(SetUMembershipsJob.class, jobMap);
    }
}

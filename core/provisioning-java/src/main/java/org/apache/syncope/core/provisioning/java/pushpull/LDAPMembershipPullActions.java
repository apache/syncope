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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.request.MembershipUR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.api.pushpull.InboundActions;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.spring.implementation.InstanceScope;
import org.apache.syncope.core.spring.implementation.SyncopeImplementation;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.LiveSyncDelta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Simple action for pulling LDAP groups memberships to Syncope group memberships, when the same resource is
 * configured for both users and groups.
 *
 * @see org.apache.syncope.core.provisioning.java.propagation.LDAPMembershipPropagationActions
 */
@SyncopeImplementation(scope = InstanceScope.PER_CONTEXT)
public class LDAPMembershipPullActions implements InboundActions {

    protected static final Logger LOG = LoggerFactory.getLogger(LDAPMembershipPullActions.class);

    @Autowired
    protected AnyTypeDAO anyTypeDAO;

    @Autowired
    protected GroupDAO groupDAO;

    @Autowired
    protected InboundMatcher inboundMatcher;

    @Autowired
    protected UserProvisioningManager userProvisioningManager;

    protected final Map<String, Set<String>> membershipsBefore = new ConcurrentHashMap<>();

    protected final Map<String, Set<String>> membershipsAfter = new ConcurrentHashMap<>();

    /**
     * Allows easy subclassing for the ConnId AD connector bundle.
     *
     * @param connector A Connector instance to query for the groupMemberAttribute property name
     * @return the name of the attribute used to keep track of group memberships
     */
    protected String getGroupMembershipAttrName(final Connector connector) {
        return connector.getConnInstance().getConf().stream().
                filter(property -> "groupMemberAttribute".equals(property.getSchema().getName())
                && !property.getValues().isEmpty()).findFirst().
                map(groupMembership -> (String) groupMembership.getValues().getFirst()).
                orElse("uniqueMember");
    }

    @Override
    public Set<String> moreAttrsToGet(final ProvisioningProfile<?, ?> profile, final Provision provision) {
        if (!AnyTypeKind.GROUP.name().equals(provision.getAnyType())) {
            return InboundActions.super.moreAttrsToGet(profile, provision);
        }

        return Set.of(getGroupMembershipAttrName(profile.getConnector()));
    }

    /**
     * Keep track of members of the group being updated before actual update takes place.
     * This is not needed on
     * <ul>
     * <li>{@link #beforeProvision} because the pulling group does not exist yet on Syncope</li>
     * <li>{@link #beforeDelete} because group delete cascades as membership removal for all users involved</li>
     * </ul>
     *
     * {@inheritDoc}
     */
    @Transactional(readOnly = true)
    @Override
    public void beforeUpdate(
            final ProvisioningProfile<?, ?> profile,
            final LiveSyncDelta delta,
            final EntityTO entity,
            final AnyUR anyUR) throws JobExecutionException {

        if (!(entity instanceof GroupTO)) {
            InboundActions.super.beforeUpdate(profile, delta, entity, anyUR);
            return;
        }

        groupDAO.findUMemberships(groupDAO.findById(entity.getKey()).
                orElseThrow(() -> new NotFoundException("Group " + entity.getKey()))).
                forEach(uMembership -> {
                    Set<String> memb = membershipsBefore.computeIfAbsent(
                            uMembership.getLeftEnd().getKey(),
                            k -> Collections.synchronizedSet(new HashSet<>()));
                    memb.add(entity.getKey());
                });
    }

    /**
     * Read values of attribute returned by {@link #getGroupMembershipAttrName}.
     *
     * @param delta representing the pulling group
     * @param connector associated to the current resource
     * @return value of attribute returned by {@link #getGroupMembershipAttrName}
     */
    protected List<Object> getMembAttrValues(final LiveSyncDelta delta, final Connector connector) {
        return Optional.ofNullable(delta.getObject().getAttributeByName(getGroupMembershipAttrName(connector))).
                map(Attribute::getValue).filter(Objects::nonNull).
                orElseGet(() -> List.of());
    }

    /**
     * Keep track of members of the group being updated after actual update took place.
     * {@inheritDoc}
     */
    @Override
    public void after(
            final ProvisioningProfile<?, ?> profile,
            final LiveSyncDelta delta,
            final EntityTO entity,
            final ProvisioningReport result) throws JobExecutionException {

        if (!(entity instanceof GroupTO)) {
            InboundActions.super.after(profile, delta, entity, result);
            return;
        }

        Optional<Provision> provision = profile.getTask().getResource().
                getProvisionByAnyType(AnyTypeKind.USER.name()).filter(p -> p.getMapping() != null);
        if (provision.isEmpty()) {
            InboundActions.super.after(profile, delta, entity, result);
            return;
        }

        getMembAttrValues(delta, profile.getConnector()).forEach(membValue -> inboundMatcher.match(
                anyTypeDAO.getUser(),
                membValue.toString(),
                profile.getTask().getResource(),
                profile.getConnector()).ifPresentOrElse(
                match -> {
                    Set<String> memb = membershipsAfter.computeIfAbsent(
                            match.getAny().getKey(),
                            k -> Collections.synchronizedSet(new HashSet<>()));
                    memb.add(entity.getKey());
                },
                () -> LOG.warn("Could not find matching user for {}", membValue)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void afterAll(final ProvisioningProfile<?, ?> profile) {
        List<UserUR> updateReqs = new ArrayList<>();

        membershipsAfter.forEach((user, groups) -> {
            UserUR userUR = new UserUR();
            userUR.setKey(user);
            updateReqs.add(userUR);

            Set<String> before = membershipsBefore.getOrDefault(user, Set.of());
            groups.stream().filter(group -> !before.contains(group)).
                    forEach(group -> userUR.getMemberships().add(new MembershipUR.Builder(group).
                    operation(PatchOperation.ADD_REPLACE).
                    build()));
        });

        membershipsBefore.forEach((user, groups) -> {
            UserUR userUR = updateReqs.stream().
                    filter(req -> user.equals(req.getKey())).findFirst().
                    orElseGet(() -> {
                        UserUR req = new UserUR.Builder(user).build();
                        updateReqs.add(req);
                        return req;
                    });

            Set<String> after = membershipsAfter.getOrDefault(user, Set.of());
            groups.stream().filter(group -> !after.contains(group)).
                    forEach(group -> userUR.getMemberships().add(new MembershipUR.Builder(group).
                    operation(PatchOperation.DELETE).
                    build()));
        });

        membershipsAfter.clear();
        membershipsBefore.clear();

        String context = "PullTask " + profile.getTask().getKey() + " '" + profile.getTask().getName() + "'";
        updateReqs.stream().filter(Predicate.not(UserUR::isEmpty)).forEach(req -> {
            LOG.debug("About to update memberships for User {}", req.getKey());
            userProvisioningManager.update(req, true, profile.getExecutor(), context);
        });
    }
}

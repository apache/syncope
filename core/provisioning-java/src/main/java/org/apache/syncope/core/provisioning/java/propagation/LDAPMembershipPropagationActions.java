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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.request.MembershipUR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.task.PropagationData;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.provisioning.api.jexl.JexlUtils;
import org.apache.syncope.core.provisioning.api.propagation.PropagationActions;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.spring.implementation.InstanceScope;
import org.apache.syncope.core.spring.implementation.SyncopeImplementation;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeDeltaBuilder;
import org.identityconnectors.framework.common.objects.AttributeDeltaUtil;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Simple action for propagating group memberships to LDAP groups, when the same resource is configured for both users
 * and groups.
 *
 * @see org.apache.syncope.core.provisioning.java.pushpull.LDAPMembershipPullActions
 */
@SyncopeImplementation(scope = InstanceScope.PER_CONTEXT)
public class LDAPMembershipPropagationActions implements PropagationActions {

    protected static final Logger LOG = LoggerFactory.getLogger(LDAPMembershipPropagationActions.class);

    @Autowired
    protected DerAttrHandler derAttrHandler;

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected GroupDAO groupDAO;

    /**
     * Allows easy subclassing for the ConnId AD connector bundle.
     *
     * @return the name of the attribute used to keep track of group memberships
     */
    protected String getGroupMembershipAttrName() {
        return "ldapGroups";
    }

    protected String evaluateGroupConnObjectLink(final String connObjectLinkTemplate, final Group group) {
        LOG.debug("Evaluating connObjectLink for {}", group);

        JexlContext jexlContext = new MapContext();
        JexlUtils.addFieldsToContext(group, jexlContext);
        JexlUtils.addPlainAttrsToContext(group.getPlainAttrs(), jexlContext);
        JexlUtils.addDerAttrsToContext(group, derAttrHandler, jexlContext);

        return JexlUtils.evaluateExpr(connObjectLinkTemplate, jexlContext).toString();
    }

    protected void buildManagedGroupConnObjectLinks(
            final ExternalResource resource,
            final String connObjectLinkTemplate,
            final Set<String> connObjectLinks) {

        List<Group> managedGroups = groupDAO.findByResourcesContaining(resource);
        managedGroups.forEach(group -> connObjectLinks.add(evaluateGroupConnObjectLink(connObjectLinkTemplate, group)));
    }

    @Transactional(readOnly = true)
    @Override
    public void before(final PropagationTaskInfo taskInfo) {
        if (AnyTypeKind.USER != taskInfo.getAnyTypeKind() || taskInfo.getOperation() == ResourceOperation.DELETE) {
            return;
        }

        taskInfo.getResource().getProvisionByAnyType(AnyTypeKind.GROUP.name()).
                map(Provision::getMapping).
                filter(mapping -> StringUtils.isNotBlank(mapping.getConnObjectLink())).ifPresentOrElse(mapping -> {

            User user = userDAO.findById(taskInfo.getEntityKey()).
                    orElseThrow(() -> new NotFoundException("User " + taskInfo.getEntityKey()));
            Set<String> groups = new HashSet<>();

            // for each user group assigned to the resource of this task, compute and add the group's 
            // connector object link
            userDAO.findAllGroupKeys(user).stream().
                    map(groupDAO::findById).flatMap(Optional::stream).
                    filter(group -> group.getResources().contains(taskInfo.getResource())).
                    forEach(group -> {
                        String groupConnObjectLink = evaluateGroupConnObjectLink(
                                mapping.getConnObjectLink(), group);

                        LOG.debug("ConnObjectLink for {} is '{}'", group, groupConnObjectLink);
                        if (StringUtils.isNotBlank(groupConnObjectLink)) {
                            groups.add(groupConnObjectLink);
                        }
                    });
            LOG.debug("Group connObjectLinks to propagate for membership: {}", groups);

            PropagationData data = taskInfo.getPropagationData();

            // if groups were defined by resource mapping, take their values and clear up
            Optional.ofNullable(AttributeUtil.find(getGroupMembershipAttrName(), data.getAttributes())).
                    ifPresent(ldapGroups -> {
                        Optional.ofNullable(ldapGroups.getValue()).
                                ifPresent(value -> value.forEach(obj -> groups.add(obj.toString())));

                        data.getAttributes().remove(ldapGroups);
                    });
            LOG.debug("Group connObjectLinks after including the ones from mapping: {}", groups);

            // take groups already assigned from beforeObj and include them too
            taskInfo.getBeforeObj().
                    map(beforeObj -> beforeObj.getAttributeByName(getGroupMembershipAttrName())).
                    filter(Objects::nonNull).
                    ifPresent(beforeLdapGroups -> {
                        Set<String> connObjectLinks = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                        buildManagedGroupConnObjectLinks(
                                taskInfo.getResource(),
                                mapping.getConnObjectLink(),
                                connObjectLinks);

                        LOG.debug("Memberships not managed by Syncope: {}", beforeLdapGroups);
                        beforeLdapGroups.getValue().stream().
                                filter(value -> !connObjectLinks.contains(String.valueOf(value))).
                                forEach(value -> groups.add(String.valueOf(value)));
                    });

            LOG.debug("Adding Group connObjectLinks to attributes: {}={}", getGroupMembershipAttrName(), groups);
            data.getAttributes().add(AttributeBuilder.build(getGroupMembershipAttrName(), groups));

            if (data.getAttributeDeltas() != null && taskInfo.getUpdateRequest() != null) {
                Set<String> groupsToAdd = new HashSet<>();
                Set<String> groupsToRemove = new HashSet<>();

                // if groups were added or removed by last update, compute and add the group's connector object link
                for (MembershipUR memb : ((UserUR) taskInfo.getUpdateRequest()).getMemberships()) {
                    String connObjectLink = evaluateGroupConnObjectLink(
                            mapping.getConnObjectLink(),
                            groupDAO.findById(memb.getGroup()).
                                    orElseThrow(() -> new NotFoundException("Group " + memb.getGroup())));
                    if (memb.getOperation() == PatchOperation.ADD_REPLACE) {
                        groupsToAdd.add(connObjectLink);
                    } else {
                        groupsToRemove.add(connObjectLink);
                    }
                }

                // if groups were already considered, take their values and clear up
                Optional.ofNullable(
                        AttributeDeltaUtil.find(getGroupMembershipAttrName(), data.getAttributeDeltas())).
                        ifPresent(ldapGroups -> {
                            Optional.ofNullable(ldapGroups.getValuesToAdd()).
                                    ifPresent(value -> value.forEach(obj -> groupsToAdd.add(obj.toString())));
                            Optional.ofNullable(ldapGroups.getValuesToRemove()).
                                    ifPresent(value -> value.forEach(obj -> groupsToRemove.add(obj.toString())));

                            data.getAttributeDeltas().remove(ldapGroups);
                        });

                if (!groupsToAdd.isEmpty() || !groupsToRemove.isEmpty()) {
                    LOG.debug("Adding Group connObjectLinks to attribute deltas: {}={},{}",
                            getGroupMembershipAttrName(), groupsToAdd, groupsToRemove);
                    data.getAttributeDeltas().add(
                            AttributeDeltaBuilder.build(getGroupMembershipAttrName(), groupsToAdd,
                                    groupsToRemove));
                }
            }
        }, () -> LOG.debug("Not about user, or group mapping missing for resource: not doing anything"));
    }
}

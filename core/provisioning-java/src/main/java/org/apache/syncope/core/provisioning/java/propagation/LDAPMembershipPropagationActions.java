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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.jexl.JexlUtils;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.provisioning.api.propagation.PropagationActions;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
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
public class LDAPMembershipPropagationActions implements PropagationActions {

    protected static final Logger LOG = LoggerFactory.getLogger(LDAPMembershipPropagationActions.class);

    @Autowired
    protected DerAttrHandler derAttrHandler;

    @Autowired
    protected AnyTypeDAO anyTypeDAO;

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected GroupDAO groupDAO;

    /**
     * Allows easy subclassing for the ConnId AD connector bundle.
     *
     * @return the name of the attribute used to keep track of group memberships
     */
    protected static String getGroupMembershipAttrName() {
        return "ldapGroups";
    }

    @Transactional(readOnly = true)
    @Override
    public void before(final PropagationTask task, final ConnectorObject beforeObj) {
        Optional<? extends Provision> provision = task.getResource().getProvision(anyTypeDAO.findGroup());
        if (AnyTypeKind.USER == task.getAnyTypeKind()
                && provision.isPresent() && provision.get().getMapping() != null
                && StringUtils.isNotBlank(provision.get().getMapping().getConnObjectLink())) {

            User user = userDAO.find(task.getEntityKey());
            if (user != null) {
                List<String> groupConnObjectLinks = new ArrayList<>();
                userDAO.findAllGroupKeys(user).forEach(groupKey -> {
                    Group group = groupDAO.find(groupKey);
                    if (group != null && groupDAO.findAllResourceKeys(groupKey).contains(task.getResource().getKey())) {

                        String groupConnObjectLink = evaluateGroupConnObjectLink(
                                provision.get().getMapping().getConnObjectLink(), group);

                        LOG.debug("ConnObjectLink for {} is '{}'", group, groupConnObjectLink);
                        if (StringUtils.isNotBlank(groupConnObjectLink)) {
                            groupConnObjectLinks.add(groupConnObjectLink);

                        }
                    }
                });
                LOG.debug("Group connObjectLinks to propagate for membership: {}", groupConnObjectLinks);

                Set<Attribute> attributes = new HashSet<>(task.getAttributes());

                Set<String> groups = new HashSet<>(groupConnObjectLinks);
                Attribute ldapGroups = AttributeUtil.find(getGroupMembershipAttrName(), attributes);
                if (ldapGroups != null) {
                    ldapGroups.getValue().forEach(obj -> groups.add(obj.toString()));
                    attributes.remove(ldapGroups);

                    if (beforeObj != null && beforeObj.getAttributeByName(getGroupMembershipAttrName()) != null) {
                        Set<String> connObjectLinks = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                        buildManagedGroupConnObjectLinks(
                                provision.get().getResource(),
                                provision.get().getMapping().getConnObjectLink(),
                                connObjectLinks);

                        Attribute beforeLdapGroups = beforeObj.getAttributeByName(getGroupMembershipAttrName());
                        LOG.debug("Memberships not managed by Syncope: {}", beforeLdapGroups);
                        beforeLdapGroups.getValue().stream().
                                filter(value -> !connObjectLinks.contains(String.valueOf(value))).
                                forEach(value -> groups.add(String.valueOf(value)));
                    }
                }
                LOG.debug("Add ldapGroups to attributes: {}", groups);
                attributes.add(AttributeBuilder.build(getGroupMembershipAttrName(), groups));

                task.setAttributes(attributes);
            }
        } else {
            LOG.debug("Not about user, or group mapping missing for resource: not doing anything");
        }
    }

    private String evaluateGroupConnObjectLink(final String connObjectLinkTemplate, final Group group) {
        LOG.debug("Evaluating connObjectLink for {}", group);

        JexlContext jexlContext = new MapContext();
        JexlUtils.addFieldsToContext(group, jexlContext);
        JexlUtils.addPlainAttrsToContext(group.getPlainAttrs(), jexlContext);
        JexlUtils.addDerAttrsToContext(group, derAttrHandler, jexlContext);

        return JexlUtils.evaluate(connObjectLinkTemplate, jexlContext);
    }

    private void buildManagedGroupConnObjectLinks(final ExternalResource externalResource,
            final String connObjectLinkTemplate, final Set<String> connObjectLinks) {
        List<Group> managedGroups = groupDAO.findByResource(externalResource);
        managedGroups.forEach(group -> connObjectLinks.add(evaluateGroupConnObjectLink(connObjectLinkTemplate, group)));
    }
}

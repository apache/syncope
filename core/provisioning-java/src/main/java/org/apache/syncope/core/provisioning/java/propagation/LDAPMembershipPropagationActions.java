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
import java.util.Set;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.java.jexl.JexlUtils;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
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
public class LDAPMembershipPropagationActions extends DefaultPropagationActions {

    protected static final Logger LOG = LoggerFactory.getLogger(LDAPMembershipPropagationActions.class);

    @Autowired
    protected AnyTypeDAO anyTypeDAO;

    @Autowired
    protected UserDAO userDAO;

    /**
     * Allows easy subclassing for the ConnId AD connector bundle.
     *
     * @return the name of the attribute used to keep track of group memberships
     */
    protected String getGroupMembershipAttrName() {
        return "ldapGroups";
    }

    @Transactional(readOnly = true)
    @Override
    public void before(final PropagationTask task, final ConnectorObject beforeObj) {
        super.before(task, beforeObj);

        Provision provision = task.getResource().getProvision(anyTypeDAO.findGroup());
        if (AnyTypeKind.USER == task.getAnyTypeKind() && provision != null && provision.getMapping() != null) {
            User user = userDAO.find(task.getEntityKey());
            if (user != null) {
                List<String> groupConnObjectLinks = new ArrayList<>();
                for (Group group : userDAO.findAllGroups(user)) {
                    if (group.getResourceKeys().contains(task.getResource().getKey())
                            && StringUtils.isNotBlank(provision.getMapping().getConnObjectLink())) {

                        LOG.debug("Evaluating connObjectLink for {}", group);

                        JexlContext jexlContext = new MapContext();
                        JexlUtils.addFieldsToContext(group, jexlContext);
                        JexlUtils.addPlainAttrsToContext(group.getPlainAttrs(), jexlContext);
                        JexlUtils.addDerAttrsToContext(group, jexlContext);

                        String groupConnObjectLinkLink =
                                JexlUtils.evaluate(provision.getMapping().getConnObjectLink(), jexlContext);
                        LOG.debug("ConnObjectLink for {} is '{}'", group, groupConnObjectLinkLink);
                        if (StringUtils.isNotBlank(groupConnObjectLinkLink)) {
                            groupConnObjectLinks.add(groupConnObjectLinkLink);
                        }
                    }
                }
                LOG.debug("Group connObjectLinks to propagate for membership: {}", groupConnObjectLinks);

                Set<Attribute> attributes = new HashSet<>(task.getAttributes());

                Set<String> groups = new HashSet<>(groupConnObjectLinks);
                Attribute ldapGroups = AttributeUtil.find(getGroupMembershipAttrName(), attributes);
                if (ldapGroups != null) {
                    for (Object obj : ldapGroups.getValue()) {
                        groups.add(obj.toString());
                    }
                    attributes.remove(ldapGroups);
                }
                attributes.add(AttributeBuilder.build(getGroupMembershipAttrName(), groups));

                task.setAttributes(attributes);
            }
        } else {
            LOG.debug("Not about user, or group mapping missing for resource: not doing anything");
        }
    }
}

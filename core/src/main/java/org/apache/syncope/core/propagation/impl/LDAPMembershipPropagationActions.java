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
package org.apache.syncope.core.propagation.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.MapContext;
import org.apache.commons.lang.StringUtils;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.propagation.DefaultPropagationActions;
import org.apache.syncope.core.util.JexlUtil;
import org.apache.syncope.types.AttributableType;
import org.apache.syncope.types.ResourceOperation;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Simple action for propagating role memberships to LDAP groups, when the same resource is configured for both users
 * and roles.
 * 
 * @see org.apache.syncope.core.sync.impl.LDAPMembershipSyncActions
 */
public class LDAPMembershipPropagationActions extends DefaultPropagationActions {

    protected static final Logger LOG = LoggerFactory.getLogger(LDAPMembershipPropagationActions.class);

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected JexlUtil jexlUtil;

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

        if (ResourceOperation.DELETE != task.getPropagationOperation()
                && AttributableType.USER == task.getSubjectType() && task.getResource().getRmapping() != null) {

            SyncopeUser user = userDAO.find(task.getSubjectId());
            if (user == null) {
                throw new IllegalArgumentException("User " + task.getSubjectId() + " not found");
            }

            List<String> roleAccountLinks = new ArrayList<String>();
            for (SyncopeRole role : user.getRoles()) {
                if (role.getResourceNames().contains(task.getResource().getName())
                        && StringUtils.isNotBlank(task.getResource().getRmapping().getAccountLink())) {

                    LOG.debug("Evaluating accountLink for {}", role);

                    final JexlContext jexlContext = new MapContext();
                    jexlUtil.addFieldsToContext(role, jexlContext);
                    jexlUtil.addAttrsToContext(role.getAttributes(), jexlContext);
                    jexlUtil.addDerAttrsToContext(role.getDerivedAttributes(), role.getAttributes(), jexlContext);
                    final String roleAccountLink = jexlUtil.evaluate(task.getResource().getRmapping().getAccountLink(),
                            jexlContext);
                    LOG.debug("AccountLink for {} is '{}'", role, roleAccountLink);
                    if (StringUtils.isNotBlank(roleAccountLink)) {
                        roleAccountLinks.add(roleAccountLink);
                    }
                }
            }
            LOG.debug("Role accountLinks to propagate for membership: {}", roleAccountLinks);

            if (!roleAccountLinks.isEmpty()) {
                Set<Attribute> attributes = new HashSet<Attribute>(task.getAttributes());
                attributes.add(AttributeBuilder.build(getGroupMembershipAttrName(), roleAccountLinks));
                task.setAttributes(attributes);
            }
        } else {
            LOG.debug("Not about user, or role mapping missing for resource: not doing anything");
        }
    }
}

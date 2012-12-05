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
package org.apache.syncope.core.propagation;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.MapContext;
import org.apache.commons.lang.StringUtils;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.util.JexlUtil;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class LDAPMembershipPropagationActions extends DefaultPropagationActions {

    private static final Logger LOG = LoggerFactory.getLogger(LDAPMembershipPropagationActions.class);

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private JexlUtil jexlUtil;

    @Transactional(readOnly = true)
    @Override
    public void before(final PropagationTask task, final ConnectorObject beforeObj) {
        super.before(task, beforeObj);

        if (beforeObj.getObjectClass() == ObjectClass.ACCOUNT && task.getResource().getRmapping() != null) {
            List<String> roleAccountLinks = new ArrayList<String>();
            for (SyncopeRole role : roleDAO.findAll()) {
                if (role.getResources().contains(task.getResource())
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
                task.getAttributes().add(AttributeBuilder.build("ldapGroups", roleAccountLinks));
            }
        } else {
            LOG.debug("It's {}, not doing anything", beforeObj.getObjectClass());
        }
    }
}

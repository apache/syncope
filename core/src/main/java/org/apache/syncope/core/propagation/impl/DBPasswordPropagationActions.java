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

import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.propagation.DefaultPropagationActions;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Propagate a non-cleartext password out to a resource, if the PropagationManager has not already
 * added a password.
 */
public class DBPasswordPropagationActions extends DefaultPropagationActions {

    protected static final Logger LOG = LoggerFactory.getLogger(DBPasswordPropagationActions.class);

    @Autowired
    protected UserDAO userDAO;

    @Transactional(readOnly = true)
    @Override
    public void before(final PropagationTask task, final ConnectorObject beforeObj) {
        super.before(task, beforeObj);

        if (AttributableType.USER == task.getSubjectType()) {
            SyncopeUser user = userDAO.find(task.getSubjectId());
            
            if (user != null && user.getPassword() != null) {
                Attribute missing = AttributeUtil.find("__MANDATORY_MISSING__", task.getAttributes());
                if (missing != null && missing.getValue() != null && missing.getValue().size() == 1
                    && missing.getValue().get(0).equals("__PASSWORD__")) {
                    List<Object> values = new ArrayList<Object>(1);
                    values.add(new GuardedString(user.getPassword().toCharArray()));
                    
                    Attribute passwordAttribute = AttributeBuilder.build("__PASSWORD__", values);
                    
                    Set<Attribute> attributes = new HashSet<Attribute>(task.getAttributes());
                    attributes.add(passwordAttribute);
                    attributes.remove(missing);
                    task.setAttributes(attributes);
                }
                
            }
        }
    }
}

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

import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.task.PropagationData;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.propagation.PropagationActions;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.spring.implementation.InstanceScope;
import org.apache.syncope.core.spring.implementation.SyncopeImplementation;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Propagate a non-cleartext password out to a resource, if the PropagationManager has not already
 * added a password. The CipherAlgorithm associated with the password must match the password
 * cipher algorithm property of the DB Connector.
 */
@SyncopeImplementation(scope = InstanceScope.PER_CONTEXT)
public class DBPasswordPropagationActions implements PropagationActions {

    protected static final String CLEARTEXT = "CLEARTEXT";

    @Autowired
    protected UserDAO userDAO;

    protected String getCipherAlgorithm(final ConnInstance connInstance) {
        Optional<ConnConfProperty> cipherAlgorithm = connInstance.getConf().stream().
                filter(property -> "cipherAlgorithm".equals(property.getSchema().getName())
                && property.getValues() != null && !property.getValues().isEmpty()).findFirst();

        return cipherAlgorithm.map(a -> (String) a.getValues().getFirst()).orElse(CLEARTEXT);
    }

    protected boolean cipherAlgorithmMatches(final String connectorAlgorithm, final CipherAlgorithm userAlgorithm) {
        if (userAlgorithm == null) {
            return false;
        }

        if (connectorAlgorithm.equals(userAlgorithm.name())) {
            return true;
        }

        // Special check for "SHA" (user sync'd from LDAP)
        return "SHA1".equals(connectorAlgorithm) && "SHA".equals(userAlgorithm.name());
    }

    @Transactional(readOnly = true)
    @Override
    public void before(final PropagationTaskInfo taskInfo) {
        if (AnyTypeKind.USER == taskInfo.getAnyTypeKind()) {
            User user = userDAO.findById(taskInfo.getEntityKey()).orElse(null);

            PropagationData data = taskInfo.getPropagationData();
            if (user != null && user.getPassword() != null && data.getAttributes() != null) {
                Set<Attribute> attrs = data.getAttributes();

                Attribute missing = AttributeUtil.find(PropagationManager.MANDATORY_MISSING_ATTR_NAME, attrs);

                ConnInstance connInstance = taskInfo.getResource().getConnector();
                if (missing != null && missing.getValue() != null && missing.getValue().size() == 1
                        && missing.getValue().getFirst().equals(OperationalAttributes.PASSWORD_NAME)
                        && cipherAlgorithmMatches(getCipherAlgorithm(connInstance), user.getCipherAlgorithm())) {

                    Attribute passwordAttribute = AttributeBuilder.buildPassword(
                            new GuardedString(user.getPassword().toCharArray()));

                    attrs.add(passwordAttribute);
                    attrs.remove(missing);

                    Attribute hashedPasswordAttribute = AttributeBuilder.build(
                            AttributeUtil.createSpecialName("HASHED_PASSWORD"), Boolean.TRUE);
                    attrs.add(hashedPasswordAttribute);
                }
            }
        }
    }
}

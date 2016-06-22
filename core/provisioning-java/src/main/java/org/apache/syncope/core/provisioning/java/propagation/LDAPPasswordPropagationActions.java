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
import java.util.Set;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.transaction.annotation.Transactional;

/**
 * Propagate a non-cleartext password out to a resource, if the PropagationManager has not already
 * added a password. The CipherAlgorithm associated with the password must match the password
 * hash algorithm property of the LDAP Connector.
 */
public class LDAPPasswordPropagationActions extends DefaultPropagationActions {

    private static final String CLEARTEXT = "CLEARTEXT";

    @Autowired
    private UserDAO userDAO;

    @Transactional(readOnly = true)
    @Override
    public void before(final PropagationTask task, final ConnectorObject beforeObj) {
        super.before(task, beforeObj);

        if (AnyTypeKind.USER == task.getAnyTypeKind()) {
            User user = userDAO.find(task.getEntityKey());

            if (user != null && user.getPassword() != null) {
                Attribute missing = AttributeUtil.find(
                        PropagationTaskExecutor.MANDATORY_MISSING_ATTR_NAME,
                        task.getAttributes());

                ConnInstance connInstance = task.getResource().getConnector();
                String cipherAlgorithm = getCipherAlgorithm(connInstance);
                if (missing != null && missing.getValue() != null && missing.getValue().size() == 1
                        && missing.getValue().get(0).equals(OperationalAttributes.PASSWORD_NAME)
                        && cipherAlgorithmMatches(getCipherAlgorithm(connInstance), user.getCipherAlgorithm())) {

                    String password = user.getPassword().toLowerCase();
                    byte[] decodedPassword = Hex.decode(password);
                    byte[] base64EncodedPassword = Base64.encode(decodedPassword);

                    String cipherPlusPassword =
                            ("{" + cipherAlgorithm.toLowerCase() + "}" + new String(base64EncodedPassword));

                    Attribute passwordAttribute = AttributeBuilder.buildPassword(
                            new GuardedString(cipherPlusPassword.toCharArray()));

                    Set<Attribute> attributes = new HashSet<>(task.getAttributes());
                    attributes.add(passwordAttribute);
                    attributes.remove(missing);

                    task.setAttributes(attributes);
                }
            }
        }
    }

    private String getCipherAlgorithm(final ConnInstance connInstance) {
        ConnConfProperty cipherAlgorithm =
                IterableUtils.find(connInstance.getConf(), new Predicate<ConnConfProperty>() {

                    @Override
                    public boolean evaluate(final ConnConfProperty property) {
                        return "passwordHashAlgorithm".equals(property.getSchema().getName())
                        && property.getValues() != null && !property.getValues().isEmpty();
                    }
                });

        return cipherAlgorithm == null
                ? CLEARTEXT
                : (String) cipherAlgorithm.getValues().get(0);
    }

    private boolean cipherAlgorithmMatches(final String connectorAlgorithm, final CipherAlgorithm userAlgorithm) {
        if (userAlgorithm == null) {
            return false;
        }

        if (connectorAlgorithm.equals(userAlgorithm.name())) {
            return true;
        }

        // Special check for "SHA" (user sync'd from LDAP)
        if ("SHA".equals(connectorAlgorithm) && "SHA1".equals(userAlgorithm.name())) {
            return true;
        }

        return false;
    }

}

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

import jakarta.xml.bind.DatatypeConverter;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
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
import org.springframework.util.CollectionUtils;

/**
 * Propagate a non-cleartext password out to a resource, if the PropagationManager has not already
 * added a password. The CipherAlgorithm associated with the password must match the password
 * hash algorithm property of the LDAP Connector.
 */
@SyncopeImplementation(scope = InstanceScope.PER_CONTEXT)
public class LDAPPasswordPropagationActions implements PropagationActions {

    protected static final String CLEARTEXT = "CLEARTEXT";

    @Autowired
    protected UserDAO userDAO;

    @Transactional(readOnly = true)
    @Override
    public void before(final PropagationTaskInfo taskInfo) {
        if (AnyTypeKind.USER == taskInfo.getAnyTypeKind()) {
            User user = userDAO.findById(taskInfo.getEntityKey()).orElse(null);
            if (user == null || user.getPassword() == null) {
                return;
            }

            Set<Attribute> attrs = taskInfo.getPropagationData().getAttributes();

            String cipherAlgorithm = getCipherAlgorithm(taskInfo.getResource().getConnector());
            Optional.ofNullable(AttributeUtil.find(PropagationManager.MANDATORY_MISSING_ATTR_NAME, attrs)).
                    filter(missing -> !CollectionUtils.isEmpty(missing.getValue())
                    && OperationalAttributes.PASSWORD_NAME.equals(missing.getValue().getFirst())
                    && cipherAlgorithmMatches(cipherAlgorithm, user.getCipherAlgorithm())).
                    ifPresent(missing -> {
                        attrs.remove(missing);

                        byte[] decodedPassword = DatatypeConverter.parseHexBinary(user.getPassword().toLowerCase());
                        String base64EncodedPassword = Base64.getEncoder().encodeToString(decodedPassword);

                        String cipherPlusPassword = '{' + cipherAlgorithm + '}' + base64EncodedPassword;

                        attrs.add(AttributeBuilder.buildPassword(new GuardedString(cipherPlusPassword.toCharArray())));
                    });
        }
    }

    protected String getCipherAlgorithm(final ConnInstance connInstance) {
        return connInstance.getConf().stream().
                filter(property -> "passwordHashAlgorithm".equals(property.getSchema().getName())
                && !property.getValues().isEmpty()).findFirst().
                map(cipherAlgorithm -> cipherAlgorithm.getValues().getFirst().toString()).
                orElse(CLEARTEXT);
    }

    protected boolean cipherAlgorithmMatches(final String connectorAlgo, final CipherAlgorithm userAlgo) {
        if (userAlgo == null) {
            return false;
        }

        if (connectorAlgo.equals(userAlgo.name())) {
            return true;
        }

        // Special check for "SHA" and "SSHA" (user pulled from LDAP)
        return ("SHA".equals(connectorAlgo) && userAlgo.name().startsWith("SHA"))
                || ("SSHA".equals(connectorAlgo) && userAlgo.name().startsWith("SSHA"));
    }
}

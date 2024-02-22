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
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.provisioning.api.propagation.PropagationActions;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.spring.implementation.InstanceScope;
import org.apache.syncope.core.spring.implementation.SyncopeImplementation;
import org.apache.syncope.core.spring.security.PasswordGenerator;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@SyncopeImplementation(scope = InstanceScope.PER_CONTEXT)
public class GenerateRandomPasswordPropagationActions implements PropagationActions {

    protected static final Logger LOG = LoggerFactory.getLogger(GenerateRandomPasswordPropagationActions.class);

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected RealmSearchDAO realmSearchDAO;

    @Autowired
    protected PasswordGenerator passwordGenerator;

    protected boolean generateRandomPassword(final PropagationTaskInfo taskInfo) {
        return AnyTypeKind.USER == taskInfo.getAnyTypeKind()
                && taskInfo.getBeforeObj().isEmpty()
                && AttributeUtil.getPasswordValue(taskInfo.getPropagationData().getAttributes()) == null;
    }

    @Transactional(readOnly = true)
    @Override
    public void before(final PropagationTaskInfo taskInfo) {
        if (generateRandomPassword(taskInfo)) {
            Set<Attribute> attrs = taskInfo.getPropagationData().getAttributes();

            // generate random password
            attrs.add(AttributeBuilder.buildPassword(passwordGenerator.generate(taskInfo.getResource(),
                    realmSearchDAO.findAncestors(userDAO.findById(taskInfo.getEntityKey()).
                            orElseThrow(() -> new NotFoundException("User " + taskInfo.getEntityKey())).getRealm())).
                    toCharArray()));

            // remove __PASSWORD__ from MANDATORY_MISSING attribute
            Optional.ofNullable(AttributeUtil.find(PropagationManager.MANDATORY_MISSING_ATTR_NAME, attrs)).
                    ifPresent(mandatoryMissing -> {
                        attrs.remove(mandatoryMissing);

                        Set<Object> newMandatoryMissing = mandatoryMissing.getValue().stream().
                                filter(v -> !OperationalAttributes.PASSWORD_NAME.equals(v)).
                                collect(Collectors.toSet());
                        if (!newMandatoryMissing.isEmpty()) {
                            attrs.add(AttributeBuilder.build(
                                    PropagationManager.MANDATORY_MISSING_ATTR_NAME, newMandatoryMissing));
                        }
                    });
        }
    }
}

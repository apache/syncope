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
package org.apache.syncope.core.provisioning.java.data;

import java.lang.reflect.Modifier;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.policy.RuleConf;
import org.apache.syncope.common.lib.report.ReportConf;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.ImplementationTypesHolder;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.provisioning.api.data.ImplementationDataBinder;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.implementation.ImplementationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImplementationDataBinderImpl implements ImplementationDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(ImplementationDataBinder.class);

    protected final EntityFactory entityFactory;

    public ImplementationDataBinderImpl(final EntityFactory entityFactory) {
        this.entityFactory = entityFactory;
    }

    @Override
    public Implementation create(final ImplementationTO implementationTO) {
        Implementation implementation = entityFactory.newEntity(Implementation.class);
        update(implementation, implementationTO);
        return implementation;
    }

    @Override
    public void update(final Implementation implementation, final ImplementationTO implementationTO) {
        SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidImplementation);

        if (implementation.getType() != null && !implementation.getType().equals(implementationTO.getType())) {
            sce.getElements().add("ImplementationType cannot be changed");
            throw sce;
        }

        if (StringUtils.isBlank(implementationTO.getBody())) {
            sce.getElements().add("No actual implementation provided");
            throw sce;
        }

        implementation.setKey(implementationTO.getKey());
        implementation.setEngine(implementationTO.getEngine());
        implementation.setType(implementationTO.getType());
        implementation.setBody(implementationTO.getBody());

        if (implementation.getEngine() == ImplementationEngine.JAVA) {
            Class<?> baseClazz = Optional.ofNullable(
                    ImplementationTypesHolder.getInstance().getValues().get(implementation.getType())).
                    map(intf -> {
                        try {
                            return Class.forName(intf);
                        } catch (ClassNotFoundException e) {
                            LOG.error("While resolving interface {} for implementation type {}",
                                    intf, implementation.getType());
                            return null;
                        }
                    }).
                    orElse(null);

            if (baseClazz == null) {
                sce.getElements().add("No Java interface found for " + implementation.getType());
                throw sce;
            }

            switch (implementation.getType()) {
                case IdRepoImplementationType.REPORT_DELEGATE:
                    ReportConf conf = POJOHelper.deserialize(implementation.getBody(), ReportConf.class);
                    if (conf == null) {
                        sce.getElements().add("Could not deserialize as " + ReportConf.class.getName());
                        throw sce;
                    }
                    break;

                case IdRepoImplementationType.ACCOUNT_RULE:
                case IdRepoImplementationType.PASSWORD_RULE:
                case IdMImplementationType.INBOUND_CORRELATION_RULE:
                case IdMImplementationType.PUSH_CORRELATION_RULE:
                    RuleConf rule = POJOHelper.deserialize(implementation.getBody(), RuleConf.class);
                    if (rule == null) {
                        sce.getElements().add("Could not deserialize as neither "
                                + "Account, Password, Pull nor Push Correlation RuleConf");
                        throw sce;
                    }
                    break;

                default:
                    Class<?> clazz = null;
                    try {
                        clazz = Class.forName(implementation.getBody());
                    } catch (Exception e) {
                        LOG.error("Class '{}' not found", implementation.getBody(), e);
                        sce.getElements().add("No Java class found: " + implementation.getBody());
                        throw sce;
                    }
                    if (!baseClazz.isAssignableFrom(clazz)) {
                        sce.getElements().add(
                                "Java class " + implementation.getBody() + " must comply with " + baseClazz.getName());
                        throw sce;
                    }
                    if (Modifier.isAbstract(clazz.getModifiers())) {
                        sce.getElements().add("Java class " + implementation.getBody() + " is abstract");
                        throw sce;
                    }
                    break;
            }
        } else if (implementation.getEngine() == ImplementationEngine.GROOVY) {
            try {
                ImplementationManager.build(implementation);
            } catch (Exception e) {
                LOG.error("While building Groovy class {}", implementation.getKey(), e);

                sce.getElements().add(e.getMessage());
                throw sce;
            }
        }
    }

    @Override
    public ImplementationTO getImplementationTO(final Implementation implementation) {
        ImplementationTO implementationTO = new ImplementationTO();
        implementationTO.setKey(implementation.getKey());
        implementationTO.setEngine(implementation.getEngine());
        implementationTO.setType(implementation.getType());
        implementationTO.setBody(implementation.getBody());

        return implementationTO;
    }
}

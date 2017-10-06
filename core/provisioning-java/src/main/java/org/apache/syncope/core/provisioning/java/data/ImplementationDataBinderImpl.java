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
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.policy.RuleConf;
import org.apache.syncope.common.lib.report.ReportletConf;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.ImplementationType;
import org.apache.syncope.core.persistence.api.attrvalue.validation.Validator;
import org.apache.syncope.core.persistence.api.dao.AccountRule;
import org.apache.syncope.core.persistence.api.dao.PasswordRule;
import org.apache.syncope.core.persistence.api.dao.Reportlet;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.provisioning.api.LogicActions;
import org.apache.syncope.core.provisioning.api.data.ImplementationDataBinder;
import org.apache.syncope.core.provisioning.api.data.ItemTransformer;
import org.apache.syncope.core.provisioning.api.job.SchedTaskJobDelegate;
import org.apache.syncope.core.provisioning.api.propagation.PropagationActions;
import org.apache.syncope.core.provisioning.api.pushpull.PullActions;
import org.apache.syncope.core.provisioning.api.pushpull.PullCorrelationRule;
import org.apache.syncope.core.provisioning.api.pushpull.PushActions;
import org.apache.syncope.core.provisioning.api.pushpull.ReconFilterBuilder;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.apache.syncope.core.provisioning.api.notification.RecipientsProvider;

@Component
public class ImplementationDataBinderImpl implements ImplementationDataBinder {

    private static final Logger LOG = LoggerFactory.getLogger(ImplementationDataBinder.class);

    @Autowired
    private EntityFactory entityFactory;

    @Override
    public Implementation create(final ImplementationTO implementationTO) {
        Implementation implementation = entityFactory.newEntity(Implementation.class);
        update(implementation, implementationTO);
        return implementation;
    }

    @Override
    public void update(final Implementation implementation, final ImplementationTO implementationTO) {
        SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidImplementation);

        if (implementation.getType() != null && implementation.getType() != implementationTO.getType()) {
            sce.getElements().add("ImplementationType cannot be changed");
            throw sce;
        }

        if (StringUtils.isBlank(implementationTO.getBody())) {
            sce.getElements().add("No actual implementation provided");
            throw sce;
        }

        BeanUtils.copyProperties(implementationTO, implementation);

        if (implementation.getEngine() == ImplementationEngine.JAVA) {
            Class<?> base = null;
            switch (implementation.getType()) {
                case REPORTLET:
                    base = Reportlet.class;
                    break;

                case ACCOUNT_RULE:
                    base = AccountRule.class;
                    break;

                case PASSWORD_RULE:
                    base = PasswordRule.class;
                    break;

                case ITEM_TRANSFORMER:
                    base = ItemTransformer.class;
                    break;

                case TASKJOB_DELEGATE:
                    base = SchedTaskJobDelegate.class;
                    break;

                case RECON_FILTER_BUILDER:
                    base = ReconFilterBuilder.class;
                    break;

                case LOGIC_ACTIONS:
                    base = LogicActions.class;
                    break;

                case PROPAGATION_ACTIONS:
                    base = PropagationActions.class;
                    break;

                case PULL_ACTIONS:
                    base = PullActions.class;
                    break;

                case PUSH_ACTIONS:
                    base = PushActions.class;
                    break;

                case PULL_CORRELATION_RULE:
                    base = PullCorrelationRule.class;
                    break;

                case VALIDATOR:
                    base = Validator.class;
                    break;

                case RECIPIENTS_PROVIDER:
                    base = RecipientsProvider.class;
                    break;

                default:
            }

            if (base == null) {
                sce.getElements().add("No Java interface found for " + implementation.getType());
                throw sce;
            }

            if (implementation.getType() == ImplementationType.REPORTLET) {
                ReportletConf reportlet = POJOHelper.deserialize(implementation.getBody(), ReportletConf.class);
                if (reportlet == null) {
                    sce.getElements().add("Could not deserialize as ReportletConf");
                    throw sce;
                }
            } else if (implementation.getType() == ImplementationType.ACCOUNT_RULE
                    || implementation.getType() == ImplementationType.PASSWORD_RULE) {

                RuleConf rule = POJOHelper.deserialize(implementation.getBody(), RuleConf.class);
                if (rule == null) {
                    sce.getElements().add("Could not deserialize as neither Account nor Password RuleConf");
                    throw sce;
                }
            } else {
                Class<?> clazz = null;
                try {
                    clazz = Class.forName(implementation.getBody());
                } catch (Exception e) {
                    LOG.error("Class '{}' not found", implementation.getBody(), e);
                    sce.getElements().add("No Java class found: " + implementation.getBody());
                    throw sce;
                }
                if (!base.isAssignableFrom(clazz)) {
                    sce.getElements().add(
                            "Java class " + implementation.getBody() + " must comply with " + base.getName());
                    throw sce;
                }
                if (Modifier.isAbstract(clazz.getModifiers())) {
                    sce.getElements().add("Java class " + implementation.getBody() + " is abstract");
                    throw sce;
                }
            }
        }
    }

    @Override
    public ImplementationTO getImplementationTO(final Implementation implementation) {
        ImplementationTO implementationTO = new ImplementationTO();
        BeanUtils.copyProperties(implementation, implementationTO);
        return implementationTO;
    }

}

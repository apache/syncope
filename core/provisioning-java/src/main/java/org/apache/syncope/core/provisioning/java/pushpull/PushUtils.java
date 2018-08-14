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
package org.apache.syncope.core.provisioning.java.pushpull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.dao.PushCorrelationRule;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.policy.PushCorrelationRuleEntity;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.TimeoutException;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.apache.syncope.core.spring.ImplementationManager;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Component
public class PushUtils {

    private static final Logger LOG = LoggerFactory.getLogger(PushUtils.class);

    @Autowired
    private MappingManager mappingManager;

    public List<ConnectorObject> match(
            final Connector connector,
            final Any<?> any,
            final Provision provision) {

        Optional<? extends PushCorrelationRuleEntity> correlationRule = provision.getResource().getPushPolicy() == null
                ? Optional.empty()
                : provision.getResource().getPushPolicy().getCorrelationRule(provision.getAnyType());

        Optional<PushCorrelationRule> rule = Optional.empty();
        if (correlationRule.isPresent()) {
            try {
                rule = ImplementationManager.buildPushCorrelationRule(correlationRule.get().getImplementation());
            } catch (Exception e) {
                LOG.error("While building {}", correlationRule.get().getImplementation(), e);
            }
        }

        try {
            return rule.isPresent()
                    ? findByCorrelationRule(connector, any, provision, rule.get())
                    : findByConnObjectKey(connector, any, provision);
        } catch (RuntimeException e) {
            LOG.error("Could not match {} with any existing {}", any, provision.getObjectClass(), e);
            return Collections.<ConnectorObject>emptyList();
        }
    }

    private List<ConnectorObject> findByCorrelationRule(
            final Connector connector,
            final Any<?> any,
            final Provision provision,
            final PushCorrelationRule rule) {

        List<ConnectorObject> objs = new ArrayList<>();

        try {
            connector.search(
                    provision.getObjectClass(),
                    rule.getFilter(any, provision),
                    obj -> {
                        objs.add(obj);
                        return true;
                    }, MappingUtils.buildOperationOptions(provision.getMapping().getItems().iterator()));
        } catch (TimeoutException toe) {
            LOG.debug("Request timeout", toe);
            throw toe;
        } catch (RuntimeException ignore) {
            LOG.debug("Unexpected exception", ignore);
        }

        return objs;
    }

    public List<ConnectorObject> findByConnObjectKey(
            final Connector connector,
            final Any<?> any,
            final Provision provision) {

        Optional<? extends MappingItem> connObjectKey = MappingUtils.getConnObjectKeyItem(provision);
        Optional<String> connObjectKeyValue = mappingManager.getConnObjectKeyValue(any, provision);

        ConnectorObject obj = null;
        if (connObjectKey.isPresent() && connObjectKeyValue.isPresent()) {
            try {
                obj = connector.getObject(
                        provision.getObjectClass(),
                        AttributeBuilder.build(connObjectKey.get().getExtAttrName(), connObjectKeyValue.get()),
                        provision.isIgnoreCaseMatch(),
                        MappingUtils.buildOperationOptions(provision.getMapping().getItems().iterator()));
            } catch (TimeoutException toe) {
                LOG.debug("Request timeout", toe);
                throw toe;
            } catch (RuntimeException ignore) {
                LOG.debug("While resolving {}", connObjectKeyValue.get(), ignore);
            }
        }

        return obj == null ? Collections.emptyList() : Collections.singletonList(obj);
    }
}

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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.PushCorrelationRule;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.LinkingMappingItem;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.policy.PushCorrelationRuleEntity;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.TimeoutException;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.apache.syncope.core.provisioning.api.propagation.PropagationActions;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.apache.syncope.core.spring.ImplementationManager;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboundMatcher {

    private static final Logger LOG = LoggerFactory.getLogger(OutboundMatcher.class);

    @Autowired
    private MappingManager mappingManager;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private AnyUtilsFactory anyUtilsFactory;

    @Autowired
    private VirSchemaDAO virSchemaDAO;

    @Autowired
    private VirAttrHandler virAttrHandler;

    private Optional<PushCorrelationRule> rule(final Provision provision) {
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

        return rule;
    }

    public List<ConnectorObject> match(
            final PropagationTask task,
            final Connector connector,
            final Provision provision,
            final List<PropagationActions> actions,
            final String connObjectKeyValue) {

        Optional<PushCorrelationRule> rule = rule(provision);

        boolean isLinkedAccount = task.getAnyTypeKind() == AnyTypeKind.USER
                && userDAO.linkedAccountExists(task.getEntityKey(), connObjectKeyValue);
        Any<?> any = null;
        if (!isLinkedAccount) {
            any = anyUtilsFactory.getInstance(task.getAnyTypeKind()).dao().find(task.getEntityKey());
        }

        Set<String> moreAttrsToGet = new HashSet<>();
        actions.forEach(action -> moreAttrsToGet.addAll(action.moreAttrsToGet(task, provision)));

        List<ConnectorObject> result = new ArrayList<>();
        try {
            if (any != null && rule.isPresent()) {
                result.addAll(matchByCorrelationRule(
                        connector,
                        rule.get().getFilter(any, provision),
                        provision,
                        Optional.of(moreAttrsToGet.toArray(new String[0])),
                        Optional.empty()));
            } else {
                MappingUtils.getConnObjectKeyItem(provision).ifPresent(connObjectKeyItem -> matchByConnObjectKeyValue(
                        connector,
                        connObjectKeyItem,
                        connObjectKeyValue,
                        provision,
                        Optional.of(moreAttrsToGet.toArray(new String[0])),
                        Optional.empty()).
                        ifPresent(result::add));
            }
        } catch (RuntimeException e) {
            LOG.error("Could not match {} with any existing {}", any, provision.getObjectClass(), e);
        }

        if (any != null && result.size() == 1) {
            virAttrHandler.setValues(any, result.get(0));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<ConnectorObject> match(
            final Connector connector,
            final Any<?> any,
            final Provision provision,
            final Optional<String[]> moreAttrsToGet,
            final LinkingMappingItem... linkingItems) {

        Optional<PushCorrelationRule> rule = rule(provision);

        List<ConnectorObject> result = new ArrayList<>();
        try {
            if (rule.isPresent()) {
                result.addAll(matchByCorrelationRule(
                        connector,
                        rule.get().getFilter(any, provision),
                        provision,
                        moreAttrsToGet,
                        ArrayUtils.isEmpty(linkingItems)
                        ? Optional.empty() : Optional.of(List.of(linkingItems))));
            } else {
                Optional<? extends MappingItem> connObjectKeyItem = MappingUtils.getConnObjectKeyItem(provision);
                Optional<String> connObjectKeyValue = mappingManager.getConnObjectKeyValue(any, provision);

                if (connObjectKeyItem.isPresent() && connObjectKeyValue.isPresent()) {
                    matchByConnObjectKeyValue(
                            connector,
                            connObjectKeyItem.get(),
                            connObjectKeyValue.get(),
                            provision,
                            moreAttrsToGet,
                            ArrayUtils.isEmpty(linkingItems)
                            ? Optional.empty() : Optional.of(List.of(linkingItems))).
                            ifPresent(result::add);
                }
            }
        } catch (RuntimeException e) {
            LOG.error("Could not match {} with any existing {}", any, provision.getObjectClass(), e);
        }

        if (any != null && result.size() == 1) {
            virAttrHandler.setValues(any, result.get(0));
        }

        return result;
    }

    private List<ConnectorObject> matchByCorrelationRule(
            final Connector connector,
            final Filter filter,
            final Provision provision,
            final Optional<String[]> moreAttrsToGet,
            final Optional<Collection<LinkingMappingItem>> linkingItems) {

        Stream<MappingItem> items = Stream.concat(
                provision.getMapping().getItems().stream(),
                linkingItems.isPresent()
                ? linkingItems.get().stream()
                : virSchemaDAO.findByProvision(provision).stream().map(VirSchema::asLinkingMappingItem));

        List<ConnectorObject> objs = new ArrayList<>();
        try {
            connector.search(provision.getObjectClass(), filter, new SearchResultsHandler() {

                @Override
                public void handleResult(final SearchResult result) {
                    // nothing to do
                }

                @Override
                public boolean handle(final ConnectorObject connectorObject) {
                    objs.add(connectorObject);
                    return true;
                }
            }, MappingUtils.buildOperationOptions(items, moreAttrsToGet.orElse(null)));
        } catch (TimeoutException toe) {
            LOG.debug("Request timeout", toe);
            throw toe;
        } catch (RuntimeException ignore) {
            LOG.debug("Unexpected exception", ignore);
        }

        return objs;
    }

    @Transactional(readOnly = true)
    public Optional<ConnectorObject> matchByConnObjectKeyValue(
            final Connector connector,
            final MappingItem connObjectKeyItem,
            final String connObjectKeyValue,
            final Provision provision,
            final Optional<String[]> moreAttrsToGet,
            final Optional<Collection<LinkingMappingItem>> linkingItems) {

        Stream<MappingItem> items = Stream.concat(
                provision.getMapping().getItems().stream(),
                linkingItems.isPresent()
                ? linkingItems.get().stream()
                : virSchemaDAO.findByProvision(provision).stream().map(VirSchema::asLinkingMappingItem));

        ConnectorObject obj = null;
        try {
            obj = connector.getObject(
                    provision.getObjectClass(),
                    AttributeBuilder.build(connObjectKeyItem.getExtAttrName(), connObjectKeyValue),
                    provision.isIgnoreCaseMatch(),
                    MappingUtils.buildOperationOptions(items, moreAttrsToGet.orElse(null)));
        } catch (TimeoutException toe) {
            LOG.debug("Request timeout", toe);
            throw toe;
        } catch (RuntimeException ignore) {
            LOG.debug("While resolving {}", connObjectKeyValue, ignore);
        }

        return Optional.ofNullable(obj);
    }
}

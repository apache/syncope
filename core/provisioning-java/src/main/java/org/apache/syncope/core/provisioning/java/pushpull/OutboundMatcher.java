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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.policy.PushCorrelationRuleEntity;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.TimeoutException;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.apache.syncope.core.provisioning.api.propagation.PropagationActions;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.provisioning.api.rules.PushCorrelationRule;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.apache.syncope.core.spring.implementation.ImplementationManager;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

public class OutboundMatcher {

    protected static final Logger LOG = LoggerFactory.getLogger(OutboundMatcher.class);

    protected final MappingManager mappingManager;

    protected final UserDAO userDAO;

    protected final AnyUtilsFactory anyUtilsFactory;

    protected final VirSchemaDAO virSchemaDAO;

    protected final VirAttrHandler virAttrHandler;

    protected final Map<String, PropagationActions> perContextActions = new ConcurrentHashMap<>();

    protected final Map<String, PushCorrelationRule> perContextPushCorrelationRules = new ConcurrentHashMap<>();

    public OutboundMatcher(
            final MappingManager mappingManager,
            final UserDAO userDAO,
            final AnyUtilsFactory anyUtilsFactory,
            final VirSchemaDAO virSchemaDAO,
            final VirAttrHandler virAttrHandler) {

        this.mappingManager = mappingManager;
        this.userDAO = userDAO;
        this.anyUtilsFactory = anyUtilsFactory;
        this.virSchemaDAO = virSchemaDAO;
        this.virAttrHandler = virAttrHandler;
    }

    protected Optional<PushCorrelationRule> rule(final ExternalResource resource, final Provision provision) {
        Optional<? extends PushCorrelationRuleEntity> correlationRule = resource.getPushPolicy() == null
                ? Optional.empty()
                : resource.getPushPolicy().getCorrelationRule(provision.getAnyType());

        Optional<PushCorrelationRule> rule = Optional.empty();
        if (correlationRule.isPresent()) {
            Implementation impl = correlationRule.get().getImplementation();
            try {
                rule = ImplementationManager.buildPushCorrelationRule(
                        impl,
                        () -> perContextPushCorrelationRules.get(impl.getKey()),
                        instance -> perContextPushCorrelationRules.put(impl.getKey(), instance));
            } catch (Exception e) {
                LOG.error("While building {}", impl, e);
            }
        }

        return rule;
    }

    public String getFIQL(
            final ConnectorObject connectorObject,
            final ExternalResource resource,
            final Provision provision) {

        return rule(resource, provision).
                map(rule -> rule.getFIQL(connectorObject, provision)).
                orElseGet(() -> PushCorrelationRule.DEFAULT_FIQL_BUILDER.apply(connectorObject, provision));
    }

    public List<ConnectorObject> match(
            final PropagationTaskInfo taskInfo,
            final Connector connector,
            final Provision provision,
            final List<PropagationActions> actions,
            final String connObjectKeyValue) {

        Optional<PushCorrelationRule> rule = rule(taskInfo.getResource(), provision);

        boolean isLinkedAccount = taskInfo.getAnyTypeKind() == AnyTypeKind.USER
                && userDAO.linkedAccountExists(taskInfo.getEntityKey(), connObjectKeyValue);
        Any any = null;
        if (!isLinkedAccount) {
            any = anyUtilsFactory.getInstance(taskInfo.getAnyTypeKind()).dao().findById(taskInfo.getEntityKey()).
                    orElse(null);
        }

        Set<String> moreAttrsToGet = new HashSet<>();
        actions.forEach(action -> moreAttrsToGet.addAll(action.moreAttrsToGet(Optional.of(taskInfo), provision)));

        List<ConnectorObject> result = new ArrayList<>();
        try {
            if (any != null && rule.isPresent()) {
                result.addAll(matchByCorrelationRule(
                        connector,
                        rule.get().getFilter(any, taskInfo.getResource(), provision),
                        taskInfo.getResource(),
                        provision,
                        Optional.of(moreAttrsToGet.toArray(String[]::new)),
                        Optional.empty()));
            } else {
                MappingUtils.getConnObjectKeyItem(provision).flatMap(connObjectKeyItem -> matchByConnObjectKeyValue(
                        connector,
                        connObjectKeyItem,
                        connObjectKeyValue,
                        taskInfo.getResource(),
                        provision,
                        Optional.of(moreAttrsToGet.toArray(String[]::new)),
                        Optional.empty())).ifPresent(result::add);
            }
        } catch (RuntimeException e) {
            LOG.error("Could not match {} with any existing {}", any, provision.getObjectClass(), e);
        }

        if (any != null && result.size() == 1) {
            virAttrHandler.setValues(any, result.getFirst());
        }

        return result;
    }

    protected List<PropagationActions> getPropagationActions(final ExternalResource resource) {
        List<PropagationActions> result = new ArrayList<>();

        resource.getPropagationActions().forEach(impl -> {
            try {
                result.add(ImplementationManager.build(
                        impl,
                        () -> perContextActions.get(impl.getKey()),
                        instance -> perContextActions.put(impl.getKey(), instance)));
            } catch (Exception e) {
                LOG.error("While building {}", impl, e);
            }
        });

        return result;
    }

    @Transactional(readOnly = true)
    public List<ConnectorObject> match(
            final Connector connector,
            final Any any,
            final ExternalResource resource,
            final Provision provision,
            final Optional<String[]> moreAttrsToGet,
            final Item... linkingItems) {

        Stream<String> matgFromPropagationActions = getPropagationActions(resource).stream().
                flatMap(a -> a.moreAttrsToGet(Optional.empty(), provision).stream());
        Optional<String[]> effectiveMATG = Optional.of(Stream.concat(
                moreAttrsToGet.stream().flatMap(Stream::of),
                matgFromPropagationActions).toArray(String[]::new));

        Optional<PushCorrelationRule> rule = rule(resource, provision);

        List<ConnectorObject> result = new ArrayList<>();
        try {
            if (rule.isPresent()) {
                result.addAll(matchByCorrelationRule(
                        connector,
                        rule.get().getFilter(any, resource, provision),
                        resource,
                        provision,
                        effectiveMATG,
                        ArrayUtils.isEmpty(linkingItems)
                        ? Optional.empty() : Optional.of(List.of(linkingItems))));
            } else {
                Optional<Item> connObjectKeyItem = MappingUtils.getConnObjectKeyItem(provision);
                Optional<String> connObjectKeyValue = mappingManager.getConnObjectKeyValue(any, resource, provision);

                if (connObjectKeyItem.isPresent() && connObjectKeyValue.isPresent()) {
                    matchByConnObjectKeyValue(
                            connector,
                            connObjectKeyItem.get(),
                            connObjectKeyValue.get(),
                            resource,
                            provision,
                            effectiveMATG,
                            ArrayUtils.isEmpty(linkingItems)
                            ? Optional.empty() : Optional.of(List.of(linkingItems))).
                            ifPresent(result::add);
                }
            }
        } catch (RuntimeException e) {
            LOG.error("Could not match {} with any existing {}", any, provision.getObjectClass(), e);
        }

        if (any != null && result.size() == 1) {
            virAttrHandler.setValues(any, result.getFirst());
        }

        return result;
    }

    protected List<ConnectorObject> matchByCorrelationRule(
            final Connector connector,
            final Filter filter,
            final ExternalResource resource,
            final Provision provision,
            final Optional<String[]> moreAttrsToGet,
            final Optional<Collection<Item>> linkingItems) {

        Stream<Item> items = Stream.concat(
                provision.getMapping().getItems().stream(),
                linkingItems.isPresent()
                ? linkingItems.get().stream()
                : virSchemaDAO.findByResourceAndAnyType(resource.getKey(), provision.getAnyType()).stream().
                        map(VirSchema::asLinkingMappingItem));

        List<ConnectorObject> objs = new ArrayList<>();
        try {
            connector.search(new ObjectClass(provision.getObjectClass()), filter, new SearchResultsHandler() {

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
            final Item connObjectKeyItem,
            final String connObjectKeyValue,
            final ExternalResource resource,
            final Provision provision,
            final Optional<String[]> moreAttrsToGet,
            final Optional<Collection<Item>> linkingItems) {

        Stream<Item> items = Stream.concat(
                provision.getMapping().getItems().stream(),
                linkingItems.isPresent()
                ? linkingItems.get().stream()
                : virSchemaDAO.findByResourceAndAnyType(resource.getKey(), provision.getAnyType()).stream().
                        map(VirSchema::asLinkingMappingItem));

        ConnectorObject obj = null;
        try {
            obj = connector.getObject(
                    new ObjectClass(provision.getObjectClass()),
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

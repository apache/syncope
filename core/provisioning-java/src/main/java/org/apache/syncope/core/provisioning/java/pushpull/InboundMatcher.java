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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.OrgUnit;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.MatchType;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.policy.InboundCorrelationRuleEntity;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.IntAttrName;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.apache.syncope.core.provisioning.api.data.ItemTransformer;
import org.apache.syncope.core.provisioning.api.rules.InboundCorrelationRule;
import org.apache.syncope.core.provisioning.api.rules.InboundMatch;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.apache.syncope.core.spring.implementation.ImplementationManager;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.LiveSyncDelta;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncDeltaBuilder;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Transactional(readOnly = true)
public class InboundMatcher {

    protected static final Logger LOG = LoggerFactory.getLogger(InboundMatcher.class);

    protected final UserDAO userDAO;

    protected final AnyObjectDAO anyObjectDAO;

    protected final GroupDAO groupDAO;

    protected final AnySearchDAO anySearchDAO;

    protected final RealmDAO realmDAO;

    protected final RealmSearchDAO realmSearchDAO;

    protected final VirSchemaDAO virSchemaDAO;

    protected final ImplementationDAO implementationDAO;

    protected final VirAttrHandler virAttrHandler;

    protected final IntAttrNameParser intAttrNameParser;

    protected final AnyUtilsFactory anyUtilsFactory;

    protected final Map<String, InboundCorrelationRule> perContextInboundCorrelationRules = new ConcurrentHashMap<>();

    public InboundMatcher(
            final UserDAO userDAO,
            final AnyObjectDAO anyObjectDAO,
            final GroupDAO groupDAO,
            final AnySearchDAO anySearchDAO,
            final RealmDAO realmDAO,
            final RealmSearchDAO realmSearchDAO,
            final VirSchemaDAO virSchemaDAO,
            final ImplementationDAO implementationDAO,
            final VirAttrHandler virAttrHandler,
            final IntAttrNameParser intAttrNameParser,
            final AnyUtilsFactory anyUtilsFactory) {

        this.userDAO = userDAO;
        this.anyObjectDAO = anyObjectDAO;
        this.groupDAO = groupDAO;
        this.anySearchDAO = anySearchDAO;
        this.realmDAO = realmDAO;
        this.realmSearchDAO = realmSearchDAO;
        this.virSchemaDAO = virSchemaDAO;
        this.implementationDAO = implementationDAO;
        this.virAttrHandler = virAttrHandler;
        this.intAttrNameParser = intAttrNameParser;
        this.anyUtilsFactory = anyUtilsFactory;
    }

    public Optional<InboundMatch> match(
            final AnyType anyType,
            final String connObjectLinkValue,
            final ExternalResource resource,
            final Connector connector) {

        Provision provision = resource.getProvisionByAnyType(anyType.getKey()).orElse(null);
        if (provision == null) {
            return Optional.empty();
        }

        // first, attempt to match the provided connObjectLinkValue against the configured connObjectLink
        // (if available) via internal search
        if (StringUtils.isNotBlank(provision.getMapping().getConnObjectLink())) {
            List<? extends Any> found = anyUtilsFactory.getInstance(anyType.getKind()).dao().
                    findByDerAttrValue(
                            provision.getMapping().getConnObjectLink(),
                            connObjectLinkValue,
                            provision.isIgnoreCaseMatch());
            if (!found.isEmpty()) {
                return Optional.of(new InboundMatch(MatchType.ANY, found.getFirst()));
            }
        }

        // then, attempt to lookup the provided connObjectLinkValue onto the connector
        Stream<Item> mapItems = Stream.concat(
                provision.getMapping().getItems().stream(),
                virSchemaDAO.findByResourceAndAnyType(resource.getKey(), anyType.getKey()).stream().
                        map(VirSchema::asLinkingMappingItem));

        List<ConnectorObject> found = new ArrayList<>();

        try {
            Name nameAttr = new Name(connObjectLinkValue);
            connector.search(
                    new ObjectClass(provision.getObjectClass()),
                    provision.isIgnoreCaseMatch()
                    ? FilterBuilder.equalsIgnoreCase(nameAttr)
                    : FilterBuilder.equalTo(nameAttr),
                    new SearchResultsHandler() {

                @Override
                public void handleResult(final SearchResult result) {
                    // nothing to do
                }

                @Override
                public boolean handle(final ConnectorObject connectorObject) {
                    return found.add(connectorObject);
                }
            }, MappingUtils.buildOperationOptions(mapItems));
        } catch (Throwable t) {
            LOG.warn("While searching for {} ...", connObjectLinkValue, t);
        }

        Optional<InboundMatch> result = Optional.empty();

        if (found.isEmpty()) {
            LOG.debug("No {} found on {} with {} {}",
                    provision.getObjectClass(), resource, Name.NAME, connObjectLinkValue);
        } else {
            if (found.size() > 1) {
                LOG.warn("More than one {} found on {} with {} {} - taking first only",
                        provision.getObjectClass(), resource, Name.NAME, connObjectLinkValue);
            }

            ConnectorObject connObj = found.getFirst();
            try {
                List<InboundMatch> matches = match(
                        new SyncDeltaBuilder().
                                setToken(new SyncToken("")).
                                setDeltaType(SyncDeltaType.CREATE_OR_UPDATE).
                                setObject(connObj).
                                build(),
                        resource,
                        provision,
                        anyType.getKind());
                if (matches.isEmpty()) {
                    LOG.debug("No matching {} found for {}, aborting", anyType.getKind(), connObj);
                } else {
                    if (matches.size() > 1) {
                        LOG.warn("More than one {} found {} - taking first only", anyType.getKind(), matches);
                    }

                    result = matches.stream().filter(match -> match.getAny() != null).findFirst();
                    result.ifPresent(inboundMatch -> virAttrHandler.setValues(inboundMatch.getAny(), connObj));
                }
            } catch (IllegalArgumentException e) {
                LOG.warn(e.getMessage());
            }
        }

        return result;
    }

    protected List<Implementation> getTransformers(final Item item) {
        return item.getTransformers().stream().
                map(implementationDAO::findById).
                flatMap(Optional::stream).
                collect(Collectors.toList());
    }

    public List<InboundMatch> matchByConnObjectKeyValue(
            final Item connObjectKeyItem,
            final String connObjectKeyValue,
            final AnyTypeKind anyTypeKind,
            final ExternalResource resource,
            final boolean ignoreCaseMatch) {

        String finalConnObjectKeyValue = connObjectKeyValue;
        for (ItemTransformer transformer
                : MappingUtils.getItemTransformers(connObjectKeyItem, getTransformers(connObjectKeyItem))) {

            List<Object> output = transformer.beforePull(
                    connObjectKeyItem,
                    null,
                    List.of(finalConnObjectKeyValue));
            if (!CollectionUtils.isEmpty(output)) {
                finalConnObjectKeyValue = output.getFirst().toString();
            }
        }

        List<InboundMatch> noMatchResult = List.of(InboundCorrelationRule.NO_MATCH);

        IntAttrName intAttrName;
        try {
            intAttrName = intAttrNameParser.parse(connObjectKeyItem.getIntAttrName(), anyTypeKind);
        } catch (ParseException e) {
            LOG.error("Invalid intAttrName '{}' specified, ignoring", connObjectKeyItem.getIntAttrName(), e);
            return noMatchResult;
        }

        AnyUtils anyUtils = anyUtilsFactory.getInstance(anyTypeKind);

        List<Any> anys = new ArrayList<>();

        if (intAttrName.getField() != null) {
            switch (intAttrName.getField()) {
                case "key" ->
                    anyUtils.dao().findById(finalConnObjectKeyValue).ifPresent(anys::add);

                case "username" -> {
                    if (anyTypeKind == AnyTypeKind.USER && ignoreCaseMatch) {
                        AnyCond cond = new AnyCond(AttrCond.Type.IEQ);
                        cond.setSchema("username");
                        cond.setExpression(finalConnObjectKeyValue);
                        anys.addAll(anySearchDAO.search(SearchCond.of(cond), AnyTypeKind.USER));
                    } else {
                        userDAO.findByUsername(finalConnObjectKeyValue).ifPresent(anys::add);
                    }
                }

                case "name" -> {
                    if (anyTypeKind == AnyTypeKind.GROUP && ignoreCaseMatch) {
                        AnyCond cond = new AnyCond(AttrCond.Type.IEQ);
                        cond.setSchema("name");
                        cond.setExpression(finalConnObjectKeyValue);
                        anys.addAll(anySearchDAO.search(SearchCond.of(cond), AnyTypeKind.GROUP));
                    } else {
                        groupDAO.findByName(finalConnObjectKeyValue).ifPresent(anys::add);
                    }

                    if (anyTypeKind == AnyTypeKind.ANY_OBJECT && ignoreCaseMatch) {
                        AnyCond cond = new AnyCond(AttrCond.Type.IEQ);
                        cond.setSchema("name");
                        cond.setExpression(finalConnObjectKeyValue);
                        anys.addAll(anySearchDAO.search(SearchCond.of(cond), AnyTypeKind.ANY_OBJECT));
                    } else {
                        anys.addAll(anyObjectDAO.findByName(finalConnObjectKeyValue));
                    }
                }

                default -> {
                }
            }
        } else if (intAttrName.getSchemaType() != null) {
            switch (intAttrName.getSchemaType()) {
                case PLAIN -> {
                    AttrCond attrCond = new AttrCond(ignoreCaseMatch ? AttrCond.Type.IEQ : AttrCond.Type.EQ);
                    attrCond.setSchema(intAttrName.getSchema().getKey());
                    attrCond.setExpression(finalConnObjectKeyValue);
                    anys.addAll(anySearchDAO.search(SearchCond.of(attrCond), anyTypeKind));
                }

                case DERIVED ->
                    anys.addAll(anyUtils.dao().findByDerAttrValue(
                            ((DerSchema) intAttrName.getSchema()).getExpression(),
                            finalConnObjectKeyValue,
                            ignoreCaseMatch));

                default -> {
                }
            }
        }

        List<InboundMatch> result = anys.stream().
                map(any -> new InboundMatch(MatchType.ANY, any)).
                toList();

        if (resource != null) {
            userDAO.findLinkedAccount(resource, finalConnObjectKeyValue).
                    map(account -> new InboundMatch(MatchType.LINKED_ACCOUNT, account)).
                    ifPresent(result::add);
        }

        return result.isEmpty() ? noMatchResult : result;
    }

    protected List<InboundMatch> matchByCorrelationRule(
            final SyncDelta syncDelta,
            final Provision provision,
            final InboundCorrelationRule rule,
            final AnyTypeKind type) {

        List<InboundMatch> result = new ArrayList<>();

        try {
            result.addAll(anySearchDAO.search(rule.getSearchCond(syncDelta, provision), type).stream().
                    map(any -> rule.matching(any, syncDelta, provision)).
                    toList());
        } catch (Throwable t) {
            LOG.error("While searching via {}", rule.getClass().getName(), t);
        }

        if (result.isEmpty()) {
            rule.unmatching(syncDelta, provision).ifPresent(result::add);
        }

        return result;
    }

    protected Optional<InboundCorrelationRule> rule(final ExternalResource resource, final Provision provision) {
        Optional<? extends InboundCorrelationRuleEntity> correlationRule = resource.getInboundPolicy() == null
                ? Optional.empty()
                : resource.getInboundPolicy().getCorrelationRule(provision.getAnyType());

        Optional<InboundCorrelationRule> rule = Optional.empty();
        if (correlationRule.isPresent()) {
            Implementation impl = correlationRule.get().getImplementation();
            try {
                rule = ImplementationManager.buildInboundCorrelationRule(impl,
                        () -> perContextInboundCorrelationRules.get(impl.getKey()),
                        instance -> perContextInboundCorrelationRules.put(impl.getKey(), instance));
            } catch (Exception e) {
                LOG.error("While building {}", impl, e);
            }
        }

        return rule;
    }

    /**
     * Finds internal entities based on external attributes and mapping.
     *
     * @param syncDelta change operation, including external attributes
     * @param resource external resource
     * @param provision mapping
     * @param anyTypeKind type kind
     * @return list of matching users' / groups' / any objects' keys
     */
    public List<InboundMatch> match(
            final SyncDelta syncDelta,
            final ExternalResource resource,
            final Provision provision,
            final AnyTypeKind anyTypeKind) {

        Optional<InboundCorrelationRule> rule = rule(resource, provision);

        List<InboundMatch> result = List.of();
        try {
            if (rule.isPresent()) {
                result = matchByCorrelationRule(syncDelta, provision, rule.get(), anyTypeKind);
            } else {
                String connObjectKeyValue = null;

                Optional<Item> connObjectKeyItem = MappingUtils.getConnObjectKeyItem(provision);
                if (connObjectKeyItem.isPresent()) {
                    Attribute connObjectKeyAttr = syncDelta.getObject().
                            getAttributeByName(connObjectKeyItem.get().getExtAttrName());
                    if (connObjectKeyAttr != null) {
                        connObjectKeyValue = AttributeUtil.getStringValue(connObjectKeyAttr);
                    }
                    // fallback to __UID__
                    if (connObjectKeyValue == null) {
                        connObjectKeyValue = syncDelta.getUid().getUidValue();
                    }
                }
                if (connObjectKeyValue == null) {
                    result = List.of(InboundCorrelationRule.NO_MATCH);
                } else {
                    result = matchByConnObjectKeyValue(
                            connObjectKeyItem.get(),
                            connObjectKeyValue,
                            anyTypeKind,
                            resource,
                            provision.isIgnoreCaseMatch());
                }
            }
        } catch (RuntimeException e) {
            LOG.error("Could not match {} with any existing {}", syncDelta, provision.getAnyType(), e);
        }

        if (result.size() == 1 && result.getFirst().getMatchTarget() == MatchType.ANY) {
            virAttrHandler.setValues(result.getFirst().getAny(), syncDelta.getObject());
        }

        return result;
    }

    /**
     * Finds internal realms based on external attributes and mapping.
     *
     * @param syncDelta change operation, including external attributes
     * @param orgUnit mapping
     * @return list of matching realms' keys.
     */
    @Transactional(readOnly = true)
    public List<Realm> match(final LiveSyncDelta syncDelta, final OrgUnit orgUnit) {
        String connObjectKey = null;

        Optional<Item> connObjectKeyItem = orgUnit.getConnObjectKeyItem();
        if (connObjectKeyItem.isPresent()) {
            Attribute connObjectKeyAttr = syncDelta.getObject().
                    getAttributeByName(connObjectKeyItem.get().getExtAttrName());
            if (connObjectKeyAttr != null) {
                connObjectKey = AttributeUtil.getStringValue(connObjectKeyAttr);
            }
        }
        if (connObjectKey == null) {
            return List.of();
        }

        for (ItemTransformer transformer
                : MappingUtils.getItemTransformers(connObjectKeyItem.get(), getTransformers(connObjectKeyItem.get()))) {

            List<Object> output = transformer.beforePull(
                    connObjectKeyItem.get(),
                    null,
                    List.of(connObjectKey));
            if (!CollectionUtils.isEmpty(output)) {
                connObjectKey = output.getFirst().toString();
            }
        }

        List<Realm> result = new ArrayList<>();

        switch (connObjectKeyItem.get().getIntAttrName()) {
            case "key" -> {
                realmDAO.findById(connObjectKey).ifPresent(result::add);
            }

            case "name" -> {
                if (orgUnit.isIgnoreCaseMatch()) {
                    result.addAll(realmSearchDAO.findDescendants(
                            SyncopeConstants.ROOT_REALM, connObjectKey, Pageable.unpaged()));
                } else {
                    result.addAll(realmSearchDAO.findByName(connObjectKey).stream().toList());
                }
            }

            case "fullpath" -> {
                realmSearchDAO.findByFullPath(connObjectKey).ifPresent(result::add);
            }

            default -> {
            }
        }

        return result;
    }
}

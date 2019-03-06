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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.core.persistence.api.attrvalue.validation.ParsingValidationException;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PullCorrelationRule;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.policy.PullCorrelationRuleEntity;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.OrgUnit;
import org.apache.syncope.core.persistence.api.entity.resource.OrgUnitItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.task.ProvisioningTask;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.IntAttrName;
import org.apache.syncope.core.provisioning.api.data.ItemTransformer;
import org.apache.syncope.core.provisioning.java.IntAttrNameParser;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.apache.syncope.core.spring.ImplementationManager;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncDeltaBuilder;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Component
public class PullUtils {

    private static final Logger LOG = LoggerFactory.getLogger(PullUtils.class);

    /**
     * Any Object DAO.
     */
    @Autowired
    private AnyObjectDAO anyObjectDAO;

    /**
     * User DAO.
     */
    @Autowired
    private UserDAO userDAO;

    /**
     * Group DAO.
     */
    @Autowired
    private GroupDAO groupDAO;

    /**
     * Search DAO.
     */
    @Autowired
    private AnySearchDAO searchDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private AnyUtilsFactory anyUtilsFactory;

    @Autowired
    private IntAttrNameParser intAttrNameParser;

    public Optional<String> match(
            final AnyType anyType,
            final String name,
            final ExternalResource resource,
            final Connector connector,
            final boolean ignoreCaseMatch) {

        Optional<? extends Provision> provision = resource.getProvision(anyType);
        if (!provision.isPresent()) {
            return Optional.empty();
        }

        Optional<String> result = Optional.empty();

        AnyUtils anyUtils = anyUtilsFactory.getInstance(anyType.getKind());

        List<ConnectorObject> found = new ArrayList<>();
        Name nameAttr = new Name(name);
        connector.search(provision.get().getObjectClass(),
                ignoreCaseMatch ? FilterBuilder.equalsIgnoreCase(nameAttr) : FilterBuilder.equalTo(nameAttr),
                new SearchResultsHandler() {

            @Override
            public void handleResult(final SearchResult result) {
                // nothing to do
            }

            @Override
            public boolean handle(final ConnectorObject connectorObject) {
                return found.add(connectorObject);
            }
        }, MappingUtils.buildOperationOptions(
                        MappingUtils.getPullItems(provision.get().getMapping().getItems()).iterator()));

        if (found.isEmpty()) {
            LOG.debug("No {} found on {} with __NAME__ {}", provision.get().getObjectClass(), resource, name);
        } else {
            if (found.size() > 1) {
                LOG.warn("More than one {} found on {} with __NAME__ {} - taking first only",
                        provision.get().getObjectClass(), resource, name);
            }

            ConnectorObject connObj = found.iterator().next();
            try {
                List<String> anyKeys = match(
                        new SyncDeltaBuilder().
                                setToken(new SyncToken("")).
                                setDeltaType(SyncDeltaType.CREATE_OR_UPDATE).
                                setObject(connObj).
                                build(),
                        provision.get(), anyUtils);
                if (anyKeys.isEmpty()) {
                    LOG.debug("No matching {} found for {}, aborting", anyUtils.anyTypeKind(), connObj);
                } else {
                    if (anyKeys.size() > 1) {
                        LOG.warn("More than one {} found {} - taking first only", anyUtils.anyTypeKind(), anyKeys);
                    }

                    result = Optional.ofNullable(anyKeys.iterator().next());
                }
            } catch (IllegalArgumentException e) {
                LOG.warn(e.getMessage());
            }
        }

        return result;
    }

    private List<String> findByConnObjectKey(
            final SyncDelta syncDelta, final Provision provision, final AnyUtils anyUtils) {

        String connObjectKey = null;

        Optional<? extends MappingItem> connObjectKeyItem = MappingUtils.getConnObjectKeyItem(provision);
        if (connObjectKeyItem.isPresent()) {
            Attribute connObjectKeyAttr = syncDelta.getObject().
                    getAttributeByName(connObjectKeyItem.get().getExtAttrName());
            if (connObjectKeyAttr != null) {
                connObjectKey = AttributeUtil.getStringValue(connObjectKeyAttr);
            }
        }
        if (connObjectKey == null) {
            return Collections.emptyList();
        }

        for (ItemTransformer transformer : MappingUtils.getItemTransformers(connObjectKeyItem.get())) {
            List<Object> output = transformer.beforePull(
                    connObjectKeyItem.get(),
                    null,
                    Collections.<Object>singletonList(connObjectKey));
            if (output != null && !output.isEmpty()) {
                connObjectKey = output.get(0).toString();
            }
        }

        List<String> result = new ArrayList<>();

        IntAttrName intAttrName;
        try {
            intAttrName = intAttrNameParser.parse(
                    connObjectKeyItem.get().getIntAttrName(),
                    provision.getAnyType().getKind());
        } catch (ParseException e) {
            LOG.error("Invalid intAttrName '{}' specified, ignoring", connObjectKeyItem.get().getIntAttrName(), e);
            return result;
        }

        if (intAttrName.getField() != null) {
            switch (intAttrName.getField()) {
                case "key":
                    Any<?> any = anyUtils.dao().find(connObjectKey);
                    if (any != null) {
                        result.add(any.getKey());
                    }
                    break;

                case "username":
                    if (provision.getAnyType().getKind() == AnyTypeKind.USER && provision.isIgnoreCaseMatch()) {
                        AnyCond cond = new AnyCond(AttributeCond.Type.IEQ);
                        cond.setSchema("username");
                        cond.setExpression(connObjectKey);
                        result.addAll(searchDAO.search(SearchCond.getLeafCond(cond), AnyTypeKind.USER).
                                stream().map(Entity::getKey).collect(Collectors.toList()));
                    } else {
                        User user = userDAO.findByUsername(connObjectKey);
                        if (user != null) {
                            result.add(user.getKey());
                        }
                    }
                    break;

                case "name":
                    if (provision.getAnyType().getKind() == AnyTypeKind.GROUP && provision.isIgnoreCaseMatch()) {
                        AnyCond cond = new AnyCond(AttributeCond.Type.IEQ);
                        cond.setSchema("name");
                        cond.setExpression(connObjectKey);
                        result.addAll(searchDAO.search(SearchCond.getLeafCond(cond), AnyTypeKind.GROUP).
                                stream().map(Entity::getKey).collect(Collectors.toList()));
                    } else {
                        Group group = groupDAO.findByName(connObjectKey);
                        if (group != null) {
                            result.add(group.getKey());
                        }
                    }

                    if (provision.getAnyType().getKind() == AnyTypeKind.ANY_OBJECT && provision.isIgnoreCaseMatch()) {
                        AnyCond cond = new AnyCond(AttributeCond.Type.IEQ);
                        cond.setSchema("name");
                        cond.setExpression(connObjectKey);
                        result.addAll(searchDAO.search(SearchCond.getLeafCond(cond), AnyTypeKind.ANY_OBJECT).
                                stream().map(Entity::getKey).collect(Collectors.toList()));
                    } else {
                        AnyObject anyObject = anyObjectDAO.findByName(connObjectKey);
                        if (anyObject != null) {
                            result.add(anyObject.getKey());
                        }
                    }
                    break;

                default:
            }
        } else if (intAttrName.getSchemaType() != null) {
            switch (intAttrName.getSchemaType()) {
                case PLAIN:
                    PlainAttrValue value = anyUtils.newPlainAttrValue();

                    if (intAttrName.getSchemaType() == SchemaType.PLAIN) {
                        value.setStringValue(connObjectKey);
                    } else {
                        try {
                            value.parseValue((PlainSchema) intAttrName.getSchema(), connObjectKey);
                        } catch (ParsingValidationException e) {
                            LOG.error("While parsing provided __UID__ {}", value, e);
                            value.setStringValue(connObjectKey);
                        }
                    }

                    result.addAll(anyUtils.dao().findByPlainAttrValue(
                            (PlainSchema) intAttrName.getSchema(), value, provision.isIgnoreCaseMatch()).
                            stream().map(Entity::getKey).collect(Collectors.toList()));
                    break;

                case DERIVED:
                    result.addAll(anyUtils.dao().findByDerAttrValue(
                            (DerSchema) intAttrName.getSchema(), connObjectKey, provision.isIgnoreCaseMatch()).
                            stream().map(Entity::getKey).collect(Collectors.toList()));
                    break;

                default:
            }
        }

        return result;
    }

    private List<String> findByCorrelationRule(
            final SyncDelta syncDelta,
            final Provision provision,
            final PullCorrelationRule rule,
            final AnyTypeKind type) {

        return searchDAO.search(rule.getSearchCond(syncDelta, provision), type).stream().
                map(Entity::getKey).collect(Collectors.toList());
    }

    /**
     * Finds internal entities based on external attributes and mapping.
     *
     * @param syncDelta change operation, including external attributes
     * @param provision mapping
     * @param anyUtils any utils
     * @return list of matching users' / groups' / any objects' keys
     */
    public List<String> match(
            final SyncDelta syncDelta,
            final Provision provision,
            final AnyUtils anyUtils) {

        Optional<? extends PullCorrelationRuleEntity> correlationRule = provision.getResource().getPullPolicy() == null
                ? Optional.empty()
                : provision.getResource().getPullPolicy().getCorrelationRule(provision.getAnyType());

        Optional<PullCorrelationRule> rule = Optional.empty();
        if (correlationRule.isPresent()) {
            try {
                rule = ImplementationManager.buildPullCorrelationRule(correlationRule.get().getImplementation());
            } catch (Exception e) {
                LOG.error("While building {}", correlationRule.get().getImplementation(), e);
            }
        }

        try {
            return rule.isPresent()
                    ? findByCorrelationRule(syncDelta, provision, rule.get(), anyUtils.anyTypeKind())
                    : findByConnObjectKey(syncDelta, provision, anyUtils);
        } catch (RuntimeException e) {
            LOG.error("Could not match {} with any existing {}", syncDelta, provision.getAnyType(), e);
            return Collections.<String>emptyList();
        }
    }

    /**
     * Finds internal realms based on external attributes and mapping.
     *
     * @param syncDelta change operation, including external attributes
     * @param orgUnit mapping
     * @return list of matching realms' keys.
     */
    public List<String> match(
            final SyncDelta syncDelta,
            final OrgUnit orgUnit) {

        String connObjectKey = null;

        Optional<? extends OrgUnitItem> connObjectKeyItem = orgUnit.getConnObjectKeyItem();
        if (connObjectKeyItem.isPresent()) {
            Attribute connObjectKeyAttr = syncDelta.getObject().
                    getAttributeByName(connObjectKeyItem.get().getExtAttrName());
            if (connObjectKeyAttr != null) {
                connObjectKey = AttributeUtil.getStringValue(connObjectKeyAttr);
            }
        }
        if (connObjectKey == null) {
            return Collections.emptyList();
        }

        for (ItemTransformer transformer : MappingUtils.getItemTransformers(connObjectKeyItem.get())) {
            List<Object> output = transformer.beforePull(
                    connObjectKeyItem.get(),
                    null,
                    Collections.<Object>singletonList(connObjectKey));
            if (output != null && !output.isEmpty()) {
                connObjectKey = output.get(0).toString();
            }
        }

        List<String> result = new ArrayList<>();

        Realm realm;
        switch (connObjectKeyItem.get().getIntAttrName()) {
            case "key":
                realm = realmDAO.find(connObjectKey);
                if (realm != null) {
                    result.add(realm.getKey());
                }
                break;

            case "name":
                if (orgUnit.isIgnoreCaseMatch()) {
                    final String realmName = connObjectKey;
                    result.addAll(realmDAO.findAll().stream().
                            filter(r -> r.getName().equalsIgnoreCase(realmName)).
                            map(Entity::getKey).collect(Collectors.toList()));
                } else {
                    result.addAll(realmDAO.findByName(connObjectKey).stream().
                            map(Entity::getKey).collect(Collectors.toList()));
                }
                break;

            case "fullpath":
                realm = realmDAO.findByFullPath(connObjectKey);
                if (realm != null) {
                    result.add(realm.getKey());
                }
                break;

            default:
        }

        return result;
    }

    public Boolean readEnabled(final ConnectorObject connectorObject, final ProvisioningTask task) {
        Boolean enabled = null;
        if (task.isSyncStatus()) {
            Attribute status = AttributeUtil.find(OperationalAttributes.ENABLE_NAME, connectorObject.getAttributes());
            if (status != null && status.getValue() != null && !status.getValue().isEmpty()) {
                enabled = (Boolean) status.getValue().get(0);
            }
        }

        return enabled;
    }
}

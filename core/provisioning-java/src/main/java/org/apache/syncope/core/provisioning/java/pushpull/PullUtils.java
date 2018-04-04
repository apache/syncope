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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.policy.PullPolicySpec;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.persistence.api.attrvalue.validation.ParsingValidationException;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.OrgUnit;
import org.apache.syncope.core.persistence.api.entity.resource.OrgUnitItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.task.ProvisioningTask;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.java.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.IntAttrName;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.apache.syncope.core.provisioning.api.pushpull.PullCorrelationRule;
import org.apache.syncope.core.provisioning.api.utils.EntityUtils;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.apache.syncope.core.provisioning.api.data.ItemTransformer;

@Transactional(readOnly = true)
@Component
public class PullUtils {

    private static final Logger LOG = LoggerFactory.getLogger(PullUtils.class);

    /**
     * Schema DAO.
     */
    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

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

    public String match(
            final AnyType anyType,
            final String name,
            final ExternalResource resource,
            final Connector connector) {

        Provision provision = resource.getProvision(anyType);
        if (provision == null) {
            return null;
        }

        String result = null;

        AnyUtils anyUtils = anyUtilsFactory.getInstance(anyType.getKind());

        final List<ConnectorObject> found = new ArrayList<>();
        connector.search(provision.getObjectClass(),
                new EqualsFilter(new Name(name)),
                new ResultsHandler() {

            @Override
            public boolean handle(final ConnectorObject obj) {
                return found.add(obj);
            }
        }, MappingUtils.buildOperationOptions(MappingUtils.getPullItems(provision.getMapping().getItems()).iterator()));

        if (found.isEmpty()) {
            LOG.debug("No {} found on {} with __NAME__ {}", provision.getObjectClass(), resource, name);
        } else {
            if (found.size() > 1) {
                LOG.warn("More than one {} found on {} with __NAME__ {} - taking first only",
                        provision.getObjectClass(), resource, name);
            }

            ConnectorObject connObj = found.iterator().next();
            try {
                List<String> anyKeys = match(connObj, provision, anyUtils);
                if (anyKeys.isEmpty()) {
                    LOG.debug("No matching {} found for {}, aborting", anyUtils.getAnyTypeKind(), connObj);
                } else {
                    if (anyKeys.size() > 1) {
                        LOG.warn("More than one {} found {} - taking first only", anyUtils.getAnyTypeKind(), anyKeys);
                    }

                    result = anyKeys.iterator().next();
                }
            } catch (IllegalArgumentException e) {
                LOG.warn(e.getMessage());
            }
        }

        return result;
    }

    private AnyDAO<?> getAnyDAO(final AnyTypeKind anyTypeKind) {
        return AnyTypeKind.USER == anyTypeKind
                ? userDAO
                : AnyTypeKind.ANY_OBJECT == anyTypeKind
                        ? anyObjectDAO
                        : groupDAO;
    }

    private List<String> findByConnObjectKey(
            final ConnectorObject connObj, final Provision provision, final AnyUtils anyUtils) {

        String connObjectKey = null;

        MappingItem connObjectKeyItem = MappingUtils.getConnObjectKeyItem(provision);
        if (connObjectKeyItem != null) {
            Attribute connObjectKeyAttr = connObj.getAttributeByName(connObjectKeyItem.getExtAttrName());
            if (connObjectKeyAttr != null) {
                connObjectKey = AttributeUtil.getStringValue(connObjectKeyAttr);
            }
        }
        if (connObjectKey == null) {
            return Collections.emptyList();
        }

        for (ItemTransformer transformer : MappingUtils.getItemTransformers(connObjectKeyItem)) {
            List<Object> output = transformer.beforePull(
                    connObjectKeyItem,
                    null,
                    Collections.<Object>singletonList(connObjectKey));
            if (output != null && !output.isEmpty()) {
                connObjectKey = output.get(0).toString();
            }
        }

        List<String> result = new ArrayList<>();

        IntAttrName intAttrName;
        try {
            intAttrName = intAttrNameParser.parse(connObjectKeyItem.getIntAttrName(), provision.getAnyType().getKind());
        } catch (ParseException e) {
            LOG.error("Invalid intAttrName '{}' specified, ignoring", connObjectKeyItem.getIntAttrName(), e);
            return result;
        }

        if (intAttrName.getField() != null) {
            switch (intAttrName.getField()) {
                case "key":
                    Any<?> any = getAnyDAO(provision.getAnyType().getKind()).find(connObjectKey);
                    if (any != null) {
                        result.add(any.getKey());
                    }
                    break;

                case "username":
                    User user = userDAO.findByUsername(connObjectKey);
                    if (user != null) {
                        result.add(user.getKey());
                    }
                    break;

                case "name":
                    Group group = groupDAO.findByName(connObjectKey);
                    if (group != null) {
                        result.add(group.getKey());
                    }
                    AnyObject anyObject = anyObjectDAO.findByName(connObjectKey);
                    if (anyObject != null) {
                        result.add(anyObject.getKey());
                    }
                    break;

                default:
            }
        } else if (intAttrName.getSchemaType() != null) {
            switch (intAttrName.getSchemaType()) {
                case PLAIN:
                    PlainAttrValue value = anyUtils.newPlainAttrValue();

                    PlainSchema schema = plainSchemaDAO.find(intAttrName.getSchemaName());
                    if (schema == null) {
                        value.setStringValue(connObjectKey);
                    } else {
                        try {
                            value.parseValue(schema, connObjectKey);
                        } catch (ParsingValidationException e) {
                            LOG.error("While parsing provided __UID__ {}", value, e);
                            value.setStringValue(connObjectKey);
                        }
                    }

                    List<? extends Any<?>> anys = getAnyDAO(provision.getAnyType().getKind()).
                            findByPlainAttrValue(intAttrName.getSchemaName(), value);
                    for (Any<?> any : anys) {
                        result.add(any.getKey());
                    }
                    break;

                case DERIVED:
                    anys = getAnyDAO(provision.getAnyType().getKind()).
                            findByDerAttrValue(intAttrName.getSchemaName(), connObjectKey);
                    for (Any<?> any : anys) {
                        result.add(any.getKey());
                    }
                    break;

                default:
            }
        }

        return result;
    }

    private List<String> findByCorrelationRule(
            final ConnectorObject connObj, final PullCorrelationRule rule, final AnyTypeKind type) {

        List<String> result = new ArrayList<>();
        for (Any<?> any : searchDAO.search(rule.getSearchCond(connObj), type)) {
            result.add(any.getKey());
        }

        return result;
    }

    /**
     * Finds internal entities based on external attributes and mapping.
     *
     * @param connObj external attributes
     * @param provision mapping
     * @param anyUtils any utils
     * @return list of matching users' / groups' / any objects' keys
     */
    public List<String> match(
            final ConnectorObject connObj,
            final Provision provision,
            final AnyUtils anyUtils) {

        PullPolicySpec pullPolicySpec = null;
        if (provision.getResource().getPullPolicy() != null) {
            pullPolicySpec = provision.getResource().getPullPolicy().getSpecification();
        }

        PullCorrelationRule pullRule = null;
        if (pullPolicySpec != null) {
            String pullCorrelationRule = pullPolicySpec.getCorrelationRules().get(provision.getAnyType().getKey());
            if (StringUtils.isNotBlank(pullCorrelationRule)) {
                if (pullCorrelationRule.charAt(0) == '[') {
                    pullRule = new PlainAttrsPullCorrelationRule(
                            POJOHelper.deserialize(pullCorrelationRule, String[].class), provision);
                } else {
                    try {
                        pullRule = (PullCorrelationRule) Class.forName(pullCorrelationRule).newInstance();
                    } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                        LOG.error("Failure instantiating correlation rule class '{}'", pullCorrelationRule, e);
                    }
                }
            }
        }

        try {
            return pullRule == null
                    ? findByConnObjectKey(connObj, provision, anyUtils)
                    : findByCorrelationRule(connObj, pullRule, anyUtils.getAnyTypeKind());
        } catch (RuntimeException e) {
            LOG.error("Could not match {} with any existing {}", connObj, provision.getAnyType(), e);
            return Collections.<String>emptyList();
        }
    }

    /**
     * Finds internal realms based on external attributes and mapping.
     *
     * @param connObj external attributes
     * @param orgUnit mapping
     * @return list of matching realms' keys.
     */
    public List<String> match(
            final ConnectorObject connObj,
            final OrgUnit orgUnit) {

        String connObjectKey = null;

        OrgUnitItem connObjectKeyItem = orgUnit.getConnObjectKeyItem();
        if (connObjectKeyItem != null) {
            Attribute connObjectKeyAttr = connObj.getAttributeByName(connObjectKeyItem.getExtAttrName());
            if (connObjectKeyAttr != null) {
                connObjectKey = AttributeUtil.getStringValue(connObjectKeyAttr);
            }
        }
        if (connObjectKey == null) {
            return Collections.emptyList();
        }

        for (ItemTransformer transformer : MappingUtils.getItemTransformers(connObjectKeyItem)) {
            List<Object> output = transformer.beforePull(
                    connObjectKeyItem,
                    null,
                    Collections.<Object>singletonList(connObjectKey));
            if (output != null && !output.isEmpty()) {
                connObjectKey = output.get(0).toString();
            }
        }

        List<String> result = new ArrayList<>();

        Realm realm;
        switch (connObjectKeyItem.getIntAttrName()) {
            case "key":
                realm = realmDAO.find(connObjectKey);
                if (realm != null) {
                    result.add(realm.getKey());
                }
                break;

            case "name":
                CollectionUtils.collect(realmDAO.findByName(connObjectKey), EntityUtils.keyTransformer(), result);
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

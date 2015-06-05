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
package org.apache.syncope.core.provisioning.java.sync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.SyncPolicySpec;
import org.apache.syncope.core.persistence.api.attrvalue.validation.ParsingValidationException;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.task.ProvisioningTask;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.sync.SyncCorrelationRule;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SyncUtils {

    private static final Logger LOG = LoggerFactory.getLogger(SyncUtils.class);

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
    private AnyUtilsFactory anyUtilsFactory;

    public Long findMatchingAnyKey(
            final AnyType anyType,
            final String name,
            final ExternalResource resource,
            final Connector connector) {

        Provision provision = resource.getProvision(anyType);
        if (provision == null) {
            return null;
        }

        Long result = null;

        AnyUtils anyUtils = anyUtilsFactory.getInstance(anyType.getKind());

        List<ConnectorObject> found = connector.search(provision.getObjectClass(),
                new EqualsFilter(new Name(name)), connector.getOperationOptions(
                        anyUtils.getMappingItems(provision, MappingPurpose.SYNCHRONIZATION)));

        if (found.isEmpty()) {
            LOG.debug("No {} found on {} with __NAME__ {}", provision.getObjectClass(), resource, name);
        } else {
            if (found.size() > 1) {
                LOG.warn("More than one {} found on {} with __NAME__ {} - taking first only",
                        provision.getObjectClass(), resource, name);
            }

            ConnectorObject connObj = found.iterator().next();
            try {
                List<Long> anyKeys = findExisting(connObj.getUid().getUidValue(), connObj, provision, anyUtils);
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

    private AnyDAO<?> getAnyDAO(final MappingItem connObjectKeyItem) {
        return AnyTypeKind.USER == connObjectKeyItem.getIntMappingType().getAnyTypeKind()
                ? userDAO
                : AnyTypeKind.ANY_OBJECT == connObjectKeyItem.getIntMappingType().getAnyTypeKind()
                        ? anyObjectDAO
                        : groupDAO;
    }

    private List<Long> findByConnObjectKeyItem(
            final String uid, final Provision provision, final AnyUtils anyUtils) {

        List<Long> result = new ArrayList<>();

        MappingItem connObjectKeyItem = anyUtils.getConnObjectKeyItem(provision);
        switch (connObjectKeyItem.getIntMappingType()) {
            case UserPlainSchema:
            case GroupPlainSchema:
            case AnyPlainSchema:
                PlainAttrValue value = anyUtils.newPlainAttrValue();

                PlainSchema schema = plainSchemaDAO.find(connObjectKeyItem.getIntAttrName());
                if (schema == null) {
                    value.setStringValue(uid);
                } else {
                    try {
                        value.parseValue(schema, uid);
                    } catch (ParsingValidationException e) {
                        LOG.error("While parsing provided __UID__ {}", uid, e);
                        value.setStringValue(uid);
                    }
                }

                List<? extends Any<?, ?, ?>> anys =
                        getAnyDAO(connObjectKeyItem).findByAttrValue(connObjectKeyItem.getIntAttrName(), value);
                for (Any<?, ?, ?> any : anys) {
                    result.add(any.getKey());
                }
                break;

            case UserDerivedSchema:
            case GroupDerivedSchema:
            case AnyDerivedSchema:
                anys = getAnyDAO(connObjectKeyItem).findByDerAttrValue(connObjectKeyItem.getIntAttrName(), uid);
                for (Any<?, ?, ?> any : anys) {
                    result.add(any.getKey());
                }
                break;

            case UserKey:
            case GroupKey:
            case AnyKey:
                Any<?, ?, ?> any = getAnyDAO(connObjectKeyItem).find(Long.parseLong(uid));
                if (any != null) {
                    result.add(any.getKey());
                }
                break;

            case Username:
                User user = userDAO.find(uid);
                if (user != null) {
                    result.add(user.getKey());
                }
                break;

            case GroupName:
                Group group = groupDAO.find(uid);
                if (group != null) {
                    result.add(group.getKey());
                }
                break;

            default:
                LOG.error("Invalid connObjectKey type '{}'", connObjectKeyItem.getIntMappingType());
        }

        return result;
    }

    private List<Long> search(final SearchCond searchCond, final AnyTypeKind type) {
        final List<Long> result = new ArrayList<>();

        List<Any<?, ?, ?>> anys = searchDAO.search(
                SyncopeConstants.FULL_ADMIN_REALMS, searchCond, Collections.<OrderByClause>emptyList(), type);
        for (Any<?, ?, ?> any : anys) {
            result.add(any.getKey());
        }

        return result;
    }

    private List<Long> findByCorrelationRule(
            final ConnectorObject connObj, final SyncCorrelationRule rule, final AnyTypeKind type) {

        return search(rule.getSearchCond(connObj), type);
    }

    private List<Long> findByAnySearch(
            final ConnectorObject connObj,
            final List<String> altSearchSchemas,
            final Provision provision,
            final AnyUtils anyUtils) {

        // search for external attribute's name/value of each specified name
        Map<String, Attribute> extValues = new HashMap<>();

        for (MappingItem item : anyUtils.getMappingItems(provision, MappingPurpose.SYNCHRONIZATION)) {
            extValues.put(item.getIntAttrName(), connObj.getAttributeByName(item.getExtAttrName()));
        }

        // search for user/group by attribute(s) specified in the policy
        SearchCond searchCond = null;

        for (String schema : altSearchSchemas) {
            Attribute value = extValues.get(schema);

            if (value == null) {
                throw new IllegalArgumentException(
                        "Connector object does not contains the attributes to perform the search: " + schema);
            }

            AttributeCond.Type type;
            String expression = null;

            if (value.getValue() == null || value.getValue().isEmpty()
                    || (value.getValue().size() == 1 && value.getValue().get(0) == null)) {

                type = AttributeCond.Type.ISNULL;
            } else {
                type = AttributeCond.Type.EQ;
                expression = value.getValue().size() > 1
                        ? value.getValue().toString()
                        : value.getValue().get(0).toString();
            }

            SearchCond nodeCond;
            // users: just id or username can be selected to be used
            // groups: just id or name can be selected to be used
            if ("key".equalsIgnoreCase(schema)
                    || "username".equalsIgnoreCase(schema) || "name".equalsIgnoreCase(schema)) {

                AnyCond cond = new AnyCond();
                cond.setSchema(schema);
                cond.setType(type);
                cond.setExpression(expression);

                nodeCond = SearchCond.getLeafCond(cond);
            } else {
                AttributeCond cond = new AttributeCond();
                cond.setSchema(schema);
                cond.setType(type);
                cond.setExpression(expression);

                nodeCond = SearchCond.getLeafCond(cond);
            }

            searchCond = searchCond == null
                    ? nodeCond
                    : SearchCond.getAndCond(searchCond, nodeCond);
        }

        return search(searchCond, anyUtils.getAnyTypeKind());
    }

    private SyncCorrelationRule getCorrelationRule(final Provision provision, final SyncPolicySpec policySpec) {
        String clazz = policySpec.getItem(provision.getAnyType().getKey()) == null
                ? null
                : policySpec.getItem(provision.getAnyType().getKey()).getJavaRule();

        SyncCorrelationRule res = null;

        if (StringUtils.isNotBlank(clazz)) {
            try {
                res = (SyncCorrelationRule) Class.forName(clazz).newInstance();
            } catch (Exception e) {
                LOG.error("Failure instantiating correlation rule class '{}'", clazz, e);
            }
        }

        return res;
    }

    private List<String> getAltSearchSchemas(final Provision provision, final SyncPolicySpec policySpec) {
        return policySpec.getItem(provision.getAnyType().getKey()) == null
                ? Collections.<String>emptyList()
                : policySpec.getItem(provision.getAnyType().getKey()).getAltSearchSchemas();
    }

    /**
     * Find any objects based on mapped uid value (or previous uid value, if updated).
     *
     * @param uid for finding by connObjectKey
     * @param connObj for finding by attribute value
     * @param provision external resource
     * @param anyUtils any util
     * @return list of matching users / groups
     */
    public List<Long> findExisting(
            final String uid,
            final ConnectorObject connObj,
            final Provision provision,
            final AnyUtils anyUtils) {

        SyncPolicySpec syncPolicySpec = null;
        if (provision.getResource().getSyncPolicy() != null) {
            syncPolicySpec = provision.getResource().getSyncPolicy().getSpecification(SyncPolicySpec.class);
        }

        SyncCorrelationRule syncRule = null;
        List<String> altSearchSchemas = null;

        if (syncPolicySpec != null) {
            syncRule = getCorrelationRule(provision, syncPolicySpec);
            altSearchSchemas = getAltSearchSchemas(provision, syncPolicySpec);
        }

        return syncRule == null
                ? altSearchSchemas == null || altSearchSchemas.isEmpty()
                        ? findByConnObjectKeyItem(uid, provision, anyUtils)
                        : findByAnySearch(connObj, altSearchSchemas, provision, anyUtils)
                : findByCorrelationRule(connObj, syncRule, anyUtils.getAnyTypeKind());
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

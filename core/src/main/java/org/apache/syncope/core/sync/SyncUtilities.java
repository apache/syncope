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
package org.apache.syncope.core.sync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.common.types.MappingPurpose;
import org.apache.syncope.common.types.SubjectType;
import org.apache.syncope.common.types.SyncPolicySpec;
import org.apache.syncope.core.persistence.beans.AbstractAttrValue;
import org.apache.syncope.core.persistence.beans.AbstractMappingItem;
import org.apache.syncope.core.persistence.beans.AbstractNormalSchema;
import org.apache.syncope.core.persistence.beans.AbstractSubject;
import org.apache.syncope.core.persistence.beans.AbstractSyncTask;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.SyncPolicy;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.EntitlementDAO;
import org.apache.syncope.core.persistence.dao.PolicyDAO;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.persistence.dao.SchemaDAO;
import org.apache.syncope.core.persistence.dao.SubjectSearchDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.persistence.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.dao.search.SearchCond;
import org.apache.syncope.core.persistence.dao.search.SubjectCond;
import org.apache.syncope.core.persistence.validation.attrvalue.ParsingValidationException;
import org.apache.syncope.core.propagation.Connector;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.EntitlementUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SyncUtilities {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(SyncUtilities.class);

    /**
     * Policy DAO.
     */
    @Autowired
    protected PolicyDAO policyDAO;

    /**
     * Entitlement DAO.
     */
    @Autowired
    protected EntitlementDAO entitlementDAO;

    /**
     * Schema DAO.
     */
    @Autowired
    protected SchemaDAO schemaDAO;

    /**
     * User DAO.
     */
    @Autowired
    protected UserDAO userDAO;

    /**
     * Role DAO.
     */
    @Autowired
    protected RoleDAO roleDAO;

    /**
     * Search DAO.
     */
    @Autowired
    protected SubjectSearchDAO searchDAO;

    public Long findMatchingAttributableId(
            final ObjectClass oclass,
            final String name,
            final ExternalResource resource,
            final Connector connector) {
        Long result = null;

        final AttributableUtil attrUtil = AttributableUtil.getInstance(oclass);

        final List<ConnectorObject> found = connector.search(oclass,
                new EqualsFilter(new Name(name)), connector.getOperationOptions(
                attrUtil.getMappingItems(resource, MappingPurpose.SYNCHRONIZATION)));

        if (found.isEmpty()) {
            LOG.debug("No {} found on {} with __NAME__ {}", oclass, resource, name);
        } else {
            if (found.size() > 1) {
                LOG.warn("More than one {} found on {} with __NAME__ {} - taking first only", oclass, resource, name);
            }

            ConnectorObject connObj = found.iterator().next();
            final List<Long> subjectIds = findExisting(connObj.getUid().getUidValue(), connObj, resource, attrUtil);
            if (subjectIds.isEmpty()) {
                LOG.debug("No matching {} found for {}, aborting", attrUtil.getType(), connObj);
            } else {
                if (subjectIds.size() > 1) {
                    LOG.warn("More than one {} found {} - taking first only", attrUtil.getType(), subjectIds);
                }

                result = subjectIds.iterator().next();
            }
        }

        return result;
    }

    public List<Long> findByAccountIdItem(
            final String uid, final ExternalResource resource, final AttributableUtil attrUtil) {
        final List<Long> result = new ArrayList<Long>();

        final AbstractMappingItem accountIdItem = attrUtil.getAccountIdItem(resource);
        switch (accountIdItem.getIntMappingType()) {
            case UserSchema:
            case RoleSchema:
                final AbstractAttrValue value = attrUtil.newAttrValue();

                AbstractNormalSchema schema = schemaDAO.find(accountIdItem.getIntAttrName(), attrUtil.schemaClass());
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

                List<AbstractSubject> subjects =
                        userDAO.findByAttrValue(accountIdItem.getIntAttrName(), value, attrUtil);
                for (AbstractSubject subject : subjects) {
                    result.add(subject.getId());
                }
                break;

            case UserDerivedSchema:
            case RoleDerivedSchema:
                subjects = userDAO.findByDerAttrValue(accountIdItem.getIntAttrName(), uid, attrUtil);
                for (AbstractSubject subject : subjects) {
                    result.add(subject.getId());
                }
                break;

            case Username:
                SyncopeUser user = userDAO.find(uid);
                if (user != null) {
                    result.add(user.getId());
                }
                break;

            case UserId:
                user = userDAO.find(Long.parseLong(uid));
                if (user != null) {
                    result.add(user.getId());
                }
                break;

            case RoleName:
                List<SyncopeRole> roles = roleDAO.find(uid);
                for (SyncopeRole role : roles) {
                    result.add(role.getId());
                }
                break;

            case RoleId:
                SyncopeRole role = roleDAO.find(Long.parseLong(uid));
                if (role != null) {
                    result.add(role.getId());
                }
                break;

            default:
                LOG.error("Invalid accountId type '{}'", accountIdItem.getIntMappingType());
        }

        return result;
    }

    public List<Long> search(final SearchCond searchCond, final SubjectType type) {
        final List<Long> result = new ArrayList<Long>();

        final List<AbstractSubject> subjects = searchDAO.search(
                EntitlementUtil.getRoleIds(entitlementDAO.findAll()),
                searchCond, Collections.<OrderByClause>emptyList(), type);
        for (AbstractSubject subject : subjects) {
            result.add(subject.getId());
        }

        return result;
    }

    public List<Long> findByCorrelationRule(
            final ConnectorObject connObj, final SyncCorrelationRule rule, final SubjectType type) {

        return search(rule.getSearchCond(connObj), type);
    }

    public List<Long> findByAttributableSearch(
            final ConnectorObject connObj,
            final List<String> altSearchSchemas,
            final ExternalResource resource,
            final AttributableUtil attrUtil) {

        // search for external attribute's name/value of each specified name
        final Map<String, Attribute> extValues = new HashMap<String, Attribute>();

        for (AbstractMappingItem item : attrUtil.getMappingItems(resource, MappingPurpose.SYNCHRONIZATION)) {
            extValues.put(item.getIntAttrName(), connObj.getAttributeByName(item.getExtAttrName()));
        }

        // search for user/role by attribute(s) specified in the policy
        SearchCond searchCond = null;

        for (String schema : altSearchSchemas) {
            Attribute value = extValues.get(schema);

            AttributeCond.Type type;
            String expression = null;

            if (value == null || value.getValue() == null || value.getValue().isEmpty()
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
            // roles: just id or name can be selected to be used
            if ("id".equalsIgnoreCase(schema) || "username".equalsIgnoreCase(schema)
                    || "name".equalsIgnoreCase(schema)) {

                SubjectCond cond = new SubjectCond();
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

        return search(searchCond, SubjectType.valueOf(attrUtil.getType().name()));
    }

    /**
     * Find users / roles based on mapped uid value (or previous uid value, if updated).
     *
     * @param uid for finding by account id
     * @param connObj for finding by attribute value
     * @param attrUtil attributable util
     * @return list of matching users / roles
     */
    public List<Long> findExisting(
            final String uid,
            final ConnectorObject connObj,
            final ExternalResource resource,
            final AttributableUtil attrUtil) {

        SyncPolicySpec syncPolicySpec = null;
        if (resource.getSyncPolicy() == null) {
            SyncPolicy globalSP = policyDAO.getGlobalSyncPolicy();
            if (globalSP != null) {
                syncPolicySpec = globalSP.<SyncPolicySpec>getSpecification();
            }
        } else {
            syncPolicySpec = resource.getSyncPolicy().<SyncPolicySpec>getSpecification();
        }

        SyncCorrelationRule syncRule = null;
        List<String> altSearchSchemas = null;

        if (syncPolicySpec != null) {
            syncRule = attrUtil.getCorrelationRule(syncPolicySpec);
            altSearchSchemas = attrUtil.getAltSearchSchemas(syncPolicySpec);
        }

        return syncRule == null ? altSearchSchemas == null || altSearchSchemas.isEmpty()
                ? findByAccountIdItem(uid, resource, attrUtil)
                : findByAttributableSearch(connObj, altSearchSchemas, resource, attrUtil)
                : findByCorrelationRule(connObj, syncRule, SubjectType.valueOf(attrUtil.getType().name()));
    }

    public Boolean readEnabled(final ConnectorObject connectorObject, final AbstractSyncTask task) {
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

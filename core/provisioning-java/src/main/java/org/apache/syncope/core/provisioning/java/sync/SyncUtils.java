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
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.SubjectType;
import org.apache.syncope.common.lib.types.SyncPolicySpec;
import org.apache.syncope.core.persistence.api.attrvalue.validation.ParsingValidationException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.SubjectDAO;
import org.apache.syncope.core.persistence.api.dao.SubjectSearchDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.dao.search.SubjectCond;
import org.apache.syncope.core.persistence.api.entity.AttributableUtils;
import org.apache.syncope.core.persistence.api.entity.AttributableUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.MappingItem;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Subject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.task.ProvisioningTask;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.sync.SyncCorrelationRule;
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
public class SyncUtils {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(SyncUtils.class);

    /**
     * Policy DAO.
     */
    @Autowired
    protected PolicyDAO policyDAO;

    /**
     * Schema DAO.
     */
    @Autowired
    protected PlainSchemaDAO plainSchemaDAO;

    /**
     * User DAO.
     */
    @Autowired
    protected UserDAO userDAO;

    /**
     * Group DAO.
     */
    @Autowired
    protected GroupDAO groupDAO;

    /**
     * Search DAO.
     */
    @Autowired
    protected SubjectSearchDAO searchDAO;

    @Autowired
    protected AttributableUtilsFactory attrUtilsFactory;

    public Long findMatchingAttributableKey(
            final ObjectClass oclass,
            final String name,
            final ExternalResource resource,
            final Connector connector) {

        Long result = null;

        final AttributableUtils attrUtils = attrUtilsFactory.getInstance(oclass);

        final List<ConnectorObject> found = connector.search(oclass,
                new EqualsFilter(new Name(name)), connector.getOperationOptions(
                        attrUtils.getMappingItems(resource, MappingPurpose.SYNCHRONIZATION)));

        if (found.isEmpty()) {
            LOG.debug("No {} found on {} with __NAME__ {}", oclass, resource, name);
        } else {
            if (found.size() > 1) {
                LOG.warn("More than one {} found on {} with __NAME__ {} - taking first only", oclass, resource, name);
            }

            ConnectorObject connObj = found.iterator().next();
            try {
                List<Long> subjectKeys = findExisting(connObj.getUid().getUidValue(), connObj, resource, attrUtils);
                if (subjectKeys.isEmpty()) {
                    LOG.debug("No matching {} found for {}, aborting", attrUtils.getType(), connObj);
                } else {
                    if (subjectKeys.size() > 1) {
                        LOG.warn("More than one {} found {} - taking first only", attrUtils.getType(), subjectKeys);
                    }

                    result = subjectKeys.iterator().next();
                }
            } catch (IllegalArgumentException e) {
                LOG.warn(e.getMessage());
            }
        }

        return result;
    }

    private SubjectDAO<?, ?, ?> getSubjectDAO(final MappingItem accountIdItem) {
        return AttributableType.USER == accountIdItem.getIntMappingType().getAttributableType() ? userDAO : groupDAO;
    }

    private List<Long> findByAccountIdItem(
            final String uid, final ExternalResource resource, final AttributableUtils attrUtils) {
        final List<Long> result = new ArrayList<>();

        final MappingItem accountIdItem = attrUtils.getAccountIdItem(resource);
        switch (accountIdItem.getIntMappingType()) {
            case UserPlainSchema:
            case GroupPlainSchema:
                final PlainAttrValue value = attrUtils.newPlainAttrValue();

                PlainSchema schema = plainSchemaDAO.find(accountIdItem.getIntAttrName(), attrUtils.plainSchemaClass());
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

                List<? extends Subject<?, ?, ?>> subjects =
                        getSubjectDAO(accountIdItem).findByAttrValue(accountIdItem.getIntAttrName(), value, attrUtils);
                for (Subject<?, ?, ?> subject : subjects) {
                    result.add(subject.getKey());
                }
                break;

            case UserDerivedSchema:
            case GroupDerivedSchema:
                subjects = getSubjectDAO(accountIdItem).
                        findByDerAttrValue(accountIdItem.getIntAttrName(), uid, attrUtils);
                for (Subject<?, ?, ?> subject : subjects) {
                    result.add(subject.getKey());
                }
                break;

            case Username:
                User user = userDAO.find(uid);
                if (user != null) {
                    result.add(user.getKey());
                }
                break;

            case UserId:
                user = userDAO.find(Long.parseLong(uid));
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

            case GroupId:
                group = groupDAO.find(Long.parseLong(uid));
                if (group != null) {
                    result.add(group.getKey());
                }
                break;

            default:
                LOG.error("Invalid accountId type '{}'", accountIdItem.getIntMappingType());
        }

        return result;
    }

    private List<Long> search(final SearchCond searchCond, final SubjectType type) {
        final List<Long> result = new ArrayList<>();

        List<Subject<?, ?, ?>> subjects = searchDAO.search(
                SyncopeConstants.FULL_ADMIN_REALMS, searchCond, Collections.<OrderByClause>emptyList(), type);
        for (Subject<?, ?, ?> subject : subjects) {
            result.add(subject.getKey());
        }

        return result;
    }

    private List<Long> findByCorrelationRule(
            final ConnectorObject connObj, final SyncCorrelationRule rule, final SubjectType type) {

        return search(rule.getSearchCond(connObj), type);
    }

    private List<Long> findByAttributableSearch(
            final ConnectorObject connObj,
            final List<String> altSearchSchemas,
            final ExternalResource resource,
            final AttributableUtils attrUtils) {

        // search for external attribute's name/value of each specified name
        final Map<String, Attribute> extValues = new HashMap<>();

        for (MappingItem item : attrUtils.getMappingItems(resource, MappingPurpose.SYNCHRONIZATION)) {
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

        return search(searchCond, SubjectType.valueOf(attrUtils.getType().name()));
    }

    private SyncCorrelationRule getCorrelationRule(final AttributableType type, final SyncPolicySpec policySpec) {
        String clazz;

        switch (type) {
            case USER:
                clazz = policySpec.getUserJavaRule();
                break;
            case GROUP:
                clazz = policySpec.getGroupJavaRule();
                break;
            case MEMBERSHIP:
            case CONFIGURATION:
            default:
                clazz = null;
        }

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

    private List<String> getAltSearchSchemas(final AttributableType type, final SyncPolicySpec policySpec) {
        List<String> result = Collections.emptyList();

        switch (type) {
            case USER:
                result = policySpec.getuAltSearchSchemas();
                break;
            case GROUP:
                result = policySpec.getrAltSearchSchemas();
                break;
            case MEMBERSHIP:
            case CONFIGURATION:
            default:
        }

        return result;
    }

    /**
     * Find users / groups based on mapped uid value (or previous uid value, if updated).
     *
     * @param uid for finding by account id
     * @param connObj for finding by attribute value
     * @param resource external resource
     * @param attrUtils attributable util
     * @return list of matching users / groups
     */
    public List<Long> findExisting(
            final String uid,
            final ConnectorObject connObj,
            final ExternalResource resource,
            final AttributableUtils attrUtils) {

        SyncPolicySpec syncPolicySpec = null;
        if (resource.getSyncPolicy() != null) {
            syncPolicySpec = resource.getSyncPolicy().getSpecification(SyncPolicySpec.class);
        }

        SyncCorrelationRule syncRule = null;
        List<String> altSearchSchemas = null;

        if (syncPolicySpec != null) {
            syncRule = getCorrelationRule(attrUtils.getType(), syncPolicySpec);
            altSearchSchemas = getAltSearchSchemas(attrUtils.getType(), syncPolicySpec);
        }

        return syncRule == null ? altSearchSchemas == null || altSearchSchemas.isEmpty()
                ? findByAccountIdItem(uid, resource, attrUtils)
                : findByAttributableSearch(connObj, altSearchSchemas, resource, attrUtils)
                : findByCorrelationRule(connObj, syncRule, SubjectType.valueOf(attrUtils.getType().name()));
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

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
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.policy.SyncPolicySpec;
import org.apache.syncope.core.misc.utils.MappingUtils;
import org.apache.syncope.core.misc.serialization.POJOHelper;
import org.apache.syncope.core.persistence.api.attrvalue.validation.ParsingValidationException;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
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
import org.apache.syncope.core.provisioning.api.data.MappingItemTransformer;
import org.apache.syncope.core.provisioning.api.sync.SyncCorrelationRule;
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

@Transactional(readOnly = true)
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

        final List<ConnectorObject> found = new ArrayList<>();
        connector.search(
                provision.getObjectClass(),
                new EqualsFilter(new Name(name)),
                new ResultsHandler() {

            @Override
            public boolean handle(final ConnectorObject obj) {
                return found.add(obj);
            }
        },
                MappingUtils.buildOperationOptions(MappingUtils.getSyncMappingItems(provision).iterator()));

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

        MappingItem connObjectKeyItem = MappingUtils.getConnObjectKeyItem(provision);

        String transfUid = uid;
        for (MappingItemTransformer transformer : MappingUtils.getMappingItemTransformers(connObjectKeyItem)) {
            List<Object> output = transformer.beforeSync(Collections.<Object>singletonList(transfUid));
            if (output != null && !output.isEmpty()) {
                transfUid = output.get(0).toString();
            }
        }

        switch (connObjectKeyItem.getIntMappingType()) {
            case UserPlainSchema:
            case GroupPlainSchema:
            case AnyObjectPlainSchema:
                PlainAttrValue value = anyUtils.newPlainAttrValue();

                PlainSchema schema = plainSchemaDAO.find(connObjectKeyItem.getIntAttrName());
                if (schema == null) {
                    value.setStringValue(transfUid);
                } else {
                    try {
                        value.parseValue(schema, transfUid);
                    } catch (ParsingValidationException e) {
                        LOG.error("While parsing provided __UID__ {}", transfUid, e);
                        value.setStringValue(transfUid);
                    }
                }

                List<? extends Any<?>> anys =
                        getAnyDAO(connObjectKeyItem).findByAttrValue(connObjectKeyItem.getIntAttrName(), value);
                for (Any<?> any : anys) {
                    result.add(any.getKey());
                }
                break;

            case UserDerivedSchema:
            case GroupDerivedSchema:
            case AnyObjectDerivedSchema:
                anys = getAnyDAO(connObjectKeyItem).findByDerAttrValue(connObjectKeyItem.getIntAttrName(), transfUid);
                for (Any<?> any : anys) {
                    result.add(any.getKey());
                }
                break;

            case UserKey:
            case GroupKey:
            case AnyObjectKey:
                Any<?> any = getAnyDAO(connObjectKeyItem).find(Long.parseLong(transfUid));
                if (any != null) {
                    result.add(any.getKey());
                }
                break;

            case Username:
                User user = userDAO.find(transfUid);
                if (user != null) {
                    result.add(user.getKey());
                }
                break;

            case GroupName:
                Group group = groupDAO.find(transfUid);
                if (group != null) {
                    result.add(group.getKey());
                }
                break;

            default:
                LOG.error("Invalid connObjectKey type '{}'", connObjectKeyItem.getIntMappingType());
        }

        return result;
    }

    private List<Long> findByCorrelationRule(
            final ConnectorObject connObj, final SyncCorrelationRule rule, final AnyTypeKind type) {

        List<Long> result = new ArrayList<>();
        for (Any<?> any : searchDAO.search(rule.getSearchCond(connObj), type)) {
            result.add(any.getKey());
        }

        return result;
    }

    private SyncCorrelationRule getCorrelationRule(final Provision provision, final SyncPolicySpec policySpec) {
        SyncCorrelationRule result = null;

        String syncCorrelationRule = policySpec.getCorrelationRules().get(provision.getAnyType().getKey());
        if (StringUtils.isNotBlank(syncCorrelationRule)) {
            if (syncCorrelationRule.charAt(0) == '[') {
                result = new PlainAttrsSyncCorrelationRule(
                        POJOHelper.deserialize(syncCorrelationRule, String[].class), provision);
            } else {
                try {
                    result = (SyncCorrelationRule) Class.forName(syncCorrelationRule).newInstance();
                } catch (Exception e) {
                    LOG.error("Failure instantiating correlation rule class '{}'", syncCorrelationRule, e);
                }
            }
        }

        return result;
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
            syncPolicySpec = provision.getResource().getSyncPolicy().getSpecification();
        }

        SyncCorrelationRule syncRule = null;
        if (syncPolicySpec != null) {
            syncRule = getCorrelationRule(provision, syncPolicySpec);
        }

        return syncRule == null
                ? findByConnObjectKeyItem(uid, provision, anyUtils)
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

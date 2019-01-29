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
package org.apache.syncope.core.provisioning.java.data;

import java.text.ParseException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.syncope.common.lib.to.OIDCProviderTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.OIDCProviderDAO;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.OIDCEntityFactory;
import org.apache.syncope.core.persistence.api.entity.OIDCProvider;
import org.apache.syncope.core.persistence.api.entity.OIDCProviderItem;
import org.apache.syncope.core.persistence.api.entity.OIDCUserTemplate;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.provisioning.api.IntAttrName;
import org.apache.syncope.core.provisioning.api.data.OIDCProviderDataBinder;
import org.apache.syncope.core.provisioning.api.utils.EntityUtils;
import org.apache.syncope.core.provisioning.java.IntAttrNameParser;
import org.apache.syncope.core.provisioning.java.jexl.JexlUtils;
import org.apache.syncope.core.spring.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OIDCProviderDataBinderImpl implements OIDCProviderDataBinder {

    private static final Logger LOG = LoggerFactory.getLogger(OIDCProviderDataBinder.class);

    private static final String[] ITEM_IGNORE_PROPERTIES = { "key", "purpose" };

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private OIDCProviderDAO oidcOPDAO;

    @Autowired
    private OIDCEntityFactory entityFactory;

    @Autowired
    private IntAttrNameParser intAttrNameParser;

    @Override
    public OIDCProvider create(final OIDCProviderTO opTO) {
        return update(entityFactory.newEntity(OIDCProvider.class), opTO);

    }

    private void populateItems(
            final OIDCProviderTO opTO,
            final OIDCProvider op,
            final AnyTypeClassTO allowedSchemas) {

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();
        SyncopeClientException invalidMapping =
                SyncopeClientException.build(ClientExceptionType.InvalidMapping);
        SyncopeClientException requiredValuesMissing =
                SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);

        for (ItemTO itemTO : opTO.getItems()) {
            if (itemTO == null) {
                LOG.error("Null {}", ItemTO.class.getSimpleName());
                invalidMapping.getElements().add("Null " + ItemTO.class.getSimpleName());
            } else if (itemTO.getIntAttrName() == null) {
                requiredValuesMissing.getElements().add("intAttrName");
                scce.addException(requiredValuesMissing);
            } else {
                IntAttrName intAttrName = null;
                try {
                    intAttrName = intAttrNameParser.parse(itemTO.getIntAttrName(), AnyTypeKind.USER);
                } catch (ParseException e) {
                    LOG.error("Invalid intAttrName '{}' specified, ignoring", itemTO.getIntAttrName(), e);
                }

                if (intAttrName == null || intAttrName.getSchemaType() == null && intAttrName.getField() == null) {
                    LOG.error("'{}' not existing", itemTO.getIntAttrName());
                    invalidMapping.getElements().add("'" + itemTO.getIntAttrName() + "' not existing");
                } else {
                    boolean allowed = true;
                    if (intAttrName.getSchemaType() != null
                            && intAttrName.getEnclosingGroup() == null
                            && intAttrName.getRelatedAnyObject() == null) {
                        switch (intAttrName.getSchemaType()) {
                            case PLAIN:
                                allowed = allowedSchemas.getPlainSchemas().contains(intAttrName.getSchema().getKey());
                                break;

                            case DERIVED:
                                allowed = allowedSchemas.getDerSchemas().contains(intAttrName.getSchema().getKey());
                                break;

                            case VIRTUAL:
                                allowed = allowedSchemas.getVirSchemas().contains(intAttrName.getSchema().getKey());
                                break;

                            default:
                        }
                    }

                    if (allowed) {
                        // no mandatory condition implies mandatory condition false
                        if (!JexlUtils.isExpressionValid(itemTO.getMandatoryCondition() == null
                                ? "false" : itemTO.getMandatoryCondition())) {

                            SyncopeClientException invalidMandatoryCondition = SyncopeClientException.build(
                                    ClientExceptionType.InvalidValues);
                            invalidMandatoryCondition.getElements().add(itemTO.getMandatoryCondition());
                            scce.addException(invalidMandatoryCondition);
                        }

                        OIDCProviderItem item = entityFactory.newEntity(OIDCProviderItem.class);
                        BeanUtils.copyProperties(itemTO, item, ITEM_IGNORE_PROPERTIES);
                        item.setOP(op);
                        item.setPurpose(MappingPurpose.NONE);
                        if (item.isConnObjectKey()) {
                            if (intAttrName.getSchemaType() == SchemaType.VIRTUAL) {
                                invalidMapping.getElements().
                                        add("Virtual attributes cannot be set as ConnObjectKey");
                            }
                            if ("password".equals(intAttrName.getField())) {
                                invalidMapping.getElements().add(
                                        "Password attributes cannot be set as ConnObjectKey");
                            }

                            op.setConnObjectKeyItem(item);
                        } else {
                            op.add(item);
                        }
                    } else {
                        LOG.error("'{}' not allowed", itemTO.getIntAttrName());
                        invalidMapping.getElements().add("'" + itemTO.getIntAttrName() + "' not allowed");
                    }
                }
            }
        }

        if (!invalidMapping.getElements().isEmpty()) {
            scce.addException(invalidMapping);
        }
        if (scce.hasExceptions()) {
            throw scce;
        }
    }

    @Override
    public OIDCProvider update(final OIDCProvider op, final OIDCProviderTO opTO) {
        op.setAuthorizationEndpoint(opTO.getAuthorizationEndpoint());
        op.setClientID(opTO.getClientID());
        op.setClientSecret(opTO.getClientSecret());
        op.setName(opTO.getName());
        op.setIssuer(opTO.getIssuer());
        op.setJwksUri(opTO.getJwksUri());
        op.setTokenEndpoint(opTO.getTokenEndpoint());
        op.setUserinfoEndpoint(opTO.getUserinfoEndpoint());
        op.setEndSessionEndpoint(opTO.getEndSessionEndpoint());
        op.setHasDiscovery(opTO.getHasDiscovery());
        op.setCreateUnmatching(opTO.isCreateUnmatching());
        op.setSelfRegUnmatching(opTO.isSelfRegUnmatching());
        op.setUpdateMatching(opTO.isUpdateMatching());

        if (opTO.getUserTemplate() == null) {
            op.setUserTemplate(null);
        } else {
            OIDCUserTemplate userTemplate = op.getUserTemplate();
            if (userTemplate == null) {
                userTemplate = entityFactory.newEntity(OIDCUserTemplate.class);
                userTemplate.setAnyType(anyTypeDAO.findUser());
                userTemplate.setOP(op);
                op.setUserTemplate(userTemplate);
            }
            userTemplate.set(opTO.getUserTemplate());
        }

        op.getItems().clear();
        AnyTypeClassTO allowedSchemas = new AnyTypeClassTO();
        for (AnyTypeClass anyTypeClass : anyTypeDAO.findUser().getClasses()) {
            allowedSchemas.getPlainSchemas().addAll(
                    CollectionUtils.collect(anyTypeClass.getPlainSchemas(),
                            EntityUtils.<PlainSchema>keyTransformer()));
            allowedSchemas.getDerSchemas().addAll(
                    CollectionUtils.collect(anyTypeClass.getDerSchemas(),
                            EntityUtils.<DerSchema>keyTransformer()));
            allowedSchemas.getVirSchemas().addAll(
                    CollectionUtils.collect(anyTypeClass.getVirSchemas(),
                            EntityUtils.<VirSchema>keyTransformer()));
        }
        populateItems(opTO, op, allowedSchemas);

        op.getActionsClassNames().clear();
        op.getActionsClassNames().addAll(opTO.getActionsClassNames());

        return oidcOPDAO.save(op);
    }

    private void populateItems(final OIDCProvider op, final OIDCProviderTO opTO) {
        for (OIDCProviderItem item : op.getItems()) {
            ItemTO itemTO = new ItemTO();
            itemTO.setKey(item.getKey());
            BeanUtils.copyProperties(item, itemTO, ITEM_IGNORE_PROPERTIES);
            itemTO.setPurpose(MappingPurpose.NONE);

            if (itemTO.isConnObjectKey()) {
                opTO.setConnObjectKeyItem(itemTO);
            } else {
                opTO.add(itemTO);
            }
        }
    }

    @Override
    public OIDCProviderTO getOIDCProviderTO(final OIDCProvider op) {
        OIDCProviderTO opTO = new OIDCProviderTO();

        opTO.setKey(op.getKey());
        opTO.setAuthorizationEndpoint(op.getAuthorizationEndpoint());
        opTO.setClientID(op.getClientID());
        opTO.setClientSecret(op.getClientSecret());
        opTO.setName(op.getName());
        opTO.setIssuer(op.getIssuer());
        opTO.setJwksUri(op.getJwksUri());
        opTO.setTokenEndpoint(op.getTokenEndpoint());
        opTO.setUserinfoEndpoint(op.getUserinfoEndpoint());
        opTO.setEndSessionEndpoint(op.getEndSessionEndpoint());
        opTO.setHasDiscovery(op.getHasDiscovery());
        opTO.setCreateUnmatching(op.isCreateUnmatching());
        opTO.setSelfRegUnmatching(op.isSelfRegUnmatching());
        opTO.setUpdateMatching(op.isUpdateMatching());

        if (op.getUserTemplate() != null) {
            opTO.setUserTemplate((UserTO) op.getUserTemplate().get());
        }

        populateItems(op, opTO);

        opTO.getActionsClassNames().addAll(op.getActionsClassNames());

        return opTO;
    }

}

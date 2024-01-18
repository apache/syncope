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
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.OIDCC4UIProviderTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.OIDCC4UIProviderDAO;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.OIDCC4UIEntityFactory;
import org.apache.syncope.core.persistence.api.entity.OIDCC4UIProvider;
import org.apache.syncope.core.persistence.api.entity.OIDCC4UIUserTemplate;
import org.apache.syncope.core.provisioning.api.IntAttrName;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.data.OIDCC4UIProviderDataBinder;
import org.apache.syncope.core.provisioning.api.jexl.JexlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OIDCC4UIProviderDataBinderImpl implements OIDCC4UIProviderDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(OIDCC4UIProviderDataBinder.class);

    protected final AnyTypeDAO anyTypeDAO;

    protected final OIDCC4UIProviderDAO oidcOPDAO;

    protected final ImplementationDAO implementationDAO;

    protected final OIDCC4UIEntityFactory entityFactory;

    protected final IntAttrNameParser intAttrNameParser;

    public OIDCC4UIProviderDataBinderImpl(
            final AnyTypeDAO anyTypeDAO,
            final OIDCC4UIProviderDAO oidcOPDAO,
            final ImplementationDAO implementationDAO,
            final OIDCC4UIEntityFactory entityFactory,
            final IntAttrNameParser intAttrNameParser) {

        this.anyTypeDAO = anyTypeDAO;
        this.oidcOPDAO = oidcOPDAO;
        this.implementationDAO = implementationDAO;
        this.entityFactory = entityFactory;
        this.intAttrNameParser = intAttrNameParser;
    }

    @Override
    public OIDCC4UIProvider create(final OIDCC4UIProviderTO opTO) {
        return update(entityFactory.newEntity(OIDCC4UIProvider.class), opTO);
    }

    protected void populateItems(final OIDCC4UIProviderTO opTO, final OIDCC4UIProvider op) {
        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();
        SyncopeClientException invalidMapping =
                SyncopeClientException.build(ClientExceptionType.InvalidMapping);
        SyncopeClientException requiredValuesMissing =
                SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);

        opTO.getItems().forEach(itemTO -> {
            if (itemTO == null) {
                LOG.error("Null {}", Item.class.getSimpleName());
                invalidMapping.getElements().add("Null " + Item.class.getSimpleName());
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
                    invalidMapping.getElements().add('\'' + itemTO.getIntAttrName() + "' not existing");
                } else {
                    // no mandatory condition implies mandatory condition false
                    if (!JexlUtils.isExpressionValid(itemTO.getMandatoryCondition() == null
                            ? "false" : itemTO.getMandatoryCondition())) {

                        SyncopeClientException invalidMandatoryCondition = SyncopeClientException.build(
                                ClientExceptionType.InvalidValues);
                        invalidMandatoryCondition.getElements().add(itemTO.getMandatoryCondition());
                        scce.addException(invalidMandatoryCondition);
                    }

                    Item item = new Item();
                    item.setIntAttrName(itemTO.getIntAttrName());
                    item.setExtAttrName(itemTO.getExtAttrName());
                    item.setMandatoryCondition(itemTO.getMandatoryCondition());
                    item.setConnObjectKey(itemTO.isConnObjectKey());
                    item.setPassword(itemTO.isPassword());
                    item.setPropagationJEXLTransformer(itemTO.getPropagationJEXLTransformer());
                    item.setPullJEXLTransformer(itemTO.getPullJEXLTransformer());
                    item.setPurpose(MappingPurpose.NONE);

                    itemTO.getTransformers().forEach(transformerKey -> {
                        implementationDAO.findById(transformerKey).ifPresentOrElse(
                                transformer -> item.getTransformers().add(transformer.getKey()),
                                () -> LOG.debug("Invalid {} {}, ignoring...",
                                        Implementation.class.getSimpleName(), transformerKey));
                        // remove all implementations not contained in the TO
                        item.getTransformers().
                                removeIf(implementation -> !itemTO.getTransformers().contains(implementation));
                    });

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
                        op.getItems().add(item);
                    }
                }
            }
        });

        if (!invalidMapping.getElements().isEmpty()) {
            scce.addException(invalidMapping);
        }
        if (scce.hasExceptions()) {
            throw scce;
        }
    }

    @Override
    public OIDCC4UIProvider update(final OIDCC4UIProvider op, final OIDCC4UIProviderTO opTO) {
        op.setAuthorizationEndpoint(opTO.getAuthorizationEndpoint());
        op.setClientID(opTO.getClientID());
        op.setClientSecret(opTO.getClientSecret());
        op.setName(opTO.getName());
        op.setIssuer(opTO.getIssuer());
        op.setJwksUri(opTO.getJwksUri());
        op.setTokenEndpoint(opTO.getTokenEndpoint());
        op.setUserinfoEndpoint(opTO.getUserinfoEndpoint());
        op.setEndSessionEndpoint(opTO.getEndSessionEndpoint());
        op.setScopes(opTO.getScopes());
        op.setHasDiscovery(opTO.getHasDiscovery());
        op.setCreateUnmatching(opTO.isCreateUnmatching());
        op.setSelfRegUnmatching(opTO.isSelfRegUnmatching());
        op.setUpdateMatching(opTO.isUpdateMatching());

        if (opTO.getUserTemplate() == null) {
            op.setUserTemplate(null);
        } else {
            OIDCC4UIUserTemplate userTemplate = op.getUserTemplate();
            if (userTemplate == null) {
                userTemplate = entityFactory.newEntity(OIDCC4UIUserTemplate.class);
                userTemplate.setAnyType(anyTypeDAO.getUser());
                userTemplate.setOP(op);
                op.setUserTemplate(userTemplate);
            }
            userTemplate.set(opTO.getUserTemplate());
        }

        op.getItems().clear();
        populateItems(opTO, op);

        opTO.getActions().forEach(action -> implementationDAO.findById(action).ifPresentOrElse(
                op::add,
                () -> LOG.debug("Invalid {} {}, ignoring...", Implementation.class.getSimpleName(), action)));
        // remove all implementations not contained in the TO
        op.getActions().removeIf(impl -> !opTO.getActions().contains(impl.getKey()));

        return oidcOPDAO.save(op);
    }

    protected static void populateItems(final OIDCC4UIProvider op, final OIDCC4UIProviderTO opTO) {
        op.getItems().forEach(item -> {
            Item itemTO = new Item();
            itemTO.setIntAttrName(item.getIntAttrName());
            itemTO.setExtAttrName(item.getExtAttrName());
            itemTO.setMandatoryCondition(item.getMandatoryCondition());
            itemTO.setConnObjectKey(item.isConnObjectKey());
            itemTO.setPassword(item.isPassword());
            itemTO.setPropagationJEXLTransformer(item.getPropagationJEXLTransformer());
            itemTO.setPullJEXLTransformer(item.getPullJEXLTransformer());
            itemTO.getTransformers().addAll(item.getTransformers());
            itemTO.setPurpose(MappingPurpose.NONE);

            if (itemTO.isConnObjectKey()) {
                opTO.setConnObjectKeyItem(itemTO);
            } else {
                opTO.add(itemTO);
            }
        });
    }

    @Override
    public OIDCC4UIProviderTO getOIDCProviderTO(final OIDCC4UIProvider op) {
        OIDCC4UIProviderTO opTO = new OIDCC4UIProviderTO();

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
        opTO.getScopes().addAll(op.getScopes());
        opTO.setHasDiscovery(op.getHasDiscovery());
        opTO.setCreateUnmatching(op.isCreateUnmatching());
        opTO.setSelfRegUnmatching(op.isSelfRegUnmatching());
        opTO.setUpdateMatching(op.isUpdateMatching());

        if (op.getUserTemplate() != null) {
            opTO.setUserTemplate((UserTO) op.getUserTemplate().get());
        }

        populateItems(op, opTO);

        opTO.getActions().addAll(op.getActions().stream().map(Entity::getKey).toList());

        return opTO;
    }
}

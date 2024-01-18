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
import java.util.Base64;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.SAML2SP4UIIdPTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.SAML2SP4UIIdPDAO;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.SAML2SP4UIEntityFactory;
import org.apache.syncope.core.persistence.api.entity.SAML2SP4UIIdP;
import org.apache.syncope.core.persistence.api.entity.SAML2SP4UIUserTemplate;
import org.apache.syncope.core.provisioning.api.IntAttrName;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.data.SAML2SP4UIIdPDataBinder;
import org.apache.syncope.core.provisioning.api.jexl.JexlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SAML2SP4UIIdPDataBinderImpl implements SAML2SP4UIIdPDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(SAML2SP4UIIdPDataBinder.class);

    protected final AnyTypeDAO anyTypeDAO;

    protected final SAML2SP4UIIdPDAO idapDAO;

    protected final ImplementationDAO implementationDAO;

    protected final SAML2SP4UIEntityFactory entityFactory;

    protected final IntAttrNameParser intAttrNameParser;

    public SAML2SP4UIIdPDataBinderImpl(
            final AnyTypeDAO anyTypeDAO,
            final SAML2SP4UIIdPDAO idapDAO,
            final ImplementationDAO implementationDAO,
            final SAML2SP4UIEntityFactory entityFactory,
            final IntAttrNameParser intAttrNameParser) {

        this.anyTypeDAO = anyTypeDAO;
        this.idapDAO = idapDAO;
        this.implementationDAO = implementationDAO;
        this.entityFactory = entityFactory;
        this.intAttrNameParser = intAttrNameParser;
    }

    @Override
    public SAML2SP4UIIdP create(final SAML2SP4UIIdPTO idpTO) {
        return update(entityFactory.newEntity(SAML2SP4UIIdP.class), idpTO);
    }

    protected void populateItems(final SAML2SP4UIIdPTO idpTO, final SAML2SP4UIIdP idp) {
        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();
        SyncopeClientException invalidMapping =
                SyncopeClientException.build(ClientExceptionType.InvalidMapping);
        SyncopeClientException requiredValuesMissing =
                SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);

        idpTO.getItems().forEach(itemTO -> {
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

                        idp.setConnObjectKeyItem(item);
                    } else {
                        idp.getItems().add(item);
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
    public SAML2SP4UIIdP update(final SAML2SP4UIIdP idp, final SAML2SP4UIIdPTO idpTO) {
        idp.setEntityID(idpTO.getEntityID());
        idp.setName(idpTO.getName());
        idp.setMetadata(Base64.getMimeDecoder().decode(idpTO.getMetadata()));
        idp.setLogoutSupported(idpTO.isLogoutSupported());
        idp.setCreateUnmatching(idpTO.isCreateUnmatching());
        idp.setSelfRegUnmatching(idpTO.isSelfRegUnmatching());
        idp.setUpdateMatching(idpTO.isUpdateMatching());
        idp.setBindingType(idpTO.getBindingType());

        if (idpTO.getRequestedAuthnContextProvider() == null) {
            idp.setRequestedAuthnContextProvider(null);
        } else {
            implementationDAO.findById(idpTO.getRequestedAuthnContextProvider()).ifPresentOrElse(
                    idp::setRequestedAuthnContextProvider,
                    () -> LOG.debug("Invalid {} {}, ignoring...",
                            Implementation.class.getSimpleName(), idpTO.getRequestedAuthnContextProvider()));
        }

        if (idpTO.getUserTemplate() == null) {
            idp.setUserTemplate(null);
        } else {
            SAML2SP4UIUserTemplate userTemplate = idp.getUserTemplate();
            if (userTemplate == null) {
                userTemplate = entityFactory.newEntity(SAML2SP4UIUserTemplate.class);
                userTemplate.setAnyType(anyTypeDAO.getUser());
                userTemplate.setIdP(idp);
                idp.setUserTemplate(userTemplate);
            }
            userTemplate.set(idpTO.getUserTemplate());
        }

        idp.getItems().clear();
        populateItems(idpTO, idp);

        idpTO.getActions().forEach(action -> implementationDAO.findById(action).ifPresentOrElse(
                idp::add,
                () -> LOG.debug("Invalid {} {}, ignoring...", Implementation.class.getSimpleName(), action)));
        // remove all implementations not contained in the TO
        idp.getActions().removeIf(impl -> !idpTO.getActions().contains(impl.getKey()));

        return idapDAO.save(idp);
    }

    protected void populateItems(final SAML2SP4UIIdP idp, final SAML2SP4UIIdPTO idpTO) {
        idp.getItems().forEach(item -> {
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
                idpTO.setConnObjectKeyItem(itemTO);
            } else {
                idpTO.add(itemTO);
            }
        });
    }

    @Override
    public SAML2SP4UIIdPTO getIdPTO(final SAML2SP4UIIdP idp) {
        SAML2SP4UIIdPTO idpTO = new SAML2SP4UIIdPTO();

        idpTO.setKey(idp.getKey());
        idpTO.setEntityID(idp.getEntityID());
        idpTO.setName(idp.getName());
        idpTO.setMetadata(Base64.getMimeEncoder().encodeToString(idp.getMetadata()));
        idpTO.setLogoutSupported(idp.isLogoutSupported());
        idpTO.setBindingType(idp.getBindingType());
        idpTO.setCreateUnmatching(idp.isCreateUnmatching());
        idpTO.setSelfRegUnmatching(idp.isSelfRegUnmatching());
        idpTO.setUpdateMatching(idp.isUpdateMatching());

        if (idp.getRequestedAuthnContextProvider() != null) {
            idpTO.setRequestedAuthnContextProvider(idp.getRequestedAuthnContextProvider().getKey());
        }

        if (idp.getUserTemplate() != null) {
            idpTO.setUserTemplate((UserTO) idp.getUserTemplate().get());
        }

        populateItems(idp, idpTO);

        idpTO.getActions().addAll(idp.getActions().stream().map(Entity::getKey).toList());

        return idpTO;
    }
}

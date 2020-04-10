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
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.syncope.common.lib.to.SAML2IdPTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.SAML2IdPDAO;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.SAML2EntityFactory;
import org.apache.syncope.core.persistence.api.entity.SAML2IdP;
import org.apache.syncope.core.persistence.api.entity.SAML2IdPItem;
import org.apache.syncope.core.persistence.api.entity.SAML2UserTemplate;
import org.apache.syncope.core.provisioning.api.IntAttrName;
import org.apache.syncope.core.provisioning.api.data.SAML2IdPDataBinder;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.jexl.JexlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SAML2IdPDataBinderImpl implements SAML2IdPDataBinder {

    private static final Logger LOG = LoggerFactory.getLogger(SAML2IdPDataBinder.class);

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private SAML2IdPDAO saml2IdPDAO;

    @Autowired
    private ImplementationDAO implementationDAO;

    @Autowired
    private SAML2EntityFactory entityFactory;

    @Autowired
    private IntAttrNameParser intAttrNameParser;

    @Override
    public SAML2IdP create(final SAML2IdPTO idpTO) {
        return update(entityFactory.newEntity(SAML2IdP.class), idpTO);
    }

    private void populateItems(final SAML2IdPTO idpTO, final SAML2IdP idp) {
        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();
        SyncopeClientException invalidMapping =
                SyncopeClientException.build(ClientExceptionType.InvalidMapping);
        SyncopeClientException requiredValuesMissing =
                SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);

        idpTO.getItems().forEach(itemTO -> {
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

                    SAML2IdPItem item = entityFactory.newEntity(SAML2IdPItem.class);
                    item.setIntAttrName(itemTO.getIntAttrName());
                    item.setExtAttrName(itemTO.getExtAttrName());
                    item.setMandatoryCondition(itemTO.getMandatoryCondition());
                    item.setConnObjectKey(itemTO.isConnObjectKey());
                    item.setPassword(itemTO.isPassword());
                    item.setPropagationJEXLTransformer(itemTO.getPropagationJEXLTransformer());
                    item.setPullJEXLTransformer(itemTO.getPullJEXLTransformer());
                    item.setIdP(idp);
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

                        idp.setConnObjectKeyItem(item);
                    } else {
                        idp.add(item);
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
    public SAML2IdP update(final SAML2IdP idp, final SAML2IdPTO idpTO) {
        idp.setEntityID(idpTO.getEntityID());
        idp.setName(idpTO.getName());
        idp.setMetadata(Base64.getMimeDecoder().decode(idpTO.getMetadata()));
        idp.setCreateUnmatching(idpTO.isCreateUnmatching());
        idp.setSelfRegUnmatching(idpTO.isSelfRegUnmatching());
        idp.setUpdateMatching(idpTO.isUpdateMatching());
        idp.setUseDeflateEncoding(idpTO.isUseDeflateEncoding());
        idp.setSupportUnsolicited(idpTO.isSupportUnsolicited());
        idp.setBindingType(idpTO.getBindingType());

        if (idpTO.getUserTemplate() == null) {
            idp.setUserTemplate(null);
        } else {
            SAML2UserTemplate userTemplate = idp.getUserTemplate();
            if (userTemplate == null) {
                userTemplate = entityFactory.newEntity(SAML2UserTemplate.class);
                userTemplate.setAnyType(anyTypeDAO.findUser());
                userTemplate.setIdP(idp);
                idp.setUserTemplate(userTemplate);
            }
            userTemplate.set(idpTO.getUserTemplate());
        }

        idp.getItems().clear();
        populateItems(idpTO, idp);

        idpTO.getActions().forEach(action -> {
            Implementation implementation = implementationDAO.find(action);
            if (implementation == null) {
                LOG.debug("Invalid " + Implementation.class.getSimpleName() + " {}, ignoring...", action);
            } else {
                idp.add(implementation);
            }
        });
        // remove all implementations not contained in the TO
        idp.getActions().removeIf(impl -> !idpTO.getActions().contains(impl.getKey()));

        if (idpTO.getRequestedAuthnContextProvider() == null) {
            idp.setRequestedAuthnContextProvider(null);
        } else {
            Implementation implementation = implementationDAO.find(idpTO.getRequestedAuthnContextProvider());
            if (implementation == null) {
                LOG.debug("Invalid " + Implementation.class.getSimpleName() + " {}, ignoring...",
                        idpTO.getRequestedAuthnContextProvider());
            } else {
                idp.setRequestedAuthnContextProvider(implementation);
            }
        }

        return saml2IdPDAO.save(idp);
    }

    private static void populateItems(final SAML2IdP idp, final SAML2IdPTO idpTO) {
        idp.getItems().forEach(item -> {
            ItemTO itemTO = new ItemTO();
            itemTO.setKey(item.getKey());
            itemTO.setIntAttrName(item.getIntAttrName());
            itemTO.setExtAttrName(item.getExtAttrName());
            itemTO.setMandatoryCondition(item.getMandatoryCondition());
            itemTO.setConnObjectKey(item.isConnObjectKey());
            itemTO.setPassword(item.isPassword());
            itemTO.setPropagationJEXLTransformer(item.getPropagationJEXLTransformer());
            itemTO.setPullJEXLTransformer(item.getPullJEXLTransformer());
            itemTO.setPurpose(MappingPurpose.NONE);

            if (itemTO.isConnObjectKey()) {
                idpTO.setConnObjectKeyItem(itemTO);
            } else {
                idpTO.add(itemTO);
            }
        });
    }

    @Override
    public SAML2IdPTO getIdPTO(final SAML2IdP idp) {
        SAML2IdPTO idpTO = new SAML2IdPTO();

        idpTO.setKey(idp.getKey());
        idpTO.setEntityID(idp.getEntityID());
        idpTO.setName(idp.getName());
        idpTO.setUseDeflateEncoding(idp.isUseDeflateEncoding());
        idpTO.setSupportUnsolicited(idp.isSupportUnsolicited());
        idpTO.setBindingType(idp.getBindingType());
        idpTO.setCreateUnmatching(idp.isCreateUnmatching());
        idpTO.setSelfRegUnmatching(idp.isSelfRegUnmatching());
        idpTO.setUpdateMatching(idp.isUpdateMatching());
        idpTO.setMetadata(Base64.getMimeEncoder().encodeToString(idp.getMetadata()));

        if (idp.getUserTemplate() != null) {
            idpTO.setUserTemplate((UserTO) idp.getUserTemplate().get());
        }

        populateItems(idp, idpTO);

        idpTO.getActions().addAll(
                idp.getActions().stream().map(Entity::getKey).collect(Collectors.toList()));

        if (idp.getRequestedAuthnContextProvider() != null) {
            idpTO.setRequestedAuthnContextProvider(idp.getRequestedAuthnContextProvider().getKey());
        }

        return idpTO;
    }
}

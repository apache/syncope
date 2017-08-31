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

import java.util.Base64;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.syncope.common.lib.to.SAML2IdPTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.SAML2IdPDAO;
import org.apache.syncope.core.persistence.api.entity.SAML2EntityFactory;
import org.apache.syncope.core.persistence.api.entity.SAML2IdP;
import org.apache.syncope.core.persistence.api.entity.SAML2IdPItem;
import org.apache.syncope.core.persistence.api.entity.SAML2UserTemplate;
import org.apache.syncope.core.provisioning.api.IntAttrName;
import org.apache.syncope.core.provisioning.api.data.SAML2IdPDataBinder;
import org.apache.syncope.core.provisioning.java.IntAttrNameParser;
import org.apache.syncope.core.provisioning.java.jexl.JexlUtils;
import org.apache.syncope.core.spring.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SAML2IdPDataBinderImpl implements SAML2IdPDataBinder {

    private static final Logger LOG = LoggerFactory.getLogger(SAML2IdPDataBinder.class);

    private static final String[] ITEM_IGNORE_PROPERTIES = { "key", "purpose" };

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private SAML2IdPDAO saml2IdPDAO;

    @Autowired
    private SAML2EntityFactory entityFactory;

    @Autowired
    private IntAttrNameParser intAttrNameParser;

    @Override
    public SAML2IdP create(final SAML2IdPTO idpTO) {
        return update(entityFactory.newEntity(SAML2IdP.class), idpTO);
    }

    private void populateItems(
            final SAML2IdPTO idpTO,
            final SAML2IdP idp,
            final AnyTypeClassTO allowedSchemas) {

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();
        SyncopeClientException invalidMapping = SyncopeClientException.build(ClientExceptionType.InvalidMapping);
        SyncopeClientException requiredValuesMissing = SyncopeClientException.build(
                ClientExceptionType.RequiredValuesMissing);

        for (ItemTO itemTO : idpTO.getItems()) {
            if (itemTO == null) {
                LOG.error("Null {}", ItemTO.class.getSimpleName());
                invalidMapping.getElements().add("Null " + ItemTO.class.getSimpleName());
            } else if (itemTO.getIntAttrName() == null) {
                requiredValuesMissing.getElements().add("intAttrName");
                scce.addException(requiredValuesMissing);
            } else {
                IntAttrName intAttrName = intAttrNameParser.parse(itemTO.getIntAttrName(), AnyTypeKind.USER);

                if (intAttrName.getSchemaType() == null && intAttrName.getField() == null) {
                    LOG.error("'{}' not existing", itemTO.getIntAttrName());
                    invalidMapping.getElements().add("'" + itemTO.getIntAttrName() + "' not existing");
                } else {
                    boolean allowed = true;
                    if (intAttrName.getSchemaType() != null
                            && intAttrName.getEnclosingGroup() == null
                            && intAttrName.getRelatedAnyObject() == null) {
                        switch (intAttrName.getSchemaType()) {
                            case PLAIN:
                                allowed = allowedSchemas.getPlainSchemas().contains(intAttrName.getSchemaName());
                                break;

                            case DERIVED:
                                allowed = allowedSchemas.getDerSchemas().contains(intAttrName.getSchemaName());
                                break;

                            case VIRTUAL:
                                allowed = allowedSchemas.getVirSchemas().contains(intAttrName.getSchemaName());
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

                        SAML2IdPItem item = entityFactory.newEntity(SAML2IdPItem.class);
                        BeanUtils.copyProperties(itemTO, item, ITEM_IGNORE_PROPERTIES);
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
    public SAML2IdP update(final SAML2IdP idp, final SAML2IdPTO idpTO) {
        idp.setEntityID(idpTO.getEntityID());
        idp.setName(idpTO.getName());
        idp.setMetadata(Base64.getMimeDecoder().decode(idpTO.getMetadata()));
        idp.setCreateUnmatching(idpTO.isCreateUnmatching());
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
        AnyTypeClassTO allowedSchemas = new AnyTypeClassTO();
        anyTypeDAO.findUser().getClasses().forEach(anyTypeClass -> {
            allowedSchemas.getPlainSchemas().addAll(anyTypeClass.getPlainSchemas().stream().
                    map(s -> s.getKey()).collect(Collectors.toList()));
            allowedSchemas.getDerSchemas().addAll(anyTypeClass.getDerSchemas().stream().
                    map(s -> s.getKey()).collect(Collectors.toList()));
            allowedSchemas.getVirSchemas().addAll(anyTypeClass.getVirSchemas().stream().
                    map(s -> s.getKey()).collect(Collectors.toList()));
        });
        populateItems(idpTO, idp, allowedSchemas);

        idp.getActionsClassNames().clear();
        idp.getActionsClassNames().addAll(idpTO.getActionsClassNames());

        return saml2IdPDAO.save(idp);
    }

    private void populateItems(final SAML2IdP idp, final SAML2IdPTO idpTO) {
        idp.getItems().forEach(item -> {
            ItemTO itemTO = new ItemTO();
            itemTO.setKey(item.getKey());
            BeanUtils.copyProperties(item, itemTO, ITEM_IGNORE_PROPERTIES);
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
        idpTO.setUpdateMatching(idp.isUpdateMatching());
        idpTO.setMetadata(Base64.getMimeEncoder().encodeToString(idp.getMetadata()));

        if (idp.getUserTemplate() != null) {
            idpTO.setUserTemplate((UserTO) idp.getUserTemplate().get());
        }

        populateItems(idp, idpTO);

        idpTO.getActionsClassNames().addAll(idp.getActionsClassNames());

        return idpTO;
    }
}

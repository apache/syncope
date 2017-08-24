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
package org.apache.syncope.core.logic.saml2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.SAML2LoginResponseTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.attrvalue.validation.ParsingValidationException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.IntAttrName;
import org.apache.syncope.core.provisioning.api.SAML2IdPActions;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.ItemTransformer;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.java.IntAttrNameParser;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.apache.syncope.core.provisioning.java.utils.TemplateUtils;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SAML2UserManager {

    private static final Logger LOG = LoggerFactory.getLogger(SAML2UserManager.class);

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private IntAttrNameParser intAttrNameParser;

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private TemplateUtils templateUtils;

    @Autowired
    private UserProvisioningManager provisioningManager;

    @Autowired
    private UserDataBinder binder;

    @Transactional(readOnly = true)
    public List<String> findMatchingUser(final String keyValue, final ItemTO connObjectKeyItem) {
        List<String> result = new ArrayList<>();

        String transformed = keyValue;
        for (ItemTransformer transformer : MappingUtils.getItemTransformers(connObjectKeyItem)) {
            List<Object> output = transformer.beforePull(
                    null,
                    null,
                    Collections.<Object>singletonList(transformed));
            if (output != null && !output.isEmpty()) {
                transformed = output.get(0).toString();
            }
        }

        IntAttrName intAttrName = intAttrNameParser.parse(connObjectKeyItem.getIntAttrName(), AnyTypeKind.USER);

        if (intAttrName.getField() != null) {
            switch (intAttrName.getField()) {
                case "key":
                    User byKey = userDAO.find(transformed);
                    if (byKey != null) {
                        result.add(byKey.getUsername());
                    }
                    break;

                case "username":
                    User byUsername = userDAO.findByUsername(transformed);
                    if (byUsername != null) {
                        result.add(byUsername.getUsername());
                    }
                    break;

                default:
            }
        } else if (intAttrName.getSchemaType() != null) {
            switch (intAttrName.getSchemaType()) {
                case PLAIN:
                    PlainAttrValue value = entityFactory.newEntity(UPlainAttrValue.class);

                    PlainSchema schema = plainSchemaDAO.find(intAttrName.getSchemaName());
                    if (schema == null) {
                        value.setStringValue(transformed);
                    } else {
                        try {
                            value.parseValue(schema, transformed);
                        } catch (ParsingValidationException e) {
                            LOG.error("While parsing provided key value {}", transformed, e);
                            value.setStringValue(transformed);
                        }
                    }

                    result.addAll(userDAO.findByPlainAttrValue(intAttrName.getSchemaName(), value).stream().
                            map(user -> user.getUsername()).collect(Collectors.toList()));
                    break;

                case DERIVED:
                    result.addAll(userDAO.findByDerAttrValue(intAttrName.getSchemaName(), transformed).stream().
                            map(user -> user.getUsername()).collect(Collectors.toList()));
                    break;

                default:
            }
        }

        return result;
    }

    private List<SAML2IdPActions> getActions(final SAML2IdPEntity idp) {
        List<SAML2IdPActions> actions = new ArrayList<>();

        idp.getActionsClassNames().forEach((className) -> {
            try {
                Class<?> actionsClass = Class.forName(className);
                SAML2IdPActions idpActions = (SAML2IdPActions) ApplicationContextProvider.getBeanFactory().
                        createBean(actionsClass, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, true);

                actions.add(idpActions);
            } catch (Exception e) {
                LOG.warn("Class '{}' not found", className, e);
            }
        });

        return actions;
    }

    private void fill(final SAML2IdPEntity idp, final SAML2LoginResponseTO responseTO, final UserTO userTO) {
        for (ItemTO item : idp.getItems()) {
            IntAttrName intAttrName = intAttrNameParser.parse(item.getIntAttrName(), AnyTypeKind.USER);

            List<String> values = Collections.emptyList();
            Optional<AttrTO> samlAttr = responseTO.getAttr(item.getExtAttrName());
            if (samlAttr.isPresent() && !samlAttr.get().getValues().isEmpty()) {
                values = samlAttr.get().getValues();

                List<Object> transformed = new ArrayList<>(values);
                for (ItemTransformer transformer : MappingUtils.getItemTransformers(item)) {
                    transformed = transformer.beforePull(null, userTO, transformed);
                }
                values.clear();
                for (Object value : transformed) {
                    values.add(value.toString());
                }
            }

            if (intAttrName.getField() != null) {
                switch (intAttrName.getField()) {
                    case "username":
                        if (!values.isEmpty()) {
                            userTO.setUsername(values.get(0));
                        }
                        break;

                    default:
                        LOG.warn("Unsupported: {}", intAttrName.getField());
                }
            } else if (intAttrName.getSchemaType() != null) {
                switch (intAttrName.getSchemaType()) {
                    case PLAIN:
                        Optional<AttrTO> attr = userTO.getPlainAttr(intAttrName.getSchemaName());
                        if (!attr.isPresent()) {
                            attr = Optional.of(new AttrTO.Builder().schema(intAttrName.getSchemaName()).build());
                            userTO.getPlainAttrs().add(attr.get());
                        } else {
                            attr.get().getValues().clear();
                        }
                        attr.get().getValues().addAll(values);
                        break;

                    default:
                        LOG.warn("Unsupported: {} {}", intAttrName.getSchemaType(), intAttrName.getSchemaName());
                }
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String create(final SAML2IdPEntity idp, final SAML2LoginResponseTO responseTO, final String nameID) {
        UserTO userTO = new UserTO();

        if (idp.getUserTemplate() != null) {
            templateUtils.apply(userTO, idp.getUserTemplate());
        }

        List<SAML2IdPActions> actions = getActions(idp);
        for (SAML2IdPActions action : actions) {
            userTO = action.beforeCreate(userTO, responseTO);
        }

        fill(idp, responseTO, userTO);

        if (userTO.getRealm() == null) {
            userTO.setRealm(SyncopeConstants.ROOT_REALM);
        }
        if (userTO.getUsername() == null) {
            userTO.setUsername(nameID);
        }

        Pair<String, List<PropagationStatus>> created = provisioningManager.create(userTO, false, false);
        userTO = binder.getUserTO(created.getKey());

        for (SAML2IdPActions action : actions) {
            userTO = action.afterCreate(userTO, responseTO);
        }

        return userTO.getUsername();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String update(final String username, final SAML2IdPEntity idp, final SAML2LoginResponseTO responseTO) {
        UserTO userTO = binder.getUserTO(userDAO.findKey(username));
        UserTO original = SerializationUtils.clone(userTO);

        fill(idp, responseTO, userTO);

        UserPatch userPatch = AnyOperations.diff(userTO, original, true);

        List<SAML2IdPActions> actions = getActions(idp);
        for (SAML2IdPActions action : actions) {
            userPatch = action.beforeUpdate(userPatch, responseTO);
        }

        Pair<String, List<PropagationStatus>> updated = provisioningManager.update(userPatch, false);
        userTO = binder.getUserTO(updated.getKey());

        for (SAML2IdPActions action : actions) {
            userTO = action.afterUpdate(userTO, responseTO);
        }

        return userTO.getUsername();
    }
}

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
package org.apache.syncope.core.logic.oidc;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.OIDCLoginResponseTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.core.persistence.api.attrvalue.validation.ParsingValidationException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.OIDCProvider;
import org.apache.syncope.core.persistence.api.entity.OIDCProviderItem;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.IntAttrName;
import org.apache.syncope.core.provisioning.api.OIDCProviderActions;
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
public class OIDCUserManager {

    private static final Logger LOG = LoggerFactory.getLogger(OIDCUserManager.class);

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
    public List<String> findMatchingUser(final String keyValue, final OIDCProviderItem connObjectKeyItem) {
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

        IntAttrName intAttrName;
        try {
            intAttrName = intAttrNameParser.parse(connObjectKeyItem.getIntAttrName(), AnyTypeKind.USER);
        } catch (ParseException e) {
            LOG.error("Invalid intAttrName '{}' specified, ignoring", connObjectKeyItem.getIntAttrName(), e);
            return result;
        }

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

                    if (intAttrName.getSchemaType() == SchemaType.PLAIN) {
                        value.setStringValue(transformed);
                    } else {
                        try {
                            value.parseValue((PlainSchema) intAttrName.getSchema(), transformed);
                        } catch (ParsingValidationException e) {
                            LOG.error("While parsing provided key value {}", transformed, e);
                            value.setStringValue(transformed);
                        }
                    }

                    CollectionUtils.collect(
                            userDAO.findByPlainAttrValue((PlainSchema) intAttrName.getSchema(), value),
                            new Transformer<User, String>() {

                        @Override
                        public String transform(final User input) {
                            return input.getUsername();
                        }
                    }, result);
                    break;

                case DERIVED:
                    CollectionUtils.collect(
                            userDAO.findByDerAttrValue((DerSchema) intAttrName.getSchema(), transformed),
                            new Transformer<User, String>() {

                        @Override
                        public String transform(final User input) {
                            return input.getUsername();
                        }
                    }, result);
                    break;

                default:
            }
        }

        return result;
    }

    private List<OIDCProviderActions> getActions(final OIDCProvider op) {
        List<OIDCProviderActions> actions = new ArrayList<>();

        for (String className : op.getActionsClassNames()) {
            try {
                Class<?> actionsClass = Class.forName(className);
                OIDCProviderActions opActions = (OIDCProviderActions) ApplicationContextProvider.getBeanFactory().
                        createBean(actionsClass, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, true);

                actions.add(opActions);
            } catch (Exception e) {
                LOG.warn("Class '{}' not found", className, e);
            }
        }

        return actions;
    }

    public void fill(final OIDCProvider op, final OIDCLoginResponseTO responseTO, final UserTO userTO) {
        for (OIDCProviderItem item : op.getItems()) {
            List<String> values = Collections.emptyList();
            AttrTO oidcAttr = responseTO.getAttr(item.getExtAttrName());
            if (oidcAttr != null && !oidcAttr.getValues().isEmpty()) {
                values = oidcAttr.getValues();

                List<Object> transformed = new ArrayList<Object>(values);
                for (ItemTransformer transformer : MappingUtils.getItemTransformers(item)) {
                    transformed = transformer.beforePull(null, userTO, transformed);
                }
                values.clear();
                for (Object value : transformed) {
                    if (value != null) {
                        values.add(value.toString());
                    }
                }
            }

            IntAttrName intAttrName = null;
            try {
                intAttrName = intAttrNameParser.parse(item.getIntAttrName(), AnyTypeKind.USER);
            } catch (ParseException e) {
                LOG.error("Invalid intAttrName '{}' specified, ignoring", item.getIntAttrName(), e);
            }

            if (intAttrName != null && intAttrName.getField() != null) {
                switch (intAttrName.getField()) {
                    case "username":
                        if (!values.isEmpty()) {
                            userTO.setUsername(values.get(0));
                        }
                        break;

                    default:
                        LOG.warn("Unsupported: {}", intAttrName.getField());
                }
            } else if (intAttrName != null && intAttrName.getSchemaType() != null) {
                switch (intAttrName.getSchemaType()) {
                    case PLAIN:
                        AttrTO attr = userTO.getPlainAttr(intAttrName.getSchema().getKey());
                        if (attr == null) {
                            attr = new AttrTO.Builder().schema(intAttrName.getSchema().getKey()).build();
                            userTO.getPlainAttrs().add(attr);
                        }
                        attr.getValues().clear();
                        attr.getValues().addAll(values);
                        break;

                    default:
                        LOG.warn("Unsupported: {} {}", intAttrName.getSchemaType(), intAttrName.getSchema().getKey());
                }
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String create(final OIDCProvider op, final OIDCLoginResponseTO responseTO, final String email) {
        UserTO userTO = new UserTO();

        if (op.getUserTemplate() != null) {
            templateUtils.apply(userTO, op.getUserTemplate());
        }

        List<OIDCProviderActions> actions = getActions(op);
        for (OIDCProviderActions action : actions) {
            userTO = action.beforeCreate(userTO, responseTO);
        }

        fill(op, responseTO, userTO);

        if (userTO.getRealm() == null) {
            userTO.setRealm(SyncopeConstants.ROOT_REALM);
        }
        if (userTO.getUsername() == null) {
            userTO.setUsername(email);
        }

        Pair<String, List<PropagationStatus>> created = provisioningManager.create(userTO, false, false);
        userTO = binder.getUserTO(created.getKey());

        for (OIDCProviderActions action : actions) {
            userTO = action.afterCreate(userTO, responseTO);
        }

        return userTO.getUsername();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String update(final String username, final OIDCProvider op, final OIDCLoginResponseTO responseTO) {
        UserTO userTO = binder.getUserTO(userDAO.findKey(username));
        UserTO original = SerializationUtils.clone(userTO);

        fill(op, responseTO, userTO);

        UserPatch userPatch = AnyOperations.diff(userTO, original, true);

        List<OIDCProviderActions> actions = getActions(op);
        for (OIDCProviderActions action : actions) {
            userPatch = action.beforeUpdate(userPatch, responseTO);
        }

        Pair<UserPatch, List<PropagationStatus>> updated = provisioningManager.update(userPatch, false);
        userTO = binder.getUserTO(updated.getLeft().getKey());

        for (OIDCProviderActions action : actions) {
            userTO = action.afterUpdate(userTO, responseTO);
        }

        return userTO.getUsername();
    }

}

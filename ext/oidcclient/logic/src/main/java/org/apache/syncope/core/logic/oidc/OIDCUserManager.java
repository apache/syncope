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
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.OIDCLoginResponseTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.core.persistence.api.attrvalue.validation.ParsingValidationException;
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
import org.apache.syncope.core.spring.ImplementationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OIDCUserManager {

    private static final Logger LOG = LoggerFactory.getLogger(OIDCUserManager.class);

    @Autowired
    private UserDAO userDAO;

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

                    result.addAll(userDAO.findByPlainAttrValue(
                            (PlainSchema) intAttrName.getSchema(), value, false).stream().
                            map(User::getUsername).collect(Collectors.toList()));
                    break;

                case DERIVED:
                    result.addAll(userDAO.findByDerAttrValue(
                            (DerSchema) intAttrName.getSchema(), transformed, false).stream().
                            map(User::getUsername).collect(Collectors.toList()));
                    break;

                default:
            }
        }

        return result;
    }

    private static List<OIDCProviderActions> getActions(final OIDCProvider op) {
        List<OIDCProviderActions> actions = new ArrayList<>();
        op.getActions().forEach(impl -> {
            try {
                actions.add(ImplementationManager.build(impl));
            } catch (Exception e) {
                LOG.warn("While building {}", impl, e);
            }
        });

        return actions;
    }

    public void fill(final OIDCProvider op, final OIDCLoginResponseTO responseTO, final UserTO userTO) {
        op.getItems().forEach(item -> {
            List<String> values = new ArrayList<>();
            Optional<Attr> oidcAttr = responseTO.getAttr(item.getExtAttrName());
            if (oidcAttr.isPresent() && !oidcAttr.get().getValues().isEmpty()) {
                values.addAll(oidcAttr.get().getValues());

                List<Object> transformed = new ArrayList<>(values);
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
                        Optional<Attr> attr = userTO.getPlainAttr(intAttrName.getSchema().getKey());
                        if (attr.isPresent()) {
                            attr.get().getValues().clear();
                        } else {
                            attr = Optional.of(new Attr.Builder(intAttrName.getSchema().getKey()).build());
                            userTO.getPlainAttrs().add(attr.get());
                        }
                        attr.get().getValues().addAll(values);
                        break;

                    default:
                        LOG.warn("Unsupported: {} {}", intAttrName.getSchemaType(), intAttrName.getSchema().getKey());
                }
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String create(final OIDCProvider op, final OIDCLoginResponseTO responseTO, final String email) {
        UserCR userCR = new UserCR();
        userCR.setStorePassword(false);

        if (op.getUserTemplate() != null && op.getUserTemplate().get() != null) {
            templateUtils.apply(userCR, op.getUserTemplate().get());
        }

        List<OIDCProviderActions> actions = getActions(op);
        for (OIDCProviderActions action : actions) {
            userCR = action.beforeCreate(userCR, responseTO);
        }

        UserTO userTO = new UserTO();
        fill(op, responseTO, userTO);
        EntityTOUtils.toAnyCR(userTO, userCR);

        if (userCR.getRealm() == null) {
            userCR.setRealm(SyncopeConstants.ROOT_REALM);
        }
        if (userCR.getUsername() == null) {
            userCR.setUsername(email);
        }

        Pair<String, List<PropagationStatus>> created = provisioningManager.create(userCR, false);
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

        UserUR userUR = AnyOperations.diff(userTO, original, true);

        List<OIDCProviderActions> actions = getActions(op);
        for (OIDCProviderActions action : actions) {
            userUR = action.beforeUpdate(userUR, responseTO);
        }

        Pair<UserUR, List<PropagationStatus>> updated = provisioningManager.update(userUR, false);
        userTO = binder.getUserTO(updated.getLeft().getKey());

        for (OIDCProviderActions action : actions) {
            userTO = action.afterUpdate(userTO, responseTO);
        }

        return userTO.getUsername();
    }
}

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
import org.apache.syncope.common.lib.oidc.OIDCLoginResponse;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.IntAttrName;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.ItemTransformer;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.java.pushpull.InboundMatcher;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.apache.syncope.core.provisioning.java.utils.TemplateUtils;
import org.apache.syncope.core.spring.ImplementationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.apache.syncope.core.provisioning.api.OIDCC4UIProviderActions;
import org.apache.syncope.core.persistence.api.entity.OIDCC4UIProvider;
import org.apache.syncope.core.persistence.api.entity.OIDCC4UIProviderItem;

public class OIDCUserManager {

    protected static final Logger LOG = LoggerFactory.getLogger(OIDCUserManager.class);

    protected static final String OIDC_CLIENT_CONTEXT = "ODIC Client";

    protected final InboundMatcher inboundMatcher;

    protected final UserDAO userDAO;

    protected final IntAttrNameParser intAttrNameParser;

    protected final TemplateUtils templateUtils;

    protected final UserProvisioningManager provisioningManager;

    protected final UserDataBinder binder;

    public OIDCUserManager(
            final InboundMatcher inboundMatcher,
            final UserDAO userDAO,
            final IntAttrNameParser intAttrNameParser,
            final TemplateUtils templateUtils,
            final UserProvisioningManager provisioningManager,
            final UserDataBinder binder) {

        this.inboundMatcher = inboundMatcher;
        this.userDAO = userDAO;
        this.intAttrNameParser = intAttrNameParser;
        this.templateUtils = templateUtils;
        this.provisioningManager = provisioningManager;
        this.binder = binder;
    }

    @Transactional(readOnly = true)
    public List<String> findMatchingUser(
            final String connObjectKeyValue,
            final OIDCC4UIProviderItem connObjectKeyItem) {

        return inboundMatcher.matchByConnObjectKeyValue(
                connObjectKeyItem, connObjectKeyValue, AnyTypeKind.USER, false, null).stream().
                filter(match -> match.getAny() != null).
                map(match -> ((User) match.getAny()).getUsername()).
                collect(Collectors.toList());
    }

    protected List<OIDCC4UIProviderActions> getActions(final OIDCC4UIProvider op) {
        List<OIDCC4UIProviderActions> actions = new ArrayList<>();
        op.getActions().forEach(impl -> {
            try {
                actions.add(ImplementationManager.build(impl));
            } catch (Exception e) {
                LOG.warn("While building {}", impl, e);
            }
        });

        return actions;
    }

    public void fill(final OIDCC4UIProvider op, final OIDCLoginResponse loginResponse, final UserTO userTO) {
        op.getItems().forEach(item -> {
            List<String> values = new ArrayList<>();
            Optional<Attr> oidcAttr = loginResponse.getAttr(item.getExtAttrName());
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
    public String create(final OIDCC4UIProvider op, final OIDCLoginResponse responseTO, final String defaultUsername) {
        UserCR userCR = new UserCR();
        userCR.setStorePassword(false);

        if (op.getUserTemplate() != null && op.getUserTemplate().get() != null) {
            templateUtils.apply(userCR, op.getUserTemplate().get());
        }

        List<OIDCC4UIProviderActions> actions = getActions(op);
        for (OIDCC4UIProviderActions action : actions) {
            userCR = action.beforeCreate(userCR, responseTO);
        }

        UserTO userTO = new UserTO();
        fill(op, responseTO, userTO);
        EntityTOUtils.toAnyCR(userTO, userCR);

        if (userCR.getRealm() == null) {
            userCR.setRealm(SyncopeConstants.ROOT_REALM);
        }
        if (userCR.getUsername() == null) {
            userCR.setUsername(defaultUsername);
        }

        Pair<String, List<PropagationStatus>> created =
                provisioningManager.create(userCR, false, userCR.getUsername(), OIDC_CLIENT_CONTEXT);
        userTO = binder.getUserTO(created.getKey());

        for (OIDCC4UIProviderActions action : actions) {
            userTO = action.afterCreate(userTO, responseTO);
        }

        return userTO.getUsername();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String update(final String username, final OIDCC4UIProvider op, final OIDCLoginResponse responseTO) {
        UserTO userTO = binder.getUserTO(userDAO.findKey(username));
        UserTO original = SerializationUtils.clone(userTO);

        fill(op, responseTO, userTO);

        UserUR userUR = AnyOperations.diff(userTO, original, true);

        List<OIDCC4UIProviderActions> actions = getActions(op);
        for (OIDCC4UIProviderActions action : actions) {
            userUR = action.beforeUpdate(userUR, responseTO);
        }

        Pair<UserUR, List<PropagationStatus>> updated =
                provisioningManager.update(userUR, false, userTO.getUsername(), OIDC_CLIENT_CONTEXT);
        userTO = binder.getUserTO(updated.getLeft().getKey());

        for (OIDCC4UIProviderActions action : actions) {
            userTO = action.afterUpdate(userTO, responseTO);
        }

        return userTO.getUsername();
    }
}

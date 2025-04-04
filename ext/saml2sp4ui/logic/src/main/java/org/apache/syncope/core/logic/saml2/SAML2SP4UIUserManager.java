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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.saml2.SAML2LoginResponse;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.SAML2SP4UIIdPDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.SAML2SP4UIIdP;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.IntAttrName;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.SAML2SP4UIIdPActions;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.ItemTransformer;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.java.pushpull.InboundMatcher;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.apache.syncope.core.provisioning.java.utils.TemplateUtils;
import org.apache.syncope.core.spring.implementation.ImplementationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class SAML2SP4UIUserManager {

    private static final Logger LOG = LoggerFactory.getLogger(SAML2SP4UIUserManager.class);

    private static final String SAML2SP_CONTEXT = "SAML 2.0 SP";

    protected final SAML2SP4UIIdPDAO idpDAO;

    protected final InboundMatcher inboundMatcher;

    protected final UserDAO userDAO;

    protected final ImplementationDAO implementationDAO;

    protected final IntAttrNameParser intAttrNameParser;

    protected final TemplateUtils templateUtils;

    protected final UserProvisioningManager provisioningManager;

    protected final UserDataBinder binder;

    protected final Map<String, SAML2SP4UIIdPActions> perContextActions = new ConcurrentHashMap<>();

    public SAML2SP4UIUserManager(
            final SAML2SP4UIIdPDAO idpDAO,
            final InboundMatcher inboundMatcher,
            final UserDAO userDAO,
            final ImplementationDAO implementationDAO,
            final IntAttrNameParser intAttrNameParser,
            final TemplateUtils templateUtils,
            final UserProvisioningManager provisioningManager,
            final UserDataBinder binder) {

        this.idpDAO = idpDAO;
        this.inboundMatcher = inboundMatcher;
        this.userDAO = userDAO;
        this.implementationDAO = implementationDAO;
        this.intAttrNameParser = intAttrNameParser;
        this.templateUtils = templateUtils;
        this.provisioningManager = provisioningManager;
        this.binder = binder;
    }

    @Transactional(readOnly = true)
    public List<String> findMatchingUser(final String connObjectKeyValue, final String idpKey) {
        SAML2SP4UIIdP idp = idpDAO.findById(idpKey).orElse(null);
        if (idp == null) {
            LOG.warn("Invalid IdP: {}", idpKey);
            return List.of();
        }
        if (idp.getConnObjectKeyItem().isEmpty()) {
            LOG.warn("Unable to determine conn object key item for  IdP: {}", idpKey);
            return Collections.emptyList();
        }
        return inboundMatcher.matchByConnObjectKeyValue(
                idp.getConnObjectKeyItem().get(), connObjectKeyValue, AnyTypeKind.USER, null, false).stream().
                filter(match -> match.getAny() != null).
                map(match -> ((User) match.getAny()).getUsername()).
                toList();
    }

    protected List<SAML2SP4UIIdPActions> getActions(final SAML2SP4UIIdP idp) {
        List<SAML2SP4UIIdPActions> result = new ArrayList<>();

        idp.getActions().forEach(impl -> {
            try {
                result.add(ImplementationManager.build(
                        impl,
                        () -> perContextActions.get(impl.getKey()),
                        instance -> perContextActions.put(impl.getKey(), instance)));
            } catch (Exception e) {
                LOG.warn("While building {}", impl, e);
            }
        });

        return result;
    }

    protected List<Implementation> getTransformers(final Item item) {
        return item.getTransformers().stream().
                map(implementationDAO::findById).
                flatMap(Optional::stream).
                collect(Collectors.toList());
    }

    public void fill(final String idpKey, final SAML2LoginResponse loginResponse, final UserTO userTO) {
        SAML2SP4UIIdP idp = idpDAO.findById(idpKey).orElse(null);
        if (idp == null) {
            LOG.warn("Invalid IdP: {}", idpKey);
            return;
        }

        idp.getItems().forEach(item -> {
            List<String> values = List.of();
            Optional<Attr> samlAttr = loginResponse.getAttr(item.getExtAttrName());
            if (samlAttr.isPresent() && !samlAttr.get().getValues().isEmpty()) {
                values = samlAttr.get().getValues();

                List<Object> transformed = new ArrayList<>(values);
                for (ItemTransformer transformer : MappingUtils.getItemTransformers(item, getTransformers(item))) {
                    transformed = transformer.beforePull(null, userTO, transformed);
                }
                values.clear();
                for (Object value : transformed) {
                    values.add(value.toString());
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
                    case "username" -> {
                        if (!values.isEmpty()) {
                            userTO.setUsername(values.getFirst());
                        }
                    }

                    default ->
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
    public String create(final SAML2SP4UIIdP idp, final SAML2LoginResponse loginResponse, final String nameID) {
        UserCR userCR = new UserCR();
        userCR.setStorePassword(false);

        if (idp.getUserTemplate() != null) {
            templateUtils.apply(userCR, idp.getUserTemplate().get());
        }

        UserTO userTO = new UserTO();
        fill(idp.getKey(), loginResponse, userTO);

        Optional.ofNullable(userTO.getUsername()).ifPresent(userCR::setUsername);
        userCR.getPlainAttrs().addAll(userTO.getPlainAttrs());

        if (userCR.getRealm() == null) {
            userCR.setRealm(SyncopeConstants.ROOT_REALM);
        }
        if (userCR.getUsername() == null) {
            userCR.setUsername(nameID);
        }

        List<SAML2SP4UIIdPActions> actions = getActions(idp);
        for (SAML2SP4UIIdPActions action : actions) {
            userCR = action.beforeCreate(userCR, loginResponse);
        }

        Pair<String, List<PropagationStatus>> created =
                provisioningManager.create(userCR, false, userCR.getUsername(), SAML2SP_CONTEXT);
        userTO = binder.getUserTO(created.getKey());

        for (SAML2SP4UIIdPActions action : actions) {
            userTO = action.afterCreate(userTO, loginResponse);
        }

        return userTO.getUsername();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String update(final String username, final SAML2SP4UIIdP idp, final SAML2LoginResponse loginResponse) {
        UserTO userTO = binder.getUserTO(userDAO.findKey(username).
                orElseThrow(() -> new NotFoundException("User " + username)));
        UserTO original = SerializationUtils.clone(userTO);

        fill(idp.getKey(), loginResponse, userTO);

        UserUR userUR = AnyOperations.diff(userTO, original, true);

        List<SAML2SP4UIIdPActions> actions = getActions(idp);
        for (SAML2SP4UIIdPActions action : actions) {
            userUR = action.beforeUpdate(userUR, loginResponse);
        }

        if (userUR.isEmpty()) {
            LOG.debug("No actual changes to apply for {}, ignoring", userTO.getUsername());
        } else {
            Pair<UserUR, List<PropagationStatus>> updated =
                    provisioningManager.update(userUR, false, userTO.getUsername(), SAML2SP_CONTEXT);
            userTO = binder.getUserTO(updated.getLeft().getKey());

            for (SAML2SP4UIIdPActions action : actions) {
                userTO = action.afterUpdate(userTO, loginResponse);
            }
        }

        return userTO.getUsername();
    }
}

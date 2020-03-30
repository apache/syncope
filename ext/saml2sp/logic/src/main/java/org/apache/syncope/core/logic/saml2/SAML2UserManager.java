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
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.SAML2LoginResponseTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.SAML2IdPDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.SAML2IdP;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.IntAttrName;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.SAML2IdPActions;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.ItemTransformer;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.java.pushpull.InboundMatcher;
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
public class SAML2UserManager {

    private static final Logger LOG = LoggerFactory.getLogger(SAML2UserManager.class);

    private static final String SAML2SP_CONTEXT = "SAML 2.0 SP";

    @Autowired
    private SAML2IdPDAO idpDAO;

    @Autowired
    private InboundMatcher inboundMatcher;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private ImplementationDAO implementationDAO;

    @Autowired
    private IntAttrNameParser intAttrNameParser;

    @Autowired
    private TemplateUtils templateUtils;

    @Autowired
    private UserProvisioningManager provisioningManager;

    @Autowired
    private UserDataBinder binder;

    @Transactional(readOnly = true)
    public List<String> findMatchingUser(final String connObjectKeyValue, final String idpKey) {
        SAML2IdP idp = idpDAO.find(idpKey);
        if (idp == null) {
            LOG.warn("Invalid IdP: {}", idpKey);
            return List.of();
        }
        if (idp.getConnObjectKeyItem().isEmpty()) {
            LOG.warn("Unable to determine conn object key item for  IdP: {}", idpKey);
            return Collections.emptyList();
        }
        return inboundMatcher.matchByConnObjectKeyValue(
                idp.getConnObjectKeyItem().get(), connObjectKeyValue, AnyTypeKind.USER, false, null).stream().
                filter(match -> match.getAny() != null).
                map(match -> ((User) match.getAny()).getUsername()).
                collect(Collectors.toList());
    }

    private List<SAML2IdPActions> getActions(final SAML2IdPEntity idp) {
        List<SAML2IdPActions> actions = new ArrayList<>();
        idp.getActions().forEach(impl -> {
            try {
                actions.add(ImplementationManager.build(implementationDAO.find(impl)));
            } catch (Exception e) {
                LOG.warn("While building {}", impl, e);
            }
        });

        return actions;
    }

    public void fill(final String idpKey, final SAML2LoginResponseTO responseTO, final UserTO userTO) {
        SAML2IdP idp = idpDAO.find(idpKey);
        if (idp == null) {
            LOG.warn("Invalid IdP: {}", idpKey);
            return;
        }

        idp.getItems().forEach(item -> {
            List<String> values = List.of();
            Optional<Attr> samlAttr = responseTO.getAttr(item.getExtAttrName());
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
    public String create(final SAML2IdPEntity idp, final SAML2LoginResponseTO responseTO, final String nameID) {
        UserCR userCR = new UserCR();
        userCR.setStorePassword(false);

        if (idp.getUserTemplate() != null) {
            templateUtils.apply(userCR, idp.getUserTemplate());
        }

        List<SAML2IdPActions> actions = getActions(idp);
        for (SAML2IdPActions action : actions) {
            userCR = action.beforeCreate(userCR, responseTO);
        }

        UserTO userTO = new UserTO();
        fill(idp.getKey(), responseTO, userTO);
        EntityTOUtils.toAnyCR(userTO, userCR);

        if (userCR.getRealm() == null) {
            userCR.setRealm(SyncopeConstants.ROOT_REALM);
        }
        if (userCR.getUsername() == null) {
            userCR.setUsername(nameID);
        }

        Pair<String, List<PropagationStatus>> created =
                provisioningManager.create(userCR, false, userCR.getUsername(), SAML2SP_CONTEXT);
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

        fill(idp.getKey(), responseTO, userTO);

        UserUR userUR = AnyOperations.diff(userTO, original, true);

        List<SAML2IdPActions> actions = getActions(idp);
        for (SAML2IdPActions action : actions) {
            userUR = action.beforeUpdate(userUR, responseTO);
        }

        Pair<UserUR, List<PropagationStatus>> updated =
                provisioningManager.update(userUR, false, userTO.getUsername(), SAML2SP_CONTEXT);
        userTO = binder.getUserTO(updated.getLeft().getKey());

        for (SAML2IdPActions action : actions) {
            userTO = action.afterUpdate(userTO, responseTO);
        }

        return userTO.getUsername();
    }
}

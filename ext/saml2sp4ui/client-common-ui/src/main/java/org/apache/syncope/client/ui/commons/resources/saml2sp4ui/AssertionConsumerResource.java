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
package org.apache.syncope.client.ui.commons.resources.saml2sp4ui;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.ui.commons.BaseSession;
import org.apache.syncope.client.ui.commons.SAML2SP4UIConstants;
import org.apache.syncope.common.lib.saml2.SAML2LoginResponse;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.service.SAML2SP4UIService;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public abstract class AssertionConsumerResource extends AbstractSAML2SP4UIResource {

    private static final long serialVersionUID = 3858609271031003370L;

    protected static final JsonMapper MAPPER =
            JsonMapper.builder().findAndAddModules().serializationInclusion(JsonInclude.Include.NON_EMPTY).build();

    protected abstract Class<? extends WebPage> getLoginPageClass();

    protected abstract Pair<Class<? extends WebPage>, PageParameters> getSelfRegInfo(UserTO newUser)
            throws JsonProcessingException;

    @Override
    protected ResourceResponse newResourceResponse(final Attributes attributes) {
        SAML2SP4UIService service = BaseSession.class.cast(Session.get()).getAnonymousService(SAML2SP4UIService.class);
        SAML2LoginResponse saml2Response = service.validateLoginResponse(extract(attributes));

        if (saml2Response.isSelfReg()) {
            UserTO newUser = new UserTO();
            newUser.setUsername(saml2Response.getUsername());
            newUser.getPlainAttrs().addAll(saml2Response.getAttrs());

            try {
                Pair<Class<? extends WebPage>, PageParameters> selfRegInfo = getSelfRegInfo(newUser);
                throw new RestartResponseException(selfRegInfo.getLeft(), selfRegInfo.getRight());
            } catch (JsonProcessingException e) {
                LOG.error("Could not serialize new user {}", newUser, e);
                throw new WicketRuntimeException(e);
            }
        } else {
            throw new RestartResponseException(
                    getLoginPageClass(),
                    new PageParameters().
                            set(SAML2SP4UIConstants.SAML2SP4UI_JWT, saml2Response.getAccessToken()).
                            set(SAML2SP4UIConstants.SAML2SP4UI_SLO_SUPPORTED, saml2Response.isSloSupported()));
        }
    }
}

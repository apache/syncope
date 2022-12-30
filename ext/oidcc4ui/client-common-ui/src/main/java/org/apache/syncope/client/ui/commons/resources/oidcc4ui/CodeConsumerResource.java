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
package org.apache.syncope.client.ui.commons.resources.oidcc4ui;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.ui.commons.BaseSession;
import org.apache.syncope.client.ui.commons.panels.OIDCC4UIConstants;
import org.apache.syncope.common.lib.oidc.OIDCConstants;
import org.apache.syncope.common.lib.oidc.OIDCLoginResponse;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.service.OIDCC4UIService;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.AbstractResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CodeConsumerResource extends AbstractResource {

    private static final long serialVersionUID = -692581789294259519L;

    protected static final Logger LOG = LoggerFactory.getLogger(CodeConsumerResource.class);

    protected static final JsonMapper MAPPER =
            JsonMapper.builder().findAndAddModules().serializationInclusion(JsonInclude.Include.NON_EMPTY).build();

    protected abstract Class<? extends WebPage> getLoginPageClass();

    protected abstract Pair<Class<? extends WebPage>, PageParameters> getSelfRegInfo(UserTO newUser)
            throws JsonProcessingException;

    @Override
    protected ResourceResponse newResourceResponse(final Attributes attributes) {
        String authorizationCode = attributes.getRequest().getQueryParameters().
                getParameterValue(OIDCConstants.CODE).toOptionalString();

        HttpServletRequest request = (HttpServletRequest) attributes.getRequest().getContainerRequest();

        OIDCC4UIService service = BaseSession.class.cast(Session.get()).getAnonymousService(OIDCC4UIService.class);
        OIDCLoginResponse oidcResponse = service.login(
                request.getRequestURL().toString(),
                authorizationCode,
                Session.get().getAttribute(OIDCConstants.OP).toString());

        if (oidcResponse.isSelfReg()) {
            UserTO newUser = new UserTO();
            newUser.setUsername(oidcResponse.getUsername());
            newUser.getPlainAttrs().addAll(oidcResponse.getAttrs());

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
                            set(OIDCC4UIConstants.OIDCC4UI_JWT, oidcResponse.getAccessToken()).
                            set(OIDCC4UIConstants.OIDCC4UI_SLO_SUPPORTED, oidcResponse.isLogoutSupported()));
        }
    }
}

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
package org.apache.syncope.core.rest.cxf.service;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.saml2.SAML2LoginResponse;
import org.apache.syncope.common.lib.saml2.SAML2Request;
import org.apache.syncope.common.lib.saml2.SAML2Response;
import org.apache.syncope.common.rest.api.service.SAML2SP4UIService;
import org.apache.syncope.core.logic.SAML2SP4UILogic;

public class SAML2SP4UIServiceImpl extends AbstractService implements SAML2SP4UIService {

    protected final SAML2SP4UILogic logic;

    public SAML2SP4UIServiceImpl(final SAML2SP4UILogic logic) {
        this.logic = logic;
    }

    @Override
    public Response getMetadata(final String spEntityID, final String urlContext) {
        StreamingOutput sout = (os) -> logic.getMetadata(StringUtils.appendIfMissing(spEntityID, "/"), urlContext, os);

        return Response.ok(sout).
                type(MediaType.APPLICATION_XML).
                build();
    }

    @Override
    public SAML2Request createLoginRequest(
            final String spEntityID, final String urlContext, final String idpEntityID) {

        return logic.createLoginRequest(
                StringUtils.appendIfMissing(spEntityID, "/"),
                urlContext,
                idpEntityID);
    }

    @Override
    public SAML2LoginResponse validateLoginResponse(final SAML2Response reponse) {
        return logic.validateLoginResponse(reponse);
    }

    @Override
    public SAML2Request createLogoutRequest(final String spEntityID, final String urlContext) {
        return logic.createLogoutRequest(
                getAccessToken(),
                StringUtils.appendIfMissing(spEntityID, "/"),
                urlContext);
    }

    @Override
    public void validateLogoutResponse(final SAML2Response response) {
        logic.validateLogoutResponse(response);
    }

    private String getAccessToken() {
        String auth = messageContext.getHttpHeaders().getHeaderString(HttpHeaders.AUTHORIZATION);
        String[] parts = Optional.ofNullable(auth).map(s -> s.split(" ")).orElse(null);
        if (parts == null || parts.length != 2 || !"Bearer".equals(parts[0])) {
            return null;
        }

        return parts[1];
    }
}

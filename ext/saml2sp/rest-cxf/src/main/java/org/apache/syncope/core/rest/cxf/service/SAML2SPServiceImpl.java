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

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.to.SAML2RequestTO;
import org.apache.syncope.common.lib.to.SAML2LoginResponseTO;
import org.apache.syncope.common.lib.to.SAML2ReceivedResponseTO;
import org.apache.syncope.common.rest.api.service.SAML2SPService;
import org.apache.syncope.core.logic.SAML2SPLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SAML2SPServiceImpl extends AbstractServiceImpl implements SAML2SPService {

    @Autowired
    private SAML2SPLogic logic;

    @Override
    public Response getMetadata(final String spEntityID, final String urlContext) {
        StreamingOutput sout = (os) -> logic.getMetadata(StringUtils.appendIfMissing(spEntityID, "/"), urlContext, os);

        return Response.ok(sout).
                type(MediaType.APPLICATION_XML).
                build();
    }

    @Override
    public SAML2RequestTO createLoginRequest(
            final String spEntityID, final String idpEntityID) {

        return logic.createLoginRequest(StringUtils.appendIfMissing(spEntityID, "/"), idpEntityID);
    }

    @Override
    public SAML2LoginResponseTO validateLoginResponse(final SAML2ReceivedResponseTO reponse) {
        return logic.validateLoginResponse(reponse);
    }

    @Override
    public SAML2RequestTO createLogoutRequest(final String spEntityID) {
        return logic.createLogoutRequest(
                getJWTToken(),
                StringUtils.appendIfMissing(spEntityID, "/"));
    }

    @Override
    public void validateLogoutResponse(final SAML2ReceivedResponseTO response) {
        logic.validateLogoutResponse(getJWTToken(), response);
    }

    private String getJWTToken() {
        String auth = messageContext.getHttpHeaders().getHeaderString(HttpHeaders.AUTHORIZATION);
        String[] parts = auth == null ? null : auth.split(" ");
        if (parts == null || parts.length != 2 || !"Bearer".equals(parts[0])) {
            return null;
        }

        return parts[1];
    }
}

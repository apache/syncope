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
import java.util.Optional;
import org.apache.syncope.common.lib.oidc.OIDCLoginResponse;
import org.apache.syncope.common.lib.oidc.OIDCRequest;
import org.apache.syncope.common.rest.api.service.OIDCC4UIService;
import org.apache.syncope.core.logic.OIDCC4UILogic;

public class OIDCC4UIServiceImpl extends AbstractService implements OIDCC4UIService {

    protected final OIDCC4UILogic logic;

    public OIDCC4UIServiceImpl(final OIDCC4UILogic logic) {
        this.logic = logic;
    }

    @Override
    public OIDCRequest createLoginRequest(final String redirectURI, final String op) {
        return logic.createLoginRequest(redirectURI, op);
    }

    @Override
    public OIDCLoginResponse login(final String redirectURI, final String authorizationCode, final String op) {
        return logic.login(redirectURI, authorizationCode, op);
    }

    @Override
    public OIDCRequest createLogoutRequest(final String redirectURI) {
        return logic.createLogoutRequest(getAccessToken(), redirectURI);
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

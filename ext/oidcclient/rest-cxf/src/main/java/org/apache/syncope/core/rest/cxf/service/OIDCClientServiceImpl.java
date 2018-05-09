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

import org.apache.syncope.common.lib.to.OIDCLoginRequestTO;
import org.apache.syncope.common.rest.api.service.OIDCClientService;
import org.apache.syncope.core.logic.OIDCClientLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.syncope.common.lib.to.OIDCLoginResponseTO;
import org.apache.syncope.common.lib.to.OIDCLogoutRequestTO;

@Service
public class OIDCClientServiceImpl extends AbstractServiceImpl implements OIDCClientService {

    @Autowired
    private OIDCClientLogic logic;

    @Override
    public OIDCLoginRequestTO createLoginRequest(final String redirectURI, final String op) {
        return logic.createLoginRequest(redirectURI, op);
    }

    @Override
    public OIDCLoginResponseTO login(final String redirectURI, final String authorizationCode, final String op) {
        return logic.login(redirectURI, authorizationCode, op);
    }

    @Override
    public OIDCLogoutRequestTO createLogoutRequest(final String op) {
        return logic.createLogoutRequest(op);
    }

}

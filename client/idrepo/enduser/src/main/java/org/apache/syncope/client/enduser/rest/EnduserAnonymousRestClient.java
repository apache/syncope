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
package org.apache.syncope.client.enduser.rest;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.ui.commons.rest.AnonymousRestClient;
import org.apache.syncope.common.lib.types.MfaCheck;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.ComplianceQuery;
import org.apache.syncope.common.rest.api.service.MfaService;
import org.apache.syncope.common.rest.api.service.UserSelfService;

public class EnduserAnonymousRestClient extends BaseRestClient implements AnonymousRestClient {

    private static final long serialVersionUID = 5595348012612367577L;

    @Override
    public boolean isMfaEnrolled(final String username) {
        try {
            return BooleanUtils.toBoolean(SyncopeEnduserSession.get().getAnonymousService(MfaService.class).
                    enrolled(username).getHeaderString(RESTHeaders.VERIFIED));
        } catch (Exception e) {
            LOG.error("While checking if MFA is enrolled for {}", username, e);
            return false;
        }
    }

    @Override
    public boolean mfaCheck(final String secret, final String otp) {
        try {
            return BooleanUtils.toBoolean(SyncopeEnduserSession.get().getAnonymousService(MfaService.class).
                    check(new MfaCheck(secret, otp)).getHeaderString(RESTHeaders.VERIFIED));
        } catch (Exception e) {
            LOG.error("While checking MFA OTP code {}", otp, e);
            return false;
        }
    }

    @Override
    public void compliance(final ComplianceQuery query) {
        SyncopeEnduserSession.get().getAnonymousService(UserSelfService.class).compliance(query);
    }
}

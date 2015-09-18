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
package org.apache.syncope.client.console.rest;

import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.springframework.stereotype.Component;

@Component
public class UserSelfRestClient extends BaseRestClient {

    private static final long serialVersionUID = 2994691796924731295L;

    public boolean isSelfRegistrationAllowed() {
        Boolean result = null;
        try {
            result = SyncopeConsoleSession.get().getSyncopeTO().isSelfRegAllowed();
        } catch (SyncopeClientException e) {
            LOG.error("While seeking if self registration is allowed", e);
        }

        return result == null
                ? false
                : result;
    }

    public UserTO read() {
        return SyncopeConsoleSession.get().getSelfTO();
    }

    public void create(final UserTO userTO, final boolean storePassword) {
        getService(UserSelfService.class).create(userTO, storePassword);
    }

    public void update(final UserPatch userPatch) {
        getService(UserSelfService.class).update(userPatch);
    }

    public void delete() {
        getService(UserSelfService.class).delete();
    }

    public boolean isPasswordResetAllowed() {
        Boolean result = null;
        try {
            result = SyncopeConsoleSession.get().getSyncopeTO().isPwdResetAllowed();
        } catch (SyncopeClientException e) {
            LOG.error("While seeking if password reset is allowed", e);
        }

        return result == null
                ? false
                : result;
    }

    public boolean isPwdResetRequiringSecurityQuestions() {
        Boolean result = null;
        try {
            result = SyncopeConsoleSession.get().getSyncopeTO().isPwdResetRequiringSecurityQuestions();
        } catch (SyncopeClientException e) {
            LOG.error("While seeking if password reset requires security question", e);
        }

        return result == null
                ? false
                : result;
    }

    public void changePassword(final String password) {
        getService(UserSelfService.class).changePassword(password);
    }

    public void requestPasswordReset(final String username, final String securityAnswer) {
        getService(UserSelfService.class).requestPasswordReset(username, securityAnswer);
    }

    public void confirmPasswordReset(final String token, final String password) {
        getService(UserSelfService.class).confirmPasswordReset(token, password);
    }

}

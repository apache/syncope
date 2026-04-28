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

import jakarta.ws.rs.core.GenericType;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.service.UserSelfService;

public class UserSelfRestClient extends BaseRestClient {

    private static final long serialVersionUID = -1575748964398293968L;

    public void mustChangePassword(final String password) {
        getService(UserSelfService.class).mustChangePassword(new PasswordPatch.Builder().value(password).build());
    }

    public void requestPasswordReset(final String username, final String securityAnswer) {
        getService(UserSelfService.class).requestPasswordReset(username, securityAnswer);
    }

    public ProvisioningResult<UserTO> create(final UserCR createReq) {
        return getService(UserSelfService.class).create(createReq).readEntity(new GenericType<>() {
        });
    }

    public ProvisioningResult<UserTO> update(final String etag, final UserUR updateReq) {
        ProvisioningResult<UserTO> result;
        synchronized (this) {
            result = getService(etag, UserSelfService.class).update(updateReq).readEntity(new GenericType<>() {
            });
            resetClient(UserSelfService.class);
        }
        return result;
    }
}

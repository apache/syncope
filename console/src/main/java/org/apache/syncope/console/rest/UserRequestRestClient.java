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
package org.apache.syncope.console.rest;

import java.util.List;

import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.services.UserRequestService;
import org.apache.syncope.common.to.UserRequestTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.UserRequestType;
import org.springframework.stereotype.Component;

@Component
public class UserRequestRestClient extends BaseRestClient {

    private static final long serialVersionUID = 171408947099311191L;

    public List<UserRequestTO> list() {
        return getService(UserRequestService.class).list();
    }

    public void delete(final Long requestId) {
        getService(UserRequestService.class).delete(requestId);
    }

    public void claim(final Long requestId) {
        getService(UserRequestService.class).claim(requestId);
    }

    public void requestCreate(final UserTO userTO) {
        UserRequestTO userRequestTO = new UserRequestTO();
        userRequestTO.setType(UserRequestType.CREATE);
        userRequestTO.setUserTO(userTO);
        getService(UserRequestService.class).create(userRequestTO);
    }

    public void requestUpdate(final UserMod userMod) {
        UserRequestTO userRequestTO = new UserRequestTO();
        userRequestTO.setType(UserRequestType.UPDATE);
        userRequestTO.setUserMod(userMod);
        getService(UserRequestService.class).create(userRequestTO);
    }

    public void requestDelete(final Long userId) {
        UserRequestTO userRequestTO = new UserRequestTO();
        userRequestTO.setType(UserRequestType.DELETE);
        userRequestTO.setUserId(userId);
        getService(UserRequestService.class).create(userRequestTO);
    }

    public UserTO executeCreate(final Long requestId, final UserTO reviewed) {
        return getService(UserRequestService.class).executeCreate(requestId, reviewed);
    }

    public UserTO executeUpdate(final Long requestId, final UserMod changes) {
        return getService(UserRequestService.class).executeUpdate(requestId, changes);
    }

    public UserTO executeDelete(final Long requestId) {
        return getService(UserRequestService.class).executeDelete(requestId);
    }
}

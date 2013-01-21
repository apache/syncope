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
import org.springframework.stereotype.Component;

@Component
public class UserRequestRestClient extends BaseRestClient {

    private static final long serialVersionUID = 171408947099311191L;

    public List<UserRequestTO> list() {
        return getService(UserRequestService.class).list();
    }

    public UserRequestTO delete(final Long requestId) {
        return getService(UserRequestService.class).delete(requestId);
    }

    public UserRequestTO requestCreate(final UserTO userTO) {
        return getService(UserRequestService.class).create(userTO);
    }

    public UserRequestTO requestUpdate(final UserMod userMod) {
        return getService(UserRequestService.class).update(userMod);
    }

    public UserRequestTO requestDelete(final Long userId) {
        return getService(UserRequestService.class).delete(userId);
    }
}

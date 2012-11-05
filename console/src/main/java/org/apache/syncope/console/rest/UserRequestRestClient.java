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

import java.util.Arrays;
import java.util.List;

import org.apache.syncope.console.SyncopeSession;
import org.apache.syncope.mod.UserMod;
import org.apache.syncope.to.UserRequestTO;
import org.apache.syncope.to.UserTO;
import org.springframework.stereotype.Component;

@Component
public class UserRequestRestClient extends AbstractBaseRestClient {

    public UserTO readProfile() {
        return SyncopeSession.get().getRestTemplate().getForObject(baseURL + "user/request/read/self", UserTO.class);
    }

    public List<UserRequestTO> list() {
        return Arrays.asList(SyncopeSession.get().getRestTemplate().getForObject(
                baseURL + "user/request/list", UserRequestTO[].class));
    }

    public UserRequestTO delete(final Long requestId) {
        return SyncopeSession.get().getRestTemplate().getForObject(
                baseURL + "user/request/deleteRequest/{requestId}", UserRequestTO.class, requestId);
    }

    public UserRequestTO requestCreate(final UserTO userTO) {
        return SyncopeSession.get().getRestTemplate().postForObject(
                baseURL + "user/request/create", userTO, UserRequestTO.class);
    }

    public UserRequestTO requestUpdate(final UserMod userMod) {
        return SyncopeSession.get().getRestTemplate().postForObject(
                baseURL + "user/request/update", userMod, UserRequestTO.class);
    }

    public UserRequestTO requestDelete(final Long userId) {
        return SyncopeSession.get().getRestTemplate().postForObject(
                baseURL + "user/request/delete/", userId, UserRequestTO.class);
    }
}

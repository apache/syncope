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
package org.apache.syncope.services.proxy;

import java.util.Arrays;
import java.util.List;
import org.apache.syncope.client.mod.UserMod;
import org.apache.syncope.client.to.UserRequestTO;
import org.apache.syncope.client.to.UserTO;
import org.apache.syncope.services.UserRequestService;
import org.springframework.web.client.RestTemplate;

public class UserRequestServiceProxy extends SpringServiceProxy implements UserRequestService {

    public UserRequestServiceProxy(final String baseUrl, final RestTemplate restTemplate) {
        super(baseUrl, restTemplate);
    }

    @Override
    public boolean isCreateAllowed() {
        return getRestTemplate().getForObject(baseUrl + "user/request/create/allowed", Boolean.class);
    }

    @Override
    public UserRequestTO create(final UserTO userTO) {
        return getRestTemplate().postForObject(baseUrl + "user/request/create", userTO, UserRequestTO.class);
    }

    @Override
    public UserRequestTO update(final UserMod userMod) {
        return getRestTemplate().postForObject(baseUrl + "user/request/update", userMod, UserRequestTO.class);
    }

    @Override
    public UserRequestTO delete(final Long userId) {
        return getRestTemplate().getForObject(baseUrl + "user/request/delete/{userId}", UserRequestTO.class, userId);
    }

    @Override
    public List<UserRequestTO> list() {
        return Arrays.asList(getRestTemplate().getForObject(baseUrl + "user/request/list", UserRequestTO[].class));
    }

    @Override
    public UserRequestTO read(final Long requestId) {
        return getRestTemplate().getForObject(
                baseUrl + "user/request/read/{requestId}", UserRequestTO.class, requestId);
    }

    @Override
    public UserRequestTO deleteRequest(final Long requestId) {
        return getRestTemplate().getForObject(
                baseUrl + "user/request/deleteRequest/{requestId}", UserRequestTO.class, requestId);
    }
}

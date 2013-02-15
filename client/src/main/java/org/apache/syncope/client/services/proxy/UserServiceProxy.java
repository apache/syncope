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
package org.apache.syncope.client.services.proxy;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.search.NodeCond;
import org.apache.syncope.common.services.UserService;
import org.apache.syncope.common.to.PropagationRequestTO;
import org.apache.syncope.common.to.UserTO;
import org.springframework.web.client.RestTemplate;

public class UserServiceProxy extends SpringServiceProxy implements UserService {

    public UserServiceProxy(final String baseUrl, final RestTemplate restTemplate) {
        super(baseUrl, restTemplate);
    }

    @Override
    public Boolean verifyPassword(final String username, final String password) {
        return getRestTemplate().getForObject(
                baseUrl + "user/verifyPassword/{username}.json?password={password}", Boolean.class,
                username, password);
    }

    @Override
    public int count() {
        return getRestTemplate().getForObject(baseUrl + "user/count.json", Integer.class);
    }

    @Override
    public List<UserTO> list() {
        return Arrays.asList(getRestTemplate().getForObject(baseUrl + "user/list.json", UserTO[].class));
    }

    @Override
    public List<UserTO> list(final int page, final int size) {
        return Arrays.asList(getRestTemplate().getForObject(baseUrl + "user/list/{page}/{size}.json",
                UserTO[].class, page, size));
    }

    @Override
    public UserTO read(final Long userId) {
        return getRestTemplate().getForObject(baseUrl + "user/read/{userId}.json", UserTO.class, userId);
    }

    @Override
    public UserTO read(final String username) {
        return getRestTemplate().getForObject(baseUrl + "user/readByUsername/{username}.json", UserTO.class,
                username);
    }

    @Override
    public Response create(final UserTO userTO) {
        UserTO created = getRestTemplate().postForObject(baseUrl + "user/create", userTO, UserTO.class);
        URI location = URI.create(baseUrl + "user/read/" + created.getId() + ".json");
        return Response.created(location)
                .header(SyncopeConstants.REST_HEADER_ID, created.getId())
                .entity(created)
                .build();
    }

    @Override
    public UserTO update(final Long userId, final UserMod userMod) {
        return getRestTemplate().postForObject(baseUrl + "user/update", userMod, UserTO.class);
    }

    @Override
    public UserTO delete(final Long userId) {
        return getRestTemplate().getForObject(baseUrl + "user/delete/{userId}", UserTO.class, userId);
    }

    @Override
    public UserTO activate(final long userId, final String token) {
        return getRestTemplate().getForObject(baseUrl + "user/activate/{userId}?token=" + token, UserTO.class, userId);
    }

    @Override
    public UserTO activate(final long userId, final String token,
            final PropagationRequestTO propagationRequestTO) {

        return getRestTemplate().postForObject(baseUrl + "user/activate/{userId}?token=" + token, propagationRequestTO,
                UserTO.class, userId);
    }

    @Override
    public UserTO activateByUsername(final String username, final String token) {
        return getRestTemplate().getForObject(baseUrl + "user/activateByUsername/{username}.json?token=" + token,
                UserTO.class, username);
    }

    @Override
    public UserTO activateByUsername(final String username, final String token,
            final PropagationRequestTO propagationRequestTO) {

        return getRestTemplate().postForObject(baseUrl + "user/activateByUsername/{username}.json?token=" + token,
                propagationRequestTO, UserTO.class, username);
    }

    @Override
    public UserTO suspend(final long userId) {
        return getRestTemplate().getForObject(baseUrl + "user/suspend/{userId}", UserTO.class, userId);
    }

    @Override
    public UserTO suspend(final long userId, final PropagationRequestTO propagationRequestTO) {
        return getRestTemplate().postForObject(baseUrl + "user/suspend/{userId}", propagationRequestTO,
                UserTO.class, userId);
    }

    @Override
    public UserTO suspendByUsername(final String username) {
        return getRestTemplate().getForObject(baseUrl + "user/suspendByUsername/{username}.json",
                UserTO.class, username);
    }

    @Override
    public UserTO suspendByUsername(final String username, final PropagationRequestTO propagationRequestTO) {
        return getRestTemplate().postForObject(baseUrl + "user/suspendByUsername/{username}.json", propagationRequestTO,
                UserTO.class, username);
    }

    @Override
    public UserTO reactivate(final long userId) {
        return getRestTemplate().getForObject(baseUrl + "user/reactivate/{userId}", UserTO.class, userId);
    }

    @Override
    public UserTO reactivate(final long userId, final PropagationRequestTO propagationRequestTO) {
        return getRestTemplate().postForObject(baseUrl + "user/reactivate/{userId}", propagationRequestTO,
                UserTO.class, userId);
    }

    @Override
    public UserTO reactivateByUsername(final String username) {
        return getRestTemplate().getForObject(baseUrl + "user/reactivateByUsername/{username}.json",
                UserTO.class, username);
    }

    @Override
    public UserTO reactivateByUsername(final String username, final PropagationRequestTO propagationRequestTO) {
        return getRestTemplate().postForObject(baseUrl + "user/reactivateByUsername/{username}.json",
                propagationRequestTO, UserTO.class, username);
    }

    @Override
    public UserTO readSelf() {
        return getRestTemplate().getForObject(baseUrl + "user/read/self", UserTO.class);
    }

    @Override
    public List<UserTO> search(final NodeCond searchCondition) {
        return Arrays.asList(getRestTemplate().postForObject(baseUrl + "user/search", searchCondition,
                UserTO[].class));
    }

    @Override
    public List<UserTO> search(final NodeCond searchCondition, final int page, final int size) {
        return Arrays.asList(getRestTemplate().postForObject(baseUrl + "user/search/{page}/{size}",
                searchCondition, UserTO[].class, page, size));
    }

    @Override
    public int searchCount(final NodeCond searchCondition) {
        return getRestTemplate().postForObject(baseUrl + "user/search/count.json", searchCondition, Integer.class);
    }
}

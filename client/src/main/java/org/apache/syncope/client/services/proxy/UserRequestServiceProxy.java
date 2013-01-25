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

import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.syncope.common.services.UserRequestService;
import org.apache.syncope.common.to.UserRequestTO;
import org.apache.syncope.common.types.UserRequestType;
import org.springframework.web.client.RestTemplate;

public class UserRequestServiceProxy extends SpringServiceProxy implements UserRequestService {

    public UserRequestServiceProxy(final String baseUrl, final RestTemplate restTemplate) {
        super(baseUrl, restTemplate);
    }
    
    @Override
    public Response getOptions() {
        return Response.ok().allow("GET", "POST", "DELETE").header(SYNCOPE_CREATE_ALLOWED, isCreateAllowed()).build();
    }

    public boolean isCreateAllowed() {
        return getRestTemplate().getForObject(baseUrl + "user/request/create/allowed", Boolean.class);
    }

    @Override
    public Response create(final UserRequestTO userRequestTO) {
        UserRequestTO created = null;
        if (userRequestTO.getType() == UserRequestType.CREATE) {
            created = getRestTemplate().postForObject(baseUrl + "user/request/create", userRequestTO.getUserTO(), UserRequestTO.class);
        } else if (userRequestTO.getType() == UserRequestType.UPDATE) {
            created = getRestTemplate().postForObject(baseUrl + "user/request/update", userRequestTO.getUserMod(), UserRequestTO.class);
        } else if (userRequestTO.getType() == UserRequestType.DELETE) {
            created = getRestTemplate().getForObject(baseUrl + "user/request/delete/{userId}", UserRequestTO.class, userRequestTO.getUserId());
        }
        URI location = URI.create(baseUrl + "user/request/read/" + created.getId());
        return Response.created(location).entity(created.getId()).build();
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
    public void delete(@PathParam("requestId") Long requestId) {
        getRestTemplate().getForObject(
                baseUrl + "user/request/deleteRequest/{requestId}", UserRequestTO.class, requestId);
    }
}

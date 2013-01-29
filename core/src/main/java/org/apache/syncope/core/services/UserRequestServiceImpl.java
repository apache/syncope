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
package org.apache.syncope.core.services;

import java.net.URI;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.syncope.common.services.UserRequestService;
import org.apache.syncope.common.to.UserRequestTO;
import org.apache.syncope.common.types.UserRequestType;
import org.apache.syncope.core.rest.controller.UserRequestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserRequestServiceImpl implements UserRequestService, ContextAware {

    @Autowired
    private UserRequestController userRequestController;

    private UriInfo uriInfo;

    @Override
    public Response getOptions() {
        return Response.ok().allow("GET", "POST", "DELETE")
                .header(SYNCOPE_CREATE_ALLOWED, userRequestController.isCreateAllowedByConf()).build();
    }

    @Override
    public boolean isCreateAllowed() {
        return userRequestController.isCreateAllowedByConf();
    }

    @Override
    public Response create(final UserRequestTO userRequestTO) {
        UserRequestTO outUserRequestTO = null;
        if (userRequestTO.getType() == UserRequestType.CREATE) {
            outUserRequestTO = userRequestController.create(userRequestTO.getUserTO());
        } else if (userRequestTO.getType() == UserRequestType.UPDATE) {
            outUserRequestTO = userRequestController.update(userRequestTO.getUserMod());
        } else if (userRequestTO.getType() == UserRequestType.DELETE) {
            userRequestController.delete(userRequestTO.getUserId());
        }
        URI location = uriInfo.getAbsolutePathBuilder().path("" + outUserRequestTO.getId()).build();
        return Response.created(location).entity(outUserRequestTO.getId()).build();
    }

    @Override
    public List<UserRequestTO> list() {
        return userRequestController.list();
    }

    @Override
    public UserRequestTO read(final Long requestId) {
        return userRequestController.read(requestId);
    }

    @Override
    public void delete(final Long requestId) {
        userRequestController.deleteRequest(requestId);
    }

    @Override
    public void setUriInfo(final UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

}

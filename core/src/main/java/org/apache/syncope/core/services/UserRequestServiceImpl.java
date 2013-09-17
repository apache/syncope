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

import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.services.UserRequestService;
import org.apache.syncope.common.to.UserRequestTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.core.rest.controller.UserRequestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserRequestServiceImpl extends AbstractServiceImpl implements UserRequestService, ContextAware {

    @Autowired
    private UserRequestController controller;

    @Override
    public Response getOptions() {
        return Response.ok().header("Allow", "GET,POST,OPTIONS,HEAD")
                .header(SYNCOPE_CREATE_ALLOWED, controller.isCreateAllowed()).
                build();
    }

    @Override
    public boolean isCreateAllowed() {
        return controller.isCreateAllowed();
    }

    @Override
    public Response create(final UserRequestTO userRequestTO) {
        UserRequestTO outUserRequestTO;
        switch (userRequestTO.getType()) {
            case CREATE:
                outUserRequestTO = controller.create(userRequestTO.getUserTO());
                break;

            case UPDATE:
                outUserRequestTO = controller.update(userRequestTO.getUserMod());
                break;

            case DELETE:
            default:
                outUserRequestTO = controller.delete(userRequestTO.getUserId());
                break;
        }

        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(outUserRequestTO.getId())).build();
        return Response.created(location)
                .header(SyncopeConstants.REST_HEADER_ID, outUserRequestTO.getId())
                .build();
    }

    @Override
    public List<UserRequestTO> list() {
        return controller.list(false);
    }

    @Override
    public UserRequestTO read(final Long requestId) {
        return controller.read(requestId);
    }

    @Override
    public void delete(final Long requestId) {
        controller.deleteRequest(requestId);
    }

    @Override
    public UserTO executeCreate(final Long requestId, final UserTO reviewed) {
        return controller.execute(controller.read(requestId), reviewed, null);
    }

    @Override
    public UserTO executeUpdate(final Long requestId, final UserMod changes) {
        return controller.execute(controller.read(requestId), null, changes);
    }

    @Override
    public UserTO executeDelete(final Long requestId) {
        return controller.execute(controller.read(requestId), null, null);
    }
}

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
import org.apache.syncope.common.mod.StatusMod;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.search.NodeCond;
import org.apache.syncope.common.services.InvalidSearchConditionException;
import org.apache.syncope.common.services.UserService;
import org.apache.syncope.common.to.BulkAction;
import org.apache.syncope.common.to.BulkActionRes;
import org.apache.syncope.common.to.ResourceNameTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.RESTHeaders;
import org.apache.syncope.common.types.ResourceAssociationActionType;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.core.rest.controller.UserController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends AbstractServiceImpl implements UserService, ContextAware {

    @Autowired
    private UserController controller;

    @Override
    public Response getUsername(final Long userId) {
        return Response.ok().header("Allow", "GET,POST,OPTIONS,HEAD").
                header(RESTHeaders.USERNAME.toString(), controller.getUsername(userId)).
                build();
    }

    @Override
    public Response getUserId(final String username) {
        return Response.ok().header("Allow", "GET,POST,OPTIONS,HEAD").
                header(RESTHeaders.USER_ID.toString(), controller.getUserId(username)).
                build();
    }

    @Override
    public int count() {
        return controller.count();
    }

    @Override
    public Response create(final UserTO userTO) {
        UserTO created = controller.create(userTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(created.getId())).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_ID.toString(), created.getId()).
                entity(created).
                build();
    }

    @Override
    public Response delete(final Long userId) {
        UserTO deleted = controller.delete(userId);
        return Response.ok(deleted).
                build();
    }

    @Override
    public List<UserTO> list() {
        return controller.list();
    }

    @Override
    public List<UserTO> list(final int page, final int size) {
        return controller.list(page, size);
    }

    @Override
    public UserTO read(final Long userId) {
        return controller.read(userId);
    }

    @Override
    public UserTO readSelf() {
        return controller.readSelf();
    }

    @Override
    public List<UserTO> search(final NodeCond searchCondition) throws InvalidSearchConditionException {
        return controller.search(searchCondition);
    }

    @Override
    public List<UserTO> search(final NodeCond searchCondition, final int page, final int size)
            throws InvalidSearchConditionException {

        return controller.search(searchCondition, page, size);
    }

    @Override
    public int searchCount(final NodeCond searchCondition) throws InvalidSearchConditionException {
        return controller.searchCount(searchCondition);
    }

    @Override
    public Response update(final Long userId, final UserMod userMod) {
        userMod.setId(userId);
        UserTO updated = controller.update(userMod);
        return Response.ok(updated).
                build();
    }

    @Override
    public Response status(final Long userId, final StatusMod statusMod) {
        statusMod.setId(userId);
        UserTO updated = controller.status(statusMod);
        return Response.ok(updated).
                build();
    }

    @Override
    public BulkActionRes bulk(final BulkAction bulkAction) {
        return controller.bulk(bulkAction);
    }

    @Override
    public Response associate(final Long userId, final ResourceAssociationActionType type,
            final List<ResourceNameTO> resourceNames) {

        UserTO updated = null;

        switch (type) {
            case UNLINK:
                updated = controller.unlink(userId, CollectionWrapper.unwrap(resourceNames));
                break;

            case UNASSIGN:
                updated = controller.unassign(userId, CollectionWrapper.unwrap(resourceNames));
                break;

            case DEPROVISION:
                updated = controller.deprovision(userId, CollectionWrapper.unwrap(resourceNames));
                break;

            default:
                updated = controller.read(userId);
        }

        return Response.ok(updated).
                build();
    }
}

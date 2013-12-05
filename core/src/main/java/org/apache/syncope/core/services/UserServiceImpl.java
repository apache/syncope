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

import java.util.List;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
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
public class UserServiceImpl extends AbstractServiceImpl implements UserService {

    @Autowired
    private UserController controller;

    @Override
    public Response getUsername(final Long userId) {
        return Response.ok().header(HttpHeaders.ALLOW, "GET,POST,OPTIONS,HEAD").
                header(RESTHeaders.USERNAME, controller.getUsername(userId)).
                build();
    }

    @Override
    public Response getUserId(final String username) {
        return Response.ok().header(HttpHeaders.ALLOW, "GET,POST,OPTIONS,HEAD").
                header(RESTHeaders.USER_ID, controller.getUserId(username)).
                build();
    }

    @Override
    public int count() {
        return controller.count();
    }

    @Override
    public Response create(final UserTO userTO) {
        UserTO created = controller.create(userTO);
        return createResponse(created.getId(), created).build();
    }

    @Override
    public Response delete(final Long userId) {
        UserTO user = controller.read(userId);

        ResponseBuilder builder = context.getRequest().evaluatePreconditions(new EntityTag(user.getETagValue()));
        if (builder == null) {
            UserTO deleted = controller.delete(userId);
            builder = modificationResponse(deleted);
        }

        return builder.build();
    }

    @Override
    public List<UserTO> list() {
        return list(1, 25);
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
    public List<UserTO> search(final NodeCond searchCondition) throws InvalidSearchConditionException {
        return controller.search(searchCondition, 1, 25);
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
        UserTO user = controller.read(userId);

        ResponseBuilder builder = context.getRequest().evaluatePreconditions(new EntityTag(user.getETagValue()));
        if (builder == null) {
            userMod.setId(userId);
            UserTO updated = controller.update(userMod);
            builder = modificationResponse(updated);
        }

        return builder.build();
    }

    @Override
    public Response status(final Long userId, final StatusMod statusMod) {
        UserTO user = controller.read(userId);

        ResponseBuilder builder = context.getRequest().evaluatePreconditions(new EntityTag(user.getETagValue()));
        if (builder == null) {
            statusMod.setId(userId);
            UserTO updated = controller.status(statusMod);
            builder = modificationResponse(updated);
        }

        return builder.build();
    }

    @Override
    public BulkActionRes bulk(final BulkAction bulkAction) {
        return controller.bulk(bulkAction);
    }

    @Override
    public Response associate(final Long userId, final ResourceAssociationActionType type,
            final List<ResourceNameTO> resourceNames) {

        UserTO user = controller.read(userId);

        ResponseBuilder builder = context.getRequest().evaluatePreconditions(new EntityTag(user.getETagValue()));
        if (builder == null) {
            UserTO updated;

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

            builder = modificationResponse(updated);
        }

        return builder.build();
    }
}

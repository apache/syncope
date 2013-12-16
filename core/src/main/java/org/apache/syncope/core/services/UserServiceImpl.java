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
import org.apache.syncope.common.services.UserService;
import org.apache.syncope.common.reqres.BulkAction;
import org.apache.syncope.common.reqres.BulkActionResult;
import org.apache.syncope.common.reqres.PagedResult;
import org.apache.syncope.common.wrap.ResourceName;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.RESTHeaders;
import org.apache.syncope.common.types.ResourceAssociationActionType;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.core.persistence.dao.search.SearchCond;
import org.apache.syncope.core.rest.controller.UserController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends AbstractServiceImpl implements UserService {

    @Autowired
    private UserController controller;

    @Override
    public Response getUsername(final Long userId) {
        return Response.ok().header(HttpHeaders.ALLOW, OPTIONS_ALLOW).
                header(RESTHeaders.USERNAME, controller.getUsername(userId)).
                build();
    }

    @Override
    public Response getUserId(final String username) {
        return Response.ok().header(HttpHeaders.ALLOW, OPTIONS_ALLOW).
                header(RESTHeaders.USER_ID, controller.getUserId(username)).
                build();
    }

    @Override
    public Response create(final UserTO userTO) {
        UserTO created = controller.create(userTO);
        return createResponse(created.getId(), created).build();
    }

    @Override
    public Response delete(final Long userId) {
        UserTO user = controller.read(userId);

        ResponseBuilder builder = messageContext.getRequest().evaluatePreconditions(new EntityTag(user.getETagValue()));
        if (builder == null) {
            UserTO deleted = controller.delete(userId);
            builder = modificationResponse(deleted);
        }

        return builder.build();
    }

    @Override
    public PagedResult<UserTO> list() {
        return list(DEFAULT_PARAM_PAGE_VALUE, DEFAULT_PARAM_SIZE_VALUE);
    }

    @Override
    public PagedResult<UserTO> list(final int page, final int size) {
        checkPageSize(page, size);
        return buildPagedResult(controller.list(page, size), page, size, controller.count());
    }

    @Override
    public UserTO read(final Long userId) {
        return controller.read(userId);
    }

    @Override
    public PagedResult<UserTO> search(final String fiql) {
        return search(fiql, DEFAULT_PARAM_PAGE_VALUE, DEFAULT_PARAM_SIZE_VALUE);
    }

    @Override
    public PagedResult<UserTO> search(final String fiql, final int page, final int size) {
        checkPageSize(page, size);
        SearchCond cond = getSearchCond(fiql);
        return buildPagedResult(controller.search(cond, page, size), page, size, controller.searchCount(cond));
    }

    @Override
    public Response update(final Long userId, final UserMod userMod) {
        UserTO user = controller.read(userId);

        ResponseBuilder builder = messageContext.getRequest().evaluatePreconditions(new EntityTag(user.getETagValue()));
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

        ResponseBuilder builder = messageContext.getRequest().evaluatePreconditions(new EntityTag(user.getETagValue()));
        if (builder == null) {
            statusMod.setId(userId);
            UserTO updated = controller.status(statusMod);
            builder = modificationResponse(updated);
        }

        return builder.build();
    }

    @Override
    public BulkActionResult bulk(final BulkAction bulkAction) {
        return controller.bulk(bulkAction);
    }

    @Override
    public Response associate(final Long userId, final ResourceAssociationActionType type,
            final List<ResourceName> resourceNames) {

        UserTO user = controller.read(userId);

        ResponseBuilder builder = messageContext.getRequest().evaluatePreconditions(new EntityTag(user.getETagValue()));
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

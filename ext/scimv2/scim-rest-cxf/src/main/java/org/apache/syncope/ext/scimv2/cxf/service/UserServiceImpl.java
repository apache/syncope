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
package org.apache.syncope.ext.scimv2.cxf.service;

import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.logic.GroupLogic;
import org.apache.syncope.core.logic.SCIMDataBinder;
import org.apache.syncope.core.logic.UserLogic;
import org.apache.syncope.core.logic.scim.SCIMConfManager;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.ext.scimv2.api.BadRequestException;
import org.apache.syncope.ext.scimv2.api.data.ListResponse;
import org.apache.syncope.ext.scimv2.api.data.SCIMSearchRequest;
import org.apache.syncope.ext.scimv2.api.data.SCIMUser;
import org.apache.syncope.ext.scimv2.api.service.UserService;
import org.apache.syncope.ext.scimv2.api.type.ErrorType;
import org.apache.syncope.ext.scimv2.api.type.Resource;
import org.apache.syncope.ext.scimv2.api.type.SortOrder;

public class UserServiceImpl extends AbstractService<SCIMUser> implements UserService {

    public UserServiceImpl(
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final UserLogic userLogic,
            final GroupLogic groupLogic,
            final SCIMDataBinder binder,
            final SCIMConfManager confManager) {

        super(userDAO, groupDAO, userLogic, groupLogic, binder, confManager);
    }

    @Override
    public Response create(final SCIMUser user) {
        ProvisioningResult<UserTO> result = userLogic.create(binder.toUserCR(user), false);
        return createResponse(
                result.getEntity().getKey(),
                binder.toSCIMUser(
                        result.getEntity(),
                        uriInfo.getAbsolutePathBuilder().path(result.getEntity().getKey()).build().toASCIIString(),
                        List.of(),
                        List.of()));
    }

    @Override
    public SCIMUser get(final String id,
            final String attributes,
            final String excludedAttributes) {

        return binder.toSCIMUser(
                userLogic.read(id),
                uriInfo.getAbsolutePathBuilder().build().toASCIIString(),
                List.of(ArrayUtils.nullToEmpty(StringUtils.split(attributes, ','))),
                List.of(ArrayUtils.nullToEmpty(StringUtils.split(excludedAttributes, ','))));
    }

    @Override
    public Response update(final String id) {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    @Override
    public Response replace(final String id, final SCIMUser user) {
        if (!id.equals(user.getId())) {
            throw new BadRequestException(ErrorType.invalidPath, "Expected " + id + ", found " + user.getId());
        }

        ResponseBuilder builder = checkETag(Resource.User, id);
        if (builder != null) {
            return builder.build();
        }

        ProvisioningResult<UserTO> result = userLogic.update(
                AnyOperations.diff(binder.toUserTO(user), userLogic.read(id), false), false);
        return updateResponse(
                result.getEntity().getKey(),
                binder.toSCIMUser(
                        result.getEntity(),
                        uriInfo.getAbsolutePathBuilder().path(result.getEntity().getKey()).build().toASCIIString(),
                        List.of(),
                        List.of()));
    }

    @Override
    public Response delete(final String id) {
        ResponseBuilder builder = checkETag(Resource.User, id);
        if (builder != null) {
            return builder.build();
        }

        anyLogic(Resource.User).delete(id, false);
        return Response.noContent().build();
    }

    @Override
    public ListResponse<SCIMUser> search(
            final String attributes,
            final String excludedAttributes,
            final String filter,
            final String sortBy,
            final SortOrder sortOrder,
            final Integer startIndex,
            final Integer count) {

        SCIMSearchRequest request = new SCIMSearchRequest(filter, sortBy, sortOrder, startIndex, count);
        if (attributes != null) {
            request.getAttributes().addAll(
                    List.of(ArrayUtils.nullToEmpty(StringUtils.split(attributes, ','))));
        }
        if (excludedAttributes != null) {
            request.getExcludedAttributes().addAll(
                    List.of(ArrayUtils.nullToEmpty(StringUtils.split(excludedAttributes, ','))));
        }

        return doSearch(Resource.User, request);
    }

    @Override
    public ListResponse<SCIMUser> search(final SCIMSearchRequest request) {
        return doSearch(Resource.User, request);
    }
}

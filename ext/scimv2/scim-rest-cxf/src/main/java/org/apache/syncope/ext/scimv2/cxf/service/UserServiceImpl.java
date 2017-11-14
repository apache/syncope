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
import java.util.UUID;
import javax.ws.rs.core.Response;
import org.apache.syncope.ext.scimv2.api.data.ListResponse;
import org.apache.syncope.ext.scimv2.api.data.SCIMUser;
import org.apache.syncope.ext.scimv2.api.service.UserService;
import org.apache.syncope.ext.scimv2.api.type.Resource;
import org.apache.syncope.ext.scimv2.api.type.SortOrder;

public class UserServiceImpl extends AbstractService<SCIMUser> implements UserService {

    @Override
    public Response create() {
        return Response.
                created(uriInfo.getAbsolutePathBuilder().path(UUID.randomUUID().toString()).build()).
                build();
    }

    @Override
    public SCIMUser read(final String id) {
        return binder().toSCIMUser(userLogic().read(id), uriInfo.getAbsolutePathBuilder().build().toASCIIString());
    }

    @Override
    public Response replace(final String id) {
        return Response.ok().build();
    }

    @Override
    public Response delete(final String id) {
        return Response.noContent().build();
    }

    @Override
    public Response update(final String id) {
        return Response.ok().build();
    }

    @Override
    public ListResponse<SCIMUser> search(
            final Integer startIndex,
            final Integer count,
            final String filter,
            final String sortBy,
            final SortOrder sortOrder,
            final List<String> attributes) {

        return doSearch(Resource.User, startIndex, count, filter, sortBy, sortOrder, attributes);
    }
}

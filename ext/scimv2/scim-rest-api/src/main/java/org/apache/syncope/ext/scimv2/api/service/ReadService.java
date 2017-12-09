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
package org.apache.syncope.ext.scimv2.api.service;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.apache.syncope.ext.scimv2.api.SCIMConstants;
import org.apache.syncope.ext.scimv2.api.data.ListResponse;
import org.apache.syncope.ext.scimv2.api.data.SCIMResource;
import org.apache.syncope.ext.scimv2.api.data.SCIMSearchRequest;
import org.apache.syncope.ext.scimv2.api.type.SortOrder;

public interface ReadService<R extends SCIMResource> {

    @GET
    @Path("{id}")
    @Produces({ SCIMConstants.APPLICATION_SCIM_JSON })
    R get(@NotNull @PathParam("id") String id,
            @QueryParam("attributes") String attributes,
            @QueryParam("excludedAttributes") String excludedAttributes);

    @GET
    @Produces({ SCIMConstants.APPLICATION_SCIM_JSON })
    ListResponse<R> search(
            @QueryParam("attributes") String attributes,
            @QueryParam("excludedAttributes") String excludedAttributes,
            @QueryParam("filter") String filter,
            @QueryParam("sortBy") String sortBy,
            @QueryParam("sortOrder") SortOrder sortOrder,
            @QueryParam("startIndex") Integer startIndex,
            @QueryParam("count") Integer count);

    @POST
    @Path(".search")
    @Produces({ SCIMConstants.APPLICATION_SCIM_JSON })
    @Consumes({ SCIMConstants.APPLICATION_SCIM_JSON })
    ListResponse<R> search(SCIMSearchRequest request);
}

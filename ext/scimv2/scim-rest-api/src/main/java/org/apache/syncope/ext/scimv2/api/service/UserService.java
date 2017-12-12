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
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.ext.PATCH;
import org.apache.syncope.ext.scimv2.api.SCIMConstants;
import org.apache.syncope.ext.scimv2.api.data.SCIMUser;

@Path("v2/Users")
public interface UserService extends ReadService<SCIMUser> {

    @POST
    @Produces({ SCIMConstants.APPLICATION_SCIM_JSON })
    @Consumes({ SCIMConstants.APPLICATION_SCIM_JSON })
    Response create(SCIMUser user);

    @PATCH
    @Path("{id}")
    @Produces({ SCIMConstants.APPLICATION_SCIM_JSON })
    @Consumes({ SCIMConstants.APPLICATION_SCIM_JSON })
    Response update(@NotNull @PathParam("id") String id);

    @PUT
    @Path("{id}")
    @Produces({ SCIMConstants.APPLICATION_SCIM_JSON })
    @Consumes({ SCIMConstants.APPLICATION_SCIM_JSON })
    Response replace(@NotNull @PathParam("id") String id, SCIMUser user);

    @DELETE
    @Path("{id}")
    @Produces({ SCIMConstants.APPLICATION_SCIM_JSON })
    Response delete(@NotNull @PathParam("id") String id);

}

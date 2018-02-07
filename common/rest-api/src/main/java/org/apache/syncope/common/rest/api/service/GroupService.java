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
package org.apache.syncope.common.rest.api.service;

import io.swagger.annotations.Api;
import io.swagger.annotations.Authorization;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.patch.GroupPatch;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.types.BulkMembersActionType;

/**
 * REST operations for groups.
 */
@Api(tags = "Groups", authorizations = {
    @Authorization(value = "BasicAuthentication")
    , @Authorization(value = "Bearer") })
@Path("groups")
public interface GroupService extends AnyService<GroupTO> {

    /**
     * Creates a new group.
     *
     * @param groupTO group to be created
     * @return Response object featuring Location header of created group as well as the any
     * object itself enriched with propagation status information - ProvisioningResult as Entity
     */
    @POST
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response create(@NotNull GroupTO groupTO);

    /**
     * Updates group matching the provided key.
     *
     * @param groupPatch modification to be applied to group matching the provided key
     * @return Response object featuring the updated group enriched with propagation status information
     * - ProvisioningResult as Entity
     */
    @PATCH
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response update(@NotNull GroupPatch groupPatch);

    /**
     * Updates group matching the provided key.
     *
     * @param groupTO complete update
     * @return Response object featuring the updated group enriched with propagation status information
     * - ProvisioningResult as Entity
     */
    @PUT
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response update(@NotNull GroupTO groupTO);

    /**
     * This method is similar to read() but uses different authentication handling to ensure that a group
     * can read his own groups.
     *
     * @return own groups
     */
    @GET
    @Path("own")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    List<GroupTO> own();

    /**
     * (De)provision all members of the given group from / onto all the resources associated to it.
     *
     * @param key group key
     * @param actionType action type to perform on all group members
     * @return execution report for the task generated on purpose
     */
    @POST
    @Path("{key}/members/{actionType}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    ExecTO bulkMembersAction(
            @NotNull @PathParam("key") String key,
            @NotNull @PathParam("actionType") BulkMembersActionType actionType);
}

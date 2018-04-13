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

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.ReconciliationStatus;
import org.apache.syncope.common.lib.types.AnyTypeKind;

/**
 * REST operations for tasks.
 */
@Tag(name = "Reconciliation")
@SecurityRequirements({
    @SecurityRequirement(name = "BasicAuthentication"),
    @SecurityRequirement(name = "Bearer") })
@Path("reconciliation")
public interface ReconciliationService extends JAXRSService {

    /**
     * Gets current attributes on Syncope and on the given External Resource, related to given user, group or
     * any object.
     *
     * @param anyTypeKind anyTypeKind
     * @param anyKey user, group or any object: if value looks like a UUID then it is interpreted as key, otherwise as
     * a (user)name
     * @param resourceKey resource key
     * @return reconciliation status
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, SyncopeConstants.APPLICATION_YAML, MediaType.APPLICATION_XML })
    ReconciliationStatus status(
            @NotNull @QueryParam("anyTypeKind") AnyTypeKind anyTypeKind,
            @NotNull @QueryParam("anyKey") String anyKey,
            @NotNull @QueryParam("resourceKey") String resourceKey);

    /**
     * Pushes the given user, group or any object in Syncope onto the External Resource.
     *
     * @param anyTypeKind anyTypeKind
     * @param anyKey user, group or any object: if value looks like a UUID then it is interpreted as key, otherwise as
     * a (user)name
     * @param resourceKey resource key
     * @param pushTask push specification
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @POST
    @Path("push")
    @Consumes({ MediaType.APPLICATION_JSON, SyncopeConstants.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, SyncopeConstants.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void push(
            @NotNull @QueryParam("anyTypeKind") AnyTypeKind anyTypeKind,
            @NotNull @QueryParam("anyKey") String anyKey,
            @NotNull @QueryParam("resourceKey") String resourceKey,
            @NotNull PushTaskTO pushTask);

    /**
     * Pulls the given user, group or any object from the External Resource into Syncope.
     *
     * @param anyTypeKind anyTypeKind
     * @param anyKey user, group or any object: if value looks like a UUID then it is interpreted as key, otherwise as
     * a (user)name
     * @param resourceKey resource key
     * @param pullTask pull specification
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @POST
    @Path("pull")
    @Consumes({ MediaType.APPLICATION_JSON, SyncopeConstants.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, SyncopeConstants.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void pull(
            @NotNull @QueryParam("anyTypeKind") AnyTypeKind anyTypeKind,
            @NotNull @QueryParam("anyKey") String anyKey,
            @NotNull @QueryParam("resourceKey") String resourceKey,
            @NotNull PullTaskTO pullTask);
}

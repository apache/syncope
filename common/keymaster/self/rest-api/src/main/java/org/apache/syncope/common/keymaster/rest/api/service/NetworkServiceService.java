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
package org.apache.syncope.common.keymaster.rest.api.service;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.io.Serializable;
import java.util.List;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;

/**
 * REST operations for Self Keymaster's service discovery.
 */
@Tag(name = "Network Services")
@SecurityRequirements({
    @SecurityRequirement(name = "BasicAuthentication") })
@Path("networkServices")
public interface NetworkServiceService extends Serializable {

    enum Action {
        register,
        unregister

    }

    /**
     * Returns the list of registered services.
     *
     * @param serviceType service type
     * @return list of registered services
     */
    @GET
    @Path("{serviceType}")
    @Produces({ MediaType.APPLICATION_JSON })
    List<NetworkService> list(@NotNull @PathParam("serviceType") NetworkService.Type serviceType);

    /**
     * Returns the service instance to invoke, for the given type.
     *
     * @param serviceType service type
     * @return service instance to invoke, for the given type
     */
    @GET
    @Path("{serviceType}/get")
    @Produces({ MediaType.APPLICATION_JSON })
    NetworkService get(@NotNull @PathParam("serviceType") NetworkService.Type serviceType);

    /**
     * (Un)registers the given service.
     *
     * @param networkService service instance
     * @param action action to perform on the given service
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    void action(
            @NotNull NetworkService networkService,
            @QueryParam("action") Action action);
}

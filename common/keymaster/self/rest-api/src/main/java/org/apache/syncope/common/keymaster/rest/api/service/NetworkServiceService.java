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

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.Serializable;
import java.util.List;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;

/**
 * REST operations for Self Keymaster's service discovery.
 */
@Path("networkServices")
public interface NetworkServiceService extends Serializable {

    enum Action {
        register,
        unregister

    }

    @GET
    @Path("{serviceType}")
    @Produces({ MediaType.APPLICATION_JSON })
    List<NetworkService> list(@NotNull @PathParam("serviceType") NetworkService.Type serviceType);

    @GET
    @Path("{serviceType}/get")
    @Produces({ MediaType.APPLICATION_JSON })
    NetworkService get(@NotNull @PathParam("serviceType") NetworkService.Type serviceType);

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    Response action(
            @NotNull NetworkService networkService,
            @QueryParam("action") Action action);
}

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

import java.io.Serializable;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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

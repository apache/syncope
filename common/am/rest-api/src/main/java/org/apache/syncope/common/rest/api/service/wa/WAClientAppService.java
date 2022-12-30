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
package org.apache.syncope.common.rest.api.service.wa;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.lib.wa.WAClientApp;
import org.apache.syncope.common.rest.api.service.JAXRSService;

/**
 * REST operations for WA to read client applications.
 */
@Tag(name = "WA")
@SecurityRequirements({
    @SecurityRequirement(name = "BasicAuthentication"),
    @SecurityRequirement(name = "Bearer") })
@Path("wa/clientApps")
public interface WAClientAppService extends JAXRSService {

    /**
     * Returns a list of all client applications available.
     *
     * @return list of all client applications.
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    List<WAClientApp> list();

    /**
     * Returns client application with matching type and clientAppId, if found.
     *
     * @param clientAppId registered client application ID to be read
     * @param type client application type
     * @return client application with matching type and clientAppId
     */
    @GET
    @Path("{clientAppId}")
    @Produces({ MediaType.APPLICATION_JSON })
    WAClientApp read(@NotNull @PathParam("clientAppId") Long clientAppId, @QueryParam("type") ClientAppType type);

    /**
     * Returns client application with matching type and name, if found.
     *
     * @param name registered client application name to be read
     * @param type client application type
     * @return client application with matching type and name
     */
    @GET
    @Path("byName/{name}")
    @Produces({ MediaType.APPLICATION_JSON })
    WAClientApp read(@NotNull @PathParam("name") String name, @QueryParam("type") ClientAppType type);
}

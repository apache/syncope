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
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST operations for Self Keymaster's conf params.
 */
@Tag(name = "Conf Parameters")
@SecurityRequirements({
    @SecurityRequirement(name = "BasicAuthentication") })
@Path("conf")
public interface ConfParamService extends Serializable {

    /**
     * Returns the full list of defined conf parameters, with values.
     *
     * @return full list of defined conf parameters, with values
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    Map<String, Object> list();

    /**
     * Returns the value(s) of the given conf parameter, if defined.
     *
     * @param key conf parameter key
     * @return the value(s) of the given conf parameter, if defined
     */
    @GET
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON })
    Response get(@NotNull @PathParam("key") String key);

    /**
     * Sets the value(s) for the given conf parameter.
     *
     * @param key conf parameter key
     * @param value conf parameter value(s)
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @POST
    @Path("{key}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    void set(@NotNull @PathParam("key") String key, InputStream value);

    /**
     * Deletes the conf parameter matching the provided key.
     *
     * @param key conf parameter key
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @DELETE
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON })
    void remove(@NotNull @PathParam("key") String key);
}

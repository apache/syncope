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
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.types.ImplementationType;

/**
 * REST operations for implementations.
 */
@Api(tags = "Implementations", authorizations = {
    @Authorization(value = "BasicAuthentication")
    , @Authorization(value = "Bearer") })
@Path("implementations")
public interface ImplementationService extends JAXRSService {

    /**
     * Returns a list of all implementations.
     *
     * @param type implementation type
     * @return list of all implementations.
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    List<ImplementationTO> list(@NotNull @MatrixParam("type") ImplementationType type);

    /**
     * Returns implementation with matching key.
     *
     * @param key key of implementation to be read
     * @return implementation with matching key
     */
    @GET
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    ImplementationTO read(@NotNull @PathParam("key") String key);

    /**
     * Creates a new implementation.
     *
     * @param implementationTO implementation.
     * @return Response object featuring Location header of created implementation
     */
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response create(@NotNull ImplementationTO implementationTO);

    /**
     * Updates an existing implementation.
     *
     * @param implementationTO implementation.
     * @return an empty response if operation was successful
     */
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response update(@NotNull ImplementationTO implementationTO);

    /**
     * Deletes the implementation matching the given key.
     *
     * @param key key for implementation to be deleted
     * @return an empty response if operation was successful
     */
    @DELETE
    @Path("{key}")
    Response delete(@NotNull @PathParam("key") String key);

}

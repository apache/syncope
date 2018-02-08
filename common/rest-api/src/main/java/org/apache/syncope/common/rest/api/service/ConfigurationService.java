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

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.AttrTO;

/**
 * REST operations for configuration.
 */
@Tag(name = "Configuration")
@SecurityRequirements({
    @SecurityRequirement(name = "BasicAuthentication")
    , @SecurityRequirement(name = "Bearer") })
@Path("configurations")
public interface ConfigurationService extends JAXRSService {

    /**
     * Exports internal storage content as downloadable XML file.
     *
     * @return internal storage content as downloadable XML file
     */
    @GET
    @Path("stream")
    Response export();

    /**
     * Returns all configuration parameters.
     *
     * @return all configuration parameters
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    List<AttrTO> list();

    /**
     * Returns configuration parameter with matching schema.
     *
     * @param schema identifier of configuration to be read
     * @return configuration parameter with matching schema
     */
    @GET
    @Path("{schema}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    AttrTO get(@NotNull @PathParam("schema") String schema);

    /**
     * Creates / updates the configuration parameter with the given schema.
     *
     * @param value parameter value
     * @return an empty response if operation was successful
     */
    @PUT
    @Path("{schema}")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response set(@NotNull AttrTO value);

    /**
     * Deletes the configuration parameter with matching schema.
     *
     * @param schema configuration parameter schema
     * @return an empty response if operation was successful
     */
    @DELETE
    @Path("{schema}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response delete(@NotNull @PathParam("schema") String schema);
}

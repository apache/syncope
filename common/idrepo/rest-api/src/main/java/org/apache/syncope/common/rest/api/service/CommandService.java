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

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.syncope.common.lib.command.CommandOutput;
import org.apache.syncope.common.lib.command.CommandTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.CommandQuery;

/**
 * REST operations for commands.
 */
@Tag(name = "Commands")
@SecurityRequirements({
    @SecurityRequirement(name = "BasicAuthentication"),
    @SecurityRequirement(name = "Bearer") })
@Path("commands")
public interface CommandService extends JAXRSService {

    /**
     * Returns a paged list of all commands.
     *
     * @param query query conditions
     * @return list of all commands.
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    PagedResult<CommandTO> search(@BeanParam CommandQuery query);

    /**
     * Returns the command for the given key, if found.
     *
     * @param key command key
     * @return the command for the given key, if found
     */
    @Parameter(name = "key", description = "Command's key", in = ParameterIn.PATH, schema =
            @Schema(type = "string"))
    @GET
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    CommandTO read(@PathParam("key") String key);

    /**
     * Runs the given command with the given arguments and returns the resulting output.
     *
     * @param command command to run, with arguments
     * @return command output
     */
    @Parameter(name = "key", description = "Command's key", in = ParameterIn.PATH, schema =
            @Schema(type = "string"))
    @POST
    @Path("{key}")
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    CommandOutput run(CommandTO command);
}

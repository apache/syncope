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
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.syncope.common.lib.form.SyncopeForm;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.TaskTO;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.ExecSpecs;
import org.apache.syncope.common.rest.api.beans.TaskQuery;

/**
 * REST operations for tasks.
 */
@Tag(name = "Tasks")
@SecurityRequirements({
    @SecurityRequirement(name = "BasicAuthentication"),
    @SecurityRequirement(name = "Bearer") })
@Path("tasks")
public interface TaskService extends ExecutableService {

    /**
     * Returns the task matching the given key.
     *
     * @param type task type
     * @param key key of task to be read
     * @param details whether include executions or not, defaults to true
     * @param <T> type of taskTO
     * @return task with matching key
     */
    @GET
    @Path("{type}/{key}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    <T extends TaskTO> T read(
            @NotNull @PathParam("type") TaskType type,
            @NotNull @PathParam("key") String key,
            @QueryParam(JAXRSService.PARAM_DETAILS) @DefaultValue("true") boolean details);

    /**
     * Returns a paged list of existing tasks matching the given query.
     *
     * @param query query conditions
     * @param <T> type of taskTO
     * @return paged list of existing tasks matching the given query
     */
    @GET
    @Path("{type}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    <T extends TaskTO> PagedResult<T> search(@BeanParam TaskQuery query);

    /**
     * Creates a new task.
     *
     * @param type task type
     * @param taskTO task to be created
     * @return Response object featuring Location header of created task
     */
    @ApiResponses(
            @ApiResponse(responseCode = "201",
                    description = "Task successfully created", headers = {
                @Header(name = RESTHeaders.RESOURCE_KEY, schema =
                        @Schema(type = "string"),
                        description = "UUID generated for the entity created"),
                @Header(name = HttpHeaders.LOCATION, schema =
                        @Schema(type = "string"),
                        description = "URL of the entity created") }))
    @POST
    @Path("{type}")
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Response create(@NotNull @PathParam("type") TaskType type, @NotNull SchedTaskTO taskTO);

    /**
     * Updates the task matching the provided key.
     *
     * @param type task type
     * @param taskTO updated task to be stored
     */
    @Parameter(name = "key", description = "Task's key", in = ParameterIn.PATH, schema =
            @Schema(type = "string"))
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @PUT
    @Path("{type}/{key}")
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void update(@NotNull @PathParam("type") TaskType type, @NotNull SchedTaskTO taskTO);

    /**
     * Deletes the task matching the provided key.
     *
     * @param type task type
     * @param key key of task to be deleted
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @DELETE
    @Path("{type}/{key}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void delete(@NotNull @PathParam("type") TaskType type, @NotNull @PathParam("key") String key);

    /**
     * Deletes all the propagation tasks whose latest execution is matching the given conditions.
     * At least one matching condition must be specified.
     *
     * @param since match all executions started afterwards
     * @param statuses execution status(es) to match
     * @param resources external resource(s) to match
     * @return deleted propagation tasks
     */
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of deleted propagation tasks, as Entity"),
        @ApiResponse(responseCode = "412", description = "At least one matching condition must be specified") })
    @DELETE
    @Path("PROPAGATION/purge")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Response purgePropagations(
            @QueryParam("since") OffsetDateTime since,
            @QueryParam("statuses") List<ExecStatus> statuses,
            @QueryParam("resources") List<String> resources);

    /**
     * Fetches the form to fill and submit for execution, for the given macro task (if defined).
     *
     * @param key macro task key
     * @param locale form locale
     * @return the form to fill and submit for execution, for the given macro task (if defined)
     */
    @GET
    @Path("MACRO/{key}/form")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    SyncopeForm getMacroTaskForm(@NotNull @PathParam("key") String key, @NotNull @QueryParam("locale") String locale);

    /**
     * Executes the macro task matching the given specs, with the provided form as input.
     *
     * @param specs conditions to exec
     * @param macroTaskForm macro task form
     * @return execution report for the macro task matching the given specs
     */
    @POST
    @Path("MACRO/{key}/execute")
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    ExecTO execute(@BeanParam ExecSpecs specs, SyncopeForm macroTaskForm);
}

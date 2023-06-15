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
import io.swagger.v3.oas.annotations.media.Content;
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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserRequest;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.lib.to.WorkflowTaskExecInput;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.UserRequestQuery;

/**
 * REST operations related to user workflow.
 */
@Tag(name = "Flowable")
@SecurityRequirements({
    @SecurityRequirement(name = "BasicAuthentication"),
    @SecurityRequirement(name = "Bearer") })
@Path("flowable/userRequests")
public interface UserRequestService extends JAXRSService {

    /**
     * Returns a list of running user requests matching the given query.
     *
     * @param query query conditions
     * @return list of all running user requests
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    PagedResult<UserRequest> listRequests(@BeanParam UserRequestQuery query);

    /**
     * Starts a new request for the given BPMN Process and user (if provided) or requesting user (if not provided).
     *
     * @param bpmnProcess BPMN process
     * @param user if value looks like a UUID then it is interpreted as key otherwise as a username
     * @param inputVariables initial request variables
     * @return data about the started request service, including execution id
     */
    @POST
    @Path("start/{bpmnProcess}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    UserRequest startRequest(
            @NotNull @PathParam("bpmnProcess") String bpmnProcess,
            @QueryParam(JAXRSService.PARAM_USER) String user,
            WorkflowTaskExecInput inputVariables);

    /**
     * Cancel a running user request.
     *
     * @param executionId execution id
     * @param reason reason to cancel the user request
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @DELETE
    @Path("{executionId}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void cancelRequest(
            @NotNull @PathParam("executionId") String executionId,
            @QueryParam("reason") String reason);

    /**
     * Returns a user request form matching the given task id.
     *
     * @param username username of the logged user
     * @param taskId workflow task id
     * @return the form for the given task id
     */
    @GET
    @Path("forms/{username}/{taskId}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    UserRequestForm getForm(
            @NotNull @PathParam("username") String username,
            @NotNull @PathParam("taskId") String taskId);

    /**
     * Returns a list of user request forms matching the given query.
     *
     * @param query query conditions
     * @return list of all available user request forms
     */
    @GET
    @Path("forms")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    PagedResult<UserRequestForm> listForms(@BeanParam UserRequestQuery query);

    /**
     * Requests to manage the form for the given task id.
     *
     * @param taskId workflow task id
     * @return the form for the given task id
     */
    @POST
    @Path("forms/{taskId}/claim")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    UserRequestForm claimForm(@NotNull @PathParam("taskId") String taskId);

    /**
     * Cancels request to manage the form for the given task id.
     *
     * @param taskId workflow task id
     * @return the workflow form for the given task id
     */
    @POST
    @Path("forms/{taskId}/unclaim")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    UserRequestForm unclaimForm(@NotNull @PathParam("taskId") String taskId);

    /**
     * Submits a user request form.
     *
     * @param form user request form.
     * @return Response object featuring the updated user enriched with propagation status information
     */
    @Parameter(
            name = RESTHeaders.PREFER, in = ParameterIn.HEADER,
            description = "Allows client to specify a preference for the result to be returned from the server",
            allowEmptyValue = true, schema =
            @Schema(defaultValue = "return-content", allowableValues = { "return-content", "return-no-content" }))
    @Parameter(name = RESTHeaders.NULL_PRIORITY_ASYNC, in = ParameterIn.HEADER,
            description = "If 'true', instructs the propagation process not to wait for completion when communicating"
            + " with External Resources with no priority set",
            allowEmptyValue = true, schema =
            @Schema(type = "boolean", defaultValue = "false"))
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                description = "User successfully updated enriched with propagation status information, as Entity",
                content =
                @Content(schema =
                        @Schema(implementation = ProvisioningResult.class))),
        @ApiResponse(responseCode = "204",
                description = "No content if 'Prefer: return-no-content' was specified", headers =
                @Header(name = RESTHeaders.PREFERENCE_APPLIED, schema =
                        @Schema(type = "string"),
                        description = "Allows the server to inform the "
                        + "client about the fact that a specified preference was applied")) })
    @POST
    @Path("forms")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Response submitForm(@NotNull UserRequestForm form);
}

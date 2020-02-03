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

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.UserRequest;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.lib.to.WorkflowTaskExecInput;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.UserRequestFormQuery;
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
    PagedResult<UserRequest> list(@BeanParam UserRequestQuery query);

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
    UserRequest start(
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
    void cancel(
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
    PagedResult<UserRequestForm> getForms(@BeanParam UserRequestFormQuery query);

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
     * @return updated user
     */
    @POST
    @Path("forms")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    UserTO submitForm(@NotNull UserRequestForm form);
}

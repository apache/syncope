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
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.WorkflowTask;
import org.apache.syncope.common.lib.to.WorkflowTaskExecInput;
import org.apache.syncope.common.rest.api.RESTHeaders;

@Tag(name = "Flowable")
@SecurityRequirements({
    @SecurityRequirement(name = "BasicAuthentication"),
    @SecurityRequirement(name = "Bearer") })
@Path("flowable/userWorkflowTasks")
public interface UserWorkflowTaskService extends JAXRSService {

    /**
     * Returns a list of available tasks for the given user key.
     *
     * @param userKey user key
     * @return list of available tasks for the given user key
     */
    @GET
    @Path("tasks/{userKey}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    List<WorkflowTask> getAvailableTasks(@NotNull @PathParam("userKey") String userKey);

    /**
     * Executes the next workflow task available, for matching user key.
     *
     * @param workflowTaskExecInput input for task execution
     * @return updated user
     */
    @Parameter(name = "userKey", description = "user key", in = ParameterIn.PATH, schema =
            @Schema(type = "string"))
    @POST
    @Path("tasks/{userKey}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    UserTO executeNextTask(@NotNull WorkflowTaskExecInput workflowTaskExecInput);
}

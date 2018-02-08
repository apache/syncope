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
import org.apache.syncope.common.lib.to.WorkflowDefinitionTO;
import org.apache.syncope.common.rest.api.RESTHeaders;

/**
 * REST operations for workflow definition.
 */
@Tag(name = "Workflow")
@SecurityRequirements({
    @SecurityRequirement(name = "BasicAuthentication")
    , @SecurityRequirement(name = "Bearer") })
@Path("workflows")
public interface WorkflowService extends JAXRSService {

    /**
     * Lists the available workflow definitions, for the given any object type.
     *
     * @param anyType any object type
     * @return available workflow definitions, for the given any object type
     */
    @GET
    @Path("{anyType}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    List<WorkflowDefinitionTO> list(@NotNull @PathParam("anyType") String anyType);

    /**
     * Exports the workflow definition for matching any object type and key.
     *
     * @param anyType any object type
     * @param key workflow definition key
     * @return workflow definition for matching any object type and key
     */
    @GET
    @Path("{anyType}/{key}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response get(
            @NotNull @PathParam("anyType") String anyType,
            @NotNull @PathParam("key") String key);

    /**
     * Exports the workflow diagram representation (if available), for matching any object type and key.
     *
     * @param anyType any object type
     * @param key workflow definition key
     * @return workflow diagram representation
     */
    @GET
    @Path("{anyType}/{key}/diagram.png")
    @Produces({ RESTHeaders.MEDIATYPE_IMAGE_PNG })
    Response exportDiagram(
            @NotNull @PathParam("anyType") String anyType,
            @NotNull @PathParam("key") String key);

    /**
     * Imports the workflow definition for matching any object type, under the provided key.
     *
     * @param anyType any object type
     * @param key workflow definition key
     * @param definition workflow definition for matching kind
     * @return an empty response if operation was successful
     */
    @PUT
    @Path("{anyType}/{key}")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response set(
            @NotNull @PathParam("anyType") String anyType,
            @NotNull @PathParam("key") String key,
            @NotNull String definition);

    /**
     * Removes the workflow definition for matching any object type, under the provided key.
     *
     * @param anyType any object type
     * @param key workflow definition key
     * @return an empty response if operation was successful
     */
    @DELETE
    @Path("{anyType}/{key}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response delete(
            @NotNull @PathParam("anyType") String anyType,
            @NotNull @PathParam("key") String key);
}

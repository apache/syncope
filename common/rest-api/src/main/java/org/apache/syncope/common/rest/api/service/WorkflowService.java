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

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.rest.api.RESTHeaders;

/**
 * REST operations for workflow definition.
 */
@Path("workflows")
public interface WorkflowService extends JAXRSService {

    /**
     * Exports workflow definition for matching kind.
     *
     * @param anyTypeKind any object type
     * @return workflow definition for matching kind
     */
    @GET
    @Path("{anyTypeKind}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response exportDefinition(@NotNull @PathParam("anyTypeKind") AnyTypeKind anyTypeKind);

    /**
     * Exports workflow diagram representation.
     *
     * @param anyTypeKind any object type
     * @return workflow diagram representation
     */
    @GET
    @Path("{anyTypeKind}/diagram.png")
    @Produces({ RESTHeaders.MEDIATYPE_IMAGE_PNG })
    Response exportDiagram(@NotNull @PathParam("anyTypeKind") AnyTypeKind anyTypeKind);

    /**
     * Imports workflow definition for matching kind.
     *
     * @param anyTypeKind any object type
     * @param definition workflow definition for matching kind
     */
    @PUT
    @Path("{anyTypeKind}")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    void importDefinition(@NotNull @PathParam("anyTypeKind") AnyTypeKind anyTypeKind, @NotNull String definition);

}

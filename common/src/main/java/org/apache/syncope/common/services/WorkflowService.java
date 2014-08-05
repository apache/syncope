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
package org.apache.syncope.common.services;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.model.wadl.Description;
import org.apache.cxf.jaxrs.model.wadl.Descriptions;
import org.apache.cxf.jaxrs.model.wadl.DocTarget;
import org.apache.syncope.common.types.RESTHeaders;
import org.apache.syncope.common.types.SubjectType;

/**
 * REST operations for workflow definition management.
 */
@Path("workflows/{kind}")
public interface WorkflowService extends JAXRSService {

    /**
     * Checks whether Activiti is enabled (for users or roles).
     *
     * @param kind user or role
     * @return <tt>Response</tt> contains special syncope HTTP header indicating if Activiti is enabled for
     * users / roles
     * @see org.apache.syncope.common.types.RESTHeaders#ACTIVITI_USER_ENABLED
     * @see org.apache.syncope.common.types.RESTHeaders#ACTIVITI_ROLE_ENABLED
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Contains special syncope HTTP header indicating if Activiti is enabled for users / roles")
    })
    @OPTIONS
    Response getOptions(@NotNull @PathParam("kind") SubjectType kind);

    /**
     * Exports workflow definition for matching kind.
     *
     * @param kind user or role
     * @return workflow definition for matching kind
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response exportDefinition(@NotNull @PathParam("kind") SubjectType kind);

    /**
     * Exports workflow diagram representation.
     *
     * @param kind user or role
     * @return workflow diagram representation
     */
    @GET
    @Path("diagram.png")
    @Produces({ RESTHeaders.MEDIATYPE_IMAGE_PNG })
    Response exportDiagram(@NotNull @PathParam("kind") SubjectType kind);

    /**
     * Imports workflow definition for matching kind.
     *
     * @param kind user or role
     * @param definition workflow definition for matching kind
     */
    @PUT
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    void importDefinition(@NotNull @PathParam("kind") SubjectType kind, @NotNull String definition);
}

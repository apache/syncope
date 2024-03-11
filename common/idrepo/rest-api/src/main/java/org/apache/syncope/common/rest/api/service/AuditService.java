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
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.apache.syncope.common.lib.to.AuditConfTO;
import org.apache.syncope.common.lib.to.AuditEventTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.types.OpEvent;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.AuditQuery;

/**
 * REST operations for audit.
 */
@Tag(name = "Audit")
@SecurityRequirements({
    @SecurityRequirement(name = "BasicAuthentication"),
    @SecurityRequirement(name = "Bearer") })
@Path("audit")
public interface AuditService extends JAXRSService {

    /**
     * Returns a list of all audit configurations.
     *
     * @return list of all audit configurations.
     */
    @GET
    @Path("conf")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    List<AuditConfTO> confs();

    /**
     * Returns the audit configuration with matching key.
     *
     * @param key audit key to be read
     * @return audit configuration with matching key
     */
    @GET
    @Path("conf/{key}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    AuditConfTO getConf(@NotNull @PathParam("key") String key);

    /**
     * Sets an audit configuration 
     *
     * @param auditTO audit configuration to be stored
     */
    @Parameter(name = "key", description = "Audit configuration 's key", in = ParameterIn.PATH, schema =
            @Schema(type = "string"))
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @PUT
    @Path("conf/{key}")
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void setConf(@NotNull AuditConfTO auditTO);

    /**
     * Deletes the audit configuration matching the provided key.
     *
     * @param key audit configuration key to be deleted
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @DELETE
    @Path("conf/{key}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void deleteConf(@NotNull @PathParam("key") String key);

    /**
     * Returns the list of all managed events in audit.
     *
     * @return list of all managed events in audit
     */
    @GET
    @Path("opEvents")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    List<OpEvent> events();

    /**
     * Returns a paged list of audit entries matching the given query.
     *
     * @param auditQuery query conditions
     * @return paged list of audit entries matching the given query
     */
    @GET
    @Path("auditEvents")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    PagedResult<AuditEventTO> search(@BeanParam AuditQuery auditQuery);

    /**
     * Persist an audit event.
     *
     * @param auditEvent audit event to persist.
     */
    @POST
    @Path("auditEvents")
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void create(@NotNull AuditEventTO auditEvent);
}

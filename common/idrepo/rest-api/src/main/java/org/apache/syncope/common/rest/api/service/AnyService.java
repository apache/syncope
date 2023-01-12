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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Set;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.request.ResourceAR;
import org.apache.syncope.common.lib.request.ResourceDR;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.types.ResourceAssociationAction;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.AnyQuery;

public interface AnyService<TO extends AnyTO> extends JAXRSService {

    /**
     * Reads the list of attributes owned by the given any object for the given schema type.
     *
     * Note that for the UserService, GroupService and AnyObjectService subclasses, if the key parameter
     * looks like a UUID then it is interpreted as as key, otherwise as a (user)name.
     *
     * @param key any object key or name
     * @param schemaType schema type
     * @return list of attributes, owned by the given any object, for the given schema type
     */
    @GET
    @Path("{key}/{schemaType}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Set<Attr> read(@NotNull @PathParam("key") String key, @NotNull @PathParam("schemaType") SchemaType schemaType);

    /**
     * Reads the attribute, owned by the given any object, for the given schema type and schema.
     *
     * Note that for the UserService, GroupService and AnyObjectService subclasses, if the key parameter
     * looks like a UUID then it is interpreted as as key, otherwise as a (user)name.
     *
     * @param key any object key or name
     * @param schemaType schema type
     * @param schema schema
     * @return attribute, owned by the given any object, for the given schema type and schema
     */
    @GET
    @Path("{key}/{schemaType}/{schema}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Attr read(
            @NotNull @PathParam("key") String key,
            @NotNull @PathParam("schemaType") SchemaType schemaType,
            @NotNull @PathParam("schema") String schema);

    /**
     * Reads the any object matching the provided key.
     *
     * @param key if value looks like a UUID then it is interpreted as key, otherwise as a (user)name
     * @return any object with matching key
     */
    @GET
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    TO read(@NotNull @PathParam("key") String key);

    /**
     * Returns a paged list of any objects matching the given query.
     *
     * @param anyQuery query conditions
     * @return paged list of any objects matching the given query
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    PagedResult<TO> search(@BeanParam AnyQuery anyQuery);

    /**
     * Adds or replaces the attribute, owned by the given any object, for the given schema type and schema.
     *
     * @param key any object key or name
     * @param schemaType schema type
     * @param attrTO attribute
     * @return Response object featuring the updated any object attribute - as Entity
     */
    @Parameter(name = "schema", description = "Attribute schema's key", in = ParameterIn.PATH, schema =
            @Schema(type = "string"))
    @PUT
    @Path("{key}/{schemaType}/{schema}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Response update(
            @NotNull @PathParam("key") String key,
            @NotNull @PathParam("schemaType") SchemaType schemaType,
            @NotNull Attr attrTO);

    /**
     * Deletes the attribute, owned by the given any object, for the given schema type and schema.
     *
     * @param key any object key or name
     * @param schemaType schema type
     * @param schema schema
     */
    @Operation(operationId = "deleteAttr")
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @DELETE
    @Path("{key}/{schemaType}/{schema}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void delete(
            @NotNull @PathParam("key") String key,
            @NotNull @PathParam("schemaType") SchemaType schemaType,
            @NotNull @PathParam("schema") String schema);

    /**
     * Deletes any object matching provided key.
     *
     * @param key any object key or name
     * @return Response object featuring the deleted any object enriched with propagation status information
     */
    @Operation(operationId = "deleteAny")
    @Parameter(name = RESTHeaders.PREFER, in = ParameterIn.HEADER,
            description = "Allows client to specify a preference for the result to be returned from the server",
            allowEmptyValue = true, schema =
            @Schema(defaultValue = "return-content", allowableValues = { "return-content", "return-no-content" }))
    @Parameter(name = HttpHeaders.IF_MATCH, in = ParameterIn.HEADER,
            description = "When the provided ETag value does not match the latest modification date of the entity, "
            + "an error is reported and the requested operation is not performed.",
            allowEmptyValue = true, schema =
            @Schema(type = "string"))
    @Parameter(name = RESTHeaders.NULL_PRIORITY_ASYNC, in = ParameterIn.HEADER,
            description = "If 'true', instructs the propagation process not to wait for completion when communicating"
            + " with External Resources with no priority set",
            allowEmptyValue = true, schema =
            @Schema(type = "boolean", defaultValue = "false"))
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                description = "User, Group or Any Object successfully deleted enriched with propagation status "
                + "information, as Entity", content =
                @Content(schema =
                        @Schema(implementation = ProvisioningResult.class))),
        @ApiResponse(responseCode = "204",
                description = "No content if 'Prefer: return-no-content' was specified", headers =
                @Header(name = RESTHeaders.PREFERENCE_APPLIED, schema =
                        @Schema(type = "string"),
                        description = "Allows the server to inform the "
                        + "client about the fact that a specified preference was applied")),
        @ApiResponse(responseCode = "412",
                description = "The ETag value provided via the 'If-Match' header does not match the latest modification"
                + " date of the entity") })
    @DELETE
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Response delete(@NotNull @PathParam("key") String key);

    /**
     * Executes resource-related operations on given entity.
     *
     * @param req external resources to be used for propagation-related operations
     * @return batch results as Response entity
     */
    @Parameter(name = RESTHeaders.PREFER, in = ParameterIn.HEADER,
            description = "Allows client to specify a preference for the result to be returned from the server",
            allowEmptyValue = true, schema =
            @Schema(defaultValue = "return-content", allowableValues = { "return-content", "return-no-content" }))
    @Parameter(name = HttpHeaders.IF_MATCH, in = ParameterIn.HEADER,
            description = "When the provided ETag value does not match the latest modification date of the entity, "
            + "an error is reported and the requested operation is not performed.",
            allowEmptyValue = true, schema =
            @Schema(type = "string"))
    @Parameter(name = RESTHeaders.NULL_PRIORITY_ASYNC, in = ParameterIn.HEADER,
            description = "If 'true', instructs the propagation process not to wait for completion when communicating"
            + " with External Resources with no priority set",
            allowEmptyValue = true, schema =
            @Schema(type = "boolean", defaultValue = "false"))
    @Parameter(name = "key", description = "Entity's key", in = ParameterIn.PATH, schema =
            @Schema(type = "string"))
    @Parameter(name = "action", description = "Deassociation action", in = ParameterIn.PATH, schema =
            @Schema(implementation = ResourceDeassociationAction.class))
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                description = "Batch results available, returned as Response entity"),
        @ApiResponse(responseCode = "204",
                description = "No content if 'Prefer: return-no-content' was specified", headers =
                @Header(name = RESTHeaders.PREFERENCE_APPLIED, schema =
                        @Schema(type = "string"),
                        description = "Allows the server to inform the "
                        + "client about the fact that a specified preference was applied")),
        @ApiResponse(responseCode = "412",
                description = "The ETag value provided via the 'If-Match' header does not match the latest modification"
                + " date of the entity") })
    @POST
    @Path("{key}/deassociate/{action}")
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces(RESTHeaders.MULTIPART_MIXED)
    Response deassociate(@NotNull ResourceDR req);

    /**
     * Executes resource-related operations on given entity.
     *
     * @param req external resources to be used for propagation-related operations
     * @return batch results as Response entity
     */
    @Parameter(name = RESTHeaders.PREFER, in = ParameterIn.HEADER,
            description = "Allows client to specify a preference for the result to be returned from the server",
            allowEmptyValue = true, schema =
            @Schema(defaultValue = "return-content", allowableValues = { "return-content", "return-no-content" }))
    @Parameter(name = HttpHeaders.IF_MATCH, in = ParameterIn.HEADER,
            description = "When the provided ETag value does not match the latest modification date of the entity, "
            + "an error is reported and the requested operation is not performed.",
            allowEmptyValue = true, schema =
            @Schema(type = "string"))
    @Parameter(name = RESTHeaders.NULL_PRIORITY_ASYNC, in = ParameterIn.HEADER,
            description = "If 'true', instructs the propagation process not to wait for completion when communicating"
            + " with External Resources with no priority set",
            allowEmptyValue = true, schema =
            @Schema(type = "boolean", defaultValue = "false"))
    @Parameter(name = "key", description = "Entity's key", in = ParameterIn.PATH, schema =
            @Schema(type = "string"))
    @Parameter(name = "action", description = "Association action", in = ParameterIn.PATH, schema =
            @Schema(implementation = ResourceAssociationAction.class))
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                description = "Batch results available, returned as Response entity"),
        @ApiResponse(responseCode = "204",
                description = "No content if 'Prefer: return-no-content' was specified", headers =
                @Header(name = RESTHeaders.PREFERENCE_APPLIED, schema =
                        @Schema(type = "string"),
                        description = "Allows the server to inform the "
                        + "client about the fact that a specified preference was applied")),
        @ApiResponse(responseCode = "412",
                description = "The ETag value provided via the 'If-Match' header does not match the latest modification"
                + " date of the entity") })
    @POST
    @Path("{key}/associate/{action}")
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces(RESTHeaders.MULTIPART_MIXED)
    Response associate(@NotNull ResourceAR req);
}

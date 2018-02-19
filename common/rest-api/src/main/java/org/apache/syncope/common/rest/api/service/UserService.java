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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.ResponseHeader;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.ext.PATCH;
import org.apache.syncope.common.lib.patch.StatusPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.AnyQuery;

/**
 * REST operations for users.
 */
@Api(tags = "Users", authorizations = {
    @Authorization(value = "BasicAuthentication")
    , @Authorization(value = "Bearer") })
@Path("users")
public interface UserService extends AnyService<UserTO> {

    @ApiResponses(
            @ApiResponse(code = 200,
                    message =
                    "User matching the provided key; if value looks like a UUID then it is interpreted as key,"
                    + " otherwise as a username.", responseHeaders =
                    @ResponseHeader(name = HttpHeaders.ETAG, response = String.class,
                            description = "Opaque identifier for the latest modification made to the entity returned"
                            + " by this endpoint")))
    @Override
    UserTO read(String key);

    @Override
    PagedResult<UserTO> search(AnyQuery anyQuery);

    /**
     * Creates a new user.
     *
     * @param userTO user to be created
     * @param storePassword whether password shall be stored internally
     * @return Response object featuring Location header of created user as well as the user itself
     * enriched with propagation status information
     */
    @ApiImplicitParams({
        @ApiImplicitParam(name = RESTHeaders.PREFER, paramType = "header", dataType = "string",
                value = "Allows the client to specify a preference for the result to be returned from the server",
                defaultValue = "return-content", allowableValues = "return-content, return-no-content",
                allowEmptyValue = true)
        , @ApiImplicitParam(name = RESTHeaders.NULL_PRIORITY_ASYNC, paramType = "header", dataType = "boolean",
                value = "If 'true', instructs the propagation process not to wait for completion when communicating"
                + " with External Resources with no priority set",
                defaultValue = "false", allowEmptyValue = true) })
    @ApiResponses(
            @ApiResponse(code = 201,
                    message = "User successfully created enriched with propagation status information, as Entity,"
                    + "or empty if 'Prefer: return-no-content' was specified",
                    response = ProvisioningResult.class, responseHeaders = {
                @ResponseHeader(name = RESTHeaders.RESOURCE_KEY, response = String.class,
                        description = "UUID generated for the user created")
                , @ResponseHeader(name = HttpHeaders.LOCATION, response = String.class,
                        description = "URL of the user created")
                , @ResponseHeader(name = RESTHeaders.PREFERENCE_APPLIED, response = String.class,
                        description = "Allows the server to inform the "
                        + "client about the fact that a specified preference was applied") }))
    @POST
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response create(
            @NotNull UserTO userTO,
            @DefaultValue("true") @QueryParam("storePassword") boolean storePassword);

    /**
     * Updates user matching the provided key.
     *
     * @param userPatch modification to be applied to user matching the provided key
     * @return Response object featuring the updated user enriched with propagation status information
     */
    @ApiImplicitParams({
        @ApiImplicitParam(name = RESTHeaders.PREFER, paramType = "header", dataType = "string",
                value = "Allows the client to specify a preference for the result to be returned from the server",
                defaultValue = "return-content", allowableValues = "return-content, return-no-content",
                allowEmptyValue = true)
        , @ApiImplicitParam(name = HttpHeaders.IF_MATCH, paramType = "header", dataType = "string",
                value = "When the provided ETag value does not match the latest modification date of the entity, "
                + "an error is reported and the requested operation is not performed.",
                allowEmptyValue = true)
        , @ApiImplicitParam(name = RESTHeaders.NULL_PRIORITY_ASYNC, paramType = "header", dataType = "boolean",
                value = "If 'true', instructs the propagation process not to wait for completion when communicating"
                + " with External Resources with no priority set",
                defaultValue = "false", allowEmptyValue = true) })
    @ApiResponses({
        @ApiResponse(code = 200,
                message = "User successfully updated enriched with propagation status information, as Entity",
                response = ProvisioningResult.class)
        , @ApiResponse(code = 204,
                message = "No content if 'Prefer: return-no-content' was specified", responseHeaders =
                @ResponseHeader(name = RESTHeaders.PREFERENCE_APPLIED, response = String.class,
                        description = "Allows the server to inform the "
                        + "client about the fact that a specified preference was applied"))
        , @ApiResponse(code = 412,
                message = "The ETag value provided via the 'If-Match' header does not match the latest modification "
                + "date of the entity") })
    @PATCH
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response update(@NotNull UserPatch userPatch);

    /**
     * Updates user matching the provided key.
     *
     * @param userTO complete update
     * @return Response object featuring the updated user enriched with propagation status information
     */
    @ApiImplicitParams({
        @ApiImplicitParam(name = RESTHeaders.PREFER, paramType = "header", dataType = "string",
                value = "Allows the client to specify a preference for the result to be returned from the server",
                defaultValue = "return-content", allowableValues = "return-content, return-no-content",
                allowEmptyValue = true)
        , @ApiImplicitParam(name = HttpHeaders.IF_MATCH, paramType = "header", dataType = "string",
                value = "When the provided ETag value does not match the latest modification date of the entity, "
                + "an error is reported and the requested operation is not performed.",
                allowEmptyValue = true)
        , @ApiImplicitParam(name = RESTHeaders.NULL_PRIORITY_ASYNC, paramType = "header", dataType = "boolean",
                value = "If 'true', instructs the propagation process not to wait for completion when communicating"
                + " with External Resources with no priority set",
                defaultValue = "false", allowEmptyValue = true) })
    @ApiResponses({
        @ApiResponse(code = 200,
                message = "User successfully updated enriched with propagation status information, as Entity",
                response = ProvisioningResult.class)
        , @ApiResponse(code = 204,
                message = "No content if 'Prefer: return-no-content' was specified", responseHeaders =
                @ResponseHeader(name = RESTHeaders.PREFERENCE_APPLIED, response = String.class,
                        description = "Allows the server to inform the "
                        + "client about the fact that a specified preference was applied"))
        , @ApiResponse(code = 412,
                message = "The ETag value provided via the 'If-Match' header does not match the latest modification "
                + "date of the entity") })
    @PUT
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response update(@NotNull UserTO userTO);

    /**
     * Performs a status update on given user.
     *
     * @param statusPatch status update details
     * @return Response object featuring the updated user enriched with propagation status information
     */
    @ApiImplicitParams({
        @ApiImplicitParam(name = RESTHeaders.PREFER, paramType = "header", dataType = "string",
                value = "Allows the client to specify a preference for the result to be returned from the server",
                defaultValue = "return-content", allowableValues = "return-content, return-no-content",
                allowEmptyValue = true)
        , @ApiImplicitParam(name = HttpHeaders.IF_MATCH, paramType = "header", dataType = "string",
                value = "When the provided ETag value does not match the latest modification date of the entity, "
                + "an error is reported and the requested operation is not performed.",
                allowEmptyValue = true)
        , @ApiImplicitParam(name = RESTHeaders.NULL_PRIORITY_ASYNC, paramType = "header", dataType = "boolean",
                value = "If 'true', instructs the propagation process not to wait for completion when communicating"
                + " with External Resources with no priority set",
                defaultValue = "false", allowEmptyValue = true) })
    @ApiResponses({
        @ApiResponse(code = 200,
                message = "User successfully updated enriched with propagation status information, as Entity",
                response = ProvisioningResult.class)
        , @ApiResponse(code = 204,
                message = "No content if 'Prefer: return-no-content' was specified", responseHeaders =
                @ResponseHeader(name = RESTHeaders.PREFERENCE_APPLIED, response = String.class,
                        description = "Allows the server to inform the "
                        + "client about the fact that a specified preference was applied"))
        , @ApiResponse(code = 412,
                message = "The ETag value provided via the 'If-Match' header does not match the latest modification "
                + "date of the entity") })
    @POST
    @Path("{key}/status")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response status(@NotNull StatusPatch statusPatch);
}

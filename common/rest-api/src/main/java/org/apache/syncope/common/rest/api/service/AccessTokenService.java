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
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.ResponseHeader;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AccessTokenTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.AccessTokenQuery;

/**
 * REST operations for access tokens.
 */
@Api(tags = "AccessTokens")
@Path("accessTokens")
public interface AccessTokenService extends JAXRSService {

    /**
     * Returns an empty response bearing the X-Syncope-Token header value, in case of successful authentication.
     * The provided value is a signed JSON Web Token.
     *
     * @return empty response bearing the X-Syncope-Token header value, in case of successful authentication
     */
    @ApiOperation(value = "", authorizations = {
        @Authorization(value = "BasicAuthentication") })
    @ApiResponses({
        @ApiResponse(code = 204,
                message = "JWT successfully generated", responseHeaders = {
                    @ResponseHeader(name = RESTHeaders.TOKEN, response = String.class,
                            description = "Generated JWT"),
                    @ResponseHeader(name = RESTHeaders.TOKEN_EXPIRE, response = String.class,
                            description = "Expiration of the generated JWT") }),
        @ApiResponse(code = 401, message = "Invalid username or password") })
    @POST
    @Path("login")
    @Produces({ MediaType.APPLICATION_JSON, SyncopeConstants.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Response login();

    /**
     * Returns an empty response bearing the X-Syncope-Token header value, with extended lifetime.
     * The provided value is a signed JSON Web Token.
     *
     * @return an empty response bearing the X-Syncope-Token header value, with extended lifetime
     */
    @ApiOperation(value = "", authorizations = {
        @Authorization(value = "Bearer") })
    @ApiResponses(
            @ApiResponse(code = 204,
                    message = "JWT successfully refreshed", responseHeaders = {
                @ResponseHeader(name = RESTHeaders.TOKEN, response = String.class,
                        description = "Generated JWT"),
                @ResponseHeader(name = RESTHeaders.TOKEN_EXPIRE, response = String.class,
                        description = "Expiration of the refreshed JWT") }))
    @POST
    @Path("refresh")
    @Produces({ MediaType.APPLICATION_JSON, SyncopeConstants.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Response refresh();

    /**
     * Invalidates the access token of the requesting user.
     */
    @ApiOperation(value = "", authorizations = {
        @Authorization(value = "Bearer") })
    @ApiResponses(
            @ApiResponse(code = 204, message = "Operation was successful"))
    @POST
    @Path("logout")
    @Produces({ MediaType.APPLICATION_JSON, SyncopeConstants.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void logout();

    /**
     * Returns a paged list of existing access tokens matching the given query.
     *
     * @param query query conditions
     * @return paged list of existing access tokens matching the given query
     */
    @ApiOperation(value = "", authorizations = {
        @Authorization(value = "BasicAuthentication"),
        @Authorization(value = "Bearer") })
    @GET
    @Consumes({ MediaType.APPLICATION_JSON, SyncopeConstants.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, SyncopeConstants.APPLICATION_YAML, MediaType.APPLICATION_XML })
    PagedResult<AccessTokenTO> list(@BeanParam AccessTokenQuery query);

    /**
     * Invalidates the access token matching the provided key.
     *
     * @param key access token key
     */
    @ApiOperation(value = "", authorizations = {
        @Authorization(value = "BasicAuthentication"),
        @Authorization(value = "Bearer") })
    @ApiResponses(
            @ApiResponse(code = 204, message = "Operation was successful"))
    @DELETE
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, SyncopeConstants.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void delete(@PathParam("key") String key);
}

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

package org.apache.syncope.common.rest.api.service.wa;

import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.syncope.common.lib.to.OIDCJWKSTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.JAXRSService;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Tag(name = "OIDC Json Web Keystore")
@SecurityRequirements({
    @SecurityRequirement(name = "BasicAuthentication"),
    @SecurityRequirement(name = "Bearer")})
@Path("wa/oidc/jwks")
public interface OIDCJWKSService extends JAXRSService {

    @GET
    @Produces({MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML})
    OIDCJWKSTO get();

    @ApiResponses({
        @ApiResponse(responseCode = "201",
            description = "JWKS successfully created", headers = {
            @Header(name = RESTHeaders.RESOURCE_KEY, schema =
            @Schema(type = "string"),
                description = "UUID generated for the entity created"),
            @Header(name = HttpHeaders.LOCATION, schema =
            @Schema(type = "string"),
                description = "URL of the entity created")}),
        @ApiResponse(responseCode = "409",
            description = "JWKS already exists")})
    @POST
    @Consumes({MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML})
    Response set();
}

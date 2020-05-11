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
import org.apache.syncope.common.lib.to.SAML2SPKeystoreTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.JAXRSService;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST operations for SAML 2.0 IdP keystore.
 */
@Tag(name = "SAML 2.0 SP Metadata Keystore")
@SecurityRequirements({
    @SecurityRequirement(name = "BasicAuthentication"),
    @SecurityRequirement(name = "Bearer") })
@Path("wa/saml2sp/keystore")
public interface SAML2SPKeystoreService extends JAXRSService {

    /**
     * Returns a document outlining keystore for Syncope as SAML 2.0 SP.
     *
     * @param name indicates the SAML 2.0 SP keystore document owner.
     * @return SAML 2.0 SP keystore
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    SAML2SPKeystoreTO getByOwner(@QueryParam("name") String name);

    /**
     * Returns the SAML 2.0 SP keystore matching the given key.
     *
     * @param key key of requested SAML 2.0 SP keystore
     * @return SAML 2.0 SP keystore with matching id
     */
    @GET
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    SAML2SPKeystoreTO read(@NotNull @PathParam("key") String key);

    /**
     * Store the keystore to finalize the keystore generation process.
     *
     * @param keystoreTO SAML2SPMetadataKeystoreTO to be created
     * @return Response object featuring Location header of created SAML 2.0 SP keystore
     */
    @ApiResponses({
        @ApiResponse(responseCode = "201",
            description = "SAML2SPKeystoreTO successfully created", headers = {
            @Header(name = RESTHeaders.RESOURCE_KEY, schema =
            @Schema(type = "string"),
                description = "UUID generated for the entity created"),
            @Header(name = HttpHeaders.LOCATION, schema =
            @Schema(type = "string"),
                description = "URL of the entity created") }),
        @ApiResponse(responseCode = "409",
            description = "Metadata already existing") })
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Response set(@NotNull SAML2SPKeystoreTO keystoreTO);

}

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
import org.apache.syncope.common.lib.to.SAML2SPMetadataTO;
import org.apache.syncope.common.rest.api.RESTHeaders;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * REST operations for SAML 2.0 SP metadata.
 */
@Tag(name = "SAML 2.0 SP Metadata")
@SecurityRequirements({
    @SecurityRequirement(name = "BasicAuthentication"),
    @SecurityRequirement(name = "Bearer")})
@Path("saml2sp/conf/metadata")
public interface SAML2SPMetadataConfService extends JAXRSService {

    /**
     * Updates SAML 2.0 SP metadata matching the given key.
     *
     * @param metadataTO SAML2SPMetadata to replace existing SAML 2.0 SP metadata
     */
    @Parameter(name = "key", description = "SAML2SPMetadata's key", in = ParameterIn.PATH, schema =
    @Schema(type = "string"))
    @ApiResponses(
        @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @PUT
    @Path("{key}")
    @Consumes({MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML})
    void update(@NotNull SAML2SPMetadataTO metadataTO);

}

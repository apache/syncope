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
import java.io.InputStream;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.SAML2SP4UIIdPTO;
import org.apache.syncope.common.rest.api.RESTHeaders;

/**
 * REST operations for SAML 2.0 SP4UI Identity Providers.
 */
@Tag(name = "SAML 2.0 SP4UI")
@SecurityRequirements({
    @SecurityRequirement(name = "BasicAuthentication"),
    @SecurityRequirement(name = "Bearer") })
@Path("saml2sp4ui/identityProviders")
public interface SAML2SP4UIIdPService extends JAXRSService {

    /**
     * Returns a list of all defined SAML 2.0 Identity Providers.
     *
     * @return list of all defined SAML 2.0 Identity Providers
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    List<SAML2SP4UIIdPTO> list();

    /**
     * Returns the SAML 2.0 Identity Provider with matching entityID, if available.
     *
     * @param key SAML 2.0 Identity Provider's entityID
     * @return SAML 2.0 Identity Provider with matching entityID, if available
     */
    @GET
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    SAML2SP4UIIdPTO read(@PathParam("key") String key);

    /**
     * Imports the SAML 2.0 Identity Provider definitions available in the provided XML metadata.
     *
     * @param input XML metadata
     * @return the entityID values for all imported SAML 2.0 Identity Providers
     */
    @POST
    @Consumes({ MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    Response importFromMetadata(@NotNull InputStream input);

    /**
     * Updates the SAML 2.0 Identity Provider with matching entityID.
     *
     * @param saml2IdpTO idp configuration to be stored
     */
    @Parameter(name = "key", description = "IdP's key", in = ParameterIn.PATH, schema =
            @Schema(type = "string"))
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @PUT
    @Path("{key}")
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void update(@NotNull SAML2SP4UIIdPTO saml2IdpTO);

    /**
     * Deletes the SAML 2.0 Identity Provider with matching entityID.
     *
     * @param key SAML 2.0 Identity Provider's entityID
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @DELETE
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void delete(@PathParam("key") String key);
}

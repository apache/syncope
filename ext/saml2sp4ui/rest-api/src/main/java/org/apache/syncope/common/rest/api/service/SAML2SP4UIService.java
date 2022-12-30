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

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.syncope.common.lib.saml2.SAML2LoginResponse;
import org.apache.syncope.common.lib.saml2.SAML2Request;
import org.apache.syncope.common.lib.saml2.SAML2Response;
import org.apache.syncope.common.rest.api.RESTHeaders;

/**
 * REST operations for the provided SAML 2.0 SP4UI Service Provider.
 */
@Tag(name = "SAML 2.0 SP4UI")
@SecurityRequirements({
    @SecurityRequirement(name = "BasicAuthentication"),
    @SecurityRequirement(name = "Bearer") })
@Path("saml2sp4ui/serviceProvider")
public interface SAML2SP4UIService extends JAXRSService {

    /**
     * Returns the XML metadata for the provided SAML 2.0 Service Provider.
     *
     * @param spEntityID SAML 2.0 SP entity ID.
     * @param urlContext SAML 2.0 SP agent URL context
     * @return XML metadata for the provided SAML 2.0 Service Provider
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML })
    Response getMetadata(@QueryParam("spEntityID") String spEntityID, @QueryParam("urlContext") String urlContext);

    /**
     * Generates SAML 2.0 authentication request for the IdP matching the provided entity ID.
     *
     * @param spEntityID SAML 2.0 SP entity ID.
     * @param urlContext SAML 2.0 SP agent URL context
     * @param idpEntityID SAML 2.0 IdP entity ID.
     * @return SAML 2.0 authentication request
     */
    @POST
    @Path("loginRequest")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    SAML2Request createLoginRequest(
            @QueryParam("spEntityID") String spEntityID,
            @QueryParam("urlContext") String urlContext,
            @QueryParam("idpEntityID") String idpEntityID);

    /**
     * Validates the received SAML 2.0 authentication response and creates JWT for the matching user, if found.
     *
     * @param response SAML response and relay state
     * @return JWT for the matching user plus attributes returned in the response
     */
    @POST
    @Path("loginResponse")
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    SAML2LoginResponse validateLoginResponse(SAML2Response response);

    /**
     * Generates SAML 2.0 logout request for the IdP matching the requesting access token.
     *
     * @param spEntityID SAML 2.0 SP entity ID.
     * @param urlContext SAML 2.0 SP agent URL context
     * @return SAML 2.0 logout request
     */
    @POST
    @Path("logoutRequest")
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    SAML2Request createLogoutRequest(
            @QueryParam("spEntityID") String spEntityID, @QueryParam("urlContext") String urlContext);

    /**
     * Validates the received SAML 2.0 logout response.
     *
     * @param response SAML response and relay state
     */
    @ApiResponses(
            @ApiResponse(responseCode = "204", description = "Operation was successful"))
    @POST
    @Path("logoutResponse")
    @Consumes({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, RESTHeaders.APPLICATION_YAML, MediaType.APPLICATION_XML })
    void validateLogoutResponse(SAML2Response response);
}

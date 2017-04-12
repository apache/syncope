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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.SAML2RequestTO;
import org.apache.syncope.common.lib.to.SAML2LoginResponseTO;
import org.apache.syncope.common.lib.to.SAML2ReceivedResponseTO;

/**
 * REST operations for the provided SAML 2.0 Service Provider.
 */
@Path("saml2sp/serviceProvider")
public interface SAML2SPService extends JAXRSService {

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
     * @param idpEntityID SAML 2.0 IdP entity ID.
     * @return SAML 2.0 authentication request
     */
    @POST
    @Path("loginRequest")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    SAML2RequestTO createLoginRequest(
            @QueryParam("spEntityID") String spEntityID,
            @QueryParam("idpEntityID") String idpEntityID);

    /**
     * Validates the received SAML 2.0 authentication response and creates JWT for the matching user, if found.
     *
     * @param response SAML response and relay state
     * @return JWT for the matching user plus attributes returned in the response
     */
    @POST
    @Path("loginResponse")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    SAML2LoginResponseTO validateLoginResponse(SAML2ReceivedResponseTO response);

    /**
     * Generates SAML 2.0 logout request for the IdP matching the requesting access token.
     *
     * @param spEntityID SAML 2.0 SP entity ID.
     * @return SAML 2.0 logout request
     */
    @POST
    @Path("logoutRequest")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    SAML2RequestTO createLogoutRequest(@QueryParam("spEntityID") String spEntityID);

    /**
     * Validates the received SAML 2.0 logout response.
     *
     * @param response SAML response and relay state
     */
    @POST
    @Path("logoutResponse")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    void validateLogoutResponse(SAML2ReceivedResponseTO response);
}

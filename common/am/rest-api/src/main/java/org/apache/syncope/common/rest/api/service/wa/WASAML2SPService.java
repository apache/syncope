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

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import org.apache.syncope.common.rest.api.service.JAXRSService;

/**
 * REST operations for WA to work with SAML2SP clients.
 */
@Tag(name = "WA")
@SecurityRequirements({
    @SecurityRequirement(name = "BasicAuthentication"),
    @SecurityRequirement(name = "Bearer") })
@Path("wa/saml2sp")
public interface WASAML2SPService extends JAXRSService {

    @GET
    @Path("{clientName}/keystore")
    Response getSAML2SPKeystore(@NotNull @PathParam("clientName") String clientName);

    @PUT
    @Path("{clientName}/keystore")
    void setSAML2SPKeystore(@NotNull @PathParam("clientName") String clientName, InputStream keystore);

    @GET
    @Path("{clientName}/metadata")
    Response getSAML2SPMetadata(@NotNull @PathParam("clientName") String clientName);

    @PUT
    @Path("{clientName}/metadata")
    void setSAML2SPMetadata(@NotNull @PathParam("clientName") String clientName, InputStream metadata);
}

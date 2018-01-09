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

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.patch.AnyObjectPatch;
import org.apache.syncope.common.lib.to.AnyObjectTO;

/**
 * REST operations for anyObjects.
 */
@Path("anyObjects")
public interface AnyObjectService extends AnyService<AnyObjectTO> {

    /**
     * Creates a new any object.
     *
     * @param anyObjectTO any object to be created
     * @return Response object featuring Location header of created any object as well as the any
     * object itself enriched with propagation status information - ProvisioningResult as Entity
     */
    @POST
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response create(@NotNull AnyObjectTO anyObjectTO);

    /**
     * Updates any object matching the provided key.
     *
     * @param anyObjectPatch modification to be applied to any object matching the provided key
     * @return Response object featuring the updated any object enriched with propagation status information
     * - ProvisioningResult as Entity
     */
    @PATCH
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response update(@NotNull AnyObjectPatch anyObjectPatch);

    /**
     * Updates any object matching the provided key.
     *
     * @param anyObjectTO complete update
     * @return Response object featuring the updated any object enriched with propagation status information
     * - ProvisioningResult as Entity
     */
    @PUT
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response update(@NotNull AnyObjectTO anyObjectTO);
}

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
import org.apache.cxf.jaxrs.model.wadl.Description;
import org.apache.cxf.jaxrs.model.wadl.Descriptions;
import org.apache.cxf.jaxrs.model.wadl.DocTarget;
import org.apache.syncope.common.lib.to.RoleTO;

/**
 * REST operations for roles.
 */
@Path("roles")
public interface RoleService extends JAXRSService {

    /**
     * Returns a list of all roles.
     *
     * @return list of all roles.
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    List<RoleTO> list();

    /**
     * Returns role with matching id.
     *
     * @param roleKey role id to be read
     * @return role with matching id
     */
    @GET
    @Path("{roleKey}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    RoleTO read(@NotNull @PathParam("roleKey") Long roleKey);

    /**
     * Creates a new role.
     *
     * @param roleTO role to be created
     * @return <tt>Response</tt> object featuring <tt>Location</tt> header of created role
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring <tt>Location</tt> header of created role")
    })
    @POST
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response create(@NotNull RoleTO roleTO);

    /**
     * Updates the role matching the provided id.
     *
     * @param roleKey role id to be updated
     * @param roleTO role to be stored
     */
    @PUT
    @Path("{roleKey}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    void update(@NotNull @PathParam("roleKey") Long roleKey, @NotNull RoleTO roleTO);

    /**
     * Deletes the role matching the provided id.
     *
     * @param roleKey role id to be deleted
     */
    @DELETE
    @Path("{roleKey}")
    void delete(@NotNull @PathParam("roleKey") Long roleKey);
}

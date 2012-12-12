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
package org.apache.syncope.services;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.syncope.NotFoundException;
import org.apache.syncope.mod.RoleMod;
import org.apache.syncope.to.RoleTO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

@Path("/roles")
public interface RoleService {

    @GET
    @Path("/{roleId}")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public RoleTO read(@PathParam("roleId") final Long roleId) throws NotFoundException,
            UnauthorizedRoleException;

    @GET
    @Path("/{roleId}/parent")
    @PreAuthorize("hasRole('ROLE_READ')")
    @Transactional(readOnly = true)
    public RoleTO parent(@PathParam("roleId") final Long roleId) throws NotFoundException,
            UnauthorizedRoleException;

    @GET
    @Path("/{roleId}/children")
    @PreAuthorize("hasRole('ROLE_READ')")
    @Transactional(readOnly = true)
    public List<RoleTO> children(@PathParam("roleId") final Long roleId) throws NotFoundException;

    @GET
    @PreAuthorize("hasRole('ROLE_READ')")
    @Transactional(readOnly = true)
    public List<RoleTO> list();

    @POST
    @Path("/")
    @PreAuthorize("hasRole('ROLE_CREATE')")
    public Response create(final RoleTO roleTO)
            throws UnauthorizedRoleException;

    @POST
    @Path("/{roleId}")
    @PreAuthorize("hasRole('ROLE_UPDATE')")
    public RoleTO update(@PathParam("roleId") final Long roleId, final RoleMod roleMod)
            throws NotFoundException, UnauthorizedRoleException;

    @DELETE
    @Path("/{roleId}")
    @PreAuthorize("hasRole('ROLE_DELETE')")
    public Response delete(@PathParam("roleId") final Long roleId) throws NotFoundException,
            UnauthorizedRoleException;

}
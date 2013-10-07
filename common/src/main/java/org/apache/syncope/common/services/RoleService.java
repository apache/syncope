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
package org.apache.syncope.common.services;

import java.util.List;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.syncope.common.search.NodeCond;
import org.apache.syncope.common.mod.RoleMod;
import org.apache.syncope.common.to.PropagationTargetsTO;
import org.apache.syncope.common.to.RoleTO;

@Path("roles")
public interface RoleService {

    /**
     * @param roleId ID of role to get children from
     * @return Returns list of children for selected role
     */
    @GET
    @Path("{roleId}/children")
    List<RoleTO> children(@PathParam("roleId") Long roleId);

    /**
     * @return Returns number of known roles. (size of list)
     */
    @GET
    @Path("count")
    int count();

    /**
     * @param roleTO Role to be created
     * @return Response containing URI location for created role, as well as the role itself enriched with propagation
     * status information
     */
    @POST
    Response create(RoleTO roleTO);

    /**
     * @param roleId ID of role to be deleted
     * @return Returns deleted role, enriched with propagation status information
     */
    @DELETE
    @Path("{roleId}")
    RoleTO delete(@PathParam("roleId") Long roleId);

    /**
     * @return Returns list of all knwon roles
     */
    @GET
    List<RoleTO> list();

    /**
     * @param page Page of roles in relation to size parameter
     * @param size Number of roles to be displayed per page
     * @return Returns paginated list of roles
     */
    @GET
    List<RoleTO> list(@QueryParam("page") int page, @QueryParam("size") @DefaultValue("25") int size);

    /**
     * @param roleId Id of role to get parent role from
     * @return Returns parent role or null if no parent exists
     */
    @GET
    @Path("{roleId}/parent")
    RoleTO parent(@PathParam("roleId") Long roleId);

    /**
     * @param roleId ID of role to be read
     * @return Returns role with matching id
     */
    @GET
    @Path("{roleId}")
    RoleTO read(@PathParam("roleId") Long roleId);

    /**
     * @param searchCondition Filter condition for role list
     * @return Returns list of roles with matching filter conditions
     * @throws InvalidSearchConditionException
     */
    @POST
    @Path("search")
    List<RoleTO> search(NodeCond searchCondition) throws InvalidSearchConditionException;

    /**
     * @param searchCondition Filter condition for role list
     * @param page Page of roles in relation to size parameter
     * @param size Number of roles to be displayed per page
     * @return Returns paginated list of roles with matching filter conditions
     * @throws InvalidSearchConditionException
     */
    @POST
    @Path("search")
    List<RoleTO> search(NodeCond searchCondition, @QueryParam("page") int page,
            @QueryParam("size") @DefaultValue("25") int size) throws InvalidSearchConditionException;

    /**
     * @param searchCondition Filter condition for role list
     * @return Returns number of roles matching provided filter conditions
     * @throws InvalidSearchConditionException
     */
    @POST
    @Path("search/count")
    int searchCount(NodeCond searchCondition) throws InvalidSearchConditionException;

    /**
     * This method is similar to {@link #read(Long)}, but uses different authentication handling to ensure that a user
     * can read his own roles.
     *
     * @param roleId ID of role to be read
     * @return Returns role with matching id
     */
    @GET
    @Path("{roleId}/own")
    RoleTO selfRead(@PathParam("roleId") Long roleId);

    /**
     * @param roleId ID of role to be updated
     * @param roleMod Role object containing list of changes to be applied for selected role
     * @return Returns updated role, merged from existing role and provided roleMod
     */
    @POST
    @Path("{roleId}")
    RoleTO update(@PathParam("roleId") Long roleId, RoleMod roleMod);

    /**
     * Unlinks role and the given external resources specified by <tt>propagationTargetsTO</tt> parameter.
     *
     * @param roleId role id.
     * @param propagationTargetsTO resource names.
     * @return updated role.
     */
    @POST
    @Path("{roleId}/unlink")
    RoleTO unlink(@PathParam("roleId") Long roleId, PropagationTargetsTO propagationTargetsTO);

    /**
     * Unassigns resources to the given role (performs unlink + de-provision).
     *
     * @param roleId role id.
     * @param propagationTargetsTO resources to be unassigned.
     * @return updated role.
     */
    @POST
    @Path("{roleId}/unassign")
    RoleTO unassign(@PathParam("roleId") Long roleId, PropagationTargetsTO propagationTargetsTO);

    /**
     * De-provision role from the given resources without unlinking.
     *
     * @param roleId role id of the role to be de-provisioned.
     * @param propagationTargetsTO resource names.
     * @return updated role.
     */
    @POST
    @Path("{roleId}/deprovision")
    RoleTO deprovision(@PathParam("roleId") Long roleId, PropagationTargetsTO propagationTargetsTO);
}
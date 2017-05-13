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
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.model.wadl.Description;
import org.apache.cxf.jaxrs.model.wadl.Descriptions;
import org.apache.cxf.jaxrs.model.wadl.DocTarget;
import org.apache.syncope.common.reqres.PagedResult;
import org.apache.syncope.common.mod.RoleMod;
import org.apache.syncope.common.reqres.BulkAction;
import org.apache.syncope.common.reqres.BulkActionResult;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.TaskExecTO;
import org.apache.syncope.common.types.ResourceAssociationActionType;
import org.apache.syncope.common.types.ResourceDeassociationActionType;
import org.apache.syncope.common.wrap.ResourceName;

/**
 * REST operations for roles.
 */
@Path("roles")
public interface RoleService extends JAXRSService {

    /**
     * Returns children roles of given role.
     *
     * @param roleId id of role to get children from
     * @param details whether include all details or not, defaults to true
     * @return children roles of given role
     */
    @GET
    @Path("{roleId}/children")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    List<RoleTO> children(@NotNull @PathParam("roleId") Long roleId,
            @QueryParam(PARAM_DETAILS) @DefaultValue("true") boolean details);

    /**
     * Returns parent role of the given role (or null if no parent exists).
     *
     * @param roleId id of role to get parent role from
     * @return parent role of the given role (or null if no parent exists)
     */
    @GET
    @Path("{roleId}/parent")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    RoleTO parent(@NotNull @PathParam("roleId") Long roleId);

    /**
     * Reads the role matching the provided roleId.
     *
     * @param roleId id of role to be read
     * @return role with matching id
     */
    @GET
    @Path("{roleId}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    RoleTO read(@NotNull @PathParam("roleId") Long roleId);

    /**
     * This method is similar to {@link #read(Long)}, but uses different authentication handling to ensure that a user
     * can read his own roles.
     *
     * @param roleId id of role to be read
     * @return role with matching id
     */
    @Descriptions({
        @Description(target = DocTarget.METHOD,
                value = "This method is similar to <tt>read()</tt>, but uses different authentication handling to "
                + "ensure that a user can read his own roles.")
    })
    @GET
    @Path("{roleId}/own")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    RoleTO readSelf(@NotNull @PathParam("roleId") Long roleId);

    /**
     * Returns a paged list of existing roles.
     *
     * @return paged list of all existing roles
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    PagedResult<RoleTO> list();

    /**
     * Returns a paged list of existing roles.
     *
     * @param orderBy list of ordering clauses, separated by comma
     * @return paged list of all existing roles
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    PagedResult<RoleTO> list(@QueryParam(PARAM_ORDERBY) String orderBy);

    /**
     * Returns a paged list of existing roles matching page/size conditions.
     *
     * @param page result page number
     * @param size number of entries per page
     * @return paged list of existing roles matching page/size conditions
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    PagedResult<RoleTO> list(
            @NotNull @Min(1) @QueryParam(PARAM_PAGE) @DefaultValue(DEFAULT_PARAM_PAGE) Integer page,
            @NotNull @Min(1) @QueryParam(PARAM_SIZE) @DefaultValue(DEFAULT_PARAM_SIZE) Integer size);

    /**
     * Returns a paged list of existing roles matching page/size conditions.
     *
     * @param page result page number
     * @param size number of entries per page
     * @param orderBy list of ordering clauses, separated by comma
     * @param details whether include all details or not, defaults to true
     * @return paged list of existing roles matching page/size conditions
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    PagedResult<RoleTO> list(
            @NotNull @Min(1) @QueryParam(PARAM_PAGE) @DefaultValue(DEFAULT_PARAM_PAGE) Integer page,
            @NotNull @Min(1) @QueryParam(PARAM_SIZE) @DefaultValue(DEFAULT_PARAM_SIZE) Integer size,
            @QueryParam(PARAM_ORDERBY) String orderBy,
            @QueryParam(PARAM_DETAILS) @DefaultValue("true") boolean details);

    /**
     * Returns a paged list of roles matching the provided FIQL search condition.
     *
     * @param fiql FIQL search expression
     * @return paged list of roles matching the provided FIQL search condition
     */
    @GET
    @Path("search")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    PagedResult<RoleTO> search(@NotNull @QueryParam(PARAM_FIQL) String fiql);

    /**
     * Returns a paged list of roles matching the provided FIQL search condition.
     *
     * @param fiql FIQL search expression
     * @param orderBy list of ordering clauses, separated by comma
     * @return paged list of roles matching the provided FIQL search condition
     */
    @GET
    @Path("search")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    PagedResult<RoleTO> search(
            @NotNull @QueryParam(PARAM_FIQL) String fiql, @QueryParam(PARAM_ORDERBY) String orderBy);

    /**
     * Returns a paged list of roles matching the provided FIQL search condition.
     *
     * @param fiql FIQL search expression
     * @param page result page number
     * @param size number of entries per page
     * @return paged list of roles matching the provided FIQL search condition
     */
    @GET
    @Path("search")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    PagedResult<RoleTO> search(@QueryParam(PARAM_FIQL) String fiql,
            @NotNull @Min(1) @QueryParam(PARAM_PAGE) @DefaultValue(DEFAULT_PARAM_PAGE) Integer page,
            @NotNull @Min(1) @QueryParam(PARAM_SIZE) @DefaultValue(DEFAULT_PARAM_SIZE) Integer size);

    /**
     * Returns a paged list of roles matching the provided FIQL search condition.
     *
     * @param fiql FIQL search expression
     * @param page result page number
     * @param size number of entries per page
     * @param orderBy list of ordering clauses, separated by comma
     * @param details whether include all details or not, defaults to true
     * @return paged list of roles matching the provided FIQL search condition
     */
    @GET
    @Path("search")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    PagedResult<RoleTO> search(@QueryParam(PARAM_FIQL) String fiql,
            @NotNull @Min(1) @QueryParam(PARAM_PAGE) @DefaultValue(DEFAULT_PARAM_PAGE) Integer page,
            @NotNull @Min(1) @QueryParam(PARAM_SIZE) @DefaultValue(DEFAULT_PARAM_SIZE) Integer size,
            @QueryParam(PARAM_ORDERBY) String orderBy,
            @QueryParam(PARAM_DETAILS) @DefaultValue("true") boolean details);

    /**
     * Creates a new role.
     *
     * @param roleTO role to be created
     * @return <tt>Response</tt> object featuring <tt>Location</tt> header of created role as well as the role itself
     * enriched with propagation status information - {@link RoleTO} as <tt>Entity</tt>
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring <tt>Location</tt> header of created role as well as the "
                + "role itself enriched with propagation status information - <tt>RoleTO</tt> as <tt>Entity</tt>")
    })
    @POST
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response create(@NotNull RoleTO roleTO);

    /**
     * Updates role matching the provided roleId.
     *
     * @param roleId id of role to be updated
     * @param roleMod modification to be applied to role matching the provided roleId
     * @return <tt>Response</tt> object featuring the updated role enriched with propagation status information
     * - {@link RoleTO} as <tt>Entity</tt>
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring the updated role enriched with propagation status information - "
                + "<tt>RoleTO</tt> as <tt>Entity</tt>")
    })
    @POST
    @Path("{roleId}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response update(@NotNull @PathParam("roleId") Long roleId, @NotNull RoleMod roleMod);

    /**
     * Deletes role matching provided roleId.
     *
     * @param roleId id of role to be deleted
     * @return <tt>Response</tt> object featuring the deleted role enriched with propagation status information
     * - {@link RoleTO} as <tt>Entity</tt>
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring the deleted role enriched with propagation status information - "
                + "<tt>RoleTO</tt> as <tt>Entity</tt>")
    })
    @DELETE
    @Path("{roleId}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response delete(@NotNull @PathParam("roleId") Long roleId);

    /**
     * Executes resource-related operations on given role.
     *
     * @param roleId role id.
     * @param type resource association action type
     * @param resourceNames external resources to be used for propagation-related operations
     * @return <tt>Response</tt> object featuring
     * {@link org.apache.syncope.common.reqres.BulkActionResult} as <tt>Entity</tt>
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring <tt>BulkActionResult</tt> as <tt>Entity</tt>")
    })
    @POST
    @Path("{roleId}/deassociate/{type}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response bulkDeassociation(@NotNull @PathParam("roleId") Long roleId,
            @NotNull @PathParam("type") ResourceDeassociationActionType type,
            @NotNull List<ResourceName> resourceNames);

    /**
     * Executes resource-related operations on given role.
     *
     * @param roleId role id.
     * @param type resource association action type
     * @param resourceNames external resources to be used for propagation-related operations
     * @return <tt>Response</tt> object featuring {@link org.apache.syncope.common.reqres.BulkActionResult}
     * as <tt>Entity</tt>
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring <tt>BulkActionResult</tt> as <tt>Entity</tt>")
    })
    @POST
    @Path("{roleId}/associate/{type}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response bulkAssociation(@NotNull @PathParam("roleId") Long roleId,
            @NotNull @PathParam("type") ResourceAssociationActionType type,
            @NotNull List<ResourceName> resourceNames);

    /**
     * Provision all members of the given role onto all the resources associated to it.
     *
     * @param roleId role id.
     * @return <tt>Response</tt> object featuring {@link org.apache.syncope.common.reqres.BulkActionResult}
     * as <tt>Entity</tt>
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring <tt>BulkActionResult</tt> as <tt>Entity</tt>")
    })
    @POST
    @Path("{roleId}/associate/PROVISION/members")
    TaskExecTO bulkProvisionMembers(@NotNull @PathParam("roleId") Long roleId);

    /**
     * Deprovision all members of the given role from all the resources associated to it.
     *
     * @param roleId role id.
     * @return <tt>Response</tt> object featuring {@link org.apache.syncope.common.reqres.BulkActionResult}
     * as <tt>Entity</tt>
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring <tt>BulkActionResult</tt> as <tt>Entity</tt>")
    })
    @POST
    @Path("{roleId}/associate/PROVISION/members")
    TaskExecTO bulkDeprovisionMembers(@NotNull @PathParam("roleId") Long roleId);

    /**
     * Executes the provided bulk action.
     *
     * @param bulkAction list of role ids against which the bulk action will be performed.
     * @return Bulk action result
     */
    @POST
    @Path("bulk")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    BulkActionResult bulk(@NotNull BulkAction bulkAction);

}

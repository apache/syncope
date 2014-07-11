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
import javax.ws.rs.OPTIONS;
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

import org.apache.syncope.common.mod.ResourceAssociationMod;
import org.apache.syncope.common.mod.StatusMod;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.wrap.ResourceName;
import org.apache.syncope.common.reqres.PagedResult;
import org.apache.syncope.common.reqres.BulkAction;
import org.apache.syncope.common.reqres.BulkActionResult;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.ResourceAssociationActionType;
import org.apache.syncope.common.types.ResourceDeassociationActionType;

/**
 * REST operations for users.
 */
@Path("users")
public interface UserService extends JAXRSService {

    /**
     * Gives the username for the provided user id.
     *
     * @param userId user id
     * @return <tt>Response</tt> object featuring HTTP header with username matching the given userId
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring HTTP header with username matching the given userId")
    })
    @OPTIONS
    @Path("{userId}/username")
    Response getUsername(@NotNull @PathParam("userId") Long userId);

    /**
     * Gives the user id for the provided username.
     *
     * @param username username
     * @return <tt>Response</tt> object featuring HTTP header with userId matching the given username
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring HTTP header with userId matching the given username")
    })
    @OPTIONS
    @Path("{username}/userId")
    Response getUserId(@NotNull @PathParam("username") String username);

    /**
     * Reads the user matching the provided userId.
     *
     * @param userId id of user to be read
     * @return User matching the provided userId
     */
    @GET
    @Path("{userId}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    UserTO read(@NotNull @PathParam("userId") Long userId);

    /**
     * Returns a paged list of existing users.
     *
     * @return paged list of all existing users
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    PagedResult<UserTO> list();

    /**
     * Returns a paged list of existing users.
     *
     * @param orderBy list of ordering clauses, separated by comma
     * @return paged list of all existing users
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    PagedResult<UserTO> list(@QueryParam(PARAM_ORDERBY) String orderBy);

    /**
     * Returns a paged list of existing users matching page/size conditions.
     *
     * @param page result page number
     * @param size number of entries per page
     * @return paged list of existing users matching page/size conditions
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    PagedResult<UserTO> list(
            @NotNull @Min(1) @QueryParam(PARAM_PAGE) @DefaultValue(DEFAULT_PARAM_PAGE) Integer page,
            @NotNull @Min(1) @QueryParam(PARAM_SIZE) @DefaultValue(DEFAULT_PARAM_SIZE) Integer size);

    /**
     * Returns a paged list of existing users matching page/size conditions.
     *
     * @param page result page number
     * @param size number of entries per page
     * @param orderBy list of ordering clauses, separated by comma
     * @return paged list of existing users matching page/size conditions
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    PagedResult<UserTO> list(
            @NotNull @Min(1) @QueryParam(PARAM_PAGE) @DefaultValue(DEFAULT_PARAM_PAGE) Integer page,
            @NotNull @Min(1) @QueryParam(PARAM_SIZE) @DefaultValue(DEFAULT_PARAM_SIZE) Integer size,
            @QueryParam(PARAM_ORDERBY) String orderBy);

    /**
     * Returns a paged list of users matching the provided FIQL search condition.
     *
     * @param fiql FIQL search expression
     * @return paged list of users matching the provided FIQL search condition
     */
    @GET
    @Path("search")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    PagedResult<UserTO> search(@NotNull @QueryParam(PARAM_FIQL) String fiql);

    /**
     * Returns a paged list of users matching the provided FIQL search condition.
     *
     * @param fiql FIQL search expression
     * @param orderBy list of ordering clauses, separated by comma
     * @return paged list of users matching the provided FIQL search condition
     */
    @GET
    @Path("search")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    PagedResult<UserTO> search(@NotNull @QueryParam(PARAM_FIQL) String fiql, @QueryParam(PARAM_ORDERBY) String orderBy);

    /**
     * Returns a paged list of users matching the provided FIQL search condition.
     *
     * @param fiql FIQL search expression
     * @param page result page number
     * @param size number of entries per page
     * @return paged list of users matching the provided FIQL search condition
     */
    @GET
    @Path("search")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    PagedResult<UserTO> search(@QueryParam(PARAM_FIQL) String fiql,
            @NotNull @Min(1) @QueryParam(PARAM_PAGE) @DefaultValue(DEFAULT_PARAM_PAGE) Integer page,
            @NotNull @Min(1) @QueryParam(PARAM_SIZE) @DefaultValue(DEFAULT_PARAM_SIZE) Integer size);

    /**
     * Returns a paged list of users matching the provided FIQL search condition.
     *
     * @param fiql FIQL search expression
     * @param page result page number
     * @param size number of entries per page
     * @param orderBy list of ordering clauses, separated by comma
     * @return paged list of users matching the provided FIQL search condition
     */
    @GET
    @Path("search")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    PagedResult<UserTO> search(@QueryParam(PARAM_FIQL) String fiql,
            @NotNull @Min(1) @QueryParam(PARAM_PAGE) @DefaultValue(DEFAULT_PARAM_PAGE) Integer page,
            @NotNull @Min(1) @QueryParam(PARAM_SIZE) @DefaultValue(DEFAULT_PARAM_SIZE) Integer size,
            @QueryParam(PARAM_ORDERBY) String orderBy);

    /**
     * Creates a new user.
     *
     * @param userTO user to be created
     * @param storePassword whether password shall be stored internally
     * @return <tt>Response</tt> object featuring <tt>Location</tt> header of created user as well as the user itself
     * enriched with propagation status information - {@link UserTO} as <tt>Entity</tt>
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring <tt>Location</tt> header of created user as well as the "
                + "user itself enriched with propagation status information - <tt>UserTO</tt> as <tt>Entity</tt>")
    })
    @POST
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response create(@NotNull UserTO userTO,
            @DefaultValue("true") @QueryParam("storePassword") boolean storePassword);

    /**
     * Updates user matching the provided userId.
     *
     * @param userId id of user to be updated
     * @param userMod modification to be applied to user matching the provided userId
     * @return <tt>Response</tt> object featuring the updated user enriched with propagation status information
     * - {@link UserTO} as <tt>Entity</tt>
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring the updated user enriched with propagation status information - "
                + "<tt>UserTO</tt> as <tt>Entity</tt>")
    })
    @POST
    @Path("{userId}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response update(@NotNull @PathParam("userId") Long userId, @NotNull UserMod userMod);

    /**
     * Performs a status update on user matching provided userId.
     *
     * @param userId id of user to be subjected to status update
     * @param statusMod status update details
     * @return <tt>Response</tt> object featuring the updated user enriched with propagation status information
     * - {@link UserTO} as <tt>Entity</tt>
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring the updated user enriched with propagation status information - "
                + "<tt>UserTO</tt> as <tt>Entity</tt>")
    })
    @POST
    @Path("{userId}/status")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response status(@NotNull @PathParam("userId") Long userId, @NotNull StatusMod statusMod);

    /**
     * Deletes user matching provided userId.
     *
     * @param userId id of user to be deleted
     * @return <tt>Response</tt> object featuring the deleted user enriched with propagation status information
     * - {@link UserTO} as <tt>Entity</tt>
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring the deleted user enriched with propagation status information - "
                + "<tt>UserTO</tt> as <tt>Entity</tt>")
    })
    @DELETE
    @Path("{userId}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response delete(@NotNull @PathParam("userId") Long userId);

    /**
     * Executes resource-related operations on given user.
     *
     * @param userId user id
     * @param type resource de-association action type
     * @param resourceNames external resources to be used for propagation-related operations
     * @return <tt>Response</tt> object featuring {@link BulkActionResult} as <tt>Entity</tt>
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring <tt>BulkActionResult</tt> as <tt>Entity</tt>")
    })
    @POST
    @Path("{userId}/bulkDeassociation/{type}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response bulkDeassociation(@NotNull @PathParam("userId") Long userId,
            @NotNull @PathParam("type") ResourceDeassociationActionType type,
            @NotNull List<ResourceName> resourceNames);

    /**
     * Executes resource-related operations on given user.
     *
     * @param userId user id.
     * @param type resource association action type
     * @param associationMod external resources to be used for propagation-related operations
     * @return <tt>Response</tt> object featuring {@link BulkActionResult} as <tt>Entity</tt>
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE, value = "Featuring <tt>BulkActionResult</tt> as <tt>Entity</tt>")
    })
    @POST
    @Path("{userId}/bulkAssociation/{type}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response bulkAssociation(@NotNull @PathParam("userId") Long userId,
            @NotNull @PathParam("type") ResourceAssociationActionType type,
            @NotNull ResourceAssociationMod associationMod);

    /**
     * Executes the provided bulk action.
     *
     * @param bulkAction list of user ids against which the bulk action will be performed.
     * @return Bulk action result
     */
    @POST
    @Path("bulk")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    BulkActionResult bulk(@NotNull BulkAction bulkAction);
}

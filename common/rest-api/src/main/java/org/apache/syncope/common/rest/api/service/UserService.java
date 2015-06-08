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
import javax.ws.rs.BeanParam;
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
import org.apache.syncope.common.lib.mod.ResourceAssociationMod;
import org.apache.syncope.common.lib.mod.StatusMod;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ResourceAssociationActionType;
import org.apache.syncope.common.lib.types.ResourceDeassociationActionType;
import org.apache.syncope.common.lib.wrap.ResourceKey;
import org.apache.syncope.common.rest.api.beans.AnyListQuery;
import org.apache.syncope.common.rest.api.beans.AnySearchQuery;

/**
 * REST operations for users.
 */
@Path("users")
public interface UserService extends JAXRSService {

    /**
     * Gives the username for the provided user key.
     *
     * @param userKey user key
     * @return <tt>Response</tt> object featuring HTTP header with username matching the given userKey
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring HTTP header with username matching the given userKey")
    })
    @OPTIONS
    @Path("{userKey}/username")
    Response getUsername(@NotNull @PathParam("userKey") Long userKey);

    /**
     * Gives the user key for the provided username.
     *
     * @param username username
     * @return <tt>Response</tt> object featuring HTTP header with userKey matching the given username
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring HTTP header with userKey matching the given username")
    })
    @OPTIONS
    @Path("{username}/userKey")
    Response getUserKey(@NotNull @PathParam("username") String username);

    /**
     * Reads the user matching the provided userKey.
     *
     * @param userKey id of user to be read
     * @return User matching the provided userKey
     */
    @GET
    @Path("{userKey}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    UserTO read(@NotNull @PathParam("userKey") Long userKey);

    /**
     * Returns a paged list of existing users matching the given query.
     *
     * @param listQuery query conditions
     * @return paged list of existing users matching the given query
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    PagedResult<UserTO> list(@BeanParam AnyListQuery listQuery);

    /**
     * Returns a paged list of users matching the given query.
     *
     * @param searchQuery query conditions
     * @return paged list of users matching the given query
     */
    @GET
    @Path("search")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    PagedResult<UserTO> search(@BeanParam AnySearchQuery searchQuery);

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
    Response create(
            @NotNull UserTO userTO,
            @DefaultValue("true") @QueryParam("storePassword") boolean storePassword);

    /**
     * Updates user matching the provided userKey.
     *
     * @param userKey id of user to be updated
     * @param userMod modification to be applied to user matching the provided userKey
     * @return <tt>Response</tt> object featuring the updated user enriched with propagation status information
     * - {@link UserTO} as <tt>Entity</tt>
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring the updated user enriched with propagation status information - "
                + "<tt>UserTO</tt> as <tt>Entity</tt>")
    })
    @POST
    @Path("{userKey}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response update(@NotNull @PathParam("userKey") Long userKey, @NotNull UserMod userMod);

    /**
     * Performs a status update on user matching provided userKey.
     *
     * @param userKey id of user to be subjected to status update
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
    @Path("{userKey}/status")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response status(@NotNull @PathParam("userKey") Long userKey, @NotNull StatusMod statusMod);

    /**
     * Deletes user matching provided userKey.
     *
     * @param userKey id of user to be deleted
     * @return <tt>Response</tt> object featuring the deleted user enriched with propagation status information
     * - {@link UserTO} as <tt>Entity</tt>
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring the deleted user enriched with propagation status information - "
                + "<tt>UserTO</tt> as <tt>Entity</tt>")
    })
    @DELETE
    @Path("{userKey}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response delete(@NotNull @PathParam("userKey") Long userKey);

    /**
     * Executes resource-related operations on given user.
     *
     * @param userKey user key
     * @param type resource de-association action type
     * @param resourceNames external resources to be used for propagation-related operations
     * @return <tt>Response</tt> object featuring {@link BulkActionResult} as <tt>Entity</tt>
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring <tt>BulkActionResult</tt> as <tt>Entity</tt>")
    })
    @POST
    @Path("{userKey}/bulkDeassociation/{type}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response bulkDeassociation(
            @NotNull @PathParam("userKey") Long userKey,
            @NotNull @PathParam("type") ResourceDeassociationActionType type,
            @NotNull List<ResourceKey> resourceNames);

    /**
     * Executes resource-related operations on given user.
     *
     * @param userKey user key.
     * @param type resource association action type
     * @param associationMod external resources to be used for propagation-related operations
     * @return <tt>Response</tt> object featuring {@link BulkActionResult} as <tt>Entity</tt>
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE, value = "Featuring <tt>BulkActionResult</tt> as <tt>Entity</tt>")
    })
    @POST
    @Path("{userKey}/bulkAssociation/{type}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response bulkAssociation(
            @NotNull @PathParam("userKey") Long userKey,
            @NotNull @PathParam("type") ResourceAssociationActionType type,
            @NotNull ResourceAssociationMod associationMod);

    /**
     * Executes the provided bulk action.
     *
     * @param bulkAction list of user keys against which the bulk action will be performed.
     * @return Bulk action result
     */
    @POST
    @Path("bulk")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    BulkActionResult bulk(@NotNull BulkAction bulkAction);
}

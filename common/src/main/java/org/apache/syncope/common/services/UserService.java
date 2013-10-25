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
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.model.wadl.Description;
import org.apache.cxf.jaxrs.model.wadl.Descriptions;
import org.apache.cxf.jaxrs.model.wadl.DocTarget;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.search.NodeCond;
import org.apache.syncope.common.to.BulkAction;
import org.apache.syncope.common.to.BulkActionRes;
import org.apache.syncope.common.to.PropagationRequestTO;
import org.apache.syncope.common.to.PropagationTargetsTO;
import org.apache.syncope.common.to.UserTO;

@Path("users")
@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
public interface UserService {

    /**
     * Reads the user matching the provided userId.
     *
     * @param userId id of user to be read
     * @return User matching the provided userId
     */
    @GET
    @Path("{userId}")
    @Descriptions({
        @Description(target = DocTarget.METHOD, value = "Reads the user matching the provided userId"),
        @Description(target = DocTarget.RETURN, value = "User matching the provided userId")
    })
    UserTO read(@Description("id of user to be read") @PathParam("userId") Long userId);

    /**
     * Returns a list of all existing users.
     *
     * @return A list of all existing users.
     */
    @GET
    @Descriptions({
        @Description(target = DocTarget.METHOD, value = "Returns a list of all existing users"),
        @Description(target = DocTarget.RETURN, value = "A list of all existing users")
    })
    List<UserTO> list();

    /**
     * Returns a paged list of all existing users.
     *
     * @param page result page number
     * @param size number of entries per page
     * @return A list of all existing users matching page/size conditions.
     */
    @GET
    @Descriptions({
        @Description(target = DocTarget.METHOD, value = "Returns a paged list of all existing users"),
        @Description(target = DocTarget.RETURN, value = "A list of all existing users matching page/size conditions")
    })
    List<UserTO> list(@Description("result page number") @QueryParam("page") int page,
            @Description("number of entries per page") @QueryParam("size") @DefaultValue("25") int size);

    /**
     * Returns the number of existing users.
     *
     * @return Number of existing users
     */
    @GET
    @Path("count")
    @Descriptions({
        @Description(target = DocTarget.METHOD, value = "Returns the number of existing users"),
        @Description(target = DocTarget.RETURN, value = "Number of existing users")
    })
    int count();

    /**
     * Reads the user matching the provided username.
     *
     * @param username username of user to be read
     * @return User matching the provided username
     */
    @GET
    @Descriptions({
        @Description(target = DocTarget.METHOD, value = "Reads the user matching the provided username"),
        @Description(target = DocTarget.RETURN, value = "User matching the provided username")
    })
    UserTO read(@Description("username of user to be read") @QueryParam("username") String username);

    /**
     * Reads data about the authenticated user.
     *
     * @return Data about the authenticated user
     */
    @GET
    @Path("self")
    @Descriptions({
        @Description(target = DocTarget.METHOD, value = "Reads the user matching the provided username"),
        @Description(target = DocTarget.RETURN, value = "Data about the authenticated user")
    })
    UserTO readSelf();

    /**
     * Returns the list of users matching the given search condition.
     *
     * @param searchCondition search condition
     * @return List of users matching the given search condition
     * @throws InvalidSearchConditionException if provided search condition is not valid
     */
    @POST
    @Path("search")
    @Descriptions({
        @Description(target = DocTarget.METHOD,
                value = "Returns the list of users matching the given search condition"),
        @Description(target = DocTarget.RETURN, value = "List of users matching the given condition")
    })
    List<UserTO> search(@Description("search condition") NodeCond searchCondition)
            throws InvalidSearchConditionException;

    /**
     * Returns the paged list of users matching the given search condition.
     *
     * @param searchCondition search condition
     * @param page result page number
     * @param size number of entries per page
     * @return List of users matching the given search and page/size conditions
     * @throws InvalidSearchConditionException if provided search condition is not valid
     */
    @POST
    @Path("search")
    @Descriptions({
        @Description(target = DocTarget.METHOD,
                value = "Returns the paged list of users matching the given search condition"),
        @Description(target = DocTarget.RETURN,
                value = "List of users matching the given search and page/size conditions")
    })
    List<UserTO> search(@Description("search condition") NodeCond searchCondition,
            @Description("result page number") @QueryParam("page") int page,
            @Description("number of entries per page") @QueryParam("size") @DefaultValue("25") int size)
            throws InvalidSearchConditionException;

    /**
     * Returns the number of users matching the provided search condition.
     *
     * @param searchCondition search condition
     * @return Number of users matching the provided search condition
     * @throws InvalidSearchConditionException if provided search condition is not valid
     */
    @POST
    @Path("search/count")
    @Descriptions({
        @Description(target = DocTarget.METHOD,
                value = "Returns the number of users matching the provided search condition"),
        @Description(target = DocTarget.RETURN,
                value = "Number of users matching the provided search condition")
    })
    int searchCount(@Description("search condition") NodeCond searchCondition)
            throws InvalidSearchConditionException;

    /**
     * Creates a new user.
     *
     * @param userTO user to be created
     * @return <tt>Response</tt> object featuring <tt>Location</tt> header of created user
     */
    @POST
    @Descriptions({
        @Description(target = DocTarget.METHOD, value = "Creates a new user"),
        @Description(target = DocTarget.RETURN,
                value = "Response object featuring <tt>Location</tt> header of created user"),
        @Description(target = DocTarget.RESPONSE,
                value = "User created available at URL specified via the <tt>Location</tt> header")
    })
    Response create(@Description("user to be created") UserTO userTO);

    /**
     * Updates user matching the provided userId.
     *
     * @param userId id of user to be updated
     * @param userMod modification to be applied to user matching the provided userId
     * @return Updated user.
     */
    @POST
    @Path("{userId}")
    @Descriptions({
        @Description(target = DocTarget.METHOD, value = "Updates user matching the provided userId"),
        @Description(target = DocTarget.RETURN, value = "Updated user")
    })
    UserTO update(@Description("id of user to be updated") @PathParam("userId") Long userId,
            @Description("modification to be applied to user matching the provided userId") UserMod userMod);

    /**
     * Deletes user matching provided userId.
     *
     * @param userId id of user to be deleted
     * @return Deleted user
     */
    @DELETE
    @Path("{userId}")
    @Descriptions({
        @Description(target = DocTarget.METHOD, value = "Deletes user matching provided userId"),
        @Description(target = DocTarget.RETURN, value = "Deleted user")
    })
    UserTO delete(@Description("id of user to be deleted") @PathParam("userId") Long userId);

    /**
     * Executes the provided bulk action.
     *
     * @param bulkAction list of &lt;username, action&gt; pairs
     * @return Bulk action result.
     */
    @POST
    @Path("bulk")
    @Descriptions({
        @Description(target = DocTarget.METHOD, value = "Executes the provided bulk action"),
        @Description(target = DocTarget.RETURN, value = "Bulk action result")
    })
    BulkActionRes bulkAction(@Description("list of &lt;username, action&gt; pairs") BulkAction bulkAction);

    /**
     * Activates user matching provided userId if provided token is valid.
     *
     * @param userId id of user to be activated
     * @param token validity token
     * @return Activated user
     */
    @POST
    @Path("{userId}/status/activate")
    @Descriptions({
        @Description(target = DocTarget.METHOD,
                value = "Activates user matching provided userId if provided token is valid"),
        @Description(target = DocTarget.RETURN, value = "Activated user")
    })
    UserTO activate(@Description("id of user to be activated") @PathParam("userId") Long userId,
            @Description("validity token") @QueryParam("token") String token);

    /**
     * Activates user matching provided userId if provided token is valid and propagates this update
     * only to resources contained in the propagation request.
     *
     * @param userId id of user to be activated
     * @param token validity token
     * @param propagationRequestTO propagation request on internal storage or on 0+ external resources
     * @return Activated user
     */
    @POST
    @Path("{userId}/status/activate/propagation")
    @Descriptions({
        @Description(target = DocTarget.METHOD,
                value = "Activates user matching provided userId if provided token is valid and propagates "
                + "this update only to resources contained in the propagation request."),
        @Description(target = DocTarget.RETURN, value = "Activated user")
    })
    UserTO activate(@Description("id of user to be activated") @PathParam("userId") Long userId,
            @Description("validity token") @QueryParam("token") String token,
            @Description("propagation request on internal storage or on 0+ external resources"
            ) PropagationRequestTO propagationRequestTO);

    /**
     * Activates user matching provided username if provided token is valid.
     *
     * @param username username of user to be activated
     * @param token validity token
     * @return Activated user
     */
    @POST
    @Path("activateByUsername/{username}")
    @Descriptions({
        @Description(target = DocTarget.METHOD,
                value = "Activates user matching provided username if provided token is valid"),
        @Description(target = DocTarget.RETURN, value = "Activated user")
    })
    UserTO activateByUsername(@Description("username of user to be activated") @PathParam("username") String username,
            @Description("validity token") @QueryParam("token") String token);

    /**
     * Activates user matching provided username if provided token is valid and propagates this update
     * only to resources contained in the propagation request.
     *
     * @param username username of user to be activated
     * @param token validity token
     * @param propagationRequestTO propagation request on internal storage or on 0+ external resources
     * @return Activated user
     */
    @POST
    @Path("activateByUsername/{username}/propagation")
    @Descriptions({
        @Description(target = DocTarget.METHOD,
                value = "Activates user matching provided username if provided token is valid and propagates "
                + "this update only to resources contained in the propagation request."),
        @Description(target = DocTarget.RETURN, value = "Activated user")
    })
    UserTO activateByUsername(@Description("username of user to be activated") @PathParam("username") String username,
            @Description("validity token") @QueryParam("token") String token,
            @Description("propagation request on internal storage or on 0+ external resources"
            ) PropagationRequestTO propagationRequestTO);

    /**
     * Suspends user matching provided userId.
     *
     * @param userId id of user to be suspended
     * @return Suspended user
     */
    @POST
    @Path("{userId}/status/suspend")
    @Descriptions({
        @Description(target = DocTarget.METHOD, value = "Suspends user matching provided userId"),
        @Description(target = DocTarget.RETURN, value = "Suspended user")
    })
    UserTO suspend(@Description("id of user to be suspended") @PathParam("userId") Long userId);

    /**
     * Suspend user matching provided userId and propagates this update only to resources contained in the
     * propagation request.
     *
     * @param userId id of user to be activated
     * @param propagationRequestTO propagation request on internal storage or on 0+ external resources
     * @return Suspended user
     */
    @POST
    @Path("{userId}/status/suspend/propagation")
    @Descriptions({
        @Description(target = DocTarget.METHOD, value = "Suspend user matching provided userId and propagates this "
                + "update only to resources contained in the propagation request"),
        @Description(target = DocTarget.RETURN, value = "Suspended user")
    })
    UserTO suspend(@Description("id of user to be suspended") @PathParam("userId") Long userId,
            @Description("propagation request on internal storage or on 0+ external resources"
            ) PropagationRequestTO propagationRequestTO);

    /**
     * Suspends user matching provided username.
     *
     * @param username username of user to be suspended
     * @return Suspended user
     */
    @POST
    @Path("suspendByUsername/{username}")
    @Descriptions({
        @Description(target = DocTarget.METHOD, value = "Suspends user matching provided username"),
        @Description(target = DocTarget.RETURN, value = "Suspended user")
    })
    UserTO suspendByUsername(@Description("username of user to be suspended") @PathParam("username") String username);

    /**
     * Suspend user matching provided username and propagates this update only to resources contained in the
     * propagation request.
     *
     * @param username username of user to be activated
     * @param propagationRequestTO propagation request on internal storage or on 0+ external resources
     * @return Suspended user
     */
    @POST
    @Path("suspendByUsername/{username}/propagation")
    @Descriptions({
        @Description(target = DocTarget.METHOD, value = "Suspend user matching provided username and propagates this "
                + "update only to resources contained in the propagation request"),
        @Description(target = DocTarget.RETURN, value = "Suspended user")
    })
    UserTO suspendByUsername(@Description("username of user to be suspended") @PathParam("username") String username,
            @Description("propagation request on internal storage or on 0+ external resources"
            ) PropagationRequestTO propagationRequestTO);

    /**
     * Reactivates user matching provided userId.
     *
     * @param userId id of user to be reactivated
     * @return Reactivated user
     */
    @POST
    @Path("{userId}/status/reactivate")
    @Descriptions({
        @Description(target = DocTarget.METHOD, value = "Reactivates user matching provided userId"),
        @Description(target = DocTarget.RETURN, value = "Reactivated user")
    })
    UserTO reactivate(@Description("id of user to be reactivated") @PathParam("userId") Long userId);

    /**
     * Reactivates user matching provided userId and propagates this update only to resources contained in the
     * propagation request.
     *
     * @param userId id of user to be activated
     * @param propagationRequestTO propagation request on internal storage or on 0+ external resources
     * @return Reactivated user
     */
    @POST
    @Path("{userId}/status/reactivate/propagation")
    @Descriptions({
        @Description(target = DocTarget.METHOD,
                value = "Reactivates user matching provided userId and propagates this update only to resources "
                + "contained in the propagation request"),
        @Description(target = DocTarget.RETURN, value = "Reactivated user")
    })
    UserTO reactivate(@Description("id of user to be reactivated") @PathParam("userId") Long userId,
            @Description("propagation request on internal storage or on 0+ external resources"
            ) PropagationRequestTO propagationRequestTO);

    /**
     * Reactivates user matching provided username.
     *
     * @param username username of user to be reactivated
     * @return Reactivated user
     */
    @POST
    @Path("reactivateByUsername/{username}")
    @Descriptions({
        @Description(target = DocTarget.METHOD, value = "Reactivates user matching provided username"),
        @Description(target = DocTarget.RETURN, value = "Reactivated user")
    })
    UserTO reactivateByUsername(
            @Description("username of user to be reactivated") @PathParam("username") String username);

    /**
     * Reactivates user matching provided username and propagates this update only to resources contained in the
     * propagation request.
     *
     * @param username username of user to be activated
     * @param propagationRequestTO propagation request on internal storage or on 0+ external resources
     * @return Reactivated user
     */
    @POST
    @Path("reactivateByUsername/{username}/propagation")
    @Descriptions({
        @Description(target = DocTarget.METHOD,
                value = "Reactivates user matching provided username and propagates this update only to resources "
                + "contained in the propagation request"),
        @Description(target = DocTarget.RETURN, value = "Reactivated user")
    })
    UserTO reactivateByUsername(
            @Description("username of user to be reactivated") @PathParam("username") String username,
            @Description("propagation request on internal storage or on 0+ external resources"
            ) PropagationRequestTO propagationRequestTO);

    /**
     * Unlinks user from the given external resources.
     *
     * @param userId id of user to be unlinked
     * @param propagationTargetsTO external resources to be used for propagation-related operations
     * @return Updated user
     */
    @PUT
    @Path("{userId}/unlink")
    @Descriptions({
        @Description(target = DocTarget.METHOD, value = "Unlinks user from the given external resources"),
        @Description(target = DocTarget.RETURN, value = "Updated user")
    })
    UserTO unlink(@Description("id of user to be unlinked") @PathParam("userId") Long userId,
            @Description("external resources to be used for propagation-related operations"
            ) PropagationTargetsTO propagationTargetsTO);

    /**
     * De-provision user from the given external resources without unlinking.
     *
     * @param userId id of user to be de-provisioned
     * @param propagationTargetsTO external resources to be used for propagation-related operations
     * @return Updated user
     */
    @PUT
    @Path("{userId}/deprovision")
    @Descriptions({
        @Description(target = DocTarget.METHOD,
                value = "De-provision user from the given external resources without unlinking"),
        @Description(target = DocTarget.RETURN, value = "Updated user")
    })
    UserTO deprovision(@Description("id of user to be de-provisioned") @PathParam("userId") Long userId,
            @Description("De-provision user from the given external resources without unlinking"
            ) PropagationTargetsTO propagationTargetsTO);

    /**
     * Unassigns (unlink + de-provision) user from the given external resources.
     *
     * @param userId id of user to be unassigned
     * @param propagationTargetsTO external resources to be used for propagation-related operations
     * @return Updated user
     */
    @PUT
    @Path("{userId}/unassign")
    @Descriptions({
        @Description(target = DocTarget.METHOD,
                value = "Unassigns (unlink + de-provision) user from the given external resources"),
        @Description(target = DocTarget.RETURN, value = "Updated user")
    })
    UserTO unassign(@Description("id of user to be unassigned") @PathParam("userId") Long userId,
            @Description("De-provision user from the given external resources without unlinking"
            ) PropagationTargetsTO propagationTargetsTO);

}

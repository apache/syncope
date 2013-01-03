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
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.syncope.client.mod.StatusMod;
import org.apache.syncope.client.mod.UserMod;
import org.apache.syncope.client.search.NodeCond;
import org.apache.syncope.client.to.UserTO;
import org.apache.syncope.client.to.WorkflowFormTO;

@Path("user")
public interface UserService {

    /**
     * @deprecated As of release 1.2.0, replaced by
     *             {@link #setStatus(Long, StatusMod)}
     */
    @Deprecated
    UserTO activate(long userId, String token);

    /**
     * @deprecated As of release 1.2.0, replaced by
     *             {@link #setStatus(Long, StatusMod)}
     */
    @Deprecated
    UserTO activateByUsername(String username, String token);

    /**
     * @deprecated This method needs to be moved to a new workflow service.
     */
    @Deprecated
    @POST
    @Path("workflow/task/{taskId}/claim")
    WorkflowFormTO claimForm(@PathParam("taskId") final String taskId);

    @GET
    @Path("count")
    int count();

    @POST
    @Path("")
    UserTO create(final UserTO userTO);

    @DELETE
    @Path("{userId}")
    UserTO delete(@PathParam("userId") final Long userId);

    /**
     * @deprecated This method needs to be moved to a new workflow service.
     */
    @Deprecated
    @POST
    UserTO executeWorkflow(@PathParam("taskId") final String taskId, final UserTO userTO);

    /**
     * @deprecated This method needs to be moved to a new workflow service.
     */
    @Deprecated
    @GET
    @Path("{userId}/workflow/form")
    WorkflowFormTO getFormForUser(@PathParam("userId") final Long userId);

    /**
     * @deprecated This method needs to be moved to a new workflow service.
     */
    @Deprecated
    @GET
    @Path("workflow/form")
    List<WorkflowFormTO> getForms();

    @GET
    List<UserTO> list();

    @GET
    List<UserTO> list(@QueryParam("page") final int page,
            @QueryParam("size") @DefaultValue("25") final int size);

    /**
     * @deprecated As of release 1.2.0, replaced by
     *             {@link #setStatus(Long, StatusMod)}
     */
    @Deprecated
    UserTO reactivate(long userId);

    /**
     * @deprecated As of release 1.2.0, replaced by
     *             {@link #setStatus(Long, StatusMod)}
     */
    @Deprecated
    UserTO reactivate(long userId, String query);

    /**
     * @deprecated As of release 1.2.0, replaced by
     *             {@link #setStatus(Long, StatusMod)}
     */
    @Deprecated
    UserTO reactivateByUsername(String username);

    @GET
    @Path("{userId}")
    UserTO read(@PathParam("userId") final Long userId);

    @GET
    UserTO read(@MatrixParam("uname") final String username);

    /**
     * @deprecated As of release 1.2.0, use {@link #read(Long)} or
     *             {@link #read(String)} instead.
     */
    @Deprecated
    UserTO readSelf();

    @POST
    @Path("search")
    List<UserTO> search(final NodeCond searchCondition);

    @POST
    @Path("search")
    List<UserTO> search(final NodeCond searchCondition, @QueryParam("page") final int page,
            @QueryParam("size") @DefaultValue("25") final int size);

    @POST
    @Path("search/count")
    int searchCount(final NodeCond searchCondition);

    @POST
    @Path("user/{userId}/status")
    public abstract UserTO setStatus(@PathParam("userId") final Long userId, final StatusMod statusUpdate);

    /**
     * @deprecated This method needs to be moved to a new workflow service.
     */
    @Deprecated
    @POST
    @Path("workflow/form")
    UserTO submitForm(final WorkflowFormTO form);

    /**
     * @deprecated As of release 1.2.0, replaced by
     *             {@link #setStatus(Long, StatusMod)}
     */
    @Deprecated
    UserTO suspend(long userId);

    /**
     * @deprecated As of release 1.2.0, replaced by
     *             {@link #setStatus(Long, StatusMod)}
     */
    @Deprecated
    UserTO suspend(long userId, String query);

    /**
     * @deprecated As of release 1.2.0, replaced by
     *             {@link #setStatus(Long, StatusMod)}
     */
    @Deprecated
    UserTO suspendByUsername(String username);

    @POST
    @Path("{userId}")
    UserTO update(@PathParam("userId") final Long userId, final UserMod userMod);

    @GET
    Boolean verifyPassword(@MatrixParam("uname") String username, @MatrixParam("pwd") final String password);
}
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
package org.apache.syncope.controller;

import java.util.List;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.syncope.NotFoundException;
import org.apache.syncope.mod.UserMod;
import org.apache.syncope.propagation.PropagationException;
import org.apache.syncope.search.NodeCond;
import org.apache.syncope.to.UserTO;
import org.apache.syncope.to.WorkflowFormTO;
import org.apache.syncope.workflow.WorkflowException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

@Path("/user")
public interface UserService {

    @GET
    @PreAuthorize("hasRole('USER_READ')")
    @Transactional(readOnly = true)
    public abstract Boolean verifyPassword(@QueryParam("uname") String username,
            @QueryParam("pw") final String password);

    @GET
    @Path("/count")
    @PreAuthorize("hasRole('USER_LIST')")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    public abstract int count();

    @POST
    @Path("/count")
    @PreAuthorize("hasRole('USER_READ')")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    public abstract int searchCount(final NodeCond searchCondition) throws InvalidSearchConditionException;

    @GET
    @PreAuthorize("hasRole('USER_LIST')")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    public abstract List<UserTO> list();

    @GET
    @PreAuthorize("hasRole('USER_LIST')")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    public abstract List<UserTO> list(@QueryParam("page") final int page,
            @QueryParam("size") @DefaultValue("25") final int size);

    @GET
    @Path("/{userId}")
    @PreAuthorize("hasRole('USER_READ')")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    public abstract UserTO read(@PathParam("userId") final Long userId) throws NotFoundException,
            UnauthorizedRoleException;

    @GET
    @PreAuthorize("hasRole('USER_READ')")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    public abstract UserTO read(@QueryParam("uname") final String username) throws NotFoundException,
            UnauthorizedRoleException;

    @POST
    @Path("search")
    @PreAuthorize("hasRole('USER_READ')")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    public abstract List<UserTO> search(final NodeCond searchCondition)
            throws InvalidSearchConditionException;

    @POST
    @Path("search")
    @PreAuthorize("hasRole('USER_READ')")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    public abstract List<UserTO> search(final NodeCond searchCondition, @QueryParam("page") final int page,
            @QueryParam("size") @DefaultValue("25") final int size) throws InvalidSearchConditionException;

    @POST
    @Path("/")
    @PreAuthorize("hasRole('USER_CREATE')")
    public abstract Response create(final UserTO userTO) throws PropagationException,
            UnauthorizedRoleException, WorkflowException, NotFoundException;

    @POST
    @Path("/{userId}")
    @PreAuthorize("hasRole('USER_UPDATE')")
    public abstract UserTO update(@PathParam("userId") final Long userId, final UserMod userMod) throws NotFoundException, PropagationException,
            UnauthorizedRoleException, WorkflowException;

    @PUT
    @Path("/user/{userId}/status/active")
    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    public abstract UserTO activate(@PathParam("userId") final Long userId,
            @FormParam("token") final String token,
            @FormParam("resourceNames") final Set<String> resourceNames,
            @FormParam("performLocally") @DefaultValue("true") final Boolean performLocally,
            @FormParam("performRemotely") @DefaultValue("true") final Boolean performRemotely)
            throws WorkflowException, NotFoundException, UnauthorizedRoleException, PropagationException;

    @DELETE
    @Path("/user/{userId}/status/active")
    @PreAuthorize("hasRole('USER_UPDATE')")
    @Transactional(rollbackFor = { Throwable.class })
    public abstract UserTO suspend(@PathParam("userId") final Long userId,
            @FormParam("resourceNames") final Set<String> resourceNames,
            @FormParam("performLocally") @DefaultValue("true") final Boolean performLocally,
            @FormParam("performRemotely") @DefaultValue("true") final Boolean performRemotely)
            throws NotFoundException, WorkflowException, UnauthorizedRoleException, PropagationException;

    @DELETE
    @Path("/{userId}")
    @PreAuthorize("hasRole('USER_DELETE')")
    public abstract Response delete(@PathParam("userId") final Long userId) throws NotFoundException,
            WorkflowException, PropagationException, UnauthorizedRoleException;

    @POST
    @Path("/workflow/task/{taskId}/execute")
    @PreAuthorize("hasRole('USER_UPDATE')")
    public abstract UserTO executeWorkflow(@PathParam("taskId") final String taskId, final UserTO userTO)
            throws WorkflowException, NotFoundException, UnauthorizedRoleException, PropagationException;

    @GET
    @Path("/workflow/form")
    @PreAuthorize("hasRole('WORKFLOW_FORM_LIST')")
    @Transactional(readOnly = true, rollbackFor = { Throwable.class })
    public abstract List<WorkflowFormTO> getForms();

    @GET
    @Path("/{userId}/workflow/form")
    @PreAuthorize("hasRole('WORKFLOW_FORM_READ') and hasRole('USER_READ')")
    @Transactional(rollbackFor = { Throwable.class })
    public abstract WorkflowFormTO getFormForUser(@PathParam("userId") final Long userId)
            throws UnauthorizedRoleException, NotFoundException, WorkflowException;

    @POST
    @Path("/workflow/task/{taskId}/claim")
    @PreAuthorize("hasRole('WORKFLOW_FORM_CLAIM')")
    @Transactional(rollbackFor = { Throwable.class })
    public abstract WorkflowFormTO claimForm(@PathParam("taskId") final String taskId)
            throws NotFoundException, WorkflowException;

    @POST
    @Path("/workflow/form")
    @PreAuthorize("hasRole('WORKFLOW_FORM_SUBMIT')")
    @Transactional(rollbackFor = { Throwable.class })
    public abstract UserTO submitForm(final WorkflowFormTO form) throws NotFoundException, WorkflowException,
            PropagationException, UnauthorizedRoleException;
}
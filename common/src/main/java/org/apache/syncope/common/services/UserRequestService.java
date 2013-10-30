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
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.mod.UserMod;

import org.apache.syncope.common.to.UserRequestTO;
import org.apache.syncope.common.to.UserTO;

@Path("userrequests")
@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
public interface UserRequestService {

    /**
     * @return Response contains special syncope HTTP header indicating if user is allowed to
     * make a create UserRequest
     * @see org.apache.syncope.common.SyncopeConstants.REST_USER_REQUEST_CREATE_ALLOWED
     */
    @OPTIONS
    Response getOptions();

    /**
     * @param userRequestTO Request for user to be created
     * @return <tt>Response</tt> object featuring <tt>Location</tt> header of created user request
     */
    @POST
    Response create(UserRequestTO userRequestTO);

    /**
     * @return Returns list of all UserRequests.
     */
    @GET
    List<UserRequestTO> list();

    /**
     * @param username user name
     * @return Returns list of all UserRequests of the given user.
     */
    @GET
    List<UserRequestTO> listByUsername(@MatrixParam("username") String username);

    /**
     * @param requestId ID of UserRequest to be read
     * @return Returns UserRequest with matching requestId.
     */
    @GET
    @Path("{requestId}")
    UserRequestTO read(@PathParam("requestId") Long requestId);

    /**
     * @param requestId ID of UserRequest to be deleted.
     */
    @DELETE
    @Path("{requestId}")
    void delete(@PathParam("requestId") Long requestId);

    @GET
    @Path("claim/{requestId}")
    UserRequestTO claim(@PathParam("requestId") Long requestId);

    @POST
    @Path("execute/create/{requestId}")
    UserTO executeCreate(@PathParam("requestId") Long requestId, UserTO reviewed);

    @POST
    @Path("execute/update/{requestId}")
    UserTO executeUpdate(@PathParam("requestId") Long requestId, UserMod changes);

    @POST
    @Path("execute/delete/{requestId}")
    UserTO executeDelete(@PathParam("requestId") Long requestId);
}

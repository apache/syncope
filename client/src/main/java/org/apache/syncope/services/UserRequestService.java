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

import org.apache.syncope.client.mod.UserMod;
import org.apache.syncope.client.to.UserRequestTO;
import org.apache.syncope.client.to.UserTO;

@Path("requests/users")
public interface UserRequestService {

    @GET
    @Path("create/allowed")
    //    @RequestMapping(method = RequestMethod.GET, value = "/create/allowed")
    boolean isCreateAllowed();

    @POST
    @Path("create")
    //    @RequestMapping(method = RequestMethod.POST, value = "/create")
    UserRequestTO create(final UserTO userTO);

    @POST
    @Path("update")
    //    @RequestMapping(method = RequestMethod.POST, value = "/update")
    UserRequestTO update(final UserMod userMod);

    @POST
    @Path("delete")
    //    @RequestMapping(method = RequestMethod.GET, value = "/delete/{userId}")
    UserRequestTO delete(final Long userId);

    @GET
    //    @RequestMapping(method = RequestMethod.GET, value = "/list")
    List<UserRequestTO> list();

    @GET
    @Path("{requestId}")
    //    @RequestMapping(method = RequestMethod.GET, value = "/read/{requestId}")
    UserRequestTO read(@PathParam("requestId") final Long requestId);

    @DELETE
    @Path("{requestId}")
    //    @RequestMapping(method = RequestMethod.GET, value = "/deleteRequest/{requestId}")
    UserRequestTO deleteRequest(@PathParam("requestId") final Long requestId);

}
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

import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.search.NodeCond;
import org.apache.syncope.common.to.PropagationRequestTO;
import org.apache.syncope.common.to.UserTO;

@Path("users")
public interface UserService {

    @POST
    @Path("{userId}/status/activate")
    UserTO activate(@PathParam("userId") long userId, @QueryParam("token") String token);

    @POST
    @Path("{userId}/status/activate/propagation")
    UserTO activate(@PathParam("userId") long userId, @QueryParam("token") String token,
            PropagationRequestTO propagationRequestTO);

    @POST
    @Path("activateByUsername/{username}")
    UserTO activateByUsername(@PathParam("username") String username, @QueryParam("token") String token);

    @POST
    @Path("activateByUsername/{username}/propagation")
    UserTO activateByUsername(@PathParam("username") String username, @QueryParam("token") String token,
            PropagationRequestTO propagationRequestTO);

    @GET
    @Path("count")
    int count();

    @POST
    Response create(UserTO userTO);

    @DELETE
    @Path("{userId}")
    UserTO delete(@PathParam("userId") Long userId);

   @GET
    List<UserTO> list();

    @GET
    List<UserTO> list(@QueryParam("page") int page, @QueryParam("size") @DefaultValue("25") int size);

    @POST
    @Path("{userId}/status/reactivate")
    UserTO reactivate(@PathParam("userId") long userId);

    @POST
    @Path("{userId}/status/reactivate/propagation")
    UserTO reactivate(@PathParam("userId") long userId, PropagationRequestTO propagationRequestTO);

    @POST
    @Path("reactivateByUsername/{username}")
    UserTO reactivateByUsername(@PathParam("username") String username);

    @POST
    @Path("reactivateByUsername/{username}/propagation")
    UserTO reactivateByUsername(@PathParam("username") String username, PropagationRequestTO propagationRequestTO);

    @GET
    @Path("{userId}")
    UserTO read(@PathParam("userId") Long userId);

    @GET
    UserTO read(@QueryParam("username") String username);

    @GET
    @Path("self")
    UserTO readSelf();

    @POST
    @Path("search")
    List<UserTO> search(NodeCond searchCondition) throws InvalidSearchConditionException;

    @POST
    @Path("search")
    List<UserTO> search(NodeCond searchCondition, @QueryParam("page") int page,
            @QueryParam("size") @DefaultValue("25") int size) throws InvalidSearchConditionException;

    @POST
    @Path("search/count")
    int searchCount(NodeCond searchCondition) throws InvalidSearchConditionException;

    @POST
    @Path("{userId}/status/suspend")
    UserTO suspend(@PathParam("userId") long userId);

    @POST
    @Path("{userId}/status/suspend/propagation")
    UserTO suspend(@PathParam("userId") long userId, PropagationRequestTO propagationRequestTO);

    @POST
    @Path("suspendByUsername/{username}")
    UserTO suspendByUsername(@PathParam("username") String username);

    @POST
    @Path("suspendByUsername/{username}/propagation")
    UserTO suspendByUsername(@PathParam("username") String username, PropagationRequestTO propagationRequestTO);

    @POST
    @Path("{userId}")
    UserTO update(@PathParam("userId") Long userId, UserMod userMod);

}

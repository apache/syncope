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
package org.apache.syncope.fit.buildtools.cxf;

import java.util.List;
import java.util.UUID;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("users")
public interface UserService {

    @GET
    List<User> list();

    @GET
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON })
    User read(@PathParam("key") UUID key);

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    void create(User user);

    @PUT
    @Path("{key}")
    @Consumes({ MediaType.APPLICATION_JSON })
    void update(@PathParam("key") UUID key, User user);

    @DELETE
    @Path("{key}")
    void delete(@PathParam("key") UUID key);

    @POST
    @Path("authenticate")
    @Produces({ MediaType.APPLICATION_JSON })
    User authenticate(@QueryParam("username") String username, @QueryParam("password") String password);

    @POST
    @Path("clear")
    void clear();
}

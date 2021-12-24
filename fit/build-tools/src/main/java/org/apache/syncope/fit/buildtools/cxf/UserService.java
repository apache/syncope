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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("users")
public class UserService {

    private static final Map<UUID, UserMetadata> USERS = new HashMap<>();

    @Context
    private UriInfo uriInfo;

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public List<User> list() {
        return USERS.values().stream().
                filter(meta -> !meta.isDeleted()).
                map(UserMetadata::getUser).
                collect(Collectors.toList());
    }

    @GET
    @Path("changelog")
    @Produces({ MediaType.APPLICATION_JSON })
    public List<UserMetadata> changelog(@QueryParam("from") final Date from) {
        Stream<UserMetadata> users = USERS.values().stream();
        if (from != null) {
            users = users.filter(meta -> meta.getLastChangeDate().after(from));
        }
        return users.collect(Collectors.toList());
    }

    @GET
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON })
    public User read(@PathParam("key") final UUID key) {
        UserMetadata meta = USERS.get(key);
        if (meta == null || meta.isDeleted()) {
            throw new NotFoundException(key.toString());
        }
        return meta.getUser();
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    public Response create(final User user) {
        if (user.getKey() == null) {
            user.setKey(UUID.randomUUID());
        }
        if (user.getStatus() == null) {
            user.setStatus(User.Status.ACTIVE);
        }

        UserMetadata meta = USERS.get(user.getKey());
        if (meta != null && !meta.isDeleted()) {
            throw new ClientErrorException("User already exists: " + user.getKey(), Response.Status.CONFLICT);
        }

        meta = new UserMetadata();
        meta.setLastChangeDate(new Date());
        meta.setUser(user);
        USERS.put(user.getKey(), meta);

        return Response.created(uriInfo.getAbsolutePathBuilder().path(user.getKey().toString()).build()).build();
    }

    @PUT
    @Path("{key}")
    @Consumes({ MediaType.APPLICATION_JSON })
    public void update(@PathParam("key") final UUID key, final User updatedUser) {
        UserMetadata meta = USERS.get(key);
        if (meta == null || meta.isDeleted()) {
            throw new NotFoundException(key.toString());
        }

        if (updatedUser.getUsername() != null) {
            meta.getUser().setUsername(updatedUser.getUsername());
        }
        if (updatedUser.getPassword() != null) {
            meta.getUser().setPassword(updatedUser.getPassword());
        }
        if (updatedUser.getFirstName() != null) {
            meta.getUser().setFirstName(updatedUser.getFirstName());
        }
        if (updatedUser.getSurname() != null) {
            meta.getUser().setSurname(updatedUser.getSurname());
        }
        if (updatedUser.getEmail() != null) {
            meta.getUser().setEmail(updatedUser.getEmail());
        }
        if (updatedUser.getStatus() != null) {
            meta.getUser().setStatus(updatedUser.getStatus());
        }

        meta.setLastChangeDate(new Date());
    }

    @DELETE
    @Path("{key}")
    public void delete(@PathParam("key") final UUID key) {
        UserMetadata meta = USERS.get(key);
        if (meta == null || meta.isDeleted()) {
            throw new NotFoundException(key.toString());
        }

        meta.setDeleted(true);
        meta.setLastChangeDate(new Date());
    }

    @POST
    @Path("authenticate")
    @Produces({ MediaType.APPLICATION_JSON })
    public User authenticate(
            @QueryParam("username") final String username,
            @QueryParam("password") final String password) {

        Optional<User> user = USERS.values().stream().
                filter(meta -> !meta.isDeleted() && username.equals(meta.getUser().getUsername())).
                findFirst().map(UserMetadata::getUser);

        if (user.isEmpty()) {
            throw new NotFoundException(username);
        }
        if (!password.equals(user.get().getPassword())) {
            throw new ForbiddenException();
        }

        return user.get();
    }

    @POST
    @Path("clear")
    public void clear() {
        USERS.clear();
    }
}

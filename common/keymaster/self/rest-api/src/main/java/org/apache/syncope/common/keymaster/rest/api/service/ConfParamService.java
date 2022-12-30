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
package org.apache.syncope.common.keymaster.rest.api.service;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;

/**
 * REST operations for Self Keymaster's conf params.
 */
@Path("conf")
public interface ConfParamService extends Serializable {

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    Map<String, Object> list();

    @GET
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON })
    Response get(@NotNull @PathParam("key") String key);

    @POST
    @Path("{key}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    Response set(@NotNull @PathParam("key") String key, InputStream value);

    @DELETE
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON })
    Response remove(@NotNull @PathParam("key") String key);
}

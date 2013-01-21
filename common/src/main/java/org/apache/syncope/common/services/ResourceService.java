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
import java.util.Set;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.syncope.common.to.ConnObjectTO;
import org.apache.syncope.common.to.ResourceTO;
import org.apache.syncope.common.types.AttributableType;

@Path("resources")
public interface ResourceService {

    @POST
    ResourceTO create(ResourceTO resourceTO);

    @PUT
    @Path("{resourceName}")
    ResourceTO update(@PathParam("resourceName") String resourceName, ResourceTO resourceTO);

    @DELETE
    @Path("{resourceName}")
    ResourceTO delete(@PathParam("resourceName") String resourceName);

    @GET
    @Path("{resourceName}")
    ResourceTO read(@PathParam("resourceName") String resourceName);

    // TODO: is it resource method?
    @GET
    @Path("propagationActionsClasses")
    Set<String> getPropagationActionsClasses();

    @GET
    List<ResourceTO> list();

    @GET
    List<ResourceTO> list(@MatrixParam("connInstanceId") Long connInstanceId);

    @GET
    @Path("{resourceName}/{type}/{objectId}")
    ConnObjectTO getConnector(@PathParam("resourceName") String resourceName,
            @PathParam("type") AttributableType type, @PathParam("objectId") String objectId);

    @POST
    @Path("validate")
    boolean check(ResourceTO resourceTO);
}

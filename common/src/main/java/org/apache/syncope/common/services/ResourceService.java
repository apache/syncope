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
import javax.ws.rs.core.Response;

import org.apache.syncope.common.to.ConnObjectTO;
import org.apache.syncope.common.to.PropagationActionClassTO;
import org.apache.syncope.common.to.ResourceTO;
import org.apache.syncope.common.types.AttributableType;

@Path("resources")
public interface ResourceService {

    /**
     * @param resourceTO Resource to be checked
     * @return Returns true, if connection to resource could be established
     */
    @POST
    @Path("check")
    boolean check(ResourceTO resourceTO);

    /**
     * @param resourceTO Resource to be created
     * @return Response containing URI location for created resource
     */
    @POST
    Response create(ResourceTO resourceTO);

    /**
     * @param resourceName Name of resource to be deleted
     */
    @DELETE
    @Path("{resourceName}")
    void delete(@PathParam("resourceName") String resourceName);

    /**
     * @param resourceName Name of resource to get connector from
     * @param type
     * @param objectId
     * @return Returns connector for matching parameters
     */
    @GET
    @Path("{resourceName}/{type}/{objectId}")
    ConnObjectTO getConnector(@PathParam("resourceName") String resourceName,
            @PathParam("type") AttributableType type, @PathParam("objectId") String objectId);

    /**
     * @return Returns PropagationActionsClasses
     */
    @GET
    @Path("propagationActionsClasses")
    Set<PropagationActionClassTO> getPropagationActionsClasses();

    /**
     * @return Returns list of all Resources
     */
    @GET
    List<ResourceTO> list();

    /**
     * @param connInstanceId Connector id to filter for resources
     * @return Returns all resources using matching connector
     */
    @GET
    List<ResourceTO> list(@MatrixParam("connectorId") Long connInstanceId);

    /**
     * @param resourceName Name of resource to be read
     * @return Resource with matching name
     */
    @GET
    @Path("{resourceName}")
    ResourceTO read(@PathParam("resourceName") String resourceName);

    /**
     * @param resourceName Name of resource to be updated
     * @param resourceTO Resource to be stored
     */
    @PUT
    @Path("{resourceName}")
    void update(@PathParam("resourceName") String resourceName, ResourceTO resourceTO);

}

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
package org.apache.syncope.common.rest.api.service;

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.patch.ResourceDeassociationPatch;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.PagedConnObjectTOResult;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.rest.api.beans.ConnObjectTOListQuery;

/**
 * REST operations for external resources.
 */
@Path("resources")
public interface ResourceService extends JAXRSService {

    /**
     * Returns connector object from the external resource, for the given type and key.
     *
     * @param key name of resource to read connector object from
     * @param anyTypeKey any object type
     * @param anyKey any object key
     * @return connector object from the external resource, for the given type and key
     */
    @GET
    @Path("{key}/{anyTypeKey}/{anyKey}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    ConnObjectTO readConnObject(
            @NotNull @PathParam("key") String key,
            @NotNull @PathParam("anyTypeKey") String anyTypeKey,
            @NotNull @PathParam("anyKey") String anyKey);

    /**
     * Returns a paged list of connector objects from external resource, for the given type, matching
     * page/size conditions.
     *
     * @param key name of resource to read connector object from
     * @param anyTypeKey any object type
     * @param listQuery query conditions
     * @return connector objects from the external resource, for the given type
     */
    @GET
    @Path("{key}/{anyTypeKey}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    PagedConnObjectTOResult listConnObjects(
            @NotNull @PathParam("key") String key,
            @NotNull @PathParam("anyTypeKey") String anyTypeKey,
            @BeanParam ConnObjectTOListQuery listQuery);

    /**
     * Returns the resource with matching name.
     *
     * @param key Name of resource to be read
     * @return resource with matching name
     */
    @GET
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    ResourceTO read(@NotNull @PathParam("key") String key);

    /**
     * Returns a list of all resources.
     *
     * @return list of all resources
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    List<ResourceTO> list();

    /**
     * Creates a new resource.
     *
     * @param resourceTO Resource to be created
     * @return Response object featuring Location header of created resource
     */
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response create(@NotNull ResourceTO resourceTO);

    /**
     * Updates the resource matching the given name.
     *
     * @param resourceTO resource to be stored
     */
    @PUT
    @Path("{key}")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    void update(@NotNull ResourceTO resourceTO);

    /**
     * Queries the connector underlying the given resource for the latest sync token value associated to the given any
     * type and stores the value internally, for later usage.
     *
     * @param key resource
     * @param anyTypeKey any type
     */
    @POST
    @Path("{key}/{anyTypeKey}")
    void setLatestSyncToken(@NotNull @PathParam("key") String key, @NotNull @PathParam("anyTypeKey") String anyTypeKey);

    /**
     * Removes the sync token value associated to the given any type from the given resource.
     *
     * @param key resource
     * @param anyTypeKey any type
     */
    @DELETE
    @Path("{key}/{anyTypeKey}")
    void removeSyncToken(@NotNull @PathParam("key") String key, @NotNull @PathParam("anyTypeKey") String anyTypeKey);

    /**
     * Deletes the resource matching the given name.
     *
     * @param key name of resource to be deleted
     */
    @DELETE
    @Path("{key}")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    void delete(@NotNull @PathParam("key") String key);

    /**
     * Checks whether the connection to resource could be established.
     *
     * @param resourceTO resource to be checked
     */
    @POST
    @Path("check")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    void check(@NotNull ResourceTO resourceTO);

    /**
     * De-associate any objects from the given resource.
     *
     * @param patch any objects to be used for propagation-related operations
     * @return Bulk action result
     */
    @POST
    @Path("{key}/bulkDeassociation/{anyTypeKey}/{action}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    BulkActionResult bulkDeassociation(@NotNull ResourceDeassociationPatch patch);
}

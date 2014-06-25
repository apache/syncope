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
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.model.wadl.Description;
import org.apache.cxf.jaxrs.model.wadl.Descriptions;
import org.apache.cxf.jaxrs.model.wadl.DocTarget;
import org.apache.syncope.common.reqres.BulkAction;
import org.apache.syncope.common.reqres.BulkActionResult;
import org.apache.syncope.common.to.ConnObjectTO;
import org.apache.syncope.common.to.ResourceTO;
import org.apache.syncope.common.types.ResourceDeassociationActionType;
import org.apache.syncope.common.types.SubjectType;
import org.apache.syncope.common.wrap.PropagationActionClass;
import org.apache.syncope.common.wrap.SubjectId;

/**
 * REST operations for external resources.
 */
@Path("resources")
public interface ResourceService extends JAXRSService {

    /**
     * Returns connector object from the external resource, for the given type and id.
     *
     * @param resourceName Name of resource to read connector object from
     * @param type user / role
     * @param id user id / role id
     * @return connector object from the external resource, for the given type and id
     */
    @GET
    @Path("{resourceName}/{type}/{id}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    ConnObjectTO getConnectorObject(@NotNull @PathParam("resourceName") String resourceName,
            @NotNull @PathParam("type") SubjectType type, @NotNull @PathParam("id") Long id);

    /**
     * Returns a list of classes that can be used to customize the propagation process.
     *
     * @return list of classes that can be used to customize the propagation process
     */
    @GET
    @Path("propagationActionsClasses")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    List<PropagationActionClass> getPropagationActionsClasses();

    /**
     * Returns the resource with matching name.
     *
     * @param resourceName Name of resource to be read
     * @return resource with matching name
     */
    @GET
    @Path("{resourceName}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    ResourceTO read(@NotNull @PathParam("resourceName") String resourceName);

    /**
     * Returns a list of all resources.
     *
     * @return list of all resources
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    List<ResourceTO> list();

    /**
     * Returns a list of resources using matching connector instance id.
     *
     * @param connInstanceId Connector id to filter for resources
     * @return resources using matching connector instance id
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    List<ResourceTO> list(@NotNull @MatrixParam("connectorId") Long connInstanceId);

    /**
     * Creates a new resource.
     *
     * @param resourceTO Resource to be created
     * @return <tt>Response</tt> object featuring <tt>Location</tt> header of created resource
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring <tt>Location</tt> header of created resource")
    })
    @POST
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response create(@NotNull ResourceTO resourceTO);

    /**
     * Updates the resource matching the given name.
     *
     * @param resourceName name of resource to be updated
     * @param resourceTO resource to be stored
     */
    @PUT
    @Path("{resourceName}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    void update(@NotNull @PathParam("resourceName") String resourceName, @NotNull ResourceTO resourceTO);

    /**
     * Deletes the resource matching the given name.
     *
     * @param resourceName name of resource to be deleted
     */
    @DELETE
    @Path("{resourceName}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    void delete(@NotNull @PathParam("resourceName") String resourceName);

    /**
     * Checks wether the connection to resource could be established.
     *
     * @param resourceTO resource to be checked
     * @return true if connection to resource could be established
     */
    @POST
    @Path("check")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    boolean check(@NotNull ResourceTO resourceTO);

    /**
     * De-associate users or roles (depending on the provided subject type) from the given resource.
     *
     * @param resourceName name of resource
     * @param subjectType subject type (user or role)
     * @param type resource de-association action type
     * @param subjectIds users or roles against which the bulk action will be performed
     * @return <tt>Response</tt> object featuring {@link BulkActionResult} as <tt>Entity</tt>
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring <tt>BulkActionResult</tt> as <tt>Entity</tt>")
    })
    @POST
    @Path("{resourceName}/bulkDeassociation/{subjType}/{type}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    BulkActionResult bulkDeassociation(@NotNull @PathParam("resourceName") String resourceName,
            @NotNull @PathParam("subjType") SubjectType subjectType,
            @NotNull @PathParam("type") ResourceDeassociationActionType type, @NotNull List<SubjectId> subjectIds);

    /**
     * Executes the provided bulk action.
     *
     * @param bulkAction list of resource names against which the bulk action will be performed
     * @return Bulk action result
     */
    @POST
    @Path("bulk")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    BulkActionResult bulk(@NotNull BulkAction bulkAction);
}

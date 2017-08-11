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

import java.util.Set;
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
import org.apache.cxf.jaxrs.ext.PATCH;
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.patch.AssociationPatch;
import org.apache.syncope.common.lib.patch.DeassociationPatch;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.rest.api.beans.AnyQuery;

public interface AnyService<TO extends AnyTO, P extends AnyPatch> extends JAXRSService {

    /**
     * Reads the list of attributes owned by the given any object for the given schema type.
     *
     * Note that for the UserService, GroupService and AnyObjectService subclasses, if the key parameter
     * looks like a UUID then it is interpreted as as key, otherwise as a (user)name.
     *
     * @param key any object key or name
     * @param schemaType schema type
     * @return list of attributes, owned by the given any object, for the given schema type
     */
    @GET
    @Path("{key}/{schemaType}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Set<AttrTO> read(@NotNull @PathParam("key") String key, @NotNull @PathParam("schemaType") SchemaType schemaType);

    /**
     * Reads the attribute, owned by the given any object, for the given schema type and schema.
     *
     * Note that for the UserService, GroupService and AnyObjectService subclasses, if the key parameter
     * looks like a UUID then it is interpreted as as key, otherwise as a (user)name.
     *
     * @param key any object key or name
     * @param schemaType schema type
     * @param schema schema
     * @return attribute, owned by the given any object, for the given schema type and schema
     */
    @GET
    @Path("{key}/{schemaType}/{schema}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    AttrTO read(
            @NotNull @PathParam("key") String key,
            @NotNull @PathParam("schemaType") SchemaType schemaType,
            @NotNull @PathParam("schema") String schema);

    /**
     * Reads the any object matching the provided key.
     *
     * Note that for the UserService, GroupService and AnyObjectService subclasses, if the key parameter
     * looks like a UUID then it is interpreted as as key, otherwise as a (user)name.
     *
     * @param key any object key or name
     * @return any object with matching key
     */
    @GET
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    TO read(@NotNull @PathParam("key") String key);

    /**
     * Returns a paged list of any objects matching the given query.
     *
     * @param anyQuery query conditions
     * @return paged list of any objects matching the given query
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    PagedResult<TO> search(@BeanParam AnyQuery anyQuery);

    /**
     * Creates a new any object.
     *
     * @param anyTO any object to be created
     * @return Response object featuring Location header of created any object as well as the any
     * object itself enriched with propagation status information - ProvisioningResult as Entity
     */
    @POST
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response create(@NotNull TO anyTO);

    /**
     * Updates any object matching the provided key.
     *
     * @param anyPatch modification to be applied to any object matching the provided key
     * @return Response object featuring the updated any object enriched with propagation status information
     * - ProvisioningResult as Entity
     */
    @PATCH
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response update(@NotNull P anyPatch);

    /**
     * Adds or replaces the attribute, owned by the given any object, for the given schema type and schema.
     *
     * @param key any object key or name
     * @param schemaType schema type
     * @param attrTO attribute
     * @return Response object featuring the updated any object attribute - as Entity
     */
    @PUT
    @Path("{key}/{schemaType}/{schema}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response update(
            @NotNull @PathParam("key") String key,
            @NotNull @PathParam("schemaType") SchemaType schemaType,
            @NotNull AttrTO attrTO);

    /**
     * Updates any object matching the provided key.
     *
     * @param anyTO complete update
     * @return Response object featuring the updated any object enriched with propagation status information
     * - ProvisioningResult as Entity
     */
    @PUT
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response update(@NotNull TO anyTO);

    /**
     * Deletes the attribute, owned by the given any object, for the given schema type and schema.
     *
     * @param key any object key or name
     * @param schemaType schema type
     * @param schema schema
     */
    @DELETE
    @Path("{key}/{schemaType}/{schema}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    void delete(
            @NotNull @PathParam("key") String key,
            @NotNull @PathParam("schemaType") SchemaType schemaType,
            @NotNull @PathParam("schema") String schema);

    /**
     * Deletes any object matching provided key.
     *
     * @param key any object key or name
     * @return Response object featuring the deleted any object enriched with propagation status information
     * - ProvisioningResult as Entity
     */
    @DELETE
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response delete(@NotNull @PathParam("key") String key);

    /**
     * Executes resource-related operations on given any object.
     *
     * @param patch external resources to be used for propagation-related operations
     * @return Response object featuring BulkActionResult as Entity
     */
    @POST
    @Path("{key}/deassociate/{action}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response deassociate(@NotNull DeassociationPatch patch);

    /**
     * Executes resource-related operations on given any object.
     *
     * @param patch external resources to be used for propagation-related operations
     * @return Response object featuring BulkActionResult as Entity
     */
    @POST
    @Path("{key}/associate/{action}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response associate(@NotNull AssociationPatch patch);

    /**
     * Executes the provided bulk action.
     *
     * @param bulkAction list of any object ids against which the bulk action will be performed.
     * @return Response object featuring BulkActionResult as Entity
     */
    @POST
    @Path("bulk")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response bulk(@NotNull BulkAction bulkAction);
}

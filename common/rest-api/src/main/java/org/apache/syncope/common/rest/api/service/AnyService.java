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
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.rest.api.beans.AnyListQuery;
import org.apache.syncope.common.rest.api.beans.AnySearchQuery;

public interface AnyService<TO extends AnyTO, P extends AnyPatch> extends JAXRSService {

    /**
     * Reads the any object matching the provided key.
     *
     * @param key key of any object to be read
     * @return any object with matching id
     */
    @GET
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    TO read(@NotNull @PathParam("key") Long key);

    /**
     * Returns a paged list of existing any objects matching the given query.
     *
     * @param listQuery query conditions
     * @return paged list of existing any objects matching the given query
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    PagedResult<TO> list(@BeanParam AnyListQuery listQuery);

    /**
     * Returns a paged list of any objects matching the given query.
     *
     * @param searchQuery query conditions
     * @return paged list of any objects matching the given query
     */
    @GET
    @Path("search")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    PagedResult<TO> search(@BeanParam AnySearchQuery searchQuery);

    /**
     * Creates a new any object.
     *
     * @param anyTO any object to be created
     * @return <tt>Response</tt> object featuring <tt>Location</tt> header of created any object as well as the any
     * object itself enriched with propagation status information - <tt>AnyTO</tt> as <tt>Entity</tt>
     */
    @POST
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response create(@NotNull TO anyTO);

    /**
     * Updates any object matching the provided key.
     *
     * @param anyPatch modification to be applied to any object matching the provided key
     * @return <tt>Response</tt> object featuring the updated any object enriched with propagation status information
     * - <tt>AnyTO</tt> as <tt>Entity</tt>
     */
    @PATCH
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response update(@NotNull P anyPatch);

    /**
     * Updates any object matching the provided key.
     *
     * @param anyTO complete update
     * @return <tt>Response</tt> object featuring the updated any object enriched with propagation status information
     * - <tt>AnyTO</tt> as <tt>Entity</tt>
     */
    @PUT
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response update(@NotNull TO anyTO);

    /**
     * Deletes any object matching provided key.
     *
     * @param key key of any object to be deleted
     * @return <tt>Response</tt> object featuring the deleted any object enriched with propagation status information
     * - <tt>AnyTO</tt> as <tt>Entity</tt>
     */
    @DELETE
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response delete(@NotNull @PathParam("key") Long key);

    /**
     * Executes resource-related operations on given any object.
     *
     * @param patch external resources to be used for propagation-related operations
     * @return <tt>Response</tt> object featuring <tt>BulkActionResult</tt> as <tt>Entity</tt>
     */
    @POST
    @Path("{key}/deassociate/{action}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response deassociate(@NotNull DeassociationPatch patch);

    /**
     * Executes resource-related operations on given any object.
     *
     * @param patch external resources to be used for propagation-related operations
     * @return <tt>Response</tt> object featuring <tt>BulkActionResult</tt> as <tt>Entity</tt>
     */
    @POST
    @Path("{key}/associate/{action}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response associate(@NotNull AssociationPatch patch);

    /**
     * Executes the provided bulk action.
     *
     * @param bulkAction list of any object ids against which the bulk action will be performed.
     * @return Bulk action result
     */
    @POST
    @Path("bulk")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    BulkActionResult bulk(@NotNull BulkAction bulkAction);
}

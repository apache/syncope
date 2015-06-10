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
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.model.wadl.Description;
import org.apache.cxf.jaxrs.model.wadl.Descriptions;
import org.apache.cxf.jaxrs.model.wadl.DocTarget;
import org.apache.syncope.common.lib.mod.AnyMod;
import org.apache.syncope.common.lib.mod.ResourceAssociationMod;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.types.ResourceAssociationActionType;
import org.apache.syncope.common.lib.types.ResourceDeassociationActionType;
import org.apache.syncope.common.lib.wrap.ResourceKey;
import org.apache.syncope.common.rest.api.beans.AnyListQuery;
import org.apache.syncope.common.rest.api.beans.AnySearchQuery;

public interface AnyService<TO extends AnyTO, MOD extends AnyMod> extends JAXRSService {

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
     * object itself enriched with propagation status information - {@link AnyTO} as <tt>Entity</tt>
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring <tt>Location</tt> header of created any object as well as the "
                + "any object itself enriched with propagation status information - "
                + "<tt>AnyTO</tt> as <tt>Entity</tt>")
    })
    @POST
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response create(@NotNull TO anyTO);

    /**
     * Updates any object matching the provided key.
     *
     * @param anyMod modification to be applied to any object matching the provided key
     * @return <tt>Response</tt> object featuring the updated any object enriched with propagation status information
     * - {@link AnyTO} as <tt>Entity</tt>
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring the updated any object enriched with propagation status information - "
                + "<tt>AnyTO</tt> as <tt>Entity</tt>")
    })
    @POST
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response update(@NotNull MOD anyMod);

    /**
     * Deletes any object matching provided key.
     *
     * @param key key of any object to be deleted
     * @return <tt>Response</tt> object featuring the deleted any object enriched with propagation status information
     * - {@link AnyTO} as <tt>Entity</tt>
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring the deleted any object enriched with propagation status information - "
                + "<tt>AnyTO</tt> as <tt>Entity</tt>")
    })
    @DELETE
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response delete(@NotNull @PathParam("key") Long key);

    /**
     * Executes resource-related operations on given any object.
     *
     * @param key any object id.
     * @param type resource association action type
     * @param resourceNames external resources to be used for propagation-related operations
     * @return <tt>Response</tt> object featuring
     * {@link BulkActionResult} as <tt>Entity</tt>
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring <tt>BulkActionResult</tt> as <tt>Entity</tt>")
    })
    @POST
    @Path("{key}/deassociate/{type}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response bulkDeassociation(
            @NotNull @PathParam("key") Long key,
            @NotNull @PathParam("type") ResourceDeassociationActionType type,
            @NotNull List<ResourceKey> resourceNames);

    /**
     * Executes resource-related operations on given any object.
     *
     * @param key any object id.
     * @param type resource association action type
     * @param associationMod external resources to be used for propagation-related operations
     * @return <tt>Response</tt> object featuring {@link BulkActionResult} as <tt>Entity</tt>
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring <tt>BulkActionResult</tt> as <tt>Entity</tt>")
    })
    @POST
    @Path("{key}/associate/{type}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response bulkAssociation(
            @NotNull @PathParam("key") Long key,
            @NotNull @PathParam("type") ResourceAssociationActionType type,
            @NotNull ResourceAssociationMod associationMod);

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

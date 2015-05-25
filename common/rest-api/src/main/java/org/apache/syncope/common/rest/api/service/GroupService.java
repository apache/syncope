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
import org.apache.syncope.common.lib.mod.GroupMod;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.types.ResourceAssociationActionType;
import org.apache.syncope.common.lib.types.ResourceDeassociationActionType;
import org.apache.syncope.common.lib.wrap.ResourceName;
import org.apache.syncope.common.rest.api.beans.AnyListQuery;
import org.apache.syncope.common.rest.api.beans.AnySearchQuery;

/**
 * REST operations for groups.
 */
@Path("groups")
public interface GroupService extends JAXRSService {

    /**
     * Reads the group matching the provided groupKey.
     *
     * @param groupKey key of group to be read
     * @return group with matching id
     */
    @GET
    @Path("{groupKey}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    GroupTO read(@NotNull @PathParam("groupKey") Long groupKey);

    /**
     * This method is similar to {@link #read(Long)}, but uses different authentication handling to ensure that a user
     * can read his own groups.
     *
     * @return own groups
     */
    @Descriptions({
        @Description(target = DocTarget.METHOD,
                value = "This method is similar to <tt>read()</tt>, but uses different authentication handling to "
                + "ensure that a user can read his own groups.")
    })
    @GET
    @Path("own")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    List<GroupTO> own();

    /**
     * Returns a paged list of existing groups matching the given query.
     *
     * @param listQuery query conditions
     * @return paged list of existing groups matching the given query
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    PagedResult<GroupTO> list(@BeanParam AnyListQuery listQuery);

    /**
     * Returns a paged list of groups matching the given query.
     *
     * @param searchQuery query conditions
     * @return paged list of groups matching the given query
     */
    @GET
    @Path("search")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    PagedResult<GroupTO> search(@BeanParam AnySearchQuery searchQuery);

    /**
     * Creates a new group.
     *
     * @param groupTO group to be created
     * @return <tt>Response</tt> object featuring <tt>Location</tt> header of created group as well as the group itself
     * enriched with propagation status information - {@link GroupTO} as <tt>Entity</tt>
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring <tt>Location</tt> header of created group as well as the "
                + "group itself enriched with propagation status information - <tt>GroupTO</tt> as <tt>Entity</tt>")
    })
    @POST
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response create(@NotNull GroupTO groupTO);

    /**
     * Updates group matching the provided groupKey.
     *
     * @param groupKey key of group to be updated
     * @param groupMod modification to be applied to group matching the provided groupKey
     * @return <tt>Response</tt> object featuring the updated group enriched with propagation status information
     * - {@link GroupTO} as <tt>Entity</tt>
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring the updated group enriched with propagation status information - "
                + "<tt>GroupTO</tt> as <tt>Entity</tt>")
    })
    @POST
    @Path("{groupKey}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response update(@NotNull @PathParam("groupKey") Long groupKey, @NotNull GroupMod groupMod);

    /**
     * Deletes group matching provided groupKey.
     *
     * @param groupKey key of group to be deleted
     * @return <tt>Response</tt> object featuring the deleted group enriched with propagation status information
     * - {@link GroupTO} as <tt>Entity</tt>
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring the deleted group enriched with propagation status information - "
                + "<tt>GroupTO</tt> as <tt>Entity</tt>")
    })
    @DELETE
    @Path("{groupKey}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response delete(@NotNull @PathParam("groupKey") Long groupKey);

    /**
     * Executes resource-related operations on given group.
     *
     * @param groupKey group id.
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
    @Path("{groupKey}/deassociate/{type}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response bulkDeassociation(
            @NotNull @PathParam("groupKey") Long groupKey,
            @NotNull @PathParam("type") ResourceDeassociationActionType type,
            @NotNull List<ResourceName> resourceNames);

    /**
     * Executes resource-related operations on given group.
     *
     * @param groupKey group id.
     * @param type resource association action type
     * @param resourceNames external resources to be used for propagation-related operations
     * @return <tt>Response</tt> object featuring {@link BulkActionResult} as <tt>Entity</tt>
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring <tt>BulkActionResult</tt> as <tt>Entity</tt>")
    })
    @POST
    @Path("{groupKey}/associate/{type}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response bulkAssociation(
            @NotNull @PathParam("groupKey") Long groupKey,
            @NotNull @PathParam("type") ResourceAssociationActionType type,
            @NotNull List<ResourceName> resourceNames);

    /**
     * Executes the provided bulk action.
     *
     * @param bulkAction list of group ids against which the bulk action will be performed.
     * @return Bulk action result
     */
    @POST
    @Path("bulk")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    BulkActionResult bulk(@NotNull BulkAction bulkAction);
}

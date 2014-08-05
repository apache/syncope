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

import org.apache.syncope.common.to.NotificationTO;

/**
 * REST operations for notifications.
 */
@Path("notifications")
public interface NotificationService extends JAXRSService {

    /**
     * Returns notification with matching id.
     *
     * @param notificationId id of notification to be read
     * @return notification with matching id
     */
    @GET
    @Path("{notificationId}")
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    NotificationTO read(@NotNull @PathParam("notificationId") Long notificationId);

    /**
     * Returns a list of all notifications.
     *
     * @return list of all notifications.
     */
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    List<NotificationTO> list();

    /**
     * Creates a new notification.
     *
     * @param notificationTO Creates a new notification.
     * @return <tt>Response</tt> object featuring <tt>Location</tt> header of created notification
     */
    @Descriptions({
        @Description(target = DocTarget.RESPONSE,
                value = "Featuring <tt>Location</tt> header of created notification")
    })
    @POST
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response create(@NotNull NotificationTO notificationTO);

    /**
     * Updates the notification matching the given id.
     *
     * @param notificationId id of notification to be updated
     * @param notificationTO notification to be stored
     */
    @PUT
    @Path("{notificationId}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    void update(@NotNull @PathParam("notificationId") Long notificationId, @NotNull NotificationTO notificationTO);

    /**
     * Deletes the notification matching the given id.
     *
     * @param notificationId id for notification to be deleted
     */
    @DELETE
    @Path("{notificationId}")
    void delete(@NotNull @PathParam("notificationId") Long notificationId);
}

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
import org.apache.syncope.common.lib.to.NotificationTO;

/**
 * REST operations for notifications.
 */
@Path("notifications")
public interface NotificationService extends JAXRSService {

    /**
     * Returns notification with matching id.
     *
     * @param key key of notification to be read
     * @return notification with matching key
     */
    @GET
    @Path("{key}")
    @Produces({ JAXRSService.APPLICATION_XML, MediaType.APPLICATION_JSON })
    NotificationTO read(@NotNull @PathParam("key") Long key);

    /**
     * Returns a list of all notifications.
     *
     * @return list of all notifications.
     */
    @GET
    @Produces({ JAXRSService.APPLICATION_XML, MediaType.APPLICATION_JSON })
    List<NotificationTO> list();

    /**
     * Creates a new notification.
     *
     * @param notificationTO Creates a new notification.
     * @return Response object featuring Location header of created notification
     */
    @POST
    @Consumes({ JAXRSService.APPLICATION_XML, MediaType.APPLICATION_JSON })
    Response create(@NotNull NotificationTO notificationTO);

    /**
     * Updates the notification matching the given key.
     *
     * @param notificationTO notification to be stored
     */
    @PUT
    @Path("{key}")
    @Consumes({ JAXRSService.APPLICATION_XML, MediaType.APPLICATION_JSON })
    void update(@NotNull NotificationTO notificationTO);

    /**
     * Deletes the notification matching the given key.
     *
     * @param key key for notification to be deleted
     */
    @DELETE
    @Path("{key}")
    void delete(@NotNull @PathParam("key") Long key);
}

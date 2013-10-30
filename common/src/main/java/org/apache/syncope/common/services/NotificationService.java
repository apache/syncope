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

import org.apache.syncope.common.to.NotificationTO;

@Path("notifications")
@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
public interface NotificationService {

    /**
     * @param notificationTO Creates a new notification.
     * @return <tt>Response</tt> object featuring <tt>Location</tt> header of created notification
     */
    @POST
    Response create(NotificationTO notificationTO);

    /**
     * @param notificationId ID for notification to be deleted.
     */
    @DELETE
    @Path("{notificationId}")
    void delete(@PathParam("notificationId") Long notificationId);

    /**
     * @return Returns list of all notifications.
     */
    @GET
    List<NotificationTO> list();

    /**
     * @param notificationId ID of notification to be read.
     * @return Notification with matching id.
     */
    @GET
    @Path("{notificationId}")
    NotificationTO read(@PathParam("notificationId") Long notificationId);

    /**
     * @param notificationId ID of notification to be updated.
     * @param notificationTO Notification to be stored.
     */
    @PUT
    @Path("{notificationId}")
    void update(@PathParam("notificationId") Long notificationId, NotificationTO notificationTO);

}

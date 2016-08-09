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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.NotificationTO;
import org.apache.syncope.common.lib.types.JobAction;

/**
 * REST operations for notifications.
 */
@Path("notifications")
public interface NotificationService extends JAXRSService {

    /**
     * Returns notification with matching key.
     *
     * @param key key of notification to be read
     * @return notification with matching key
     */
    @GET
    @Path("{key}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    NotificationTO read(@NotNull @PathParam("key") String key);

    /**
     * Returns a list of all notifications.
     *
     * @return list of all notifications.
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    List<NotificationTO> list();

    /**
     * Creates a new notification.
     *
     * @param notificationTO Creates a new notification.
     * @return Response object featuring Location header of created notification
     */
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    Response create(@NotNull NotificationTO notificationTO);

    /**
     * Updates the notification matching the given key.
     *
     * @param notificationTO notification to be stored
     */
    @PUT
    @Path("{key}")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    void update(@NotNull NotificationTO notificationTO);

    /**
     * Deletes the notification matching the given key.
     *
     * @param key key for notification to be deleted
     */
    @DELETE
    @Path("{key}")
    void delete(@NotNull @PathParam("key") String key);

    /**
     * Returns details about notification job.
     *
     * @return details about notification job
     */
    @GET
    @Path("job")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    JobTO getJob();

    /**
     * Executes an action on the notification job.
     *
     * @param action action to execute
     */
    @POST
    @Path("job")
    void actionJob(@QueryParam("action") JobAction action);
}

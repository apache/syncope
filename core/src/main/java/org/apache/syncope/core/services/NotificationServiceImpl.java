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
package org.apache.syncope.core.services;

import java.net.URI;
import java.util.List;

import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.syncope.common.services.NotificationService;
import org.apache.syncope.common.to.NotificationTO;
import org.apache.syncope.core.rest.controller.NotificationController;
import org.apache.syncope.core.util.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService, ContextAware {

    @Autowired
    private NotificationController notificationController;
    
    private UriInfo uriInfo;

    @Override
    public Response create(NotificationTO notificationTO) {
        try {
            NotificationTO createdNotificationTO = notificationController.createInternal(notificationTO);
            URI location = uriInfo.getAbsolutePathBuilder().path("" + createdNotificationTO.getId()).build();
            return Response.created(location).build();
        } catch (NotFoundException e) {
            throw new javax.ws.rs.NotFoundException(e);
        }
    }

    @Override
    public NotificationTO read(@PathParam("notificationId") Long notificationId) throws javax.ws.rs.NotFoundException {
        try {
            return notificationController.read(notificationId);
        } catch (NotFoundException e) {
            throw new javax.ws.rs.NotFoundException(e);
        }
    }

    @Override
    public List<NotificationTO> list() {
        return notificationController.list();
    }

    @Override
    public NotificationTO update(@PathParam("notificationId") Long notificationId,
            NotificationTO notificationTO) {
        try {
            return notificationController.update(notificationTO);
        } catch (NotFoundException e) {
            throw new javax.ws.rs.NotFoundException(e);
        }
    }

    @Override
    public NotificationTO delete(@PathParam("notificationId") Long notificationId) {
        try {
            return notificationController.delete(notificationId);
        } catch (NotFoundException e) {
            throw new javax.ws.rs.NotFoundException(e);
        }
    }

    @Override
    public void setUriInfo(final UriInfo ui) {
        this.uriInfo = ui;
    }
}

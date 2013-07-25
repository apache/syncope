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

import javax.ws.rs.core.Response;

import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.services.NotificationService;
import org.apache.syncope.common.to.NotificationTO;
import org.apache.syncope.core.rest.controller.NotificationController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl extends AbstractServiceImpl implements NotificationService, ContextAware {

    @Autowired
    private NotificationController controller;

    @Override
    public Response create(final NotificationTO notificationTO) {
        NotificationTO createdNotificationTO = controller.create(notificationTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(createdNotificationTO.getId())).build();
        return Response.created(location)
                .header(SyncopeConstants.REST_HEADER_ID, createdNotificationTO.getId())
                .build();
    }

    @Override
    public NotificationTO read(final Long notificationId) {
        return controller.read(notificationId);
    }

    @Override
    public List<NotificationTO> list() {
        return controller.list();
    }

    @Override
    public void update(final Long notificationId, final NotificationTO notificationTO) {
        controller.update(notificationTO);
    }

    @Override
    public void delete(final Long notificationId) {
        controller.delete(notificationId);
    }
}

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
package org.apache.syncope.core.rest.controller;

import java.util.ArrayList;
import java.util.List;


import org.apache.syncope.common.to.NotificationTO;
import org.apache.syncope.common.types.AuditElements.Category;
import org.apache.syncope.common.types.AuditElements.NotificationSubCategory;
import org.apache.syncope.common.types.AuditElements.Result;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.persistence.beans.Notification;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.dao.NotificationDAO;
import org.apache.syncope.core.rest.data.NotificationDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class NotificationController extends AbstractController {

    @Autowired
    private AuditManager auditManager;

    @Autowired
    private NotificationDAO notificationDAO;

    @Autowired
    private NotificationDataBinder binder;

    @PreAuthorize("hasRole('NOTIFICATION_READ')")
    public NotificationTO read(final Long notificationId) {
        Notification notification = notificationDAO.find(notificationId);
        if (notification == null) {
            LOG.error("Could not find notification '" + notificationId + "'");

            throw new NotFoundException(String.valueOf(notificationId));
        }

        return binder.getNotificationTO(notification);
    }

    @PreAuthorize("hasRole('NOTIFICATION_LIST')")
    public List<NotificationTO> list() {
        List<Notification> notifications = notificationDAO.findAll();

        List<NotificationTO> notificationTOs = new ArrayList<NotificationTO>();
        for (Notification notification : notifications) {
            notificationTOs.add(binder.getNotificationTO(notification));
        }

        auditManager.audit(Category.notification, NotificationSubCategory.list, Result.success,
                "Successfully listed all notifications: " + notificationTOs.size());

        return notificationTOs;
    }

    @PreAuthorize("hasRole('NOTIFICATION_CREATE')")
    public NotificationTO create(final NotificationTO notificationTO) {
        LOG.debug("Notification create called with parameter {}", notificationTO);

        Notification notification = notificationDAO.save(binder.createNotification(notificationTO));

        auditManager.audit(Category.notification, NotificationSubCategory.create, Result.success,
                "Successfully created notification: " + notification.getId());

        return binder.getNotificationTO(notification);
    }

    @PreAuthorize("hasRole('NOTIFICATION_UPDATE')")
    public NotificationTO update(final NotificationTO notificationTO) {
        LOG.debug("ConnNotificationtor update called with parameter {}", notificationTO);

        Notification notification = notificationDAO.find(notificationTO.getId());
        if (notification == null) {
            LOG.error("Could not find notification '" + notificationTO.getId() + "'");

            throw new NotFoundException(String.valueOf(notificationTO.getId()));
        }

        binder.updateNotification(notification, notificationTO);
        notification = notificationDAO.save(notification);

        auditManager.audit(Category.notification, NotificationSubCategory.update, Result.success,
                "Successfully updated notification: " + notification.getId());

        return binder.getNotificationTO(notification);
    }

    @PreAuthorize("hasRole('CONNECTOR_DELETE')")
    public NotificationTO delete(final Long notificationId) {
        Notification notification = notificationDAO.find(notificationId);
        if (notification == null) {
            LOG.error("Could not find notificatin '" + notificationId + "'");

            throw new NotFoundException(String.valueOf(notificationId));
        }

        NotificationTO notificationToDelete = binder.getNotificationTO(notification);

        auditManager.audit(Category.notification, NotificationSubCategory.delete, Result.success,
                "Successfully deleted notification: " + notification.getId());

        notificationDAO.delete(notificationId);

        return notificationToDelete;
    }
}

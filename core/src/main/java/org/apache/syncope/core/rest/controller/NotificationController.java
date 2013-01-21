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

import javax.servlet.http.HttpServletResponse;

import org.apache.syncope.common.to.NotificationTO;
import org.apache.syncope.common.types.AuditElements.Category;
import org.apache.syncope.common.types.AuditElements.NotificationSubCategory;
import org.apache.syncope.common.types.AuditElements.Result;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.persistence.beans.Notification;
import org.apache.syncope.core.persistence.dao.NotificationDAO;
import org.apache.syncope.core.rest.data.NotificationDataBinder;
import org.apache.syncope.core.util.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/notification")
public class NotificationController extends AbstractController {

    @Autowired
    private AuditManager auditManager;

    @Autowired
    private NotificationDAO notificationDAO;

    @Autowired
    private NotificationDataBinder binder;

    @PreAuthorize("hasRole('NOTIFICATION_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/read/{notificationId}")
    public NotificationTO read(@PathVariable("notificationId") final Long notificationId) throws NotFoundException {

        Notification notification = notificationDAO.find(notificationId);
        if (notification == null) {
            LOG.error("Could not find notification '" + notificationId + "'");

            throw new NotFoundException(String.valueOf(notificationId));
        }

        return binder.getNotificationTO(notification);
    }

    @PreAuthorize("hasRole('NOTIFICATION_LIST')")
    @RequestMapping(method = RequestMethod.GET, value = "/list")
    public List<NotificationTO> list() throws NotFoundException {

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
    @RequestMapping(method = RequestMethod.POST, value = "/create")
    public NotificationTO create(final HttpServletResponse response, @RequestBody final NotificationTO notificationTO)
            throws NotFoundException {

        LOG.debug("Notification create called with parameter {}", notificationTO);

        Notification notification = notificationDAO.save(binder.createNotification(notificationTO));

        auditManager.audit(Category.notification, NotificationSubCategory.create, Result.success,
                "Successfully created notification: " + notification.getId());

        response.setStatus(HttpServletResponse.SC_CREATED);
        return binder.getNotificationTO(notification);
    }

    @PreAuthorize("hasRole('NOTIFICATION_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/update")
    public NotificationTO update(@RequestBody final NotificationTO notificationTO) throws NotFoundException {

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
    @RequestMapping(method = RequestMethod.GET, value = "/delete/{notificationId}")
    public NotificationTO delete(@PathVariable("notificationId") final Long notificationId) throws NotFoundException {

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

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.core.rest.controller;

import java.util.ArrayList;
import java.util.List;
import javassist.NotFoundException;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.syncope.client.to.NotificationTO;
import org.syncope.core.persistence.beans.Notification;
import org.syncope.core.persistence.dao.NotificationDAO;
import org.syncope.core.rest.data.NotificationDataBinder;

@Controller
@RequestMapping("/notification")
public class NotificationController extends AbstractController {

    @Autowired
    private NotificationDAO notificationDAO;

    @Autowired
    private NotificationDataBinder binder;

    @PreAuthorize("hasRole('NOTIFICATION_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/read/{notificationId}")
    public NotificationTO read(
            @PathVariable("notificationId") Long notificationId)
            throws NotFoundException {

        Notification notification = notificationDAO.find(notificationId);
        if (notification == null) {
            LOG.error("Could not find notification '" + notificationId + "'");

            throw new NotFoundException(String.valueOf(notificationId));
        }

        return binder.getNotificationTO(notification);
    }

    @PreAuthorize("hasRole('NOTIFICATION_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/list")
    public List<NotificationTO> list()
            throws NotFoundException {

        List<Notification> notifications = notificationDAO.findAll();

        List<NotificationTO> notificationTOs =
                new ArrayList<NotificationTO>();
        for (Notification notification : notifications) {
            notificationTOs.add(binder.getNotificationTO(notification));
        }

        return notificationTOs;
    }

    @PreAuthorize("hasRole('NOTIFICATION_CREATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/create")
    public NotificationTO create(final HttpServletResponse response,
            @RequestBody final NotificationTO notificationTO)
            throws NotFoundException {

        LOG.debug("Notification create called with parameter {}",
                notificationTO);

        Notification notification = binder.createNotification(notificationTO);
        notification = notificationDAO.save(notification);

        response.setStatus(HttpServletResponse.SC_CREATED);
        return binder.getNotificationTO(notification);
    }

    @PreAuthorize("hasRole('NOTIFICATION_UPDATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/update")
    public NotificationTO update(
            @RequestBody final NotificationTO notificationTO)
            throws NotFoundException {

        LOG.debug("ConnNotificationtor update called with parameter {}",
                notificationTO);

        Notification notification =
                notificationDAO.find(notificationTO.getId());
        if (notification == null) {
            LOG.error("Could not find notification '"
                    + notificationTO.getId() + "'");

            throw new NotFoundException(String.valueOf(notificationTO.getId()));
        }

        binder.updateNotification(notification, notificationTO);
        notification = notificationDAO.save(notification);

        return binder.getNotificationTO(notification);
    }

    @PreAuthorize("hasRole('CONNECTOR_DELETE')")
    @RequestMapping(method = RequestMethod.DELETE,
    value = "/delete/{notificationId}")
    public void delete(
            @PathVariable("notificationId") final Long notificationId)
            throws NotFoundException {

        Notification notification = notificationDAO.find(notificationId);
        if (notification == null) {
            LOG.error("Could not find notificatin '" + notificationId + "'");

            throw new NotFoundException(String.valueOf(notificationId));
        }

        notificationDAO.delete(notificationId);
    }
}

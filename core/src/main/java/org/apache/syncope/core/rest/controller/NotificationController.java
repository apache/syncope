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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.to.NotificationTO;
import org.apache.syncope.core.persistence.beans.Notification;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.dao.NotificationDAO;
import org.apache.syncope.core.rest.data.NotificationDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class NotificationController extends AbstractTransactionalController<NotificationTO> {

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

        return notificationTOs;
    }

    @PreAuthorize("hasRole('NOTIFICATION_CREATE')")
    public NotificationTO create(final NotificationTO notificationTO) {
        LOG.debug("Notification create called with parameter {}", notificationTO);
        return binder.getNotificationTO(notificationDAO.save(binder.createNotification(notificationTO)));
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
        notificationDAO.delete(notificationId);
        return notificationToDelete;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NotificationTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {
        Long id = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; id == null && i < args.length; i++) {
                if (args[i] instanceof Long) {
                    id = (Long) args[i];
                } else if (args[i] instanceof NotificationTO) {
                    id = ((NotificationTO) args[i]).getId();
                }
            }
        }

        if (id != null) {
            try {
                return binder.getNotificationTO(notificationDAO.find(id));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}

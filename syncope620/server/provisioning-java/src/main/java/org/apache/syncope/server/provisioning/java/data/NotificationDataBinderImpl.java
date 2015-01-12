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
package org.apache.syncope.server.provisioning.java.data;

import org.apache.syncope.server.provisioning.api.data.NotificationDataBinder;
import org.apache.syncope.common.lib.to.NotificationTO;
import org.apache.syncope.server.persistence.api.entity.EntityFactory;
import org.apache.syncope.server.persistence.api.entity.Notification;
import org.apache.syncope.server.misc.spring.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NotificationDataBinderImpl implements NotificationDataBinder {

    private static final String[] IGNORE_PROPERTIES = { "key", "about", "recipients" };

    @Autowired
    private EntityFactory entityFactory;

    @Override
    public NotificationTO getNotificationTO(final Notification notification) {
        NotificationTO result = new NotificationTO();

        BeanUtils.copyProperties(notification, result, IGNORE_PROPERTIES);

        result.setKey(notification.getKey());
        result.setUserAbout(notification.getUserAbout());
        result.setRoleAbout(notification.getRoleAbout());
        result.setRecipients(notification.getRecipients());

        return result;
    }

    @Override
    public Notification create(final NotificationTO notificationTO) {
        Notification result = entityFactory.newEntity(Notification.class);
        update(result, notificationTO);
        return result;
    }

    @Override
    public void update(final Notification notification, final NotificationTO notificationTO) {
        BeanUtils.copyProperties(notificationTO, notification, IGNORE_PROPERTIES);

        notification.setUserAbout(notificationTO.getUserAbout());
        notification.setRoleAbout(notificationTO.getRoleAbout());
        notification.setRecipients(notificationTO.getRecipients());
    }
}

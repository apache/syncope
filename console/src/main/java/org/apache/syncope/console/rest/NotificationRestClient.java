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
package org.apache.syncope.console.rest;

import java.util.ArrayList;
import java.util.List;

import org.apache.syncope.common.services.ConfigurationService;
import org.apache.syncope.common.services.NotificationService;
import org.apache.syncope.common.to.MailTemplateTO;
import org.apache.syncope.common.to.NotificationTO;
import org.apache.syncope.common.util.CollectionWrapper;
import org.springframework.stereotype.Component;

@Component
public class NotificationRestClient extends BaseRestClient {

    private static final long serialVersionUID = 6328933265096511690L;

    public List<NotificationTO> getAllNotifications() {
        return getService(NotificationService.class).list();
    }

    public NotificationTO readNotification(final Long id) {
        return getService(NotificationService.class).read(id);
    }

    public void createNotification(final NotificationTO notificationTO) {
        getService(NotificationService.class).create(notificationTO);
    }

    public void updateNotification(final NotificationTO notificationTO) {
        getService(NotificationService.class).update(notificationTO.getId(), notificationTO);
    }

    public void deleteNotification(final Long id) {
        getService(NotificationService.class).delete(id);
    }

    public List<String> getMailTemplates() {
        return CollectionWrapper.unwrap(
                new ArrayList<MailTemplateTO>(getService(ConfigurationService.class).getMailTemplates()));
    }
}

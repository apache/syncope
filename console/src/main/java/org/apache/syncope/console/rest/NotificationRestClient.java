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

import java.util.Arrays;
import java.util.List;
import org.apache.syncope.client.to.NotificationTO;
import org.apache.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.console.SyncopeSession;
import org.springframework.stereotype.Component;

@Component
public class NotificationRestClient extends AbstractBaseRestClient {

    public List<NotificationTO> getAllNotifications()
            throws SyncopeClientCompositeErrorException {
        return Arrays.asList(SyncopeSession.get().getRestTemplate().getForObject(
                baseURL + "notification/list.json", NotificationTO[].class));
    }

    public NotificationTO readNotification(final Long id)
            throws SyncopeClientCompositeErrorException {
        return SyncopeSession.get().getRestTemplate().getForObject(
                baseURL + "notification/read/{notificationId}.json", NotificationTO.class, id);
    }

    public void createNotification(final NotificationTO notificationTO)
            throws SyncopeClientCompositeErrorException {
        SyncopeSession.get().getRestTemplate().postForObject(
                baseURL + "notification/create.json", notificationTO, NotificationTO.class);
    }

    public void updateNotification(final NotificationTO notificationTO)
            throws SyncopeClientCompositeErrorException {
        SyncopeSession.get().getRestTemplate().postForObject(
                baseURL + "notification/update.json", notificationTO, NotificationTO.class);
    }

    public void deleteNotification(final Long id)
            throws SyncopeClientCompositeErrorException {
        SyncopeSession.get().getRestTemplate().getForObject(
                baseURL + "notification/delete/{notificationId}.json", NotificationTO.class, id);
    }

    public List<String> getMailTemplates()
            throws SyncopeClientCompositeErrorException {
        return Arrays.asList(SyncopeSession.get().getRestTemplate().getForObject(
                baseURL + "configuration/mailTemplates.json", String[].class));
    }

    public List<String> getEvents()
            throws SyncopeClientCompositeErrorException {
        return Arrays.asList(SyncopeSession.get().getRestTemplate().getForObject(
                baseURL + "workflow/tasks.json", String[].class));
    }
}

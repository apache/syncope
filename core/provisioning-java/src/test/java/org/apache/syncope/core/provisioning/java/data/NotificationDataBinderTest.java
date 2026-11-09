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
package org.apache.syncope.core.provisioning.java.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.NotificationTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.core.persistence.api.dao.NotificationDAO;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.apache.syncope.core.provisioning.api.data.NotificationDataBinder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotificationDataBinderTest extends AbstractDataBinderTest {

    @Autowired
    private NotificationDataBinder binder;

    @Autowired
    private NotificationDAO notificationDAO;

    @Test
    void get() {
        NotificationTO notification = binder.getNotificationTO(
                notificationDAO.findById("9e2b911c-25de-4c77-bcea-b86ed9451050").orElseThrow());
        assertEquals("9e2b911c-25de-4c77-bcea-b86ed9451050", notification.getKey());
        assertEquals("test", notification.getTemplate());
        assertEquals("test@syncope.apache.org", notification.getSender());
        assertEquals("Test subject", notification.getSubject());
        assertEquals("email", notification.getRecipientAttrName());
        assertEquals("$groups==7", notification.getRecipientsFIQL());
        assertEquals(TraceLevel.FAILURES, notification.getTraceLevel());
        assertTrue(notification.isActive());
        assertEquals("fullname==*o*;fullname==*i*", notification.getAbouts().get(AnyTypeKind.USER.name()));
    }

    @Test
    void createUpdate() {
        NotificationTO notificationTO = new NotificationTO();
        notificationTO.setTemplate("test");
        notificationTO.getEvents().add("[CUSTOM]:[]:[]:[test]:[SUCCESS]");
        notificationTO.setRecipientAttrName("email");
        notificationTO.setSender("sender@syncope.apache.org");
        notificationTO.setSubject("Test subject");
        notificationTO.setRecipientsFIQL("$groups==7");
        notificationTO.getStaticRecipients().add("static@syncope.apache.org");
        notificationTO.setTraceLevel(TraceLevel.ALL);
        notificationTO.setActive(true);
        notificationTO.getAbouts().put(AnyTypeKind.USER.name(), "fullname==*o*");
        notificationTO.getAbouts().put("invalid", "fullname==*o*");

        Notification notification = notificationDAO.save(binder.create(notificationTO));

        NotificationTO created = binder.getNotificationTO(notification);
        assertEquals("test", created.getTemplate());
        assertEquals("sender@syncope.apache.org", created.getSender());
        assertEquals("email", created.getRecipientAttrName());
        assertEquals(List.of("static@syncope.apache.org"), created.getStaticRecipients());
        assertEquals(TraceLevel.ALL, created.getTraceLevel());
        assertEquals(Map.of(AnyTypeKind.USER.name(), "fullname==*o*"), created.getAbouts());

        created.setSubject("Updated subject");
        created.getEvents().clear();
        created.getEvents().add("[CUSTOM]:[]:[]:[test2]:[SUCCESS]");
        binder.update(notification, created);

        NotificationTO updated = binder.getNotificationTO(notification);
        assertEquals("Updated subject", updated.getSubject());
        assertEquals(List.of("[CUSTOM]:[]:[]:[test2]:[SUCCESS]"), updated.getEvents());
    }

    @Test
    void invalidTemplateAndEvents() {
        NotificationTO notificationTO = new NotificationTO();
        notificationTO.setTemplate("not-existing");
        notificationTO.setRecipientAttrName("email");
        notificationTO.setSender("sender@syncope.apache.org");
        notificationTO.setSubject("Test subject");
        assertThrows(SyncopeClientException.class, () -> binder.create(notificationTO));

        notificationTO.setTemplate("test");
        assertThrows(SyncopeClientException.class, () -> binder.create(notificationTO));
    }

    @Test
    void invalidStaticRecipientAndAttribute() {
        NotificationTO notificationTO = new NotificationTO();
        notificationTO.setTemplate("test");
        notificationTO.getEvents().add("[CUSTOM]:[]:[]:[test]:[SUCCESS]");
        notificationTO.setRecipientAttrName("email");
        notificationTO.getStaticRecipients().add("not-an-email");
        assertThrows(SyncopeClientException.class, () -> binder.create(notificationTO));

        notificationTO.getStaticRecipients().clear();
        notificationTO.setRecipientAttrName("relationships[unknown].cn");
        assertThrows(SyncopeClientException.class, () -> binder.create(notificationTO));
    }
}

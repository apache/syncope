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
package org.apache.syncope.core.persistence.neo4j.inner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.MailTemplateDAO;
import org.apache.syncope.core.persistence.api.dao.NotificationDAO;
import org.apache.syncope.core.persistence.api.entity.AnyAbout;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.apache.syncope.core.persistence.neo4j.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class NotificationTest extends AbstractTest {

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private NotificationDAO notificationDAO;

    @Autowired
    private MailTemplateDAO mailTemplateDAO;

    @Test
    public void find() {
        Notification notification = notificationDAO.findById("9e2b911c-25de-4c77-bcea-b86ed9451050").orElseThrow();
        assertNotNull(notification.getEvents());
        assertFalse(notification.getEvents().isEmpty());
        assertNotNull(notification.getAbout(anyTypeDAO.getUser()));
        assertNotNull(notification.getRecipientsFIQL());
    }

    @Test
    public void findAll() {
        List<? extends Notification> notifications = notificationDAO.findAll();
        assertNotNull(notifications);
        assertFalse(notifications.isEmpty());
    }

    @Test
    public void save() {
        Notification notification = entityFactory.newEntity(Notification.class);
        notification.getEvents().add("save");

        AnyAbout about = entityFactory.newEntity(AnyAbout.class);
        about.setNotification(notification);
        notification.add(about);
        about.setAnyType(anyTypeDAO.getUser());
        about.set("fake search condition");

        notification.setRecipientsFIQL("fake recipients");

        notification.setRecipientAttrName("email");

        notification.setSender("syncope@syncope.apache.org");
        notification.setSubject("Test notification");
        notification.setTemplate(mailTemplateDAO.findById("test").orElseThrow());

        Notification actual = notificationDAO.save(notification);
        assertNotNull(actual);
        assertNotNull(actual.getKey());
    }

    @Test
    public void delete() {
        notificationDAO.deleteById("9e2b911c-25de-4c77-bcea-b86ed9451050");
        assertTrue(notificationDAO.findById("9e2b911c-25de-4c77-bcea-b86ed9451050").isEmpty());
    }

    @Test
    public void issueSYNCOPE445() {
        Notification notification = entityFactory.newEntity(Notification.class);
        notification.getEvents().add("save");

        AnyAbout about = entityFactory.newEntity(AnyAbout.class);
        about.setNotification(notification);
        notification.add(about);
        about.setAnyType(anyTypeDAO.getUser());
        about.set("fake search condition");

        notification.setRecipientsFIQL("fake search condition");

        notification.setRecipientAttrName("email");

        notification.getStaticRecipients().add("syncope445@syncope.apache.org");

        notification.setSender("syncope@syncope.apache.org");
        notification.setSubject("Test notification");
        notification.setTemplate(mailTemplateDAO.findById("test").orElseThrow());

        notification = notificationDAO.save(notification);

        Notification actual = notificationDAO.findById(notification.getKey()).orElseThrow();
        assertNotNull(actual.getKey());
        assertNotNull(actual.getStaticRecipients());
        assertFalse(actual.getStaticRecipients().isEmpty());
    }

    @Test
    public void issueSYNCOPE446() {
        Notification notification = entityFactory.newEntity(Notification.class);
        notification.getEvents().add("[LOGIC]:[GroupLogic]:[]:[create]:[SUCCESS]");

        AnyAbout about = entityFactory.newEntity(AnyAbout.class);
        about.setNotification(notification);
        notification.add(about);
        about.setAnyType(anyTypeDAO.getUser());
        about.set("fake search condition");

        notification.setRecipientAttrName("email");

        notification.getStaticRecipients().add("syncope446@syncope.apache.org");

        notification.setSender("syncope@syncope.apache.org");
        notification.setSubject("Test notification");
        notification.setTemplate(mailTemplateDAO.findById("test").orElseThrow());

        notification = notificationDAO.save(notification);

        Notification actual = notificationDAO.findById(notification.getKey()).orElseThrow();
        assertNotNull(actual.getKey());
        assertNotNull(actual.getStaticRecipients());
        assertFalse(actual.getStaticRecipients().isEmpty());
    }
}

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
package org.apache.syncope.core.persistence.dao;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.apache.syncope.client.SyncopeClient;
import org.apache.syncope.common.types.IntMappingType;
import org.apache.syncope.core.persistence.beans.Notification;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class NotificationTest extends AbstractDAOTest {

    @Autowired
    private NotificationDAO notificationDAO;

    @Test
    public void find() {
        Notification notification = notificationDAO.find(1L);
        assertNotNull(notification);
        assertNotNull(notification.getEvents());
        assertFalse(notification.getEvents().isEmpty());
        assertNotNull(notification.getAbout());
        assertNotNull(notification.getRecipients());
    }

    @Test
    public void findAll() {
        List<Notification> notifications = notificationDAO.findAll();
        assertNotNull(notifications);
        assertFalse(notifications.isEmpty());
    }

    @Test
    public void save() {
        Notification notification = new Notification();
        notification.addEvent("save");

        notification.setAbout(SyncopeClient.getSearchConditionBuilder().
                is("fullname").equalTo("*o*").and("fullname").equalTo("*i*").query());

        notification.setRecipients(SyncopeClient.getSearchConditionBuilder().hasRoles(7L).query());

        notification.setRecipientAttrName("email");
        notification.setRecipientAttrType(IntMappingType.UserSchema);

        notification.setSender("syncope@syncope.apache.org");
        notification.setSubject("Test notification");
        notification.setTemplate("test");

        Notification actual = notificationDAO.save(notification);
        assertNotNull(actual);
        assertNotNull(actual.getId());
    }

    @Test
    public void delete() {
        notificationDAO.delete(1L);
        assertNull(notificationDAO.find(1L));
    }
}

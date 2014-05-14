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
package org.apache.syncope.core.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;

import javax.ws.rs.core.Response;
import org.apache.syncope.client.SyncopeClient;
import org.apache.syncope.common.services.NotificationService;
import org.apache.syncope.common.to.NotificationTO;
import org.apache.syncope.common.types.IntMappingType;
import org.apache.syncope.common.types.ClientExceptionType;
import org.apache.syncope.common.types.TraceLevel;
import org.apache.syncope.common.SyncopeClientException;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class NotificationTestITCase extends AbstractTest {

    private NotificationTO buildNotificationTO() {
        NotificationTO notificationTO = new NotificationTO();
        notificationTO.setTraceLevel(TraceLevel.SUMMARY);
        notificationTO.getEvents().add("create");

        notificationTO.setAbout(SyncopeClient.getUserSearchConditionBuilder().
                is("fullname").equalTo("*o*").and("fullname").equalTo("*i*").query());

        notificationTO.setRecipientAttrName("email");
        notificationTO.setRecipientAttrType(IntMappingType.UserSchema);
        
        notificationTO.setSender("syncope@syncope.apache.org");
        notificationTO.setSubject("Test notification");
        notificationTO.setTemplate("test");
        return notificationTO;
    }

    @Test
    public void read() {
        NotificationTO notificationTO = notificationService.read(1L);
        assertNotNull(notificationTO);
    }

    @Test
    public void list() {
        List<NotificationTO> notificationTOs = notificationService.list();
        assertNotNull(notificationTOs);
        assertFalse(notificationTOs.isEmpty());
        for (NotificationTO instance : notificationTOs) {
            assertNotNull(instance);
        }
    }

    @Test
    public void create() {
        NotificationTO notificationTO = buildNotificationTO();
        notificationTO.setRecipients(SyncopeClient.getUserSearchConditionBuilder().hasRoles(7L).query());

        Response response = notificationService.create(notificationTO);
        NotificationTO actual = getObject(response.getLocation(), NotificationService.class,
                NotificationTO.class);

        assertNotNull(actual);
        assertNotNull(actual.getId());
        notificationTO.setId(actual.getId());
        assertEquals(actual, notificationTO);
    }

    @Test
    public void update() {
        NotificationTO notificationTO = notificationService.read(1L);
        notificationTO.setRecipients(SyncopeClient.getUserSearchConditionBuilder().hasRoles(7L).query());

        notificationService.update(notificationTO.getId(), notificationTO);
        NotificationTO actual = notificationService.read(notificationTO.getId());
        assertNotNull(actual);
        assertEquals(actual, notificationTO);
    }

    @Test
    public void delete() {
        NotificationTO notification = buildNotificationTO();
        notification.setSelfAsRecipient(true);
        Response response = notificationService.create(notification);
        notification = getObject(response.getLocation(), NotificationService.class, NotificationTO.class);

        notificationService.delete(notification.getId());

        try {
            notificationService.read(notification.getId());
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

    @Test
    public void issueSYNCOPE83() {
        NotificationTO notificationTO = buildNotificationTO();
        notificationTO.setSelfAsRecipient(true);

        NotificationTO actual = null;
        try {
            Response response = notificationService.create(notificationTO);
            actual = getObject(response.getLocation(), NotificationService.class, NotificationTO.class);
        } catch (SyncopeClientException e) {
            assertNotNull(e);
        }
        assertNotNull(actual);
        assertNotNull(actual.getId());
        notificationTO.setId(actual.getId());
        assertEquals(actual, notificationTO);
    }

    @Test
    public void issueSYNCOPE445() {
        NotificationTO notificationTO = buildNotificationTO();
        notificationTO.getStaticRecipients().add("syncope445@syncope.apache.org");

        NotificationTO actual = null;
        try {
            Response response = notificationService.create(notificationTO);
            actual = getObject(response.getLocation(), NotificationService.class, NotificationTO.class);
        } catch (SyncopeClientException e) {
            assertNotNull(e);
        }
        assertNotNull(actual);
        assertNotNull(actual.getId());
        notificationTO.setId(actual.getId());
        assertEquals(actual, notificationTO);
    }

}

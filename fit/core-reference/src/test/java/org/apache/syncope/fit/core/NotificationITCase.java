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
package org.apache.syncope.fit.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.NotificationTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.common.rest.api.service.NotificationService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.Test;

public class NotificationITCase extends AbstractITCase {

    private NotificationTO buildNotificationTO() {
        NotificationTO notificationTO = new NotificationTO();
        notificationTO.setTraceLevel(TraceLevel.SUMMARY);
        notificationTO.getEvents().add("create");

        notificationTO.getAbouts().put(AnyTypeKind.USER.name(),
                SyncopeClient.getUserSearchConditionBuilder().
                        is("fullname").equalTo("*o*").and("fullname").equalTo("*i*").query());

        notificationTO.setRecipientAttrName("email");

        notificationTO.setSender("syncope@syncope.apache.org");
        notificationTO.setSubject("Test notification");
        notificationTO.setTemplate("test");
        return notificationTO;
    }

    @Test
    public void read() {
        NotificationTO notificationTO = notificationService.read(
                "9e2b911c-25de-4c77-bcea-b86ed9451050");
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
        notificationTO.setRecipientsFIQL(SyncopeClient.getUserSearchConditionBuilder().
                inGroups("bf825fe1-7320-4a54-bd64-143b5c18ab97").query());

        Response response = notificationService.create(notificationTO);
        NotificationTO actual = getObject(response.getLocation(), NotificationService.class, NotificationTO.class);

        assertNotNull(actual);
        assertNotNull(actual.getKey());
        notificationTO.setKey(actual.getKey());
        assertEquals(actual, notificationTO);
    }

    @Test
    public void update() {
        NotificationTO notificationTO = notificationService.read(
                "9e2b911c-25de-4c77-bcea-b86ed9451050");
        notificationTO.setRecipientsFIQL(SyncopeClient.getUserSearchConditionBuilder().inGroups(
                "bf825fe1-7320-4a54-bd64-143b5c18ab97").query());

        notificationService.update(notificationTO);
        NotificationTO actual = notificationService.read(notificationTO.getKey());
        assertNotNull(actual);
        assertEquals(actual, notificationTO);
    }

    @Test
    public void delete() {
        NotificationTO notification = buildNotificationTO();
        notification.setSelfAsRecipient(true);
        Response response = notificationService.create(notification);
        notification = getObject(response.getLocation(), NotificationService.class, NotificationTO.class);

        notificationService.delete(notification.getKey());

        try {
            notificationService.read(notification.getKey());
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
        assertNotNull(actual.getKey());
        notificationTO.setKey(actual.getKey());
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
        assertNotNull(actual.getKey());
        notificationTO.setKey(actual.getKey());
        assertEquals(actual, notificationTO);
    }

    @Test
    public void issueSYNCOPE446() {
        NotificationTO notificationTO = buildNotificationTO();
        notificationTO.getStaticRecipients().add("syncope446@syncope.apache.org");
        notificationTO.getAbouts().put(AnyTypeKind.GROUP.name(),
                SyncopeClient.getGroupSearchConditionBuilder().is("name").equalTo("citizen").query());

        NotificationTO actual = null;
        try {
            Response response = notificationService.create(notificationTO);
            actual = getObject(response.getLocation(), NotificationService.class, NotificationTO.class);
        } catch (SyncopeClientException e) {
            assertNotNull(e);
        }
        assertNotNull(actual);
        assertNotNull(actual.getKey());
        notificationTO.setKey(actual.getKey());
        assertEquals(actual, notificationTO);
    }

    @Test
    public void issueSYNCOPE974() {
        NotificationTO notificationTO = new NotificationTO();
        notificationTO.setRecipientAttrName("email");
        notificationTO.setSelfAsRecipient(false);
        notificationTO.setSender("sender@ukr.net");
        notificationTO.setSubject("subject 21");
        notificationTO.setTemplate("requestPasswordReset");
        notificationTO.setTraceLevel(TraceLevel.ALL);
        notificationTO.setActive(true);

        try {
            notificationService.create(notificationTO);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.RequiredValuesMissing, e.getType());
            assertTrue(e.getMessage().contains("events"));
        }
    }
}

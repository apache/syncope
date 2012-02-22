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
package org.syncope.core.rest;

import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

import org.junit.Test;
import org.syncope.client.search.AttributeCond;
import org.syncope.client.search.MembershipCond;
import org.syncope.client.search.NodeCond;
import org.syncope.client.to.NotificationTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.types.SyncopeClientExceptionType;
import org.syncope.types.TraceLevel;

public class NotificationTestITCase extends AbstractTest {

    @Test
    public void read() {
        NotificationTO notificationTO = restTemplate.getForObject(
                BASE_URL + "notification/read/{notificationId}.json",
                NotificationTO.class, "100");

        assertNotNull(notificationTO);
    }

    @Test
    public void list() {
        List<NotificationTO> notificationTOs = Arrays.asList(
                restTemplate.getForObject(
                BASE_URL + "notification/list.json", NotificationTO[].class));
        assertNotNull(notificationTOs);
        assertFalse(notificationTOs.isEmpty());
        for (NotificationTO instance : notificationTOs) {
            assertNotNull(instance);
        }
    }

    @Test
    public void create() {
        NotificationTO notificationTO = new NotificationTO();
        notificationTO.setTraceLevel(TraceLevel.SUMMARY);
        notificationTO.addEvent("create");

        AttributeCond fullnameLeafCond1 =
                new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond1.setSchema("fullname");
        fullnameLeafCond1.setExpression("%o%");
        AttributeCond fullnameLeafCond2 =
                new AttributeCond(AttributeCond.Type.LIKE);
        fullnameLeafCond2.setSchema("fullname");
        fullnameLeafCond2.setExpression("%i%");
        NodeCond about = NodeCond.getAndCond(
                NodeCond.getLeafCond(fullnameLeafCond1),
                NodeCond.getLeafCond(fullnameLeafCond2));

        notificationTO.setAbout(about);

        MembershipCond membCond = new MembershipCond();
        membCond.setRoleId(7L);
        NodeCond recipients = NodeCond.getLeafCond(membCond);

        notificationTO.setRecipients(recipients);

        notificationTO.setSender("syncope@syncope-idm.org");
        notificationTO.setSubject("Test notification");
        notificationTO.setTemplate("test");

        NotificationTO actual = restTemplate.postForObject(
                BASE_URL + "notification/create.json",
                notificationTO, NotificationTO.class);
        assertNotNull(actual);
        assertNotNull(actual.getId());
        notificationTO.setId(actual.getId());
        assertEquals(actual, notificationTO);
    }

    @Test
    public void update() {
        NotificationTO notificationTO = restTemplate.getForObject(
                BASE_URL + "notification/read/{notificationId}.json",
                NotificationTO.class, "100");
        assertNotNull(notificationTO);

        notificationTO.setRecipients(
                NodeCond.getLeafCond(new MembershipCond()));

        SyncopeClientException exception = null;
        try {
            restTemplate.postForObject(
                    BASE_URL + "notification/update.json",
                    notificationTO, NotificationTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            exception = e.getException(
                    SyncopeClientExceptionType.InvalidNotification);
        }
        assertNotNull(exception);

        MembershipCond membCond = new MembershipCond();
        membCond.setRoleId(7L);
        NodeCond recipients = NodeCond.getLeafCond(membCond);

        notificationTO.setRecipients(recipients);

        NotificationTO actual = restTemplate.postForObject(
                BASE_URL + "notification/update.json",
                notificationTO, NotificationTO.class);
        assertNotNull(actual);
        assertEquals(actual, notificationTO);
    }

    @Test
    public void delete() {
        restTemplate.delete(
                BASE_URL + "notification/delete/{notificationId}.json", "101");

        SyncopeClientException exception = null;
        try {
            restTemplate.getForObject(
                    BASE_URL + "notification/read/{notificationId}.json",
                    NotificationTO.class, "101");
        } catch (SyncopeClientCompositeErrorException e) {
            exception = e.getException(SyncopeClientExceptionType.NotFound);
        }
        assertNotNull(exception);
    }
}

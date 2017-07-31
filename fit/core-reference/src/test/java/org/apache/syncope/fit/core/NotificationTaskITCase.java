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

import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.NotificationTaskTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.NotificationTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.common.rest.api.beans.ExecuteQuery;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.NotificationService;
import org.apache.syncope.core.provisioning.java.job.notification.NotificationJob;
import org.apache.syncope.fit.core.reference.TestNotificationRecipientsProvider;
import org.junit.Test;

public class NotificationTaskITCase extends AbstractNotificationTaskITCase {

    @Test
    public void notifyByMail() throws Exception {
        String sender = "syncopetest-" + getUUIDString() + "@syncope.apache.org";
        String subject = "Test notification " + getUUIDString();
        Pair<String, String> created = createNotificationTask(true, true, TraceLevel.ALL, sender, subject);
        NotificationTaskTO taskTO = findNotificationTask(created.getLeft(), 50);
        assertNotNull(taskTO);
        assertNotNull(taskTO.getNotification());
        assertTrue(taskTO.getExecutions().isEmpty());

        execNotificationTask(taskService, taskTO.getKey(), 50);

        assertTrue(verifyMail(sender, subject, created.getRight()));

        // verify message body
        taskTO = taskService.read(taskTO.getKey(), true);
        assertNotNull(taskTO);
        assertTrue(taskTO.isExecuted());
        assertNotNull(taskTO.getTextBody());
        assertTrue("Notification mail text doesn't contain expected content.",
                taskTO.getTextBody().contains("Your email address is " + created.getRight() + "."));
        assertTrue("Notification mail text doesn't contain expected content.",
                taskTO.getTextBody().contains("Your email address inside a link: "
                        + "http://localhost/?email=" + created.getRight().replaceAll("@", "%40")));
    }

    @Test
    public void notifyByMailEmptyAbout() throws Exception {
        String sender = "syncopetest-" + getUUIDString() + "@syncope.apache.org";
        String subject = "Test notification " + getUUIDString();
        Pair<String, String> created = createNotificationTask(true, false, TraceLevel.ALL, sender, subject);
        NotificationTaskTO taskTO = findNotificationTask(created.getLeft(), 50);
        assertNotNull(taskTO);
        assertTrue(taskTO.getExecutions().isEmpty());

        execNotificationTask(taskService, taskTO.getKey(), 50);

        assertTrue(verifyMail(sender, subject, created.getRight()));
    }

    @Test
    public void notifyByMailWithRetry() throws Exception {
        // 1. Set higher number of retries
        AttrTO origMaxRetries = configurationService.get("notification.maxRetries");

        configurationService.set(attrTO(origMaxRetries.getSchema(), "10"));

        // 2. Stop mail server to force errors while sending out e-mails
        stopGreenMail();

        try {
            // 3. create notification and user
            String sender = "syncopetest-" + getUUIDString() + "@syncope.apache.org";
            String subject = "Test notification " + getUUIDString();
            Pair<String, String> created = createNotificationTask(true, true, TraceLevel.ALL, sender, subject);
            NotificationTaskTO taskTO = findNotificationTask(created.getLeft(), 50);
            assertNotNull(taskTO);
            assertNotNull(taskTO.getNotification());
            int preExecs = taskTO.getExecutions().size();

            // 4. verify notification could not be delivered
            execTask(taskService, taskTO.getKey(), NotificationJob.Status.NOT_SENT.name(), 5, false);

            taskTO = taskService.read(taskTO.getKey(), true);
            assertNotNull(taskTO);
            assertFalse(taskTO.isExecuted());
            assertTrue(preExecs <= taskTO.getExecutions().size());
            for (ExecTO exec : taskTO.getExecutions()) {
                assertEquals(NotificationJob.Status.NOT_SENT.name(), exec.getStatus());
            }
        } finally {
            // start mail server again
            startGreenMail();
            // reset number of retries
            configurationService.set(origMaxRetries);
        }
    }

    @Test
    public void issueSYNCOPE81() {
        String sender = "syncope81@syncope.apache.org";
        Pair<String, String> created = createNotificationTask(true, true, TraceLevel.ALL, sender, "Test notification");
        NotificationTaskTO taskTO = findNotificationTask(created.getLeft(), 50);
        assertNotNull(taskTO);
        assertNotNull(taskTO.getNotification());
        assertTrue(taskTO.getExecutions().isEmpty());

        // generate an execution in order to verify the deletion of a notification task with one or more executions
        execNotificationTask(taskService, taskTO.getKey(), 50);

        taskTO = taskService.read(taskTO.getKey(), true);
        assertTrue(taskTO.isExecuted());
        assertFalse(taskTO.getExecutions().isEmpty());

        taskService.delete(taskTO.getKey());
    }

    @Test
    public void issueSYNCOPE86() {
        // 1. create notification task
        String sender = "syncope86@syncope.apache.org";
        Pair<String, String> created = createNotificationTask(true, true, TraceLevel.ALL, sender, "Test notification");

        // 2. get NotificationTaskTO for user just created
        NotificationTaskTO taskTO = findNotificationTask(created.getLeft(), 50);
        assertNotNull(taskTO);
        assertNotNull(taskTO.getNotification());
        assertTrue(taskTO.getExecutions().isEmpty());

        try {
            // 3. execute the generated NotificationTask
            execNotificationTask(taskService, taskTO.getKey(), 50);

            // 4. verify
            taskTO = taskService.read(taskTO.getKey(), true);
            assertNotNull(taskTO);
            assertTrue(taskTO.isExecuted());
            assertEquals(1, taskTO.getExecutions().size());
        } finally {
            // Remove execution to make test re-runnable
            taskService.deleteExecution(taskTO.getExecutions().get(0).getKey());
        }
    }

    @Test
    public void issueSYNCOPE192() throws Exception {
        String sender = "syncopetest-" + getUUIDString() + "@syncope.apache.org";
        String subject = "Test notification " + getUUIDString();
        Pair<String, String> created = createNotificationTask(true, true, TraceLevel.NONE, sender, subject);
        NotificationTaskTO taskTO = findNotificationTask(created.getLeft(), 50);
        assertNotNull(taskTO);
        assertNotNull(taskTO.getNotification());
        assertTrue(taskTO.getExecutions().isEmpty());

        taskService.execute(new ExecuteQuery.Builder().key(taskTO.getKey()).build());

        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
        }

        assertTrue(verifyMail(sender, subject, created.getRight()));

        // verify that last exec status was updated
        taskTO = taskService.read(taskTO.getKey(), true);
        assertNotNull(taskTO);
        assertTrue(taskTO.isExecuted());
        assertTrue(taskTO.getExecutions().isEmpty());
        assertTrue(StringUtils.isNotBlank(taskTO.getLatestExecStatus()));
    }

    @Test
    public void issueSYNCOPE445() throws Exception {
        String sender = "syncopetest-" + getUUIDString() + "@syncope.apache.org";
        String subject = "Test notification " + getUUIDString();
        Pair<String, String> created = createNotificationTask(
                true, true, TraceLevel.ALL, sender, subject, "syncope445@syncope.apache.org");
        NotificationTaskTO taskTO = findNotificationTask(created.getLeft(), 50);
        assertNotNull(taskTO);
        assertNotNull(taskTO.getNotification());
        assertTrue(taskTO.getExecutions().isEmpty());

        execNotificationTask(taskService, taskTO.getKey(), 50);

        assertTrue(verifyMail(sender, subject, created.getRight()));

        // verify task
        taskTO = taskService.read(taskTO.getKey(), true);
        assertTrue(taskTO.isExecuted());
        assertNotNull(taskTO);
        assertTrue(taskTO.getRecipients().contains("syncope445@syncope.apache.org"));
    }

    @Test
    public void issueSYNCOPE446() throws Exception {
        // 1. Create notification
        NotificationTO notification = new NotificationTO();
        notification.setTraceLevel(TraceLevel.ALL);
        notification.getEvents().add("[LOGIC]:[GroupLogic]:[]:[create]:[SUCCESS]");

        String groupName = "group" + getUUIDString();
        notification.getAbouts().put(AnyTypeKind.GROUP.name(),
                SyncopeClient.getGroupSearchConditionBuilder().is("name").equalTo(groupName).query());

        notification.setRecipientsFIQL(SyncopeClient.getUserSearchConditionBuilder().
                inGroups("f779c0d4-633b-4be5-8f57-32eb478a3ca5").query());
        notification.setSelfAsRecipient(false);
        notification.setRecipientAttrName("email");
        notification.getStaticRecipients().add("notificationtest@syncope.apache.org");
        notification.setRecipientsProviderClassName(TestNotificationRecipientsProvider.class.getName());

        String sender = "syncopetest-" + getUUIDString() + "@syncope.apache.org";
        notification.setSender(sender);
        String subject = "Test notification " + getUUIDString();
        notification.setSubject(subject);
        notification.setTemplate("optin");
        notification.setActive(true);

        Response response = notificationService.create(notification);
        notification = getObject(response.getLocation(), NotificationService.class, NotificationTO.class);
        assertNotNull(notification);
        assertEquals(TestNotificationRecipientsProvider.class.getName(), notification.getRecipientsProviderClassName());

        // 2. create group
        GroupTO groupTO = new GroupTO();
        groupTO.setName(groupName);
        groupTO.setRealm("/even/two");
        groupTO = createGroup(groupTO).getEntity();
        assertNotNull(groupTO);

        // 3. verify
        NotificationTaskTO taskTO = findNotificationTask(notification.getKey(), 50);
        assertNotNull(taskTO);
        assertNotNull(taskTO.getNotification());
        assertTrue(taskTO.getRecipients().containsAll(
                new TestNotificationRecipientsProvider().provideRecipients(null)));

        NotificationTaskTO foundViaList = taskService.<NotificationTaskTO>list(
                new TaskQuery.Builder(TaskType.NOTIFICATION).notification(notification.getKey()).build()).
                getResult().get(0);
        assertEquals(taskTO, foundViaList);

        execNotificationTask(taskService, taskTO.getKey(), 50);

        assertTrue(verifyMail(sender, subject, "notificationtest@syncope.apache.org"));
    }

    @Test
    public void issueSYNCOPE492() throws Exception {
        String sender = "syncopetest-" + getUUIDString() + "@syncope.apache.org";
        String subject = "Test notification " + getUUIDString();
        Pair<String, String> created =
                createNotificationTask(false, true, TraceLevel.NONE, sender, subject, "syncope445@syncope.apache.org");

        // verify that no task was created for disabled notification
        PagedResult<NotificationTaskTO> tasks =
                taskService.list(new TaskQuery.Builder(TaskType.NOTIFICATION).notification(created.getLeft()).build());
        assertEquals(0, tasks.getSize());
    }

}

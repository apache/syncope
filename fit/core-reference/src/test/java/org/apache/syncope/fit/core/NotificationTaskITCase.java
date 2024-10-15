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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.NotificationTO;
import org.apache.syncope.common.lib.to.NotificationTaskTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.ExecSpecs;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.NotificationService;
import org.apache.syncope.core.provisioning.java.job.notification.NotificationJob;
import org.apache.syncope.fit.core.reference.TestNotificationRecipientsProvider;
import org.junit.jupiter.api.Test;

public class NotificationTaskITCase extends AbstractNotificationTaskITCase {

    @Test
    public void notifyByMail() throws Exception {
        String sender = "syncopetest-" + getUUIDString() + "@syncope.apache.org";
        String subject = "Test notification " + getUUIDString();
        Pair<String, String> created = createNotificationTask(true, true, TraceLevel.ALL, sender, subject);
        NotificationTaskTO taskTO = findNotificationTask(created.getLeft(), MAX_WAIT_SECONDS);
        assertNotNull(taskTO);
        assertNotNull(taskTO.getNotification());

        execNotificationTask(TASK_SERVICE, taskTO.getKey(), MAX_WAIT_SECONDS);

        verifyMail(sender, subject, created.getRight(), MAX_WAIT_SECONDS);

        // verify message body
        taskTO = TASK_SERVICE.read(TaskType.NOTIFICATION, taskTO.getKey(), true);
        assertNotNull(taskTO);
        assertTrue(taskTO.isExecuted());
        assertNotNull(taskTO.getTextBody());
        assertTrue(taskTO.getTextBody().contains("Your email address is " + created.getRight() + '.'));
        assertTrue(taskTO.getTextBody().contains("Your email address inside a link: "
                + "http://localhost/?email=" + created.getRight().replaceAll("@", "%40")));
    }

    @Test
    public void notifyByMailEmptyAbout() throws Exception {
        String sender = "syncopetest-" + getUUIDString() + "@syncope.apache.org";
        String subject = "Test notification " + getUUIDString();
        Pair<String, String> created = createNotificationTask(true, false, TraceLevel.ALL, sender, subject);
        NotificationTaskTO taskTO = findNotificationTask(created.getLeft(), MAX_WAIT_SECONDS);
        assertNotNull(taskTO);

        execNotificationTask(TASK_SERVICE, taskTO.getKey(), MAX_WAIT_SECONDS);

        verifyMail(sender, subject, created.getRight(), MAX_WAIT_SECONDS);
    }

    @Test
    public void notifyByMailWithRetry() throws Exception {
        // 1. Set higher number of retries
        Long origMaxRetries = confParamOps.get(SyncopeConstants.MASTER_DOMAIN,
                "notification.maxRetries", null, Long.class);

        confParamOps.set(SyncopeConstants.MASTER_DOMAIN, "notification.maxRetries", 10);

        // 2. Stop mail server to force errors while sending out e-mails
        WebClient.create(BUILD_TOOLS_ADDRESS + "/rest/greenMail/stop").post(null);

        try {
            // 3. create notification and user
            String sender = "syncopetest-" + getUUIDString() + "@syncope.apache.org";
            String subject = "Test notification " + getUUIDString();
            Pair<String, String> created = createNotificationTask(true, true, TraceLevel.ALL, sender, subject);
            NotificationTaskTO taskTO = findNotificationTask(created.getLeft(), MAX_WAIT_SECONDS);
            assertNotNull(taskTO);
            assertNotNull(taskTO.getNotification());
            int preExecs = taskTO.getExecutions().size();

            // 4. verify notification could not be delivered
            execTask(TASK_SERVICE, TaskType.NOTIFICATION, taskTO.getKey(), NotificationJob.Status.NOT_SENT.name(), 5,
                    false);

            taskTO = TASK_SERVICE.read(TaskType.NOTIFICATION, taskTO.getKey(), true);
            assertNotNull(taskTO);
            assertFalse(taskTO.isExecuted());
            assertTrue(preExecs <= taskTO.getExecutions().size());
            for (ExecTO exec : taskTO.getExecutions()) {
                assertEquals(NotificationJob.Status.NOT_SENT.name(), exec.getStatus());
            }
        } finally {
            // start mail server again
            WebClient.create(BUILD_TOOLS_ADDRESS + "/rest/greenMail/start").post(null);
            // reset number of retries
            confParamOps.set(SyncopeConstants.MASTER_DOMAIN, "notification.maxRetries", origMaxRetries);
        }
    }

    @Test
    public void issueSYNCOPE81() {
        String sender = "syncope81@syncope.apache.org";
        Pair<String, String> created = createNotificationTask(true, true, TraceLevel.ALL, sender, "Test notification");
        NotificationTaskTO taskTO = findNotificationTask(created.getLeft(), MAX_WAIT_SECONDS);
        assertNotNull(taskTO);
        assertNotNull(taskTO.getNotification());

        if (taskTO.getExecutions().isEmpty()) {
            // generate an execution in order to verify the deletion of a notification task with one or more executions
            execNotificationTask(TASK_SERVICE, taskTO.getKey(), MAX_WAIT_SECONDS);
        }

        taskTO = TASK_SERVICE.read(TaskType.NOTIFICATION, taskTO.getKey(), true);
        assertTrue(taskTO.isExecuted());
        assertFalse(taskTO.getExecutions().isEmpty());

        TASK_SERVICE.delete(TaskType.NOTIFICATION, taskTO.getKey());
    }

    @Test
    public void issueSYNCOPE86() {
        // 1. create notification task
        String sender = "syncope86@syncope.apache.org";
        Pair<String, String> created = createNotificationTask(true, true, TraceLevel.ALL, sender, "Test notification");

        // 2. get NotificationTaskTO for user just created
        NotificationTaskTO taskTO = findNotificationTask(created.getLeft(), MAX_WAIT_SECONDS);
        assertNotNull(taskTO);
        assertNotNull(taskTO.getNotification());

        try {
            // 3. execute the generated NotificationTask if needed
            if (taskTO.getExecutions().isEmpty()) {
                execNotificationTask(TASK_SERVICE, taskTO.getKey(), MAX_WAIT_SECONDS);
            }

            // 4. verify
            taskTO = TASK_SERVICE.read(TaskType.NOTIFICATION, taskTO.getKey(), true);
            assertNotNull(taskTO);
            assertTrue(taskTO.isExecuted());
            assertFalse(taskTO.getExecutions().isEmpty());
        } finally {
            // Remove execution to make test re-runnable
            taskTO.getExecutions().forEach(e -> TASK_SERVICE.deleteExecution(e.getKey()));
        }
    }

    @Test
    public void issueSYNCOPE192() throws Exception {
        String sender = "syncopetest-" + getUUIDString() + "@syncope.apache.org";
        String subject = "Test notification " + getUUIDString();
        Pair<String, String> created = createNotificationTask(true, true, TraceLevel.NONE, sender, subject);
        NotificationTaskTO taskTO = findNotificationTask(created.getLeft(), MAX_WAIT_SECONDS);
        assertNotNull(taskTO);
        assertNotNull(taskTO.getNotification());

        if (taskTO.getExecutions().isEmpty()) {
            TASK_SERVICE.execute(new ExecSpecs.Builder().key(taskTO.getKey()).build());
        }

        verifyMail(sender, subject, created.getRight(), MAX_WAIT_SECONDS);

        // verify that last exec status was updated
        taskTO = TASK_SERVICE.read(TaskType.NOTIFICATION, taskTO.getKey(), true);
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
        NotificationTaskTO taskTO = findNotificationTask(created.getLeft(), MAX_WAIT_SECONDS);
        assertNotNull(taskTO);
        assertNotNull(taskTO.getNotification());

        if (taskTO.getExecutions().isEmpty()) {
            execNotificationTask(TASK_SERVICE, taskTO.getKey(), MAX_WAIT_SECONDS);
        }

        verifyMail(sender, subject, created.getRight(), MAX_WAIT_SECONDS);

        // verify task
        taskTO = TASK_SERVICE.read(TaskType.NOTIFICATION, taskTO.getKey(), true);
        assertTrue(taskTO.isExecuted());
        assertNotNull(taskTO);
        assertTrue(taskTO.getRecipients().contains("syncope445@syncope.apache.org"));
    }

    @Test
    public void issueSYNCOPE446() throws Exception {
        // 1. Create notification
        ImplementationTO recipientsProvider = new ImplementationTO();
        recipientsProvider.setKey(TestNotificationRecipientsProvider.class.getSimpleName());
        recipientsProvider.setEngine(ImplementationEngine.JAVA);
        recipientsProvider.setType(IdRepoImplementationType.RECIPIENTS_PROVIDER);
        recipientsProvider.setBody(TestNotificationRecipientsProvider.class.getName());
        Response response = IMPLEMENTATION_SERVICE.create(recipientsProvider);
        recipientsProvider = IMPLEMENTATION_SERVICE.read(
                recipientsProvider.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
        assertNotNull(recipientsProvider);

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
        notification.setRecipientsProvider(recipientsProvider.getKey());

        String sender = "syncopetest-" + getUUIDString() + "@syncope.apache.org";
        notification.setSender(sender);
        String subject = "Test notification " + getUUIDString();
        notification.setSubject(subject);
        notification.setTemplate("optin");
        notification.setActive(true);

        response = NOTIFICATION_SERVICE.create(notification);
        notification = getObject(response.getLocation(), NotificationService.class, NotificationTO.class);
        assertNotNull(notification);
        assertEquals(recipientsProvider.getKey(), notification.getRecipientsProvider());

        // 2. create group
        GroupCR groupCR = new GroupCR();
        groupCR.setName(groupName);
        groupCR.setRealm("/even/two");
        GroupTO groupTO = createGroup(groupCR).getEntity();
        assertNotNull(groupTO);

        // 3. verify
        NotificationTaskTO taskTO = findNotificationTask(notification.getKey(), MAX_WAIT_SECONDS);
        assertNotNull(taskTO);
        assertNotNull(taskTO.getNotification());
        assertTrue(taskTO.getRecipients().containsAll(
                new TestNotificationRecipientsProvider().provideRecipients(null, null, null)));

        execNotificationTask(TASK_SERVICE, taskTO.getKey(), MAX_WAIT_SECONDS);

        verifyMail(sender, subject, "notificationtest@syncope.apache.org", MAX_WAIT_SECONDS);
    }

    @Test
    public void issueSYNCOPE492() throws Exception {
        String sender = "syncopetest-" + getUUIDString() + "@syncope.apache.org";
        String subject = "Test notification " + getUUIDString();
        Pair<String, String> created =
                createNotificationTask(false, true, TraceLevel.NONE, sender, subject, "syncope445@syncope.apache.org");

        // verify that no task was created for disabled notification
        PagedResult<NotificationTaskTO> tasks = TASK_SERVICE.search(new TaskQuery.Builder(TaskType.NOTIFICATION).
                notification(created.getLeft()).build());
        assertEquals(0, tasks.getSize());
    }
}

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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import java.io.InputStream;
import java.util.Properties;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.ws.rs.core.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.NotificationTO;
import org.apache.syncope.common.lib.to.NotificationTaskTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.common.rest.api.beans.ExecuteQuery;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.NotificationService;
import org.apache.syncope.core.logic.notification.NotificationJob;
import org.apache.syncope.fit.core.reference.TestNotificationRecipientsProvider;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class NotificationTaskITCase extends AbstractTaskITCase {

    private static final String MAIL_ADDRESS = "notificationtest@syncope.apache.org";

    private static final String POP3_HOST = "localhost";

    private static final int POP3_PORT = 1110;

    private static String SMTP_HOST;

    private static int SMTP_PORT;

    private static GreenMail greenMail;

    @BeforeClass
    public static void startGreenMail() {
        Properties props = new Properties();
        InputStream propStream = null;
        try {
            propStream = ExceptionMapperITCase.class.getResourceAsStream("/mail.properties");
            props.load(propStream);
        } catch (Exception e) {
            LOG.error("Could not load /mail.properties", e);
        } finally {
            IOUtils.closeQuietly(propStream);
        }

        SMTP_HOST = props.getProperty("smtpHost");
        assertNotNull(SMTP_HOST);
        SMTP_PORT = Integer.parseInt(props.getProperty("smtpPort"));
        assertNotNull(SMTP_PORT);

        ServerSetup[] config = new ServerSetup[2];
        config[0] = new ServerSetup(SMTP_PORT, SMTP_HOST, ServerSetup.PROTOCOL_SMTP);
        config[1] = new ServerSetup(POP3_PORT, POP3_HOST, ServerSetup.PROTOCOL_POP3);
        greenMail = new GreenMail(config);
        greenMail.start();
    }

    @AfterClass
    public static void stopGreenMail() {
        if (greenMail != null) {
            greenMail.stop();
        }
    }

    private boolean verifyMail(final String sender, final String subject, final String mailAddress) throws Exception {
        LOG.info("Waiting for notification to be sent...");
        greenMail.waitForIncomingEmail(1);

        boolean found = false;
        Session session = Session.getDefaultInstance(System.getProperties());
        session.setDebug(true);
        Store store = session.getStore("pop3");
        store.connect(POP3_HOST, POP3_PORT, mailAddress, mailAddress);

        Folder inbox = store.getFolder("INBOX");
        assertNotNull(inbox);
        inbox.open(Folder.READ_WRITE);

        Message[] messages = inbox.getMessages();
        for (Message message : messages) {
            if (sender.equals(message.getFrom()[0].toString()) && subject.equals(message.getSubject())) {
                found = true;
                message.setFlag(Flags.Flag.DELETED, true);
            }
        }

        inbox.close(true);
        store.close();
        return found;
    }

    @Test
    public void notifyByMail() throws Exception {
        String sender = "syncopetest-" + getUUIDString() + "@syncope.apache.org";
        String subject = "Test notification " + getUUIDString();
        String recipient = createNotificationTask(true, true, TraceLevel.ALL, sender, subject);
        NotificationTaskTO taskTO = findNotificationTaskBySender(sender);
        assertNotNull(taskTO);
        assertNotNull(taskTO.getNotification());
        assertTrue(taskTO.getExecutions().isEmpty());

        execNotificationTask(taskService, taskTO.getKey(), 50);

        assertTrue(verifyMail(sender, subject, recipient));

        // verify message body
        taskTO = taskService.read(taskTO.getKey(), true);
        assertNotNull(taskTO);
        assertTrue(taskTO.isExecuted());
        assertNotNull(taskTO.getTextBody());
        assertTrue("Notification mail text doesn't contain expected content.",
                taskTO.getTextBody().contains("Your email address is " + recipient + "."));
        assertTrue("Notification mail text doesn't contain expected content.",
                taskTO.getTextBody().contains("Your email address inside a link: "
                        + "http://localhost/?email=" + recipient.replaceAll("@", "%40")));
    }

    @Test
    public void notifyByMailEmptyAbout() throws Exception {
        String sender = "syncopetest-" + getUUIDString() + "@syncope.apache.org";
        String subject = "Test notification " + getUUIDString();
        String recipient = createNotificationTask(true, false, TraceLevel.ALL, sender, subject);
        NotificationTaskTO taskTO = findNotificationTaskBySender(sender);
        assertNotNull(taskTO);
        assertTrue(taskTO.getExecutions().isEmpty());

        execNotificationTask(taskService, taskTO.getKey(), 50);

        assertTrue(verifyMail(sender, subject, recipient));
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
            createNotificationTask(true, true, TraceLevel.ALL, sender, subject);
            NotificationTaskTO taskTO = findNotificationTaskBySender(sender);
            assertNotNull(taskTO);
            assertNotNull(taskTO.getNotification());
            assertTrue(taskTO.getExecutions().isEmpty());

            // 4. verify notification could not be delivered
            execTask(taskService, taskTO.getKey(), NotificationJob.Status.NOT_SENT.name(), 5, false);

            taskTO = taskService.read(taskTO.getKey(), true);
            assertNotNull(taskTO);
            assertFalse(taskTO.isExecuted());
            assertFalse(taskTO.getExecutions().isEmpty());
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
        createNotificationTask(true, true, TraceLevel.ALL, sender, "Test notification");
        NotificationTaskTO taskTO = findNotificationTaskBySender(sender);
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
        createNotificationTask(true, true, TraceLevel.ALL, sender, "Test notification");

        // 2. get NotificationTaskTO for user just created
        NotificationTaskTO taskTO = findNotificationTaskBySender(sender);
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
        String recipient = createNotificationTask(true, true, TraceLevel.NONE, sender, subject);
        NotificationTaskTO taskTO = findNotificationTaskBySender(sender);
        assertNotNull(taskTO);
        assertNotNull(taskTO.getNotification());
        assertTrue(taskTO.getExecutions().isEmpty());

        taskService.execute(new ExecuteQuery.Builder().key(taskTO.getKey()).build());

        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
        }

        assertTrue(verifyMail(sender, subject, recipient));

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
        String recipient = createNotificationTask(
                true, true, TraceLevel.ALL, sender, subject, "syncope445@syncope.apache.org");
        NotificationTaskTO taskTO = findNotificationTaskBySender(sender);
        assertNotNull(taskTO);
        assertNotNull(taskTO.getNotification());
        assertTrue(taskTO.getExecutions().isEmpty());

        execNotificationTask(taskService, taskTO.getKey(), 50);

        assertTrue(verifyMail(sender, subject, recipient));

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
        notification.getStaticRecipients().add(MAIL_ADDRESS);
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
        NotificationTaskTO taskTO = findNotificationTaskBySender(sender);
        assertNotNull(taskTO);
        assertNotNull(taskTO.getNotification());
        assertTrue(taskTO.getRecipients().containsAll(
                new TestNotificationRecipientsProvider().provideRecipients(null)));

        NotificationTaskTO foundViaList = taskService.<NotificationTaskTO>list(
                new TaskQuery.Builder(TaskType.NOTIFICATION).notification(notification.getKey()).build()).
                getResult().get(0);
        assertEquals(taskTO, foundViaList);

        execNotificationTask(taskService, taskTO.getKey(), 50);

        assertTrue(verifyMail(sender, subject, MAIL_ADDRESS));
    }

    @Test
    public void issueSYNCOPE492() throws Exception {
        String sender = "syncopetest-" + getUUIDString() + "@syncope.apache.org";
        String subject = "Test notification " + getUUIDString();
        createNotificationTask(false, true, TraceLevel.NONE, sender, subject, "syncope445@syncope.apache.org");

        // verify that no task was created for disabled notification
        assertNull(findNotificationTaskBySender(sender));
    }

    private String createNotificationTask(
            final boolean active,
            final boolean includeAbout,
            final TraceLevel traceLevel,
            final String sender,
            final String subject,
            final String... staticRecipients) {

        // 1. Create notification
        NotificationTO notification = new NotificationTO();
        notification.setTraceLevel(traceLevel);
        notification.getEvents().add("[LOGIC]:[UserLogic]:[]:[create]:[SUCCESS]");

        if (includeAbout) {
            notification.getAbouts().put(AnyTypeKind.USER.name(),
                    SyncopeClient.getUserSearchConditionBuilder().
                    inGroups("bf825fe1-7320-4a54-bd64-143b5c18ab97").query());
        }

        notification.setRecipientsFIQL(SyncopeClient.getUserSearchConditionBuilder().
                inGroups("f779c0d4-633b-4be5-8f57-32eb478a3ca5").query());
        notification.setSelfAsRecipient(true);
        notification.setRecipientAttrName("email");
        if (staticRecipients != null) {
            CollectionUtils.addAll(notification.getStaticRecipients(), staticRecipients);
        }

        notification.setSender(sender);
        notification.setSubject(subject);
        notification.setTemplate("optin");
        notification.setActive(active);

        Response response = notificationService.create(notification);
        notification = getObject(response.getLocation(), NotificationService.class, NotificationTO.class);
        assertNotNull(notification);

        // 2. create user
        UserTO userTO = UserITCase.getUniqueSampleTO(MAIL_ADDRESS);
        userTO.getMemberships().add(
                new MembershipTO.Builder().group("bf825fe1-7320-4a54-bd64-143b5c18ab97").build());

        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);
        return userTO.getUsername();
    }

}

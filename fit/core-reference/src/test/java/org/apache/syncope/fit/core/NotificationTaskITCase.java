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

import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.NotificationTaskTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.core.provisioning.java.job.notification.NotificationJob;
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
}

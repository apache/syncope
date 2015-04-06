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
package org.apache.syncope.fit.core.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.Response;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.NotificationTO;
import org.apache.syncope.common.lib.to.NotificationTaskTO;
import org.apache.syncope.common.lib.to.TaskExecTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.common.rest.api.service.NotificationService;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class NotificationTaskITCase extends AbstractTaskITCase {

    @Test
    public void issueSYNCOPE81() {
        String sender = "syncope81@syncope.apache.org";
        createNotificationTask(sender);
        NotificationTaskTO taskTO = findNotificationTaskBySender(sender);
        assertNotNull(taskTO);

        assertTrue(taskTO.getExecutions().isEmpty());

        // generate an execution in order to verify the deletion of a notification task with one or more executions
        TaskExecTO execution = taskService.execute(taskTO.getKey(), false);
        assertEquals("NOT_SENT", execution.getStatus());

        int i = 0;
        int maxit = 50;
        int executions = 0;

        // wait for task exec completion (executions incremented)
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            taskTO = taskService.read(taskTO.getKey());

            assertNotNull(taskTO);
            assertNotNull(taskTO.getExecutions());

            i++;
        } while (executions == taskTO.getExecutions().size() && i < maxit);

        assertFalse(taskTO.getExecutions().isEmpty());

        taskService.delete(taskTO.getKey());
    }

    @Test
    public void issueSYNCOPE86() {
        // 1. create notification task
        String sender = "syncope86@syncope.apache.org";
        createNotificationTask(sender);

        // 2. get NotificationTaskTO for user just created
        NotificationTaskTO taskTO = findNotificationTaskBySender(sender);
        assertNotNull(taskTO);
        assertTrue(taskTO.getExecutions().isEmpty());

        try {
            // 3. execute the generated NotificationTask
            TaskExecTO execution = taskService.execute(taskTO.getKey(), false);
            assertNotNull(execution);

            // 4. verify
            taskTO = taskService.read(taskTO.getKey());
            assertNotNull(taskTO);
            assertEquals(1, taskTO.getExecutions().size());
        } finally {
            // Remove execution to make test re-runnable
            taskService.deleteExecution(taskTO.getExecutions().get(0).getKey());
        }
    }

    private void createNotificationTask(final String sender) {
        // 1. Create notification
        NotificationTO notification = new NotificationTO();
        notification.setTraceLevel(TraceLevel.FAILURES);
        notification.getEvents().add("[REST]:[UserLogic]:[]:[create]:[SUCCESS]");

        notification.setUserAbout(SyncopeClient.getUserSearchConditionBuilder().inGroups(7L).query());

        notification.setRecipients(SyncopeClient.getUserSearchConditionBuilder().inGroups(8L).query());
        notification.setSelfAsRecipient(true);

        notification.setRecipientAttrName("email");
        notification.setRecipientAttrType(IntMappingType.UserPlainSchema);

        notification.setSender(sender);
        String subject = "Test notification";
        notification.setSubject(subject);
        notification.setTemplate("optin");
        notification.setActive(true);

        Response response = notificationService.create(notification);
        notification = getObject(response.getLocation(), NotificationService.class, NotificationTO.class);
        assertNotNull(notification);

        // 2. create user
        UserTO userTO = UserITCase.getUniqueSampleTO("syncope@syncope.apache.org");
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setGroupId(7);
        userTO.getMemberships().add(membershipTO);

        userTO = createUser(userTO);
        assertNotNull(userTO);
    }

}

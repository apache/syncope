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

import java.util.Collections;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.apache.syncope.client.search.AttributableCond;
import org.apache.syncope.client.search.MembershipCond;
import org.apache.syncope.client.search.NodeCond;
import org.apache.syncope.client.to.AttributeTO;
import org.apache.syncope.client.to.MembershipTO;
import org.apache.syncope.client.to.NotificationTO;
import org.apache.syncope.client.to.NotificationTaskTO;
import org.apache.syncope.client.to.PropagationTaskTO;
import org.apache.syncope.client.to.RoleTO;
import org.apache.syncope.client.to.SchedTaskTO;
import org.apache.syncope.client.to.SyncTaskTO;
import org.apache.syncope.client.to.TaskExecTO;
import org.apache.syncope.client.to.TaskTO;
import org.apache.syncope.client.to.UserTO;
import org.apache.syncope.core.sync.SyncJob;
import org.apache.syncope.core.sync.TestSyncActions;
import org.apache.syncope.core.workflow.ActivitiDetector;
import org.apache.syncope.types.IntMappingType;
import org.apache.syncope.types.PropagationTaskExecStatus;
import org.apache.syncope.types.TaskType;
import org.apache.syncope.types.TraceLevel;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.HttpStatusCodeException;

@FixMethodOrder(MethodSorters.JVM)
public class TaskTestITCase extends AbstractTest {

    private static final Long SCHED_TASK_ID = 5L;

    private static final Long SYNC_TASK_ID = 4L;

    @Test
    public void getJobClasses() {
        Set<String> jobClasses = taskService.getJobClasses();
        assertNotNull(jobClasses);
        assertFalse(jobClasses.isEmpty());
    }

    @Test
    public void getSyncActionsClasses() {
        Set<String> actions = taskService.getSyncActionsClasses();
        assertNotNull(actions);
        assertFalse(actions.isEmpty());
    }

    @Test
    public void create() {
        SyncTaskTO task = new SyncTaskTO();
        task.setName("Test create Sync");
        task.setResource("ws-target-resource-2");

        UserTO userTemplate = new UserTO();
        userTemplate.addResource("ws-target-resource-2");
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(8L);
        userTemplate.addMembership(membershipTO);
        task.setUserTemplate(userTemplate);

        RoleTO roleTemplate = new RoleTO();
        roleTemplate.addResource("resource-ldap");
        task.setRoleTemplate(roleTemplate);

        SyncTaskTO actual = taskService.create(task);
        assertNotNull(actual);

        task = taskService.read(TaskType.SYNCHRONIZATION, actual.getId());
        assertNotNull(task);
        assertEquals(actual.getId(), task.getId());
        assertEquals(actual.getJobClassName(), task.getJobClassName());
        assertEquals(userTemplate, task.getUserTemplate());
        assertEquals(roleTemplate, task.getRoleTemplate());
    }

    @Test
    public void update() {
        SchedTaskTO task = taskService.read(TaskType.SCHEDULED, SCHED_TASK_ID);
        assertNotNull(task);

        SchedTaskTO taskMod = new SchedTaskTO();
        taskMod.setId(5);
        taskMod.setCronExpression(null);

        SchedTaskTO actual = taskService.update(taskMod.getId(), taskMod);
        assertNotNull(actual);
        assertEquals(task.getId(), actual.getId());
        assertNull(actual.getCronExpression());
    }

    @Test
    public void count() {
        Integer count = taskService.count(TaskType.PROPAGATION);
        assertNotNull(count);
        assertTrue(count > 0);
    }

    @Test
    public void list() {
        List<PropagationTaskTO> tasks = taskService.list(TaskType.PROPAGATION);

        assertNotNull(tasks);
        assertFalse(tasks.isEmpty());
        for (TaskTO task : tasks) {
            assertNotNull(task);
        }
    }

    @Test
    public void paginatedList() {
        List<PropagationTaskTO> tasks = taskService.list(TaskType.PROPAGATION, 1, 2);

        assertNotNull(tasks);
        assertFalse(tasks.isEmpty());
        assertEquals(2, tasks.size());

        for (TaskTO task : tasks) {
            assertNotNull(task);
        }

        tasks = taskService.list(TaskType.PROPAGATION, 2, 2);

        assertNotNull(tasks);
        assertFalse(tasks.isEmpty());

        for (TaskTO task : tasks) {
            assertNotNull(task);
        }

        tasks = taskService.list(TaskType.PROPAGATION, 100, 2);

        assertNotNull(tasks);
        assertTrue(tasks.isEmpty());
    }

    @Test
    public void listExecutions() {
        List<TaskExecTO> executions = taskService.listExecutions(TaskType.PROPAGATION);
        assertNotNull(executions);
        assertFalse(executions.isEmpty());
        for (TaskExecTO execution : executions) {
            assertNotNull(execution);
        }
    }

    @Test
    public void read() {
        PropagationTaskTO taskTO = taskService.read(TaskType.PROPAGATION, 3L);

        assertNotNull(taskTO);
        assertNotNull(taskTO.getExecutions());
        assertTrue(taskTO.getExecutions().isEmpty());
    }

    @Test
    public void readExecution() {
        TaskExecTO taskTO = taskService.readExecution(1L);
        assertNotNull(taskTO);
    }

    @Test
    public void deal() {
        try {
            taskService.delete(TaskType.PROPAGATION, 0L);
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
        TaskExecTO exec = taskService.execute(1L, false);
        assertEquals(PropagationTaskExecStatus.SUBMITTED.name(), exec.getStatus());

        exec = taskService.report(exec.getId(), PropagationTaskExecStatus.SUCCESS, "OK");
        assertEquals(PropagationTaskExecStatus.SUCCESS.name(), exec.getStatus());
        assertEquals("OK", exec.getMessage());

        taskService.delete(TaskType.PROPAGATION, 1L);
        try {
            taskService.readExecution(exec.getId());
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }

    @Test
    public void sync() {
        //-----------------------------
        // Create a new user ... it should be updated applying sync policy
        //-----------------------------
        UserTO userTO = new UserTO();
        userTO.setPassword("password123");
        userTO.setUsername("test9");
        userTO.addAttribute(attributeTO("firstname", "nome9"));
        userTO.addAttribute(attributeTO("surname", "cognome"));
        userTO.addAttribute(attributeTO("type", "a type"));
        userTO.addAttribute(attributeTO("fullname", "nome cognome"));
        userTO.addAttribute(attributeTO("userId", "user5@syncope.apache.org"));
        userTO.addAttribute(attributeTO("email", "user5@syncope.apache.org"));
        userTO.addDerivedAttribute(attributeTO("csvuserid", null));

        userTO = userService.create(userTO);
        assertNotNull(userTO);
        //-----------------------------

        int usersPre = userService.count();
        assertNotNull(usersPre);

        // Update sync task
        SyncTaskTO task = taskService.read(TaskType.SYNCHRONIZATION, SYNC_TASK_ID);
        assertNotNull(task);

        //  add custom SyncJob actions
        task.setActionsClassName(TestSyncActions.class.getName());

        //  add user template
        UserTO template = new UserTO();
        template.addAttribute(attributeTO("type", "email == 'test8@syncope.apache.org'? 'TYPE_8': 'TYPE_OTHER'"));
        template.addDerivedAttribute(attributeTO("cn", null));
        template.addResource("resource-testdb");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(8L);
        membershipTO.addAttribute(attributeTO("subscriptionDate", "'2009-08-18T16:33:12.203+0200'"));
        template.addMembership(membershipTO);

        task.setUserTemplate(template);

        SyncTaskTO actual = taskService.update(task.getId(), task);
        assertNotNull(actual);
        assertEquals(task.getId(), actual.getId());
        assertEquals(TestSyncActions.class.getName(), actual.getActionsClassName());

        execSyncTask(SYNC_TASK_ID, 50, false);

        // after execution of the sync task the user data should be synced from
        // csv datasource and processed by user template
        userTO = userService.read(userTO.getId());
        assertNotNull(userTO);
        assertEquals("test9", userTO.getUsername());
        assertEquals(ActivitiDetector.isActivitiEnabledForUsers()
                ? "active"
                : "created", userTO.getStatus());
        assertEquals("test9@syncope.apache.org", userTO.getAttributeMap().get("email").getValues().get(0));
        assertEquals("test9@syncope.apache.org", userTO.getAttributeMap().get("userId").getValues().get(0));
        assertTrue(Integer.valueOf(userTO.getAttributeMap().get("fullname").getValues().get(0)) <= 10);

        // check for user template
        userTO = userService.read("test7");
        assertNotNull(userTO);
        assertEquals("TYPE_OTHER", userTO.getAttributeMap().get("type").getValues().get(0));
        assertEquals(2, userTO.getResources().size());
        assertTrue(userTO.getResources().contains("resource-testdb"));
        assertTrue(userTO.getResources().contains("ws-target-resource-2"));
        assertEquals(1, userTO.getMemberships().size());
        assertTrue(userTO.getMemberships().get(0).getAttributeMap().containsKey("subscriptionDate"));

        userTO = userService.read("test8");
        assertNotNull(userTO);
        assertEquals("TYPE_8", userTO.getAttributeMap().get("type").getValues().get(0));

        // check for sync results
        int usersPost = userService.count();
        assertNotNull(usersPost);
        assertEquals(usersPre + 9, usersPost);

        // Check for issue 215:
        // * expected disabled user test1
        // * expected enabled user test2

        userTO = userService.read("test1");
        assertNotNull(userTO);
        assertEquals("suspended", userTO.getStatus());

        userTO = userService.read("test3");
        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());
    }

    @Test
    public void reconcileUsers() {
        // Update sync task
        SyncTaskTO task = taskService.read(TaskType.SYNCHRONIZATION, 7L);
        assertNotNull(task);

        //  add user template
        UserTO template = new UserTO();
        template.addAttribute(attributeTO("type", "'type a'"));
        template.addAttribute(attributeTO("userId", "'reconciled@syncope.apache.org'"));
        template.addAttribute(attributeTO("fullname", "'reconciled fullname'"));
        template.addAttribute(attributeTO("surname", "'surname'"));

        task.setUserTemplate(template);

        SyncTaskTO actual = taskService.update(task.getId(), task);
        assertNotNull(actual);
        assertEquals(task.getId(), actual.getId());
        assertEquals(template, actual.getUserTemplate());
        assertEquals(new RoleTO(), actual.getRoleTemplate());

        TaskExecTO execution = execSyncTask(actual.getId(), 20, false);

        final String status = execution.getStatus();
        assertNotNull(status);
        assertTrue(PropagationTaskExecStatus.valueOf(status).isSuccessful());

        final UserTO userTO = userService.read("testuser1");
        assertNotNull(userTO);
        assertEquals("reconciled@syncope.apache.org", userTO.getAttributeMap().get("userId").getValues().get(0));
    }

    @Test
    public void reconcileRoles() {
        // Update sync task
        SyncTaskTO task = taskService.read(TaskType.SYNCHRONIZATION, 11L);
        assertNotNull(task);

        //  add user template
        RoleTO template = new RoleTO();
        template.setParent(8L);
        template.addAttribute(attributeTO("show", "'true'"));

        task.setRoleTemplate(template);

        SyncTaskTO actual = taskService.update(task.getId(), task);
        assertNotNull(actual);
        assertEquals(task.getId(), actual.getId());
        assertEquals(template, actual.getRoleTemplate());
        assertEquals(new UserTO(), actual.getUserTemplate());

        TaskExecTO execution = execSyncTask(actual.getId(), 20, false);

        // 1. verify execution status
        final String status = execution.getStatus();
        assertNotNull(status);
        assertTrue(PropagationTaskExecStatus.valueOf(status).isSuccessful());

        // 2. verify that synchronized role is found, with expected attributes
        final AttributableCond rolenameLeafCond = new AttributableCond(AttributableCond.Type.EQ);
        rolenameLeafCond.setSchema("name");
        rolenameLeafCond.setExpression("testLDAPGroup");
        final List<RoleTO> matchingRoles = roleService.search(NodeCond.getLeafCond(rolenameLeafCond));
        assertNotNull(matchingRoles);
        assertEquals(1, matchingRoles.size());

        final RoleTO roleTO = matchingRoles.iterator().next();
        assertNotNull(roleTO);
        assertEquals("testLDAPGroup", roleTO.getName());
        assertEquals(8L, roleTO.getParent());
        assertEquals("true", roleTO.getAttributeMap().get("show").getValues().get(0));

        // 3. verify that LDAP group membership is propagated as Syncope role membership
        final MembershipCond membershipCond = new MembershipCond();
        membershipCond.setRoleId(roleTO.getId());
        final List<UserTO> members = userService.search(NodeCond.getLeafCond(membershipCond));
        assertNotNull(members);
        assertEquals(1, members.size());
    }

    @Test
    public void issue196() {
        TaskExecTO exec = taskService.execute(6L, false);
        assertNotNull(exec);
        assertEquals(0, exec.getId());
        assertNotNull(exec.getTask());
    }

    @Test
    public void dryRun() {
        TaskExecTO execution = execSyncTask(SYNC_TASK_ID, 50, true);
        assertEquals("Execution of task " + execution.getTask() + " failed with message " + execution.getMessage(),
                "SUCCESS", execution.getStatus());
    }

    @Test
    public void issueSYNCOPE81() {
        NotificationTaskTO taskTO = taskService.read(TaskType.NOTIFICATION, 8L);
        assertNotNull(taskTO);

        int executions = taskTO.getExecutions().size();

        if (executions == 0) {
            // generate an execution in order to verify the deletion of a notification task with one or more executions

            TaskExecTO execution = taskService.execute(taskTO.getId(), false);
            assertEquals("NOT_SENT", execution.getStatus());

            int i = 0;
            int maxit = 50;

            // wait for task exec completion (executions incremented)
            do {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }

                taskTO = taskService.read(TaskType.NOTIFICATION, taskTO.getId());

                assertNotNull(taskTO);
                assertNotNull(taskTO.getExecutions());

                i++;
            } while (executions == taskTO.getExecutions().size() && i < maxit);

            assertFalse(taskTO.getExecutions().isEmpty());
        }

        taskTO = taskService.delete(TaskType.NOTIFICATION, taskTO.getId());
        assertNotNull(taskTO);
    }

    @Test
    public void issueSYNCOPE86() {
        // 1. create suitable notification for subsequent tests
        NotificationTO notification = new NotificationTO();
        notification.setTraceLevel(TraceLevel.FAILURES);
        notification.addEvent("create");

        MembershipCond membCond = new MembershipCond();
        membCond.setRoleId(7L);
        notification.setAbout(NodeCond.getLeafCond(membCond));

        membCond = new MembershipCond();
        membCond.setRoleId(8L);
        notification.setRecipients(NodeCond.getLeafCond(membCond));
        notification.setSelfAsRecipient(true);

        notification.setRecipientAttrName("email");
        notification.setRecipientAttrType(IntMappingType.UserSchema);

        String sender = "syncope86@syncope.apache.org";
        notification.setSender(sender);
        String subject = "Test notification SYNCOPE-86";
        notification.setSubject(subject);
        notification.setTemplate("optin");

        notification = notificationService.create(notification);
        assertNotNull(notification);

        // 2. create user
        UserTO userTO = UserTestITCase.getSampleTO("syncope86@syncope.apache.org");
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7);
        userTO.addMembership(membershipTO);

        userTO = userService.create(userTO);
        assertNotNull(userTO);

        // 3. get NotificationTaskTO for user just created
        List<NotificationTaskTO> tasks = taskService.list(TaskType.NOTIFICATION);
        assertNotNull(tasks);
        assertFalse(tasks.isEmpty());

        NotificationTaskTO taskTO = null;
        for (NotificationTaskTO task : tasks) {
            if (sender.equals(task.getSender())) {
                taskTO = task;
            }
        }
        assertNotNull(taskTO);
        assertTrue(taskTO.getExecutions().isEmpty());

        // 4. execute the generated NotificationTask
        TaskExecTO execution = taskService.execute(taskTO.getId(), false);
        assertNotNull(execution);

        // 5. verify
        taskTO = taskService.read(TaskType.NOTIFICATION, taskTO.getId());
        assertNotNull(taskTO);
        assertEquals(1, taskTO.getExecutions().size());
    }

    @Test
    public void issueSYNCOPE68() {
        //-----------------------------
        // Create a new user ... it should be updated applying sync policy
        //-----------------------------
        UserTO userTO = new UserTO();
        userTO.setPassword("password123");
        userTO.setUsername("testuser2");

        userTO.addAttribute(attributeTO("firstname", "testuser2"));
        userTO.addAttribute(attributeTO("surname", "testuser2"));
        userTO.addAttribute(attributeTO("type", "a type"));
        userTO.addAttribute(attributeTO("fullname", "a type"));
        userTO.addAttribute(attributeTO("userId", "testuser2@syncope.apache.org"));
        userTO.addAttribute(attributeTO("email", "testuser2@syncope.apache.org"));

        userTO.addResource("ws-target-resource-nopropagation2");
        userTO.addResource("ws-target-resource-nopropagation4");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);

        userTO.addMembership(membershipTO);

        userTO = userService.create(userTO);
        assertNotNull(userTO);
        assertEquals("testuser2", userTO.getUsername());
        assertEquals(1, userTO.getMemberships().size());
        assertEquals(3, userTO.getResources().size());
        //-----------------------------

        //-----------------------------
        //  add user template
        //-----------------------------
        UserTO template = new UserTO();

        membershipTO = new MembershipTO();
        membershipTO.setRoleId(10L);

        template.addMembership(membershipTO);

        template.addResource("ws-target-resource-nopropagation4");
        //-----------------------------

        // Update sync task
        SyncTaskTO task = taskService.read(TaskType.SYNCHRONIZATION, 9L);
        assertNotNull(task);

        task.setUserTemplate(template);

        SyncTaskTO actual = taskService.update(task.getId(), task);
        assertNotNull(actual);
        assertEquals(task.getId(), actual.getId());
        assertFalse(actual.getUserTemplate().getResources().isEmpty());
        assertFalse(actual.getUserTemplate().getMemberships().isEmpty());

        TaskExecTO execution = execSyncTask(actual.getId(), 50, false);
        final String status = execution.getStatus();
        assertNotNull(status);
        assertTrue(PropagationTaskExecStatus.valueOf(status).isSuccessful());

        userTO = userService.read("testuser2");
        assertNotNull(userTO);
        assertEquals("testuser2@syncope.apache.org", userTO.getAttributeMap().get("userId").getValues().get(0));
        assertEquals(2, userTO.getMemberships().size());
        assertEquals(4, userTO.getResources().size());
    }

    @Test
    public void issueSYNCOPE144() {
        SchedTaskTO task = new SchedTaskTO();
        task.setName("issueSYNCOPE144");
        task.setDescription("issueSYNCOPE144 Description");
        task.setJobClassName(SyncJob.class.getName());

        SchedTaskTO actual = taskService.create(task);
        assertNotNull(actual);
        assertEquals("issueSYNCOPE144", actual.getName());
        assertEquals("issueSYNCOPE144 Description", actual.getDescription());

        task = taskService.read(TaskType.SCHEDULED, actual.getId());
        assertNotNull(task);
        assertEquals("issueSYNCOPE144", task.getName());
        assertEquals("issueSYNCOPE144 Description", task.getDescription());

        task.setName("issueSYNCOPE144_2");
        task.setDescription("issueSYNCOPE144 Description_2");

        actual = taskService.create(task);
        assertNotNull(actual);
        assertEquals("issueSYNCOPE144_2", actual.getName());
        assertEquals("issueSYNCOPE144 Description_2", actual.getDescription());
    }

    @Test
    public void issueSYNCOPE230() {
        // 1. read SyncTask for resource-db-sync (table TESTSYNC on external H2)
        execSyncTask(10L, 20, false);

        // 3. read e-mail address for user created by the SyncTask first execution
        UserTO userTO = userService.read("issuesyncope230");
        assertNotNull(userTO);
        String email = userTO.getAttributeMap().get("email").getValues().iterator().next();
        assertNotNull(email);

        // 4. update TESTSYNC on external H2 by changing e-mail address
        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        jdbcTemplate.execute("UPDATE TESTSYNC SET email='updatedSYNCOPE230@syncope.apache.org'");

        // 5. re-execute the SyncTask
        execSyncTask(10L, 20, false);

        // 6. verify that the e-mail was updated
        userTO = userService.read("issuesyncope230");
        assertNotNull(userTO);
        email = userTO.getAttributeMap().get("email").getValues().iterator().next();
        assertNotNull(email);
        assertEquals("updatedSYNCOPE230@syncope.apache.org", email);
    }

    private TaskExecTO execSyncTask(final Long taskId, final int maxWaitSeconds,
            final boolean dryRun) {

        TaskTO taskTO = taskService.read(TaskType.SYNCHRONIZATION, taskId);
        assertNotNull(taskTO);
        assertNotNull(taskTO.getExecutions());

        int preSyncSize = taskTO.getExecutions().size();
        TaskExecTO execution = taskService.execute(taskTO.getId(), dryRun);
        assertEquals("JOB_FIRED", execution.getStatus());

        int i = 0;
        int maxit = maxWaitSeconds;

        // wait for sync completion (executions incremented)
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            taskTO = taskService.read(TaskType.SYNCHRONIZATION, taskTO.getId());

            assertNotNull(taskTO);
            assertNotNull(taskTO.getExecutions());

            i++;
        } while (preSyncSize == taskTO.getExecutions().size() && i < maxit);
        if (i == maxit) {
            throw new RuntimeException("Timeout when executing task " + taskId);
        }
        return taskTO.getExecutions().get(0);
    }

    @Test
    public void issueSYNCOPE272() {

        //Create user with testdb resource
        UserTO userTO = new UserTO();
        userTO.setUsername("syncope272@syncope.apache.org");
        userTO.setPassword("password");
        userTO.addAttribute(attributeTO("firstname", "syncope272"));
        userTO.addAttribute(attributeTO("surname", "syncope272"));
        userTO.addAttribute(attributeTO("userId", "syncope272@syncope.apache.org"));
        userTO.addAttribute(attributeTO("fullname", "syncope272"));
        userTO.addResource("resource-testdb");

        userTO = userService.create(userTO);

        assertNotNull(userTO);
        assertEquals(1, userTO.getPropagationTOs().size());
        assertTrue(userTO.getPropagationTOs().get(0).getStatus().isSuccessful());

        // Update sync task
        SyncTaskTO task = taskService.read(TaskType.SYNCHRONIZATION, SYNC_TASK_ID);
        assertNotNull(task);

        // add user template

        AttributeTO newAttrTO = new AttributeTO();
        newAttrTO.setSchema("firstname");
        newAttrTO.setValues(Collections.singletonList(""));

        UserTO template = new UserTO();
        template.addAttribute(newAttrTO);
        template.addAttribute(attributeTO("userId", "'test'"));
        template.addAttribute(attributeTO("fullname", "'test'"));
        template.addAttribute(attributeTO("surname", "'test'"));
        template.addResource("resource-testdb");

        task.setUserTemplate(template);

        SyncTaskTO actual = taskService.update(task.getId(), task);
        assertNotNull(actual);
        assertEquals(task.getId(), actual.getId());

        TaskExecTO taskExecTO = execSyncTask(SYNC_TASK_ID, 50, false);
        assertNotNull(actual);
        assertEquals(task.getId(), actual.getId());

        assertNotNull(taskExecTO.getStatus());
        assertTrue(PropagationTaskExecStatus.valueOf(taskExecTO.getStatus()).isSuccessful());

        userTO = userService.read("syncope272@syncope.apache.org");
        assertNotNull(userTO);
        assertNotNull(userTO.getAttributeMap().get("firstname").getValues().get(0));
    }
}
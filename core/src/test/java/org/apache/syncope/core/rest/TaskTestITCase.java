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

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.apache.syncope.client.search.MembershipCond;
import org.apache.syncope.client.search.NodeCond;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.apache.syncope.client.to.AttributeTO;
import org.apache.syncope.client.to.MembershipTO;
import org.apache.syncope.client.to.NotificationTO;
import org.apache.syncope.client.to.NotificationTaskTO;
import org.apache.syncope.client.to.TaskExecTO;
import org.apache.syncope.client.to.PropagationTaskTO;
import org.apache.syncope.client.to.SchedTaskTO;
import org.apache.syncope.client.to.SyncTaskTO;
import org.apache.syncope.client.to.TaskTO;
import org.apache.syncope.client.to.UserTO;
import org.apache.syncope.core.scheduling.SyncJob;
import org.apache.syncope.core.init.SpringContextInitializer;
import org.apache.syncope.types.PropagationTaskExecStatus;
import org.apache.syncope.core.scheduling.TestSyncJobActions;
import org.apache.syncope.types.IntMappingType;
import org.apache.syncope.types.TraceLevel;

public class TaskTestITCase extends AbstractTest {

    @Test
    public void getJobClasses() {
        Set<String> jobClasses = restTemplate.getForObject(BASE_URL + "task/jobClasses.json", Set.class);
        assertNotNull(jobClasses);
        assertFalse(jobClasses.isEmpty());
    }

    @Test
    public void getJobActionsClasses() {
        Set<String> actions = restTemplate.getForObject(BASE_URL + "task/jobActionsClasses.json", Set.class);
        assertNotNull(actions);
        assertFalse(actions.isEmpty());
    }

    @Test
    public void create() {
        SyncTaskTO task = new SyncTaskTO();
        task.setName("Test create Sync");
        task.setResource("ws-target-resource-2");

        UserTO template = new UserTO();
        template.addResource("ws-target-resource-2");
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(8L);
        template.addMembership(membershipTO);
        task.setUserTemplate(template);

        SyncTaskTO actual = restTemplate.postForObject(BASE_URL + "task/create/sync", task, SyncTaskTO.class);
        assertNotNull(actual);

        task = restTemplate.getForObject(BASE_URL + "task/read/{taskId}", SyncTaskTO.class, actual.getId());
        assertNotNull(task);
        assertEquals(actual.getId(), task.getId());
        assertEquals(actual.getJobClassName(), task.getJobClassName());
    }

    @Test
    public void update() {
        SchedTaskTO task = restTemplate.getForObject(BASE_URL + "task/read/{taskId}", SchedTaskTO.class, 5);
        assertNotNull(task);

        SchedTaskTO taskMod = new SchedTaskTO();
        taskMod.setId(5);
        taskMod.setCronExpression(null);

        SchedTaskTO actual = restTemplate.postForObject(BASE_URL + "task/update/sched", taskMod, SchedTaskTO.class);
        assertNotNull(actual);
        assertEquals(task.getId(), actual.getId());
        assertNull(actual.getCronExpression());
    }

    @Test
    public void count() {
        Integer count = restTemplate.getForObject(BASE_URL + "task/propagation/count.json", Integer.class);
        assertNotNull(count);
        assertTrue(count > 0);
    }

    @Test
    public void list() {
        List<PropagationTaskTO> tasks = Arrays.asList(restTemplate.getForObject(BASE_URL + "task/propagation/list",
                PropagationTaskTO[].class));
        assertNotNull(tasks);
        assertFalse(tasks.isEmpty());
        for (TaskTO task : tasks) {
            assertNotNull(task);
        }
    }

    @Test
    public void paginatedList() {
        List<PropagationTaskTO> tasks = Arrays.asList(restTemplate.getForObject(BASE_URL
                + "task/propagation/list/{page}/{size}.json", PropagationTaskTO[].class, 1, 2));

        assertNotNull(tasks);
        assertFalse(tasks.isEmpty());
        assertEquals(2, tasks.size());

        for (TaskTO task : tasks) {
            assertNotNull(task);
        }

        tasks = Arrays.asList(restTemplate.getForObject(BASE_URL + "task/propagation/list/{page}/{size}.json",
                PropagationTaskTO[].class, 2, 2));

        assertNotNull(tasks);
        assertFalse(tasks.isEmpty());

        for (TaskTO task : tasks) {
            assertNotNull(task);
        }

        tasks = Arrays.asList(restTemplate.getForObject(BASE_URL + "task/propagation/list/{page}/{size}.json",
                PropagationTaskTO[].class, 100, 2));

        assertNotNull(tasks);
        assertTrue(tasks.isEmpty());
    }

    @Test
    public void listExecutions() {
        List<TaskExecTO> executions = Arrays.asList(restTemplate.getForObject(BASE_URL
                + "task/propagation/execution/list", TaskExecTO[].class));
        assertNotNull(executions);
        assertFalse(executions.isEmpty());
        for (TaskExecTO execution : executions) {
            assertNotNull(execution);
        }
    }

    @Test
    public void read() {
        PropagationTaskTO taskTO = restTemplate.getForObject(BASE_URL + "task/read/{taskId}", PropagationTaskTO.class,
                3);

        assertNotNull(taskTO);
        assertNotNull(taskTO.getExecutions());
        assertTrue(taskTO.getExecutions().isEmpty());
    }

    @Test
    public void readExecution() {
        TaskExecTO taskTO = restTemplate.getForObject(BASE_URL + "task/execution/read/{taskId}", TaskExecTO.class, 1);
        assertNotNull(taskTO);
    }

    @Test
    public void deal() {
        try {
            restTemplate.getForObject(BASE_URL + "task/delete/{taskId}", TaskTO.class, 0);
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
        TaskExecTO execution = restTemplate.postForObject(BASE_URL + "task/execute/{taskId}", null, TaskExecTO.class, 1);
        assertEquals(PropagationTaskExecStatus.SUBMITTED.name(), execution.getStatus());

        execution = restTemplate.getForObject(BASE_URL + "task/execution/report/{executionId}"
                + "?executionStatus=SUCCESS&message=OK", TaskExecTO.class, execution.getId());
        assertEquals(PropagationTaskExecStatus.SUCCESS.name(), execution.getStatus());
        assertEquals("OK", execution.getMessage());

        restTemplate.getForObject(BASE_URL + "task/delete/{taskId}", PropagationTaskTO.class, 1);
        try {
            restTemplate.getForObject(BASE_URL + "task/execution/read/{executionId}", TaskExecTO.class,
                    execution.getId());
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

        AttributeTO firstnameTO = new AttributeTO();
        firstnameTO.setSchema("firstname");
        firstnameTO.addValue("nome9");
        userTO.addAttribute(firstnameTO);

        AttributeTO surnameTO = new AttributeTO();
        surnameTO.setSchema("surname");
        surnameTO.addValue("cognome");
        userTO.addAttribute(surnameTO);

        AttributeTO typeTO = new AttributeTO();
        typeTO.setSchema("type");
        typeTO.addValue("a type");
        userTO.addAttribute(typeTO);

        AttributeTO fullnameTO = new AttributeTO();
        fullnameTO.setSchema("fullname");
        fullnameTO.addValue("nome cognome");
        userTO.addAttribute(fullnameTO);

        AttributeTO userIdTO = new AttributeTO();
        userIdTO.setSchema("userId");
        userIdTO.addValue("user5@syncope.apache.org");
        userTO.addAttribute(userIdTO);

        AttributeTO emailTO = new AttributeTO();
        emailTO.setSchema("email");
        emailTO.addValue("user5@syncope.apache.org");
        userTO.addAttribute(emailTO);

        // add a derived attribute (accountId for csvdir)
        AttributeTO csvuseridTO = new AttributeTO();
        csvuseridTO.setSchema("csvuserid");
        userTO.addDerivedAttribute(csvuseridTO);

        userTO = restTemplate.postForObject(BASE_URL + "user/create", userTO, UserTO.class);
        assertNotNull(userTO);
        //-----------------------------

        Integer usersPre = restTemplate.getForObject(BASE_URL + "user/count.json", Integer.class);
        assertNotNull(usersPre);

        // Update sync task
        SyncTaskTO task = restTemplate.getForObject(BASE_URL + "task/read/{taskId}", SyncTaskTO.class, 4);
        assertNotNull(task);

        //  add custom SyncJob actions
        task.setJobActionsClassName(TestSyncJobActions.class.getName());

        //  add user template
        UserTO template = new UserTO();

        AttributeTO attrTO = new AttributeTO();
        attrTO.setSchema("type");
        attrTO.addValue("email == 'test8@syncope.apache.org'? 'TYPE_8': 'TYPE_OTHER'");
        template.addAttribute(attrTO);

        attrTO = new AttributeTO();
        attrTO.setSchema("cn");
        template.addDerivedAttribute(attrTO);

        template.addResource("resource-testdb");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(8L);
        AttributeTO membershipAttr = new AttributeTO();
        membershipAttr.setSchema("subscriptionDate");
        membershipAttr.addValue("'2009-08-18T16:33:12.203+0200'");
        membershipTO.addAttribute(membershipAttr);
        template.addMembership(membershipTO);

        task.setUserTemplate(template);

        SyncTaskTO actual = restTemplate.postForObject(BASE_URL + "task/update/sync", task, SyncTaskTO.class);
        assertNotNull(actual);
        assertEquals(task.getId(), actual.getId());
        assertEquals(TestSyncJobActions.class.getName(), actual.getJobActionsClassName());

        SyncTaskTO taskTO = restTemplate.getForObject(BASE_URL + "task/read/{taskId}", SyncTaskTO.class, 4L);

        assertNotNull(taskTO);
        assertNotNull(taskTO.getExecutions());

        // read executions before sync (dryrun test could be executed before)
        int preSyncSize = taskTO.getExecutions().size();

        TaskExecTO execution = restTemplate.postForObject(BASE_URL + "task/execute/{taskId}", null, TaskExecTO.class,
                taskTO.getId());
        assertEquals("JOB_FIRED", execution.getStatus());

        int i = 0;
        int maxit = 50;

        // wait for sync completion (executions incremented)
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            taskTO = restTemplate.getForObject(BASE_URL + "task/read/{taskId}", SyncTaskTO.class, taskTO.getId());

            assertNotNull(taskTO);
            assertNotNull(taskTO.getExecutions());

            i++;
        } while (preSyncSize == taskTO.getExecutions().size() && i < maxit);

        // check for sync policy
        userTO = restTemplate.getForObject(BASE_URL + "user/read/{userId}.json", UserTO.class, userTO.getId());

        assertNotNull(userTO);
        assertEquals("test9", userTO.getUsername());
        assertEquals(SpringContextInitializer.isActivitiConfigured() ? "active" : "created", userTO.getStatus());
        assertEquals("test9@syncope.apache.org", userTO.getAttributeMap().get("email").getValues().get(0));
        assertEquals("test9@syncope.apache.org", userTO.getAttributeMap().get("userId").getValues().get(0));
        assertTrue(Integer.valueOf(userTO.getAttributeMap().get("fullname").getValues().get(0)) <= 10);

        // check for user template
        userTO = restTemplate.getForObject(BASE_URL + "user/readByUsername/{username}.json", UserTO.class, "test7");
        assertNotNull(userTO);
        assertEquals("TYPE_OTHER", userTO.getAttributeMap().get("type").getValues().get(0));
        assertEquals(2, userTO.getResources().size());
        assertTrue(userTO.getResources().contains("resource-testdb"));
        assertTrue(userTO.getResources().contains("ws-target-resource-2"));
        assertEquals(1, userTO.getMemberships().size());
        assertTrue(userTO.getMemberships().get(0).getAttributeMap().containsKey("subscriptionDate"));

        userTO = restTemplate.getForObject(BASE_URL + "user/readByUsername/{username}.json", UserTO.class, "test8");
        assertNotNull(userTO);
        assertEquals("TYPE_8", userTO.getAttributeMap().get("type").getValues().get(0));

        // check for sync results
        Integer usersPost = restTemplate.getForObject(BASE_URL + "user/count.json", Integer.class);
        assertNotNull(usersPost);
        assertTrue("Expected " + (usersPre + 9) + ", found " + usersPost, usersPost == usersPre + 9);

        // Check for issue 215: 
        // * expected disabled user test1
        // * expected enabled user test2

        userTO = restTemplate.getForObject(BASE_URL + "user/readByUsername/{username}.json", UserTO.class, "test1");
        assertNotNull(userTO);
        assertEquals("suspended", userTO.getStatus());

        userTO = restTemplate.getForObject(BASE_URL + "user/readByUsername/{username}.json", UserTO.class, "test3");
        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());
    }

    @Test
    public void reconcile() {
        // Update sync task
        SyncTaskTO task = restTemplate.getForObject(BASE_URL + "task/read/{taskId}", SyncTaskTO.class, 7);
        assertNotNull(task);

        //  add user template
        UserTO template = new UserTO();

        AttributeTO attrTO = new AttributeTO();
        attrTO.setSchema("type");
        attrTO.addValue("'type a'");
        template.addAttribute(attrTO);

        attrTO = new AttributeTO();
        attrTO.setSchema("userId");
        attrTO.addValue("'reconciled@syncope.apache.org'");
        template.addAttribute(attrTO);

        attrTO = new AttributeTO();
        attrTO.setSchema("fullname");
        attrTO.addValue("'reconciled fullname'");
        template.addAttribute(attrTO);

        attrTO = new AttributeTO();
        attrTO.setSchema("surname");
        attrTO.addValue("'surname'");
        template.addAttribute(attrTO);

        task.setUserTemplate(template);

        SyncTaskTO actual = restTemplate.postForObject(BASE_URL + "task/update/sync", task, SyncTaskTO.class);
        assertNotNull(actual);
        assertEquals(task.getId(), actual.getId());

        // read executions before sync (dryrun test could be executed before)
        int preSyncSize = actual.getExecutions().size();

        TaskExecTO execution = restTemplate.postForObject(BASE_URL + "task/execute/{taskId}", null, TaskExecTO.class,
                actual.getId());
        assertEquals("JOB_FIRED", execution.getStatus());

        int i = 0;
        int maxit = 20;

        // wait for sync completion (executions incremented)
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            actual = restTemplate.getForObject(BASE_URL + "task/read/{taskId}", SyncTaskTO.class, actual.getId());

            assertNotNull(actual);
            assertNotNull(actual.getExecutions());

            i++;

        } while (preSyncSize == actual.getExecutions().size() && i < maxit);

        assertEquals(1, actual.getExecutions().size());

        final String status = actual.getExecutions().get(0).getStatus();
        assertNotNull(status);
        assertTrue(PropagationTaskExecStatus.valueOf(status).isSuccessful());

        final UserTO userTO =
                restTemplate.getForObject(BASE_URL + "user/readByUsername/{username}.json", UserTO.class, "testuser1");

        assertNotNull(userTO);
        assertEquals("reconciled@syncope.apache.org", userTO.getAttributeMap().get("userId").getValues().get(0));
    }

    @Test
    public void issue196() {
        TaskExecTO execution = restTemplate.postForObject(BASE_URL + "task/execute/{taskId}", null, TaskExecTO.class, 6);
        assertNotNull(execution);
        assertEquals(0, execution.getId());
        assertNotNull(execution.getTask());
    }

    @Test
    public void dryRun() {
        SyncTaskTO taskTO = restTemplate.getForObject(BASE_URL + "task/read/{taskId}", SyncTaskTO.class, 4L);

        assertNotNull(taskTO);
        assertNotNull(taskTO.getExecutions());

        int preDryRunSize = taskTO.getExecutions().size();

        TaskExecTO execution = restTemplate.postForObject(BASE_URL + "task/execute/{taskId}?dryRun=true", null,
                TaskExecTO.class, 4);
        assertNotNull(execution);

        // wait for sync completion (executions incremented)
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            taskTO = restTemplate.getForObject(BASE_URL + "task/read/{taskId}", SyncTaskTO.class, taskTO.getId());

            assertNotNull(taskTO);
            assertNotNull(taskTO.getExecutions());

        } while (preDryRunSize == taskTO.getExecutions().size());

        assertEquals("SUCCESS", taskTO.getExecutions().get(0).getStatus());
    }

    @Test
    public void issueSYNCOPE81() {
        NotificationTaskTO taskTO = restTemplate.getForObject(
                BASE_URL + "task/read/{taskId}", NotificationTaskTO.class, 8L);
        assertNotNull(taskTO);

        int executions = taskTO.getExecutions().size();

        if (executions == 0) {
            // generate an execution in order to verify the deletion of a notification task with one or more executions

            TaskExecTO execution = restTemplate.postForObject(
                    BASE_URL + "task/execute/{taskId}", null, TaskExecTO.class, taskTO.getId());
            assertEquals("NOT_SENT", execution.getStatus());

            int i = 0;
            int maxit = 50;

            // wait for task exec completion (executions incremented)
            do {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }

                taskTO = restTemplate.getForObject(
                        BASE_URL + "task/read/{taskId}", NotificationTaskTO.class, taskTO.getId());

                assertNotNull(taskTO);
                assertNotNull(taskTO.getExecutions());

                i++;
            } while (executions == taskTO.getExecutions().size() && i < maxit);

            assertFalse(taskTO.getExecutions().isEmpty());
        }

        taskTO = restTemplate.getForObject(BASE_URL + "task/delete/{taskId}", NotificationTaskTO.class, taskTO.getId());
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

        notification = restTemplate.postForObject(BASE_URL + "notification/create.json",
                notification, NotificationTO.class);
        assertNotNull(notification);

        // 2. create user
        UserTO userTO = UserTestITCase.getSampleTO("syncope86@syncope.apache.org");
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7);
        userTO.addMembership(membershipTO);

        userTO = restTemplate.postForObject(BASE_URL + "user/create", userTO, UserTO.class);
        assertNotNull(userTO);

        // 3. get NotificationTaskTO for user just created
        List<NotificationTaskTO> tasks = Arrays.asList(restTemplate.getForObject(BASE_URL + "task/notification/list",
                NotificationTaskTO[].class));
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
        TaskExecTO execution = restTemplate.postForObject(
                BASE_URL + "task/execute/{taskId}", null, TaskExecTO.class, taskTO.getId());
        assertNotNull(execution);

        // 5. verify
        taskTO = restTemplate.getForObject(BASE_URL + "task/read/{taskId}", NotificationTaskTO.class,
                taskTO.getId());
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

        AttributeTO firstnameTO = new AttributeTO();
        firstnameTO.setSchema("firstname");
        firstnameTO.addValue("testuser2");
        userTO.addAttribute(firstnameTO);

        AttributeTO surnameTO = new AttributeTO();
        surnameTO.setSchema("surname");
        surnameTO.addValue("testuser2");
        userTO.addAttribute(surnameTO);

        AttributeTO typeTO = new AttributeTO();
        typeTO.setSchema("type");
        typeTO.addValue("a type");
        userTO.addAttribute(typeTO);

        AttributeTO fullnameTO = new AttributeTO();
        fullnameTO.setSchema("fullname");
        fullnameTO.addValue("testuser2");
        userTO.addAttribute(fullnameTO);

        AttributeTO userIdTO = new AttributeTO();
        userIdTO.setSchema("userId");
        userIdTO.addValue("testuser2@syncope.apache.org");
        userTO.addAttribute(userIdTO);

        AttributeTO emailTO = new AttributeTO();
        emailTO.setSchema("email");
        emailTO.addValue("testuser2@syncope.apache.org");
        userTO.addAttribute(emailTO);

        userTO.addResource("ws-target-resource-nopropagation2");
        userTO.addResource("ws-target-resource-nopropagation4");

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);

        userTO.addMembership(membershipTO);

        userTO = restTemplate.postForObject(BASE_URL + "user/create", userTO, UserTO.class);
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
        SyncTaskTO task = restTemplate.getForObject(BASE_URL + "task/read/{taskId}", SyncTaskTO.class, 9);
        assertNotNull(task);

        task.setUserTemplate(template);

        SyncTaskTO actual = restTemplate.postForObject(BASE_URL + "task/update/sync", task, SyncTaskTO.class);
        assertNotNull(actual);
        assertEquals(task.getId(), actual.getId());
        assertFalse(actual.getUserTemplate().getResources().isEmpty());
        assertFalse(actual.getUserTemplate().getMemberships().isEmpty());

        // read executions before sync (dryrun test could be executed before)
        int preSyncSize = actual.getExecutions().size();

        TaskExecTO execution =
                restTemplate.postForObject(BASE_URL + "task/execute/{taskId}", null, TaskExecTO.class, actual.getId());
        assertEquals("JOB_FIRED", execution.getStatus());

        int i = 0;
        int maxit = 50;

        // wait for sync completion (executions incremented)
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            actual = restTemplate.getForObject(BASE_URL + "task/read/{taskId}", SyncTaskTO.class, actual.getId());
            assertNotNull(actual);
            assertNotNull(actual.getExecutions());

            i++;
        } while (preSyncSize == actual.getExecutions().size() && i < maxit);
        assertEquals(preSyncSize + 1, actual.getExecutions().size());

        final String status = actual.getExecutions().get(0).getStatus();
        assertNotNull(status);
        assertTrue(PropagationTaskExecStatus.valueOf(status).isSuccessful());

        userTO = restTemplate.getForObject(BASE_URL + "user/readByUsername/{username}.json", UserTO.class, "testuser2");
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

        SchedTaskTO actual = restTemplate.postForObject(BASE_URL + "task/create/sched", task, SchedTaskTO.class);
        assertNotNull(actual);
        assertEquals("issueSYNCOPE144", actual.getName());
        assertEquals("issueSYNCOPE144 Description", actual.getDescription());

        task = restTemplate.getForObject(BASE_URL + "task/read/{taskId}", SchedTaskTO.class, actual.getId());
        assertNotNull(task);
        assertEquals("issueSYNCOPE144", task.getName());
        assertEquals("issueSYNCOPE144 Description", task.getDescription());

        task.setName("issueSYNCOPE144_2");
        task.setDescription("issueSYNCOPE144 Description_2");

        actual = restTemplate.postForObject(BASE_URL + "task/create/sched", task, SchedTaskTO.class);
        assertNotNull(actual);
        assertEquals("issueSYNCOPE144_2", actual.getName());
        assertEquals("issueSYNCOPE144 Description_2", actual.getDescription());
    }
}

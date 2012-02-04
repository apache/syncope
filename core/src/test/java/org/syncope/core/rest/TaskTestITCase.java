/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.rest;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.MembershipTO;
import org.syncope.client.to.TaskExecTO;
import org.syncope.client.to.PropagationTaskTO;
import org.syncope.client.to.SchedTaskTO;
import org.syncope.client.to.SyncTaskTO;
import org.syncope.client.to.TaskTO;
import org.syncope.client.to.UserTO;
import org.syncope.types.PropagationTaskExecStatus;
import org.syncope.core.scheduling.TestSyncJobActions;

public class TaskTestITCase extends AbstractTest {

    @Test
    public void create() {
        SyncTaskTO task = new SyncTaskTO();
        task.setResource("ws-target-resource-2");

        UserTO template = new UserTO();
        template.addResource("ws-target-resource-2");
        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(8L);
        template.addMembership(membershipTO);
        task.setUserTemplate(template);

        SyncTaskTO actual = restTemplate.postForObject(
                BASE_URL + "task/create/sync",
                task, SyncTaskTO.class);
        assertNotNull(actual);

        task = restTemplate.getForObject(
                BASE_URL + "task/read/{taskId}", SyncTaskTO.class,
                actual.getId());
        assertNotNull(task);
        assertEquals(actual.getId(), task.getId());
        assertEquals(actual.getJobClassName(), task.getJobClassName());
    }

    @Test
    public void update() {
        SchedTaskTO task = restTemplate.getForObject(
                BASE_URL + "task/read/{taskId}", SchedTaskTO.class,
                5);
        assertNotNull(task);

        SchedTaskTO taskMod = new SchedTaskTO();
        taskMod.setId(5);
        taskMod.setCronExpression(null);

        SchedTaskTO actual = restTemplate.postForObject(
                BASE_URL + "task/update/sched",
                taskMod, SchedTaskTO.class);
        assertNotNull(actual);
        assertEquals(task.getId(), actual.getId());
        assertNull(actual.getCronExpression());
    }

    @Test
    public void count() {
        Integer count = restTemplate.getForObject(
                BASE_URL + "task/propagation/count.json", Integer.class);
        assertNotNull(count);
        assertTrue(count > 0);
    }

    @Test
    public void list() {
        List<PropagationTaskTO> tasks = Arrays.asList(
                restTemplate.getForObject(
                BASE_URL + "task/propagation/list", PropagationTaskTO[].class));
        assertNotNull(tasks);
        assertFalse(tasks.isEmpty());
        for (TaskTO task : tasks) {
            assertNotNull(task);
        }
    }

    @Test
    public void paginatedList() {
        List<PropagationTaskTO> tasks = Arrays.asList(restTemplate.getForObject(
                BASE_URL + "task/propagation/list/{page}/{size}.json",
                PropagationTaskTO[].class, 1, 2));

        assertNotNull(tasks);
        assertFalse(tasks.isEmpty());
        assertEquals(2, tasks.size());

        for (TaskTO task : tasks) {
            assertNotNull(task);
        }

        tasks = Arrays.asList(restTemplate.getForObject(
                BASE_URL + "task/propagation/list/{page}/{size}.json",
                PropagationTaskTO[].class, 2, 2));

        assertNotNull(tasks);
        assertFalse(tasks.isEmpty());

        for (TaskTO task : tasks) {
            assertNotNull(task);
        }

        tasks = Arrays.asList(restTemplate.getForObject(
                BASE_URL + "task/propagation/list/{page}/{size}.json",
                PropagationTaskTO[].class, 100, 2));

        assertNotNull(tasks);
        assertTrue(tasks.isEmpty());
    }

    @Test
    public void listExecutions() {
        List<TaskExecTO> executions = Arrays.asList(
                restTemplate.getForObject(
                BASE_URL + "task/propagation/execution/list",
                TaskExecTO[].class));
        assertNotNull(executions);
        assertFalse(executions.isEmpty());
        for (TaskExecTO execution : executions) {
            assertNotNull(execution);
        }
    }

    @Test
    public void read() {
        PropagationTaskTO taskTO = restTemplate.getForObject(
                BASE_URL + "task/read/{taskId}", PropagationTaskTO.class, 3);

        assertNotNull(taskTO);
        assertNotNull(taskTO.getExecutions());
        assertTrue(taskTO.getExecutions().isEmpty());
    }

    @Test
    public void readExecution() {
        TaskExecTO taskTO = restTemplate.getForObject(
                BASE_URL + "task/execution/read/{taskId}",
                TaskExecTO.class, 1);
        assertNotNull(taskTO);
    }

    @Test
    public void deal() {
        try {
            restTemplate.delete(BASE_URL + "task/delete/{taskId}", 0);
        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }

        TaskExecTO execution = restTemplate.postForObject(
                BASE_URL + "task/execute/{taskId}", null,
                TaskExecTO.class, 1);
        assertEquals(PropagationTaskExecStatus.SUBMITTED.name(),
                execution.getStatus());

        execution = restTemplate.getForObject(
                BASE_URL + "task/execution/report/{executionId}"
                + "?executionStatus=SUCCESS&message=OK",
                TaskExecTO.class, execution.getId());
        assertEquals(PropagationTaskExecStatus.SUCCESS.name(),
                execution.getStatus());
        assertEquals("OK", execution.getMessage());

        restTemplate.delete(BASE_URL + "task/delete/{taskId}", 1);
        try {
            restTemplate.getForObject(
                    BASE_URL + "task/execution/read/{executionId}",
                    TaskExecTO.class, execution.getId());
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
        userIdTO.addValue("user5@syncope-idm.org");
        userTO.addAttribute(userIdTO);

        AttributeTO emailTO = new AttributeTO();
        emailTO.setSchema("email");
        emailTO.addValue("user5@syncope-idm.org");
        userTO.addAttribute(emailTO);

        // add a derived attribute (accountId for csvdir)
        AttributeTO csvuseridTO = new AttributeTO();
        csvuseridTO.setSchema("csvuserid");
        userTO.addDerivedAttribute(csvuseridTO);

        userTO = restTemplate.postForObject(
                BASE_URL + "user/create", userTO, UserTO.class);
        assertNotNull(userTO);
        //-----------------------------

        Integer usersPre = restTemplate.getForObject(
                BASE_URL + "user/count.json", Integer.class);
        assertNotNull(usersPre);

        // Update sync task
        SyncTaskTO task = restTemplate.getForObject(
                BASE_URL + "task/read/{taskId}", SyncTaskTO.class, 4);
        assertNotNull(task);

        //  add custom SyncJob actions
        task.setJobActionsClassName(TestSyncJobActions.class.getName());

        //  add user template
        UserTO template = new UserTO();

        AttributeTO attrTO = new AttributeTO();
        attrTO.setSchema("type");
        attrTO.addValue("email == 'test8@syncope.org'? 'TYPE_8': 'TYPE_OTHER'");
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

        SyncTaskTO actual = restTemplate.postForObject(
                BASE_URL + "task/update/sync",
                task, SyncTaskTO.class);
        assertNotNull(actual);
        assertEquals(task.getId(), actual.getId());
        assertEquals(TestSyncJobActions.class.getName(),
                actual.getJobActionsClassName());

        SyncTaskTO taskTO = restTemplate.getForObject(
                BASE_URL + "task/read/{taskId}", SyncTaskTO.class, 4L);

        assertNotNull(taskTO);
        assertNotNull(taskTO.getExecutions());

        // read executions before sync (dryrun test could be executed before)
        int preSyncSize = taskTO.getExecutions().size();

        TaskExecTO execution = restTemplate.postForObject(
                BASE_URL + "task/execute/{taskId}", null,
                TaskExecTO.class, taskTO.getId());
        assertEquals("JOB_FIRED", execution.getStatus());

        int i = 0;
        int maxit = 20;

        // wait for sync completion (executions incremented)
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            taskTO = restTemplate.getForObject(
                    BASE_URL + "task/read/{taskId}",
                    SyncTaskTO.class, taskTO.getId());

            assertNotNull(taskTO);
            assertNotNull(taskTO.getExecutions());

            i++;

        } while (preSyncSize == taskTO.getExecutions().size() && i < maxit);

        // check for sync policy
        userTO = restTemplate.getForObject(BASE_URL + "user/read/{userId}.json",
                UserTO.class, userTO.getId());

        assertNotNull(userTO);
        assertEquals("test9", userTO.getUsername());
        assertEquals("active", userTO.getStatus());
        assertEquals("test9@syncope.org",
                userTO.getAttributeMap().get("email").getValues().get(0));
        assertEquals("test9@syncope.org",
                userTO.getAttributeMap().get("userId").getValues().get(0));
        assertTrue(Integer.valueOf(userTO.getAttributeMap().
                get("fullname").getValues().get(0)) <= 10);

        // check for user template
        userTO = restTemplate.getForObject(
                BASE_URL + "user/read.json?username=test7",
                UserTO.class);
        assertNotNull(userTO);
        assertEquals("TYPE_OTHER",
                userTO.getAttributeMap().get("type").getValues().get(0));
        assertEquals(2, userTO.getResources().size());
        assertTrue(userTO.getResources().contains("resource-testdb"));
        assertTrue(userTO.getResources().contains("ws-target-resource-2"));
        assertEquals(1, userTO.getMemberships().size());
        assertTrue(userTO.getMemberships().get(0).getAttributeMap().
                containsKey("subscriptionDate"));

        userTO = restTemplate.getForObject(
                BASE_URL + "user/read.json?username=test8",
                UserTO.class);
        assertNotNull(userTO);
        assertEquals("TYPE_8",
                userTO.getAttributeMap().get("type").getValues().get(0));

        // check for sync results
        Integer usersPost = restTemplate.getForObject(
                BASE_URL + "user/count.json", Integer.class);
        assertNotNull(usersPost);
        assertTrue("Expected " + (usersPre + 9) + ", found " + usersPost,
                usersPost == usersPre + 9);

        // Check for issue 215: 
        // * expected disabled user test1
        // * expected enabled user test2

        userTO = restTemplate.getForObject(
                BASE_URL + "user/read.json?username=test1",
                UserTO.class);
        assertNotNull(userTO);
        assertEquals("suspended", userTO.getStatus());

        userTO = restTemplate.getForObject(
                BASE_URL + "user/read.json?username=test3",
                UserTO.class);
        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());
    }

    @Test
    public void reconcile() {
        // Update sync task
        SyncTaskTO task = restTemplate.getForObject(
                BASE_URL + "task/read/{taskId}", SyncTaskTO.class, 7);
        assertNotNull(task);

        //  add user template
        UserTO template = new UserTO();

        AttributeTO attrTO = new AttributeTO();
        attrTO.setSchema("type");
        attrTO.addValue("'type a'");
        template.addAttribute(attrTO);

        attrTO = new AttributeTO();
        attrTO.setSchema("userId");
        attrTO.addValue("'reconciled@syncope.org'");
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

        SyncTaskTO actual = restTemplate.postForObject(
                BASE_URL + "task/update/sync", task, SyncTaskTO.class);
        assertNotNull(actual);
        assertEquals(task.getId(), actual.getId());

        // read executions before sync (dryrun test could be executed before)
        int preSyncSize = actual.getExecutions().size();

        TaskExecTO execution = restTemplate.postForObject(
                BASE_URL + "task/execute/{taskId}", null,
                TaskExecTO.class, actual.getId());
        assertEquals("JOB_FIRED", execution.getStatus());

        int i = 0;
        int maxit = 20;

        // wait for sync completion (executions incremented)
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            actual = restTemplate.getForObject(BASE_URL + "task/read/{taskId}",
                    SyncTaskTO.class, actual.getId());

            assertNotNull(actual);
            assertNotNull(actual.getExecutions());

            i++;

        } while (preSyncSize == actual.getExecutions().size() && i < maxit);

        assertEquals(1, actual.getExecutions().size());

        final String status = actual.getExecutions().get(0).getStatus();
        assertNotNull(status);
        assertTrue(PropagationTaskExecStatus.valueOf(status).isSuccessful());

        final UserTO userTO = restTemplate.getForObject(
                BASE_URL + "user/read.json?username=testuser1", UserTO.class);

        assertNotNull(userTO);
        assertEquals("reconciled@syncope.org",
                userTO.getAttributeMap().get("userId").getValues().get(0));
    }

    @Test
    public void issue196() {
        TaskExecTO execution = restTemplate.postForObject(
                BASE_URL + "task/execute/{taskId}", null,
                TaskExecTO.class, 6);
        assertNotNull(execution);
        assertEquals(0, execution.getId());
        assertNotNull(execution.getTask());
    }

    @Test
    public void dryRun() {
        SyncTaskTO taskTO = restTemplate.getForObject(
                BASE_URL + "task/read/{taskId}", SyncTaskTO.class, 4L);

        assertNotNull(taskTO);
        assertNotNull(taskTO.getExecutions());

        int preDryRunSize = taskTO.getExecutions().size();

        TaskExecTO execution = restTemplate.postForObject(
                BASE_URL + "task/execute/{taskId}?dryRun=true", null,
                TaskExecTO.class, 4);
        assertNotNull(execution);

        // wait for sync completion (executions incremented)
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            taskTO = restTemplate.getForObject(
                    BASE_URL + "task/read/{taskId}",
                    SyncTaskTO.class, taskTO.getId());

            assertNotNull(taskTO);
            assertNotNull(taskTO.getExecutions());

        } while (preDryRunSize == taskTO.getExecutions().size());

        assertEquals("SUCCESS", taskTO.getExecutions().get(0).getStatus());
    }
}

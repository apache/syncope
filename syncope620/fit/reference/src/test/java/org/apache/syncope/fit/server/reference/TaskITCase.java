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
package org.apache.syncope.fit.server.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.mod.StatusMod;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.MappingTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.NotificationTO;
import org.apache.syncope.common.lib.to.NotificationTaskTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.ReportExecTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.SyncPolicyTO;
import org.apache.syncope.common.lib.to.SyncTaskTO;
import org.apache.syncope.common.lib.to.TaskExecTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.syncope.common.lib.types.ResourceDeassociationActionType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.SubjectType;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.common.lib.wrap.ResourceName;
import org.apache.syncope.common.rest.api.CollectionWrapper;
import org.apache.syncope.common.rest.api.service.NotificationService;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.server.misc.security.Encryptor;
import org.apache.syncope.server.provisioning.api.job.SyncJob;
import org.apache.syncope.server.provisioning.java.sync.DBPasswordSyncActions;
import org.apache.syncope.server.provisioning.java.sync.LDAPPasswordSyncActions;
import org.identityconnectors.framework.common.objects.Name;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

@FixMethodOrder(MethodSorters.JVM)
public class TaskITCase extends AbstractITCase {

    private static final Long SCHED_TASK_ID = 5L;

    private static final Long SYNC_TASK_ID = 4L;

    private static class ThreadExec implements Callable<TaskExecTO> {

        private final TaskITCase test;

        private final Long taskKey;

        private final int maxWaitSeconds;

        private final boolean dryRun;

        public ThreadExec(TaskITCase test, Long taskKey, int maxWaitSeconds, boolean dryRun) {
            this.test = test;
            this.taskKey = taskKey;
            this.maxWaitSeconds = maxWaitSeconds;
            this.dryRun = dryRun;
        }

        @Override
        public TaskExecTO call() throws Exception {
            return test.execSyncTask(taskKey, maxWaitSeconds, dryRun);
        }
    }

    @BeforeClass
    public static void testSyncActionsSetup() {
        SyncTaskTO syncTask = taskService.read(SYNC_TASK_ID);
        syncTask.getActionsClassNames().add(TestSyncActions.class.getName());
        taskService.update(SYNC_TASK_ID, syncTask);
    }

    /**
     * Remove initial and synchronized users to make test re-runnable.
     */
    private void removeTestUsers() {
        for (int i = 0; i < 10; i++) {
            String cUserName = "test" + i;
            try {
                UserTO cUserTO = readUser(cUserName);
                userService.delete(cUserTO.getKey());
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    @Test
    public void getJobClasses() {
        List<String> jobClasses = syncopeService.info().getTaskJobs();
        assertNotNull(jobClasses);
        assertFalse(jobClasses.isEmpty());
    }

    @Test
    public void getSyncActionsClasses() {
        List<String> actions = syncopeService.info().getSyncActions();
        assertNotNull(actions);
        assertFalse(actions.isEmpty());
    }

    @Test
    public void getPushActionsClasses() {
        List<String> actions = syncopeService.info().getPushActions();
        assertNotNull(actions);
    }

    @Test
    public void createSyncTask() {
        SyncTaskTO task = new SyncTaskTO();
        task.setName("Test create Sync");
        task.setResource(RESOURCE_NAME_WS2);

        UserTO userTemplate = new UserTO();
        userTemplate.getResources().add(RESOURCE_NAME_WS2);

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(8L);
        userTemplate.getMemberships().add(membershipTO);
        task.setUserTemplate(userTemplate);

        RoleTO roleTemplate = new RoleTO();
        roleTemplate.getResources().add(RESOURCE_NAME_LDAP);
        task.setRoleTemplate(roleTemplate);

        Response response = taskService.create(task);
        SyncTaskTO actual = getObject(response.getLocation(), TaskService.class, SyncTaskTO.class);
        assertNotNull(actual);

        task = taskService.read(actual.getKey());
        assertNotNull(task);
        assertEquals(actual.getKey(), task.getKey());
        assertEquals(actual.getJobClassName(), task.getJobClassName());
        assertEquals(userTemplate, task.getUserTemplate());
        assertEquals(roleTemplate, task.getRoleTemplate());
    }

    @Test
    public void createPushTask() {
        PushTaskTO task = new PushTaskTO();
        task.setName("Test create Push");
        task.setResource(RESOURCE_NAME_WS2);
        task.setUserFilter(
                SyncopeClient.getUserSearchConditionBuilder().hasNotResources(RESOURCE_NAME_TESTDB2).query());
        task.setRoleFilter(
                SyncopeClient.getRoleSearchConditionBuilder().isNotNull("cool").query());
        task.setMatchingRule(MatchingRule.LINK);

        final Response response = taskService.create(task);
        final PushTaskTO actual = getObject(response.getLocation(), TaskService.class, PushTaskTO.class);
        assertNotNull(actual);

        task = taskService.read(actual.getKey());
        assertNotNull(task);
        assertEquals(task.getKey(), actual.getKey());
        assertEquals(task.getJobClassName(), actual.getJobClassName());
        assertEquals(task.getUserFilter(), actual.getUserFilter());
        assertEquals(task.getRoleFilter(), actual.getRoleFilter());
        assertEquals(UnmatchingRule.ASSIGN, actual.getUnmatchingRule());
        assertEquals(MatchingRule.LINK, actual.getMatchingRule());
    }

    @Test
    public void update() {
        SchedTaskTO task = taskService.read(SCHED_TASK_ID);
        assertNotNull(task);

        final SchedTaskTO taskMod = new SchedTaskTO();
        taskMod.setKey(5);
        taskMod.setCronExpression(null);

        taskService.update(taskMod.getKey(), taskMod);
        SchedTaskTO actual = taskService.read(taskMod.getKey());
        assertNotNull(actual);
        assertEquals(task.getKey(), actual.getKey());
        assertNull(actual.getCronExpression());
    }

    @Test
    public void listSchedTask() {
        final PagedResult<SchedTaskTO> tasks = taskService.list(TaskType.SCHEDULED);
        assertFalse(tasks.getResult().isEmpty());
        for (AbstractTaskTO task : tasks.getResult()) {
            if (!(task instanceof SchedTaskTO) || task instanceof SyncTaskTO || task instanceof PushTaskTO) {
                fail();
            }
        }
    }

    @Test
    public void listSyncTask() {
        final PagedResult<SyncTaskTO> tasks = taskService.list(TaskType.SYNCHRONIZATION);
        assertFalse(tasks.getResult().isEmpty());
        for (AbstractTaskTO task : tasks.getResult()) {
            if (!(task instanceof SyncTaskTO)) {
                fail();
            }
        }
    }

    @Test
    public void list() {
        final PagedResult<PushTaskTO> tasks = taskService.list(TaskType.PUSH);
        assertFalse(tasks.getResult().isEmpty());
        for (AbstractTaskTO task : tasks.getResult()) {
            if (!(task instanceof PushTaskTO)) {
                fail();
            }
        }
    }

    @Test
    public void paginatedList() {
        PagedResult<PropagationTaskTO> tasks = taskService.list(TaskType.PROPAGATION, 1, 2);

        assertNotNull(tasks);
        assertFalse(tasks.getResult().isEmpty());
        assertEquals(2, tasks.getResult().size());

        for (AbstractTaskTO task : tasks.getResult()) {
            assertNotNull(task);
        }

        tasks = taskService.list(TaskType.PROPAGATION, 2, 2);

        assertNotNull(tasks);
        assertFalse(tasks.getResult().isEmpty());

        for (AbstractTaskTO task : tasks.getResult()) {
            assertNotNull(task);
        }

        tasks = taskService.list(TaskType.PROPAGATION, 1000, 2);

        assertNotNull(tasks);
        assertTrue(tasks.getResult().isEmpty());
    }

    @Test
    public void read() {
        final PropagationTaskTO taskTO = taskService.read(3L);

        assertNotNull(taskTO);
        assertNotNull(taskTO.getExecutions());
        assertTrue(taskTO.getExecutions().isEmpty());

        final PushTaskTO pushTaskTO = taskService.<PushTaskTO>read(17L);
        assertEquals(UnmatchingRule.ASSIGN, pushTaskTO.getUnmatchingRule());
        assertEquals(MatchingRule.UPDATE, pushTaskTO.getMatchingRule());
    }

    @Test
    public void readExecution() {
        TaskExecTO taskTO = taskService.readExecution(6L);
        assertNotNull(taskTO);
    }

    @Test
    // Currently test is not re-runnable.
    // To successfully run test second time it is necessary to restart cargo.
    public void deal() {
        try {
            taskService.delete(0L);
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
        TaskExecTO exec = taskService.execute(1L, false);
        assertEquals(PropagationTaskExecStatus.SUBMITTED.name(), exec.getStatus());

        ReportExecTO report = new ReportExecTO();
        report.setStatus(PropagationTaskExecStatus.SUCCESS.name());
        report.setMessage("OK");
        taskService.report(exec.getKey(), report);
        exec = taskService.readExecution(exec.getKey());
        assertEquals(PropagationTaskExecStatus.SUCCESS.name(), exec.getStatus());
        assertEquals("OK", exec.getMessage());

        taskService.delete(1L);
        try {
            taskService.readExecution(exec.getKey());
        } catch (SyncopeClientException e) {
            assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
        }
    }

    @Test
    public void sync() throws Exception {
        removeTestUsers();

        // -----------------------------
        // Create a new user ... it should be updated applying sync policy
        // -----------------------------
        UserTO inUserTO = new UserTO();
        inUserTO.setPassword("password123");
        String userName = "test9";
        inUserTO.setUsername(userName);
        inUserTO.getPlainAttrs().add(attrTO("firstname", "nome9"));
        inUserTO.getPlainAttrs().add(attrTO("surname", "cognome"));
        inUserTO.getPlainAttrs().add(attrTO("type", "a type"));
        inUserTO.getPlainAttrs().add(attrTO("fullname", "nome cognome"));
        inUserTO.getPlainAttrs().add(attrTO("userId", "puccini@syncope.apache.org"));
        inUserTO.getPlainAttrs().add(attrTO("email", "puccini@syncope.apache.org"));
        inUserTO.getDerAttrs().add(attrTO("csvuserid", null));

        inUserTO = createUser(inUserTO);
        assertNotNull(inUserTO);
        assertFalse(inUserTO.getResources().contains(RESOURCE_NAME_CSV));

        // -----------------------------
        try {
            int usersPre = userService.list(1, 1).getTotalCount();
            assertNotNull(usersPre);

            execSyncTask(SYNC_TASK_ID, 50, false);

            // after execution of the sync task the user data should be synced from
            // csv datasource and processed by user template
            UserTO userTO = userService.read(inUserTO.getKey());
            assertNotNull(userTO);
            assertEquals(userName, userTO.getUsername());
            assertEquals(ActivitiDetector.isActivitiEnabledForUsers(syncopeService)
                    ? "active" : "created", userTO.getStatus());
            assertEquals("test9@syncope.apache.org", userTO.getPlainAttrMap().get("email").getValues().get(0));
            assertEquals("test9@syncope.apache.org", userTO.getPlainAttrMap().get("userId").getValues().get(0));
            assertTrue(Integer.valueOf(userTO.getPlainAttrMap().get("fullname").getValues().get(0)) <= 10);
            assertTrue(userTO.getResources().contains(RESOURCE_NAME_TESTDB));
            assertTrue(userTO.getResources().contains(RESOURCE_NAME_WS2));

            // Matching --> Update (no link)
            assertFalse(userTO.getResources().contains(RESOURCE_NAME_CSV));

            // check for user template
            userTO = readUser("test7");
            assertNotNull(userTO);
            assertEquals("TYPE_OTHER", userTO.getPlainAttrMap().get("type").getValues().get(0));
            assertEquals(3, userTO.getResources().size());
            assertTrue(userTO.getResources().contains(RESOURCE_NAME_TESTDB));
            assertTrue(userTO.getResources().contains(RESOURCE_NAME_WS2));
            assertEquals(1, userTO.getMemberships().size());
            assertTrue(userTO.getMemberships().get(0).getPlainAttrMap().containsKey("subscriptionDate"));

            // Unmatching --> Assign (link)
            assertTrue(userTO.getResources().contains(RESOURCE_NAME_CSV));

            userTO = readUser("test8");
            assertNotNull(userTO);
            assertEquals("TYPE_8", userTO.getPlainAttrMap().get("type").getValues().get(0));

            // check for sync results
            int usersPost = userService.list(1, 1).getTotalCount();
            assertNotNull(usersPost);
            assertEquals(usersPre + 9, usersPost);

            // Check for issue 215:
            // * expected disabled user test1
            // * expected enabled user test2
            userTO = readUser("test1");
            assertNotNull(userTO);
            assertEquals("suspended", userTO.getStatus());

            userTO = readUser("test3");
            assertNotNull(userTO);
            assertEquals("active", userTO.getStatus());

            // SYNCOPE-317
            execSyncTask(SYNC_TASK_ID, 50, false);

            final Set<Long> pushTaskIds = new HashSet<Long>();
            pushTaskIds.add(25L);
            pushTaskIds.add(26L);

            execSyncTasks(pushTaskIds, 50, false);
            // Matching --> UNLINK
            assertFalse(readUser("test9").getResources().contains(RESOURCE_NAME_CSV));
            assertFalse(readUser("test7").getResources().contains(RESOURCE_NAME_CSV));
        } finally {
            removeTestUsers();
        }
    }

    @Test
    public void reconcileFromDB() {
        // update sync task
        TaskExecTO execution = execSyncTask(7L, 50, false);
        assertNotNull(execution.getStatus());
        assertTrue(PropagationTaskExecStatus.valueOf(execution.getStatus()).isSuccessful());

        UserTO userTO = readUser("testuser1");
        assertNotNull(userTO);
        assertEquals("reconciled@syncope.apache.org", userTO.getPlainAttrMap().get("userId").getValues().get(0));
        assertEquals("suspended", userTO.getStatus());

        // enable user on external resource
        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        jdbcTemplate.execute("UPDATE TEST SET STATUS=TRUE");

        // re-execute the same SyncTask: now user must be active
        execution = execSyncTask(7L, 50, false);
        assertNotNull(execution.getStatus());
        assertTrue(PropagationTaskExecStatus.valueOf(execution.getStatus()).isSuccessful());

        userTO = readUser("testuser1");
        assertNotNull(userTO);
        assertEquals("active", userTO.getStatus());
    }

    /**
     * Clean Syncope and LDAP resource status.
     */
    private void ldapCleanup() {
        PagedResult<RoleTO> matchingRoles = roleService.search(
                SyncopeClient.getRoleSearchConditionBuilder().is("name").equalTo("testLDAPGroup").query());
        if (matchingRoles.getSize() > 0) {
            for (RoleTO role : matchingRoles.getResult()) {
                roleService.bulkDeassociation(role.getKey(),
                        ResourceDeassociationActionType.UNLINK,
                        CollectionWrapper.wrap(RESOURCE_NAME_LDAP, ResourceName.class));
                roleService.delete(role.getKey());
            }
        }
        PagedResult<UserTO> matchingUsers = userService.search(
                SyncopeClient.getUserSearchConditionBuilder().is("username").equalTo("syncFromLDAP").query());
        if (matchingUsers.getSize() > 0) {
            for (UserTO user : matchingUsers.getResult()) {
                userService.bulkDeassociation(user.getKey(),
                        ResourceDeassociationActionType.UNLINK,
                        CollectionWrapper.wrap(RESOURCE_NAME_LDAP, ResourceName.class));
                userService.delete(user.getKey());
            }
        }
    }

    @Test
    public void reconcileFromLDAP() {
        // First of all, clear any potential conflict with existing user / role
        ldapCleanup();

        // Update sync task
        TaskExecTO execution = execSyncTask(11L, 50, false);

        // 1. verify execution status
        final String status = execution.getStatus();
        assertNotNull(status);
        assertTrue(PropagationTaskExecStatus.valueOf(status).isSuccessful());

        // 2. verify that synchronized role is found, with expected attributes
        final PagedResult<RoleTO> matchingRoles = roleService.search(
                SyncopeClient.getRoleSearchConditionBuilder().is("name").equalTo("testLDAPGroup").query());
        assertNotNull(matchingRoles);
        assertEquals(1, matchingRoles.getResult().size());

        final PagedResult<UserTO> matchingUsers = userService.search(
                SyncopeClient.getUserSearchConditionBuilder().is("username").equalTo("syncFromLDAP").query());
        assertNotNull(matchingUsers);
        assertEquals(1, matchingUsers.getResult().size());

        // Check for SYNCOPE-436
        assertEquals("syncFromLDAP",
                matchingUsers.getResult().get(0).getVirAttrMap().get("virtualReadOnly").getValues().get(0));
        // Check for SYNCOPE-270
        assertNotNull(matchingUsers.getResult().get(0).getPlainAttrMap().get("obscure"));
        // Check for SYNCOPE-123
        assertNotNull(matchingUsers.getResult().get(0).getPlainAttrMap().get("photo"));

        final RoleTO roleTO = matchingRoles.getResult().iterator().next();
        assertNotNull(roleTO);
        assertEquals("testLDAPGroup", roleTO.getName());
        assertEquals(8L, roleTO.getParent());
        assertEquals("true", roleTO.getPlainAttrMap().get("show").getValues().get(0));
        assertEquals(matchingUsers.getResult().iterator().next().getKey(), (long) roleTO.getUserOwner());
        assertNull(roleTO.getRoleOwner());

        // 3. verify that LDAP group membership is propagated as Syncope role membership
        final PagedResult<UserTO> members = userService.search(
                SyncopeClient.getUserSearchConditionBuilder().hasRoles(roleTO.getKey()).query());
        assertNotNull(members);
        assertEquals(1, members.getResult().size());
    }

    @Test
    public void issue196() {
        TaskExecTO exec = taskService.execute(6L, false);
        assertNotNull(exec);
        assertEquals(0, exec.getKey());
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

    private NotificationTaskTO findNotificationTaskBySender(final String sender) {
        PagedResult<NotificationTaskTO> tasks = taskService.list(TaskType.NOTIFICATION);
        assertNotNull(tasks);
        assertFalse(tasks.getResult().isEmpty());
        NotificationTaskTO taskTO = null;
        for (NotificationTaskTO task : tasks.getResult()) {
            if (sender.equals(task.getSender())) {
                taskTO = task;
            }
        }
        return taskTO;
    }

    private void createNotificationTask(final String sender) {
        // 1. Create notification
        NotificationTO notification = new NotificationTO();
        notification.setTraceLevel(TraceLevel.FAILURES);
        notification.getEvents().add("[REST]:[UserLogic]:[]:[create]:[SUCCESS]");

        notification.setUserAbout(SyncopeClient.getUserSearchConditionBuilder().hasRoles(7L).query());

        notification.setRecipients(SyncopeClient.getUserSearchConditionBuilder().hasRoles(8L).query());
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
        membershipTO.setRoleId(7);
        userTO.getMemberships().add(membershipTO);

        userTO = createUser(userTO);
        assertNotNull(userTO);
    }

    @Test
    public void issueSYNCOPE68() {
        //-----------------------------
        // Create a new user ... it should be updated applying sync policy
        //-----------------------------
        UserTO userTO = new UserTO();
        userTO.setPassword("password123");
        userTO.setUsername("testuser2");

        userTO.getPlainAttrs().add(attrTO("firstname", "testuser2"));
        userTO.getPlainAttrs().add(attrTO("surname", "testuser2"));
        userTO.getPlainAttrs().add(attrTO("type", "a type"));
        userTO.getPlainAttrs().add(attrTO("fullname", "a type"));
        userTO.getPlainAttrs().add(attrTO("userId", "testuser2@syncope.apache.org"));
        userTO.getPlainAttrs().add(attrTO("email", "testuser2@syncope.apache.org"));

        userTO.getResources().add(RESOURCE_NAME_NOPROPAGATION2);
        userTO.getResources().add(RESOURCE_NAME_NOPROPAGATION4);

        MembershipTO membershipTO = new MembershipTO();
        membershipTO.setRoleId(7L);

        userTO.getMemberships().add(membershipTO);

        userTO = createUser(userTO);
        assertNotNull(userTO);
        assertEquals("testuser2", userTO.getUsername());
        assertEquals(1, userTO.getMemberships().size());
        assertEquals(3, userTO.getResources().size());
        //-----------------------------

        try {
            //-----------------------------
            //  add user template
            //-----------------------------
            UserTO template = new UserTO();

            membershipTO = new MembershipTO();
            membershipTO.setRoleId(10L);

            template.getMemberships().add(membershipTO);

            template.getResources().add(RESOURCE_NAME_NOPROPAGATION4);
            //-----------------------------

            // Update sync task
            SyncTaskTO task = taskService.read(9L);
            assertNotNull(task);

            task.setUserTemplate(template);

            taskService.update(task.getKey(), task);
            SyncTaskTO actual = taskService.read(task.getKey());
            assertNotNull(actual);
            assertEquals(task.getKey(), actual.getKey());
            assertFalse(actual.getUserTemplate().getResources().isEmpty());
            assertFalse(actual.getUserTemplate().getMemberships().isEmpty());

            TaskExecTO execution = execSyncTask(actual.getKey(), 50, false);
            final String status = execution.getStatus();
            assertNotNull(status);
            assertTrue(PropagationTaskExecStatus.valueOf(status).isSuccessful());

            userTO = readUser("testuser2");
            assertNotNull(userTO);
            assertEquals("testuser2@syncope.apache.org", userTO.getPlainAttrMap().get("userId").getValues().get(0));
            assertEquals(2, userTO.getMemberships().size());
            assertEquals(4, userTO.getResources().size());
        } finally {
            UserTO dUserTO = deleteUser(userTO.getKey());
            assertNotNull(dUserTO);
        }
    }

    @Test
    public void issueSYNCOPE144() {
        SchedTaskTO task = new SchedTaskTO();
        task.setName("issueSYNCOPE144");
        task.setDescription("issueSYNCOPE144 Description");
        task.setJobClassName(SyncJob.class.getName());

        Response response = taskService.create(task);
        SchedTaskTO actual = getObject(response.getLocation(), TaskService.class, SchedTaskTO.class);
        assertNotNull(actual);
        assertEquals("issueSYNCOPE144", actual.getName());
        assertEquals("issueSYNCOPE144 Description", actual.getDescription());

        task = taskService.read(actual.getKey());
        assertNotNull(task);
        assertEquals("issueSYNCOPE144", task.getName());
        assertEquals("issueSYNCOPE144 Description", task.getDescription());

        task.setName("issueSYNCOPE144_2");
        task.setDescription("issueSYNCOPE144 Description_2");

        response = taskService.create(task);
        actual = getObject(response.getLocation(), TaskService.class, SchedTaskTO.class);
        assertNotNull(actual);
        assertEquals("issueSYNCOPE144_2", actual.getName());
        assertEquals("issueSYNCOPE144 Description_2", actual.getDescription());
    }

    @Test
    public void issueSYNCOPE230() {
        // 1. read SyncTask for resource-db-sync (table TESTSYNC on external H2)
        execSyncTask(10L, 50, false);

        // 3. read e-mail address for user created by the SyncTask first execution
        UserTO userTO = readUser("issuesyncope230");
        assertNotNull(userTO);
        String email = userTO.getPlainAttrMap().get("email").getValues().iterator().next();
        assertNotNull(email);

        // 4. update TESTSYNC on external H2 by changing e-mail address
        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        jdbcTemplate.execute("UPDATE TESTSYNC SET email='updatedSYNCOPE230@syncope.apache.org'");

        // 5. re-execute the SyncTask
        execSyncTask(10L, 50, false);

        // 6. verify that the e-mail was updated
        userTO = readUser("issuesyncope230");
        assertNotNull(userTO);
        email = userTO.getPlainAttrMap().get("email").getValues().iterator().next();
        assertNotNull(email);
        assertEquals("updatedSYNCOPE230@syncope.apache.org", email);
    }

    private TaskExecTO execSyncTask(final Long taskKey, final int maxWaitSeconds, final boolean dryRun) {
        AbstractTaskTO taskTO = taskService.read(taskKey);
        assertNotNull(taskTO);
        assertNotNull(taskTO.getExecutions());

        int preSyncSize = taskTO.getExecutions().size();
        TaskExecTO execution = taskService.execute(taskTO.getKey(), dryRun);
        assertEquals("JOB_FIRED", execution.getStatus());

        int i = 0;
        int maxit = maxWaitSeconds;

        // wait for sync completion (executions incremented)
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            taskTO = taskService.read(taskTO.getKey());

            assertNotNull(taskTO);
            assertNotNull(taskTO.getExecutions());

            i++;
        } while (preSyncSize == taskTO.getExecutions().size() && i < maxit);
        if (i == maxit) {
            fail("Timeout when executing task " + taskKey);
        }
        return taskTO.getExecutions().get(taskTO.getExecutions().size() - 1);
    }

    private Map<Long, TaskExecTO> execSyncTasks(
            final Set<Long> taskKeys, final int maxWaitSeconds, final boolean dryRun) throws Exception {

        final ExecutorService service = Executors.newFixedThreadPool(taskKeys.size());
        final List<Future<TaskExecTO>> futures = new ArrayList<>();

        for (final Long id : taskKeys) {
            futures.add(service.submit(new ThreadExec(this, id, maxWaitSeconds, dryRun)));
        }

        final Map<Long, TaskExecTO> res = new HashMap<>();

        for (Future<TaskExecTO> f : futures) {
            TaskExecTO taskExecTO = f.get(100, TimeUnit.SECONDS);
            res.put(taskExecTO.getTask(), taskExecTO);
        }

        service.shutdownNow();

        return res;
    }

    @Test
    public void issueSYNCOPE272() {
        removeTestUsers();

        // create user with testdb resource
        UserTO userTO = UserITCase.getUniqueSampleTO("syncope272@syncope.apache.org");
        userTO.getResources().add(RESOURCE_NAME_TESTDB);

        userTO = createUser(userTO);
        try {
            assertNotNull(userTO);
            assertEquals(1, userTO.getPropagationStatusTOs().size());
            assertTrue(userTO.getPropagationStatusTOs().get(0).getStatus().isSuccessful());

            TaskExecTO taskExecTO = execSyncTask(24L, 50, false);

            assertNotNull(taskExecTO.getStatus());
            assertTrue(PropagationTaskExecStatus.valueOf(taskExecTO.getStatus()).isSuccessful());

            userTO = userService.read(userTO.getKey());
            assertNotNull(userTO);
            assertNotNull(userTO.getPlainAttrMap().get("firstname").getValues().get(0));
        } finally {
            removeTestUsers();
        }
    }

    @Test
    public void issueSYNCOPE258() {
        // -----------------------------
        // Add a custom correlation rule
        // -----------------------------
        SyncPolicyTO policyTO = policyService.read(9L);
        policyTO.getSpecification().setUserJavaRule(TestSyncRule.class.getName());

        policyService.update(policyTO.getKey(), policyTO);
        // -----------------------------

        SyncTaskTO task = new SyncTaskTO();
        task.setName("Test Sync Rule");
        task.setResource(RESOURCE_NAME_WS2);
        task.setFullReconciliation(true);
        task.setPerformCreate(true);
        task.setPerformDelete(true);
        task.setPerformUpdate(true);

        Response response = taskService.create(task);
        SyncTaskTO actual = getObject(response.getLocation(), TaskService.class, SyncTaskTO.class);
        assertNotNull(actual);

        UserTO userTO = UserITCase.getUniqueSampleTO("s258_1@apache.org");
        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_WS2);

        createUser(userTO);

        userTO = UserITCase.getUniqueSampleTO("s258_2@apache.org");
        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_WS2);

        userTO = createUser(userTO);

        // change email in order to unmatch the second user
        UserMod userMod = new UserMod();
        userMod.setKey(userTO.getKey());
        userMod.getPlainAttrsToRemove().add("email");
        userMod.getPlainAttrsToUpdate().add(attrMod("email", "s258@apache.org"));

        userService.update(userMod.getKey(), userMod);

        execSyncTask(actual.getKey(), 50, false);

        SyncTaskTO executed = taskService.read(actual.getKey());
        assertEquals(1, executed.getExecutions().size());

        // asser for just one match
        assertTrue(executed.getExecutions().get(0).getMessage().substring(0, 55) + "...",
                executed.getExecutions().get(0).getMessage().contains("[updated/failures]: 1/0"));
    }

    @Test
    public void issueSYNCOPE307() {
        UserTO userTO = UserITCase.getUniqueSampleTO("s307@apache.org");

        AttrTO csvuserid = new AttrTO();
        csvuserid.setSchema("csvuserid");
        userTO.getDerAttrs().add(csvuserid);

        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_WS2);
        userTO.getResources().add(RESOURCE_NAME_CSV);

        userTO = createUser(userTO);
        assertNotNull(userTO);

        userTO = userService.read(userTO.getKey());
        assertEquals("virtualvalue", userTO.getVirAttrMap().get("virtualdata").getValues().get(0));

        // Update sync task
        SyncTaskTO task = taskService.read(12L);
        assertNotNull(task);

        //  add user template
        UserTO template = new UserTO();
        template.getResources().add(RESOURCE_NAME_DBVIRATTR);

        AttrTO userId = attrTO("userId", "'s307@apache.org'");
        template.getPlainAttrs().add(userId);

        AttrTO email = attrTO("email", "'s307@apache.org'");
        template.getPlainAttrs().add(email);

        task.setUserTemplate(template);

        taskService.update(task.getKey(), task);
        execSyncTask(task.getKey(), 50, false);

        // check for sync policy
        userTO = userService.read(userTO.getKey());
        assertEquals("virtualvalue", userTO.getVirAttrMap().get("virtualdata").getValues().get(0));

        try {
            final JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);

            String value = jdbcTemplate.queryForObject(
                    "SELECT USERNAME FROM testsync WHERE ID=?", String.class, userTO.getKey());
            assertEquals("virtualvalue", value);
        } catch (EmptyResultDataAccessException e) {
            assertTrue(false);
        }
    }

    @Test
    public void bulkAction() {
        final PagedResult<PropagationTaskTO> before = taskService.list(TaskType.PROPAGATION);

        // create user with testdb resource
        final UserTO userTO = UserITCase.getUniqueSampleTO("taskBulk@apache.org");
        userTO.getResources().add(RESOURCE_NAME_TESTDB);
        createUser(userTO);

        final List<PropagationTaskTO> after = new ArrayList<PropagationTaskTO>(
                taskService.<PropagationTaskTO>list(TaskType.PROPAGATION).getResult());

        after.removeAll(before.getResult());

        assertFalse(after.isEmpty());

        final BulkAction bulkAction = new BulkAction();
        bulkAction.setOperation(BulkAction.Type.DELETE);

        for (AbstractTaskTO taskTO : after) {
            bulkAction.getTargets().add(String.valueOf(taskTO.getKey()));
        }

        taskService.bulk(bulkAction);

        assertFalse(taskService.list(TaskType.PROPAGATION).getResult().containsAll(after));
    }

    @Test
    public void pushMatchingUnmatchingRoles() {
        assertFalse(roleService.read(3L).getResources().contains(RESOURCE_NAME_LDAP));

        execSyncTask(23L, 50, false);

        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, 3L));
        assertTrue(roleService.read(3L).getResources().contains(RESOURCE_NAME_LDAP));

        execSyncTask(23L, 50, false);

        assertNotNull(resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.ROLE, 3L));
        assertFalse(roleService.read(3L).getResources().contains(RESOURCE_NAME_LDAP));
    }

    @Test
    public void pushUnmatchingUsers() throws Exception {
        assertFalse(userService.read(2L).getResources().contains(RESOURCE_NAME_TESTDB2));
        assertFalse(userService.read(3L).getResources().contains(RESOURCE_NAME_TESTDB2));
        assertFalse(userService.read(4L).getResources().contains(RESOURCE_NAME_TESTDB2));
        assertTrue(userService.read(5L).getResources().contains(RESOURCE_NAME_TESTDB2));

        final JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        assertEquals(0, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='puccini'").size());

        // ------------------------------------------
        // Unmatching --> Assign --> dryRuyn
        // ------------------------------------------
        execSyncTask(13L, 50, true);
        assertEquals(0, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='vivaldi'").size());
        assertFalse(userService.read(3L).getResources().contains(RESOURCE_NAME_TESTDB2));
        // ------------------------------------------

        final Set<Long> pushTaskIds = new HashSet<>();
        pushTaskIds.add(13L);
        pushTaskIds.add(14L);
        pushTaskIds.add(15L);
        pushTaskIds.add(16L);
        execSyncTasks(pushTaskIds, 50, false);

        // ------------------------------------------
        // Unatching --> Ignore
        // ------------------------------------------
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='verdi'").size());
        assertFalse(userService.read(2L).getResources().contains(RESOURCE_NAME_TESTDB2));
        // ------------------------------------------

        // ------------------------------------------
        // Unmatching --> Assign
        // ------------------------------------------
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='vivaldi'").size());
        assertTrue(userService.read(3L).getResources().contains(RESOURCE_NAME_TESTDB2));
        jdbcTemplate.execute("DELETE FROM test2 WHERE ID='vivaldi'");
        // ------------------------------------------

        // ------------------------------------------
        // Unmatching --> Provision
        // ------------------------------------------
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='bellini'").size());
        assertFalse(userService.read(4L).getResources().contains(RESOURCE_NAME_TESTDB2));
        jdbcTemplate.execute("DELETE FROM test2 WHERE ID='bellini'");
        // ------------------------------------------

        // ------------------------------------------
        // Unmatching --> Unlink
        // ------------------------------------------
        assertEquals(0, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='puccini'").size());
        assertFalse(userService.read(5L).getResources().contains(RESOURCE_NAME_TESTDB2));
        // ------------------------------------------
    }

    @Test
    public void pushMatchingUser() throws Exception {
        assertTrue(userService.read(1L).getResources().contains(RESOURCE_NAME_TESTDB2));
        assertFalse(userService.read(2L).getResources().contains(RESOURCE_NAME_TESTDB2));

        final JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='verdi'").size());
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='rossini'").size());

        // ------------------------------------------
        // Matching --> Deprovision --> dryRuyn
        // ------------------------------------------
        execSyncTask(19L, 50, true);
        assertTrue(userService.read(1L).getResources().contains(RESOURCE_NAME_TESTDB2));
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='rossini'").size());
        // ------------------------------------------

        final Set<Long> pushTaskIds = new HashSet<>();
        pushTaskIds.add(18L);
        pushTaskIds.add(19L);
        pushTaskIds.add(16L);

        execSyncTasks(pushTaskIds, 50, false);

        // ------------------------------------------
        // Matching --> Deprovision && Ignore
        // ------------------------------------------
        assertFalse(userService.read(2L).getResources().contains(RESOURCE_NAME_TESTDB2));
        // DELETE Capability not available ....
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='verdi'").size());
        // ------------------------------------------

        // ------------------------------------------
        // Matching --> Unassign
        // ------------------------------------------
        assertFalse(userService.read(1L).getResources().contains(RESOURCE_NAME_TESTDB2));
        // DELETE Capability not available ....
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='rossini'").size());
        // ------------------------------------------

        // ------------------------------------------
        // Matching --> Link
        // ------------------------------------------
        execSyncTask(20L, 50, false);
        assertTrue(userService.read(2L).getResources().contains(RESOURCE_NAME_TESTDB2));
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='verdi'").size());
        // ------------------------------------------

        pushTaskIds.clear();
        pushTaskIds.add(21L);
        pushTaskIds.add(22L);

        execSyncTasks(pushTaskIds, 50, false);

        // ------------------------------------------
        // Matching --> Unlink && Update
        // ------------------------------------------
        assertFalse(userService.read(2L).getResources().contains(RESOURCE_NAME_TESTDB2));
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='verdi'").size());
        // ------------------------------------------
    }

    @Test
    public void issueSYNCOPE313DB() throws Exception {
        // 1. create user in DB
        UserTO user = UserITCase.getUniqueSampleTO("syncope313-db@syncope.apache.org");
        user.setPassword("security");
        user.getResources().add(RESOURCE_NAME_TESTDB);
        user = createUser(user);
        assertNotNull(user);
        assertFalse(user.getResources().isEmpty());

        // 2. Check that the DB resource has the correct password
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        String value = jdbcTemplate.queryForObject(
                "SELECT PASSWORD FROM test WHERE ID=?", String.class, user.getUsername());
        assertEquals(Encryptor.getInstance().encode("security", CipherAlgorithm.SHA1), value.toUpperCase());

        // 3. Update the password in the DB
        String newPassword = Encryptor.getInstance().encode("new-security", CipherAlgorithm.SHA1);
        jdbcTemplate.execute(
                "UPDATE test set PASSWORD='" + newPassword + "' where ID='" + user.getUsername() + "'");

        // 4. Sync the user from the resource
        SyncTaskTO syncTask = new SyncTaskTO();
        syncTask.setName("DB Sync Task");
        syncTask.setPerformCreate(true);
        syncTask.setPerformUpdate(true);
        syncTask.setFullReconciliation(true);
        syncTask.setResource(RESOURCE_NAME_TESTDB);
        syncTask.getActionsClassNames().add(DBPasswordSyncActions.class.getName());
        Response taskResponse = taskService.create(syncTask);

        SyncTaskTO actual = getObject(taskResponse.getLocation(), TaskService.class, SyncTaskTO.class);
        assertNotNull(actual);

        syncTask = taskService.read(actual.getKey());
        assertNotNull(syncTask);
        assertEquals(actual.getKey(), syncTask.getKey());
        assertEquals(actual.getJobClassName(), syncTask.getJobClassName());

        TaskExecTO execution = execSyncTask(syncTask.getKey(), 50, false);
        final String status = execution.getStatus();
        assertNotNull(status);
        assertTrue(PropagationTaskExecStatus.valueOf(status).isSuccessful());

        // 5. Test the sync'd user
        UserTO updatedUser = userService.read(user.getKey());
        assertEquals(newPassword, updatedUser.getPassword());

        // 6. Delete SyncTask + user
        taskService.delete(syncTask.getKey());
        deleteUser(user.getKey());
    }

    @Test
    public void issueSYNCOPE313LDAP() throws Exception {
        // First of all, clear any potential conflict with existing user / role
        ldapCleanup();

        // 1. create user in LDAP
        UserTO user = UserITCase.getUniqueSampleTO("syncope313-ldap@syncope.apache.org");
        user.setPassword("security");
        user.getResources().add(RESOURCE_NAME_LDAP);
        user = createUser(user);
        assertNotNull(user);
        assertFalse(user.getResources().isEmpty());

        // 2. request to change password only on Syncope and not on LDAP
        UserMod userMod = new UserMod();
        userMod.setKey(user.getKey());
        userMod.setPassword("new-security");
        StatusMod pwdPropRequest = new StatusMod();
        pwdPropRequest.setOnSyncope(true);
        pwdPropRequest.getResourceNames().clear();
        userMod.setPwdPropRequest(pwdPropRequest);
        updateUser(userMod);

        // 3. Check that the Syncope user now has the changed password
        UserTO updatedUser = userService.read(user.getKey());
        String encodedNewPassword = Encryptor.getInstance().encode("new-security", CipherAlgorithm.SHA1);
        assertEquals(encodedNewPassword, updatedUser.getPassword());

        // 4. Check that the LDAP resource has the old password
        ConnObjectTO connObject =
                resourceService.getConnectorObject(RESOURCE_NAME_LDAP, SubjectType.USER, user.getKey());
        assertNotNull(getLdapRemoteObject(
                connObject.getPlainAttrMap().get(Name.NAME).getValues().get(0),
                "security",
                connObject.getPlainAttrMap().get(Name.NAME).getValues().get(0)));

        // 5. Update the LDAP Connector to retrieve passwords
        ResourceTO ldapResource = resourceService.read(RESOURCE_NAME_LDAP);
        ConnInstanceTO resourceConnector = connectorService.read(ldapResource.getConnectorId());
        ConnConfProperty property = resourceConnector.getConfigurationMap().get("retrievePasswordsWithSearch");
        property.getValues().clear();
        property.getValues().add(Boolean.TRUE);
        connectorService.update(ldapResource.getConnectorId(), resourceConnector);

        // 6. Sync the user from the resource
        SyncTaskTO syncTask = new SyncTaskTO();
        syncTask.setName("LDAP Sync Task");
        syncTask.setPerformCreate(true);
        syncTask.setPerformUpdate(true);
        syncTask.setFullReconciliation(true);
        syncTask.setResource(RESOURCE_NAME_LDAP);
        syncTask.getActionsClassNames().add(LDAPPasswordSyncActions.class.getName());
        Response taskResponse = taskService.create(syncTask);

        SyncTaskTO actual = getObject(taskResponse.getLocation(), TaskService.class, SyncTaskTO.class);
        assertNotNull(actual);

        syncTask = taskService.read(actual.getKey());
        assertNotNull(syncTask);
        assertEquals(actual.getKey(), syncTask.getKey());
        assertEquals(actual.getJobClassName(), syncTask.getJobClassName());

        TaskExecTO execution = execSyncTask(syncTask.getKey(), 50, false);
        final String status = execution.getStatus();
        assertNotNull(status);
        assertTrue(PropagationTaskExecStatus.valueOf(status).isSuccessful());

        // 7. Test the sync'd user
        String syncedPassword = Encryptor.getInstance().encode("security", CipherAlgorithm.SHA1);
        updatedUser = userService.read(user.getKey());
        assertEquals(syncedPassword, updatedUser.getPassword());

        // 8. Delete SyncTask + user + reset the connector
        taskService.delete(syncTask.getKey());
        property.getValues().clear();
        property.getValues().add(Boolean.FALSE);
        connectorService.update(ldapResource.getConnectorId(), resourceConnector);
        deleteUser(updatedUser.getKey());
    }

    @Test
    public void issueSYNCOPE598() {
        // create a new role schema
        final PlainSchemaTO schemaTO = new PlainSchemaTO();
        schemaTO.setKey("LDAPGroupName" + getUUIDString());
        schemaTO.setType(AttrSchemaType.String);
        schemaTO.setMandatoryCondition("true");

        final PlainSchemaTO newPlainSchemaTO = createSchema(AttributableType.ROLE, SchemaType.PLAIN, schemaTO);
        assertEquals(schemaTO, newPlainSchemaTO);

        // create a new sample role
        RoleTO roleTO = new RoleTO();
        roleTO.setName("all" + getUUIDString());
        roleTO.setParent(8L);

        roleTO.getRAttrTemplates().add(newPlainSchemaTO.getKey());
        roleTO.getPlainAttrs().add(attrTO(newPlainSchemaTO.getKey(), "all"));

        roleTO = createRole(roleTO);
        assertNotNull(roleTO);

        String resourceName = "resource-ldap-roleonly";
        ResourceTO newResourceTO = null;

        try {
            // Create resource ad-hoc
            ResourceTO resourceTO = new ResourceTO();
            resourceTO.setKey(resourceName);
            resourceTO.setConnectorId(105L);

            final MappingTO umapping = new MappingTO();
            MappingItemTO item = new MappingItemTO();
            item.setIntMappingType(IntMappingType.Username);
            item.setExtAttrName("cn");
            item.setAccountid(true);
            item.setPurpose(MappingPurpose.PROPAGATION);
            item.setMandatoryCondition("true");
            umapping.setAccountIdItem(item);

            item = new MappingItemTO();
            item.setIntMappingType(IntMappingType.UserPlainSchema);
            item.setExtAttrName("surname");
            item.setIntAttrName("sn");
            item.setPurpose(MappingPurpose.BOTH);
            umapping.addItem(item);

            item = new MappingItemTO();
            item.setIntMappingType(IntMappingType.UserPlainSchema);
            item.setExtAttrName("email");
            item.setIntAttrName("mail");
            item.setPurpose(MappingPurpose.BOTH);
            umapping.addItem(item);

            item = new MappingItemTO();
            item.setIntMappingType(IntMappingType.Password);
            item.setPassword(true);
            item.setPurpose(MappingPurpose.BOTH);
            item.setMandatoryCondition("true");
            umapping.addItem(item);

            umapping.setAccountLink("'cn=' + username + ',ou=people,o=isp'");

            final MappingTO rmapping = new MappingTO();

            item = new MappingItemTO();
            item.setIntMappingType(IntMappingType.RolePlainSchema);
            item.setExtAttrName("cn");
            item.setIntAttrName(newPlainSchemaTO.getKey());
            item.setAccountid(true);
            item.setPurpose(MappingPurpose.BOTH);
            rmapping.setAccountIdItem(item);

            rmapping.setAccountLink("'cn=' + " + newPlainSchemaTO.getKey() + " + ',ou=groups,o=isp'");

            resourceTO.setRmapping(rmapping);

            Response response = resourceService.create(resourceTO);
            newResourceTO = getObject(response.getLocation(), ResourceService.class, ResourceTO.class);

            assertNotNull(newResourceTO);
            assertNull(newResourceTO.getUmapping());
            assertNotNull(newResourceTO.getRmapping());

            // create push task ad-hoc
            final PushTaskTO task = new PushTaskTO();
            task.setName("issueSYNCOPE598");
            task.setResource(resourceName);
            task.setPerformCreate(true);
            task.setPerformDelete(true);
            task.setPerformUpdate(true);
            task.setUnmatchingRule(UnmatchingRule.ASSIGN);
            task.setMatchingRule(MatchingRule.UPDATE);

            response = taskService.create(task);
            final PushTaskTO push = getObject(response.getLocation(), TaskService.class, PushTaskTO.class);

            assertNotNull(push);

            // execute the new task
            final TaskExecTO pushExec = execSyncTask(push.getKey(), 50, false);
            assertTrue(PropagationTaskExecStatus.valueOf(pushExec.getStatus()).isSuccessful());
        } finally {
            roleService.delete(roleTO.getKey());
            if (newResourceTO != null) {
                resourceService.delete(resourceName);
            }
        }
    }
}

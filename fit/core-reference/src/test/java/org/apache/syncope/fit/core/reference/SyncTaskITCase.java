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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.Response;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.patch.DeassociationPatch;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.policy.SyncPolicyTO;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.SyncTaskTO;
import org.apache.syncope.common.lib.to.TaskExecTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;
import org.apache.syncope.common.lib.types.SyncMode;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.beans.AnySearchQuery;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.core.misc.security.Encryptor;
import org.apache.syncope.core.provisioning.java.sync.DBPasswordSyncActions;
import org.apache.syncope.core.provisioning.java.sync.LDAPPasswordSyncActions;
import org.identityconnectors.framework.common.objects.Name;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

@FixMethodOrder(MethodSorters.JVM)
public class SyncTaskITCase extends AbstractTaskITCase {

    @BeforeClass
    public static void testSyncActionsSetup() {
        SyncTaskTO syncTask = taskService.read(SYNC_TASK_ID, true);
        syncTask.getActionsClassNames().add(TestSyncActions.class.getName());
        taskService.update(syncTask);
    }

    @Test
    public void getSyncActionsClasses() {
        Set<String> actions = syncopeService.info().getSyncActions();
        assertNotNull(actions);
        assertFalse(actions.isEmpty());
    }

    @Test
    public void list() {
        PagedResult<SyncTaskTO> tasks = taskService.list(
                new TaskQuery.Builder().type(TaskType.SYNCHRONIZATION).build());
        assertFalse(tasks.getResult().isEmpty());
        for (AbstractTaskTO task : tasks.getResult()) {
            if (!(task instanceof SyncTaskTO)) {
                fail();
            }
        }
    }

    @Test
    public void create() {
        SyncTaskTO task = new SyncTaskTO();
        task.setName("Test create Sync");
        task.setDestinationRealm("/");
        task.setResource(RESOURCE_NAME_WS2);
        task.setSyncMode(SyncMode.FULL_RECONCILIATION);

        UserTO userTemplate = new UserTO();
        userTemplate.getResources().add(RESOURCE_NAME_WS2);

        userTemplate.getMemberships().add(new MembershipTO.Builder().group(8L).build());
        task.getTemplates().put(AnyTypeKind.USER.name(), userTemplate);

        GroupTO groupTemplate = new GroupTO();
        groupTemplate.getResources().add(RESOURCE_NAME_LDAP);
        task.getTemplates().put(AnyTypeKind.GROUP.name(), groupTemplate);

        Response response = taskService.create(task);
        SyncTaskTO actual = getObject(response.getLocation(), TaskService.class, SyncTaskTO.class);
        assertNotNull(actual);

        task = taskService.read(actual.getKey(), true);
        assertNotNull(task);
        assertEquals(actual.getKey(), task.getKey());
        assertEquals(actual.getJobDelegateClassName(), task.getJobDelegateClassName());
        assertEquals(userTemplate, task.getTemplates().get(AnyTypeKind.USER.name()));
        assertEquals(groupTemplate, task.getTemplates().get(AnyTypeKind.GROUP.name()));
    }

    @Test
    public void sync() throws Exception {
        removeTestUsers();

        // -----------------------------
        // Create a new user ... it should be updated applying sync policy
        // -----------------------------
        UserTO inUserTO = new UserTO();
        inUserTO.setRealm(SyncopeConstants.ROOT_REALM);
        inUserTO.setPassword("password123");
        String userName = "test9";
        inUserTO.setUsername(userName);
        inUserTO.getPlainAttrs().add(attrTO("firstname", "nome9"));
        inUserTO.getPlainAttrs().add(attrTO("surname", "cognome"));
        inUserTO.getPlainAttrs().add(attrTO("type", "a type"));
        inUserTO.getPlainAttrs().add(attrTO("fullname", "nome cognome"));
        inUserTO.getPlainAttrs().add(attrTO("userId", "puccini@syncope.apache.org"));
        inUserTO.getPlainAttrs().add(attrTO("email", "puccini@syncope.apache.org"));
        inUserTO.getAuxClasses().add("csv");
        inUserTO.getDerAttrs().add(attrTO("csvuserid", null));

        inUserTO = createUser(inUserTO).getAny();
        assertNotNull(inUserTO);
        assertFalse(inUserTO.getResources().contains(RESOURCE_NAME_CSV));

        // -----------------------------
        try {
            int usersPre = userService.list(
                    new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                    page(1).size(1).build()).getTotalCount();
            assertNotNull(usersPre);

            execProvisioningTask(taskService, SYNC_TASK_ID, 50, false);

            // after execution of the sync task the user data should have been synced from CSV
            // and processed by user template
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
            assertEquals(8, userTO.getMemberships().get(0).getRightKey());

            // Unmatching --> Assign (link) - SYNCOPE-658
            assertTrue(userTO.getResources().contains(RESOURCE_NAME_CSV));
            assertEquals(1, IterableUtils.countMatches(userTO.getDerAttrs(), new Predicate<AttrTO>() {

                @Override
                public boolean evaluate(final AttrTO attributeTO) {
                    return "csvuserid".equals(attributeTO.getSchema());
                }
            }));

            userTO = readUser("test8");
            assertNotNull(userTO);
            assertEquals("TYPE_8", userTO.getPlainAttrMap().get("type").getValues().get(0));

            // Check for ignored user - SYNCOPE-663
            try {
                readUser("test2");
                fail();
            } catch (SyncopeClientException e) {
                assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
            }

            // check for sync results
            int usersPost = userService.list(
                    new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                    page(1).size(1).build()).getTotalCount();
            assertNotNull(usersPost);
            assertEquals(usersPre + 8, usersPost);

            // Check for issue 215:
            // * expected disabled user test1
            // * expected enabled user test2
            userTO = readUser("test1");
            assertNotNull(userTO);
            assertEquals("suspended", userTO.getStatus());

            userTO = readUser("test3");
            assertNotNull(userTO);
            assertEquals("active", userTO.getStatus());

            Set<Long> otherSyncTaskKeys = new HashSet<>();
            otherSyncTaskKeys.add(25L);
            otherSyncTaskKeys.add(26L);
            execProvisioningTasks(taskService, otherSyncTaskKeys, 50, false);

            // Matching --> UNLINK
            assertFalse(readUser("test9").getResources().contains(RESOURCE_NAME_CSV));
            assertFalse(readUser("test7").getResources().contains(RESOURCE_NAME_CSV));
        } finally {
            removeTestUsers();
        }
    }

    @Test
    public void dryRun() {
        TaskExecTO execution = execProvisioningTask(taskService, SYNC_TASK_ID, 50, true);
        assertEquals(
                "Execution of task " + execution.getTask() + " failed with message " + execution.getMessage(),
                "SUCCESS", execution.getStatus());
    }

    @Test
    public void reconcileFromDB() {
        UserTO userTO = null;
        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        try {
            TaskExecTO execution = execProvisioningTask(taskService, 7L, 50, false);
            assertNotNull(execution.getStatus());
            assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(execution.getStatus()));

            userTO = readUser("testuser1");
            assertNotNull(userTO);
            assertEquals("reconciled@syncope.apache.org", userTO.getPlainAttrMap().get("userId").getValues().get(0));
            assertEquals("suspended", userTO.getStatus());

            // enable user on external resource
            jdbcTemplate.execute("UPDATE TEST SET status=TRUE WHERE id='testuser1'");

            // re-execute the same SyncTask: now user must be active
            execution = execProvisioningTask(taskService, 7L, 50, false);
            assertNotNull(execution.getStatus());
            assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(execution.getStatus()));

            userTO = readUser("testuser1");
            assertNotNull(userTO);
            assertEquals("active", userTO.getStatus());
        } finally {
            jdbcTemplate.execute("UPDATE TEST SET status=FALSE WHERE id='testUser1'");
            if (userTO != null) {
                userService.delete(userTO.getKey());
            }
        }
    }

    /**
     * Clean Syncope and LDAP resource status.
     */
    private void ldapCleanup() {
        PagedResult<GroupTO> matchingGroups = groupService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getGroupSearchConditionBuilder().is("name").equalTo("testLDAPGroup").query()).
                build());
        if (matchingGroups.getSize() > 0) {
            for (GroupTO group : matchingGroups.getResult()) {
                DeassociationPatch deassociationPatch = new DeassociationPatch();
                deassociationPatch.setKey(group.getKey());
                deassociationPatch.setAction(ResourceDeassociationAction.UNLINK);
                deassociationPatch.getResources().add(RESOURCE_NAME_LDAP);
                groupService.deassociate(deassociationPatch);
                groupService.delete(group.getKey());
            }
        }
        PagedResult<UserTO> matchingUsers = userService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().is("username").equalTo("syncFromLDAP").query()).
                build());
        if (matchingUsers.getSize() > 0) {
            for (UserTO user : matchingUsers.getResult()) {
                DeassociationPatch deassociationPatch = new DeassociationPatch();
                deassociationPatch.setKey(user.getKey());
                deassociationPatch.setAction(ResourceDeassociationAction.UNLINK);
                deassociationPatch.getResources().add(RESOURCE_NAME_LDAP);
                userService.deassociate(deassociationPatch);
                userService.delete(user.getKey());
            }
        }
    }

    @Test
    public void reconcileFromLDAP() {
        // First of all, clear any potential conflict with existing user / group
        ldapCleanup();

        // 0. synchronize
        TaskExecTO execution = execProvisioningTask(taskService, 11L, 50, false);

        // 1. verify execution status
        assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(execution.getStatus()));

        // 2. verify that synchronized group is found
        PagedResult<GroupTO> matchingGroups = groupService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getGroupSearchConditionBuilder().is("name").equalTo("testLDAPGroup").query()).
                build());
        assertNotNull(matchingGroups);
        assertEquals(1, matchingGroups.getResult().size());

        // 3. verify that synchronized user is found
        PagedResult<UserTO> matchingUsers = userService.search(
                new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getUserSearchConditionBuilder().is("username").equalTo("syncFromLDAP").query()).
                build());
        assertNotNull(matchingUsers);
        assertEquals(1, matchingUsers.getResult().size());

        // Check for SYNCOPE-436
        assertEquals("syncFromLDAP",
                matchingUsers.getResult().get(0).getVirAttrMap().get("virtualReadOnly").getValues().get(0));
        // Check for SYNCOPE-270
        assertNotNull(matchingUsers.getResult().get(0).getPlainAttrMap().get("obscure"));
        // Check for SYNCOPE-123
        assertNotNull(matchingUsers.getResult().get(0).getPlainAttrMap().get("photo"));

        GroupTO groupTO = matchingGroups.getResult().iterator().next();
        assertNotNull(groupTO);
        assertEquals("testLDAPGroup", groupTO.getName());
        assertEquals("true", groupTO.getPlainAttrMap().get("show").getValues().get(0));
        assertEquals(matchingUsers.getResult().iterator().next().getKey(), groupTO.getUserOwner(), 0);
        assertNull(groupTO.getGroupOwner());

        // SYNCOPE-317
        execProvisioningTask(taskService, 11L, 50, false);
    }

    @Test
    public void reconcileFromScriptedSQL() {
        // 0. reset sync token and set MappingItemTransformer
        ResourceTO resource = resourceService.read(RESOURCE_NAME_DBSCRIPTED);
        ResourceTO originalResource = SerializationUtils.clone(resource);
        ProvisionTO provision = resource.getProvision("PRINTER");
        assertNotNull(provision);

        try {
            provision.setSyncToken(null);

            MappingItemTO mappingItem = IterableUtils.find(
                    provision.getMapping().getItems(), new Predicate<MappingItemTO>() {

                @Override
                public boolean evaluate(final MappingItemTO object) {
                    return "location".equals(object.getIntAttrName());
                }
            });
            assertNotNull(mappingItem);
            mappingItem.getMappingItemTransformerClassNames().clear();
            mappingItem.getMappingItemTransformerClassNames().add(PrefixMappingItemTransformer.class.getName());

            resourceService.update(resource);

            // 1. create printer on external resource
            AnyObjectTO anyObjectTO = AnyObjectITCase.getSampleTO("sync");
            String originalLocation = anyObjectTO.getPlainAttrMap().get("location").getValues().get(0);
            assertFalse(originalLocation.startsWith(PrefixMappingItemTransformer.PREFIX));

            anyObjectTO = createAnyObject(anyObjectTO).getAny();
            assertNotNull(anyObjectTO);

            // 2. verify that PrefixMappingItemTransformer was applied during propagation
            // (location starts with given prefix on external resource)
            ConnObjectTO connObjectTO = resourceService.
                    readConnObject(RESOURCE_NAME_DBSCRIPTED, anyObjectTO.getType(), anyObjectTO.getKey());
            assertFalse(anyObjectTO.getPlainAttrMap().get("location").getValues().get(0).
                    startsWith(PrefixMappingItemTransformer.PREFIX));
            assertTrue(connObjectTO.getPlainAttrMap().get("location").getValues().get(0).
                    startsWith(PrefixMappingItemTransformer.PREFIX));

            // 3. unlink any existing printer and delete from Syncope (printer is now only on external resource)
            PagedResult<AnyObjectTO> matchingPrinters = anyObjectService.search(
                    new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                    fiql(SyncopeClient.getAnyObjectSearchConditionBuilder("PRINTER").
                            is("location").equalTo("sync*").query()).build());
            assertTrue(matchingPrinters.getSize() > 0);
            for (AnyObjectTO printer : matchingPrinters.getResult()) {
                DeassociationPatch deassociationPatch = new DeassociationPatch();
                deassociationPatch.setKey(printer.getKey());
                deassociationPatch.setAction(ResourceDeassociationAction.UNLINK);
                deassociationPatch.getResources().add(RESOURCE_NAME_DBSCRIPTED);
                anyObjectService.deassociate(deassociationPatch);
                anyObjectService.delete(printer.getKey());
            }

            // 4. synchronize
            execProvisioningTask(taskService, 28L, 50, false);

            // 5. verify that printer was re-created in Syncope (implies that location does not start with given prefix,
            // hence PrefixMappingItemTransformer was applied during sync)
            matchingPrinters = anyObjectService.search(
                    new AnySearchQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                    fiql(SyncopeClient.getAnyObjectSearchConditionBuilder("PRINTER").
                            is("location").equalTo("sync*").query()).build());
            assertTrue(matchingPrinters.getSize() > 0);

            // 6. verify that synctoken was updated
            assertNotNull(
                    resourceService.read(RESOURCE_NAME_DBSCRIPTED).getProvision(anyObjectTO.getType()).getSyncToken());
        } finally {
            resourceService.update(originalResource);
        }
    }

    @Test
    public void filteredReconciliation() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        SyncTaskTO task = null;
        UserTO userTO = null;
        try {
            // 1. create 2 users on testsync
            jdbcTemplate.execute("INSERT INTO testsync VALUES (1001, 'user1', 'Doe', 'mail1@apache.org')");
            jdbcTemplate.execute("INSERT INTO testsync VALUES (1002, 'user2', 'Rossi', 'mail2@apache.org')");

            // 2. create new sync task for test-db, with reconciliation filter (surname 'Rossi') 
            task = taskService.read(10L, true);
            task.setSyncMode(SyncMode.FILTERED_RECONCILIATION);
            task.setReconciliationFilterBuilderClassName(TestReconciliationFilterBuilder.class.getName());
            Response response = taskService.create(task);
            task = getObject(response.getLocation(), TaskService.class, SyncTaskTO.class);
            assertNotNull(task);
            assertEquals(
                    TestReconciliationFilterBuilder.class.getName(),
                    task.getReconciliationFilterBuilderClassName());

            // 3. exec task
            TaskExecTO execution = execProvisioningTask(taskService, task.getKey(), 50, false);
            assertNotNull(execution.getStatus());
            assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(execution.getStatus()));

            // 4. verify that only enabled user was synchronized
            userTO = readUser("user2");
            assertNotNull(userTO);

            try {
                readUser("user1");
                fail();
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.NotFound, e.getType());
            }
        } finally {
            jdbcTemplate.execute("DELETE FROM testsync WHERE id = 1001");
            jdbcTemplate.execute("DELETE FROM testsync WHERE id = 1002");
            if (task != null && task.getKey() != 7L) {
                taskService.delete(task.getKey());
            }
            if (userTO != null) {
                userService.delete(userTO.getKey());
            }
        }
    }

    @Test
    public void issueSYNCOPE68() {
        //-----------------------------
        // Create a new user ... it should be updated applying sync policy
        //-----------------------------
        UserTO userTO = new UserTO();
        userTO.setRealm(SyncopeConstants.ROOT_REALM);
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

        userTO.getMemberships().add(new MembershipTO.Builder().group(7L).build());

        userTO = createUser(userTO).getAny();
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

            template.getMemberships().add(new MembershipTO.Builder().group(10L).build());

            template.getResources().add(RESOURCE_NAME_NOPROPAGATION4);
            //-----------------------------

            // Update sync task
            SyncTaskTO task = taskService.read(9L, true);
            assertNotNull(task);

            task.getTemplates().put(AnyTypeKind.USER.name(), template);

            taskService.update(task);
            SyncTaskTO actual = taskService.read(task.getKey(), true);
            assertNotNull(actual);
            assertEquals(task.getKey(), actual.getKey());
            assertFalse(actual.getTemplates().get(AnyTypeKind.USER.name()).getResources().isEmpty());
            assertFalse(((UserTO) actual.getTemplates().get(AnyTypeKind.USER.name())).getMemberships().isEmpty());

            TaskExecTO execution = execProvisioningTask(taskService, actual.getKey(), 50, false);
            assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(execution.getStatus()));

            userTO = readUser("testuser2");
            assertNotNull(userTO);
            assertEquals("testuser2@syncope.apache.org", userTO.getPlainAttrMap().get("userId").getValues().get(0));
            assertEquals(2, userTO.getMemberships().size());
            assertEquals(4, userTO.getResources().size());
        } finally {
            UserTO dUserTO = deleteUser(userTO.getKey()).getAny();
            assertNotNull(dUserTO);
        }
    }

    @Test
    public void issueSYNCOPE230() {
        // 1. read SyncTask for resource-db-sync (table TESTSYNC on external H2)
        execProvisioningTask(taskService, 10L, 50, false);

        // 3. read e-mail address for user created by the SyncTask first execution
        UserTO userTO = readUser("issuesyncope230");
        assertNotNull(userTO);
        String email = userTO.getPlainAttrMap().get("email").getValues().iterator().next();
        assertNotNull(email);

        // 4. update TESTSYNC on external H2 by changing e-mail address
        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        jdbcTemplate.execute("UPDATE TESTSYNC SET email='updatedSYNCOPE230@syncope.apache.org'");

        // 5. re-execute the SyncTask
        execProvisioningTask(taskService, 10L, 50, false);

        // 6. verify that the e-mail was updated
        userTO = readUser("issuesyncope230");
        assertNotNull(userTO);
        email = userTO.getPlainAttrMap().get("email").getValues().iterator().next();
        assertNotNull(email);
        assertEquals("updatedSYNCOPE230@syncope.apache.org", email);
    }

    @Test
    public void issueSYNCOPE258() {
        // -----------------------------
        // Add a custom correlation rule
        // -----------------------------
        SyncPolicyTO policyTO = policyService.read(9L);
        policyTO.getSpecification().getCorrelationRules().put(AnyTypeKind.USER.name(), TestSyncRule.class.getName());
        policyService.update(policyTO);
        // -----------------------------

        SyncTaskTO task = new SyncTaskTO();
        task.setDestinationRealm(SyncopeConstants.ROOT_REALM);
        task.setName("Test Sync Rule");
        task.setActive(true);
        task.setResource(RESOURCE_NAME_WS2);
        task.setSyncMode(SyncMode.FULL_RECONCILIATION);
        task.setPerformCreate(true);
        task.setPerformDelete(true);
        task.setPerformUpdate(true);

        Response response = taskService.create(task);
        task = getObject(response.getLocation(), TaskService.class, SyncTaskTO.class);

        UserTO userTO = UserITCase.getUniqueSampleTO("s258_1@apache.org");
        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_WS2);

        createUser(userTO);

        userTO = UserITCase.getUniqueSampleTO("s258_2@apache.org");
        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_WS2);

        userTO = createUser(userTO).getAny();

        // change email in order to unmatch the second user
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.getPlainAttrs().add(attrAddReplacePatch("email", "s258@apache.org"));

        userService.update(userPatch);

        execProvisioningTask(taskService, task.getKey(), 50, false);

        SyncTaskTO executed = taskService.read(task.getKey(), true);
        assertEquals(1, executed.getExecutions().size());

        // asser for just one match
        assertTrue(executed.getExecutions().get(0).getMessage().substring(0, 55) + "...",
                executed.getExecutions().get(0).getMessage().contains("[updated/failures]: 1/0"));
    }

    @Test
    public void issueSYNCOPE272() {
        removeTestUsers();

        // create user with testdb resource
        UserTO userTO = UserITCase.getUniqueSampleTO("syncope272@syncope.apache.org");
        userTO.getResources().add(RESOURCE_NAME_TESTDB);

        ProvisioningResult<UserTO> result = createUser(userTO);
        userTO = result.getAny();
        try {
            assertNotNull(userTO);
            assertEquals(1, result.getPropagationStatuses().size());
            assertEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());

            TaskExecTO taskExecTO = execProvisioningTask(taskService, 24L, 50, false);

            assertNotNull(taskExecTO.getStatus());
            assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(taskExecTO.getStatus()));

            userTO = userService.read(userTO.getKey());
            assertNotNull(userTO);
            assertNotNull(userTO.getPlainAttrMap().get("firstname").getValues().get(0));
        } finally {
            removeTestUsers();
        }
    }

    @Test
    public void issueSYNCOPE307() {
        UserTO userTO = UserITCase.getUniqueSampleTO("s307@apache.org");
        userTO.setUsername("test0");
        userTO.getPlainAttrMap().get("firstname").getValues().clear();
        userTO.getPlainAttrMap().get("firstname").getValues().add("nome0");
        userTO.getAuxClasses().add("csv");

        AttrTO csvuserid = new AttrTO();
        csvuserid.setSchema("csvuserid");
        userTO.getDerAttrs().add(csvuserid);

        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_WS2);

        userTO = createUser(userTO).getAny();
        assertNotNull(userTO);

        userTO = userService.read(userTO.getKey());
        assertTrue(userTO.getVirAttrMap().isEmpty());

        // Update sync task
        SyncTaskTO task = taskService.read(12L, true);
        assertNotNull(task);

        UserTO template = new UserTO();
        template.setPassword("'password123'");
        template.getResources().add(RESOURCE_NAME_DBVIRATTR);
        template.getVirAttrs().add(attrTO("virtualdata", "'virtualvalue'"));

        task.getTemplates().put(AnyTypeKind.USER.name(), template);

        taskService.update(task);

        // exec task: one user from CSV will match the user created above and template will be applied
        execProvisioningTask(taskService, task.getKey(), 50, false);

        // check that template was successfully applied...
        userTO = userService.read(userTO.getKey());
        assertEquals("virtualvalue", userTO.getVirAttrMap().get("virtualdata").getValues().get(0));

        // ...and that propagation to db succeeded
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);

            String value = jdbcTemplate.queryForObject(
                    "SELECT USERNAME FROM testsync WHERE ID=?", String.class, userTO.getKey());
            assertEquals("virtualvalue", value);
        } catch (EmptyResultDataAccessException e) {
            fail();
        }
    }

    @Test
    public void issueSYNCOPE313DB() throws Exception {
        // 1. create user in DB
        UserTO user = UserITCase.getUniqueSampleTO("syncope313-db@syncope.apache.org");
        user.setPassword("security123");
        user.getResources().add(RESOURCE_NAME_TESTDB);
        user = createUser(user).getAny();
        assertNotNull(user);
        assertFalse(user.getResources().isEmpty());

        // 2. Check that the DB resource has the correct password
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        String value = jdbcTemplate.queryForObject(
                "SELECT PASSWORD FROM test WHERE ID=?", String.class, user.getUsername());
        assertEquals(Encryptor.getInstance().encode("security123", CipherAlgorithm.SHA1), value.toUpperCase());

        // 3. Update the password in the DB
        String newCleanPassword = "new-security";
        String newPassword = Encryptor.getInstance().encode(newCleanPassword, CipherAlgorithm.SHA1);
        jdbcTemplate.execute("UPDATE test set PASSWORD='" + newPassword + "' where ID='" + user.getUsername() + "'");

        // 4. Sync the user from the resource
        SyncTaskTO syncTask = new SyncTaskTO();
        syncTask.setDestinationRealm(SyncopeConstants.ROOT_REALM);
        syncTask.setName("DB Sync Task");
        syncTask.setActive(true);
        syncTask.setPerformCreate(true);
        syncTask.setPerformUpdate(true);
        syncTask.setSyncMode(SyncMode.FULL_RECONCILIATION);
        syncTask.setResource(RESOURCE_NAME_TESTDB);
        syncTask.getActionsClassNames().add(DBPasswordSyncActions.class.getName());
        Response taskResponse = taskService.create(syncTask);

        SyncTaskTO actual = getObject(taskResponse.getLocation(), TaskService.class, SyncTaskTO.class);
        assertNotNull(actual);

        syncTask = taskService.read(actual.getKey(), true);
        assertNotNull(syncTask);
        assertEquals(actual.getKey(), syncTask.getKey());
        assertEquals(actual.getJobDelegateClassName(), syncTask.getJobDelegateClassName());

        TaskExecTO execution = execProvisioningTask(taskService, syncTask.getKey(), 50, false);
        assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(execution.getStatus()));

        // 5. Test the sync'd user
        Pair<Map<String, Set<String>>, UserTO> self = clientFactory.create(user.getUsername(), newCleanPassword).self();
        assertNotNull(self);

        // 6. Delete SyncTask + user
        taskService.delete(syncTask.getKey());
        deleteUser(user.getKey());
    }

    @Test
    public void issueSYNCOPE313LDAP() throws Exception {
        // First of all, clear any potential conflict with existing user / group
        ldapCleanup();

        // 1. create user in LDAP
        String oldCleanPassword = "security123";
        UserTO user = UserITCase.getUniqueSampleTO("syncope313-ldap@syncope.apache.org");
        user.setPassword(oldCleanPassword);
        user.getResources().add(RESOURCE_NAME_LDAP);
        user = createUser(user).getAny();
        assertNotNull(user);
        assertFalse(user.getResources().isEmpty());

        // 2. request to change password only on Syncope and not on LDAP
        String newCleanPassword = "new-security123";
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(user.getKey());
        userPatch.setPassword(new PasswordPatch.Builder().value(newCleanPassword).build());
        user = updateUser(userPatch).getAny();

        // 3. Check that the Syncope user now has the changed password
        Pair<Map<String, Set<String>>, UserTO> self = clientFactory.create(user.getUsername(), newCleanPassword).self();
        assertNotNull(self);

        // 4. Check that the LDAP resource has the old password
        ConnObjectTO connObject =
                resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), user.getKey());
        assertNotNull(getLdapRemoteObject(
                connObject.getPlainAttrMap().get(Name.NAME).getValues().get(0),
                oldCleanPassword,
                connObject.getPlainAttrMap().get(Name.NAME).getValues().get(0)));

        // 5. Update the LDAP Connector to retrieve passwords
        ResourceTO ldapResource = resourceService.read(RESOURCE_NAME_LDAP);
        ConnInstanceTO resourceConnector = connectorService.read(
                ldapResource.getConnector(), Locale.ENGLISH.getLanguage());
        ConnConfProperty property = resourceConnector.getConfMap().get("retrievePasswordsWithSearch");
        property.getValues().clear();
        property.getValues().add(Boolean.TRUE);
        connectorService.update(resourceConnector);

        // 6. Sync the user from the resource
        SyncTaskTO syncTask = new SyncTaskTO();
        syncTask.setDestinationRealm(SyncopeConstants.ROOT_REALM);
        syncTask.setName("LDAP Sync Task");
        syncTask.setActive(true);
        syncTask.setPerformCreate(true);
        syncTask.setPerformUpdate(true);
        syncTask.setSyncMode(SyncMode.FULL_RECONCILIATION);
        syncTask.setResource(RESOURCE_NAME_LDAP);
        syncTask.getActionsClassNames().add(LDAPPasswordSyncActions.class.getName());
        Response taskResponse = taskService.create(syncTask);

        syncTask = getObject(taskResponse.getLocation(), TaskService.class, SyncTaskTO.class);
        assertNotNull(syncTask);

        TaskExecTO execution = execProvisioningTask(taskService, syncTask.getKey(), 50, false);
        assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(execution.getStatus()));

        // 7. Test the sync'd user
        self = clientFactory.create(user.getUsername(), oldCleanPassword).self();
        assertNotNull(self);

        // 8. Delete SyncTask + user + reset the connector
        taskService.delete(syncTask.getKey());
        property.getValues().clear();
        property.getValues().add(Boolean.FALSE);
        connectorService.update(resourceConnector);
        deleteUser(user.getKey());
    }
}

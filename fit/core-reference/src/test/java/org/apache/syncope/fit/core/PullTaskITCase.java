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
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import javax.ws.rs.core.Response;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.patch.DeassociationPatch;
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
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.ConnectorService;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.fit.ActivitiDetector;
import org.apache.syncope.fit.core.reference.PrefixMappingItemTransformer;
import org.apache.syncope.fit.core.reference.TestReconciliationFilterBuilder;
import org.apache.syncope.fit.core.reference.TestPullActions;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:testJDBCEnv.xml" })
public class PullTaskITCase extends AbstractTaskITCase {

    @Autowired
    private DataSource testDataSource;

    @BeforeClass
    public static void testPullActionsSetup() {
        PullTaskTO pullTask = taskService.read(PULL_TASK_KEY, true);
        pullTask.getActionsClassNames().add(TestPullActions.class.getName());
        taskService.update(pullTask);
    }

    @Test
    public void getPullActionsClasses() {
        Set<String> actions = syncopeService.platform().getPullActions();
        assertNotNull(actions);
        assertFalse(actions.isEmpty());
    }

    @Test
    public void list() {
        PagedResult<PullTaskTO> tasks = taskService.list(new TaskQuery.Builder(TaskType.PULL).build());
        assertFalse(tasks.getResult().isEmpty());
        for (AbstractTaskTO task : tasks.getResult()) {
            if (!(task instanceof PullTaskTO)) {
                fail();
            }
        }
    }

    @Test
    public void create() {
        PullTaskTO task = new PullTaskTO();
        task.setName("Test create Pull");
        task.setDestinationRealm("/");
        task.setResource(RESOURCE_NAME_WS2);
        task.setPullMode(PullMode.FULL_RECONCILIATION);

        UserTO userTemplate = new UserTO();
        userTemplate.getResources().add(RESOURCE_NAME_WS2);

        userTemplate.getMemberships().add(
                new MembershipTO.Builder().group("f779c0d4-633b-4be5-8f57-32eb478a3ca5").build());
        task.getTemplates().put(AnyTypeKind.USER.name(), userTemplate);

        GroupTO groupTemplate = new GroupTO();
        groupTemplate.getResources().add(RESOURCE_NAME_LDAP);
        task.getTemplates().put(AnyTypeKind.GROUP.name(), groupTemplate);

        Response response = taskService.create(task);
        PullTaskTO actual = getObject(response.getLocation(), TaskService.class, PullTaskTO.class);
        assertNotNull(actual);

        task = taskService.read(actual.getKey(), true);
        assertNotNull(task);
        assertEquals(actual.getKey(), task.getKey());
        assertEquals(actual.getJobDelegateClassName(), task.getJobDelegateClassName());
        assertEquals(userTemplate, task.getTemplates().get(AnyTypeKind.USER.name()));
        assertEquals(groupTemplate, task.getTemplates().get(AnyTypeKind.GROUP.name()));
    }

    @Test
    public void fromCSV() throws Exception {
        removeTestUsers();

        // Attemp to reset CSV content
        Properties props = new Properties();
        InputStream propStream = null;
        InputStream srcStream = null;
        OutputStream dstStream = null;
        try {
            propStream = getClass().getResourceAsStream("/core-test.properties");
            props.load(propStream);

            srcStream = new FileInputStream(props.getProperty("test.csv.src"));
            dstStream = new FileOutputStream(props.getProperty("test.csv.dst"));

            IOUtils.copy(srcStream, dstStream);
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            IOUtils.closeQuietly(propStream);
            IOUtils.closeQuietly(srcStream);
            IOUtils.closeQuietly(dstStream);
        }

        // -----------------------------
        // Create a new user ... it should be updated applying pull policy
        // -----------------------------
        UserTO inUserTO = new UserTO();
        inUserTO.setRealm(SyncopeConstants.ROOT_REALM);
        inUserTO.setPassword("password123");
        String userName = "test9";
        inUserTO.setUsername(userName);
        inUserTO.getPlainAttrs().add(attrTO("firstname", "nome9"));
        inUserTO.getPlainAttrs().add(attrTO("surname", "cognome"));
        inUserTO.getPlainAttrs().add(attrTO("ctype", "a type"));
        inUserTO.getPlainAttrs().add(attrTO("fullname", "nome cognome"));
        inUserTO.getPlainAttrs().add(attrTO("userId", "puccini@syncope.apache.org"));
        inUserTO.getPlainAttrs().add(attrTO("email", "puccini@syncope.apache.org"));
        inUserTO.getAuxClasses().add("csv");
        inUserTO.getDerAttrs().add(attrTO("csvuserid", null));

        inUserTO = createUser(inUserTO).getEntity();
        assertNotNull(inUserTO);
        assertFalse(inUserTO.getResources().contains(RESOURCE_NAME_CSV));

        // -----------------------------
        try {
            int usersPre = userService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                    page(1).size(1).build()).getTotalCount();
            assertNotNull(usersPre);

            ExecTO exec = execProvisioningTask(taskService, PULL_TASK_KEY, 50, false);
            assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(exec.getStatus()));

            LOG.debug("Execution of task {}:\n{}", PULL_TASK_KEY, exec);

            // check for pull results
            int usersPost = userService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                    page(1).size(1).build()).getTotalCount();
            assertNotNull(usersPost);
            assertEquals(usersPre + 8, usersPost);

            // after execution of the pull task the user data should have been pulled from CSV
            // and processed by user template
            UserTO userTO = userService.read(inUserTO.getKey());
            assertNotNull(userTO);
            assertEquals(userName, userTO.getUsername());
            assertEquals(ActivitiDetector.isActivitiEnabledForUsers(syncopeService)
                    ? "active" : "created", userTO.getStatus());
            assertEquals("test9@syncope.apache.org", userTO.getPlainAttr("email").getValues().get(0));
            assertEquals("test9@syncope.apache.org", userTO.getPlainAttr("userId").getValues().get(0));
            assertTrue(Integer.valueOf(userTO.getPlainAttr("fullname").getValues().get(0)) <= 10);
            assertTrue(userTO.getResources().contains(RESOURCE_NAME_TESTDB));
            assertTrue(userTO.getResources().contains(RESOURCE_NAME_WS2));

            // Matching --> Update (no link)
            assertFalse(userTO.getResources().contains(RESOURCE_NAME_CSV));

            // check for user template
            userTO = userService.read("test7");
            assertNotNull(userTO);
            assertEquals("TYPE_OTHER", userTO.getPlainAttr("ctype").getValues().get(0));
            assertEquals(3, userTO.getResources().size());
            assertTrue(userTO.getResources().contains(RESOURCE_NAME_TESTDB));
            assertTrue(userTO.getResources().contains(RESOURCE_NAME_WS2));
            assertEquals(1, userTO.getMemberships().size());
            assertEquals("f779c0d4-633b-4be5-8f57-32eb478a3ca5", userTO.getMemberships().get(0).getRightKey());

            // Unmatching --> Assign (link) - SYNCOPE-658
            assertTrue(userTO.getResources().contains(RESOURCE_NAME_CSV));
            assertEquals(1, IterableUtils.countMatches(userTO.getDerAttrs(), new Predicate<AttrTO>() {

                @Override
                public boolean evaluate(final AttrTO attrTO) {
                    return "csvuserid".equals(attrTO.getSchema());
                }
            }));

            userTO = userService.read("test8");
            assertNotNull(userTO);
            assertEquals("TYPE_8", userTO.getPlainAttr("ctype").getValues().get(0));

            // Check for ignored user - SYNCOPE-663
            try {
                userService.read("test2");
                fail();
            } catch (SyncopeClientException e) {
                assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
            }

            // Check for issue 215:
            // * expected disabled user test1
            // * expected enabled user test3
            userTO = userService.read("test1");
            assertNotNull(userTO);
            assertEquals("suspended", userTO.getStatus());

            userTO = userService.read("test3");
            assertNotNull(userTO);
            assertEquals("active", userTO.getStatus());

            Set<String> otherPullTaskKeys = new HashSet<>();
            otherPullTaskKeys.add("feae4e57-15ca-40d9-b973-8b9015efca49");
            otherPullTaskKeys.add("55d5e74b-497e-4bc0-9156-73abef4b9adc");
            execProvisioningTasks(taskService, otherPullTaskKeys, 50, false);

            // Matching --> UNLINK
            assertFalse(userService.read("test9").getResources().contains(RESOURCE_NAME_CSV));
            assertFalse(userService.read("test7").getResources().contains(RESOURCE_NAME_CSV));
        } finally {
            removeTestUsers();
        }
    }

    @Test
    public void dryRun() {
        ExecTO execution = execProvisioningTask(taskService, PULL_TASK_KEY, 50, true);
        assertEquals(
                "Execution of " + execution.getRefDesc() + " failed with message " + execution.getMessage(),
                "SUCCESS", execution.getStatus());
    }

    @Test
    public void reconcileFromDB() {
        UserTO userTO = null;
        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        try {
            ExecTO execution = execProvisioningTask(
                    taskService, "83f7e85d-9774-43fe-adba-ccd856312994", 50, false);
            assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(execution.getStatus()));

            userTO = userService.read("testuser1");
            assertNotNull(userTO);
            assertEquals("reconciled@syncope.apache.org", userTO.getPlainAttr("userId").getValues().get(0));
            assertEquals("suspended", userTO.getStatus());

            // enable user on external resource
            jdbcTemplate.execute("UPDATE TEST SET status=TRUE WHERE id='testuser1'");

            // re-execute the same PullTask: now user must be active
            execution = execProvisioningTask(
                    taskService, "83f7e85d-9774-43fe-adba-ccd856312994", 50, false);
            assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(execution.getStatus()));

            userTO = userService.read("testuser1");
            assertNotNull(userTO);
            assertEquals("active", userTO.getStatus());
        } finally {
            jdbcTemplate.execute("UPDATE TEST SET status=FALSE WHERE id='testUser1'");
            if (userTO != null) {
                userService.delete(userTO.getKey());
            }
        }
    }

    @Test
    public void reconcileFromLDAP() {
        // First of all, clear any potential conflict with existing user / group
        ldapCleanup();

        // 0. pull
        ExecTO execution = execProvisioningTask(taskService, "1e419ca4-ea81-4493-a14f-28b90113686d", 50, false);

        // 1. verify execution status
        assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(execution.getStatus()));

        // 2. verify that pulled group is found
        PagedResult<GroupTO> matchingGroups = groupService.search(new AnyQuery.Builder().realm(
                SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getGroupSearchConditionBuilder().is("name").equalTo("testLDAPGroup").query()).
                build());
        assertNotNull(matchingGroups);
        assertEquals(1, matchingGroups.getResult().size());
        // SYNCOPE-898
        PullTaskTO task = taskService.read("1e419ca4-ea81-4493-a14f-28b90113686d", false);
        assertEquals("/", task.getDestinationRealm());
        assertEquals("/", matchingGroups.getResult().get(0).getRealm());

        // 3. verify that pulled user is found
        PagedResult<UserTO> matchingUsers = userService.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().is("username").equalTo("pullFromLDAP").
                                query()).
                        build());
        assertNotNull(matchingUsers);
        assertEquals(1, matchingUsers.getResult().size());
        // SYNCOPE-898
        assertEquals("/odd", matchingUsers.getResult().get(0).getRealm());

        // Check for SYNCOPE-436
        assertEquals("pullFromLDAP",
                matchingUsers.getResult().get(0).getVirAttr("virtualReadOnly").getValues().get(0));
        // Check for SYNCOPE-270
        assertNotNull(matchingUsers.getResult().get(0).getPlainAttr("obscure"));
        // Check for SYNCOPE-123
        assertNotNull(matchingUsers.getResult().get(0).getPlainAttr("photo"));

        GroupTO groupTO = matchingGroups.getResult().iterator().next();
        assertNotNull(groupTO);
        assertEquals("testLDAPGroup", groupTO.getName());
        assertEquals("true", groupTO.getPlainAttr("show").getValues().get(0));
        assertEquals(matchingUsers.getResult().iterator().next().getKey(), groupTO.getUserOwner());
        assertNull(groupTO.getGroupOwner());

        // SYNCOPE-317
        execProvisioningTask(taskService, "1e419ca4-ea81-4493-a14f-28b90113686d", 50, false);

        // 4. verify that LDAP group membership is propagated as Syncope membership
        int i = 0;
        int maxit = 50;
        PagedResult<UserTO> members;
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            members = userService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                    fiql(SyncopeClient.getUserSearchConditionBuilder().inGroups(groupTO.getKey()).query()).
                    build());
            assertNotNull(members);

            i++;
        } while (members.getResult().isEmpty() && i < maxit);
        if (i == maxit) {
            fail("Timeout while checking for memberships of " + groupTO.getName());
        }
        assertEquals(1, members.getResult().size());
    }

    @Test
    public void reconcileFromScriptedSQL() {
        // 0. reset sync token and set MappingItemTransformer
        ResourceTO resource = resourceService.read(RESOURCE_NAME_DBSCRIPTED);
        ResourceTO originalResource = SerializationUtils.clone(resource);
        ProvisionTO provision = resource.getProvision("PRINTER");
        assertNotNull(provision);

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

        try {
            resourceService.update(resource);
            resourceService.removeSyncToken(resource.getKey(), provision.getAnyType());

            // insert a deleted record in the external resource (SYNCOPE-923), which will be returned
            // as sync event prior to the CREATE_OR_UPDATE events generated by the actions below (before pull)
            JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
            jdbcTemplate.update(
                    "INSERT INTO TESTPRINTER (id, printername, location, deleted, lastmodification) VALUES (?,?,?,?,?)",
                    UUID.randomUUID().toString(), "Mysterious Printer", "Nowhere", true, new Date());

            // 1. create printer on external resource
            AnyObjectTO anyObjectTO = AnyObjectITCase.getSampleTO("pull");
            String originalLocation = anyObjectTO.getPlainAttr("location").getValues().get(0);
            assertFalse(originalLocation.startsWith(PrefixMappingItemTransformer.PREFIX));

            anyObjectTO = createAnyObject(anyObjectTO).getEntity();
            assertNotNull(anyObjectTO);

            // 2. verify that PrefixMappingItemTransformer was applied during propagation
            // (location starts with given prefix on external resource)
            ConnObjectTO connObjectTO = resourceService.
                    readConnObject(RESOURCE_NAME_DBSCRIPTED, anyObjectTO.getType(), anyObjectTO.getKey());
            assertFalse(anyObjectTO.getPlainAttr("location").getValues().get(0).
                    startsWith(PrefixMappingItemTransformer.PREFIX));
            assertTrue(connObjectTO.getAttr("LOCATION").getValues().get(0).
                    startsWith(PrefixMappingItemTransformer.PREFIX));

            // 3. unlink any existing printer and delete from Syncope (printer is now only on external resource)
            PagedResult<AnyObjectTO> matchingPrinters = anyObjectService.search(
                    new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                            fiql(SyncopeClient.getAnyObjectSearchConditionBuilder("PRINTER").
                                    is("location").equalTo("pull*").query()).build());
            assertTrue(matchingPrinters.getSize() > 0);
            for (AnyObjectTO printer : matchingPrinters.getResult()) {
                DeassociationPatch deassociationPatch = new DeassociationPatch();
                deassociationPatch.setKey(printer.getKey());
                deassociationPatch.setAction(ResourceDeassociationAction.UNLINK);
                deassociationPatch.getResources().add(RESOURCE_NAME_DBSCRIPTED);
                anyObjectService.deassociate(deassociationPatch);
                anyObjectService.delete(printer.getKey());
            }

            // ensure that the pull task does not have the DELETE capability (SYNCOPE-923)
            PullTaskTO pullTask = taskService.read("30cfd653-257b-495f-8665-281281dbcb3d", false);
            assertNotNull(pullTask);
            assertFalse(pullTask.isPerformDelete());

            // 4. pull
            execProvisioningTask(taskService, "30cfd653-257b-495f-8665-281281dbcb3d", 50, false);

            // 5. verify that printer was re-created in Syncope (implies that location does not start with given prefix,
            // hence PrefixMappingItemTransformer was applied during pull)
            matchingPrinters = anyObjectService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                    fiql(SyncopeClient.getAnyObjectSearchConditionBuilder("PRINTER").
                            is("location").equalTo("pull*").query()).build());
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
        String user1OnTestPull = UUID.randomUUID().toString();
        String user2OnTestPull = UUID.randomUUID().toString();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        PullTaskTO task = null;
        UserTO userTO = null;
        try {
            // 1. create 2 users on testpull
            jdbcTemplate.execute("INSERT INTO testpull VALUES ("
                    + "'" + user1OnTestPull + "', 'user1', 'Doe', 'mail1@apache.org', NULL)");
            jdbcTemplate.execute("INSERT INTO testpull VALUES ("
                    + "'" + user2OnTestPull + "', 'user2', 'Rossi', 'mail2@apache.org', NULL)");

            // 2. create new pull task for test-db, with reconciliation filter (surname 'Rossi') 
            task = taskService.read("7c2242f4-14af-4ab5-af31-cdae23783655", true);
            task.setPullMode(PullMode.FILTERED_RECONCILIATION);
            task.setReconciliationFilterBuilderClassName(TestReconciliationFilterBuilder.class.getName());
            Response response = taskService.create(task);
            task = getObject(response.getLocation(), TaskService.class, PullTaskTO.class);
            assertNotNull(task);
            assertEquals(
                    TestReconciliationFilterBuilder.class.getName(),
                    task.getReconciliationFilterBuilderClassName());

            // 3. exec task
            ExecTO execution = execProvisioningTask(taskService, task.getKey(), 50, false);
            assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(execution.getStatus()));

            // 4. verify that only enabled user was pulled
            userTO = userService.read("user2");
            assertNotNull(userTO);

            try {
                userService.read("user1");
                fail();
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.NotFound, e.getType());
            }
        } finally {
            jdbcTemplate.execute("DELETE FROM testpull WHERE id = '" + user1OnTestPull + "'");
            jdbcTemplate.execute("DELETE FROM testpull WHERE id = '" + user2OnTestPull + "'");
            if (task != null && !"83f7e85d-9774-43fe-adba-ccd856312994".equals(task.getKey())) {
                taskService.delete(task.getKey());
            }
            if (userTO != null) {
                userService.delete(userTO.getKey());
            }
        }
    }

    @Test
    public void syncTokenWithErrors() {
        ResourceTO origResource = resourceService.read(RESOURCE_NAME_DBPULL);
        ConnInstanceTO origConnector = connectorService.read(origResource.getConnector(), null);

        ResourceTO resForTest = SerializationUtils.clone(origResource);
        resForTest.setKey("syncTokenWithErrors");
        resForTest.setConnector(null);
        ConnInstanceTO connForTest = SerializationUtils.clone(origConnector);
        connForTest.setKey(null);
        connForTest.setDisplayName("For syncTokenWithErrors");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        try {
            connForTest.getCapabilities().add(ConnectorCapability.SYNC);

            ConnConfProperty changeLogColumn = connForTest.getConfMap().get("changeLogColumn");
            assertNotNull(changeLogColumn);
            assertTrue(changeLogColumn.getValues().isEmpty());
            changeLogColumn.getValues().add("lastModification");

            Response response = connectorService.create(connForTest);
            if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
                throw (RuntimeException) clientFactory.getExceptionMapper().fromResponse(response);
            }
            connForTest = getObject(response.getLocation(), ConnectorService.class, ConnInstanceTO.class);
            assertNotNull(connForTest);

            resForTest.setConnector(connForTest.getKey());
            resForTest = createResource(resForTest);
            assertNotNull(resForTest);

            PullTaskTO pullTask = new PullTaskTO();
            pullTask.setActive(true);
            pullTask.setName("For syncTokenWithErrors");
            pullTask.setResource(resForTest.getKey());
            pullTask.setDestinationRealm(SyncopeConstants.ROOT_REALM);
            pullTask.setPullMode(PullMode.INCREMENTAL);
            pullTask.setPerformCreate(true);
            pullTask.setPerformUpdate(true);
            pullTask.setPerformDelete(true);

            response = taskService.create(pullTask);
            if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
                throw (RuntimeException) clientFactory.getExceptionMapper().fromResponse(response);
            }
            pullTask = getObject(response.getLocation(), TaskService.class, PullTaskTO.class);
            assertNotNull(pullTask);

            jdbcTemplate.execute("DELETE FROM testpull");
            jdbcTemplate.execute("INSERT INTO testpull VALUES "
                    + "(1040, 'syncTokenWithErrors1', 'Surname1', "
                    + "'syncTokenWithErrors1@syncope.apache.org', '2014-05-23 13:53:24.293')");
            jdbcTemplate.execute("INSERT INTO testpull VALUES "
                    + "(1041, 'syncTokenWithErrors2', 'Surname2', "
                    + "'syncTokenWithErrors1@syncope.apache.org', '2015-05-23 13:53:24.293')");

            ExecTO exec = execProvisioningTask(taskService, pullTask.getKey(), 50, false);
            assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(exec.getStatus()));

            resForTest = resourceService.read(resForTest.getKey());
            assertTrue(resForTest.getProvision(AnyTypeKind.USER.name()).getSyncToken().contains("2014-05-23"));

            jdbcTemplate.execute("UPDATE testpull "
                    + "SET email='syncTokenWithErrors2@syncope.apache.org', lastModification='2016-05-23 13:53:24.293' "
                    + "WHERE ID=1041");

            exec = execProvisioningTask(taskService, pullTask.getKey(), 50, false);
            assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(exec.getStatus()));

            resForTest = resourceService.read(resForTest.getKey());
            assertTrue(resForTest.getProvision(AnyTypeKind.USER.name()).getSyncToken().contains("2016-05-23"));
        } finally {
            if (resForTest.getConnector() != null) {
                resourceService.delete(resForTest.getKey());
                connectorService.delete(connForTest.getKey());
            }

            jdbcTemplate.execute("DELETE FROM testpull WHERE ID=1040");
            jdbcTemplate.execute("DELETE FROM testpull WHERE ID=1041");
        }
    }
}

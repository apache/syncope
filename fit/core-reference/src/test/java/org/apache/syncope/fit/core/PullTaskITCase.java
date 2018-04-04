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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.patch.DeassociationPatch;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.policy.PullPolicyTO;
import org.apache.syncope.common.lib.to.TaskTO;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RemediationTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.ImplementationType;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.ConnectorService;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.core.provisioning.java.pushpull.DBPasswordPullActions;
import org.apache.syncope.core.provisioning.java.pushpull.LDAPPasswordPullActions;
import org.apache.syncope.core.spring.security.Encryptor;
import org.apache.syncope.fit.FlowableDetector;
import org.apache.syncope.fit.core.reference.TestPullActions;
import org.identityconnectors.framework.common.objects.Name;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(locations = { "classpath:testJDBCEnv.xml" })
public class PullTaskITCase extends AbstractTaskITCase {

    @Autowired
    private DataSource testDataSource;

    @BeforeAll
    public static void testPullActionsSetup() {
        ImplementationTO pullActions = null;
        try {
            pullActions = implementationService.read(
                    ImplementationType.PULL_ACTIONS, TestPullActions.class.getSimpleName());
        } catch (SyncopeClientException e) {
            if (e.getType().getResponseStatus() == Response.Status.NOT_FOUND) {
                pullActions = new ImplementationTO();
                pullActions.setKey(TestPullActions.class.getSimpleName());
                pullActions.setEngine(ImplementationEngine.JAVA);
                pullActions.setType(ImplementationType.PULL_ACTIONS);
                pullActions.setBody(TestPullActions.class.getName());
                Response response = implementationService.create(pullActions);
                pullActions = implementationService.read(
                        pullActions.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                assertNotNull(pullActions);
            }
        }
        assertNotNull(pullActions);

        PullTaskTO pullTask = taskService.read(TaskType.PULL, PULL_TASK_KEY, true);
        pullTask.getActions().add(pullActions.getKey());
        taskService.update(TaskType.PULL, pullTask);
    }

    @Test
    public void getPullActionsClasses() {
        Set<String> actions = syncopeService.platform().
                getJavaImplInfo(ImplementationType.PULL_ACTIONS).get().getClasses();
        assertNotNull(actions);
        assertFalse(actions.isEmpty());
    }

    @Test
    public void list() {
        PagedResult<PullTaskTO> tasks = taskService.search(new TaskQuery.Builder(TaskType.PULL).build());
        assertFalse(tasks.getResult().isEmpty());
        tasks.getResult().stream().
                filter(task -> (!(task instanceof PullTaskTO))).
                forEach(item -> fail("This should not happen"));
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

        Response response = taskService.create(TaskType.PULL, task);
        PullTaskTO actual = getObject(response.getLocation(), TaskService.class, PullTaskTO.class);
        assertNotNull(actual);

        task = taskService.read(TaskType.PULL, actual.getKey(), true);
        assertNotNull(task);
        assertEquals(actual.getKey(), task.getKey());
        assertEquals(actual.getJobDelegate(), task.getJobDelegate());
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
            if (propStream != null) {
                propStream.close();
            }
            if (srcStream != null) {
                srcStream.close();
            }
            if (dstStream != null) {
                dstStream.close();
            }
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

        inUserTO = createUser(inUserTO).getEntity();
        assertNotNull(inUserTO);
        assertFalse(inUserTO.getResources().contains(RESOURCE_NAME_CSV));

        // -----------------------------
        try {
            int usersPre = userService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                    page(1).size(1).build()).getTotalCount();
            assertNotNull(usersPre);

            ExecTO exec = execProvisioningTask(taskService, TaskType.PULL, PULL_TASK_KEY, 50, false);
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
            assertEquals(FlowableDetector.isFlowableEnabledForUsers(syncopeService)
                    ? "active" : "created", userTO.getStatus());
            assertEquals("test9@syncope.apache.org", userTO.getPlainAttr("email").get().getValues().get(0));
            assertEquals("test9@syncope.apache.org", userTO.getPlainAttr("userId").get().getValues().get(0));
            assertTrue(Integer.valueOf(userTO.getPlainAttr("fullname").get().getValues().get(0)) <= 10);
            assertTrue(userTO.getResources().contains(RESOURCE_NAME_TESTDB));
            assertTrue(userTO.getResources().contains(RESOURCE_NAME_WS2));

            // Matching --> Update (no link)
            assertFalse(userTO.getResources().contains(RESOURCE_NAME_CSV));

            // check for user template
            userTO = userService.read("test7");
            assertNotNull(userTO);
            assertEquals("TYPE_OTHER", userTO.getPlainAttr("ctype").get().getValues().get(0));
            assertEquals(3, userTO.getResources().size());
            assertTrue(userTO.getResources().contains(RESOURCE_NAME_TESTDB));
            assertTrue(userTO.getResources().contains(RESOURCE_NAME_WS2));
            assertEquals(1, userTO.getMemberships().size());
            assertEquals("f779c0d4-633b-4be5-8f57-32eb478a3ca5", userTO.getMemberships().get(0).getGroupKey());

            // Unmatching --> Assign (link) - SYNCOPE-658
            assertTrue(userTO.getResources().contains(RESOURCE_NAME_CSV));
            assertEquals(1, userTO.getDerAttrs().stream().
                    filter(attrTO -> "csvuserid".equals(attrTO.getSchema())).count());

            userTO = userService.read("test8");
            assertNotNull(userTO);
            assertEquals("TYPE_8", userTO.getPlainAttr("ctype").get().getValues().get(0));

            // Check for ignored user - SYNCOPE-663
            try {
                userService.read("test2");
                fail("This should not happen");
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
            execProvisioningTasks(taskService, TaskType.PULL, otherPullTaskKeys, 50, false);

            // Matching --> UNLINK
            assertFalse(userService.read("test9").getResources().contains(RESOURCE_NAME_CSV));
            assertFalse(userService.read("test7").getResources().contains(RESOURCE_NAME_CSV));
        } finally {
            removeTestUsers();
        }
    }

    @Test
    public void dryRun() {
        ExecTO execution = execProvisioningTask(taskService, TaskType.PULL, PULL_TASK_KEY, 50, true);
        assertEquals("SUCCESS", execution.getStatus());
    }

    @Test
    public void reconcileFromDB() {
        UserTO userTO = null;
        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        try {
            ExecTO execution = execProvisioningTask(
                    taskService, TaskType.PULL, "83f7e85d-9774-43fe-adba-ccd856312994", 50, false);
            assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(execution.getStatus()));

            userTO = userService.read("testuser1");
            assertNotNull(userTO);
            assertEquals("reconciled@syncope.apache.org", userTO.getPlainAttr("userId").get().getValues().get(0));
            assertEquals("suspended", userTO.getStatus());

            // enable user on external resource
            jdbcTemplate.execute("UPDATE TEST SET status=TRUE WHERE id='testuser1'");

            // re-execute the same PullTask: now user must be active
            execution = execProvisioningTask(
                    taskService, TaskType.PULL, "83f7e85d-9774-43fe-adba-ccd856312994", 50, false);
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
        ExecTO execution = execProvisioningTask(
                taskService, TaskType.PULL, "1e419ca4-ea81-4493-a14f-28b90113686d", 50, false);

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
        PullTaskTO task = taskService.read(TaskType.PULL, "1e419ca4-ea81-4493-a14f-28b90113686d", false);
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
                matchingUsers.getResult().get(0).getVirAttr("virtualReadOnly").get().getValues().get(0));
        // Check for SYNCOPE-270
        assertNotNull(matchingUsers.getResult().get(0).getPlainAttr("obscure"));
        // Check for SYNCOPE-123
        assertNotNull(matchingUsers.getResult().get(0).getPlainAttr("photo"));

        GroupTO groupTO = matchingGroups.getResult().iterator().next();
        assertNotNull(groupTO);
        assertEquals("testLDAPGroup", groupTO.getName());
        assertEquals("true", groupTO.getPlainAttr("show").get().getValues().get(0));
        assertEquals(matchingUsers.getResult().iterator().next().getKey(), groupTO.getUserOwner());
        assertNull(groupTO.getGroupOwner());

        // SYNCOPE-317
        execProvisioningTask(taskService, TaskType.PULL, "1e419ca4-ea81-4493-a14f-28b90113686d", 50, false);

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
    public void reconcileFromScriptedSQL() throws IOException {
        // 0. reset sync token and set MappingItemTransformer
        ResourceTO resource = resourceService.read(RESOURCE_NAME_DBSCRIPTED);
        ResourceTO originalResource = SerializationUtils.clone(resource);
        ProvisionTO provision = resource.getProvision("PRINTER").get();
        assertNotNull(provision);

        ItemTO mappingItem = provision.getMapping().getItems().stream().
                filter(object -> "location".equals(object.getIntAttrName())).findFirst().get();
        assertNotNull(mappingItem);

        final String prefix = "PREFIX_";

        ImplementationTO transformer = new ImplementationTO();
        transformer.setKey("PrefixItemTransformer");
        transformer.setEngine(ImplementationEngine.GROOVY);
        transformer.setType(ImplementationType.ITEM_TRANSFORMER);
        transformer.setBody(IOUtils.toString(
                getClass().getResourceAsStream("/PrefixItemTransformer.groovy"), StandardCharsets.UTF_8));
        Response response = implementationService.create(transformer);
        transformer = implementationService.read(
                transformer.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
        assertNotNull(transformer);

        mappingItem.getTransformers().clear();
        mappingItem.getTransformers().add(transformer.getKey());

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
            String originalLocation = anyObjectTO.getPlainAttr("location").get().getValues().get(0);
            assertFalse(originalLocation.startsWith(prefix));

            anyObjectTO = createAnyObject(anyObjectTO).getEntity();
            assertNotNull(anyObjectTO);

            // 2. verify that PrefixMappingItemTransformer was applied during propagation
            // (location starts with given prefix on external resource)
            ConnObjectTO connObjectTO = resourceService.
                    readConnObject(RESOURCE_NAME_DBSCRIPTED, anyObjectTO.getType(), anyObjectTO.getKey());
            assertFalse(anyObjectTO.getPlainAttr("location").get().getValues().get(0).startsWith(prefix));
            assertTrue(connObjectTO.getAttr("LOCATION").get().getValues().get(0).startsWith(prefix));

            // 3. unlink any existing printer and delete from Syncope (printer is now only on external resource)
            PagedResult<AnyObjectTO> matchingPrinters = anyObjectService.search(
                    new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                            fiql(SyncopeClient.getAnyObjectSearchConditionBuilder("PRINTER").
                                    is("location").equalTo("pull*").query()).build());
            assertTrue(matchingPrinters.getSize() > 0);
            for (AnyObjectTO printer : matchingPrinters.getResult()) {
                anyObjectService.deassociate(new DeassociationPatch.Builder().key(printer.getKey()).
                        action(ResourceDeassociationAction.UNLINK).resource(RESOURCE_NAME_DBSCRIPTED).build());
                anyObjectService.delete(printer.getKey());
            }

            // ensure that the pull task does not have the DELETE capability (SYNCOPE-923)
            PullTaskTO pullTask = taskService.read(TaskType.PULL, "30cfd653-257b-495f-8665-281281dbcb3d", false);
            assertNotNull(pullTask);
            assertFalse(pullTask.isPerformDelete());

            // 4. pull
            execProvisioningTask(taskService, TaskType.PULL, pullTask.getKey(), 50, false);

            // 5. verify that printer was re-created in Syncope (implies that location does not start with given prefix,
            // hence PrefixItemTransformer was applied during pull)
            matchingPrinters = anyObjectService.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                    fiql(SyncopeClient.getAnyObjectSearchConditionBuilder("PRINTER").
                            is("location").equalTo("pull*").query()).build());
            assertTrue(matchingPrinters.getSize() > 0);

            // 6. verify that synctoken was updated
            assertNotNull(resourceService.read(RESOURCE_NAME_DBSCRIPTED).
                    getProvision(anyObjectTO.getType()).get().getSyncToken());
        } finally {
            resourceService.update(originalResource);
        }
    }

    @Test
    public void filteredReconciliation() throws IOException {
        String user1OnTestPull = UUID.randomUUID().toString();
        String user2OnTestPull = UUID.randomUUID().toString();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        PullTaskTO task = null;
        UserTO userTO = null;
        try {
            // 1. create 2 users on testpull
            jdbcTemplate.execute("INSERT INTO testpull VALUES ("
                    + "'" + user1OnTestPull + "', 'user1', 'Doe', false, 'mail1@apache.org', NULL)");
            jdbcTemplate.execute("INSERT INTO testpull VALUES ("
                    + "'" + user2OnTestPull + "', 'user2', 'Rossi', false, 'mail2@apache.org', NULL)");

            // 2. create new pull task for test-db, with reconciliation filter (surname 'Rossi') 
            ImplementationTO reconFilterBuilder = new ImplementationTO();
            reconFilterBuilder.setKey("TestReconFilterBuilder");
            reconFilterBuilder.setEngine(ImplementationEngine.GROOVY);
            reconFilterBuilder.setType(ImplementationType.RECON_FILTER_BUILDER);
            reconFilterBuilder.setBody(IOUtils.toString(
                    getClass().getResourceAsStream("/TestReconFilterBuilder.groovy"), StandardCharsets.UTF_8));
            Response response = implementationService.create(reconFilterBuilder);
            reconFilterBuilder = implementationService.read(
                    reconFilterBuilder.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
            assertNotNull(reconFilterBuilder);

            task = taskService.read(TaskType.PULL, "7c2242f4-14af-4ab5-af31-cdae23783655", true);
            task.setPullMode(PullMode.FILTERED_RECONCILIATION);
            task.setReconFilterBuilder(reconFilterBuilder.getKey());
            response = taskService.create(TaskType.PULL, task);
            task = getObject(response.getLocation(), TaskService.class, PullTaskTO.class);
            assertNotNull(task);
            assertEquals(reconFilterBuilder.getKey(), task.getReconFilterBuilder());

            // 3. exec task
            ExecTO execution = execProvisioningTask(taskService, TaskType.PULL, task.getKey(), 50, false);
            assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(execution.getStatus()));

            // 4. verify that only enabled user was pulled
            userTO = userService.read("user2");
            assertNotNull(userTO);

            try {
                userService.read("user1");
                fail("This should not happen");
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.NotFound, e.getType());
            }
        } finally {
            jdbcTemplate.execute("DELETE FROM testpull WHERE id = '" + user1OnTestPull + "'");
            jdbcTemplate.execute("DELETE FROM testpull WHERE id = '" + user2OnTestPull + "'");
            if (task != null && !"7c2242f4-14af-4ab5-af31-cdae23783655".equals(task.getKey())) {
                taskService.delete(TaskType.PULL, task.getKey());
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

            ConnConfProperty changeLogColumn = connForTest.getConf("changeLogColumn").get();
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

            response = taskService.create(TaskType.PULL, pullTask);
            if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
                throw (RuntimeException) clientFactory.getExceptionMapper().fromResponse(response);
            }
            pullTask = getObject(response.getLocation(), TaskService.class, PullTaskTO.class);
            assertNotNull(pullTask);

            jdbcTemplate.execute("DELETE FROM testpull");
            jdbcTemplate.execute("INSERT INTO testpull VALUES "
                    + "(1040, 'syncTokenWithErrors1', 'Surname1', "
                    + "false, 'syncTokenWithErrors1@syncope.apache.org', '2014-05-23 13:53:24.293')");
            jdbcTemplate.execute("INSERT INTO testpull VALUES "
                    + "(1041, 'syncTokenWithErrors2', 'Surname2', "
                    + "false, 'syncTokenWithErrors1@syncope.apache.org', '2015-05-23 13:53:24.293')");

            ExecTO exec = execProvisioningTask(taskService, TaskType.PULL, pullTask.getKey(), 50, false);
            assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(exec.getStatus()));

            resForTest = resourceService.read(resForTest.getKey());
            assertTrue(resForTest.getProvision(AnyTypeKind.USER.name()).get().getSyncToken().contains("2014-05-23"));

            jdbcTemplate.execute("UPDATE testpull "
                    + "SET email='syncTokenWithErrors2@syncope.apache.org', lastModification='2016-05-23 13:53:24.293' "
                    + "WHERE ID=1041");

            exec = execProvisioningTask(taskService, TaskType.PULL, pullTask.getKey(), 50, false);
            assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(exec.getStatus()));

            resForTest = resourceService.read(resForTest.getKey());
            assertTrue(resForTest.getProvision(AnyTypeKind.USER.name()).get().getSyncToken().contains("2016-05-23"));
        } finally {
            if (resForTest.getConnector() != null) {
                resourceService.delete(resForTest.getKey());
                connectorService.delete(connForTest.getKey());
            }

            jdbcTemplate.execute("DELETE FROM testpull WHERE ID=1040");
            jdbcTemplate.execute("DELETE FROM testpull WHERE ID=1041");
        }
    }

    @Test
    public void remediation() {
        // First of all, clear any potential conflict with existing user / group
        ldapCleanup();

        // 1. create ldap cloned resource, where 'userId' (mandatory on Syncope) is removed from mapping
        ResourceTO ldap = resourceService.read(RESOURCE_NAME_LDAP);
        ldap.setKey("ldapForRemediation");

        ProvisionTO provision = ldap.getProvision(AnyTypeKind.USER.name()).get();
        provision.getVirSchemas().clear();
        provision.getMapping().getItems().removeIf(item -> "userId".equals(item.getIntAttrName()));

        ldap = createResource(ldap);

        // 2. create PullTask with remediation enabled, for the new resource
        PullTaskTO pullTask = (PullTaskTO) taskService.search(new TaskQuery.Builder(TaskType.PULL).
                resource(RESOURCE_NAME_LDAP).build()).getResult().get(0);
        assertNotNull(pullTask);
        pullTask.setResource(ldap.getKey());
        pullTask.setRemediation(true);
        pullTask.getActions().clear();

        Response response = taskService.create(TaskType.PULL, pullTask);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            throw (RuntimeException) clientFactory.getExceptionMapper().fromResponse(response);
        }
        pullTask = getObject(response.getLocation(), TaskService.class, PullTaskTO.class);
        assertNotNull(pullTask);

        try {
            // 3. execute the pull task and verify that:
            ExecTO execution = execProvisioningTask(taskService, TaskType.PULL, pullTask.getKey(), 50, false);
            assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(execution.getStatus()));

            // 3a. user was not pulled
            try {
                userService.read("pullFromLDAP");
                fail("This should never happen");
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.NotFound, e.getType());
            }

            // 3b. remediation was created
            Optional<RemediationTO> remediation = remediationService.list().stream().
                    filter(r -> "uid=pullFromLDAP,ou=People,o=isp".equalsIgnoreCase(r.getRemoteName())).
                    findFirst();
            assertTrue(remediation.isPresent());
            assertEquals(AnyTypeKind.USER.name(), remediation.get().getAnyType());
            assertEquals(ResourceOperation.CREATE, remediation.get().getOperation());
            assertNotNull(remediation.get().getAnyTOPayload());
            assertNull(remediation.get().getAnyPatchPayload());
            assertNull(remediation.get().getKeyPayload());
            assertTrue(remediation.get().getError().contains("RequiredValuesMissing [userId]"));

            // 4. remedy by copying the email value to userId
            UserTO user = (UserTO) remediation.get().getAnyTOPayload();
            user.getResources().clear();

            String email = user.getPlainAttr("email").get().getValues().get(0);
            user.getPlainAttrs().add(new AttrTO.Builder().schema("userId").value(email).build());

            remediationService.remedy(remediation.get().getKey(), user);

            // 5. user is now found
            user = userService.read("pullFromLDAP");
            assertNotNull(user);
            assertEquals(email, user.getPlainAttr("userId").get().getValues().get(0));

            // 6. remediation was removed
            try {
                remediationService.read(remediation.get().getKey());
                fail("This should never happen");
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.NotFound, e.getType());
            }
        } finally {
            resourceService.delete(ldap.getKey());
        }
    }

    @Test
    public void issueSYNCOPE68() {
        //-----------------------------
        // Create a new user ... it should be updated applying pull policy
        //-----------------------------
        UserTO userTO = new UserTO();
        userTO.setRealm(SyncopeConstants.ROOT_REALM);
        userTO.setPassword("password123");
        userTO.setUsername("testuser2");

        userTO.getPlainAttrs().add(attrTO("firstname", "testuser2"));
        userTO.getPlainAttrs().add(attrTO("surname", "testuser2"));
        userTO.getPlainAttrs().add(attrTO("ctype", "a type"));
        userTO.getPlainAttrs().add(attrTO("fullname", "a type"));
        userTO.getPlainAttrs().add(attrTO("userId", "testuser2@syncope.apache.org"));
        userTO.getPlainAttrs().add(attrTO("email", "testuser2@syncope.apache.org"));

        userTO.getResources().add(RESOURCE_NAME_NOPROPAGATION2);
        userTO.getResources().add(RESOURCE_NAME_NOPROPAGATION4);

        userTO.getMemberships().add(
                new MembershipTO.Builder().group("bf825fe1-7320-4a54-bd64-143b5c18ab97").build());

        userTO = createUser(userTO).getEntity();
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

            template.getMemberships().add(
                    new MembershipTO.Builder().group("b8d38784-57e7-4595-859a-076222644b55").build());

            template.getResources().add(RESOURCE_NAME_NOPROPAGATION4);
            //-----------------------------

            // Update pull task
            PullTaskTO task = taskService.read(TaskType.PULL, "81d88f73-d474-4450-9031-605daa4e313f", true);
            assertNotNull(task);

            task.getTemplates().put(AnyTypeKind.USER.name(), template);

            taskService.update(TaskType.PULL, task);
            PullTaskTO actual = taskService.read(TaskType.PULL, task.getKey(), true);
            assertNotNull(actual);
            assertEquals(task.getKey(), actual.getKey());
            assertFalse(actual.getTemplates().get(AnyTypeKind.USER.name()).getResources().isEmpty());
            assertFalse(((UserTO) actual.getTemplates().get(AnyTypeKind.USER.name())).getMemberships().isEmpty());

            ExecTO execution = execProvisioningTask(taskService, TaskType.PULL, actual.getKey(), 50, false);
            assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(execution.getStatus()));

            userTO = userService.read("testuser2");
            assertNotNull(userTO);
            assertEquals("testuser2@syncope.apache.org", userTO.getPlainAttr("userId").get().getValues().get(0));
            assertEquals(2, userTO.getMemberships().size());
            assertEquals(4, userTO.getResources().size());
        } finally {
            UserTO dUserTO = deleteUser(userTO.getKey()).getEntity();
            assertNotNull(dUserTO);
        }
    }

    @Test
    public void issueSYNCOPE230() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);

        String id = "a54b3794-b231-47be-b24a-11e1a42949f6";

        // 1. populate the external table
        jdbcTemplate.execute("INSERT INTO testpull VALUES"
                + "('" + id + "', 'issuesyncope230', 'Surname', false, 'syncope230@syncope.apache.org', NULL)");

        // 2. execute PullTask for resource-db-pull (table TESTPULL on external H2)
        execProvisioningTask(taskService, TaskType.PULL, "7c2242f4-14af-4ab5-af31-cdae23783655", 50, false);

        // 3. read e-mail address for user created by the PullTask first execution
        UserTO userTO = userService.read("issuesyncope230");
        assertNotNull(userTO);
        String email = userTO.getPlainAttr("email").get().getValues().iterator().next();
        assertNotNull(email);

        // 4. update TESTPULL on external H2 by changing e-mail address
        jdbcTemplate.execute("UPDATE TESTPULL SET email='updatedSYNCOPE230@syncope.apache.org' WHERE id='" + id + "'");

        // 5. re-execute the PullTask
        execProvisioningTask(taskService, TaskType.PULL, "7c2242f4-14af-4ab5-af31-cdae23783655", 50, false);

        // 6. verify that the e-mail was updated
        userTO = userService.read("issuesyncope230");
        assertNotNull(userTO);
        email = userTO.getPlainAttr("email").get().getValues().iterator().next();
        assertNotNull(email);
        assertEquals("updatedSYNCOPE230@syncope.apache.org", email);
    }

    @Test
    public void issueSYNCOPE258() throws IOException {
        // -----------------------------
        // Add a custom correlation rule
        // -----------------------------
        ImplementationTO corrRule = null;
        try {
            corrRule = implementationService.read(ImplementationType.PULL_CORRELATION_RULE, "TestPullRule");
        } catch (SyncopeClientException e) {
            if (e.getType().getResponseStatus() == Response.Status.NOT_FOUND) {
                corrRule = new ImplementationTO();
                corrRule.setKey("TestPullRule");
                corrRule.setEngine(ImplementationEngine.GROOVY);
                corrRule.setType(ImplementationType.PULL_CORRELATION_RULE);
                corrRule.setBody(IOUtils.toString(
                        getClass().getResourceAsStream("/TestPullRule.groovy"), StandardCharsets.UTF_8));
                Response response = implementationService.create(corrRule);
                corrRule = implementationService.read(
                        corrRule.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                assertNotNull(corrRule);
            }
        }
        assertNotNull(corrRule);

        PullPolicyTO policyTO = policyService.read(PolicyType.PULL, "9454b0d7-2610-400a-be82-fc23cf553dd6");
        policyTO.getCorrelationRules().put(AnyTypeKind.USER.name(), corrRule.getKey());
        policyService.update(PolicyType.PULL, policyTO);
        // -----------------------------

        PullTaskTO task = new PullTaskTO();
        task.setDestinationRealm(SyncopeConstants.ROOT_REALM);
        task.setName("Test Pull Rule");
        task.setActive(true);
        task.setResource(RESOURCE_NAME_WS2);
        task.setPullMode(PullMode.FULL_RECONCILIATION);
        task.setPerformCreate(true);
        task.setPerformDelete(true);
        task.setPerformUpdate(true);

        Response response = taskService.create(TaskType.PULL, task);
        task = getObject(response.getLocation(), TaskService.class, PullTaskTO.class);

        UserTO userTO = UserITCase.getUniqueSampleTO("s258_1@apache.org");
        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_WS2);

        createUser(userTO);

        userTO = UserITCase.getUniqueSampleTO("s258_2@apache.org");
        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_WS2);

        userTO = createUser(userTO).getEntity();

        // change email in order to unmatch the second user
        UserPatch userPatch = new UserPatch();
        userPatch.setKey(userTO.getKey());
        userPatch.getPlainAttrs().add(attrAddReplacePatch("email", "s258@apache.org"));

        userService.update(userPatch);

        execProvisioningTask(taskService, TaskType.PULL, task.getKey(), 50, false);

        PullTaskTO executed = taskService.read(TaskType.PULL, task.getKey(), true);
        assertEquals(1, executed.getExecutions().size());

        // asser for just one match
        assertTrue(executed.getExecutions().get(0).getMessage().contains("[updated/failures]: 1/0"),
                executed.getExecutions().get(0).getMessage().substring(0, 55) + "...");
    }

    @Test
    public void issueSYNCOPE272() {
        removeTestUsers();

        // create user with testdb resource
        UserTO userTO = UserITCase.getUniqueSampleTO("syncope272@syncope.apache.org");
        userTO.getResources().add(RESOURCE_NAME_TESTDB);

        ProvisioningResult<UserTO> result = createUser(userTO);
        userTO = result.getEntity();
        try {
            assertNotNull(userTO);
            assertEquals(1, result.getPropagationStatuses().size());
            assertEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());

            ExecTO taskExecTO = execProvisioningTask(
                    taskService, TaskType.PULL, "986867e2-993b-430e-8feb-aa9abb4c1dcd", 50, false);

            assertNotNull(taskExecTO.getStatus());
            assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(taskExecTO.getStatus()));

            userTO = userService.read(userTO.getKey());
            assertNotNull(userTO);
            assertNotNull(userTO.getPlainAttr("firstname").get().getValues().get(0));
        } finally {
            removeTestUsers();
        }
    }

    @Test
    public void issueSYNCOPE307() {
        UserTO userTO = UserITCase.getUniqueSampleTO("s307@apache.org");
        userTO.setUsername("test0");
        userTO.getPlainAttr("firstname").get().getValues().clear();
        userTO.getPlainAttr("firstname").get().getValues().add("nome0");
        userTO.getAuxClasses().add("csv");

        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_WS2);

        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);

        userTO = userService.read(userTO.getKey());
        assertTrue(userTO.getVirAttrs().isEmpty());

        // Update pull task
        PullTaskTO task = taskService.read(TaskType.PULL, "38abbf9e-a1a3-40a1-a15f-7d0ac02f47f1", true);
        assertNotNull(task);

        UserTO template = new UserTO();
        template.setPassword("'password123'");
        template.getResources().add(RESOURCE_NAME_DBVIRATTR);
        template.getVirAttrs().add(attrTO("virtualdata", "'virtualvalue'"));

        task.getTemplates().put(AnyTypeKind.USER.name(), template);

        taskService.update(TaskType.PULL, task);

        // exec task: one user from CSV will match the user created above and template will be applied
        execProvisioningTask(taskService, TaskType.PULL, task.getKey(), 50, false);

        // check that template was successfully applied...
        userTO = userService.read(userTO.getKey());
        assertEquals("virtualvalue", userTO.getVirAttr("virtualdata").get().getValues().get(0));

        // ...and that propagation to db succeeded
        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        String value = queryForObject(
                jdbcTemplate, 50, "SELECT USERNAME FROM testpull WHERE ID=?", String.class, userTO.getKey());
        assertEquals("virtualvalue", value);
    }

    @Test
    public void issueSYNCOPE313DB() throws Exception {
        // 1. create user in DB
        UserTO user = UserITCase.getUniqueSampleTO("syncope313-db@syncope.apache.org");
        user.setPassword("security123");
        user.getResources().add(RESOURCE_NAME_TESTDB);
        user = createUser(user).getEntity();
        assertNotNull(user);
        assertFalse(user.getResources().isEmpty());

        // 2. Check that the DB resource has the correct password
        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        String value = queryForObject(
                jdbcTemplate, 50, "SELECT PASSWORD FROM test WHERE ID=?", String.class, user.getUsername());
        assertEquals(Encryptor.getInstance().encode("security123", CipherAlgorithm.SHA1), value.toUpperCase());

        // 3. Update the password in the DB
        String newCleanPassword = "new-security";
        String newPassword = Encryptor.getInstance().encode(newCleanPassword, CipherAlgorithm.SHA1);
        jdbcTemplate.execute("UPDATE test set PASSWORD='" + newPassword + "' where ID='" + user.getUsername() + "'");

        // 4. Pull the user from the resource
        ImplementationTO pullActions = new ImplementationTO();
        pullActions.setKey(DBPasswordPullActions.class.getSimpleName());
        pullActions.setEngine(ImplementationEngine.JAVA);
        pullActions.setType(ImplementationType.PULL_ACTIONS);
        pullActions.setBody(DBPasswordPullActions.class.getName());
        Response response = implementationService.create(pullActions);
        pullActions = implementationService.read(
                pullActions.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
        assertNotNull(pullActions);

        PullTaskTO pullTask = new PullTaskTO();
        pullTask.setDestinationRealm(SyncopeConstants.ROOT_REALM);
        pullTask.setName("DB Pull Task");
        pullTask.setActive(true);
        pullTask.setPerformCreate(true);
        pullTask.setPerformUpdate(true);
        pullTask.setPullMode(PullMode.FULL_RECONCILIATION);
        pullTask.setResource(RESOURCE_NAME_TESTDB);
        pullTask.getActions().add(pullActions.getKey());
        Response taskResponse = taskService.create(TaskType.PULL, pullTask);

        PullTaskTO actual = getObject(taskResponse.getLocation(), TaskService.class, PullTaskTO.class);
        assertNotNull(actual);

        pullTask = taskService.read(TaskType.PULL, actual.getKey(), true);
        assertNotNull(pullTask);
        assertEquals(actual.getKey(), pullTask.getKey());
        assertEquals(actual.getJobDelegate(), pullTask.getJobDelegate());

        ExecTO execution = execProvisioningTask(taskService, TaskType.PULL, pullTask.getKey(), 50, false);
        assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(execution.getStatus()));

        // 5. Test the pulled user
        Pair<Map<String, Set<String>>, UserTO> self = clientFactory.create(user.getUsername(), newCleanPassword).self();
        assertNotNull(self);

        // 6. Delete PullTask + user
        taskService.delete(TaskType.PULL, pullTask.getKey());
        deleteUser(user.getKey());
    }

    @Test
    public void issueSYNCOPE313LDAP() throws Exception {
        // First of all, clear any potential conflict with existing user / group
        ldapCleanup();

        UserTO user = null;
        PullTaskTO pullTask = null;
        ConnInstanceTO resourceConnector = null;
        ConnConfProperty property = null;
        try {
            // 1. create user in LDAP
            String oldCleanPassword = "security123";
            user = UserITCase.getUniqueSampleTO("syncope313-ldap@syncope.apache.org");
            user.setPassword(oldCleanPassword);
            user.getResources().add(RESOURCE_NAME_LDAP);
            user = createUser(user).getEntity();
            assertNotNull(user);
            assertFalse(user.getResources().isEmpty());

            // 2. request to change password only on Syncope and not on LDAP
            String newCleanPassword = "new-security123";
            UserPatch userPatch = new UserPatch();
            userPatch.setKey(user.getKey());
            userPatch.setPassword(new PasswordPatch.Builder().value(newCleanPassword).build());
            user = updateUser(userPatch).getEntity();

            // 3. Check that the Syncope user now has the changed password
            Pair<Map<String, Set<String>>, UserTO> self =
                    clientFactory.create(user.getUsername(), newCleanPassword).self();
            assertNotNull(self);

            // 4. Check that the LDAP resource has the old password
            ConnObjectTO connObject =
                    resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), user.getKey());
            assertNotNull(getLdapRemoteObject(
                    connObject.getAttr(Name.NAME).get().getValues().get(0),
                    oldCleanPassword,
                    connObject.getAttr(Name.NAME).get().getValues().get(0)));

            // 5. Update the LDAP Connector to retrieve passwords
            ResourceTO ldapResource = resourceService.read(RESOURCE_NAME_LDAP);
            resourceConnector = connectorService.read(
                    ldapResource.getConnector(), Locale.ENGLISH.getLanguage());
            property = resourceConnector.getConf("retrievePasswordsWithSearch").get();
            property.getValues().clear();
            property.getValues().add(Boolean.TRUE);
            connectorService.update(resourceConnector);

            // 6. Pull the user from the resource
            ImplementationTO pullActions = new ImplementationTO();
            pullActions.setKey(LDAPPasswordPullActions.class.getSimpleName());
            pullActions.setEngine(ImplementationEngine.JAVA);
            pullActions.setType(ImplementationType.PULL_ACTIONS);
            pullActions.setBody(LDAPPasswordPullActions.class.getName());
            Response response = implementationService.create(pullActions);
            pullActions = implementationService.read(
                    pullActions.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
            assertNotNull(pullActions);

            pullTask = new PullTaskTO();
            pullTask.setDestinationRealm(SyncopeConstants.ROOT_REALM);
            pullTask.setName("LDAP Pull Task");
            pullTask.setActive(true);
            pullTask.setPerformCreate(true);
            pullTask.setPerformUpdate(true);
            pullTask.setPullMode(PullMode.FULL_RECONCILIATION);
            pullTask.setResource(RESOURCE_NAME_LDAP);
            pullTask.getActions().add(pullActions.getKey());
            Response taskResponse = taskService.create(TaskType.PULL, pullTask);

            pullTask = getObject(taskResponse.getLocation(), TaskService.class, PullTaskTO.class);
            assertNotNull(pullTask);

            ExecTO execution = execProvisioningTask(taskService, TaskType.PULL, pullTask.getKey(), 50, false);
            assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(execution.getStatus()));

            // 7. Test the pulled user
            self = clientFactory.create(user.getUsername(), oldCleanPassword).self();
            assertNotNull(self);
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            // Delete PullTask + user + reset the connector
            if (pullTask != null) {
                taskService.delete(TaskType.PULL, pullTask.getKey());
            }

            if (resourceConnector != null && property != null) {
                property.getValues().clear();
                property.getValues().add(Boolean.FALSE);
                connectorService.update(resourceConnector);
            }

            if (user != null) {
                deleteUser(user.getKey());
            }
        }
    }

    @Test
    public void issueSYNCOPE1062() {
        GroupTO propagationGroup = null;
        PullTaskTO pullTask = null;
        UserTO user = null;
        GroupTO group = null;
        try {
            // 1. create group with resource for propagation
            propagationGroup = GroupITCase.getBasicSampleTO("SYNCOPE1062");
            propagationGroup.getResources().add(RESOURCE_NAME_DBPULL);
            propagationGroup = createGroup(propagationGroup).getEntity();

            // 2. create pull task for another resource, with user template assigning the group above
            pullTask = new PullTaskTO();
            pullTask.setDestinationRealm(SyncopeConstants.ROOT_REALM);
            pullTask.setName("SYNCOPE1062");
            pullTask.setActive(true);
            pullTask.setPerformCreate(true);
            pullTask.setPerformUpdate(true);
            pullTask.setPullMode(PullMode.FULL_RECONCILIATION);
            pullTask.setResource(RESOURCE_NAME_LDAP);

            UserTO template = new UserTO();
            template.getAuxClasses().add("minimal group");
            template.getMemberships().add(new MembershipTO.Builder().group(propagationGroup.getKey()).build());
            template.getPlainAttrs().add(attrTO("firstname", "'fixed'"));
            pullTask.getTemplates().put(AnyTypeKind.USER.name(), template);

            Response taskResponse = taskService.create(TaskType.PULL, pullTask);
            pullTask = getObject(taskResponse.getLocation(), TaskService.class, PullTaskTO.class);
            assertNotNull(pullTask);
            assertFalse(pullTask.getTemplates().isEmpty());

            // 3. exec the pull task
            ExecTO execution = execProvisioningTask(taskService, TaskType.PULL, pullTask.getKey(), 50, false);
            assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(execution.getStatus()));

            // the user is successfully pulled...
            user = userService.read("pullFromLDAP");
            assertNotNull(user);
            assertEquals("pullFromLDAP@syncope.apache.org", user.getPlainAttr("email").get().getValues().get(0));

            group = groupService.read("testLDAPGroup");
            assertNotNull(group);

            ConnObjectTO connObject =
                    resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), user.getKey());
            assertNotNull(connObject);
            assertEquals("pullFromLDAP@syncope.apache.org", connObject.getAttr("mail").get().getValues().get(0));
            AttrTO userDn = connObject.getAttr(Name.NAME).get();
            assertNotNull(userDn);
            assertEquals(1, userDn.getValues().size());
            assertNotNull(
                    getLdapRemoteObject(RESOURCE_LDAP_ADMIN_DN, RESOURCE_LDAP_ADMIN_PWD, userDn.getValues().get(0)));

            // ...and propagated
            PagedResult<TaskTO> propagationTasks = taskService.search(new TaskQuery.Builder(TaskType.PROPAGATION).
                    resource(RESOURCE_NAME_DBPULL).
                    anyTypeKind(AnyTypeKind.USER).entityKey(user.getKey()).build());
            assertEquals(1, propagationTasks.getSize());

            // 4. update the user on the external resource
            updateLdapRemoteObject(RESOURCE_LDAP_ADMIN_DN, RESOURCE_LDAP_ADMIN_PWD,
                    userDn.getValues().get(0), Pair.of("mail", "pullFromLDAP2@syncope.apache.org"));

            connObject = resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), user.getKey());
            assertNotNull(connObject);
            assertEquals("pullFromLDAP2@syncope.apache.org", connObject.getAttr("mail").get().getValues().get(0));

            // 5. exec the pull task again
            execution = execProvisioningTask(taskService, TaskType.PULL, pullTask.getKey(), 50, false);
            assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(execution.getStatus()));

            // the internal is updated...
            user = userService.read("pullFromLDAP");
            assertNotNull(user);
            assertEquals("pullFromLDAP2@syncope.apache.org", user.getPlainAttr("email").get().getValues().get(0));

            // ...and propagated
            propagationTasks = taskService.search(new TaskQuery.Builder(TaskType.PROPAGATION).
                    resource(RESOURCE_NAME_DBPULL).
                    anyTypeKind(AnyTypeKind.USER).entityKey(user.getKey()).build());
            assertEquals(2, propagationTasks.getSize());
        } catch (Exception e) {
            LOG.error("Unexpected during issueSYNCOPE1062()", e);
            fail(e.getMessage());
        } finally {
            if (pullTask != null) {
                taskService.delete(TaskType.PULL, pullTask.getKey());
            }

            if (propagationGroup != null) {
                groupService.delete(propagationGroup.getKey());
            }

            if (group != null) {
                groupService.delete(group.getKey());
            }
            if (user != null) {
                userService.delete(user.getKey());
            }
        }
    }
}

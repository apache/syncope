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

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.policy.PullPolicyTO;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.ResourceDR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ConnObject;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.RemediationTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.TaskTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.AnyQuery;
import org.apache.syncope.common.rest.api.beans.ReconQuery;
import org.apache.syncope.common.rest.api.beans.RemediationQuery;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.ConnectorService;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.core.provisioning.java.pushpull.DBPasswordPullActions;
import org.apache.syncope.core.provisioning.java.pushpull.LDAPPasswordPullActions;
import org.apache.syncope.core.spring.security.Encryptor;
import org.apache.syncope.fit.core.reference.TestPullActions;
import org.identityconnectors.framework.common.objects.Name;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class PullTaskITCase extends AbstractTaskITCase {

    @BeforeAll
    public static void testPullActionsSetup() {
        ImplementationTO pullActions = null;
        try {
            pullActions = IMPLEMENTATION_SERVICE.read(
                    IdMImplementationType.PULL_ACTIONS, TestPullActions.class.getSimpleName());
        } catch (SyncopeClientException e) {
            if (e.getType().getResponseStatus() == Response.Status.NOT_FOUND) {
                pullActions = new ImplementationTO();
                pullActions.setKey(TestPullActions.class.getSimpleName());
                pullActions.setEngine(ImplementationEngine.JAVA);
                pullActions.setType(IdMImplementationType.PULL_ACTIONS);
                pullActions.setBody(TestPullActions.class.getName());
                Response response = IMPLEMENTATION_SERVICE.create(pullActions);
                pullActions = IMPLEMENTATION_SERVICE.read(
                        pullActions.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                assertNotNull(pullActions);
            }
        }
        assertNotNull(pullActions);

        PullTaskTO pullTask = TASK_SERVICE.read(TaskType.PULL, PULL_TASK_KEY, true);
        pullTask.getActions().add(pullActions.getKey());
        TASK_SERVICE.update(TaskType.PULL, pullTask);
    }

    @Test
    public void getPullActionsClasses() {
        Set<String> actions = ADMIN_CLIENT.platform().
                getJavaImplInfo(IdMImplementationType.PULL_ACTIONS).get().getClasses();
        assertNotNull(actions);
        assertFalse(actions.isEmpty());
    }

    @Test
    public void list() {
        PagedResult<PullTaskTO> tasks = TASK_SERVICE.search(new TaskQuery.Builder(TaskType.PULL).build());
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

        userTemplate.getMemberships().add(new MembershipTO.Builder("f779c0d4-633b-4be5-8f57-32eb478a3ca5").build());
        task.getTemplates().put(AnyTypeKind.USER.name(), userTemplate);

        GroupTO groupTemplate = new GroupTO();
        groupTemplate.getResources().add(RESOURCE_NAME_LDAP);
        task.getTemplates().put(AnyTypeKind.GROUP.name(), groupTemplate);

        Response response = TASK_SERVICE.create(TaskType.PULL, task);
        PullTaskTO actual = getObject(response.getLocation(), TaskService.class, PullTaskTO.class);
        assertNotNull(actual);

        task = TASK_SERVICE.read(TaskType.PULL, actual.getKey(), true);
        assertNotNull(task);
        assertEquals(actual.getKey(), task.getKey());
        assertEquals(actual.getJobDelegate(), task.getJobDelegate());
        assertEquals(userTemplate, task.getTemplates().get(AnyTypeKind.USER.name()));
        assertEquals(groupTemplate, task.getTemplates().get(AnyTypeKind.GROUP.name()));
    }

    @Test
    public void fromCSV() throws Exception {
        assumeFalse(IS_ELASTICSEARCH_ENABLED);

        removeTestUsers();

        // Attemp to reset CSV content
        Properties props = new Properties();
        InputStream propStream = null;
        InputStream srcStream = null;
        OutputStream dstStream = null;
        try {
            propStream = getClass().getResourceAsStream("/test.properties");
            props.load(propStream);

            srcStream = new FileInputStream(props.getProperty("test.csv.src"));
            dstStream = new FileOutputStream(props.getProperty("test.csv.dst"));

            IOUtils.copy(srcStream, dstStream);
        } catch (IOException e) {
            fail(e::getMessage);
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
        UserCR inUserRC = new UserCR();
        inUserRC.setRealm(SyncopeConstants.ROOT_REALM);
        inUserRC.setPassword("password123");
        String userName = "test9";
        inUserRC.setUsername(userName);
        inUserRC.getPlainAttrs().add(attr("firstname", "nome9"));
        inUserRC.getPlainAttrs().add(attr("surname", "cognome"));
        inUserRC.getPlainAttrs().add(attr("ctype", "a type"));
        inUserRC.getPlainAttrs().add(attr("fullname", "nome cognome"));
        inUserRC.getPlainAttrs().add(attr("userId", "puccini@syncope.apache.org"));
        inUserRC.getPlainAttrs().add(attr("email", "puccini@syncope.apache.org"));
        inUserRC.getAuxClasses().add("csv");

        UserTO inUserTO = createUser(inUserRC).getEntity();
        assertNotNull(inUserTO);
        assertFalse(inUserTO.getResources().contains(RESOURCE_NAME_CSV));

        // -----------------------------
        try {
            int usersPre = USER_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                    page(1).size(1).build()).getTotalCount();
            assertNotNull(usersPre);

            ExecTO exec = execProvisioningTask(TASK_SERVICE, TaskType.PULL, PULL_TASK_KEY, MAX_WAIT_SECONDS, false);
            assertEquals(ExecStatus.SUCCESS, ExecStatus.valueOf(exec.getStatus()));

            LOG.debug("Execution of task {}:\n{}", PULL_TASK_KEY, exec);

            // check for pull results
            int usersPost = USER_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                    page(1).size(1).build()).getTotalCount();
            assertNotNull(usersPost);
            assertEquals(usersPre + 8, usersPost);

            // after execution of the pull task the user data should have been pulled from CSV
            // and processed by user template
            UserTO userTO = USER_SERVICE.read(inUserTO.getKey());
            assertNotNull(userTO);
            assertEquals(userName, userTO.getUsername());
            assertEquals(IS_FLOWABLE_ENABLED
                    ? "active" : "created", userTO.getStatus());
            assertEquals("test9@syncope.apache.org", userTO.getPlainAttr("email").get().getValues().get(0));
            assertEquals("test9@syncope.apache.org", userTO.getPlainAttr("userId").get().getValues().get(0));
            assertTrue(Integer.valueOf(userTO.getPlainAttr("fullname").get().getValues().get(0)) <= 10);
            assertTrue(userTO.getResources().contains(RESOURCE_NAME_TESTDB));
            assertTrue(userTO.getResources().contains(RESOURCE_NAME_WS2));

            // Matching --> Update (no link)
            assertFalse(userTO.getResources().contains(RESOURCE_NAME_CSV));

            // check for user template
            userTO = USER_SERVICE.read("test7");
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

            userTO = USER_SERVICE.read("test8");
            assertNotNull(userTO);
            assertEquals("TYPE_8", userTO.getPlainAttr("ctype").get().getValues().get(0));

            // Check for ignored user - SYNCOPE-663
            try {
                USER_SERVICE.read("test2");
                fail("This should not happen");
            } catch (SyncopeClientException e) {
                assertEquals(Response.Status.NOT_FOUND, e.getType().getResponseStatus());
            }

            // Check for issue 215:
            // * expected disabled user test1
            // * expected enabled user test3
            userTO = USER_SERVICE.read("test1");
            assertNotNull(userTO);
            assertEquals("suspended", userTO.getStatus());

            userTO = USER_SERVICE.read("test3");
            assertNotNull(userTO);
            assertEquals("active", userTO.getStatus());

            Set<String> otherPullTaskKeys = Set.of(
                    "feae4e57-15ca-40d9-b973-8b9015efca49",
                    "55d5e74b-497e-4bc0-9156-73abef4b9adc");
            execProvisioningTasks(TASK_SERVICE, TaskType.PULL, otherPullTaskKeys, MAX_WAIT_SECONDS, false);

            // Matching --> UNLINK
            assertFalse(USER_SERVICE.read("test9").getResources().contains(RESOURCE_NAME_CSV));
            assertFalse(USER_SERVICE.read("test7").getResources().contains(RESOURCE_NAME_CSV));
        } finally {
            removeTestUsers();
        }
    }

    @Test
    public void dryRun() {
        ExecTO execution = execProvisioningTask(TASK_SERVICE, TaskType.PULL, PULL_TASK_KEY, MAX_WAIT_SECONDS, true);
        assertEquals("SUCCESS", execution.getStatus());
    }

    @Test
    public void reconcileFromDB() {
        UserTO userTO = null;
        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        try {
            ExecTO execution = execProvisioningTask(
                    TASK_SERVICE, TaskType.PULL, "83f7e85d-9774-43fe-adba-ccd856312994", MAX_WAIT_SECONDS, false);
            assertEquals(ExecStatus.SUCCESS, ExecStatus.valueOf(execution.getStatus()));

            userTO = USER_SERVICE.read("testuser1");
            assertNotNull(userTO);
            assertEquals("reconciled@syncope.apache.org", userTO.getPlainAttr("userId").get().getValues().get(0));
            assertEquals("suspended", userTO.getStatus());

            // enable user on external resource
            jdbcTemplate.execute("UPDATE TEST SET status=TRUE WHERE id='testuser1'");

            // re-execute the same PullTask: now user must be active
            execution = execProvisioningTask(
                    TASK_SERVICE, TaskType.PULL, "83f7e85d-9774-43fe-adba-ccd856312994", MAX_WAIT_SECONDS, false);
            assertEquals(ExecStatus.SUCCESS, ExecStatus.valueOf(execution.getStatus()));

            userTO = USER_SERVICE.read("testuser1");
            assertNotNull(userTO);
            assertEquals("active", userTO.getStatus());
        } finally {
            jdbcTemplate.execute("UPDATE TEST SET status=FALSE WHERE id='testuser1'");
            if (userTO != null) {
                USER_SERVICE.delete(userTO.getKey());
            }
        }
    }

    @Test
    public void reconcileFromLDAP() {
        // First of all, clear any potential conflict with existing user / group
        ldapCleanup();

        // 0. pull
        ExecTO execution = execProvisioningTask(
                TASK_SERVICE, TaskType.PULL, "1e419ca4-ea81-4493-a14f-28b90113686d", MAX_WAIT_SECONDS, false);

        // 1. verify execution status
        assertEquals(ExecStatus.SUCCESS, ExecStatus.valueOf(execution.getStatus()));

        // SYNCOPE-898
        PullTaskTO task = TASK_SERVICE.read(TaskType.PULL, "1e419ca4-ea81-4493-a14f-28b90113686d", false);
        assertEquals(SyncopeConstants.ROOT_REALM, task.getDestinationRealm());

        // 2. verify that pulled group is found
        PagedResult<GroupTO> matchingGroups = GROUP_SERVICE.search(new AnyQuery.Builder().realm(
                SyncopeConstants.ROOT_REALM).
                fiql(SyncopeClient.getGroupSearchConditionBuilder().is("name").equalTo("testLDAPGroup").query()).
                build());
        assertNotNull(matchingGroups);
        assertEquals(1, matchingGroups.getResult().size());
        assertEquals(SyncopeConstants.ROOT_REALM, matchingGroups.getResult().get(0).getRealm());

        // 3. verify that pulled user is found
        PagedResult<UserTO> matchingUsers = USER_SERVICE.search(
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
        // Check for SYNCOPE-1343
        assertEquals("odd", matchingUsers.getResult().get(0).getPlainAttr("title").get().getValues().get(0));

        PagedResult<UserTO> matchByLastChangeContext = USER_SERVICE.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().is("lastChangeContext").
                                equalTo("*PullTask " + task.getKey() + "*").query()).
                        build());
        assertNotNull(matchByLastChangeContext);
        assertNotEquals(0, matchByLastChangeContext.getTotalCount());

        GroupTO groupTO = matchingGroups.getResult().get(0);
        assertNotNull(groupTO);
        assertEquals("testLDAPGroup", groupTO.getName());
        assertTrue(groupTO.getLastChangeContext().contains("PullTask " + task.getKey()));
        assertEquals("true", groupTO.getPlainAttr("show").get().getValues().get(0));
        assertEquals(matchingUsers.getResult().get(0).getKey(), groupTO.getUserOwner());
        assertNull(groupTO.getGroupOwner());
        // SYNCOPE-1343, set value title to null on LDAP
        ConnObject userConnObject = RESOURCE_SERVICE.readConnObject(
                RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), matchingUsers.getResult().get(0).getKey());
        assertNotNull(userConnObject);
        assertEquals("odd", userConnObject.getAttr("title").get().getValues().get(0));
        Attr userDn = userConnObject.getAttr(Name.NAME).get();
        updateLdapRemoteObject(RESOURCE_LDAP_ADMIN_DN, RESOURCE_LDAP_ADMIN_PWD,
                userDn.getValues().get(0), Collections.singletonMap("title", null));

        // SYNCOPE-317
        execProvisioningTask(
                TASK_SERVICE, TaskType.PULL, "1e419ca4-ea81-4493-a14f-28b90113686d", MAX_WAIT_SECONDS, false);

        // 4. verify that LDAP group membership is pulled as Syncope membership
        AtomicReference<Integer> numMembers = new AtomicReference<>();
        await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                PagedResult<UserTO> members = USER_SERVICE.search(
                        new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                                fiql(SyncopeClient.getUserSearchConditionBuilder().inGroups(groupTO.getKey()).query()).
                                build());
                numMembers.set(members.getResult().size());
                return !members.getResult().isEmpty();
            } catch (Exception e) {
                return false;
            }
        });
        assertEquals(1, numMembers.get());

        // SYNCOPE-1343, verify that the title attribute has been reset
        matchingUsers = USER_SERVICE.search(
                new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                        fiql(SyncopeClient.getUserSearchConditionBuilder().is("username").equalTo("pullFromLDAP").
                                query()).
                        build());
        assertNull(matchingUsers.getResult().get(0).getPlainAttr("title").orElse(null));

        // SYNCOPE-1356 remove group membership from LDAP, pull and check in Syncope
        ConnObject groupConnObject = RESOURCE_SERVICE.readConnObject(
                RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), matchingGroups.getResult().get(0).getKey());
        assertNotNull(groupConnObject);
        Attr groupDn = groupConnObject.getAttr(Name.NAME).get();
        updateLdapRemoteObject(RESOURCE_LDAP_ADMIN_DN, RESOURCE_LDAP_ADMIN_PWD,
                groupDn.getValues().get(0), Map.of("uniquemember", "uid=admin,ou=system"));

        execProvisioningTask(
                TASK_SERVICE, TaskType.PULL, "1e419ca4-ea81-4493-a14f-28b90113686d", MAX_WAIT_SECONDS, false);

        await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                return USER_SERVICE.search(
                        new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                                fiql(SyncopeClient.getUserSearchConditionBuilder().inGroups(groupTO.getKey()).query()).
                                build()).getResult().isEmpty();
            } catch (Exception e) {
                return false;
            }
        });
    }

    @Test
    public void reconcileFromScriptedSQL() throws IOException {
        // 0. reset sync token and set MappingItemTransformer
        ResourceTO resource = RESOURCE_SERVICE.read(RESOURCE_NAME_DBSCRIPTED);
        ResourceTO originalResource = SerializationUtils.clone(resource);
        Provision provision = resource.getProvision(PRINTER).get();
        assertNotNull(provision);

        ImplementationTO transformer = null;
        try {
            transformer = IMPLEMENTATION_SERVICE.read(
                    IdRepoImplementationType.ITEM_TRANSFORMER, "PrefixItemTransformer");
        } catch (SyncopeClientException e) {
            if (e.getType().getResponseStatus() == Response.Status.NOT_FOUND) {
                transformer = new ImplementationTO();
                transformer.setKey("PrefixItemTransformer");
                transformer.setEngine(ImplementationEngine.GROOVY);
                transformer.setType(IdRepoImplementationType.ITEM_TRANSFORMER);
                transformer.setBody(IOUtils.toString(
                        getClass().getResourceAsStream("/PrefixItemTransformer.groovy"), StandardCharsets.UTF_8));
                Response response = IMPLEMENTATION_SERVICE.create(transformer);
                transformer = IMPLEMENTATION_SERVICE.read(
                        transformer.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                assertNotNull(transformer.getKey());
            }
        }
        assertNotNull(transformer);

        Item mappingItem = provision.getMapping().getItems().stream().
                filter(object -> "location".equals(object.getIntAttrName())).findFirst().get();
        assertNotNull(mappingItem);
        mappingItem.getTransformers().clear();
        mappingItem.getTransformers().add(transformer.getKey());

        final String prefix = "PREFIX_";
        try {
            RESOURCE_SERVICE.update(resource);
            RESOURCE_SERVICE.removeSyncToken(resource.getKey(), provision.getAnyType());

            // insert a deleted record in the external resource (SYNCOPE-923), which will be returned
            // as sync event prior to the CREATE_OR_UPDATE events generated by the actions below (before pull)
            JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
            jdbcTemplate.update(
                    "INSERT INTO TESTPRINTER (id, printername, location, deleted, lastmodification) VALUES (?,?,?,?,?)",
                    UUID.randomUUID().toString(), "Mysterious Printer", "Nowhere", true, new Date());

            // 1. create printer on external resource
            AnyObjectCR anyObjectCR = AnyObjectITCase.getSample("pull");
            AnyObjectTO anyObjectTO = createAnyObject(anyObjectCR).getEntity();
            assertNotNull(anyObjectTO);
            String originalLocation = anyObjectTO.getPlainAttr("location").get().getValues().get(0);
            assertFalse(originalLocation.startsWith(prefix));

            // 2. verify that PrefixMappingItemTransformer was applied during propagation
            // (location starts with given prefix on external resource)
            ConnObject connObjectTO = RESOURCE_SERVICE.readConnObject(
                    RESOURCE_NAME_DBSCRIPTED, anyObjectTO.getType(), anyObjectTO.getKey());
            assertFalse(anyObjectTO.getPlainAttr("location").get().getValues().get(0).startsWith(prefix));
            assertTrue(connObjectTO.getAttr("LOCATION").get().getValues().get(0).startsWith(prefix));

            // 3. unlink any existing printer and delete from Syncope (printer is now only on external resource)
            PagedResult<AnyObjectTO> matchingPrinters = ANY_OBJECT_SERVICE.search(
                    new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                            fiql(SyncopeClient.getAnyObjectSearchConditionBuilder(PRINTER).
                                    is("location").equalTo("pull*").query()).build());
            assertTrue(matchingPrinters.getSize() > 0);
            for (AnyObjectTO printer : matchingPrinters.getResult()) {
                ANY_OBJECT_SERVICE.deassociate(new ResourceDR.Builder().key(printer.getKey()).
                        action(ResourceDeassociationAction.UNLINK).resource(RESOURCE_NAME_DBSCRIPTED).build());
                ANY_OBJECT_SERVICE.delete(printer.getKey());
            }

            // ensure that the pull task does not have the DELETE capability (SYNCOPE-923)
            PullTaskTO pullTask = TASK_SERVICE.read(TaskType.PULL, "30cfd653-257b-495f-8665-281281dbcb3d", false);
            assertNotNull(pullTask);
            assertFalse(pullTask.isPerformDelete());

            // 4. pull
            execProvisioningTask(TASK_SERVICE, TaskType.PULL, pullTask.getKey(), MAX_WAIT_SECONDS, false);

            if (IS_ELASTICSEARCH_ENABLED) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    // ignore
                }
            }

            // 5. verify that printer was re-created in Syncope (implies that location does not start with given prefix,
            // hence PrefixItemTransformer was applied during pull)
            matchingPrinters = ANY_OBJECT_SERVICE.search(new AnyQuery.Builder().realm(SyncopeConstants.ROOT_REALM).
                    fiql(SyncopeClient.getAnyObjectSearchConditionBuilder(PRINTER).
                            is("location").equalTo("pull*").query()).build());
            assertTrue(matchingPrinters.getSize() > 0);

            // 6. verify that synctoken was updated
            assertNotNull(RESOURCE_SERVICE.read(RESOURCE_NAME_DBSCRIPTED).
                    getProvision(anyObjectTO.getType()).get().getSyncToken());
        } finally {
            RESOURCE_SERVICE.update(originalResource);
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
                    + '\'' + user1OnTestPull + "', 'user1', 'Doe', false, 'mail1@apache.org', NULL)");
            jdbcTemplate.execute("INSERT INTO testpull VALUES ("
                    + '\'' + user2OnTestPull + "', 'user2', 'Rossi', false, 'mail2@apache.org', NULL)");

            // 2. create new pull task for test-db, with reconciliation filter (surname 'Rossi') 
            ImplementationTO reconFilterBuilder = new ImplementationTO();
            reconFilterBuilder.setKey("TestReconFilterBuilder");
            reconFilterBuilder.setEngine(ImplementationEngine.GROOVY);
            reconFilterBuilder.setType(IdMImplementationType.RECON_FILTER_BUILDER);
            reconFilterBuilder.setBody(IOUtils.toString(
                    getClass().getResourceAsStream("/TestReconFilterBuilder.groovy"), StandardCharsets.UTF_8));
            Response response = IMPLEMENTATION_SERVICE.create(reconFilterBuilder);
            reconFilterBuilder = IMPLEMENTATION_SERVICE.read(
                    reconFilterBuilder.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
            assertNotNull(reconFilterBuilder);

            task = TASK_SERVICE.read(TaskType.PULL, "7c2242f4-14af-4ab5-af31-cdae23783655", true);
            task.setPullMode(PullMode.FILTERED_RECONCILIATION);
            task.setReconFilterBuilder(reconFilterBuilder.getKey());
            response = TASK_SERVICE.create(TaskType.PULL, task);
            task = getObject(response.getLocation(), TaskService.class, PullTaskTO.class);
            assertNotNull(task);
            assertEquals(reconFilterBuilder.getKey(), task.getReconFilterBuilder());

            // 3. exec task
            ExecTO execution = execProvisioningTask(
                    TASK_SERVICE, TaskType.PULL, task.getKey(), MAX_WAIT_SECONDS, false);
            assertEquals(ExecStatus.SUCCESS, ExecStatus.valueOf(execution.getStatus()));

            // 4. verify that only enabled user was pulled
            userTO = USER_SERVICE.read("user2");
            assertNotNull(userTO);

            try {
                USER_SERVICE.read("user1");
                fail("This should not happen");
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.NotFound, e.getType());
            }
        } finally {
            jdbcTemplate.execute("DELETE FROM testpull WHERE id = '" + user1OnTestPull + '\'');
            jdbcTemplate.execute("DELETE FROM testpull WHERE id = '" + user2OnTestPull + '\'');
            if (task != null && !"7c2242f4-14af-4ab5-af31-cdae23783655".equals(task.getKey())) {
                TASK_SERVICE.delete(TaskType.PULL, task.getKey());
            }
            if (userTO != null) {
                USER_SERVICE.delete(userTO.getKey());
            }
        }
    }

    @Test
    public void syncTokenWithErrors() {
        ResourceTO origResource = RESOURCE_SERVICE.read(RESOURCE_NAME_DBPULL);
        ConnInstanceTO origConnector = CONNECTOR_SERVICE.read(origResource.getConnector(), null);

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

            Response response = CONNECTOR_SERVICE.create(connForTest);
            if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
                throw (RuntimeException) CLIENT_FACTORY.getExceptionMapper().fromResponse(response);
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

            response = TASK_SERVICE.create(TaskType.PULL, pullTask);
            if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
                throw (RuntimeException) CLIENT_FACTORY.getExceptionMapper().fromResponse(response);
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

            ExecTO exec = execProvisioningTask(TASK_SERVICE, TaskType.PULL, pullTask.getKey(), MAX_WAIT_SECONDS, false);
            assertEquals(ExecStatus.SUCCESS, ExecStatus.valueOf(exec.getStatus()));

            resForTest = RESOURCE_SERVICE.read(resForTest.getKey());
            assertTrue(resForTest.getProvision(AnyTypeKind.USER.name()).get().getSyncToken().contains("2014-05-23"));

            jdbcTemplate.execute("UPDATE testpull "
                    + "SET email='syncTokenWithErrors2@syncope.apache.org', lastModification='2016-05-23 13:53:24.293' "
                    + "WHERE ID=1041");

            exec = execProvisioningTask(TASK_SERVICE, TaskType.PULL, pullTask.getKey(), MAX_WAIT_SECONDS, false);
            assertEquals(ExecStatus.SUCCESS, ExecStatus.valueOf(exec.getStatus()));

            resForTest = RESOURCE_SERVICE.read(resForTest.getKey());
            assertTrue(resForTest.getProvision(AnyTypeKind.USER.name()).get().getSyncToken().contains("2016-05-23"));
        } finally {
            if (resForTest.getConnector() != null) {
                RESOURCE_SERVICE.delete(resForTest.getKey());
                CONNECTOR_SERVICE.delete(connForTest.getKey());
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
        ResourceTO ldap = RESOURCE_SERVICE.read(RESOURCE_NAME_LDAP);
        ldap.setKey("ldapForRemediation");

        Provision provision = ldap.getProvision(AnyTypeKind.USER.name()).get();
        provision.getMapping().getItems().removeIf(item -> "userId".equals(item.getIntAttrName()));
        provision.getMapping().getItems().removeIf(item -> "mail".equals(item.getIntAttrName()));
        provision.getVirSchemas().clear();

        ldap.getProvisions().clear();
        ldap.getProvisions().add(provision);

        ldap = createResource(ldap);

        // 2. create PullTask with remediation enabled, for the new resource
        PullTaskTO pullTask = (PullTaskTO) TASK_SERVICE.search(new TaskQuery.Builder(TaskType.PULL).
                resource(RESOURCE_NAME_LDAP).build()).getResult().get(0);
        assertNotNull(pullTask);
        pullTask.setResource(ldap.getKey());
        pullTask.setRemediation(true);
        pullTask.getActions().clear();

        Response response = TASK_SERVICE.create(TaskType.PULL, pullTask);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            throw (RuntimeException) CLIENT_FACTORY.getExceptionMapper().fromResponse(response);
        }
        pullTask = getObject(response.getLocation(), TaskService.class, PullTaskTO.class);
        assertNotNull(pullTask);

        try {
            // 3. execute the pull task and verify that:
            ExecTO execution = execProvisioningTask(
                    TASK_SERVICE, TaskType.PULL, pullTask.getKey(), MAX_WAIT_SECONDS, false);
            assertEquals(ExecStatus.SUCCESS, ExecStatus.valueOf(execution.getStatus()));

            // 3a. user was not pulled
            try {
                USER_SERVICE.read("pullFromLDAP");
                fail("This should never happen");
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.NotFound, e.getType());
            }

            // 3b. remediation was created
            Optional<RemediationTO> remediation = REMEDIATION_SERVICE.list(
                    new RemediationQuery.Builder().page(1).size(1000).build()).getResult().stream().
                    filter(r -> "uid=pullFromLDAP,ou=People,o=isp".equalsIgnoreCase(r.getRemoteName())).
                    findFirst();
            assertTrue(remediation.isPresent());
            assertEquals(AnyTypeKind.USER.name(), remediation.get().getAnyType());
            assertEquals(ResourceOperation.CREATE, remediation.get().getOperation());
            assertNotNull(remediation.get().getAnyCRPayload());
            assertNull(remediation.get().getAnyURPayload());
            assertNull(remediation.get().getKeyPayload());
            assertTrue(remediation.get().getError().contains("RequiredValuesMissing [userId]"));

            // 4. remedy by copying the email value to userId
            AnyCR userCR = remediation.get().getAnyCRPayload();
            userCR.getResources().clear();

            String email = userCR.getPlainAttr("email").get().getValues().get(0);
            userCR.getPlainAttrs().add(new Attr.Builder("userId").value(email).build());

            REMEDIATION_SERVICE.remedy(remediation.get().getKey(), userCR);

            // 5. user is now found
            UserTO user = USER_SERVICE.read("pullFromLDAP");
            assertNotNull(user);
            assertEquals(email, user.getPlainAttr("userId").get().getValues().get(0));

            // 6. remediation was removed
            try {
                REMEDIATION_SERVICE.read(remediation.get().getKey());
                fail("This should never happen");
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.NotFound, e.getType());
            }
        } finally {
            RESOURCE_SERVICE.delete(ldap.getKey());
        }
    }

    @Test
    public void remediationSinglePull() throws IOException {
        // First of all, clear any potential conflict with existing user / group
        ldapCleanup();

        ResourceTO ldap = RESOURCE_SERVICE.read(RESOURCE_NAME_LDAP);
        ldap.setKey("ldapForRemediationSinglePull");

        Provision provision = ldap.getProvision(AnyTypeKind.USER.name()).get();
        provision.getMapping().getItems().removeIf(item -> "userId".equals(item.getIntAttrName()));
        provision.getMapping().getItems().removeIf(item -> "email".equals(item.getIntAttrName()));
        provision.getVirSchemas().clear();

        ldap.getProvisions().clear();
        ldap.getProvisions().add(provision);

        ldap = createResource(ldap);

        try {
            // 2. pull an user
            PullTaskTO pullTask = new PullTaskTO();
            pullTask.setResource(ldap.getKey());
            pullTask.setDestinationRealm(SyncopeConstants.ROOT_REALM);
            pullTask.setRemediation(true);
            pullTask.setPerformCreate(true);
            pullTask.setPerformUpdate(true);
            pullTask.setUnmatchingRule(UnmatchingRule.ASSIGN);
            pullTask.setMatchingRule(MatchingRule.UPDATE);

            try {
                RECONCILIATION_SERVICE.pull(new ReconQuery.Builder(AnyTypeKind.USER.name(), ldap.getKey()).fiql(
                        "uid==pullFromLDAP").build(), pullTask);
                fail("Should not arrive here");
            } catch (SyncopeClientException sce) {
                assertEquals(ClientExceptionType.Reconciliation, sce.getType());
            }
            Optional<RemediationTO> remediation = REMEDIATION_SERVICE.list(
                    new RemediationQuery.Builder().after(OffsetDateTime.now().minusSeconds(30)).
                            page(1).size(1000).build()).getResult().stream().
                    filter(r -> "uid=pullFromLDAP,ou=People,o=isp".equalsIgnoreCase(r.getRemoteName())).
                    findFirst();
            assertTrue(remediation.isPresent());
            assertEquals(AnyTypeKind.USER.name(), remediation.get().getAnyType());
            assertEquals(ResourceOperation.CREATE, remediation.get().getOperation());
            assertNotNull(remediation.get().getAnyCRPayload());
            assertNull(remediation.get().getAnyURPayload());
            assertNull(remediation.get().getKeyPayload());
            assertTrue(remediation.get().getError().contains(
                    "SyncopeClientCompositeException: {[RequiredValuesMissing [userId]]}"));
        } finally {
            RESOURCE_SERVICE.delete(ldap.getKey());
            cleanUpRemediations();
        }
    }

    @Test
    public void issueSYNCOPE68() {
        //-----------------------------
        // Create a new user ... it should be updated applying pull policy
        //-----------------------------
        UserCR userCR = new UserCR();
        userCR.setRealm(SyncopeConstants.ROOT_REALM);
        userCR.setPassword("password123");
        userCR.setUsername("testuser2");

        userCR.getPlainAttrs().add(attr("firstname", "testuser2"));
        userCR.getPlainAttrs().add(attr("surname", "testuser2"));
        userCR.getPlainAttrs().add(attr("ctype", "a type"));
        userCR.getPlainAttrs().add(attr("fullname", "a type"));
        userCR.getPlainAttrs().add(attr("userId", "testuser2@syncope.apache.org"));
        userCR.getPlainAttrs().add(attr("email", "testuser2@syncope.apache.org"));

        userCR.getResources().add(RESOURCE_NAME_NOPROPAGATION2);
        userCR.getResources().add(RESOURCE_NAME_NOPROPAGATION4);

        userCR.getMemberships().add(new MembershipTO.Builder("bf825fe1-7320-4a54-bd64-143b5c18ab97").build());

        UserTO userTO = createUser(userCR).getEntity();
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

            template.getMemberships().add(new MembershipTO.Builder("b8d38784-57e7-4595-859a-076222644b55").build());

            template.getResources().add(RESOURCE_NAME_NOPROPAGATION4);
            //-----------------------------

            // Update pull task
            PullTaskTO task = TASK_SERVICE.read(TaskType.PULL, "81d88f73-d474-4450-9031-605daa4e313f", true);
            assertNotNull(task);

            task.getTemplates().put(AnyTypeKind.USER.name(), template);

            TASK_SERVICE.update(TaskType.PULL, task);
            PullTaskTO actual = TASK_SERVICE.read(TaskType.PULL, task.getKey(), true);
            assertNotNull(actual);
            assertEquals(task.getKey(), actual.getKey());
            assertFalse(actual.getTemplates().get(AnyTypeKind.USER.name()).getResources().isEmpty());
            assertFalse(((UserTO) actual.getTemplates().get(AnyTypeKind.USER.name())).getMemberships().isEmpty());

            ExecTO execution = execProvisioningTask(
                    TASK_SERVICE, TaskType.PULL, actual.getKey(), MAX_WAIT_SECONDS, false);
            assertEquals(ExecStatus.SUCCESS, ExecStatus.valueOf(execution.getStatus()));

            userTO = USER_SERVICE.read("testuser2");
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
                + "('" + id + "', 'issuesyncope230', 'Surname230', false, 'syncope230@syncope.apache.org', NULL)");

        // 2. execute PullTask for resource-db-pull (table TESTPULL on external H2)
        execProvisioningTask(
                TASK_SERVICE, TaskType.PULL, "7c2242f4-14af-4ab5-af31-cdae23783655", MAX_WAIT_SECONDS, false);

        // 3. read e-mail address for user created by the PullTask first execution
        UserTO userTO = USER_SERVICE.read("issuesyncope230");
        assertNotNull(userTO);
        String email = userTO.getPlainAttr("email").get().getValues().iterator().next();
        assertNotNull(email);

        // 4. update TESTPULL on external H2 by changing e-mail address
        jdbcTemplate.execute("UPDATE TESTPULL SET email='updatedSYNCOPE230@syncope.apache.org' WHERE id='" + id + '\'');

        // 5. re-execute the PullTask
        execProvisioningTask(
                TASK_SERVICE, TaskType.PULL, "7c2242f4-14af-4ab5-af31-cdae23783655", MAX_WAIT_SECONDS, false);

        // 6. verify that the e-mail was updated
        userTO = USER_SERVICE.read("issuesyncope230");
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
            corrRule = IMPLEMENTATION_SERVICE.read(IdMImplementationType.PULL_CORRELATION_RULE, "TestPullRule");
        } catch (SyncopeClientException e) {
            if (e.getType().getResponseStatus() == Response.Status.NOT_FOUND) {
                corrRule = new ImplementationTO();
                corrRule.setKey("TestPullRule");
                corrRule.setEngine(ImplementationEngine.GROOVY);
                corrRule.setType(IdMImplementationType.PULL_CORRELATION_RULE);
                corrRule.setBody(IOUtils.toString(
                        getClass().getResourceAsStream("/TestPullRule.groovy"), StandardCharsets.UTF_8));
                Response response = IMPLEMENTATION_SERVICE.create(corrRule);
                corrRule = IMPLEMENTATION_SERVICE.read(
                        corrRule.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                assertNotNull(corrRule);
            }
        }
        assertNotNull(corrRule);

        PullPolicyTO policyTO = POLICY_SERVICE.read(PolicyType.PULL, "9454b0d7-2610-400a-be82-fc23cf553dd6");
        policyTO.getCorrelationRules().put(AnyTypeKind.USER.name(), corrRule.getKey());
        POLICY_SERVICE.update(PolicyType.PULL, policyTO);
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

        Response response = TASK_SERVICE.create(TaskType.PULL, task);
        task = getObject(response.getLocation(), TaskService.class, PullTaskTO.class);

        UserCR userCR = UserITCase.getUniqueSample("s258_1@apache.org");
        userCR.getResources().clear();
        userCR.getResources().add(RESOURCE_NAME_WS2);

        createUser(userCR);

        userCR = UserITCase.getUniqueSample("s258_2@apache.org");
        userCR.getResources().clear();
        userCR.getResources().add(RESOURCE_NAME_WS2);

        UserTO userTO = createUser(userCR).getEntity();

        // change email in order to unmatch the second user
        UserUR userUR = new UserUR();
        userUR.setKey(userTO.getKey());
        userUR.getPlainAttrs().add(attrAddReplacePatch("email", "s258@apache.org"));

        USER_SERVICE.update(userUR);

        execProvisioningTask(TASK_SERVICE, TaskType.PULL, task.getKey(), MAX_WAIT_SECONDS, false);

        PullTaskTO executed = TASK_SERVICE.read(TaskType.PULL, task.getKey(), true);
        assertEquals(1, executed.getExecutions().size());

        // asser for just one match
        assertTrue(executed.getExecutions().get(0).getMessage().contains("[updated/failures]: 1/0"));
    }

    @Test
    public void issueSYNCOPE272() {
        removeTestUsers();

        // create user with testdb resource
        UserCR userCR = UserITCase.getUniqueSample("syncope272@syncope.apache.org");
        userCR.getResources().add(RESOURCE_NAME_TESTDB);

        ProvisioningResult<UserTO> result = createUser(userCR);
        UserTO userTO = result.getEntity();
        try {
            assertNotNull(userTO);
            assertEquals(1, result.getPropagationStatuses().size());
            assertEquals(ExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());

            ExecTO taskExecTO = execProvisioningTask(
                    TASK_SERVICE, TaskType.PULL, "986867e2-993b-430e-8feb-aa9abb4c1dcd", MAX_WAIT_SECONDS, false);

            assertNotNull(taskExecTO.getStatus());
            assertEquals(ExecStatus.SUCCESS, ExecStatus.valueOf(taskExecTO.getStatus()));

            userTO = USER_SERVICE.read(userTO.getKey());
            assertNotNull(userTO);
            assertNotNull(userTO.getPlainAttr("firstname").get().getValues().get(0));
        } finally {
            removeTestUsers();
        }
    }

    @Test
    public void issueSYNCOPE307() {
        assumeFalse(IS_ELASTICSEARCH_ENABLED);

        UserCR userCR = UserITCase.getUniqueSample("s307@apache.org");
        userCR.setUsername("test0");
        userCR.getPlainAttrs().removeIf(attr -> "firstname".equals(attr.getSchema()));
        userCR.getPlainAttrs().add(attr("firstname", "nome0"));
        userCR.getAuxClasses().add("csv");

        userCR.getResources().clear();
        userCR.getResources().add(RESOURCE_NAME_WS2);

        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);

        userTO = USER_SERVICE.read(userTO.getKey());
        assertTrue(userTO.getVirAttrs().isEmpty());

        // Update pull task
        PullTaskTO task = TASK_SERVICE.read(TaskType.PULL, "38abbf9e-a1a3-40a1-a15f-7d0ac02f47f1", true);
        assertNotNull(task);

        UserTO template = new UserTO();
        template.setPassword("'password123'");
        template.getResources().add(RESOURCE_NAME_DBVIRATTR);
        template.getVirAttrs().add(attr("virtualdata", "'virtualvalue'"));

        task.getTemplates().put(AnyTypeKind.USER.name(), template);

        TASK_SERVICE.update(TaskType.PULL, task);

        // exec task: one user from CSV will match the user created above and template will be applied
        ExecTO exec = execProvisioningTask(TASK_SERVICE, TaskType.PULL, task.getKey(), MAX_WAIT_SECONDS, false);

        // check that template was successfully applied
        // 1. propagation to db
        assertEquals(ExecStatus.SUCCESS.name(), exec.getStatus());

        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        String value = queryForObject(jdbcTemplate,
                MAX_WAIT_SECONDS, "SELECT USERNAME FROM testpull WHERE ID=?", String.class, userTO.getKey());
        assertEquals("virtualvalue", value);

        // 2. virtual attribute
        userTO = USER_SERVICE.read(userTO.getKey());
        assertEquals("virtualvalue", userTO.getVirAttr("virtualdata").get().getValues().get(0));
    }

    @Test
    public void issueSYNCOPE313DB() throws Exception {
        // 1. create user in DB
        UserCR userCR = UserITCase.getUniqueSample("syncope313-db@syncope.apache.org");
        userCR.setPassword("security123");
        userCR.getResources().add(RESOURCE_NAME_TESTDB);
        UserTO user = createUser(userCR).getEntity();
        assertNotNull(user);
        assertFalse(user.getResources().isEmpty());

        // 2. Check that the DB resource has the correct password
        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        String value = queryForObject(jdbcTemplate,
                MAX_WAIT_SECONDS, "SELECT PASSWORD FROM test WHERE ID=?", String.class, user.getUsername());
        assertEquals(Encryptor.getInstance().encode("security123", CipherAlgorithm.SHA1), value.toUpperCase());

        // 3. Update the password in the DB
        String newCleanPassword = "new-security";
        String newPassword = Encryptor.getInstance().encode(newCleanPassword, CipherAlgorithm.SHA1);
        jdbcTemplate.execute("UPDATE test set PASSWORD='" + newPassword + "' where ID='" + user.getUsername() + '\'');

        // 4. Pull the user from the resource
        ImplementationTO pullActions = new ImplementationTO();
        pullActions.setKey(DBPasswordPullActions.class.getSimpleName());
        pullActions.setEngine(ImplementationEngine.JAVA);
        pullActions.setType(IdMImplementationType.PULL_ACTIONS);
        pullActions.setBody(DBPasswordPullActions.class.getName());
        Response response = IMPLEMENTATION_SERVICE.create(pullActions);
        pullActions = IMPLEMENTATION_SERVICE.read(
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
        Response taskResponse = TASK_SERVICE.create(TaskType.PULL, pullTask);

        PullTaskTO actual = getObject(taskResponse.getLocation(), TaskService.class, PullTaskTO.class);
        assertNotNull(actual);

        pullTask = TASK_SERVICE.read(TaskType.PULL, actual.getKey(), true);
        assertNotNull(pullTask);
        assertEquals(actual.getKey(), pullTask.getKey());
        assertEquals(actual.getJobDelegate(), pullTask.getJobDelegate());

        ExecTO execution = execProvisioningTask(
                TASK_SERVICE, TaskType.PULL, pullTask.getKey(), MAX_WAIT_SECONDS, false);
        assertEquals(ExecStatus.SUCCESS, ExecStatus.valueOf(execution.getStatus()));

        // 5. Test the pulled user
        Triple<Map<String, Set<String>>, List<String>, UserTO> self =
                CLIENT_FACTORY.create(user.getUsername(), newCleanPassword).self();
        assertNotNull(self);

        // 6. Delete PullTask + user
        TASK_SERVICE.delete(TaskType.PULL, pullTask.getKey());
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
            UserCR userCR = UserITCase.getUniqueSample("syncope313-ldap@syncope.apache.org");
            userCR.setPassword(oldCleanPassword);
            userCR.getResources().add(RESOURCE_NAME_LDAP);
            user = createUser(userCR).getEntity();
            assertNotNull(user);
            assertFalse(user.getResources().isEmpty());

            // 2. request to change password only on Syncope and not on LDAP
            String newCleanPassword = "new-security123";
            UserUR userUR = new UserUR();
            userUR.setKey(user.getKey());
            userUR.setPassword(new PasswordPatch.Builder().value(newCleanPassword).build());
            user = updateUser(userUR).getEntity();

            // 3. Check that the Syncope user now has the changed password
            Triple<Map<String, Set<String>>, List<String>, UserTO> self =
                    CLIENT_FACTORY.create(user.getUsername(), newCleanPassword).self();
            assertNotNull(self);

            // 4. Check that the LDAP resource has the old password
            ConnObject connObject =
                    RESOURCE_SERVICE.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), user.getKey());
            assertNotNull(getLdapRemoteObject(
                    connObject.getAttr(Name.NAME).get().getValues().get(0),
                    oldCleanPassword,
                    connObject.getAttr(Name.NAME).get().getValues().get(0)));

            // 5. Update the LDAP Connector to retrieve passwords
            ResourceTO ldapResource = RESOURCE_SERVICE.read(RESOURCE_NAME_LDAP);
            resourceConnector = CONNECTOR_SERVICE.read(
                    ldapResource.getConnector(), Locale.ENGLISH.getLanguage());
            property = resourceConnector.getConf("retrievePasswordsWithSearch").get();
            property.getValues().clear();
            property.getValues().add(Boolean.TRUE);
            CONNECTOR_SERVICE.update(resourceConnector);

            // 6. Pull the user from the resource
            ImplementationTO pullActions = new ImplementationTO();
            pullActions.setKey(LDAPPasswordPullActions.class.getSimpleName());
            pullActions.setEngine(ImplementationEngine.JAVA);
            pullActions.setType(IdMImplementationType.PULL_ACTIONS);
            pullActions.setBody(LDAPPasswordPullActions.class.getName());
            Response response = IMPLEMENTATION_SERVICE.create(pullActions);
            pullActions = IMPLEMENTATION_SERVICE.read(
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
            Response taskResponse = TASK_SERVICE.create(TaskType.PULL, pullTask);

            pullTask = getObject(taskResponse.getLocation(), TaskService.class, PullTaskTO.class);
            assertNotNull(pullTask);

            ExecTO execution = execProvisioningTask(
                    TASK_SERVICE, TaskType.PULL, pullTask.getKey(), MAX_WAIT_SECONDS, false);
            assertEquals(ExecStatus.SUCCESS, ExecStatus.valueOf(execution.getStatus()));

            // 7. Test the pulled user
            self = CLIENT_FACTORY.create(user.getUsername(), oldCleanPassword).self();
            assertNotNull(self);
        } catch (Exception e) {
            fail(e::getMessage);
        } finally {
            // Delete PullTask + user + reset the connector
            if (pullTask != null) {
                TASK_SERVICE.delete(TaskType.PULL, pullTask.getKey());
            }

            if (resourceConnector != null && property != null) {
                property.getValues().clear();
                property.getValues().add(Boolean.FALSE);
                CONNECTOR_SERVICE.update(resourceConnector);
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
            GroupCR propagationGroupCR = GroupITCase.getBasicSample("SYNCOPE1062");
            propagationGroupCR.getResources().add(RESOURCE_NAME_DBPULL);
            propagationGroup = createGroup(propagationGroupCR).getEntity();

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
            template.getMemberships().add(new MembershipTO.Builder(propagationGroup.getKey()).build());
            template.getPlainAttrs().add(attr("firstname", "'fixed'"));
            pullTask.getTemplates().put(AnyTypeKind.USER.name(), template);

            Response taskResponse = TASK_SERVICE.create(TaskType.PULL, pullTask);
            pullTask = getObject(taskResponse.getLocation(), TaskService.class, PullTaskTO.class);
            assertNotNull(pullTask);
            assertFalse(pullTask.getTemplates().isEmpty());

            // 3. exec the pull task
            ExecTO execution = execProvisioningTask(
                    TASK_SERVICE, TaskType.PULL, pullTask.getKey(), MAX_WAIT_SECONDS, false);
            assertEquals(ExecStatus.SUCCESS, ExecStatus.valueOf(execution.getStatus()));

            // the user is successfully pulled...
            user = USER_SERVICE.read("pullFromLDAP");
            assertNotNull(user);
            assertEquals("pullFromLDAP@syncope.apache.org", user.getPlainAttr("email").get().getValues().get(0));

            group = GROUP_SERVICE.read("testLDAPGroup");
            assertNotNull(group);

            ConnObject connObject =
                    RESOURCE_SERVICE.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), user.getKey());
            assertNotNull(connObject);
            assertEquals("pullFromLDAP@syncope.apache.org", connObject.getAttr("mail").get().getValues().get(0));
            Attr userDn = connObject.getAttr(Name.NAME).get();
            assertNotNull(userDn);
            assertEquals(1, userDn.getValues().size());
            assertNotNull(
                    getLdapRemoteObject(RESOURCE_LDAP_ADMIN_DN, RESOURCE_LDAP_ADMIN_PWD, userDn.getValues().get(0)));

            // ...and propagated
            PagedResult<TaskTO> propagationTasks = TASK_SERVICE.search(new TaskQuery.Builder(TaskType.PROPAGATION).
                    resource(RESOURCE_NAME_DBPULL).
                    anyTypeKind(AnyTypeKind.USER).entityKey(user.getKey()).build());
            assertEquals(1, propagationTasks.getSize());

            // 4. update the user on the external resource
            updateLdapRemoteObject(RESOURCE_LDAP_ADMIN_DN, RESOURCE_LDAP_ADMIN_PWD,
                    userDn.getValues().get(0), Map.of("mail", "pullFromLDAP2@syncope.apache.org"));

            connObject = RESOURCE_SERVICE.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), user.getKey());
            assertNotNull(connObject);
            assertEquals("pullFromLDAP2@syncope.apache.org", connObject.getAttr("mail").get().getValues().get(0));

            // 5. exec the pull task again
            execution = execProvisioningTask(TASK_SERVICE, TaskType.PULL, pullTask.getKey(), MAX_WAIT_SECONDS, false);
            assertEquals(ExecStatus.SUCCESS, ExecStatus.valueOf(execution.getStatus()));

            // the internal is updated...
            user = USER_SERVICE.read("pullFromLDAP");
            assertNotNull(user);
            assertEquals("pullFromLDAP2@syncope.apache.org", user.getPlainAttr("email").get().getValues().get(0));

            // ...and propagated
            propagationTasks = TASK_SERVICE.search(new TaskQuery.Builder(TaskType.PROPAGATION).
                    resource(RESOURCE_NAME_DBPULL).
                    anyTypeKind(AnyTypeKind.USER).entityKey(user.getKey()).build());
            assertEquals(2, propagationTasks.getSize());
        } catch (Exception e) {
            LOG.error("Unexpected during issueSYNCOPE1062()", e);
            fail(e::getMessage);
        } finally {
            if (pullTask != null) {
                TASK_SERVICE.delete(TaskType.PULL, pullTask.getKey());
            }

            if (propagationGroup != null) {
                GROUP_SERVICE.delete(propagationGroup.getKey());
            }

            if (group != null) {
                GROUP_SERVICE.delete(group.getKey());
            }
            if (user != null) {
                USER_SERVICE.delete(user.getKey());
            }
        }
    }

    @Test
    public void issueSYNCOPE1656() throws NamingException {
        // preliminary create a new LDAP object
        createLdapRemoteObject(RESOURCE_LDAP_ADMIN_DN, RESOURCE_LDAP_ADMIN_PWD, prepareLdapAttributes(
                "pullFromLDAP_issue1656",
                "pullFromLDAP_issue1656@syncope.apache.org",
                "Active",
                "pullFromLDAP_issue1656",
                "Surname",
                "5BAA61E4C9B93F3F0682250B6CF8331B7EE68FD8",
                "odd",
                "password"));
        try {
            // 1. Pull from resource-ldap
            PullTaskTO pullTask = new PullTaskTO();
            pullTask.setDestinationRealm(SyncopeConstants.ROOT_REALM);
            pullTask.setName("SYNCOPE1656");
            pullTask.setActive(true);
            pullTask.setPerformCreate(true);
            pullTask.setPerformUpdate(true);
            pullTask.setRemediation(true);
            pullTask.setPullMode(PullMode.FULL_RECONCILIATION);
            pullTask.setResource(RESOURCE_NAME_LDAP);

            Response taskResponse = TASK_SERVICE.create(TaskType.PULL, pullTask);
            pullTask = getObject(taskResponse.getLocation(), TaskService.class, PullTaskTO.class);
            assertNotNull(pullTask);

            ExecTO execution = execProvisioningTask(
                    TASK_SERVICE, TaskType.PULL, pullTask.getKey(), MAX_WAIT_SECONDS, false);
            assertEquals(ExecStatus.SUCCESS, ExecStatus.valueOf(execution.getStatus()));

            UserTO pullFromLDAP4issue1656 = USER_SERVICE.read("pullFromLDAP_issue1656");
            assertEquals("pullFromLDAP_issue1656@syncope.apache.org",
                    pullFromLDAP4issue1656.getPlainAttr("email").get().getValues().get(0));
            // 2. Edit mail attribute directly on the resource in order to have a not valid email
            ConnObject connObject = RESOURCE_SERVICE.readConnObject(
                    RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), pullFromLDAP4issue1656.getKey());
            assertNotNull(connObject);
            assertEquals("pullFromLDAP_issue1656@syncope.apache.org",
                    connObject.getAttr("mail").get().getValues().get(0));
            Attr userDn = connObject.getAttr(Name.NAME).get();
            assertNotNull(userDn);
            assertEquals(1, userDn.getValues().size());
            updateLdapRemoteObject(RESOURCE_LDAP_ADMIN_DN, RESOURCE_LDAP_ADMIN_PWD,
                    userDn.getValues().get(0), Collections.singletonMap("mail", "pullFromLDAP_issue1656@"));
            // 3. Pull again from resource-ldap
            execution = execProvisioningTask(TASK_SERVICE, TaskType.PULL, pullTask.getKey(), MAX_WAIT_SECONDS, false);
            assertEquals(ExecStatus.SUCCESS, ExecStatus.valueOf(execution.getStatus()));
            assertTrue(execution.getMessage().contains("UPDATE FAILURE"));
            pullFromLDAP4issue1656 = USER_SERVICE.read("pullFromLDAP_issue1656");
            assertEquals("pullFromLDAP_issue1656@syncope.apache.org",
                    pullFromLDAP4issue1656.getPlainAttr("email").get().getValues().get(0));
            String pullFromLDAP4issue1656Key = pullFromLDAP4issue1656.getKey();
            // a remediation for pullFromLDAP_issue1656 email should have been created
            PagedResult<RemediationTO> remediations =
                    REMEDIATION_SERVICE.list(new RemediationQuery.Builder().page(1).size(10).build());
            assertTrue(remediations.getResult().stream().filter(r -> r.getAnyURPayload() != null).anyMatch(
                    r -> pullFromLDAP4issue1656Key.equals(r.getAnyURPayload().getKey())));
            assertTrue(remediations.getResult().stream().anyMatch(r -> StringUtils.contains(r.getError(),
                    "\"pullFromLDAP_issue1656@\" is not a valid email address")));
        } finally {
            // remove test entity
            removeLdapRemoteObject(RESOURCE_LDAP_ADMIN_DN, RESOURCE_LDAP_ADMIN_PWD,
                    "uid=pullFromLDAP_issue1656,ou=People,o=isp");
            cleanUpRemediations();
        }
    }

    private void cleanUpRemediations() {
        REMEDIATION_SERVICE.list(new RemediationQuery.Builder().page(1).size(100).build()).getResult().forEach(
                r -> REMEDIATION_SERVICE.delete(r.getKey()));
    }

    private Pair<String, Set<Attribute>> prepareLdapAttributes(
            final String uid,
            final String email,
            final String description,
            final String givenName,
            final String sn,
            final String registeredAddress,
            final String title,
            final String password) {
        String entryDn = "uid=" + uid + ",ou=People,o=isp";
        Set<Attribute> attributes = new HashSet<>();

        attributes.add(new BasicAttribute("description", description));
        attributes.add(new BasicAttribute("givenName", givenName));
        attributes.add(new BasicAttribute("mail", email));
        attributes.add(new BasicAttribute("sn", sn));
        attributes.add(new BasicAttribute("cn", uid));
        attributes.add(new BasicAttribute("uid", uid));
        attributes.add(new BasicAttribute("registeredaddress", registeredAddress));
        attributes.add(new BasicAttribute("title", title));
        attributes.add(new BasicAttribute("userpassword", password));

        Attribute oc = new BasicAttribute("objectClass");
        oc.add("top");
        oc.add("person");
        oc.add("inetOrgPerson");
        oc.add("organizationalPerson");
        attributes.add(oc);

        return Pair.of(entryDn, attributes);
    }
}

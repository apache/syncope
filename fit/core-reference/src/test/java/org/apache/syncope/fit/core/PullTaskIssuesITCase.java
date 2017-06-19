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
import static org.junit.Assert.fail;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.policy.PullPolicyTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.core.spring.security.Encryptor;
import org.apache.syncope.core.provisioning.java.pushpull.DBPasswordPullActions;
import org.apache.syncope.core.provisioning.java.pushpull.LDAPPasswordPullActions;
import org.apache.syncope.fit.core.reference.TestPullRule;
import org.identityconnectors.framework.common.objects.Name;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:testJDBCEnv.xml" })
public class PullTaskIssuesITCase extends AbstractTaskITCase {

    @Autowired
    private DataSource testDataSource;

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
            PullTaskTO task = taskService.read("81d88f73-d474-4450-9031-605daa4e313f", true);
            assertNotNull(task);

            task.getTemplates().put(AnyTypeKind.USER.name(), template);

            taskService.update(task);
            PullTaskTO actual = taskService.read(task.getKey(), true);
            assertNotNull(actual);
            assertEquals(task.getKey(), actual.getKey());
            assertFalse(actual.getTemplates().get(AnyTypeKind.USER.name()).getResources().isEmpty());
            assertFalse(((UserTO) actual.getTemplates().get(AnyTypeKind.USER.name())).getMemberships().isEmpty());

            ExecTO execution = execProvisioningTask(taskService, actual.getKey(), 50, false);
            assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(execution.getStatus()));

            userTO = userService.read("testuser2");
            assertNotNull(userTO);
            assertEquals("testuser2@syncope.apache.org", userTO.getPlainAttr("userId").getValues().get(0));
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

        // 1. populate the external table
        jdbcTemplate.execute("INSERT INTO testpull VALUES"
                + "('a54b3794-b231-47be-b24a-11e1a42949f6', 'issuesyncope230', 'Surname', 'syncope230@syncope.apache.org', NULL)");

        // 2. execute PullTask for resource-db-pull (table TESTPULL on external H2)
        execProvisioningTask(taskService, "7c2242f4-14af-4ab5-af31-cdae23783655", 50, false);

        // 3. read e-mail address for user created by the PullTask first execution
        UserTO userTO = userService.read("issuesyncope230");
        assertNotNull(userTO);
        String email = userTO.getPlainAttr("email").getValues().iterator().next();
        assertNotNull(email);

        // 4. update TESTPULL on external H2 by changing e-mail address
        jdbcTemplate.execute("UPDATE TESTPULL SET email='updatedSYNCOPE230@syncope.apache.org'");

        // 5. re-execute the PullTask
        execProvisioningTask(taskService, "7c2242f4-14af-4ab5-af31-cdae23783655", 50, false);

        // 6. verify that the e-mail was updated
        userTO = userService.read("issuesyncope230");
        assertNotNull(userTO);
        email = userTO.getPlainAttr("email").getValues().iterator().next();
        assertNotNull(email);
        assertEquals("updatedSYNCOPE230@syncope.apache.org", email);
    }

    @Test
    public void issueSYNCOPE258() {
        // -----------------------------
        // Add a custom correlation rule
        // -----------------------------
        PullPolicyTO policyTO = policyService.read("9454b0d7-2610-400a-be82-fc23cf553dd6");
        policyTO.getSpecification().getCorrelationRules().put(AnyTypeKind.USER.name(), TestPullRule.class.getName());
        policyService.update(policyTO);
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

        Response response = taskService.create(task);
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

        execProvisioningTask(taskService, task.getKey(), 50, false);

        PullTaskTO executed = taskService.read(task.getKey(), true);
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
        userTO = result.getEntity();
        try {
            assertNotNull(userTO);
            assertEquals(1, result.getPropagationStatuses().size());
            assertEquals(PropagationTaskExecStatus.SUCCESS, result.getPropagationStatuses().get(0).getStatus());

            ExecTO taskExecTO = execProvisioningTask(
                    taskService, "986867e2-993b-430e-8feb-aa9abb4c1dcd", 50, false);

            assertNotNull(taskExecTO.getStatus());
            assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(taskExecTO.getStatus()));

            userTO = userService.read(userTO.getKey());
            assertNotNull(userTO);
            assertNotNull(userTO.getPlainAttr("firstname").getValues().get(0));
        } finally {
            removeTestUsers();
        }
    }

    @Test
    public void issueSYNCOPE307() {
        UserTO userTO = UserITCase.getUniqueSampleTO("s307@apache.org");
        userTO.setUsername("test0");
        userTO.getPlainAttr("firstname").getValues().clear();
        userTO.getPlainAttr("firstname").getValues().add("nome0");
        userTO.getAuxClasses().add("csv");

        AttrTO csvuserid = new AttrTO();
        csvuserid.setSchema("csvuserid");
        userTO.getDerAttrs().add(csvuserid);

        userTO.getResources().clear();
        userTO.getResources().add(RESOURCE_NAME_WS2);

        userTO = createUser(userTO).getEntity();
        assertNotNull(userTO);

        userTO = userService.read(userTO.getKey());
        assertTrue(userTO.getVirAttrs().isEmpty());

        // Update pull task
        PullTaskTO task = taskService.read("38abbf9e-a1a3-40a1-a15f-7d0ac02f47f1", true);
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
        assertEquals("virtualvalue", userTO.getVirAttr("virtualdata").getValues().get(0));

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
        PullTaskTO pullTask = new PullTaskTO();
        pullTask.setDestinationRealm(SyncopeConstants.ROOT_REALM);
        pullTask.setName("DB Pull Task");
        pullTask.setActive(true);
        pullTask.setPerformCreate(true);
        pullTask.setPerformUpdate(true);
        pullTask.setPullMode(PullMode.FULL_RECONCILIATION);
        pullTask.setResource(RESOURCE_NAME_TESTDB);
        pullTask.getActionsClassNames().add(DBPasswordPullActions.class.getName());
        Response taskResponse = taskService.create(pullTask);

        PullTaskTO actual = getObject(taskResponse.getLocation(), TaskService.class, PullTaskTO.class);
        assertNotNull(actual);

        pullTask = taskService.read(actual.getKey(), true);
        assertNotNull(pullTask);
        assertEquals(actual.getKey(), pullTask.getKey());
        assertEquals(actual.getJobDelegateClassName(), pullTask.getJobDelegateClassName());

        ExecTO execution = execProvisioningTask(taskService, pullTask.getKey(), 50, false);
        assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(execution.getStatus()));

        // 5. Test the pulled user
        Pair<Map<String, Set<String>>, UserTO> self = clientFactory.create(user.getUsername(), newCleanPassword).self();
        assertNotNull(self);

        // 6. Delete PullTask + user
        taskService.delete(pullTask.getKey());
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
                    connObject.getAttr(Name.NAME).getValues().get(0),
                    oldCleanPassword,
                    connObject.getAttr(Name.NAME).getValues().get(0)));

            // 5. Update the LDAP Connector to retrieve passwords
            ResourceTO ldapResource = resourceService.read(RESOURCE_NAME_LDAP);
            resourceConnector = connectorService.read(
                    ldapResource.getConnector(), Locale.ENGLISH.getLanguage());
            property = resourceConnector.getConfMap().get("retrievePasswordsWithSearch");
            property.getValues().clear();
            property.getValues().add(Boolean.TRUE);
            connectorService.update(resourceConnector);

            // 6. Pull the user from the resource
            pullTask = new PullTaskTO();
            pullTask.setDestinationRealm(SyncopeConstants.ROOT_REALM);
            pullTask.setName("LDAP Pull Task");
            pullTask.setActive(true);
            pullTask.setPerformCreate(true);
            pullTask.setPerformUpdate(true);
            pullTask.setPullMode(PullMode.FULL_RECONCILIATION);
            pullTask.setResource(RESOURCE_NAME_LDAP);
            pullTask.getActionsClassNames().add(LDAPPasswordPullActions.class.getName());
            Response taskResponse = taskService.create(pullTask);

            pullTask = getObject(taskResponse.getLocation(), TaskService.class, PullTaskTO.class);
            assertNotNull(pullTask);

            ExecTO execution = execProvisioningTask(taskService, pullTask.getKey(), 50, false);
            assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(execution.getStatus()));

            // 7. Test the pulled user
            self = clientFactory.create(user.getUsername(), oldCleanPassword).self();
            assertNotNull(self);
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            // Delete PullTask + user + reset the connector
            if (pullTask != null) {
                taskService.delete(pullTask.getKey());
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

            Response taskResponse = taskService.create(pullTask);
            pullTask = getObject(taskResponse.getLocation(), TaskService.class, PullTaskTO.class);
            assertNotNull(pullTask);
            assertFalse(pullTask.getTemplates().isEmpty());

            // 3. exec the pull task
            ExecTO execution = execProvisioningTask(taskService, pullTask.getKey(), 50, false);
            assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(execution.getStatus()));

            // the user is successfully pulled...
            user = userService.read("pullFromLDAP");
            assertNotNull(user);
            assertEquals("pullFromLDAP@syncope.apache.org", user.getPlainAttr("email").getValues().get(0));

            group = groupService.read("testLDAPGroup");
            assertNotNull(group);

            ConnObjectTO connObject =
                    resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), user.getKey());
            assertNotNull(connObject);
            assertEquals("pullFromLDAP@syncope.apache.org", connObject.getAttr("mail").getValues().get(0));
            AttrTO userDn = connObject.getAttr(Name.NAME);
            assertNotNull(userDn);
            assertEquals(1, userDn.getValues().size());
            assertNotNull(
                    getLdapRemoteObject(RESOURCE_LDAP_ADMIN_DN, RESOURCE_LDAP_ADMIN_PWD, userDn.getValues().get(0)));

            // ...and propagated
            PagedResult<AbstractTaskTO> propagationTasks = taskService.list(new TaskQuery.Builder(TaskType.PROPAGATION).
                    resource(RESOURCE_NAME_DBPULL).
                    anyTypeKind(AnyTypeKind.USER).entityKey(user.getKey()).build());
            assertEquals(1, propagationTasks.getSize());

            // 4. update the user on the external resource
            updateLdapRemoteObject(RESOURCE_LDAP_ADMIN_DN, RESOURCE_LDAP_ADMIN_PWD,
                    userDn.getValues().get(0), Pair.of("mail", "pullFromLDAP2@syncope.apache.org"));

            connObject = resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.USER.name(), user.getKey());
            assertNotNull(connObject);
            assertEquals("pullFromLDAP2@syncope.apache.org", connObject.getAttr("mail").getValues().get(0));

            // 5. exec the pull task again
            execution = execProvisioningTask(taskService, pullTask.getKey(), 50, false);
            assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(execution.getStatus()));

            // the internal is updated...
            user = userService.read("pullFromLDAP");
            assertNotNull(user);
            assertEquals("pullFromLDAP2@syncope.apache.org", user.getPlainAttr("email").getValues().get(0));

            // ...and propagated
            propagationTasks = taskService.list(new TaskQuery.Builder(TaskType.PROPAGATION).
                    resource(RESOURCE_NAME_DBPULL).
                    anyTypeKind(AnyTypeKind.USER).entityKey(user.getKey()).build());
            assertEquals(2, propagationTasks.getSize());
        } catch (Exception e) {
            LOG.error("Unexpected during issueSYNCOPE1062()", e);
            fail(e.getMessage());
        } finally {
            if (pullTask != null) {
                taskService.delete(pullTask.getKey());
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

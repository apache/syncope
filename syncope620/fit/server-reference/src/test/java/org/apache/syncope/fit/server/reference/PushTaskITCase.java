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

import static org.apache.syncope.fit.server.reference.AbstractITCase.taskService;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.MappingTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.TaskExecTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.SubjectType;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.jdbc.core.JdbcTemplate;

@FixMethodOrder(MethodSorters.JVM)
public class PushTaskITCase extends AbstractTaskITCase {

    @Test
    public void getPushActionsClasses() {
        List<String> actions = syncopeService.info().getPushActions();
        assertNotNull(actions);
    }

    @Test
    public void read() {
        PushTaskTO pushTaskTO = taskService.<PushTaskTO>read(17L);
        assertEquals(UnmatchingRule.ASSIGN, pushTaskTO.getUnmatchingRule());
        assertEquals(MatchingRule.UPDATE, pushTaskTO.getMatchingRule());
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

        roleTO.getRPlainAttrTemplates().add(newPlainSchemaTO.getKey());
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

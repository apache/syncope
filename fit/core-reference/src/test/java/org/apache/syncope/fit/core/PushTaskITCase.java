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

import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:testJDBCEnv.xml" })
public class PushTaskITCase extends AbstractTaskITCase {

    @Autowired
    private DataSource testDataSource;

    @Test
    public void getPushActionsClasses() {
        Set<String> actions = syncopeService.platform().getPushActions();
        assertNotNull(actions);
    }

    @Test
    public void read() {
        PushTaskTO pushTaskTO = taskService.<PushTaskTO>read(
                "0bc11a19-6454-45c2-a4e3-ceef84e5d79b", true);
        assertEquals(UnmatchingRule.ASSIGN, pushTaskTO.getUnmatchingRule());
        assertEquals(MatchingRule.UPDATE, pushTaskTO.getMatchingRule());
    }

    @Test
    public void list() {
        PagedResult<PushTaskTO> tasks = taskService.list(new TaskQuery.Builder(TaskType.PUSH).build());
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
        task.getFilters().put(AnyTypeKind.USER.name(),
                SyncopeClient.getUserSearchConditionBuilder().hasNotResources(RESOURCE_NAME_TESTDB2).query());
        task.getFilters().put(AnyTypeKind.GROUP.name(),
                SyncopeClient.getGroupSearchConditionBuilder().isNotNull("cool").query());
        task.setMatchingRule(MatchingRule.LINK);

        final Response response = taskService.create(task);
        final PushTaskTO actual = getObject(response.getLocation(), TaskService.class, PushTaskTO.class);
        assertNotNull(actual);

        task = taskService.read(actual.getKey(), true);
        assertNotNull(task);
        assertEquals(task.getKey(), actual.getKey());
        assertEquals(task.getJobDelegateClassName(), actual.getJobDelegateClassName());
        assertEquals(task.getFilters().get(AnyTypeKind.USER.name()),
                actual.getFilters().get(AnyTypeKind.USER.name()));
        assertEquals(task.getFilters().get(AnyTypeKind.GROUP.name()),
                actual.getFilters().get(AnyTypeKind.GROUP.name()));
        assertEquals(UnmatchingRule.ASSIGN, actual.getUnmatchingRule());
        assertEquals(MatchingRule.LINK, actual.getMatchingRule());
    }

    @Test
    public void pushMatchingUnmatchingGroups() {
        assertFalse(groupService.read("29f96485-729e-4d31-88a1-6fc60e4677f3").
                getResources().contains(RESOURCE_NAME_LDAP));

        execProvisioningTask(taskService, "fd905ba5-9d56-4f51-83e2-859096a67b75", 50, false);

        assertNotNull(resourceService.readConnObject(
                RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), "29f96485-729e-4d31-88a1-6fc60e4677f3"));
        assertTrue(groupService.read("29f96485-729e-4d31-88a1-6fc60e4677f3").
                getResources().contains(RESOURCE_NAME_LDAP));
    }

    @Test
    public void pushUnmatchingUsers() throws Exception {
        assertFalse(userService.read("74cd8ece-715a-44a4-a736-e17b46c4e7e6").
                getResources().contains(RESOURCE_NAME_TESTDB2));
        assertFalse(userService.read("b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee").
                getResources().contains(RESOURCE_NAME_TESTDB2));
        assertFalse(userService.read("c9b2dec2-00a7-4855-97c0-d854842b4b24").
                getResources().contains(RESOURCE_NAME_TESTDB2));
        assertTrue(userService.read("823074dc-d280-436d-a7dd-07399fae48ec").
                getResources().contains(RESOURCE_NAME_TESTDB2));

        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        assertEquals(0, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='puccini'").size());

        // ------------------------------------------
        // Unmatching --> Assign --> dryRuyn
        // ------------------------------------------
        execProvisioningTask(taskService, "af558be4-9d2f-4359-bf85-a554e6e90be1", 50, true);
        assertEquals(0, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='vivaldi'").size());
        assertFalse(userService.read("b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee").
                getResources().contains(RESOURCE_NAME_TESTDB2));
        // ------------------------------------------

        Set<String> pushTaskKeys = new HashSet<>();
        pushTaskKeys.add("af558be4-9d2f-4359-bf85-a554e6e90be1");
        pushTaskKeys.add("97f327b6-2eff-4d35-85e8-d581baaab855");
        pushTaskKeys.add("03aa2a04-4881-4573-9117-753f81b04865");
        pushTaskKeys.add("5e5f7c7e-9de7-4c6a-99f1-4df1af959807");
        execProvisioningTasks(taskService, pushTaskKeys, 50, false);

        // ------------------------------------------
        // Unatching --> Ignore
        // ------------------------------------------
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='verdi'").size());
        assertFalse(userService.read("74cd8ece-715a-44a4-a736-e17b46c4e7e6").
                getResources().contains(RESOURCE_NAME_TESTDB2));
        // ------------------------------------------

        // ------------------------------------------
        // Unmatching --> Assign
        // ------------------------------------------
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='vivaldi'").size());
        assertTrue(userService.read("b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee").
                getResources().contains(RESOURCE_NAME_TESTDB2));
        jdbcTemplate.execute("DELETE FROM test2 WHERE ID='vivaldi'");
        // ------------------------------------------

        // ------------------------------------------
        // Unmatching --> Provision
        // ------------------------------------------
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='bellini'").size());
        assertFalse(userService.read("c9b2dec2-00a7-4855-97c0-d854842b4b24").
                getResources().contains(RESOURCE_NAME_TESTDB2));
        jdbcTemplate.execute("DELETE FROM test2 WHERE ID='bellini'");
        // ------------------------------------------

        // ------------------------------------------
        // Unmatching --> Unlink
        // ------------------------------------------
        assertEquals(0, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='puccini'").size());
        assertFalse(userService.read("823074dc-d280-436d-a7dd-07399fae48ec").
                getResources().contains(RESOURCE_NAME_TESTDB2));
        // ------------------------------------------
    }

    @Test
    public void pushMatchingUser() throws Exception {
        assertTrue(userService.read("1417acbe-cbf6-4277-9372-e75e04f97000").
                getResources().contains(RESOURCE_NAME_TESTDB2));
        assertFalse(userService.read("74cd8ece-715a-44a4-a736-e17b46c4e7e6").
                getResources().contains(RESOURCE_NAME_TESTDB2));

        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='verdi'").size());
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='rossini'").size());

        // ------------------------------------------
        // Matching --> Deprovision --> dryRuyn
        // ------------------------------------------
        execProvisioningTask(taskService, "c46edc3a-a18b-4af2-b707-f4a415507496", 50, true);
        assertTrue(userService.read("1417acbe-cbf6-4277-9372-e75e04f97000").
                getResources().contains(RESOURCE_NAME_TESTDB2));
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='rossini'").size());
        // ------------------------------------------

        Set<String> pushTaskKeys = new HashSet<>();
        pushTaskKeys.add("ec674143-480a-4816-98ad-b61fa090821e");
        pushTaskKeys.add("c46edc3a-a18b-4af2-b707-f4a415507496");
        pushTaskKeys.add("5e5f7c7e-9de7-4c6a-99f1-4df1af959807");

        execProvisioningTasks(taskService, pushTaskKeys, 50, false);

        // ------------------------------------------
        // Matching --> Deprovision && Ignore
        // ------------------------------------------
        assertFalse(userService.read("74cd8ece-715a-44a4-a736-e17b46c4e7e6").
                getResources().contains(RESOURCE_NAME_TESTDB2));
        // DELETE Capability not available ....
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='verdi'").size());
        // ------------------------------------------

        // ------------------------------------------
        // Matching --> Unassign
        // ------------------------------------------
        assertFalse(userService.read("1417acbe-cbf6-4277-9372-e75e04f97000").
                getResources().contains(RESOURCE_NAME_TESTDB2));
        // DELETE Capability not available ....
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='rossini'").size());
        // ------------------------------------------

        // ------------------------------------------
        // Matching --> Link
        // ------------------------------------------
        execProvisioningTask(taskService, "51318433-cce4-4f71-8f45-9534b6c9c819", 50, false);
        assertTrue(userService.read("74cd8ece-715a-44a4-a736-e17b46c4e7e6").
                getResources().contains(RESOURCE_NAME_TESTDB2));
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='verdi'").size());
        // ------------------------------------------

        pushTaskKeys.clear();
        pushTaskKeys.add("24b1be9c-7e3b-443a-86c9-798ebce5eaf2");
        pushTaskKeys.add("375c7b7f-9e3a-4833-88c9-b7787b0a69f2");

        execProvisioningTasks(taskService, pushTaskKeys, 50, false);

        // ------------------------------------------
        // Matching --> Unlink && Update
        // ------------------------------------------
        assertFalse(userService.read("74cd8ece-715a-44a4-a736-e17b46c4e7e6").
                getResources().contains(RESOURCE_NAME_TESTDB2));
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='verdi'").size());
        // ------------------------------------------
    }

    @Test
    public void orgUnit() {
        assertNull(getLdapRemoteObject(RESOURCE_LDAP_ADMIN_DN, RESOURCE_LDAP_ADMIN_PWD, "ou=odd,o=isp"));
        assertNull(getLdapRemoteObject(RESOURCE_LDAP_ADMIN_DN, RESOURCE_LDAP_ADMIN_PWD, "ou=even,o=isp"));
        assertNull(getLdapRemoteObject(RESOURCE_LDAP_ADMIN_DN, RESOURCE_LDAP_ADMIN_PWD, "ou=two,ou=even,o=isp"));

        // 1. create task for pulling org units
        PushTaskTO task = new PushTaskTO();
        task.setName("For orgUnit");
        task.setActive(true);
        task.setResource(RESOURCE_NAME_LDAP_ORGUNIT);
        task.setPerformCreate(true);
        task.setPerformDelete(true);
        task.setPerformUpdate(true);

        Response response = taskService.create(task);
        PushTaskTO pushTask = getObject(response.getLocation(), TaskService.class, PushTaskTO.class);
        assertNotNull(pushTask);

        ExecTO exec = execProvisioningTask(taskService, pushTask.getKey(), 50, false);
        assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(exec.getStatus()));

        // 2. check
        assertNotNull(getLdapRemoteObject(RESOURCE_LDAP_ADMIN_DN, RESOURCE_LDAP_ADMIN_PWD, "ou=odd,o=isp"));
        assertNotNull(getLdapRemoteObject(RESOURCE_LDAP_ADMIN_DN, RESOURCE_LDAP_ADMIN_PWD, "ou=even,o=isp"));
        assertNotNull(getLdapRemoteObject(RESOURCE_LDAP_ADMIN_DN, RESOURCE_LDAP_ADMIN_PWD, "ou=two,ou=even,o=isp"));
    }
}

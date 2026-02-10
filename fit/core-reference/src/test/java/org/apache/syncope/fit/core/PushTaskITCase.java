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

import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.Mapping;
import org.apache.syncope.common.lib.to.NotificationTO;
import org.apache.syncope.common.lib.to.NotificationTaskTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.ReconStatus;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.lib.types.ThreadPoolSettings;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.ReconQuery;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.NotificationService;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class PushTaskITCase extends AbstractTaskITCase {

    @Test
    public void getPushActionsClasses() {
        Set<String> actions = ANONYMOUS_CLIENT.platform().
                getJavaImplInfo(IdMImplementationType.PUSH_ACTIONS).orElseThrow().getClasses();
        assertNotNull(actions);
    }

    @Test
    public void read() {
        PushTaskTO pushTaskTO = TASK_SERVICE.read(
                TaskType.PUSH, "0bc11a19-6454-45c2-a4e3-ceef84e5d79b", true);
        assertEquals(UnmatchingRule.ASSIGN, pushTaskTO.getUnmatchingRule());
        assertEquals(MatchingRule.UPDATE, pushTaskTO.getMatchingRule());
    }

    @Test
    public void list() {
        PagedResult<PushTaskTO> tasks = TASK_SERVICE.search(new TaskQuery.Builder(TaskType.PUSH).build());
        assertFalse(tasks.getResult().isEmpty());
        tasks.getResult().stream().
                filter((task) -> (!(task instanceof PushTaskTO))).
                forEach(item -> fail("This should not happen"));
    }

    @Test
    public void createPushTask() {
        PushTaskTO task = new PushTaskTO();
        task.setName(getUUIDString());
        task.setResource(RESOURCE_NAME_WS2);
        task.setSourceRealm(SyncopeConstants.ROOT_REALM);
        task.getFilters().put(AnyTypeKind.USER.name(),
                SyncopeClient.getUserSearchConditionBuilder().hasNotResources(RESOURCE_NAME_TESTDB2).query());
        task.getFilters().put(AnyTypeKind.GROUP.name(),
                SyncopeClient.getGroupSearchConditionBuilder().isNotNull("cool").query());
        task.setMatchingRule(MatchingRule.LINK);

        Response response = TASK_SERVICE.create(TaskType.PUSH, task);
        PushTaskTO actual = getObject(response.getLocation(), TaskService.class, PushTaskTO.class);
        assertNotNull(actual);

        task = TASK_SERVICE.read(TaskType.PUSH, actual.getKey(), true);
        assertNotNull(task);
        assertEquals(task.getKey(), actual.getKey());
        assertEquals(task.getJobDelegate(), actual.getJobDelegate());
        assertEquals(task.getFilters().get(AnyTypeKind.USER.name()),
                actual.getFilters().get(AnyTypeKind.USER.name()));
        assertEquals(task.getFilters().get(AnyTypeKind.GROUP.name()),
                actual.getFilters().get(AnyTypeKind.GROUP.name()));
        assertEquals(UnmatchingRule.ASSIGN, actual.getUnmatchingRule());
        assertEquals(MatchingRule.LINK, actual.getMatchingRule());
    }

    @Test
    public void pushMatchingUnmatchingGroups() {
        assertFalse(GROUP_SERVICE.read("29f96485-729e-4d31-88a1-6fc60e4677f3").
                getResources().contains(RESOURCE_NAME_LDAP));

        execSchedTask(
                TASK_SERVICE, TaskType.PUSH, "fd905ba5-9d56-4f51-83e2-859096a67b75", MAX_WAIT_SECONDS, false);

        assertNotNull(RESOURCE_SERVICE.readConnObject(
                RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), "29f96485-729e-4d31-88a1-6fc60e4677f3"));
        assertTrue(GROUP_SERVICE.read("29f96485-729e-4d31-88a1-6fc60e4677f3").
                getResources().contains(RESOURCE_NAME_LDAP));
    }

    @Test
    public void pushUnmatchingUsers() throws Exception {
        assertFalse(USER_SERVICE.read("74cd8ece-715a-44a4-a736-e17b46c4e7e6").
                getResources().contains(RESOURCE_NAME_TESTDB2));
        assertFalse(USER_SERVICE.read("b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee").
                getResources().contains(RESOURCE_NAME_TESTDB2));
        assertFalse(USER_SERVICE.read("c9b2dec2-00a7-4855-97c0-d854842b4b24").
                getResources().contains(RESOURCE_NAME_TESTDB2));
        assertTrue(USER_SERVICE.read("823074dc-d280-436d-a7dd-07399fae48ec").
                getResources().contains(RESOURCE_NAME_TESTDB2));

        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        assertEquals(0, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='puccini'").size());

        // ------------------------------------------
        // Unmatching --> Assign --> dryRuyn
        // ------------------------------------------
        execSchedTask(
                TASK_SERVICE, TaskType.PUSH, "af558be4-9d2f-4359-bf85-a554e6e90be1", MAX_WAIT_SECONDS, true);
        assertEquals(0, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='vivaldi'").size());
        assertFalse(USER_SERVICE.read("b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee").
                getResources().contains(RESOURCE_NAME_TESTDB2));
        // ------------------------------------------

        Set<String> pushTaskKeys = Set.of(
                "af558be4-9d2f-4359-bf85-a554e6e90be1",
                "97f327b6-2eff-4d35-85e8-d581baaab855",
                "03aa2a04-4881-4573-9117-753f81b04865",
                "5e5f7c7e-9de7-4c6a-99f1-4df1af959807");
        execProvisioningTasks(TASK_SERVICE, TaskType.PUSH, pushTaskKeys, MAX_WAIT_SECONDS, false);

        // ------------------------------------------
        // Unatching --> Ignore
        // ------------------------------------------
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='verdi'").size());
        assertFalse(USER_SERVICE.read("74cd8ece-715a-44a4-a736-e17b46c4e7e6").
                getResources().contains(RESOURCE_NAME_TESTDB2));
        // ------------------------------------------

        // ------------------------------------------
        // Unmatching --> Assign
        // ------------------------------------------
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='vivaldi'").size());
        assertTrue(USER_SERVICE.read("b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee").
                getResources().contains(RESOURCE_NAME_TESTDB2));
        jdbcTemplate.execute("DELETE FROM test2 WHERE ID='vivaldi'");
        // ------------------------------------------

        // ------------------------------------------
        // Unmatching --> Provision
        // ------------------------------------------
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='bellini'").size());
        assertFalse(USER_SERVICE.read("c9b2dec2-00a7-4855-97c0-d854842b4b24").
                getResources().contains(RESOURCE_NAME_TESTDB2));
        jdbcTemplate.execute("DELETE FROM test2 WHERE ID='bellini'");
        // ------------------------------------------

        // ------------------------------------------
        // Unmatching --> Unlink
        // ------------------------------------------
        assertEquals(0, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='puccini'").size());
        assertFalse(USER_SERVICE.read("823074dc-d280-436d-a7dd-07399fae48ec").
                getResources().contains(RESOURCE_NAME_TESTDB2));
        // ------------------------------------------
    }

    @Test
    public void pushMatchingUser() throws Exception {
        assertTrue(USER_SERVICE.read("1417acbe-cbf6-4277-9372-e75e04f97000").
                getResources().contains(RESOURCE_NAME_TESTDB2));
        assertFalse(USER_SERVICE.read("74cd8ece-715a-44a4-a736-e17b46c4e7e6").
                getResources().contains(RESOURCE_NAME_TESTDB2));

        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='verdi'").size());
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='rossini'").size());

        // ------------------------------------------
        // Matching --> Deprovision --> dryRuyn
        // ------------------------------------------
        execSchedTask(
                TASK_SERVICE, TaskType.PUSH, "c46edc3a-a18b-4af2-b707-f4a415507496", MAX_WAIT_SECONDS, true);
        assertTrue(USER_SERVICE.read("1417acbe-cbf6-4277-9372-e75e04f97000").
                getResources().contains(RESOURCE_NAME_TESTDB2));
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='rossini'").size());
        // ------------------------------------------

        Set<String> pushTaskKeys = Set.of(
                "ec674143-480a-4816-98ad-b61fa090821e",
                "c46edc3a-a18b-4af2-b707-f4a415507496",
                "5e5f7c7e-9de7-4c6a-99f1-4df1af959807");
        execProvisioningTasks(TASK_SERVICE, TaskType.PUSH, pushTaskKeys, MAX_WAIT_SECONDS, false);

        // ------------------------------------------
        // Matching --> Deprovision && Ignore
        // ------------------------------------------
        assertFalse(USER_SERVICE.read("74cd8ece-715a-44a4-a736-e17b46c4e7e6").
                getResources().contains(RESOURCE_NAME_TESTDB2));
        // DELETE Capability not available ....
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='verdi'").size());
        // ------------------------------------------

        // ------------------------------------------
        // Matching --> Unassign
        // ------------------------------------------
        assertFalse(USER_SERVICE.read("1417acbe-cbf6-4277-9372-e75e04f97000").
                getResources().contains(RESOURCE_NAME_TESTDB2));
        // DELETE Capability not available ....
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='rossini'").size());
        // ------------------------------------------

        // ------------------------------------------
        // Matching --> Link
        // ------------------------------------------
        execSchedTask(
                TASK_SERVICE, TaskType.PUSH, "51318433-cce4-4f71-8f45-9534b6c9c819", MAX_WAIT_SECONDS, false);
        assertTrue(USER_SERVICE.read("74cd8ece-715a-44a4-a736-e17b46c4e7e6").
                getResources().contains(RESOURCE_NAME_TESTDB2));
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='verdi'").size());
        // ------------------------------------------

        pushTaskKeys = Set.of(
                "24b1be9c-7e3b-443a-86c9-798ebce5eaf2",
                "375c7b7f-9e3a-4833-88c9-b7787b0a69f2");
        execProvisioningTasks(TASK_SERVICE, TaskType.PUSH, pushTaskKeys, MAX_WAIT_SECONDS, false);

        // ------------------------------------------
        // Matching --> Unlink && Update
        // ------------------------------------------
        assertFalse(USER_SERVICE.read("74cd8ece-715a-44a4-a736-e17b46c4e7e6").
                getResources().contains(RESOURCE_NAME_TESTDB2));
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='verdi'").size());
        // ------------------------------------------
    }

    @Test
    public void pushPolicy() {
        // 1. set push policy on ldap
        ResourceTO ldap = RESOURCE_SERVICE.read(RESOURCE_NAME_LDAP);
        assertNull(ldap.getPushPolicy());

        try {
            ldap.setPushPolicy("fb6530e5-892d-4f47-a46b-180c5b6c5c83");
            RESOURCE_SERVICE.update(ldap);

            // 2. create push task with sole scope as the user 'vivaldi'
            PushTaskTO sendVivaldi = new PushTaskTO();
            sendVivaldi.setName("Send Vivaldi");
            sendVivaldi.setResource(RESOURCE_NAME_LDAP);
            sendVivaldi.setUnmatchingRule(UnmatchingRule.PROVISION);
            sendVivaldi.setMatchingRule(MatchingRule.UPDATE);
            sendVivaldi.setSourceRealm(SyncopeConstants.ROOT_REALM);
            sendVivaldi.getFilters().put(AnyTypeKind.GROUP.name(), "name==$null");
            sendVivaldi.getFilters().put(AnyTypeKind.USER.name(), "username==vivaldi");
            sendVivaldi.setPerformCreate(true);
            sendVivaldi.setPerformUpdate(true);

            Response response = TASK_SERVICE.create(TaskType.PUSH, sendVivaldi);
            sendVivaldi = getObject(response.getLocation(), TaskService.class, PushTaskTO.class);
            assertNotNull(sendVivaldi);

            // 3. execute push: vivaldi is found on ldap
            execSchedTask(TASK_SERVICE, TaskType.PUSH, sendVivaldi.getKey(), MAX_WAIT_SECONDS, false);

            ReconStatus status = RECONCILIATION_SERVICE.status(
                    new ReconQuery.Builder(AnyTypeKind.USER.name(), RESOURCE_NAME_LDAP).anyKey("vivaldi").build());
            assertNotNull(status.getOnResource());

            // 4. update vivaldi on ldap: reconciliation status does not find it anymore, as remote key was changed
            Map<String, String> attrs = new HashMap<>();
            attrs.put("sn", "VivaldiZ");
            updateLdapRemoteObject("uid=vivaldi,ou=People,o=isp", attrs);

            status = RECONCILIATION_SERVICE.status(
                    new ReconQuery.Builder(AnyTypeKind.USER.name(), RESOURCE_NAME_LDAP).anyKey("vivaldi").build());
            assertNull(status.getOnResource());

            // 5. execute push again: propagation task for CREATE will be generated, but that will fail
            // as task executor is not able any more to identify the entry to UPDATE
            execSchedTask(TASK_SERVICE, TaskType.PUSH, sendVivaldi.getKey(), MAX_WAIT_SECONDS, false);

            status = RECONCILIATION_SERVICE.status(
                    new ReconQuery.Builder(AnyTypeKind.USER.name(), RESOURCE_NAME_LDAP).anyKey("vivaldi").build());
            assertNull(status.getOnResource());
        } finally {
            ldap.setPushPolicy(null);
            RESOURCE_SERVICE.update(ldap);
        }
    }

    @Test
    public void orgUnit() {
        assertNull(getLdapRemoteObject("ou=odd,o=isp"));
        assertNull(getLdapRemoteObject("ou=even,o=isp"));
        assertNull(getLdapRemoteObject("ou=two,ou=even,o=isp"));

        // 1. create task for pulling org units
        PushTaskTO task = new PushTaskTO();
        task.setName("For orgUnit");
        task.setActive(true);
        task.setResource(RESOURCE_NAME_LDAP_ORGUNIT);
        task.setSourceRealm(SyncopeConstants.ROOT_REALM);
        task.setPerformCreate(true);
        task.setPerformDelete(true);
        task.setPerformUpdate(true);

        Response response = TASK_SERVICE.create(TaskType.PUSH, task);
        PushTaskTO pushTask = getObject(response.getLocation(), TaskService.class, PushTaskTO.class);
        assertNotNull(pushTask);

        ExecTO exec = execSchedTask(TASK_SERVICE, TaskType.PUSH, pushTask.getKey(), MAX_WAIT_SECONDS, false);
        assertEquals(ExecStatus.SUCCESS, ExecStatus.valueOf(exec.getStatus()));

        // 2. check
        assertNotNull(getLdapRemoteObject("ou=odd,o=isp"));
        assertNotNull(getLdapRemoteObject("ou=even,o=isp"));
        assertNotNull(getLdapRemoteObject("ou=two,ou=even,o=isp"));
    }

    @Test
    public void concurrentPush() {
        // 1. create new concurrent pull task
        PushTaskTO pushTask = TASK_SERVICE.read(TaskType.PUSH, "97f327b6-2eff-4d35-85e8-d581baaab855", false);
        assertNull(pushTask.getConcurrentSettings());
        pushTask.setKey(null);
        pushTask.setName("Concurrent Export on resource-testdb2");
        pushTask.setDescription("Concurrent Export on resource-testdb2");
        pushTask.getFilters().put(AnyTypeKind.USER.name(), "username!=puccini;username!=vivaldi");

        ThreadPoolSettings tps = new ThreadPoolSettings();
        tps.setPoolSize(3);
        pushTask.setConcurrentSettings(tps);

        Response response = TASK_SERVICE.create(TaskType.PUSH, pushTask);
        String pushTaskKey = response.getHeaderString(RESTHeaders.RESOURCE_KEY);

        // 2. run concurrent pull task
        ExecTO execution = execSchedTask(TASK_SERVICE, TaskType.PUSH, pushTaskKey, MAX_WAIT_SECONDS, false);

        // 3. verify execution status
        assertEquals(ExecStatus.SUCCESS, ExecStatus.valueOf(execution.getStatus()));
    }

    @Test
    public void issueSYNCOPE598() {
        // create a new group schema
        PlainSchemaTO schemaTO = new PlainSchemaTO();
        schemaTO.setKey("LDAPGroupName" + getUUIDString());
        schemaTO.setType(AttrSchemaType.String);
        schemaTO.setMandatoryCondition("true");

        schemaTO = createSchema(SchemaType.PLAIN, schemaTO);
        assertNotNull(schemaTO);

        AnyTypeClassTO typeClass = new AnyTypeClassTO();
        typeClass.setKey("SYNCOPE-598" + getUUIDString());
        typeClass.getPlainSchemas().add(schemaTO.getKey());
        ANY_TYPE_CLASS_SERVICE.create(typeClass);

        // create a new sample group
        GroupCR groupCR = new GroupCR();
        groupCR.setName("all" + getUUIDString());
        groupCR.setRealm("/even");
        groupCR.getAuxClasses().add(typeClass.getKey());

        groupCR.getPlainAttrs().add(attr(schemaTO.getKey(), "all"));

        GroupTO groupTO = createGroup(groupCR).getEntity();
        assertNotNull(groupTO);

        String resourceName = "resource-ldap-grouponly";
        ResourceTO newResourceTO = null;

        try {
            // Create resource ad-hoc
            ResourceTO resourceTO = new ResourceTO();
            resourceTO.setKey(resourceName);
            resourceTO.setConnector("74141a3b-0762-4720-a4aa-fc3e374ef3ef");

            Provision provisionTO = new Provision();
            provisionTO.setAnyType(AnyTypeKind.GROUP.name());
            provisionTO.setObjectClass(ObjectClass.GROUP_NAME);
            provisionTO.getAuxClasses().add(typeClass.getKey());
            resourceTO.getProvisions().add(provisionTO);

            Mapping mapping = new Mapping();
            provisionTO.setMapping(mapping);

            Item item = new Item();
            item.setExtAttrName("cn");
            item.setIntAttrName(schemaTO.getKey());
            item.setConnObjectKey(true);
            item.setPurpose(MappingPurpose.BOTH);
            mapping.setConnObjectKeyItem(item);

            mapping.setConnObjectLink("'cn=' + " + schemaTO.getKey() + " + ',ou=groups,o=isp'");

            Response response = RESOURCE_SERVICE.create(resourceTO);
            newResourceTO = getObject(response.getLocation(), ResourceService.class, ResourceTO.class);
            assertNotNull(newResourceTO);
            assertFalse(newResourceTO.getProvision(AnyTypeKind.USER.name()).isPresent());
            assertNotNull(newResourceTO.getProvision(AnyTypeKind.GROUP.name()).orElseThrow().getMapping());

            // create push task ad-hoc
            PushTaskTO task = new PushTaskTO();
            task.setName("issueSYNCOPE598");
            task.setActive(true);
            task.setResource(resourceName);
            task.setSourceRealm(SyncopeConstants.ROOT_REALM);
            task.setPerformCreate(true);
            task.setPerformDelete(true);
            task.setPerformUpdate(true);
            task.setUnmatchingRule(UnmatchingRule.ASSIGN);
            task.setMatchingRule(MatchingRule.UPDATE);
            task.getFilters().put(AnyTypeKind.GROUP.name(),
                    SyncopeClient.getGroupSearchConditionBuilder().is("name").equalTo(groupTO.getName()).query());

            response = TASK_SERVICE.create(TaskType.PUSH, task);
            PushTaskTO push = getObject(response.getLocation(), TaskService.class, PushTaskTO.class);
            assertNotNull(push);

            // execute the new task
            ExecTO exec = execSchedTask(TASK_SERVICE, TaskType.PUSH, push.getKey(), MAX_WAIT_SECONDS, false);
            assertEquals(ExecStatus.SUCCESS, ExecStatus.valueOf(exec.getStatus()));
        } finally {
            GROUP_SERVICE.delete(groupTO.getKey());
            if (newResourceTO != null) {
                RESOURCE_SERVICE.delete(resourceName);
            }
        }
    }

    @Test
    public void issueSYNCOPE648() {
        // 1. Create Push Task
        PushTaskTO task = new PushTaskTO();
        task.setName("Test create Push");
        task.setActive(true);
        task.setResource(RESOURCE_NAME_LDAP);
        task.setSourceRealm(SyncopeConstants.ROOT_REALM);
        task.getFilters().put(AnyTypeKind.USER.name(),
                SyncopeClient.getUserSearchConditionBuilder().is("username").equalTo("_NO_ONE_").query());
        task.getFilters().put(AnyTypeKind.GROUP.name(),
                SyncopeClient.getGroupSearchConditionBuilder().is("name").equalTo("citizen").query());
        task.setMatchingRule(MatchingRule.IGNORE);
        task.setUnmatchingRule(UnmatchingRule.IGNORE);

        Response response = TASK_SERVICE.create(TaskType.PUSH, task);
        PushTaskTO actual = getObject(response.getLocation(), TaskService.class, PushTaskTO.class);
        assertNotNull(actual);

        // 2. Create notification
        NotificationTO notification = new NotificationTO();
        notification.setTraceLevel(TraceLevel.FAILURES);
        notification.getEvents().add("[PUSH]:[GROUP]:[resource-ldap]:[matchingrule_ignore]:[SUCCESS]");
        notification.getEvents().add("[PUSH]:[GROUP]:[resource-ldap]:[unmatchingrule_ignore]:[SUCCESS]");

        notification.getStaticRecipients().add("issueyncope648@syncope.apache.org");
        notification.setSelfAsRecipient(false);
        notification.setRecipientAttrName("email");

        notification.setSender("syncope648@syncope.apache.org");
        String subject = "Test notification";
        notification.setSubject(subject);
        notification.setTemplate("optin");
        notification.setActive(true);

        Response responseNotification = NOTIFICATION_SERVICE.create(notification);
        notification = getObject(responseNotification.getLocation(), NotificationService.class, NotificationTO.class);
        assertNotNull(notification);

        execSchedTask(TASK_SERVICE, TaskType.PUSH, actual.getKey(), MAX_WAIT_SECONDS, false);

        NotificationTaskTO taskTO = findNotificationTask(notification.getKey(), 50);
        assertNotNull(taskTO);
    }
}

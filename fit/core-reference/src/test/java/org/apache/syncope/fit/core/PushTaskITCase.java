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
import javax.ws.rs.core.Response;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.MappingTO;
import org.apache.syncope.common.lib.to.NotificationTO;
import org.apache.syncope.common.lib.to.NotificationTaskTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.NotificationService;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.jdbc.core.JdbcTemplate;

@FixMethodOrder(MethodSorters.JVM)
public class PushTaskITCase extends AbstractTaskITCase {

    @Test
    public void getPushActionsClasses() {
        Set<String> actions = syncopeService.platform().getPushActions();
        assertNotNull(actions);
    }

    @Test
    public void read() {
        PushTaskTO pushTaskTO = taskService.<PushTaskTO>read(17L, true);
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
        assertFalse(groupService.read(3L).getResources().contains(RESOURCE_NAME_LDAP));

        execProvisioningTask(taskService, 23L, 50, false);

        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), 3L));
        assertTrue(groupService.read(3L).getResources().contains(RESOURCE_NAME_LDAP));

        execProvisioningTask(taskService, 23L, 50, false);

        assertNotNull(resourceService.readConnObject(RESOURCE_NAME_LDAP, AnyTypeKind.GROUP.name(), 3L));
        assertFalse(groupService.read(3L).getResources().contains(RESOURCE_NAME_LDAP));
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
        execProvisioningTask(taskService, 13L, 50, true);
        assertEquals(0, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='vivaldi'").size());
        assertFalse(userService.read(3L).getResources().contains(RESOURCE_NAME_TESTDB2));
        // ------------------------------------------

        Set<Long> pushTaskIds = new HashSet<>();
        pushTaskIds.add(13L);
        pushTaskIds.add(14L);
        pushTaskIds.add(15L);
        pushTaskIds.add(16L);
        execProvisioningTasks(taskService, pushTaskIds, 50, false);

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

        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='verdi'").size());
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='rossini'").size());

        // ------------------------------------------
        // Matching --> Deprovision --> dryRuyn
        // ------------------------------------------
        execProvisioningTask(taskService, 19L, 50, true);
        assertTrue(userService.read(1L).getResources().contains(RESOURCE_NAME_TESTDB2));
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='rossini'").size());
        // ------------------------------------------

        Set<Long> pushTaskKeys = new HashSet<>();
        pushTaskKeys.add(18L);
        pushTaskKeys.add(19L);
        pushTaskKeys.add(16L);

        execProvisioningTasks(taskService, pushTaskKeys, 50, false);

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
        execProvisioningTask(taskService, 20L, 50, false);
        assertTrue(userService.read(2L).getResources().contains(RESOURCE_NAME_TESTDB2));
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='verdi'").size());
        // ------------------------------------------

        pushTaskKeys.clear();
        pushTaskKeys.add(21L);
        pushTaskKeys.add(22L);

        execProvisioningTasks(taskService, pushTaskKeys, 50, false);

        // ------------------------------------------
        // Matching --> Unlink && Update
        // ------------------------------------------
        assertFalse(userService.read(2L).getResources().contains(RESOURCE_NAME_TESTDB2));
        assertEquals(1, jdbcTemplate.queryForList("SELECT ID FROM test2 WHERE ID='verdi'").size());
        // ------------------------------------------
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
        anyTypeClassService.create(typeClass);

        // create a new sample group
        GroupTO groupTO = new GroupTO();
        groupTO.setName("all" + getUUIDString());
        groupTO.setRealm("/even");
        groupTO.getAuxClasses().add(typeClass.getKey());

        groupTO.getPlainAttrs().add(attrTO(schemaTO.getKey(), "all"));

        groupTO = createGroup(groupTO).getAny();
        assertNotNull(groupTO);

        String resourceName = "resource-ldap-grouponly";
        ResourceTO newResourceTO = null;

        try {
            // Create resource ad-hoc
            ResourceTO resourceTO = new ResourceTO();
            resourceTO.setKey(resourceName);
            resourceTO.setConnector(105L);

            ProvisionTO provisionTO = new ProvisionTO();
            provisionTO.setAnyType(AnyTypeKind.GROUP.name());
            provisionTO.setObjectClass(ObjectClass.GROUP_NAME);
            provisionTO.getAuxClasses().add(typeClass.getKey());
            resourceTO.getProvisions().add(provisionTO);

            MappingTO mapping = new MappingTO();
            provisionTO.setMapping(mapping);

            MappingItemTO item = new MappingItemTO();
            item.setIntMappingType(IntMappingType.GroupPlainSchema);
            item.setExtAttrName("cn");
            item.setIntAttrName(schemaTO.getKey());
            item.setConnObjectKey(true);
            item.setPurpose(MappingPurpose.BOTH);
            mapping.setConnObjectKeyItem(item);

            mapping.setConnObjectLink("'cn=' + " + schemaTO.getKey() + " + ',ou=groups,o=isp'");

            Response response = resourceService.create(resourceTO);
            newResourceTO = getObject(response.getLocation(), ResourceService.class, ResourceTO.class);
            assertNotNull(newResourceTO);
            assertNull(newResourceTO.getProvision(AnyTypeKind.USER.name()));
            assertNotNull(newResourceTO.getProvision(AnyTypeKind.GROUP.name()).getMapping());

            // create push task ad-hoc
            PushTaskTO task = new PushTaskTO();
            task.setName("issueSYNCOPE598");
            task.setActive(true);
            task.setResource(resourceName);
            task.setPerformCreate(true);
            task.setPerformDelete(true);
            task.setPerformUpdate(true);
            task.setUnmatchingRule(UnmatchingRule.ASSIGN);
            task.setMatchingRule(MatchingRule.UPDATE);
            task.getFilters().put(AnyTypeKind.GROUP.name(),
                    SyncopeClient.getGroupSearchConditionBuilder().is("name").equalTo(groupTO.getName()).query());

            response = taskService.create(task);
            PushTaskTO push = getObject(response.getLocation(), TaskService.class, PushTaskTO.class);
            assertNotNull(push);

            // execute the new task
            ExecTO pushExec = execProvisioningTask(taskService, push.getKey(), 50, false);
            assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(pushExec.getStatus()));
        } finally {
            groupService.delete(groupTO.getKey());
            if (newResourceTO != null) {
                resourceService.delete(resourceName);
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
        task.getFilters().put(AnyTypeKind.USER.name(),
                SyncopeClient.getUserSearchConditionBuilder().is("username").equalTo("_NO_ONE_").query());
        task.getFilters().put(AnyTypeKind.GROUP.name(),
                SyncopeClient.getGroupSearchConditionBuilder().is("name").equalTo("citizen").query());
        task.setMatchingRule(MatchingRule.IGNORE);
        task.setUnmatchingRule(UnmatchingRule.IGNORE);

        Response response = taskService.create(task);
        PushTaskTO actual = getObject(response.getLocation(), TaskService.class, PushTaskTO.class);
        assertNotNull(actual);

        // 2. Create notification
        NotificationTO notification = new NotificationTO();
        notification.setTraceLevel(TraceLevel.FAILURES);
        notification.getEvents().add("[PushTask]:[group]:[resource-ldap]:[matchingrule_ignore]:[SUCCESS]");
        notification.getEvents().add("[PushTask]:[group]:[resource-ldap]:[unmatchingrule_ignore]:[SUCCESS]");

        notification.getStaticRecipients().add("issueyncope648@syncope.apache.org");
        notification.setSelfAsRecipient(false);
        notification.setRecipientAttrName("email");
        notification.setRecipientAttrType(IntMappingType.UserPlainSchema);

        notification.setSender("syncope648@syncope.apache.org");
        String subject = "Test notification";
        notification.setSubject(subject);
        notification.setTemplate("optin");
        notification.setActive(true);

        Response responseNotification = notificationService.create(notification);
        notification = getObject(responseNotification.getLocation(), NotificationService.class, NotificationTO.class);
        assertNotNull(notification);

        execProvisioningTask(taskService, actual.getKey(), 50, false);

        NotificationTaskTO taskTO = findNotificationTaskBySender("syncope648@syncope.apache.org");
        assertNotNull(taskTO);
    }
}

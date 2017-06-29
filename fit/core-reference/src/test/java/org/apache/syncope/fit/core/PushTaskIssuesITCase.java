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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.ws.rs.core.Response;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.to.MappingTO;
import org.apache.syncope.common.lib.to.NotificationTO;
import org.apache.syncope.common.lib.to.NotificationTaskTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.PropagationTaskExecStatus;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.common.rest.api.service.NotificationService;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.junit.Test;

public class PushTaskIssuesITCase extends AbstractTaskITCase {

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

        groupTO = createGroup(groupTO).getEntity();
        assertNotNull(groupTO);

        String resourceName = "resource-ldap-grouponly";
        ResourceTO newResourceTO = null;

        try {
            // Create resource ad-hoc
            ResourceTO resourceTO = new ResourceTO();
            resourceTO.setKey(resourceName);
            resourceTO.setConnector("74141a3b-0762-4720-a4aa-fc3e374ef3ef");

            ProvisionTO provisionTO = new ProvisionTO();
            provisionTO.setAnyType(AnyTypeKind.GROUP.name());
            provisionTO.setObjectClass(ObjectClass.GROUP_NAME);
            provisionTO.getAuxClasses().add(typeClass.getKey());
            resourceTO.getProvisions().add(provisionTO);

            MappingTO mapping = new MappingTO();
            provisionTO.setMapping(mapping);

            MappingItemTO item = new MappingItemTO();
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
            ExecTO exec = execProvisioningTask(taskService, push.getKey(), 50, false);
            assertEquals(PropagationTaskExecStatus.SUCCESS, PropagationTaskExecStatus.valueOf(exec.getStatus()));
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

        notification.setSender("syncope648@syncope.apache.org");
        String subject = "Test notification";
        notification.setSubject(subject);
        notification.setTemplate("optin");
        notification.setActive(true);

        Response responseNotification = notificationService.create(notification);
        notification = getObject(responseNotification.getLocation(), NotificationService.class, NotificationTO.class);
        assertNotNull(notification);

        execProvisioningTask(taskService, actual.getKey(), 50, false);

        NotificationTaskTO taskTO = findNotificationTask(notification.getKey(), 50);
        assertNotNull(taskTO);
    }
}

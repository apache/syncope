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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.client.lib.batch.BatchRequest;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.TaskTO;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.beans.ExecuteQuery;
import org.apache.syncope.common.rest.api.beans.ExecQuery;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.junit.jupiter.api.Test;

public class PropagationTaskITCase extends AbstractTaskITCase {

    @Test
    public void paginatedList() {
        PagedResult<PropagationTaskTO> tasks = taskService.search(
                new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(2).build());
        assertNotNull(tasks);
        assertEquals(2, tasks.getResult().size());

        for (TaskTO task : tasks.getResult()) {
            assertNotNull(task);
        }

        tasks = taskService.search(
                new TaskQuery.Builder(TaskType.PROPAGATION).page(2).size(2).build());
        assertNotNull(tasks);
        assertEquals(2, tasks.getPage());
        assertEquals(2, tasks.getResult().size());

        for (TaskTO task : tasks.getResult()) {
            assertNotNull(task);
        }

        tasks = taskService.search(
                new TaskQuery.Builder(TaskType.PROPAGATION).page(1000).size(2).build());
        assertNotNull(tasks);
        assertTrue(tasks.getResult().isEmpty());
    }

    @Test
    public void read() {
        PropagationTaskTO taskTO = taskService.read(TaskType.PROPAGATION, "316285cc-ae52-4ea2-a33b-7355e189ac3f", true);
        assertNotNull(taskTO);
        assertNotNull(taskTO.getExecutions());
        assertTrue(taskTO.getExecutions().isEmpty());
    }

    @Test
    public void batch() throws IOException {
        // create user with testdb resource
        UserCR userCR = UserITCase.getUniqueSample("taskBatch@apache.org");
        userCR.getResources().add(RESOURCE_NAME_TESTDB);
        UserTO userTO = createUser(userCR).getEntity();

        List<PropagationTaskTO> tasks = new ArrayList<>(
                taskService.<PropagationTaskTO>search(new TaskQuery.Builder(TaskType.PROPAGATION).
                        anyTypeKind(AnyTypeKind.USER).entityKey(userTO.getKey()).build()).
                        getResult());
        assertFalse(tasks.isEmpty());

        BatchRequest batchRequest = adminClient.batch();

        TaskService batchTaskService = batchRequest.getService(TaskService.class);
        tasks.forEach(task -> batchTaskService.delete(TaskType.PROPAGATION, task.getKey()));

        Response response = batchRequest.commit().getResponse();
        parseBatchResponse(response);

        assertFalse(taskService.search(
                new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(100).build()).
                getResult().containsAll(tasks));
    }

    @Test
    public void propagationJEXLTransformer() {
        // 0. Set propagation JEXL MappingItemTransformer
        ResourceTO resource = resourceService.read(RESOURCE_NAME_DBSCRIPTED);
        ResourceTO originalResource = SerializationUtils.clone(resource);
        ProvisionTO provision = resource.getProvision("PRINTER").get();
        assertNotNull(provision);

        Optional<ItemTO> mappingItem = provision.getMapping().getItems().stream().
                filter(item -> "location".equals(item.getIntAttrName())).findFirst();
        assertTrue(mappingItem.isPresent());
        assertTrue(mappingItem.get().getTransformers().isEmpty());

        String suffix = getUUIDString();
        mappingItem.get().setPropagationJEXLTransformer("value + '" + suffix + "'");

        try {
            resourceService.update(resource);

            // 1. create printer on external resource
            AnyObjectCR anyObjectCR = AnyObjectITCase.getSample("propagationJEXLTransformer");
            String originalLocation = anyObjectCR.getPlainAttr("location").get().getValues().get(0);
            assertFalse(originalLocation.endsWith(suffix));

            AnyObjectTO anyObjectTO = createAnyObject(anyObjectCR).getEntity();
            assertNotNull(anyObjectTO);

            // 2. verify that JEXL MappingItemTransformer was applied during propagation
            // (location ends with given suffix on external resource)
            ConnObjectTO connObjectTO = resourceService.
                    readConnObject(RESOURCE_NAME_DBSCRIPTED, anyObjectTO.getType(), anyObjectTO.getKey());
            assertFalse(anyObjectTO.getPlainAttr("location").get().getValues().get(0).endsWith(suffix));
            assertTrue(connObjectTO.getAttr("LOCATION").get().getValues().get(0).endsWith(suffix));
        } finally {
            resourceService.update(originalResource);
        }
    }

    @Test
    public void privileges() {
        ResourceTO ldap = resourceService.read(RESOURCE_NAME_LDAP);
        ldap.setKey("ldapWithPrivileges");

        ProvisionTO provision = ldap.getProvision(AnyTypeKind.USER.name()).orElse(null);
        provision.getMapping().getItems().removeIf(item -> "mail".equals(item.getIntAttrName()));
        provision.getVirSchemas().clear();

        ldap.getProvisions().clear();
        ldap.getProvisions().add(provision);

        ItemTO item = new ItemTO();
        item.setIntAttrName("privileges[mightyApp]");
        item.setExtAttrName("businessCategory");
        item.setPurpose(MappingPurpose.PROPAGATION);

        provision.getMapping().add(item);

        ldap = createResource(ldap);

        try {
            UserCR userCR = UserITCase.getUniqueSample("privilege@syncope.apache.org");
            userCR.getResources().add(ldap.getKey());
            userCR.getRoles().add("Other");

            ProvisioningResult<UserTO> result = createUser(userCR);
            assertEquals(1, result.getPropagationStatuses().size());
            assertNotNull(result.getPropagationStatuses().get(0).getAfterObj());

            Attr businessCategory =
                    result.getPropagationStatuses().get(0).getAfterObj().getAttr("businessCategory").orElse(null);
            assertNotNull(businessCategory);
            assertEquals(1, businessCategory.getValues().size());
            assertEquals("postMighty", businessCategory.getValues().get(0));
        } finally {
            resourceService.delete(ldap.getKey());
        }
    }

    @Test
    public void issueSYNCOPE741() {
        for (int i = 0; i < 3; i++) {
            taskService.execute(new ExecuteQuery.Builder().
                    key("1e697572-b896-484c-ae7f-0c8f63fcbc6c").build());
            taskService.execute(new ExecuteQuery.Builder().
                    key("316285cc-ae52-4ea2-a33b-7355e189ac3f").build());
        }
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            // ignore
        }

        // check list
        PagedResult<TaskTO> tasks = taskService.search(
                new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(2).details(false).build());
        for (TaskTO item : tasks.getResult()) {
            assertTrue(item.getExecutions().isEmpty());
        }

        tasks = taskService.search(
                new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(2).details(true).build());
        for (TaskTO item : tasks.getResult()) {
            assertFalse(item.getExecutions().isEmpty());
        }

        // check read
        PropagationTaskTO task = taskService.read(TaskType.PROPAGATION, "1e697572-b896-484c-ae7f-0c8f63fcbc6c", false);
        assertNotNull(task);
        assertEquals("1e697572-b896-484c-ae7f-0c8f63fcbc6c", task.getKey());
        assertTrue(task.getExecutions().isEmpty());

        task = taskService.read(TaskType.PROPAGATION, "1e697572-b896-484c-ae7f-0c8f63fcbc6c", true);
        assertNotNull(task);
        assertEquals("1e697572-b896-484c-ae7f-0c8f63fcbc6c", task.getKey());
        assertFalse(task.getExecutions().isEmpty());

        // check list executions
        PagedResult<ExecTO> execs = taskService.listExecutions(new ExecQuery.Builder().key(
                "1e697572-b896-484c-ae7f-0c8f63fcbc6c").
                page(1).size(2).build());
        assertTrue(execs.getTotalCount() >= execs.getResult().size());
    }

    @Test
    public void issueSYNCOPE1288() {
        // create a new user
        UserCR userCR = UserITCase.getUniqueSample("xxxyyy@xxx.xxx");
        userCR.getResources().add(RESOURCE_NAME_LDAP);

        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);

        // generate some PropagationTasks
        for (int i = 0; i < 9; i++) {
            UserUR userUR = new UserUR();
            userUR.setKey(userTO.getKey());
            userUR.getPlainAttrs().add(new AttrPatch.Builder(new Attr.Builder("userId").value(
                    "test" + getUUIDString() + i + "@test.com").build()).
                    operation(PatchOperation.ADD_REPLACE).
                    build());

            userService.update(userUR);
        }

        // ASC order
        PagedResult<TaskTO> unorderedTasks = taskService.search(
                new TaskQuery.Builder(TaskType.PROPAGATION).
                        resource(RESOURCE_NAME_LDAP).
                        entityKey(userTO.getKey()).
                        anyTypeKind(AnyTypeKind.USER).
                        page(1).
                        size(10).
                        build());
        Collections.sort(unorderedTasks.getResult(), new Comparator<TaskTO>() {

            @Override
            public int compare(final TaskTO o1, final TaskTO o2) {
                return o1.getStart().compareTo(o2.getStart());
            }
        });
        assertNotNull(unorderedTasks);
        assertFalse(unorderedTasks.getResult().isEmpty());
        assertEquals(10, unorderedTasks.getResult().size());

        PagedResult<TaskTO> orderedTasks = taskService.search(
                new TaskQuery.Builder(TaskType.PROPAGATION).
                        resource(RESOURCE_NAME_LDAP).
                        entityKey(userTO.getKey()).
                        anyTypeKind(AnyTypeKind.USER).
                        page(1).
                        size(10).
                        orderBy("start").
                        build());
        assertNotNull(orderedTasks);
        assertFalse(orderedTasks.getResult().isEmpty());
        assertEquals(10, orderedTasks.getResult().size());

        assertTrue(orderedTasks.getResult().equals(unorderedTasks.getResult()));

        // DESC order
        Collections.reverse(unorderedTasks.getResult());
        orderedTasks = taskService.search(
                new TaskQuery.Builder(TaskType.PROPAGATION).
                        resource(RESOURCE_NAME_LDAP).
                        entityKey(userTO.getKey()).
                        anyTypeKind(AnyTypeKind.USER).
                        page(1).
                        size(10).
                        orderBy("start DESC").
                        build());

        assertTrue(orderedTasks.getResult().equals(unorderedTasks.getResult()));
    }
}

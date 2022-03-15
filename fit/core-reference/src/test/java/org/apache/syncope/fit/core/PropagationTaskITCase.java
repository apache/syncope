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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.lib.batch.BatchRequest;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.core.GenericType;
import javax.xml.ws.WebServiceException;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.TaskTO;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.request.MembershipUR;
import org.apache.syncope.common.lib.request.ResourceDR;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.ExecSpecs;
import org.apache.syncope.common.rest.api.beans.ExecListQuery;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.provisioning.api.utils.FormatUtils;
import org.apache.syncope.fit.core.reference.DateToDateItemTransformer;
import org.apache.syncope.fit.core.reference.DateToLongItemTransformer;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

public class PropagationTaskITCase extends AbstractTaskITCase {

    @BeforeAll
    public static void testItemTransformersSetup() {
        ImplementationTO dateToLong = null;
        ImplementationTO dateToDate = null;
        try {
            dateToLong = implementationService.read(
                    IdRepoImplementationType.ITEM_TRANSFORMER, DateToLongItemTransformer.class.getSimpleName());
            dateToDate = implementationService.read(
                    IdRepoImplementationType.ITEM_TRANSFORMER, DateToDateItemTransformer.class.getSimpleName());
        } catch (SyncopeClientException e) {
            if (e.getType().getResponseStatus() == Response.Status.NOT_FOUND) {
                dateToLong = new ImplementationTO();
                dateToLong.setKey(DateToLongItemTransformer.class.getSimpleName());
                dateToLong.setEngine(ImplementationEngine.JAVA);
                dateToLong.setType(IdRepoImplementationType.ITEM_TRANSFORMER);
                dateToLong.setBody(DateToLongItemTransformer.class.getName());
                Response response = implementationService.create(dateToLong);
                dateToLong = implementationService.read(
                        dateToLong.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                assertNotNull(dateToLong);

                dateToDate = new ImplementationTO();
                dateToDate.setKey(DateToDateItemTransformer.class.getSimpleName());
                dateToDate.setEngine(ImplementationEngine.JAVA);
                dateToDate.setType(IdRepoImplementationType.ITEM_TRANSFORMER);
                dateToDate.setBody(DateToDateItemTransformer.class.getName());
                response = implementationService.create(dateToDate);
                dateToDate = implementationService.read(
                        dateToDate.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                assertNotNull(dateToDate);
            }
        }
        assertNotNull(dateToLong);
        assertNotNull(dateToDate);
    }

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
        ProvisionTO provision = resource.getProvision(PRINTER).get();
        assertNotNull(provision);

        Optional<ItemTO> mappingItem = provision.getMapping().getItems().stream().
                filter(item -> "location".equals(item.getIntAttrName())).findFirst();
        assertTrue(mappingItem.isPresent());
        assertTrue(mappingItem.get().getTransformers().isEmpty());

        String suffix = getUUIDString();
        mappingItem.get().setPropagationJEXLTransformer("value + '" + suffix + '\'');

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
    public void purgePropagations() {
        try {
            taskService.purgePropagations(null, null, null);
            fail();
        } catch (WebServiceException e) {
            assertNotNull(e);
        }

        OffsetDateTime oneWeekAgo = OffsetDateTime.now().minusWeeks(1);
        Response response = taskService.purgePropagations(
                oneWeekAgo,
                List.of(ExecStatus.SUCCESS),
                List.of(RESOURCE_NAME_WS1));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        List<PropagationTaskTO> deleted = response.readEntity(new GenericType<List<PropagationTaskTO>>() {
        });
        assertNotNull(deleted);
        // only ws-target-resource-1 PROPAGATION tasks should have been deleted
        assertEquals(1, deleted.size());
        assertTrue(deleted.stream().allMatch(d -> RESOURCE_NAME_WS1.equals(d.getResource())));
        // check that other propagation tasks haven't been affected
        assertFalse(taskService.search(new TaskQuery.Builder(TaskType.PROPAGATION)
                .anyTypeKind(AnyTypeKind.USER)
                .page(0).size(10)
                .build()).getResult().isEmpty());
        // delete all remaining SUCCESS tasks
        response = taskService.purgePropagations(oneWeekAgo, List.of(ExecStatus.SUCCESS), List.of());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        deleted = response.readEntity(new GenericType<List<PropagationTaskTO>>() {
        });
        assertNotNull(deleted);
    }

    @Test
    public void propagationPolicy() throws InterruptedException {
        SyncopeClient.nullPriorityAsync(anyObjectService, true);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        jdbcTemplate.execute("ALTER TABLE TESTPRINTER ADD COLUMN MAND_VALUE VARCHAR(1)");
        jdbcTemplate.execute("UPDATE TESTPRINTER SET MAND_VALUE='C'");
        jdbcTemplate.execute("ALTER TABLE TESTPRINTER ALTER COLUMN MAND_VALUE VARCHAR(1) NOT NULL");
        try {
            String entityKey = createAnyObject(AnyObjectITCase.getSample("propagationPolicy")).getEntity().getKey();

            Thread.sleep(1000);
            jdbcTemplate.execute("ALTER TABLE TESTPRINTER DROP COLUMN MAND_VALUE");

            PagedResult<PropagationTaskTO> propagations = await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).until(
                    () -> taskService.search(
                            new TaskQuery.Builder(TaskType.PROPAGATION).resource(RESOURCE_NAME_DBSCRIPTED).
                                    anyTypeKind(AnyTypeKind.ANY_OBJECT).entityKey(entityKey).build()),
                    p -> p.getTotalCount() > 0);

            propagations.getResult().get(0).getExecutions().stream().
                    anyMatch(e -> ExecStatus.FAILURE.name().equals(e.getStatus()));
            propagations.getResult().get(0).getExecutions().stream().
                    anyMatch(e -> ExecStatus.SUCCESS.name().equals(e.getStatus()));
        } finally {
            SyncopeClient.nullPriorityAsync(anyObjectService, false);

            try {
                jdbcTemplate.execute("ALTER TABLE TESTPRINTER DROP COLUMN MAND_VALUE");
            } catch (DataAccessException e) {
                // ignore
            }
        }
    }

    @Test
    public void issueSYNCOPE741() {
        for (int i = 0; i < 3; i++) {
            taskService.execute(new ExecSpecs.Builder().
                    key("1e697572-b896-484c-ae7f-0c8f63fcbc6c").build());
            taskService.execute(new ExecSpecs.Builder().
                    key("316285cc-ae52-4ea2-a33b-7355e189ac3f").build());
        }
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            // ignore
        }

        // check list
        PagedResult<TaskTO> tasks = taskService.search(
                new TaskQuery.Builder(TaskType.PROPAGATION).
                        page(1).size(2).orderBy("operation DESC").details(false).build());
        for (TaskTO item : tasks.getResult()) {
            assertTrue(item.getExecutions().isEmpty());
        }

        tasks = taskService.search(
                new TaskQuery.Builder(TaskType.PROPAGATION).
                        page(1).size(2).orderBy("operation DESC").details(true).build());
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
        PagedResult<ExecTO> execs = taskService.listExecutions(new ExecListQuery.Builder().key(
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
        Collections.sort(unorderedTasks.getResult(), (t1, t2) -> t1.getStart().compareTo(t2.getStart()));
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

    @Test
    public void issueSYNCOPE1430() throws ParseException {
        ResourceTO ldap = resourceService.read(RESOURCE_NAME_LDAP);
        try {
            // 1. clone the LDAP resource and add some sensible mappings
            ProvisionTO provision = ldap.getProvision(AnyTypeKind.USER.name()).orElse(null);
            assertNotNull(provision);
            provision.getMapping().getItems().removeIf(item -> "mail".equals(item.getExtAttrName()));
            provision.getVirSchemas().clear();

            // Date -> long (JEXL expression) -> string (as all JEXL in Syncope)
            ItemTO loginDateForJexlAsLong = new ItemTO();
            loginDateForJexlAsLong.setPurpose(MappingPurpose.PROPAGATION);
            loginDateForJexlAsLong.setIntAttrName("loginDate");
            loginDateForJexlAsLong.setExtAttrName("employeeNumber");
            loginDateForJexlAsLong.setPropagationJEXLTransformer("value.toInstant().toEpochMilli()");
            provision.getMapping().add(loginDateForJexlAsLong);

            // Date -> string (JEXL expression)
            ItemTO loginDateForJexlAsString = new ItemTO();
            loginDateForJexlAsString.setPurpose(MappingPurpose.PROPAGATION);
            loginDateForJexlAsString.setIntAttrName("loginDate");
            loginDateForJexlAsString.setExtAttrName("street");
            loginDateForJexlAsString.setPropagationJEXLTransformer(
                    "value.toInstant().toString().split(\"T\")[0].replace(\"-\", \"\")");
            provision.getMapping().add(loginDateForJexlAsString);

            // Date -> long
            ItemTO loginDateForJavaToLong = new ItemTO();
            loginDateForJavaToLong.setPurpose(MappingPurpose.PROPAGATION);
            loginDateForJavaToLong.setIntAttrName("loginDate");
            loginDateForJavaToLong.setExtAttrName("st");
            loginDateForJavaToLong.getTransformers().add(DateToLongItemTransformer.class.getSimpleName());
            provision.getMapping().add(loginDateForJavaToLong);

            // Date -> date
            ItemTO loginDateForJavaToDate = new ItemTO();
            loginDateForJavaToDate.setPurpose(MappingPurpose.PROPAGATION);
            loginDateForJavaToDate.setIntAttrName("loginDate");
            loginDateForJavaToDate.setExtAttrName("carLicense");
            loginDateForJavaToDate.getTransformers().add(DateToDateItemTransformer.class.getSimpleName());
            provision.getMapping().add(loginDateForJavaToDate);

            ldap.getProvisions().clear();
            ldap.getProvisions().add(provision);
            ldap.setKey(RESOURCE_NAME_LDAP + "1430" + getUUIDString());
            resourceService.create(ldap);

            // 2. create user with the new resource assigned
            UserCR createReq = UserITCase.getUniqueSample("syncope1430@syncope.apache.org");
            createReq.getResources().clear();
            createReq.getResources().add(ldap.getKey());
            createReq.getPlainAttrs().removeIf(attr -> "loginDate".equals(attr.getSchema()));
            createReq.getPlainAttrs().add(attr("loginDate", "2019-01-29"));
            UserTO user = createUser(createReq).getEntity();

            // 3. check attributes prepared for propagation
            PagedResult<PropagationTaskTO> tasks = taskService.search(new TaskQuery.Builder(TaskType.PROPAGATION).
                    resource(user.getResources().iterator().next()).
                    anyTypeKind(AnyTypeKind.USER).entityKey(user.getKey()).build());
            assertEquals(1, tasks.getSize());

            Set<Attribute> propagationAttrs = new HashSet<>();
            if (StringUtils.isNotBlank(tasks.getResult().get(0).getAttributes())) {
                propagationAttrs.addAll(List.of(
                        POJOHelper.deserialize(tasks.getResult().get(0).getAttributes(), Attribute[].class)));
            }

            OffsetDateTime loginDate = LocalDate.parse(user.getPlainAttr("loginDate").get().getValues().get(0)).
                    atStartOfDay(FormatUtils.DEFAULT_OFFSET).toOffsetDateTime();

            Attribute employeeNumber = AttributeUtil.find("employeeNumber", propagationAttrs);
            assertNotNull(employeeNumber);
            assertEquals(String.valueOf(loginDate.toInstant().toEpochMilli()), employeeNumber.getValue().get(0));

            Attribute street = AttributeUtil.find("street", propagationAttrs);
            assertNotNull(street);
            assertEquals(loginDate.toInstant().toString().split("T")[0].replace("-", ""), street.getValue().get(0));

            Attribute st = AttributeUtil.find("st", propagationAttrs);
            assertNotNull(st);
            assertEquals(loginDate.toInstant().toEpochMilli(), st.getValue().get(0));

            Attribute carLicense = AttributeUtil.find("carLicense", propagationAttrs);
            assertNotNull(carLicense);
            assertEquals(DateTimeFormatter.ISO_LOCAL_DATE.format(loginDate.plusDays(1)), carLicense.getValue().get(0));
        } finally {
            try {
                resourceService.delete(ldap.getKey());
            } catch (Exception ignore) {
                // ignore
            }
        }
    }

    @Test
    public void issueSYNCOPE1473() throws ParseException {
        // create a new group schema
        PlainSchemaTO schemaTO = new PlainSchemaTO();
        schemaTO.setKey("ldapGroups" + getUUIDString());
        schemaTO.setType(AttrSchemaType.String);
        schemaTO.setMultivalue(true);
        schemaTO.setReadonly(true);
        schemaTO.setAnyTypeClass("minimal user");

        schemaTO = createSchema(SchemaType.PLAIN, schemaTO);
        assertNotNull(schemaTO);

        ResourceTO ldap = resourceService.read(RESOURCE_NAME_LDAP);
        UserTO userTO = null;
        try {
            // 1. clone the LDAP resource and add some sensible mappings
            ProvisionTO provisionGroup =
                    SerializationUtils.clone(ldap.getProvision(AnyTypeKind.GROUP.name()).orElse(null));
            assertNotNull(provisionGroup);
            provisionGroup.getVirSchemas().clear();

            ProvisionTO provisionUser =
                    SerializationUtils.clone(ldap.getProvision(AnyTypeKind.USER.name()).orElse(null));
            assertNotNull(provisionUser);
            provisionUser.getMapping().getItems().removeIf(item -> "mail".equals(item.getExtAttrName()));
            provisionUser.getVirSchemas().clear();

            ItemTO ldapGroups = new ItemTO();
            ldapGroups.setPurpose(MappingPurpose.PROPAGATION);
            ldapGroups.setIntAttrName(schemaTO.getKey());
            ldapGroups.setExtAttrName("ldapGroups");
            provisionUser.getMapping().add(ldapGroups);

            ldap.getProvisions().clear();
            ldap.getProvisions().add(provisionUser);
            ldap.getProvisions().add(provisionGroup);
            ldap.setKey(RESOURCE_NAME_LDAP + "1473" + getUUIDString());
            resourceService.create(ldap);

            // 1. create group with the new resource assigned
            GroupCR groupCR = new GroupCR();
            groupCR.setName("SYNCOPEGROUP1473-" + getUUIDString());
            groupCR.setRealm(SyncopeConstants.ROOT_REALM);
            groupCR.getResources().add(ldap.getKey());

            GroupTO groupTO = createGroup(groupCR).getEntity();
            assertNotNull(groupCR);

            // 2. create user with the new resource assigned
            UserCR userCR = UserITCase.getUniqueSample("syncope1473@syncope.apache.org");
            userCR.getResources().clear();
            userCR.getResources().add(ldap.getKey());
            userCR.getMemberships().add(new MembershipTO.Builder(groupTO.getKey()).build());

            userTO = createUser(userCR).getEntity();
            assertNotNull(userTO);

            // 3. check attributes prepared for propagation
            PagedResult<PropagationTaskTO> tasks = taskService.search(new TaskQuery.Builder(TaskType.PROPAGATION).
                    resource(userTO.getResources().iterator().next()).
                    anyTypeKind(AnyTypeKind.USER).entityKey(userTO.getKey()).build());
            assertEquals(1, tasks.getSize());

            ResourceDR resourceDR = new ResourceDR.Builder().key(groupTO.getKey()).
                    action(ResourceDeassociationAction.UNLINK).resource(ldap.getKey()).build();

            groupService.deassociate(resourceDR);
            groupService.delete(groupTO.getKey());

            GroupCR newGroupCR = new GroupCR();
            newGroupCR.setName("NEWSYNCOPEGROUP1473-" + getUUIDString());
            newGroupCR.setRealm(SyncopeConstants.ROOT_REALM);
            newGroupCR.getResources().add(ldap.getKey());

            GroupTO newGroupTO = createGroup(newGroupCR).getEntity();
            assertNotNull(newGroupTO);

            UserUR userUR = new UserUR();
            userUR.setKey(userTO.getKey());
            userUR.getMemberships().add(
                    new MembershipUR.Builder(newGroupTO.getKey()).operation(PatchOperation.ADD_REPLACE).build());
            userService.update(userUR);

            ConnObjectTO connObject =
                    resourceService.readConnObject(ldap.getKey(), AnyTypeKind.USER.name(), userTO.getKey());
            assertNotNull(connObject);
            assertTrue(connObject.getAttr("ldapGroups").isPresent());
            assertEquals(2, connObject.getAttr("ldapGroups").get().getValues().size());
        } finally {
            try {
                resourceService.delete(ldap.getKey());
                if (userTO != null) {
                    userService.delete(userTO.getKey());
                }
                schemaService.delete(SchemaType.PLAIN, schemaTO.getKey());
            } catch (Exception ignore) {
                // ignore
            }
        }
    }

    @Test
    public void issueSYNCOPE1567() {
        ResourceTO ldap = resourceService.read(RESOURCE_NAME_LDAP);
        try {
            // 1. clone the LDAP resource and add the relationships mapping
            ProvisionTO provisionUser =
                    SerializationUtils.clone(ldap.getProvision(AnyTypeKind.USER.name()).orElse(null));
            assertNotNull(provisionUser);
            provisionUser.getVirSchemas().clear();

            ItemTO relationships = new ItemTO();
            relationships.setPurpose(MappingPurpose.PROPAGATION);
            relationships.setIntAttrName("relationships[neighborhood][PRINTER].model");
            relationships.setExtAttrName("l");
            provisionUser.getMapping().add(relationships);

            ldap.getProvisions().clear();
            ldap.getProvisions().add(provisionUser);
            ldap.setKey(RESOURCE_NAME_LDAP + "1567" + getUUIDString());
            resourceService.create(ldap);

            // 1. create user with relationship and the new resource assigned
            UserCR userCR = UserITCase.getUniqueSample("syncope1567@syncope.apache.org");
            userCR.getRelationships().add(new RelationshipTO.Builder().
                    type("neighborhood").otherEnd(PRINTER, "fc6dbc3a-6c07-4965-8781-921e7401a4a5").build());
            userCR.getResources().clear();
            userCR.getResources().add(ldap.getKey());

            UserTO userTO = createUser(userCR).getEntity();
            assertNotNull(userTO);
            assertFalse(userTO.getRelationships().isEmpty());

            // 2. check attributes prepared for propagation
            PagedResult<PropagationTaskTO> tasks = taskService.search(new TaskQuery.Builder(TaskType.PROPAGATION).
                    resource(userCR.getResources().iterator().next()).
                    anyTypeKind(AnyTypeKind.USER).entityKey(userTO.getKey()).build());
            assertEquals(1, tasks.getSize());

            Set<Attribute> propagationAttrs = Stream.of(
                    POJOHelper.deserialize(tasks.getResult().get(0).getAttributes(), Attribute[].class)).
                    collect(Collectors.toSet());
            Attribute attr = AttributeUtil.find("l", propagationAttrs);
            assertNotNull(attr);
            assertNotNull(attr.getValue());
            assertEquals("Canon MFC8030", attr.getValue().get(0).toString());

            // 3. check propagated value
            ConnObjectTO connObject =
                    resourceService.readConnObject(ldap.getKey(), AnyTypeKind.USER.name(), userTO.getKey());
            assertNotNull(connObject);
            assertTrue(connObject.getAttr("l").isPresent());
            assertEquals("Canon MFC8030", connObject.getAttr("l").get().getValues().get(0));
        } finally {
            try {
                resourceService.delete(ldap.getKey());
            } catch (Exception ignore) {
                // ignore
            }
        }
    }

    @Test
    public void issueSYNCOPE1605() throws ParseException {
        ResourceTO ldap = resourceService.read(RESOURCE_NAME_LDAP);
        try {
            // 1. clone the LDAP resource and add some sensible mappings
            ProvisionTO provisionGroup =
                    SerializationUtils.clone(ldap.getProvision(AnyTypeKind.GROUP.name()).orElse(null));
            assertNotNull(provisionGroup);
            provisionGroup.getVirSchemas().clear();
            provisionGroup.getMapping().getItems().clear();

            ItemTO item = new ItemTO();
            item.setConnObjectKey(true);
            item.setIntAttrName("name");
            item.setExtAttrName("description");
            item.setPurpose(MappingPurpose.BOTH);

            provisionGroup.getMapping().setConnObjectKeyItem(item);
            provisionGroup.getMapping().setConnObjectLink("'cn=' + originalName + ',ou=groups,o=isp'");

            ldap.getProvisions().clear();
            ldap.getProvisions().add(provisionGroup);

            ldap.setKey(RESOURCE_NAME_LDAP + "1605" + getUUIDString());
            resourceService.create(ldap);

            // 1. create group with the new resource assigned
            String originalName = "grp1605-" + getUUIDString();

            GroupCR groupCR = new GroupCR();
            groupCR.setName("SYNCOPEGROUP1605-" + getUUIDString());
            groupCR.setRealm(SyncopeConstants.ROOT_REALM);
            groupCR.getResources().add(ldap.getKey());
            groupCR.getPlainAttrs().add(new Attr.Builder("originalName").value(originalName).build());

            GroupTO groupTO = createGroup(groupCR).getEntity();
            assertNotNull(groupTO);

            // 3. check attributes prepared for propagation
            PagedResult<PropagationTaskTO> tasks = taskService.search(new TaskQuery.Builder(TaskType.PROPAGATION).
                    resource(ldap.getKey()).anyTypeKind(AnyTypeKind.GROUP).entityKey(groupTO.getKey()).build());
            assertEquals(1, tasks.getSize());
            assertEquals(ResourceOperation.CREATE, tasks.getResult().get(0).getOperation());
            assertEquals(ExecStatus.SUCCESS.name(), tasks.getResult().get(0).getLatestExecStatus());

            ConnObjectTO beforeConnObject =
                    resourceService.readConnObject(ldap.getKey(), AnyTypeKind.GROUP.name(), groupTO.getKey());

            GroupUR groupUR = new GroupUR();
            groupUR.setKey(groupTO.getKey());

            groupUR.getPlainAttrs().add(attrAddReplacePatch("originalName", "new" + originalName));
            groupTO = updateGroup(groupUR).getEntity();

            tasks = taskService.search(new TaskQuery.Builder(TaskType.PROPAGATION).
                    resource(ldap.getKey()).anyTypeKind(AnyTypeKind.GROUP).entityKey(groupTO.getKey()).
                    orderBy("start DESC").build());
            assertEquals(2, tasks.getSize());
            assertEquals(ResourceOperation.UPDATE, tasks.getResult().get(0).getOperation());
            assertEquals(ExecStatus.SUCCESS.name(), tasks.getResult().get(0).getLatestExecStatus());

            ConnObjectTO afterConnObject =
                    resourceService.readConnObject(ldap.getKey(), AnyTypeKind.GROUP.name(), groupTO.getKey());
            assertNotEquals(afterConnObject.getAttr(Name.NAME).get().getValues().get(0),
                    beforeConnObject.getAttr(Name.NAME).get().getValues().get(0));
            assertTrue(afterConnObject.getAttr(Name.NAME).get().getValues().get(0).contains("new" + originalName));
        } finally {
            try {
                resourceService.delete(ldap.getKey());
            } catch (Exception ignore) {
                // ignore
            }
        }
    }
}

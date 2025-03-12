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

import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.ws.WebServiceException;
import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.batch.BatchRequest;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.policy.PropagationPolicyTO;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.request.MembershipUR;
import org.apache.syncope.common.lib.request.ResourceDR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.ConnObject;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.ReconStatus;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.TaskTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.ExecQuery;
import org.apache.syncope.common.rest.api.beans.ExecSpecs;
import org.apache.syncope.common.rest.api.beans.ReconQuery;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.core.persistence.api.entity.task.PropagationData;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.fit.core.reference.DateToDateItemTransformer;
import org.apache.syncope.fit.core.reference.DateToLongItemTransformer;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.CollectionUtils;

public class PropagationTaskITCase extends AbstractTaskITCase {

    @BeforeAll
    public static void testItemTransformersSetup() {
        ImplementationTO dateToLong = null;
        ImplementationTO dateToDate = null;
        try {
            dateToLong = IMPLEMENTATION_SERVICE.read(
                    IdRepoImplementationType.ITEM_TRANSFORMER, DateToLongItemTransformer.class.getSimpleName());
            dateToDate = IMPLEMENTATION_SERVICE.read(
                    IdRepoImplementationType.ITEM_TRANSFORMER, DateToDateItemTransformer.class.getSimpleName());
        } catch (SyncopeClientException e) {
            if (e.getType().getResponseStatus() == Response.Status.NOT_FOUND) {
                dateToLong = new ImplementationTO();
                dateToLong.setKey(DateToLongItemTransformer.class.getSimpleName());
                dateToLong.setEngine(ImplementationEngine.JAVA);
                dateToLong.setType(IdRepoImplementationType.ITEM_TRANSFORMER);
                dateToLong.setBody(DateToLongItemTransformer.class.getName());
                Response response = IMPLEMENTATION_SERVICE.create(dateToLong);
                dateToLong = IMPLEMENTATION_SERVICE.read(
                        dateToLong.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                assertNotNull(dateToLong);

                dateToDate = new ImplementationTO();
                dateToDate.setKey(DateToDateItemTransformer.class.getSimpleName());
                dateToDate.setEngine(ImplementationEngine.JAVA);
                dateToDate.setType(IdRepoImplementationType.ITEM_TRANSFORMER);
                dateToDate.setBody(DateToDateItemTransformer.class.getName());
                response = IMPLEMENTATION_SERVICE.create(dateToDate);
                dateToDate = IMPLEMENTATION_SERVICE.read(
                        dateToDate.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                assertNotNull(dateToDate);
            }
        }
        assertNotNull(dateToLong);
        assertNotNull(dateToDate);
    }

    @Test
    public void paginatedList() {
        long count = TASK_SERVICE.search(
                new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(1).build()).getTotalCount();

        if (count >= 2) {
            PagedResult<PropagationTaskTO> tasks = TASK_SERVICE.search(
                    new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(2).build());
            assertNotNull(tasks);
            assertEquals(2, tasks.getResult().size());

            for (TaskTO task : tasks.getResult()) {
                assertNotNull(task);
            }
        }

        if (count >= 4) {
            PagedResult<TaskTO> tasks = TASK_SERVICE.search(
                    new TaskQuery.Builder(TaskType.PROPAGATION).page(2).size(2).build());
            assertNotNull(tasks);
            assertEquals(2, tasks.getPage());
            assertEquals(2, tasks.getResult().size());

            for (TaskTO task : tasks.getResult()) {
                assertNotNull(task);
            }
        }

        PagedResult<TaskTO> tasks = TASK_SERVICE.search(
                new TaskQuery.Builder(TaskType.PROPAGATION).page(1000).size(2).build());
        assertNotNull(tasks);
        assertTrue(tasks.getResult().isEmpty());
    }

    @Test
    public void read() {
        PropagationTaskTO taskTO = TASK_SERVICE.read(
                TaskType.PROPAGATION, "316285cc-ae52-4ea2-a33b-7355e189ac3f", true);
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
                TASK_SERVICE.<PropagationTaskTO>search(new TaskQuery.Builder(TaskType.PROPAGATION).
                        anyTypeKind(AnyTypeKind.USER).entityKey(userTO.getKey()).build()).
                        getResult());
        assertFalse(tasks.isEmpty());

        BatchRequest batchRequest = ADMIN_CLIENT.batch();

        TaskService batchTaskService = batchRequest.getService(TaskService.class);
        tasks.forEach(task -> batchTaskService.delete(TaskType.PROPAGATION, task.getKey()));

        Response response = batchRequest.commit().getResponse();
        parseBatchResponse(response);

        assertFalse(TASK_SERVICE.search(
                new TaskQuery.Builder(TaskType.PROPAGATION).page(1).size(100).build()).
                getResult().containsAll(tasks));
    }

    @Test
    public void propagationJEXLTransformer() throws IOException {
        // 0. Set propagation JEXL MappingItemTransformer
        ResourceTO resource = RESOURCE_SERVICE.read(RESOURCE_NAME_DBSCRIPTED);
        ResourceTO originalResource = RESOURCE_SERVICE.read(RESOURCE_NAME_DBSCRIPTED);
        Provision provision = resource.getProvision(PRINTER).orElseThrow();
        assertNotNull(provision);

        Optional<Item> mappingItem = provision.getMapping().getItems().stream().
                filter(item -> "location".equals(item.getIntAttrName())).findFirst();
        assertTrue(mappingItem.isPresent());
        assertTrue(mappingItem.orElseThrow().getTransformers().isEmpty());

        String suffix = getUUIDString();
        mappingItem.orElseThrow().setPropagationJEXLTransformer("value + '" + suffix + '\'');

        try {
            RESOURCE_SERVICE.update(resource);

            // 1. create printer on external resource
            AnyObjectCR anyObjectCR = AnyObjectITCase.getSample("propagationJEXLTransformer");
            String originalLocation = anyObjectCR.getPlainAttr("location").orElseThrow().getValues().getFirst();
            assertFalse(originalLocation.endsWith(suffix));

            AnyObjectTO anyObjectTO = createAnyObject(anyObjectCR).getEntity();
            assertNotNull(anyObjectTO);

            // 2. verify that JEXL MappingItemTransformer was applied during propagation
            // (location ends with given suffix on external resource)
            ConnObject connObjectTO = RESOURCE_SERVICE.
                    readConnObject(RESOURCE_NAME_DBSCRIPTED, anyObjectTO.getType(), anyObjectTO.getKey());
            assertFalse(anyObjectTO.getPlainAttr("location").orElseThrow().getValues().getFirst().endsWith(suffix));
            assertTrue(connObjectTO.getAttr("LOCATION").orElseThrow().getValues().getFirst().endsWith(suffix));
        } finally {
            RESOURCE_SERVICE.update(originalResource);
        }
    }

    @Test
    public void purgePropagations() {
        try {
            TASK_SERVICE.purgePropagations(null, null, null);
            fail();
        } catch (WebServiceException e) {
            assertNotNull(e);
        }

        long count = TASK_SERVICE.search(new TaskQuery.Builder(TaskType.PROPAGATION).
                resource(RESOURCE_NAME_WS1).page(1).size(100).build()).getResult().stream().
                filter(t -> ExecStatus.SUCCESS.name().equals(t.getLatestExecStatus())).count();
        OffsetDateTime since = OffsetDateTime.now().minusWeeks(1);
        if (count == 0) {
            UserCR userCR = UserITCase.getUniqueSample("purge@syncope.org");
            userCR.getResources().add(RESOURCE_NAME_WS1);

            UserTO userTO = createUser(userCR).getEntity();
            assertNotNull(userTO);

            count = await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).until(
                    () -> TASK_SERVICE.<PropagationTaskTO>search(new TaskQuery.Builder(TaskType.PROPAGATION).
                            resource(RESOURCE_NAME_WS1).page(1).size(100).build()).getResult().stream().
                            filter(t -> ExecStatus.SUCCESS.name().equals(t.getLatestExecStatus())).count(),
                    c -> c > 0);
            since = OffsetDateTime.now().plusWeeks(1);
        }

        Response response = TASK_SERVICE.purgePropagations(
                since,
                List.of(ExecStatus.SUCCESS),
                List.of(RESOURCE_NAME_WS1));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        List<PropagationTaskTO> deleted = response.readEntity(new GenericType<List<PropagationTaskTO>>() {
        });
        assertNotNull(deleted);
        // only ws-target-resource-1 PROPAGATION tasks should have been deleted
        assertEquals(count, deleted.size());
        assertTrue(deleted.stream().allMatch(d -> RESOURCE_NAME_WS1.equals(d.getResource())));
    }

    @Test
    public void propagationPolicyRetry() throws InterruptedException {
        SyncopeClient.nullPriorityAsync(ANY_OBJECT_SERVICE, true);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
        jdbcTemplate.execute("ALTER TABLE TESTPRINTER ADD COLUMN MAND_VALUE VARCHAR(1)");
        jdbcTemplate.execute("UPDATE TESTPRINTER SET MAND_VALUE='C'");
        jdbcTemplate.execute("ALTER TABLE TESTPRINTER ALTER COLUMN MAND_VALUE VARCHAR(1) NOT NULL");
        try {
            String entityKey = createAnyObject(AnyObjectITCase.getSample("propagationPolicy")).getEntity().getKey();

            Thread.sleep(1000);
            jdbcTemplate.execute("ALTER TABLE TESTPRINTER DROP COLUMN MAND_VALUE");

            PagedResult<PropagationTaskTO> propagations = await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).until(
                    () -> TASK_SERVICE.search(
                            new TaskQuery.Builder(TaskType.PROPAGATION).resource(RESOURCE_NAME_DBSCRIPTED).
                                    anyTypeKind(AnyTypeKind.ANY_OBJECT).entityKey(entityKey).build()),
                    p -> p.getTotalCount() > 0);

            propagations.getResult().getFirst().getExecutions().stream().
                    anyMatch(e -> ExecStatus.FAILURE.name().equals(e.getStatus()));
            propagations.getResult().getFirst().getExecutions().stream().
                    anyMatch(e -> ExecStatus.SUCCESS.name().equals(e.getStatus()));
        } finally {
            SyncopeClient.nullPriorityAsync(ANY_OBJECT_SERVICE, false);

            try {
                jdbcTemplate.execute("ALTER TABLE TESTPRINTER DROP COLUMN MAND_VALUE");
            } catch (DataAccessException e) {
                // ignore
            }
        }
    }

    private static String propagationPolicyOptimizeKey() {
        return POLICY_SERVICE.list(PolicyType.PROPAGATION).stream().
                filter(p -> "optimize".equals(p.getName())).
                findFirst().
                orElseGet(() -> {
                    PropagationPolicyTO policy = new PropagationPolicyTO();
                    policy.setName("optimize");
                    policy.setFetchAroundProvisioning(false);
                    policy.setUpdateDelta(true);
                    return createPolicy(PolicyType.PROPAGATION, policy);
                }).getKey();
    }

    @Test
    public void propagationPolicyOptimizeToLDAP() {
        String policyKey = propagationPolicyOptimizeKey();

        ResourceTO ldap = RESOURCE_SERVICE.read(RESOURCE_NAME_LDAP);
        assertNull(ldap.getPropagationPolicy());

        ldap.setPropagationPolicy(policyKey);
        RESOURCE_SERVICE.update(ldap);

        try {
            // 0. create groups on LDAP
            GroupTO group1 = createGroup(GroupITCase.getSample("propagationPolicyOptimizeToLDAP")).getEntity();
            GroupTO group2 = createGroup(GroupITCase.getSample("propagationPolicyOptimizeToLDAP")).getEntity();

            // 1a. create user on LDAP and verify success
            UserCR userCR = UserITCase.getUniqueSample("propagationPolicyOptimizeToLDAP@syncope.apache.org");
            userCR.getAuxClasses().add("minimal group");
            userCR.getPlainAttrs().add(attr("title", "title1"));
            userCR.getMemberships().add(new MembershipTO.Builder(group1.getKey()).build());
            ProvisioningResult<UserTO> created = createUser(userCR);
            assertEquals(RESOURCE_NAME_LDAP, created.getPropagationStatuses().getFirst().getResource());
            assertEquals(ExecStatus.SUCCESS, created.getPropagationStatuses().getFirst().getStatus());

            // 1b. read from LDAP the effective object
            ReconStatus status = RECONCILIATION_SERVICE.status(
                    new ReconQuery.Builder(AnyTypeKind.USER.name(), RESOURCE_NAME_LDAP).
                            anyKey(created.getEntity().getKey()).moreAttrsToGet("ldapGroups").build());
            assertEquals(List.of("title1"), status.getOnResource().getAttr("title").orElseThrow().getValues());
            assertEquals(
                    List.of("cn=" + group1.getName() + ",ou=groups,o=isp"),
                    status.getOnResource().getAttr("ldapGroups").orElseThrow().getValues());

            // 1c. check the generated propagation data
            PagedResult<PropagationTaskTO> tasks = TASK_SERVICE.search(new TaskQuery.Builder(TaskType.PROPAGATION).
                    resource(RESOURCE_NAME_LDAP).
                    anyTypeKind(AnyTypeKind.USER).entityKey(created.getEntity().getKey()).build());
            assertEquals(1, tasks.getSize());

            PropagationData data = POJOHelper.deserialize(
                    tasks.getResult().getFirst().getPropagationData(), PropagationData.class);
            assertNull(data.getAttributeDeltas());

            TASK_SERVICE.delete(TaskType.PROPAGATION, tasks.getResult().getFirst().getKey());

            // 2a. update user on LDAP and verify success
            UserUR userUR = new UserUR.Builder(created.getEntity().getKey()).plainAttr(new AttrPatch.Builder(
                    new Attr.Builder("title").values("title1", "title2").build()).build()).
                    membership(new MembershipUR.Builder(group2.getKey()).build()).
                    build();
            ProvisioningResult<UserTO> updated = updateUser(userUR);
            assertEquals(RESOURCE_NAME_LDAP, updated.getPropagationStatuses().getFirst().getResource());
            assertEquals(ExecStatus.SUCCESS, updated.getPropagationStatuses().getFirst().getStatus());

            // 2b. read from LDAP the effective object
            status = RECONCILIATION_SERVICE.status(
                    new ReconQuery.Builder(AnyTypeKind.USER.name(), RESOURCE_NAME_LDAP).
                            anyKey(created.getEntity().getKey()).moreAttrsToGet("ldapGroups").build());
            assertEquals(
                    Set.of("title1", "title2"),
                    new HashSet<>(status.getOnResource().getAttr("title").orElseThrow().getValues()));
            assertEquals(
                    Set.of("cn=" + group1.getName() + ",ou=groups,o=isp",
                            "cn=" + group2.getName() + ",ou=groups,o=isp"),
                    new HashSet<>(status.getOnResource().getAttr("ldapGroups").orElseThrow().getValues()));

            // 2c. check the generated propagation data
            tasks = TASK_SERVICE.search(new TaskQuery.Builder(TaskType.PROPAGATION).
                    resource(RESOURCE_NAME_LDAP).
                    anyTypeKind(AnyTypeKind.USER).entityKey(created.getEntity().getKey()).build());
            assertEquals(1, tasks.getSize());

            data = POJOHelper.deserialize(tasks.getResult().getFirst().getPropagationData(), PropagationData.class);
            assertNotNull(data.getAttributeDeltas());

            TASK_SERVICE.delete(TaskType.PROPAGATION, tasks.getResult().getFirst().getKey());
        } finally {
            ldap.setPropagationPolicy(null);
            RESOURCE_SERVICE.update(ldap);
        }
    }

    @Test
    public void propagationPolicyOptimizeToScriptedDB() {
        String policyKey = propagationPolicyOptimizeKey();

        ResourceTO db = RESOURCE_SERVICE.read(RESOURCE_NAME_DBSCRIPTED);
        String beforePolicyKey = db.getPropagationPolicy();
        assertNotNull(beforePolicyKey);

        db.setPropagationPolicy(policyKey);

        // 0. create new schema and change resource mapping to include it
        PlainSchemaTO paperformat = new PlainSchemaTO();
        paperformat.setKey("paperformat");
        paperformat.setMultivalue(true);
        SCHEMA_SERVICE.create(SchemaType.PLAIN, paperformat);

        AnyTypeClassTO printer = ANY_TYPE_CLASS_SERVICE.read("minimal printer");
        printer.getPlainSchemas().add(paperformat.getKey());
        ANY_TYPE_CLASS_SERVICE.update(printer);

        Item paperformatItem = new Item();
        paperformatItem.setPurpose(MappingPurpose.PROPAGATION);
        paperformatItem.setIntAttrName("paperformat");
        paperformatItem.setExtAttrName("paperformat");
        db.getProvision(PRINTER).orElseThrow().getMapping().add(paperformatItem);
        RESOURCE_SERVICE.update(db);

        ProvisioningResult<AnyObjectTO> created = null;
        try {
            // 1a. create printer on db and verify success
            created = createAnyObject(AnyObjectITCase.getSample("ppOptimizeToDB"));
            assertEquals(RESOURCE_NAME_DBSCRIPTED, created.getPropagationStatuses().getFirst().getResource());
            assertEquals(ExecStatus.SUCCESS, created.getPropagationStatuses().getFirst().getStatus());

            // 1b. check the generated propagation data
            PagedResult<PropagationTaskTO> tasks = TASK_SERVICE.search(new TaskQuery.Builder(TaskType.PROPAGATION).
                    resource(RESOURCE_NAME_DBSCRIPTED).
                    anyTypeKind(AnyTypeKind.ANY_OBJECT).entityKey(created.getEntity().getKey()).build());
            assertEquals(1, tasks.getSize());

            PropagationData data = POJOHelper.deserialize(
                    tasks.getResult().getFirst().getPropagationData(), PropagationData.class);
            assertNull(data.getAttributeDeltas());

            TASK_SERVICE.delete(TaskType.PROPAGATION, tasks.getResult().getFirst().getKey());

            // 2a. update printer on db and verify success
            AnyObjectUR req = new AnyObjectUR.Builder(created.getEntity().getKey()).plainAttr(new AttrPatch.Builder(
                    new Attr.Builder("paperformat").values("format1", "format2").build()).build()).
                    build();
            ProvisioningResult<AnyObjectTO> updated = updateAnyObject(req);
            assertEquals(RESOURCE_NAME_DBSCRIPTED, updated.getPropagationStatuses().getFirst().getResource());
            assertEquals(ExecStatus.SUCCESS, updated.getPropagationStatuses().getFirst().getStatus());

            // 2b. read from db the effective object
            JdbcTemplate jdbcTemplate = new JdbcTemplate(testDataSource);
            List<String> values = queryForList(jdbcTemplate,
                    MAX_WAIT_SECONDS,
                    "SELECT paper_format FROM testPRINTER_PAPERFORMAT WHERE printer_id=?",
                    String.class,
                    created.getEntity().getKey());
            assertEquals(Set.of("format1", "format2"), new HashSet<>(values));

            // 2c. check the generated propagation data
            tasks = TASK_SERVICE.search(new TaskQuery.Builder(TaskType.PROPAGATION).
                    resource(RESOURCE_NAME_DBSCRIPTED).
                    anyTypeKind(AnyTypeKind.ANY_OBJECT).entityKey(created.getEntity().getKey()).build());
            assertEquals(1, tasks.getSize());

            data = POJOHelper.deserialize(tasks.getResult().getFirst().getPropagationData(), PropagationData.class);
            assertNotNull(data.getAttributeDeltas());

            TASK_SERVICE.delete(TaskType.PROPAGATION, tasks.getResult().getFirst().getKey());

            // 3a. update printer on db and verify success
            req = new AnyObjectUR.Builder(created.getEntity().getKey()).plainAttr(new AttrPatch.Builder(
                    new Attr.Builder("paperformat").values("format1", "format3").build()).build()).
                    build();
            updated = updateAnyObject(req);
            assertEquals(RESOURCE_NAME_DBSCRIPTED, updated.getPropagationStatuses().getFirst().getResource());
            assertEquals(ExecStatus.SUCCESS, updated.getPropagationStatuses().getFirst().getStatus());

            // 3b. read from db the effective object
            values = queryForList(jdbcTemplate,
                    MAX_WAIT_SECONDS,
                    "SELECT paper_format FROM testPRINTER_PAPERFORMAT WHERE printer_id=?",
                    String.class,
                    created.getEntity().getKey());
            assertEquals(Set.of("format1", "format3"), new HashSet<>(values));

            // 3c. check the generated propagation data
            tasks = TASK_SERVICE.search(new TaskQuery.Builder(TaskType.PROPAGATION).
                    resource(RESOURCE_NAME_DBSCRIPTED).
                    anyTypeKind(AnyTypeKind.ANY_OBJECT).entityKey(created.getEntity().getKey()).build());
            assertEquals(1, tasks.getSize());

            data = POJOHelper.deserialize(tasks.getResult().getFirst().getPropagationData(), PropagationData.class);
            assertNotNull(data.getAttributeDeltas());

            TASK_SERVICE.delete(TaskType.PROPAGATION, tasks.getResult().getFirst().getKey());
        } finally {
            Optional.ofNullable(created).map(c -> c.getEntity().getKey()).ifPresent(ANY_OBJECT_SERVICE::delete);

            SCHEMA_SERVICE.delete(SchemaType.PLAIN, "paperformat");

            db.setPropagationPolicy(beforePolicyKey);
            db.getProvision(PRINTER).ifPresent(provision -> provision.getMapping().
                    getItems().removeIf(item -> "paperformat".equals(item.getIntAttrName())));
            RESOURCE_SERVICE.update(db);
        }
    }

    @Test
    public void issueSYNCOPE741() {
        for (int i = 0; i < 3; i++) {
            TASK_SERVICE.execute(new ExecSpecs.Builder().key("1e697572-b896-484c-ae7f-0c8f63fcbc6c").build());
            TASK_SERVICE.execute(new ExecSpecs.Builder().key("316285cc-ae52-4ea2-a33b-7355e189ac3f").build());
        }
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            // ignore
        }

        // check list
        PagedResult<TaskTO> tasks = TASK_SERVICE.search(
                new TaskQuery.Builder(TaskType.PROPAGATION).
                        page(1).size(2).orderBy("operation DESC").details(false).build());
        for (TaskTO item : tasks.getResult()) {
            assertTrue(item.getExecutions().isEmpty());
        }

        tasks = TASK_SERVICE.search(
                new TaskQuery.Builder(TaskType.PROPAGATION).
                        page(1).size(2).orderBy("operation DESC").details(true).build());
        for (TaskTO item : tasks.getResult()) {
            assertFalse(item.getExecutions().isEmpty());
        }

        // check read
        PropagationTaskTO task = TASK_SERVICE.read(TaskType.PROPAGATION, "1e697572-b896-484c-ae7f-0c8f63fcbc6c", false);
        assertNotNull(task);
        assertEquals("1e697572-b896-484c-ae7f-0c8f63fcbc6c", task.getKey());
        assertTrue(task.getExecutions().isEmpty());

        task = TASK_SERVICE.read(TaskType.PROPAGATION, "1e697572-b896-484c-ae7f-0c8f63fcbc6c", true);
        assertNotNull(task);
        assertEquals("1e697572-b896-484c-ae7f-0c8f63fcbc6c", task.getKey());
        assertFalse(task.getExecutions().isEmpty());

        // check list executions
        PagedResult<ExecTO> execs = TASK_SERVICE.listExecutions(new ExecQuery.Builder().
                key("1e697572-b896-484c-ae7f-0c8f63fcbc6c").
                before(OffsetDateTime.now().plusSeconds(30)).
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

            USER_SERVICE.update(userUR);
        }

        // ASC order
        PagedResult<TaskTO> unorderedTasks = TASK_SERVICE.search(
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

        PagedResult<TaskTO> orderedTasks = TASK_SERVICE.search(
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
        orderedTasks = TASK_SERVICE.search(
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
        ResourceTO ldap = RESOURCE_SERVICE.read(RESOURCE_NAME_LDAP);
        try {
            // 1. clone the LDAP resource and add some sensible mappings
            Provision provision = ldap.getProvision(AnyTypeKind.USER.name()).orElse(null);
            assertNotNull(provision);
            provision.getMapping().getItems().removeIf(item -> "mail".equals(item.getExtAttrName()));
            provision.getVirSchemas().clear();

            // Date -> long (JEXL expression) -> string
            Item loginDateForJexlAsLong = new Item();
            loginDateForJexlAsLong.setPurpose(MappingPurpose.PROPAGATION);
            loginDateForJexlAsLong.setIntAttrName("loginDate");
            loginDateForJexlAsLong.setExtAttrName("employeeNumber");
            loginDateForJexlAsLong.setPropagationJEXLTransformer("value.toInstant().toEpochMilli()");
            provision.getMapping().add(loginDateForJexlAsLong);

            // Date -> string (JEXL expression)
            Item loginDateForJexlAsString = new Item();
            loginDateForJexlAsString.setPurpose(MappingPurpose.PROPAGATION);
            loginDateForJexlAsString.setIntAttrName("loginDate");
            loginDateForJexlAsString.setExtAttrName("street");
            loginDateForJexlAsString.setPropagationJEXLTransformer(
                    "value.toInstant().toString().split(\"T\")[0].replace(\"-\", \"\")");
            provision.getMapping().add(loginDateForJexlAsString);

            // Date -> long
            Item loginDateForJavaToLong = new Item();
            loginDateForJavaToLong.setPurpose(MappingPurpose.PROPAGATION);
            loginDateForJavaToLong.setIntAttrName("loginDate");
            loginDateForJavaToLong.setExtAttrName("st");
            loginDateForJavaToLong.getTransformers().add(DateToLongItemTransformer.class.getSimpleName());
            provision.getMapping().add(loginDateForJavaToLong);

            // Date -> date
            Item loginDateForJavaToDate = new Item();
            loginDateForJavaToDate.setPurpose(MappingPurpose.PROPAGATION);
            loginDateForJavaToDate.setIntAttrName("loginDate");
            loginDateForJavaToDate.setExtAttrName("carLicense");
            loginDateForJavaToDate.getTransformers().add(DateToDateItemTransformer.class.getSimpleName());
            provision.getMapping().add(loginDateForJavaToDate);

            ldap.getProvisions().clear();
            ldap.getProvisions().add(provision);
            ldap.setKey(RESOURCE_NAME_LDAP + "1430" + getUUIDString());
            RESOURCE_SERVICE.create(ldap);

            // 2. create user with the new resource assigned
            UserCR createReq = UserITCase.getUniqueSample("syncope1430@syncope.apache.org");
            createReq.getResources().clear();
            createReq.getResources().add(ldap.getKey());
            createReq.getPlainAttrs().removeIf(attr -> "loginDate".equals(attr.getSchema()));
            createReq.getPlainAttrs().add(attr("loginDate", "2019-01-29"));
            UserTO user = createUser(createReq).getEntity();

            // 3. check attributes prepared for propagation
            PagedResult<PropagationTaskTO> tasks = TASK_SERVICE.search(new TaskQuery.Builder(TaskType.PROPAGATION).
                    resource(user.getResources().iterator().next()).
                    anyTypeKind(AnyTypeKind.USER).entityKey(user.getKey()).build());
            assertEquals(1, tasks.getSize());

            Set<Attribute> propagationAttrs = new HashSet<>();
            if (StringUtils.isNotBlank(tasks.getResult().getFirst().getPropagationData())) {
                propagationAttrs.addAll(POJOHelper.deserialize(
                        tasks.getResult().getFirst().getPropagationData(), PropagationData.class).getAttributes());
            }

            OffsetDateTime loginDate = LocalDate.parse(user.getPlainAttr("loginDate").
                    orElseThrow().getValues().getFirst()).
                    atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

            Attribute employeeNumber = AttributeUtil.find("employeeNumber", propagationAttrs);
            assertNotNull(employeeNumber);
            assertEquals(loginDate.toInstant().toEpochMilli(), employeeNumber.getValue().getFirst());

            Attribute street = AttributeUtil.find("street", propagationAttrs);
            assertNotNull(street);
            assertEquals(loginDate.toInstant().toString().split("T")[0].replace("-", ""), street.getValue().getFirst());

            Attribute st = AttributeUtil.find("st", propagationAttrs);
            assertNotNull(st);
            assertEquals(loginDate.toInstant().toEpochMilli(), st.getValue().getFirst());

            Attribute carLicense = AttributeUtil.find("carLicense", propagationAttrs);
            assertNotNull(carLicense);
            assertEquals(DateTimeFormatter.ISO_LOCAL_DATE.format(loginDate.plusDays(1)),
                carLicense.getValue().getFirst());
        } finally {
            try {
                RESOURCE_SERVICE.delete(ldap.getKey());
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

        ResourceTO ldap = RESOURCE_SERVICE.read(RESOURCE_NAME_LDAP);
        UserTO userTO = null;
        try {
            // 1. clone the LDAP resource and add some sensible mappings
            Provision provisionGroup =
                    SerializationUtils.clone(ldap.getProvision(AnyTypeKind.GROUP.name()).orElse(null));
            assertNotNull(provisionGroup);
            provisionGroup.getVirSchemas().clear();

            Provision provisionUser =
                    SerializationUtils.clone(ldap.getProvision(AnyTypeKind.USER.name()).orElse(null));
            assertNotNull(provisionUser);
            provisionUser.getMapping().getItems().removeIf(item -> "mail".equals(item.getExtAttrName()));
            provisionUser.getVirSchemas().clear();

            Item ldapGroups = new Item();
            ldapGroups.setPurpose(MappingPurpose.PROPAGATION);
            ldapGroups.setIntAttrName(schemaTO.getKey());
            ldapGroups.setExtAttrName("ldapGroups");
            provisionUser.getMapping().add(ldapGroups);

            ldap.getProvisions().clear();
            ldap.getProvisions().add(provisionUser);
            ldap.getProvisions().add(provisionGroup);
            ldap.setKey(RESOURCE_NAME_LDAP + "1473" + getUUIDString());
            RESOURCE_SERVICE.create(ldap);

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
            PagedResult<PropagationTaskTO> tasks = TASK_SERVICE.search(new TaskQuery.Builder(TaskType.PROPAGATION).
                    resource(userTO.getResources().iterator().next()).
                    anyTypeKind(AnyTypeKind.USER).entityKey(userTO.getKey()).build());
            assertEquals(1, tasks.getSize());

            ResourceDR resourceDR = new ResourceDR.Builder().key(groupTO.getKey()).
                    action(ResourceDeassociationAction.UNLINK).resource(ldap.getKey()).build();

            GROUP_SERVICE.deassociate(resourceDR);
            GROUP_SERVICE.delete(groupTO.getKey());

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
            USER_SERVICE.update(userUR);

            ConnObject connObject =
                    RESOURCE_SERVICE.readConnObject(ldap.getKey(), AnyTypeKind.USER.name(), userTO.getKey());
            assertNotNull(connObject);
            assertTrue(connObject.getAttr("ldapGroups").isPresent());
            assertEquals(2, connObject.getAttr("ldapGroups").orElseThrow().getValues().size());
        } finally {
            try {
                RESOURCE_SERVICE.delete(ldap.getKey());
                if (userTO != null) {
                    USER_SERVICE.delete(userTO.getKey());
                }
                SCHEMA_SERVICE.delete(SchemaType.PLAIN, schemaTO.getKey());
            } catch (Exception ignore) {
                // ignore
            }
        }
    }

    @Test
    public void issueSYNCOPE1567() {
        ResourceTO ldap = RESOURCE_SERVICE.read(RESOURCE_NAME_LDAP);
        ldap.setKey(RESOURCE_NAME_LDAP + "1567" + getUUIDString());
        try {
            // 1. clone the LDAP resource and add the relationships mapping
            Provision provision = SerializationUtils.clone(ldap.getProvision(AnyTypeKind.USER.name()).orElseThrow());
            assertNotNull(provision);
            provision.getVirSchemas().clear();

            Item relationships = new Item();
            relationships.setPurpose(MappingPurpose.PROPAGATION);
            relationships.setIntAttrName("relationships[neighborhood][PRINTER].model");
            relationships.setExtAttrName("l");
            provision.getMapping().add(relationships);

            ldap.getProvisions().clear();
            ldap.getProvisions().add(provision);
            RESOURCE_SERVICE.create(ldap);

            // 1. create user with relationship and the new resource assigned
            UserCR userCR = UserITCase.getUniqueSample("syncope1567@syncope.apache.org");
            userCR.getRelationships().add(new RelationshipTO.Builder("neighborhood").
                    otherEnd(PRINTER, "fc6dbc3a-6c07-4965-8781-921e7401a4a5").build());
            userCR.getResources().clear();
            userCR.getResources().add(ldap.getKey());

            UserTO userTO = createUser(userCR).getEntity();
            assertNotNull(userTO);
            assertFalse(userTO.getRelationships().isEmpty());

            // 2. check attributes prepared for propagation
            PagedResult<PropagationTaskTO> tasks = TASK_SERVICE.search(new TaskQuery.Builder(TaskType.PROPAGATION).
                    resource(userCR.getResources().iterator().next()).
                    anyTypeKind(AnyTypeKind.USER).entityKey(userTO.getKey()).build());
            assertEquals(1, tasks.getSize());

            Set<Attribute> propagationAttrs = POJOHelper.deserialize(
                    tasks.getResult().getFirst().getPropagationData(), PropagationData.class).getAttributes();
            List<Object> value = Optional.ofNullable(AttributeUtil.find("l", propagationAttrs)).
                    map(Attribute::getValue).orElseThrow();
            assertFalse(CollectionUtils.isEmpty(value));
            assertEquals("Canon MFC8030", value.getFirst().toString());

            // 3. check propagated value
            ConnObject connObject =
                    RESOURCE_SERVICE.readConnObject(ldap.getKey(), AnyTypeKind.USER.name(), userTO.getKey());
            assertNotNull(connObject);
            List<String> values = connObject.getAttr("l").map(Attr::getValues).orElseThrow();
            assertFalse(values.isEmpty());
            assertEquals("Canon MFC8030", values.getFirst());
        } finally {
            try {
                RESOURCE_SERVICE.delete(ldap.getKey());
            } catch (Exception ignore) {
                // ignore
            }
        }
    }

    @Test
    public void issueSYNCOPE1605() throws ParseException {
        ResourceTO ldap = RESOURCE_SERVICE.read(RESOURCE_NAME_LDAP);
        try {
            // 1. clone the LDAP resource and add some sensible mappings
            Provision provisionGroup =
                    SerializationUtils.clone(ldap.getProvision(AnyTypeKind.GROUP.name()).orElse(null));
            assertNotNull(provisionGroup);
            provisionGroup.getVirSchemas().clear();
            provisionGroup.getMapping().getItems().clear();

            Item item = new Item();
            item.setConnObjectKey(true);
            item.setIntAttrName("name");
            item.setExtAttrName("description");
            item.setPurpose(MappingPurpose.BOTH);

            provisionGroup.getMapping().setConnObjectKeyItem(item);
            provisionGroup.getMapping().setConnObjectLink("'cn=' + originalName + ',ou=groups,o=isp'");

            ldap.getProvisions().clear();
            ldap.getProvisions().add(provisionGroup);

            ldap.setKey(RESOURCE_NAME_LDAP + "1605" + getUUIDString());
            RESOURCE_SERVICE.create(ldap);

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
            PagedResult<PropagationTaskTO> tasks = TASK_SERVICE.search(new TaskQuery.Builder(TaskType.PROPAGATION).
                    resource(ldap.getKey()).anyTypeKind(AnyTypeKind.GROUP).entityKey(groupTO.getKey()).build());
            assertEquals(1, tasks.getSize());
            assertEquals(ResourceOperation.CREATE, tasks.getResult().getFirst().getOperation());
            assertEquals(ExecStatus.SUCCESS.name(), tasks.getResult().getFirst().getLatestExecStatus());

            ConnObject beforeConnObject =
                    RESOURCE_SERVICE.readConnObject(ldap.getKey(), AnyTypeKind.GROUP.name(), groupTO.getKey());

            GroupUR groupUR = new GroupUR();
            groupUR.setKey(groupTO.getKey());

            groupUR.getPlainAttrs().add(attrAddReplacePatch("originalName", "new" + originalName));
            groupTO = updateGroup(groupUR).getEntity();

            tasks = TASK_SERVICE.search(new TaskQuery.Builder(TaskType.PROPAGATION).
                    resource(ldap.getKey()).anyTypeKind(AnyTypeKind.GROUP).entityKey(groupTO.getKey()).
                    orderBy("start DESC").build());
            assertEquals(2, tasks.getSize());
            assertEquals(ResourceOperation.UPDATE, tasks.getResult().getFirst().getOperation());
            assertEquals(ExecStatus.SUCCESS.name(), tasks.getResult().getFirst().getLatestExecStatus());

            ConnObject afterConnObject =
                    RESOURCE_SERVICE.readConnObject(ldap.getKey(), AnyTypeKind.GROUP.name(), groupTO.getKey());
            assertNotEquals(afterConnObject.getAttr(Name.NAME).orElseThrow().getValues().getFirst(),
                    beforeConnObject.getAttr(Name.NAME).orElseThrow().getValues().getFirst());
            assertTrue(afterConnObject.getAttr(Name.NAME).orElseThrow().getValues().getFirst().
                    contains("new" + originalName));
        } finally {
            try {
                RESOURCE_SERVICE.delete(ldap.getKey());
            } catch (Exception ignore) {
                // ignore
            }
        }
    }

    @Test
    public void issueSYNCOPE1751() {
        // 1. Create a Group with a resource assigned
        GroupTO groupTO = createGroup(
                new GroupCR.Builder(SyncopeConstants.ROOT_REALM, "SYNCOPEGROUP1751-" + getUUIDString()).
                        resource(RESOURCE_NAME_LDAP).build()).getEntity();
        // 2. Create a user
        String username = "SYNCOPEUSER1751" + getUUIDString();
        UserTO userTO = createUser(
                new UserCR.Builder(SyncopeConstants.ROOT_REALM, username).plainAttrs(
                        new Attr.Builder("userId").value(username + "@syncope.org").build(),
                        new Attr.Builder("fullname").value(username).build(),
                        new Attr.Builder("surname").value(username).build()).
                        build()).getEntity();
        // 3. Update the user assigning the group previously created -> group-based provisioning
        userTO = updateUser(
                new UserUR.Builder(userTO.getKey()).
                        membership(new MembershipUR.Builder(groupTO.getKey()).build()).
                        build()).getEntity();
        // since the resource is flagged to generate random pwd must populate the password on effective create on the
        // resource, even if it is an update on Syncope
        PagedResult<TaskTO> propTasks = TASK_SERVICE.search(new TaskQuery.Builder(TaskType.PROPAGATION).
                resource(RESOURCE_NAME_LDAP).anyTypeKind(AnyTypeKind.USER).entityKey(userTO.getKey()).build());
        assertFalse(propTasks.getResult().isEmpty());
        assertEquals(1, propTasks.getSize());
        PropagationData propagationData = POJOHelper.deserialize(
                PropagationTaskTO.class.cast(propTasks.getResult().getFirst()).getPropagationData(),
                PropagationData.class);
        assertTrue(propagationData.getAttributes().stream().
                anyMatch(a -> OperationalAttributes.PASSWORD_NAME.equals(a.getName())));
    }
}

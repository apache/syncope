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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.LiveSyncTaskTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.ExecSpecs;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.core.provisioning.java.pushpull.KafkaInboundActions;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.core.reference.TestLiveSyncDeltaMapper;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class LiveSyncITCase extends AbstractITCase {

    private static final String ACCOUNT_TOPIC = "account-provisioning";

    private static final String GROUP_TOPIC = "group-provisioning";

    @BeforeAll
    public static void testLiveSyncImplementationSetup() {
        ImplementationTO liveSDM = null;
        try {
            liveSDM = IMPLEMENTATION_SERVICE.read(
                    IdMImplementationType.LIVE_SYNC_DELTA_MAPPER, TestLiveSyncDeltaMapper.class.getSimpleName());
        } catch (SyncopeClientException e) {
            if (e.getType().getResponseStatus() == Response.Status.NOT_FOUND) {
                liveSDM = new ImplementationTO();
                liveSDM.setKey(TestLiveSyncDeltaMapper.class.getSimpleName());
                liveSDM.setEngine(ImplementationEngine.JAVA);
                liveSDM.setType(IdMImplementationType.LIVE_SYNC_DELTA_MAPPER);
                liveSDM.setBody(TestLiveSyncDeltaMapper.class.getName());
                Response response = IMPLEMENTATION_SERVICE.create(liveSDM);
                liveSDM = IMPLEMENTATION_SERVICE.read(
                        liveSDM.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                assertNotNull(liveSDM);
            }
        }
        assertNotNull(liveSDM);

        ImplementationTO kafkaInboundActions = null;
        try {
            kafkaInboundActions = IMPLEMENTATION_SERVICE.read(
                    IdMImplementationType.INBOUND_ACTIONS, KafkaInboundActions.class.getSimpleName());
        } catch (SyncopeClientException e) {
            if (e.getType().getResponseStatus() == Response.Status.NOT_FOUND) {
                kafkaInboundActions = new ImplementationTO();
                kafkaInboundActions.setKey(KafkaInboundActions.class.getSimpleName());
                kafkaInboundActions.setEngine(ImplementationEngine.JAVA);
                kafkaInboundActions.setType(IdMImplementationType.INBOUND_ACTIONS);
                kafkaInboundActions.setBody(KafkaInboundActions.class.getName());
                Response response = IMPLEMENTATION_SERVICE.create(kafkaInboundActions);
                kafkaInboundActions = IMPLEMENTATION_SERVICE.read(
                        kafkaInboundActions.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                assertNotNull(kafkaInboundActions);
            }
        }
        assertNotNull(kafkaInboundActions);
    }

    private static KafkaProducer<String, String> createProducer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092");
        props.put(ProducerConfig.CLIENT_ID_CONFIG, LiveSyncITCase.class.getSimpleName());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        return new KafkaProducer<>(props);
    }

    private static KafkaConsumer<String, String> createConsumer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092");
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, LiveSyncITCase.class.getSimpleName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, LiveSyncITCase.class.getSimpleName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(
                props,
                Serdes.String().deserializer(),
                Serdes.String().deserializer());
        consumer.subscribe(List.of(ACCOUNT_TOPIC, GROUP_TOPIC));
        return consumer;
    }

    private static boolean found(final SyncDeltaType syncDeltaType, final String username) {
        Mutable<Boolean> found = new MutableObject<>(false);
        try (KafkaConsumer<String, String> consumer = createConsumer()) {
            consumer.poll(Duration.ofSeconds(10)).forEach(record -> {
                if (ACCOUNT_TOPIC.equals(record.topic())) {
                    SyncDeltaType sdt = null;
                    String uid = null;
                    try {
                        JsonNode syncDelta = JSON_MAPPER.readTree(record.value());
                        if (syncDelta.has("deltaType")) {
                            sdt = SyncDeltaType.valueOf(syncDelta.get("deltaType").asText());
                        }
                        if (syncDelta.has("uid") && syncDelta.get("uid").has("value")) {
                            uid = syncDelta.get("uid").get("value").iterator().next().asText();
                        }
                    } catch (IOException e) {
                        fail(e.getMessage(), e);
                    }

                    found.setValue(syncDeltaType == sdt && username.equals(uid));
                }
            });
        }
        return found.getValue();
    }

    @Test
    public void crud() {
        UserCR userCR = UserITCase.getUniqueSample("kafka@syncope.apache.org");
        userCR.getResources().add(RESOURCE_NAME_KAFKA);
        ProvisioningResult<UserTO> created = createUser(userCR);
        assertEquals(RESOURCE_NAME_KAFKA, created.getPropagationStatuses().getFirst().getResource());
        assertEquals(ExecStatus.SUCCESS, created.getPropagationStatuses().getFirst().getStatus());

        assertTrue(found(SyncDeltaType.CREATE, created.getEntity().getUsername()));

        UserUR req = new UserUR();
        req.setKey(created.getEntity().getKey());
        req.getPlainAttrs().add(attrAddReplacePatch("firstname", "Updated"));
        ProvisioningResult<UserTO> updated = updateUser(req);
        assertEquals(RESOURCE_NAME_KAFKA, updated.getPropagationStatuses().getFirst().getResource());
        assertEquals(ExecStatus.SUCCESS, updated.getPropagationStatuses().getFirst().getStatus());

        assertTrue(found(SyncDeltaType.UPDATE, updated.getEntity().getUsername()));

        deleteUser(created.getEntity().getKey());

        assertTrue(found(SyncDeltaType.DELETE, updated.getEntity().getUsername()));
    }

    @Test
    public void liveSync() {
        // 1. create and execute the live sync task
        LiveSyncTaskTO task = new LiveSyncTaskTO();
        task.setName("Test LiveSync");
        task.setDestinationRealm(SyncopeConstants.ROOT_REALM);
        task.setResource(RESOURCE_NAME_KAFKA);
        task.setLiveSyncDeltaMapper(TestLiveSyncDeltaMapper.class.getSimpleName());
        task.getActions().add(KafkaInboundActions.class.getSimpleName());
        task.setPerformCreate(true);
        task.setPerformUpdate(true);
        task.setPerformDelete(true);

        Response response = TASK_SERVICE.create(TaskType.LIVE_SYNC, task);
        LiveSyncTaskTO actual = getObject(response.getLocation(), TaskService.class, LiveSyncTaskTO.class);
        assertNotNull(actual);

        task = TASK_SERVICE.read(TaskType.LIVE_SYNC, actual.getKey(), true);
        assertNotNull(task);
        assertEquals(actual.getKey(), task.getKey());
        assertNotNull(actual.getJobDelegate());
        assertEquals(actual.getLiveSyncDeltaMapper(), task.getLiveSyncDeltaMapper());

        TASK_SERVICE.execute(new ExecSpecs.Builder().key(task.getKey()).build());

        try {
            // 2. send event to the queue
            String email = "liveSync" + getUUIDString() + "@syncope.apache.org";
            try (KafkaProducer<String, String> producer = createProducer()) {
                producer.send(new ProducerRecord<>(
                        ACCOUNT_TOPIC,
                        UUID.randomUUID().toString(),
                        """
                    {
                        "username": "%s",
                        "email": "%s",
                        "givenName": "LiveSync",
                        "lastName": "LiveSync",
                        "type": "CREATE_OR_UPDATE"
                    }                    
                    """.
                                formatted(email, email)));
            }

            // 3. find the user created in Syncope
            UserTO user = await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(
                    () -> {
                        try {
                            return USER_SERVICE.read(email);
                        } catch (SyncopeClientException e) {
                            return null;
                        }
                    }, Objects::nonNull);
            assertEquals(email, user.getPlainAttr("email").orElseThrow().getValues().getFirst());
            assertEquals(email, user.getPlainAttr("userId").orElseThrow().getValues().getFirst());
            assertEquals("LiveSync", user.getPlainAttr("firstname").orElseThrow().getValues().getFirst());
            assertEquals("LiveSync", user.getPlainAttr("surname").orElseThrow().getValues().getFirst());
            assertEquals("LiveSync LiveSync", user.getPlainAttr("fullname").orElseThrow().getValues().getFirst());

            await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(
                    () -> TASK_SERVICE.read(TaskType.LIVE_SYNC, actual.getKey(), true).getExecutions(),
                    execs -> execs.size() == 1
                    && execs.stream().allMatch(exec -> ExecStatus.SUCCESS.name().equals(exec.getStatus())));

            // 4. stop live syncing
            assertTrue(TASK_SERVICE.getJob(task.getKey()).isRunning());

            TASK_SERVICE.actionJob(task.getKey(), JobAction.STOP);

            await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).
                    until(() -> !TASK_SERVICE.getJob(actual.getKey()).isRunning());

            // 5. send new event to the queue, but no further executions
            String groupName = "liveSync" + getUUIDString();
            try (KafkaProducer<String, String> producer = createProducer()) {
                producer.send(new ProducerRecord<>(
                        GROUP_TOPIC,
                        UUID.randomUUID().toString(),
                        """
                    {
                        "name": "%s",
                        "type": "CREATE_OR_UPDATE"
                    }                    
                    """.
                                formatted(groupName)));
            }

            // 6. start again live syncing and find the new group in Syncope
            TASK_SERVICE.actionJob(task.getKey(), JobAction.START);

            await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(
                    () -> {
                        try {
                            return GROUP_SERVICE.read(groupName);
                        } catch (SyncopeClientException e) {
                            return null;
                        }
                    }, Objects::nonNull);

            await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(
                    () -> TASK_SERVICE.read(TaskType.LIVE_SYNC, actual.getKey(), true).getExecutions(),
                    execs -> execs.size() == 2
                    && execs.stream().allMatch(exec -> ExecStatus.SUCCESS.name().equals(exec.getStatus())));
        } finally {
            // finally stop live syncing
            TASK_SERVICE.actionJob(task.getKey(), JobAction.STOP);
        }
    }
}

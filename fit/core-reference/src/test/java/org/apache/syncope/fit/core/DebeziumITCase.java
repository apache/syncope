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

import static org.assertj.core.api.Assumptions.assumeThatCollection;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.LiveSyncTaskTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.ExecSpecs;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.core.provisioning.java.pushpull.KafkaInboundActions;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.core.reference.DebeziumLiveSyncDeltaMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class DebeziumITCase extends AbstractITCase {

    @BeforeAll
    public static void debeziumSetup() {
        assumeThatCollection(System.getProperties().keySet()).anyMatch("CONNECT_IP"::equals);

        ImplementationTO liveSDM = null;
        try {
            liveSDM = IMPLEMENTATION_SERVICE.read(
                    IdMImplementationType.LIVE_SYNC_DELTA_MAPPER, DebeziumLiveSyncDeltaMapper.class.getSimpleName());
        } catch (SyncopeClientException e) {
            if (e.getType().getResponseStatus() == Response.Status.NOT_FOUND) {
                liveSDM = new ImplementationTO();
                liveSDM.setKey(DebeziumLiveSyncDeltaMapper.class.getSimpleName());
                liveSDM.setEngine(ImplementationEngine.JAVA);
                liveSDM.setType(IdMImplementationType.LIVE_SYNC_DELTA_MAPPER);
                liveSDM.setBody(DebeziumLiveSyncDeltaMapper.class.getName());
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

        ConnInstanceTO connector = SerializationUtils.clone(
                CONNECTOR_SERVICE.read("01938bdf-7ac6-7149-a103-3ec9e74cc824", null));
        connector.getConf().stream().filter(p -> "bootstrapServers".equals(p.getSchema().getName())).findFirst().
                ifPresent(p -> {
                    p.getValues().clear();
                    p.getValues().add(System.getProperty("KAFKA_BOOTSTRAP_SERVERS"));
                });
        connector.getConf().stream().filter(p -> "accountTopic".equals(p.getSchema().getName())).findFirst().
                ifPresent(p -> {
                    p.getValues().clear();
                    p.getValues().add("dbserver1.inventory.customers");
                });
        connector.getConf().removeIf(p -> "groupTopic".equals(p.getSchema().getName()));

        CONNECTOR_SERVICE.update(connector);
    }

    @Test
    void liveSync() {
        assumeThatCollection(System.getProperties().keySet()).anyMatch("CONNECT_IP"::equals);

        // 0. datasource for Debezium
        DataSource dataSource = new DriverManagerDataSource("jdbc:mysql://" + System.getProperty("MYSQL_IP") + ":3306/"
                + "inventory?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8", "syncope", "syncope");
        JdbcTemplate debezium = new JdbcTemplate(dataSource);

        // 1. create and execute the live sync task
        LiveSyncTaskTO task = new LiveSyncTaskTO();
        task.setName("Debezium LiveSync");
        task.setDestinationRealm(SyncopeConstants.ROOT_REALM);
        task.setResource(RESOURCE_NAME_KAFKA);
        task.setLiveSyncDeltaMapper(DebeziumLiveSyncDeltaMapper.class.getSimpleName());
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

        // 2. create the Debezium connector
        WebClient.create("http://" + System.getProperty("CONNECT_IP") + ":8083/connectors").
                accept(MediaType.APPLICATION_JSON).
                type(MediaType.APPLICATION_JSON).
                post("""
                     {
                       "name": "inventory-connector",
                       "config": {
                         "connector.class": "io.debezium.connector.mysql.MySqlConnector",
                         "tasks.max": "1",
                         "database.hostname": "mysql",
                         "database.port": "3306",
                         "database.user": "debezium",
                         "database.password": "dbz",
                         "database.server.id": "184054",
                         "topic.prefix": "dbserver1",
                         "database.include.list": "inventory",
                         "schema.history.internal.kafka.bootstrap.servers": "kafka:9092",
                         "schema.history.internal.kafka.topic": "schema-changes.inventory"
                       }
                     }                     
                     """);

        try {
            // 3. check that the initial customers were pulled
            UserTO user = await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(
                    () -> {
                        try {
                            return USER_SERVICE.read("1004");
                        } catch (SyncopeClientException e) {
                            return null;
                        }
                    }, Objects::nonNull);
            assertEquals("annek@noanswer.org", user.getPlainAttr("email").orElseThrow().getValues().getFirst());
            assertEquals("annek@noanswer.org", user.getPlainAttr("userId").orElseThrow().getValues().getFirst());
            assertEquals("Anne", user.getPlainAttr("firstname").orElseThrow().getValues().getFirst());
            assertEquals("Kretchmar", user.getPlainAttr("surname").orElseThrow().getValues().getFirst());
            assertEquals("Anne Kretchmar", user.getPlainAttr("fullname").orElseThrow().getValues().getFirst());

            // 4. create new customer
            debezium.update("INSERT INTO customers VALUES (1005,?,?,?)", "John", "Doe", "j.doe@syncope.apache.org");

            user = await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(
                    () -> {
                        try {
                            return USER_SERVICE.read("1005");
                        } catch (SyncopeClientException e) {
                            return null;
                        }
                    }, Objects::nonNull);
            assertEquals("j.doe@syncope.apache.org", user.getPlainAttr("email").orElseThrow().getValues().getFirst());
            assertEquals("j.doe@syncope.apache.org", user.getPlainAttr("userId").orElseThrow().getValues().getFirst());
            assertEquals("John", user.getPlainAttr("firstname").orElseThrow().getValues().getFirst());
            assertEquals("Doe", user.getPlainAttr("surname").orElseThrow().getValues().getFirst());
            assertEquals("John Doe", user.getPlainAttr("fullname").orElseThrow().getValues().getFirst());

            // 5. update existing customer
            debezium.update("UPDATE customers SET email=? WHERE id=?", "annek@syncope.apache.org", 1004);

            await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).
                    pollInterval(1, TimeUnit.SECONDS).until(() -> "annek@syncope.apache.org".
                    equals(USER_SERVICE.read("1004").getPlainAttr("email").orElseThrow().getValues().getFirst()));
            user = USER_SERVICE.read("1004");
            assertEquals("annek@syncope.apache.org", user.getPlainAttr("userId").orElseThrow().getValues().getFirst());
            assertEquals("Anne", user.getPlainAttr("firstname").orElseThrow().getValues().getFirst());
            assertEquals("Kretchmar", user.getPlainAttr("surname").orElseThrow().getValues().getFirst());
            assertEquals("Anne Kretchmar", user.getPlainAttr("fullname").orElseThrow().getValues().getFirst());

            // 6. delete customer
            debezium.update("DELETE FROM customers WHERE id=?", 1005);

            SyncopeClientException sce = await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).
                    pollInterval(1, TimeUnit.SECONDS).until(() -> {
                try {
                    USER_SERVICE.read("1005");
                    return null;
                } catch (SyncopeClientException e) {
                    return e;
                }
            }, Objects::nonNull);
            assertEquals(ClientExceptionType.NotFound, sce.getType());
        } finally {
            // finally stop live syncing
            TASK_SERVICE.actionJob(task.getKey(), JobAction.STOP);
        }
    }
}

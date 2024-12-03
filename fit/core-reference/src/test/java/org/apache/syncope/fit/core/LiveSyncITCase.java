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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.fit.AbstractITCase;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.junit.jupiter.api.Test;

public class LiveSyncITCase extends AbstractITCase {

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
        consumer.subscribe(List.of("account-provisioning", "group-provisioning"));
        return consumer;
    }

    private static boolean found(final SyncDeltaType syncDeltaType, final String username) {
        AtomicBoolean found = new AtomicBoolean(false);
        try (KafkaConsumer<String, String> consumer = createConsumer()) {
            consumer.poll(Duration.ofSeconds(10)).forEach(record -> {
                if ("account-provisioning".equals(record.topic())) {
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

                    found.set(syncDeltaType == sdt && username.equals(uid));
                }
            });
        }
        return found.get();
    }

    @Test
    public void crud() {
        UserCR userCR = UserITCase.getUniqueSample("kafka@syncope.apache.org");
        userCR.getResources().add(RESOURCE_NAME_KAFKA);
        ProvisioningResult<UserTO> created = createUser(userCR);
        assertEquals(RESOURCE_NAME_KAFKA, created.getPropagationStatuses().get(0).getResource());
        assertEquals(ExecStatus.SUCCESS, created.getPropagationStatuses().get(0).getStatus());

        assertTrue(found(SyncDeltaType.CREATE, created.getEntity().getUsername()));

        UserUR req = new UserUR();
        req.setKey(created.getEntity().getKey());
        req.getPlainAttrs().add(attrAddReplacePatch("firstname", "Updated"));
        ProvisioningResult<UserTO> updated = updateUser(req);
        assertEquals(RESOURCE_NAME_KAFKA, updated.getPropagationStatuses().get(0).getResource());
        assertEquals(ExecStatus.SUCCESS, updated.getPropagationStatuses().get(0).getStatus());

        assertTrue(found(SyncDeltaType.UPDATE, updated.getEntity().getUsername()));

        deleteUser(created.getEntity().getKey());

        assertTrue(found(SyncDeltaType.DELETE, updated.getEntity().getUsername()));
    }
}

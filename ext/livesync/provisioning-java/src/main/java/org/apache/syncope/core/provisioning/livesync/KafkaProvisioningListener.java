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
package org.apache.syncope.core.provisioning.livesync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.SyncDeltaBuilder;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

public class KafkaProvisioningListener {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaProvisioningListener.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @KafkaListener(id = "provisioningRegex", topicPattern = "dbserver1.inventory.*")
    public void pollTable(final @Payload String payload,
                          final @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String primaryKey) {
       SyncDeltaBuilder syncDeltaBuilder = new SyncDeltaBuilder();
        try {
            JsonNode root = mapper.readTree(payload);
            Map<String, Object> after = mapper.convertValue(root.path("after"),
                    new TypeReference<Map<String, Object>>() {
                    });
            Map<String, Object> before = mapper.convertValue(root.path("before"),
                    new TypeReference<Map<String, Object>>() {
                    });
            Map<String, Object> source = mapper.convertValue(root.path("source"),
                    new TypeReference<Map<String, Object>>() {
                    });
            Map<String, Object> keyJson = mapper.readValue(primaryKey, new TypeReference<Map<String, Object>>() {
            });
            final String operation = root.path("op").asText();
            LOG.debug("Map key {}", keyJson);
            LOG.debug("Map after {}", after);
            LOG.debug("Map source {}", source);
            LOG.debug("Map before {}", before);
            LOG.debug("Operation {}", operation);
            if (!"products".equals(source.get("table"))) {
                return;
            }
            ConnectorObjectBuilder connectorBuilder = new ConnectorObjectBuilder()
                    .setObjectClass(ObjectClass.ACCOUNT);
            final Uid uid = new Uid(keyJson.entrySet().iterator().next().getValue().toString());
            syncDeltaBuilder.setUid(uid);
            connectorBuilder.addAttribute(uid);
            final SyncToken token = new SyncToken(source.get("ts_ms").toString());
            syncDeltaBuilder.setToken(token);
            if ("d".equals(operation)) {
                syncDeltaBuilder.setDeltaType(SyncDeltaType.DELETE);
                LOG.debug("This is my syncDeltaBuilder {}",
                        syncDeltaBuilder.build());
                return;
            }
            final Name name = new Name(after.get("name").toString());
            connectorBuilder.addAttribute(name);
            after.forEach((k, v) -> connectorBuilder.addAttribute(AttributeBuilder.build(k, v)));
            if ("c".equals(operation) || "r".equals(operation)) {
                syncDeltaBuilder.setDeltaType(SyncDeltaType.CREATE);
            } else if ("u".equals(operation)) {
                syncDeltaBuilder.setDeltaType(SyncDeltaType.UPDATE);
            }
            LOG.debug("This is my syncDeltaBuilder {}",
                    syncDeltaBuilder.setObject(connectorBuilder.build()).build());
        } catch (JsonProcessingException e) {
            LOG.error("Debezium payload not in json format {}", payload);
        }
    }
}

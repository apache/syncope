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

    private final SyncDeltaBuilder syncDeltaBuilder = new SyncDeltaBuilder();

    @KafkaListener(id = "provisioningRegex", topicPattern = "dbserver1.inventory.*")
    public void pollTable(final @Payload DebeziumMessage payload,
                          final @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) Map<String, Object> primaryKey) {
        
        ConnectorObjectBuilder connectorBuilder = new ConnectorObjectBuilder().setObjectClass(ObjectClass.ACCOUNT);
        final String uidValue = primaryKey.entrySet().iterator().next().getValue().toString();
        final Uid uid = new Uid(uidValue);
        final Name name = new Name(uidValue);
        final SyncToken token = new SyncToken(payload.getSource().get("ts_ms").toString());
        syncDeltaBuilder.setUid(uid);
        connectorBuilder.addAttribute(uid);
        connectorBuilder.addAttribute(name);
        syncDeltaBuilder.setToken(token);
        if ("d".equals(payload.getOp())) {
            syncDeltaBuilder.setDeltaType(SyncDeltaType.DELETE);
            LOG.debug("This is my syncDeltaBuilder {}",
                    syncDeltaBuilder.build());
            return;
        }
        payload.getAfter().forEach((k, v) -> connectorBuilder.addAttribute(AttributeBuilder.build(k, v)));
        if ("c".equals(payload.getOp()) || "r".equals(payload.getOp())) {
            syncDeltaBuilder.setDeltaType(SyncDeltaType.CREATE);
        } else if ("u".equals(payload.getOp())) {
            syncDeltaBuilder.setDeltaType(SyncDeltaType.UPDATE);
        }
        LOG.debug("This is my syncDeltaBuilder {}",
                syncDeltaBuilder.setObject(connectorBuilder.build()).build());
    }
}

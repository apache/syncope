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

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.provisioning.api.pushpull.stream.SyncopeStreamPullExecutor;
import org.apache.syncope.core.provisioning.java.pushpull.stream.LiveSyncConnector;
import org.apache.syncope.core.provisioning.java.pushpull.stream.LiveSyncStreamPullJobDelegate;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncDeltaBuilder;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class KafkaProvisioningListener {

    protected static final Logger LOG = LoggerFactory.getLogger(KafkaProvisioningListener.class);

    @Autowired
    protected AnyTypeDAO anyTypeDAO;

    @Autowired
    protected ThreadPoolTaskExecutor livesyncTaskExecutorAsyncExecutor;

    @KafkaListener(id = "provisioningRegex", topics = "dbserver1.inventory.users")
    public void pollTable(
            final @Payload DebeziumMessage payload,
            final @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) Map<String, Object> primaryKey) {

        if ("r".equals(payload.getOp())) {
            //skip debezium read message
            return;
        }
        LOG.debug("my pool size : {}", livesyncTaskExecutorAsyncExecutor.getPoolSize());

        String uidValue = primaryKey.entrySet().iterator().next().getValue().toString();
        Uid uid = new Uid(uidValue);

        SyncDeltaBuilder syncDeltaBuilder = new SyncDeltaBuilder();
        syncDeltaBuilder.setUid(uid);
        syncDeltaBuilder.setToken(new SyncToken(payload.getSource().get("ts_ms").toString()));

        ConnectorObjectBuilder connectorBuilder = new ConnectorObjectBuilder().
                setObjectClass(new ObjectClass(AnyTypeKind.USER.name()));
        connectorBuilder.addAttribute(uid);
        connectorBuilder.addAttribute(new Name(uidValue));

        Map<String, Object> obj;
        if ("d".equals(payload.getOp())) {
            syncDeltaBuilder.setDeltaType(SyncDeltaType.DELETE);

            obj = payload.getBefore();
        } else {
            syncDeltaBuilder.setDeltaType(SyncDeltaType.CREATE_OR_UPDATE);

            obj = payload.getAfter();
        }

        obj.forEach((k, v) -> connectorBuilder.addAttribute(AttributeBuilder.build(k, v)));

        SyncDelta delta = syncDeltaBuilder.setObject(connectorBuilder.build()).build();
        LOG.debug("This is my syncDelta {}", delta);

        livesyncTaskExecutorAsyncExecutor.submit((Callable<Void>) () -> {
            pull(delta, primaryKey.entrySet().iterator().next().getKey(),
                    obj.keySet().stream().collect(Collectors.toList()));
            return null;
        });
    }

    protected void pull(
            final SyncDelta delta,
            final String idName,
            final List<String> valueName) {

        AnyType anyType = anyTypeDAO.findUser();

        PullTaskTO pullTask = new PullTaskTO();
        pullTask.setDestinationRealm(SyncopeConstants.ROOT_REALM);
        pullTask.setRemediation(false);
        pullTask.setMatchingRule(MatchingRule.UPDATE);
        pullTask.setUnmatchingRule(UnmatchingRule.PROVISION);

        LiveSyncConnector connector = new LiveSyncConnector(delta);

        SyncopeStreamPullExecutor executor = (SyncopeStreamPullExecutor) ApplicationContextProvider.getBeanFactory().
                createBean(LiveSyncStreamPullJobDelegate.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);

        AuthContextUtils.callAsAdmin(SyncopeConstants.MASTER_DOMAIN,
                () -> executor.pull(
                        anyType,
                        idName,
                        valueName,
                        ConflictResolutionAction.IGNORE,
                        null,
                        connector,
                        pullTask));
    }
}

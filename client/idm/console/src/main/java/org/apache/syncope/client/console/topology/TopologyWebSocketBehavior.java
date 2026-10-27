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
package org.apache.syncope.client.console.topology;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.wicket.ws.BasePageWebSocketBehavior;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.wicket.protocol.ws.api.WebSocketRequestHandler;
import org.apache.wicket.protocol.ws.api.message.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class TopologyWebSocketBehavior extends BasePageWebSocketBehavior.OnMessageChild {

    private static final long serialVersionUID = -1653665542635275551L;

    protected static final Logger LOG = LoggerFactory.getLogger(TopologyWebSocketBehavior.class);

    protected static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    protected static final String CONNECTOR_TEST_TIMEOUT_PARAMETER = "connector.test.timeout";

    protected static final String RESOURCE_TEST_TIMEOUT_PARAMETER = "resource.test.timeout";

    protected final ServiceOps serviceOps;

    protected final ConfParamOps confParamOps;

    protected final ConnectorRestClient connectorRestClient;

    protected final ResourceRestClient resourceRestClient;

    protected final Map<String, StatusMessage> connectors = Collections.synchronizedMap(new HashMap<>());

    protected final Set<String> runningConnCheck = Collections.synchronizedSet(new HashSet<>());

    protected final Map<String, StatusMessage> resources = Collections.synchronizedMap(new HashMap<>());

    protected final Set<String> runningResCheck = Collections.synchronizedSet(new HashSet<>());

    protected String coreAddress;

    protected String domain;

    protected String jwt;

    protected Integer connectorTestTimeout;

    protected Integer resourceTestTimeout;

    public TopologyWebSocketBehavior(
            final ServiceOps serviceOps,
            final ConfParamOps confParamOps,
            final ConnectorRestClient connectorRestClient,
            final ResourceRestClient resourceRestClient) {

        this.serviceOps = serviceOps;
        this.confParamOps = confParamOps;
        this.connectorRestClient = connectorRestClient;
        this.resourceRestClient = resourceRestClient;

        coreAddress = serviceOps.get(NetworkService.Type.CORE).getAddress();
        domain = SyncopeConsoleSession.get().getDomain();
        jwt = SyncopeConsoleSession.get().getJWT();

        connectorTestTimeout = confParamOps.get(domain, CONNECTOR_TEST_TIMEOUT_PARAMETER, 30, Integer.class);
        resourceTestTimeout = confParamOps.get(domain, RESOURCE_TEST_TIMEOUT_PARAMETER, 30, Integer.class);
    }

    @Override
    protected void onMessage(final WebSocketRequestHandler handler, final TextMessage message) {
        try {
            JsonNode obj = MAPPER.readTree(message.getText());
            String target = obj.get("target").asString();
            switch (Topology.SupportedOperation.valueOf(obj.get("kind").asString())) {
                case CHECK_CONNECTOR ->
                    checkConnection(
                            new ConnectorChecker(target),
                            connectorTestTimeout, connectors, runningConnCheck, handler);

                case CHECK_RESOURCE ->
                    checkConnection(
                            new ResourceChecker(target),
                            resourceTestTimeout, resources, runningResCheck, handler);

                case ADD_ENDPOINT -> {
                    String source = obj.get("source").asString();
                    String scope = obj.get("scope").asString();
                    handler.appendJavaScript("addEndpoint('%s', '%s', '%s');".formatted(source, target, scope));
                }

                default -> {
                }
            }
        } catch (Exception e) {
            LOG.error("Error managing websocket message", e);
        }
    }

    public boolean connCheckDone(final Collection<String> connectors) {
        return this.connectors.keySet().containsAll(connectors);
    }

    public boolean resCheckDone(final Collection<String> resources) {
        return this.resources.keySet().containsAll(resources);
    }

    protected void checkConnection(
            final Checker checker,
            final Integer timeout,
            final Map<String, StatusMessage> responses,
            final Set<String> running,
            final WebSocketRequestHandler handler) {

        StatusMessage response = responses.get(checker.key);
        handler.push(MAPPER.valueToTree(response == null
                ? new StatusMessage(TopologyNode.Status.UNKNOWN, checker.key)
                : response).toString());

        if (response == null && running.add(checker.key)) {
            SyncopeConsoleSession session = SyncopeConsoleSession.get();
            session.execute(() -> {
                try {
                    StatusMessage result = checkWithTimeout(checker, timeout, session);
                    responses.put(checker.key, result);
                    handler.push(MAPPER.valueToTree(result).toString());
                } finally {
                    running.remove(checker.key);
                }
            });
        }
    }

    protected StatusMessage checkWithTimeout(
            final Checker checker,
            final Integer timeout,
            final SyncopeConsoleSession session) {

        Future<Boolean> task = null;
        try {
            boolean reachable;
            if (timeout == null || timeout < 0) {
                reachable = checker.call();
            } else {
                task = session.execute(checker);
                reachable = task.get(timeout, TimeUnit.SECONDS);
            }
            return new StatusMessage(reachable
                    ? TopologyNode.Status.REACHABLE
                    : TopologyNode.Status.UNREACHABLE, checker.key);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new StatusMessage(TopologyNode.Status.UNREACHABLE, checker.key);
        } catch (TimeoutException e) {
            LOG.warn("Connection with {} timed out", checker.key);
            return new StatusMessage(TopologyNode.Status.UNREACHABLE, checker.key);
        } catch (Exception e) {
            LOG.warn("Error checking connection for {}", checker.key, e);
            return new StatusMessage(TopologyNode.Status.FAILURE, checker.key);
        } finally {
            if (task != null && !task.isDone()) {
                task.cancel(true);
            }
        }
    }

    protected record StatusMessage(TopologyNode.Status status, String target) implements Serializable {

    }

    protected abstract static class Checker implements Callable<Boolean> {

        protected final String key;

        Checker(final String key) {
            this.key = key;
        }
    }

    protected class ConnectorChecker extends Checker {

        ConnectorChecker(final String key) {
            super(key);
        }

        @Override
        public Boolean call() throws IOException {
            return connectorRestClient.check(coreAddress, domain, jwt, key);
        }
    }

    protected class ResourceChecker extends Checker {

        ResourceChecker(final String key) {
            super(key);
        }

        @Override
        public Boolean call() throws IOException {
            return resourceRestClient.check(coreAddress, domain, jwt, key);
        }
    }
}

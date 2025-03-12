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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.wicket.protocol.ws.api.WebSocketBehavior;
import org.apache.wicket.protocol.ws.api.WebSocketRequestHandler;
import org.apache.wicket.protocol.ws.api.message.TextMessage;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopologyWebSocketBehavior extends WebSocketBehavior {

    private static final long serialVersionUID = -1653665542635275551L;

    protected static final Logger LOG = LoggerFactory.getLogger(TopologyWebSocketBehavior.class);

    protected static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    protected static final String CONNECTOR_TEST_TIMEOUT_PARAMETER = "connector.test.timeout";

    protected static final String RESOURCE_TEST_TIMEOUT_PARAMETER = "resource.test.timeout";

    protected static void timeoutHandlingConnectionChecker(
            final Checker checker,
            final Integer timeout,
            final Map<String, String> responses,
            final Set<String> running) {

        String response = null;
        try {
            if (timeout == null || timeout < 0) {
                LOG.debug("No timeouts for resource connection checking ... ");
                response = checker.call();
            } else if (timeout > 0) {
                LOG.debug("Timeouts provided for resource connection checking ... ");
                response = SyncopeConsoleSession.get().execute(checker).get(timeout, TimeUnit.SECONDS);
            }
        } catch (InterruptedException | TimeoutException e) {
            LOG.warn("Connection with {} timed out", checker.key);
            response = String.format("{ \"status\": \"%s\", \"target\": \"%s\"}",
                    TopologyNode.Status.UNREACHABLE, checker.key);
        } catch (Exception e) {
            LOG.error("Unexpected exception connecting to {}", checker.key, e);
            response = String.format("{ \"status\": \"%s\", \"target\": \"%s\"}",
                    TopologyNode.Status.FAILURE, checker.key);
        }

        if (response != null) {
            responses.put(checker.key, response);
        }

        running.remove(checker.key);
    }

    @SpringBean
    protected ServiceOps serviceOps;

    @SpringBean
    protected ConfParamOps confParamOps;

    @SpringBean
    protected ConnectorRestClient connectorRestClient;

    @SpringBean
    protected ResourceRestClient resourceRestClient;

    protected final Map<String, String> connectors = Collections.synchronizedMap(new HashMap<>());

    protected final Set<String> runningConnCheck = Collections.synchronizedSet(new HashSet<>());

    protected final Map<String, String> resources = Collections.synchronizedMap(new HashMap<>());

    protected final Set<String> runningResCheck = Collections.synchronizedSet(new HashSet<>());

    protected String coreAddress;

    protected String domain;

    protected String jwt;

    protected Integer connectorTestTimeout = null;

    protected Integer resourceTestTimeout = null;

    public TopologyWebSocketBehavior() {
        coreAddress = serviceOps.get(NetworkService.Type.CORE).getAddress();
        domain = SyncopeConsoleSession.get().getDomain();
        jwt = SyncopeConsoleSession.get().getJWT();

        // Handling with timeout as per SYNCOPE-1379
        try {
            connectorTestTimeout = confParamOps.get(domain, CONNECTOR_TEST_TIMEOUT_PARAMETER, null, Integer.class);
            resourceTestTimeout = confParamOps.get(domain, RESOURCE_TEST_TIMEOUT_PARAMETER, null, Integer.class);
        } catch (Exception e) {
            LOG.debug("No {} or {} conf parameters found",
                    CONNECTOR_TEST_TIMEOUT_PARAMETER, RESOURCE_TEST_TIMEOUT_PARAMETER, e);
        }
    }

    @Override
    protected void onMessage(final WebSocketRequestHandler handler, final TextMessage message) {
        try {
            JsonNode obj = MAPPER.readTree(message.getText());
            switch (Topology.SupportedOperation.valueOf(obj.get("kind").asText())) {
                case CHECK_CONNECTOR:
                    String ckey = obj.get("target").asText();

                    if (connectors.containsKey(ckey)) {
                        handler.push(connectors.get(ckey));
                    } else {
                        handler.push(String.format(
                                "{ \"status\": \"%s\", \"target\": \"%s\"}", TopologyNode.Status.UNKNOWN, ckey));
                    }

                    if (runningConnCheck.contains(ckey)) {
                        LOG.debug("Running connection check for connector {}", ckey);
                    } else {
                        try {
                            SyncopeConsoleSession.get().execute(() -> timeoutHandlingConnectionChecker(
                                    new ConnectorChecker(ckey), connectorTestTimeout, connectors, runningConnCheck));

                            runningConnCheck.add(ckey);
                        } catch (Exception e) {
                            LOG.error("Unexpected error", e);
                        }
                    }
                    break;

                case CHECK_RESOURCE:
                    String rkey = obj.get("target").asText();

                    if (resources.containsKey(rkey)) {
                        handler.push(resources.get(rkey));
                    } else {
                        handler.push(String.format(
                                "{ \"status\": \"%s\", \"target\": \"%s\"}", TopologyNode.Status.UNKNOWN, rkey));
                    }

                    if (runningResCheck.contains(rkey)) {
                        LOG.debug("Running connection check for resource {}", rkey);
                    } else {
                        try {
                            SyncopeConsoleSession.get().execute(() -> timeoutHandlingConnectionChecker(
                                    new ResourceChecker(rkey), resourceTestTimeout, resources, runningResCheck));

                            runningResCheck.add(rkey);
                        } catch (Exception e) {
                            LOG.error("Unexpected error", e);
                        }
                    }
                    break;

                case ADD_ENDPOINT:
                    handler.appendJavaScript(String.format("addEndpoint('%s', '%s', '%s');",
                            obj.get("source").asText(),
                            obj.get("target").asText(),
                            obj.get("scope").asText()));
                    break;

                default:
            }
        } catch (IOException e) {
            LOG.error("Error managing websocket message", e);
        }
    }

    public boolean connCheckDone(final Collection<String> connectors) {
        return this.connectors.keySet().containsAll(connectors);
    }

    public boolean resCheckDone(final Collection<String> resources) {
        return this.resources.keySet().containsAll(resources);
    }

    private abstract class Checker implements Callable<String> {

        protected final String key;

        Checker(final String key) {
            this.key = key;
        }
    }

    private class ConnectorChecker extends Checker {

        ConnectorChecker(final String key) {
            super(key);
        }

        @Override
        public String call() {
            try {
                return String.format("{ \"status\": \"%s\", \"target\": \"%s\"}",
                        connectorRestClient.check(coreAddress, domain, jwt, key)
                        ? TopologyNode.Status.REACHABLE : TopologyNode.Status.UNREACHABLE, key);
            } catch (Exception e) {
                LOG.warn("Error checking connection for {}", key, e);
                return String.format("{ \"status\": \"%s\", \"target\": \"%s\"}",
                        TopologyNode.Status.FAILURE, key);
            }
        }
    }

    private class ResourceChecker extends Checker {

        ResourceChecker(final String key) {
            super(key);
        }

        @Override
        public String call() {
            try {
                return String.format("{ \"status\": \"%s\", \"target\": \"%s\"}",
                        resourceRestClient.check(coreAddress, domain, jwt, key)
                        ? TopologyNode.Status.REACHABLE : TopologyNode.Status.UNREACHABLE, key);
            } catch (Exception e) {
                LOG.warn("Error checking connection for {}", key, e);
                return String.format("{ \"status\": \"%s\", \"target\": \"%s\"}",
                        TopologyNode.Status.FAILURE, key);
            }
        }
    }
}

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
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.wicket.Application;
import org.apache.wicket.Session;
import org.apache.wicket.ThreadContext;
import org.apache.wicket.protocol.ws.api.WebSocketBehavior;
import org.apache.wicket.protocol.ws.api.WebSocketRequestHandler;
import org.apache.wicket.protocol.ws.api.message.TextMessage;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopologyWebSocketBehavior extends WebSocketBehavior {

    private static final long serialVersionUID = -1653665542635275551L;

    private static final Logger LOG = LoggerFactory.getLogger(TopologyWebSocketBehavior.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @SpringBean
    private ConfParamOps confParamOps;

    private final Map<String, String> resources = Collections.<String, String>synchronizedMap(new HashMap<>());

    private static final String CONNECTOR_TEST_TIMEOUT_PARAMETER = "connector.test.timeout";

    private Integer connectorTestTimeout = null;

    private static final String RESOURCE_TEST_TIMEOUT_PARAMETER = "resource.test.timeout";

    private Integer resourceTestTimeout = null;

    private final Set<String> runningResCheck = Collections.synchronizedSet(new HashSet<>());

    private final Map<String, String> connectors = Collections.<String, String>synchronizedMap(new HashMap<>());

    private final Set<String> runningConnCheck = Collections.synchronizedSet(new HashSet<>());

    public TopologyWebSocketBehavior() {
        // Handling with timeout as per SYNCOPE-1379
        try {
            connectorTestTimeout = confParamOps.get(SyncopeConsoleSession.get().getDomain(),
                    CONNECTOR_TEST_TIMEOUT_PARAMETER, null, Integer.class);
            resourceTestTimeout = confParamOps.get(SyncopeConsoleSession.get().getDomain(),
                    RESOURCE_TEST_TIMEOUT_PARAMETER, null, Integer.class);
        } catch (Exception e) {
            LOG.debug("No {} or {} conf parameters found",
                    CONNECTOR_TEST_TIMEOUT_PARAMETER, RESOURCE_TEST_TIMEOUT_PARAMETER, e);
        }
    }

    @Override
    protected void onMessage(final WebSocketRequestHandler handler, final TextMessage message) {
        try {
            JsonNode obj = OBJECT_MAPPER.readTree(message.getText());

            switch (Topology.SupportedOperation.valueOf(obj.get("kind").asText())) {
                case CHECK_CONNECTOR:
                    final String ckey = obj.get("target").asText();

                    if (connectors.containsKey(ckey)) {
                        handler.push(connectors.get(ckey));
                    } else {
                        handler.push(String.format(
                                "{ \"status\": \"%s\", \"target\": \"%s\"}", TopologyNode.Status.UNKNOWN, ckey));
                    }

                    if (runningConnCheck.contains(ckey)) {
                        LOG.debug("Running connection check for connector {}", ckey);
                    } else {
                        runningConnCheck.add(ckey);
                    }

                    try {
                        SyncopeConsoleSession.get().execute(new ConnCheck(ckey));
                    } catch (Exception e) {
                        LOG.error("Unexpected error", e);
                    }

                    break;
                case CHECK_RESOURCE:
                    final String rkey = obj.get("target").asText();

                    if (resources.containsKey(rkey)) {
                        handler.push(resources.get(rkey));
                    } else {
                        handler.push(String.format(
                                "{ \"status\": \"%s\", \"target\": \"%s\"}", TopologyNode.Status.UNKNOWN, rkey));
                    }

                    if (runningResCheck.contains(rkey)) {
                        LOG.debug("Running connection check for resource {}", rkey);
                    } else {
                        runningResCheck.add(rkey);
                    }

                    try {
                        SyncopeConsoleSession.get().execute(new ResCheck(rkey));
                    } catch (Exception e) {
                        LOG.error("Unexpected error", e);
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
            LOG.error("Eror managing websocket message", e);
        }
    }

    public boolean connCheckDone(final Collection<String> connectors) {
        return this.connectors.keySet().containsAll(connectors);
    }

    public boolean resCheckDone(final Collection<String> resources) {
        return this.resources.keySet().containsAll(resources);
    }

    private void timeoutHandlingConnectionChecker(
            final Checker checker,
            final Integer timeout,
            final Map<String, String> responses,
            final Set<String> running) {
        String res = null;
        try {
            if (timeout == null) {
                LOG.debug("No timeouts for resource connection checking ... ");
                res = SyncopeConsoleSession.get().execute(checker).get();
            } else if (timeout > 0) {
                LOG.debug("Timeouts provided for resource connection checking ... ");
                res = SyncopeConsoleSession.get().execute(checker).get(timeout, TimeUnit.SECONDS);
            }
        } catch (InterruptedException | TimeoutException e) {
            LOG.warn("Connection with {} timed out", checker.getKey());
            res = String.format("{ \"status\": \"%s\", \"target\": \"%s\"}",
                    TopologyNode.Status.UNREACHABLE, checker.getKey());
        } catch (Exception e) {
            LOG.error("Unexpected exception conneting to {}", checker.getKey(), e);
            res = String.format("{ \"status\": \"%s\", \"target\": \"%s\"}",
                    TopologyNode.Status.FAILURE, checker.getKey());
        }

        if (res != null) {
            responses.put(checker.getKey(), res);
        }

        running.remove(checker.getKey());
    }

    class ConnCheck implements Runnable {

        private final String key;

        private final Application application;

        private final Session session;

        ConnCheck(final String key) {
            this.key = key;
            this.application = Application.get();
            this.session = Session.exists() ? Session.get() : null;
        }

        @Override
        public void run() {
            ThreadContext.setApplication(application);
            ThreadContext.setSession(session);

            try {
                timeoutHandlingConnectionChecker(
                        new ConnectorChecker(key, this.application),
                        connectorTestTimeout,
                        connectors,
                        runningConnCheck);
            } finally {
                ThreadContext.detach();
            }
        }
    }

    class ResCheck implements Runnable {

        private final String key;

        private final Application application;

        private final Session session;

        ResCheck(final String key) {
            this.key = key;
            this.application = Application.get();
            this.session = Session.exists() ? Session.get() : null;
        }

        @Override
        public void run() {
            ThreadContext.setApplication(application);
            ThreadContext.setSession(session);

            try {
                timeoutHandlingConnectionChecker(
                        new ResourceChecker(key, this.application),
                        resourceTestTimeout,
                        resources,
                        runningResCheck);
            } finally {
                ThreadContext.detach();
            }
        }
    }

    abstract static class Checker implements Callable<String> {

        protected final String key;

        protected final Application application;

        protected final Session session;

        Checker(final String key, final Application application) {
            this.key = key;
            this.application = application;
            this.session = Session.exists() ? Session.get() : null;
        }

        public String getKey() {
            return key;
        }

        @Override
        public abstract String call() throws Exception;
    }

    class ConnectorChecker extends Checker {

        ConnectorChecker(final String key, final Application application) {
            super(key, application);
        }

        @Override
        public String call() throws Exception {
            ThreadContext.setApplication(application);
            ThreadContext.setSession(session);

            try {
                final ConnInstanceTO connector = ConnectorRestClient.read(key);
                return String.format("{ \"status\": \"%s\", \"target\": \"%s\"}",
                        ConnectorRestClient.check(connector).getLeft()
                        ? TopologyNode.Status.REACHABLE : TopologyNode.Status.UNREACHABLE, key);
            } catch (Exception e) {
                LOG.warn("Error checking connection for {}", key, e);
                return String.format("{ \"status\": \"%s\", \"target\": \"%s\"}",
                        TopologyNode.Status.FAILURE, key);
            } finally {
                ThreadContext.detach();
            }
        }
    }

    class ResourceChecker extends Checker {

        ResourceChecker(final String key, final Application application) {
            super(key, application);
        }

        @Override
        public String call() throws Exception {
            ThreadContext.setApplication(application);
            ThreadContext.setSession(session);

            try {
                final ResourceTO resource = ResourceRestClient.read(key);
                return String.format("{ \"status\": \"%s\", \"target\": \"%s\"}",
                        ResourceRestClient.check(resource).getLeft()
                        ? TopologyNode.Status.REACHABLE : TopologyNode.Status.UNREACHABLE, key);
            } catch (Exception e) {
                LOG.warn("Error checking connection for {}", key, e);
                return String.format("{ \"status\": \"%s\", \"target\": \"%s\"}",
                        TopologyNode.Status.FAILURE,
                        key);
            } finally {
                ThreadContext.detach();
            }
        }
    }
}

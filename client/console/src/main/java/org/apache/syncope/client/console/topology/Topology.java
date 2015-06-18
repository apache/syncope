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

import static org.apache.syncope.client.console.topology.TopologyNode.Status.FAILURE;
import static org.apache.syncope.client.console.topology.TopologyNode.Status.REACHABLE;
import static org.apache.syncope.client.console.topology.TopologyNode.Status.UNREACHABLE;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.ResourceModal.ResourceCreateEvent;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.Entitlement;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.ws.api.WebSocketBehavior;
import org.apache.wicket.protocol.ws.api.WebSocketRequestHandler;
import org.apache.wicket.protocol.ws.api.message.TextMessage;
import org.apache.wicket.util.time.Duration;

public class Topology extends BasePage {

    private static final long serialVersionUID = -1100228004207271272L;

    private static final String CONNECTOR_SERVER_LOCATION_PREFIX = "connid://";

    private final int origX = 3100;

    private final int origY = 2800;

    private static final int RESOURCE_MODAL_WIN_HEIGHT = 700;

    private static final int RESOURCE_MODAL_WIN_WIDTH = 1000;

    final ModalWindow modal;

    private final LoadableDetachableModel<List<ResourceTO>> resModel
            = new LoadableDetachableModel<List<ResourceTO>>() {

                private static final long serialVersionUID = 5275935387613157431L;

                @Override
                protected List<ResourceTO> load() {
                    final List<ResourceTO> result = resourceRestClient.getAll();
                    return result;
                }
            };

    private final LoadableDetachableModel<Pair<List<ConnInstanceTO>, List<ConnInstanceTO>>> connModel
            = new LoadableDetachableModel<Pair<List<ConnInstanceTO>, List<ConnInstanceTO>>>() {

                private static final long serialVersionUID = 5275935387613157432L;

                @Override
                protected Pair<List<ConnInstanceTO>, List<ConnInstanceTO>> load() {
                    final List<ConnInstanceTO> level1 = new ArrayList<>();
                    final List<ConnInstanceTO> level2 = new ArrayList<>();

                    for (ConnInstanceTO conn : connectorRestClient.getAllConnectors()) {
                        if (conn.getLocation().startsWith(CONNECTOR_SERVER_LOCATION_PREFIX)) {
                            level2.add(conn);
                        } else {
                            level1.add(conn);
                        }
                    }

                    return Pair.of(level1, level2);
                }
            };

    private final LoadableDetachableModel<List<URI>> csModel = new LoadableDetachableModel<List<URI>>() {

        private static final long serialVersionUID = 5275935387613157433L;

        @Override
        protected List<URI> load() {
            final List<URI> locations = new ArrayList<>();

            for (String location : SyncopeConsoleSession.get().getSyncopeTO().getConnIdLocations()) {
                if (location.startsWith(CONNECTOR_SERVER_LOCATION_PREFIX)) {
                    locations.add(URI.create(location));
                }
            }

            return locations;
        }
    };

    private enum SupportedOperation {

        CHECK_RESOURCE,
        CHECK_CONNECTOR,
        ADD_ENDPOINT;

    }

    public Topology() {
        modal = new ModalWindow("modal");
        add(modal);

        modal.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        modal.setInitialHeight(RESOURCE_MODAL_WIN_HEIGHT);
        modal.setInitialWidth(RESOURCE_MODAL_WIN_WIDTH);
        modal.setCookieName("resource-modal");

        add(new WebSocketBehavior() {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onMessage(final WebSocketRequestHandler handler, final TextMessage message) {
                try {
                    final ObjectMapper mapper = new ObjectMapper();
                    final JsonNode obj = mapper.readTree(message.getText());

                    switch (SupportedOperation.valueOf(obj.get("kind").asText())) {
                        case CHECK_CONNECTOR:
                            try {
                                final ConnInstanceTO connector = connectorRestClient.read(obj.get("target").asLong());
                                handler.push(String.format("{ \"status\": \"%s\", \"target\": \"%s\"}",
                                        connectorRestClient.check(connector) ? REACHABLE : UNREACHABLE,
                                        obj.get("target").asLong()));
                            } catch (Exception e) {
                                handler.push(String.format("{ \"status\": \"%s\", \"target\": \"%s\"}",
                                        FAILURE,
                                        obj.get("target").asLong()));
                            }
                            break;
                        case CHECK_RESOURCE:
                            try {
                                final ResourceTO resource = resourceRestClient.read(obj.get("target").asText());
                                handler.push(String.format("{ \"status\": \"%s\", \"target\": \"%s\"}",
                                        connectorRestClient.check(resource) ? REACHABLE : UNREACHABLE,
                                        obj.get("target").asText()));
                            } catch (Exception e) {
                                handler.push(String.format("{ \"status\": \"%s\", \"target\": \"%s\"}",
                                        FAILURE,
                                        obj.get("target").asText()));
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

                } catch (IOException ex) {
                    Logger.getLogger(Topology.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        });

        // -----------------------------------------
        // Add Zoom panel
        // -----------------------------------------
        final ActionLinksPanel zoomActionPanel = new ActionLinksPanel("zoom", new Model<String>(), getPageReference());
        add(zoomActionPanel);

        zoomActionPanel.add(new ActionLink() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                target.appendJavaScript("zoomIn($('#drawing')[0]);");
            }

        }, ActionLink.ActionType.ZOOM_IN, Entitlement.RESOURCE_LIST).add(new ActionLink() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                target.appendJavaScript("zoomOut($('#drawing')[0]);");
            }
        }, ActionLink.ActionType.ZOOM_OUT, Entitlement.RESOURCE_LIST);
        // -----------------------------------------

        // -----------------------------------------
        // Add Syncope (root topologynode)
        // -----------------------------------------
        final TopologyNode syncopeTopologyNode = new TopologyNode("Syncope", "Syncope", TopologyNode.Kind.SYNCOPE);
        syncopeTopologyNode.setX(origX);
        syncopeTopologyNode.setY(origY);

        final URI uri = WebClient.client(SyncopeConsoleSession.get().getService(SyncopeService.class)).getBaseURI();
        syncopeTopologyNode.setHost(uri.getHost());
        syncopeTopologyNode.setPort(uri.getPort());

        add(topologyNodePanel("syncope", syncopeTopologyNode));

        final Map<Serializable, Map<Serializable, TopologyNode>> connections = new HashMap<>();
        final Map<Serializable, TopologyNode> syncopeConnections = new HashMap<>();
        connections.put(syncopeTopologyNode.getKey(), syncopeConnections);

        // required to retrieve parent positions
        final Map<String, TopologyNode> servers = new HashMap<>();
        final Map<Long, TopologyNode> connectors = new HashMap<>();
        // -----------------------------------------

        // -----------------------------------------
        // Add Connector Servers
        // -----------------------------------------
        final ListView<URI> connectorServers = new ListView<URI>("connectorServers", csModel) {

            private static final long serialVersionUID = 6978621871488360380L;

            private final int size = csModel.getObject().size() + 1;

            @Override
            protected void populateItem(final ListItem<URI> item) {
                int kx = size >= 4 ? 800 : (200 * size);

                int x = (int) Math.round(origX + kx * Math.cos(Math.PI + Math.PI * (item.getIndex() + 1) / size));
                int y = (int) Math.round(origY + 100 * Math.sin(Math.PI + Math.PI * (item.getIndex() + 1) / size));

                final URI location = item.getModelObject();
                final String url = location.toASCIIString();

                final TopologyNode topologynode = new TopologyNode(url, url, TopologyNode.Kind.CONNECTOR_SERVER);

                topologynode.setHost(location.getHost());
                topologynode.setPort(location.getPort());
                topologynode.setX(x);
                topologynode.setY(y);

                servers.put(String.class.cast(topologynode.getKey()), topologynode);

                item.add(topologyNodePanel("cs", topologynode));

                syncopeConnections.put(url, topologynode);
                connections.put(url, new HashMap<Serializable, TopologyNode>());
            }
        };

        connectorServers.setOutputMarkupId(true);
        add(connectorServers);
        // -----------------------------------------

        // -----------------------------------------
        // Add Connector Intances (first level)
        // -----------------------------------------
        final ListView<ConnInstanceTO> conn1
                = new ListView<ConnInstanceTO>("conn1", connModel.getObject().getLeft()) {

                    private static final long serialVersionUID = 6978621871488360381L;

                    private final int size = connModel.getObject().getLeft().size() + 1;

                    @Override
                    protected void populateItem(final ListItem<ConnInstanceTO> item) {
                        int kx = size >= 6 ? 800 : (130 * size);

                        int x = (int) Math.round(origX + kx * Math.cos(Math.PI * (item.getIndex() + 1) / size));
                        int y = (int) Math.round(origY + 100 * Math.sin(Math.PI * (item.getIndex() + 1) / size));

                        final ConnInstanceTO conn = item.getModelObject();
                        final TopologyNode topologynode = new TopologyNode(
                                Long.valueOf(conn.getKey()), conn.getDisplayName(), TopologyNode.Kind.CONNECTOR);
                        topologynode.setConnectinDisplayName(conn.getBundleName());
                        topologynode.setX(x);
                        topologynode.setY(y);

                        connectors.put(Long.class.cast(topologynode.getKey()), topologynode);

                        item.add(topologyNodePanel("conn", topologynode));

                        if (conn.getLocation().startsWith(CONNECTOR_SERVER_LOCATION_PREFIX)) {
                            final Map<Serializable, TopologyNode> remoteConnections;

                            if (connections.containsKey(conn.getLocation())) {
                                remoteConnections = connections.get(conn.getLocation());
                            } else {
                                remoteConnections = new HashMap<>();
                                connections.put(conn.getLocation(), remoteConnections);
                            }
                            remoteConnections.put(conn.getKey(), topologynode);
                        } else {
                            syncopeConnections.put(conn.getKey(), topologynode);
                        }
                    }
                };

        conn1.setOutputMarkupId(true);
        add(conn1);
            // -----------------------------------------

        // -----------------------------------------
        // Add Connector Intances (second level)
        // -----------------------------------------
        final ListView<ConnInstanceTO> conn2
                = new ListView<ConnInstanceTO>("conn2", connModel.getObject().getRight()) {

                    private static final long serialVersionUID = 6978621871488360381L;

                    private final int size = connModel.getObject().getRight().size() + 1;

                    @Override
                    protected void populateItem(final ListItem<ConnInstanceTO> item) {
                        final ConnInstanceTO conn = item.getModelObject();

                        final TopologyNode parent = servers.get(conn.getLocation());

                        int kx = size >= 6 ? 800 : (130 * size);

                        int x = (int) Math.round((parent == null ? origX : parent.getX())
                                + kx * Math.cos(Math.PI + Math.PI * (item.getIndex() + 1) / size));
                        int y = (int) Math.round((parent == null ? origY : parent.getY())
                                + 100 * Math.sin(Math.PI + Math.PI * (item.getIndex() + 1) / size));

                        final TopologyNode topologynode = new TopologyNode(
                                Long.valueOf(conn.getKey()), conn.getDisplayName(), TopologyNode.Kind.CONNECTOR);
                        topologynode.setConnectinDisplayName(conn.getBundleName());
                        topologynode.setX(x);
                        topologynode.setY(y);

                        connectors.put(Long.class.cast(topologynode.getKey()), topologynode);

                        item.add(topologyNodePanel("conn", topologynode));

                        if (conn.getLocation().startsWith(CONNECTOR_SERVER_LOCATION_PREFIX)) {
                            final Map<Serializable, TopologyNode> remoteConnections;

                            if (connections.containsKey(conn.getLocation())) {
                                remoteConnections = connections.get(conn.getLocation());
                            } else {
                                remoteConnections = new HashMap<>();
                                connections.put(conn.getLocation(), remoteConnections);
                            }
                            remoteConnections.put(conn.getKey(), topologynode);
                        } else {
                            syncopeConnections.put(conn.getKey(), topologynode);
                        }
                    }
                };

        conn2.setOutputMarkupId(true);
        add(conn2);
            // -----------------------------------------

        // -----------------------------------------
        // Add Resources
        // -----------------------------------------
        final List<Long> connToBeProcessed = new ArrayList<>();
        for (ResourceTO resourceTO : resModel.getObject()) {
            final TopologyNode topologynode = new TopologyNode(
                    resourceTO.getKey(), resourceTO.getKey(), TopologyNode.Kind.RESOURCE);
            topologynode.setX(origX);
            topologynode.setY(origY);

            final Map<Serializable, TopologyNode> remoteConnections;

            if (connections.containsKey(resourceTO.getConnector())) {
                remoteConnections = connections.get(resourceTO.getConnector());
            } else {
                remoteConnections = new HashMap<>();
                connections.put(resourceTO.getConnector(), remoteConnections);
            }

            remoteConnections.put(topologynode.getKey(), topologynode);

            if (!connToBeProcessed.contains(resourceTO.getConnector())) {
                connToBeProcessed.add(resourceTO.getConnector());
            }
        }

        final ListView<Long> resources = new ListView<Long>("resources", connToBeProcessed) {

            private static final long serialVersionUID = 697862187148836038L;

            @Override
            protected void populateItem(final ListItem<Long> item) {
                final Long connectorId = item.getModelObject();

                final ListView<TopologyNode> innerListView = new ListView<TopologyNode>("resources",
                        new ArrayList<>(connections.get(connectorId).values())) {

                            private static final long serialVersionUID = 1L;

                            private final int size = getModelObject().size() + 1;

                            @Override
                            protected void populateItem(final ListItem<TopologyNode> item) {
                                final TopologyNode topologynode = item.getModelObject();
                                final TopologyNode parent = connectors.get(connectorId);

                                final double k;

                                if (parent == null || parent.getY() < syncopeTopologyNode.getY()) {
                                    k = Math.PI;
                                } else {
                                    k = 0.0;
                                }

                                int kx = size >= 16 ? 800 : (48 * size);
                                int ky = size < 4 ? 100 : size < 6 ? 350 : 750;

                                int x = (int) Math.round((parent == null ? origX : parent.getX())
                                        + kx * Math.cos(k + Math.PI * (item.getIndex() + 1) / size));
                                int y = (int) Math.round((parent == null ? origY : parent.getY())
                                        + ky * Math.sin(k + Math.PI * (item.getIndex() + 1) / size));

                                topologynode.setX(x);
                                topologynode.setY(y);

                                item.add(topologyNodePanel("res", topologynode));
                            }
                        };

                innerListView.setOutputMarkupId(true);
                item.add(innerListView);
            }
        };

        resources.setOutputMarkupId(true);
        add(resources);
        // -----------------------------------------

        // -----------------------------------------
        // Create connections
        // -----------------------------------------
        final WebMarkupContainer jsPlace = new WebMarkupContainer("jsPlace");
        jsPlace.setOutputMarkupId(true);
        add(jsPlace);

        jsPlace.add(new Behavior() {

            private static final long serialVersionUID = 2661717818979056044L;

            @Override
            public void renderHead(final Component component, final IHeaderResponse response) {
                final StringBuilder jsPlumbConf = new StringBuilder();
                jsPlumbConf.append(String.format(Locale.US, "activate(%.2f);", 0.68f));

                for (String str : createConnections(connections)) {
                    jsPlumbConf.append(str);
                }

                response.render(OnDomReadyHeaderItem.forScript(jsPlumbConf.toString()));
            }
        });

        jsPlace.add(new AbstractAjaxTimerBehavior(Duration.seconds(2)) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onTimer(final AjaxRequestTarget target) {
                target.appendJavaScript("checkConnection()");

                if (getUpdateInterval().seconds() < 60.0) {
                    setUpdateInterval(Duration.seconds(60));
                }
            }
        });
        // -----------------------------------------
    }

    private List<String> createConnections(final Map<Serializable, Map<Serializable, TopologyNode>> targets) {
        List<String> list = new ArrayList<>();

        for (Map.Entry<Serializable, Map<Serializable, TopologyNode>> source : targets.entrySet()) {
            for (Map.Entry<Serializable, TopologyNode> target : source.getValue().entrySet()) {
                list.add(String.format("connect('%s','%s','%s');",
                        source.getKey(),
                        target.getKey(),
                        target.getValue().getKind()));
            }
        }
        return list;
    }

    private Panel topologyNodePanel(final String id, final TopologyNode node) {

        final Panel panel = new TopologyNodePanel(id, node, getPageReference(), modal);
        panel.setMarkupId(String.valueOf(node.getKey()));
        panel.setOutputMarkupId(true);

        panel.add(new Behavior() {

            private static final long serialVersionUID = 1L;

            @Override
            public void renderHead(final Component component, final IHeaderResponse response) {
                response.render(OnDomReadyHeaderItem.forScript(String.format("setPosition('%s', %d, %d)",
                        node.getKey(), node.getX(), node.getY())));
            }
        });

        return panel;
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        super.onEvent(event);

        if (event.getPayload() instanceof ResourceCreateEvent) {
            final ResourceCreateEvent resourceCreateEvent = ResourceCreateEvent.class.cast(event.getPayload());
            resourceCreateEvent.getTarget().appendJavaScript(String.format(
                    "window.Wicket.WebSocket.send('"
                    + "{\"kind\":\"%s\",\"target\":\"%s\",\"source\":\"%s\",\"scope\":\"%s\"}"
                    + "');",
                    SupportedOperation.ADD_ENDPOINT,
                    resourceCreateEvent.getResourceTO().getKey(),
                    resourceCreateEvent.getResourceTO().getConnector(),
                    TopologyNode.Kind.RESOURCE));
        }
    }

    @Override
    public String getAjaxIndicatorMarkupId() {
        return StringUtils.EMPTY;
    }
}

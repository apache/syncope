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

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.wizards.resources.AbstractResourceWizardBuilder.CreateEvent;
import org.apache.syncope.client.console.rest.BaseRestClient;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.util.time.Duration;

public class Topology extends BasePage {

    private static final long serialVersionUID = -1100228004207271272L;

    public static final String CONNECTOR_SERVER_LOCATION_PREFIX = "connid://";

    public static final String ROOT_NAME = "Syncope";

    private final ResourceRestClient resourceRestClient = new ResourceRestClient();

    private final ConnectorRestClient connectorRestClient = new ConnectorRestClient();

    private final int origX = 3100;

    private final int origY = 2800;

    private final BaseModal<Serializable> modal;

    private final WebMarkupContainer newlyCreatedContainer;

    private final ListView<TopologyNode> newlyCreated;

    private final TopologyTogglePanel togglePanel;

    private final LoadableDetachableModel<List<ResourceTO>> resModel = new LoadableDetachableModel<List<ResourceTO>>() {

        private static final long serialVersionUID = 5275935387613157431L;

        @Override
        protected List<ResourceTO> load() {
            return resourceRestClient.list();
        }
    };

    private final LoadableDetachableModel<Map<String, List<ConnInstanceTO>>> connModel =
            new LoadableDetachableModel<Map<String, List<ConnInstanceTO>>>() {

        private static final long serialVersionUID = 5275935387613157432L;

        @Override
        protected Map<String, List<ConnInstanceTO>> load() {
            final Map<String, List<ConnInstanceTO>> res = new HashMap<>();

            connectorRestClient.getAllConnectors().forEach(conn -> {
                final List<ConnInstanceTO> conns;
                if (res.containsKey(conn.getLocation())) {
                    conns = res.get(conn.getLocation());
                } else {
                    conns = new ArrayList<>();
                    res.put(conn.getLocation(), conns);
                }
                conns.add(conn);
            });

            return res;
        }
    };

    private final LoadableDetachableModel<Pair<List<URI>, List<URI>>> csModel =
            new LoadableDetachableModel<Pair<List<URI>, List<URI>>>() {

        private static final long serialVersionUID = 5275935387613157433L;

        @Override
        protected Pair<List<URI>, List<URI>> load() {
            final List<URI> connectorServers = new ArrayList<>();
            final List<URI> filePaths = new ArrayList<>();

            SyncopeConsoleSession.get().getPlatformInfo().getConnIdLocations().forEach(location -> {
                if (location.startsWith(CONNECTOR_SERVER_LOCATION_PREFIX)) {
                    connectorServers.add(URI.create(location));
                } else {
                    filePaths.add(URI.create(location));
                }
            });

            return Pair.of(connectorServers, filePaths);
        }
    };

    protected enum SupportedOperation {

        CHECK_RESOURCE,
        CHECK_CONNECTOR,
        ADD_ENDPOINT;

    }

    public Topology() {
        modal = new BaseModal<>("resource-modal");
        body.add(modal.size(Modal.Size.Large));
        modal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                modal.show(false);
            }
        });

        body.add(new TopologyWebSocketBehavior());

        togglePanel = new TopologyTogglePanel("toggle", getPageReference());
        body.add(togglePanel);

        // -----------------------------------------
        // Add Zoom panel
        // -----------------------------------------
        final ActionsPanel<Serializable> zoomActionPanel = new ActionsPanel<>("zoom", null);

        zoomActionPanel.add(new ActionLink<Serializable>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                target.appendJavaScript("zoomIn($('#drawing')[0]);");
            }
        }, ActionLink.ActionType.ZOOM_IN, StandardEntitlement.CONNECTOR_LIST).disableIndicator().hideLabel();
        zoomActionPanel.add(new ActionLink<Serializable>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                target.appendJavaScript("zoomOut($('#drawing')[0]);");
            }
        }, ActionLink.ActionType.ZOOM_OUT, StandardEntitlement.CONNECTOR_LIST).disableIndicator().hideLabel();

        body.add(zoomActionPanel);
        // -----------------------------------------

        // -----------------------------------------
        // Add Syncope (root topologynode)
        // -----------------------------------------
        final TopologyNode syncopeTopologyNode = new TopologyNode(ROOT_NAME, ROOT_NAME, TopologyNode.Kind.SYNCOPE);
        syncopeTopologyNode.setX(origX);
        syncopeTopologyNode.setY(origY);

        final URI uri = WebClient.client(BaseRestClient.getSyncopeService()).getBaseURI();
        syncopeTopologyNode.setHost(uri.getHost());
        syncopeTopologyNode.setPort(uri.getPort());

        body.add(topologyNodePanel("syncope", syncopeTopologyNode));

        final Map<Serializable, Map<Serializable, TopologyNode>> connections = new HashMap<>();
        final Map<Serializable, TopologyNode> syncopeConnections = new HashMap<>();
        connections.put(syncopeTopologyNode.getKey(), syncopeConnections);

        // required to retrieve parent positions
        final Map<String, TopologyNode> servers = new HashMap<>();
        final Map<String, TopologyNode> connectors = new HashMap<>();
        // -----------------------------------------

        // -----------------------------------------
        // Add Connector Servers
        // -----------------------------------------
        final ListView<URI> connectorServers = new ListView<URI>("connectorServers", csModel.getObject().getLeft()) {

            private static final long serialVersionUID = 6978621871488360380L;

            private final int size = csModel.getObject().getLeft().size() + 1;

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
                connections.put(url, new HashMap<>());
            }
        };

        connectorServers.setOutputMarkupId(true);
        body.add(connectorServers);
        // -----------------------------------------

        // -----------------------------------------
        // Add File Paths
        // -----------------------------------------
        final ListView<URI> filePaths = new ListView<URI>("filePaths", csModel.getObject().getRight()) {

            private static final long serialVersionUID = 6978621871488360380L;

            private final int size = csModel.getObject().getRight().size() + 1;

            @Override
            protected void populateItem(final ListItem<URI> item) {
                int kx = size >= 4 ? 800 : (200 * size);

                int x = (int) Math.round(origX + kx * Math.cos(Math.PI * (item.getIndex() + 1) / size));
                int y = (int) Math.round(origY + 100 * Math.sin(Math.PI * (item.getIndex() + 1) / size));

                final URI location = item.getModelObject();
                final String url = location.toASCIIString();

                final TopologyNode topologynode = new TopologyNode(url, url, TopologyNode.Kind.FS_PATH);

                topologynode.setHost(location.getHost());
                topologynode.setPort(location.getPort());
                topologynode.setX(x);
                topologynode.setY(y);

                servers.put(String.class.cast(topologynode.getKey()), topologynode);

                item.add(topologyNodePanel("fp", topologynode));

                syncopeConnections.put(url, topologynode);
                connections.put(url, new HashMap<>());
            }
        };

        filePaths.setOutputMarkupId(true);
        body.add(filePaths);
        // -----------------------------------------

        // -----------------------------------------
        // Add Connector Intances
        // -----------------------------------------
        final List<List<ConnInstanceTO>> allConns = new ArrayList<>(connModel.getObject().values());

        final ListView<List<ConnInstanceTO>> conns = new ListView<List<ConnInstanceTO>>("conns", allConns) {

            private static final long serialVersionUID = 697862187148836036L;

            @Override
            protected void populateItem(final ListItem<List<ConnInstanceTO>> item) {

                final int size = item.getModelObject().size() + 1;

                final ListView<ConnInstanceTO> conns = new ListView<ConnInstanceTO>("conns", item.getModelObject()) {

                    private static final long serialVersionUID = 6978621871488360381L;

                    @Override
                    protected void populateItem(final ListItem<ConnInstanceTO> item) {
                        final ConnInstanceTO conn = item.getModelObject();

                        final TopologyNode topologynode = new TopologyNode(
                                conn.getKey(), conn.getDisplayName(), TopologyNode.Kind.CONNECTOR);

                        // Define the parent note
                        final TopologyNode parent = servers.get(conn.getLocation());

                        // Set the position
                        int kx = size >= 6 ? 800 : (130 * size);

                        final double hpos;
                        if (conn.getLocation().startsWith(CONNECTOR_SERVER_LOCATION_PREFIX)) {
                            hpos = Math.PI;
                        } else {
                            hpos = 0.0;
                        }

                        int x = (int) Math.round((parent == null ? origX : parent.getX())
                                + kx * Math.cos(hpos + Math.PI * (item.getIndex() + 1) / size));
                        int y = (int) Math.round((parent == null ? origY : parent.getY())
                                + 100 * Math.sin(hpos + Math.PI * (item.getIndex() + 1) / size));

                        topologynode.setConnectionDisplayName(conn.getBundleName());
                        topologynode.setX(x);
                        topologynode.setY(y);

                        connectors.put(String.class.cast(topologynode.getKey()), topologynode);
                        item.add(topologyNodePanel("conn", topologynode));

                        // Update connections
                        final Map<Serializable, TopologyNode> remoteConnections;

                        if (connections.containsKey(conn.getLocation())) {
                            remoteConnections = connections.get(conn.getLocation());
                        } else {
                            remoteConnections = new HashMap<>();
                            connections.put(conn.getLocation(), remoteConnections);
                        }
                        remoteConnections.put(conn.getKey(), topologynode);
                    }
                };

                conns.setOutputMarkupId(true);
                item.add(conns);
            }
        };

        conns.setOutputMarkupId(true);
        body.add(conns);
        // -----------------------------------------

        // -----------------------------------------
        // Add Resources
        // -----------------------------------------
        final Collection<String> administrableConns = new HashSet<>();
        connModel.getObject().values().forEach(connInstances -> {
            administrableConns.addAll(connInstances.stream().map(EntityTO::getKey).collect(Collectors.toList()));
        });

        final List<String> connToBeProcessed = new ArrayList<>();
        resModel.getObject().stream().
                filter((resourceTO) -> (administrableConns.contains(resourceTO.getConnector()))).
                forEachOrdered(resourceTO -> {
                    final TopologyNode topologynode = new TopologyNode(
                            resourceTO.getKey(), resourceTO.getKey(), TopologyNode.Kind.RESOURCE);
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
                });

        final ListView<String> resources = new ListView<String>("resources", connToBeProcessed) {

            private static final long serialVersionUID = 697862187148836038L;

            @Override
            protected void populateItem(final ListItem<String> item) {
                final String connectorKey = item.getModelObject();

                final ListView<TopologyNode> innerListView = new ListView<TopologyNode>("resources",
                        new ArrayList<>(connections.get(connectorKey).values())) {

                    private static final long serialVersionUID = -3447760771863754342L;

                    private final int size = getModelObject().size() + 1;

                    @Override
                    protected void populateItem(final ListItem<TopologyNode> item) {
                        final TopologyNode topologynode = item.getModelObject();
                        final TopologyNode parent = connectors.get(connectorKey);

                        // Set position
                        int kx = size >= 16 ? 800 : (48 * size);
                        int ky = size < 4 ? 100 : size < 6 ? 350 : 750;

                        final double hpos;
                        if (parent == null || parent.getY() < syncopeTopologyNode.getY()) {
                            hpos = Math.PI;
                        } else {
                            hpos = 0.0;
                        }

                        int x = (int) Math.round((parent == null ? origX : parent.getX())
                                + kx * Math.cos(hpos + Math.PI * (item.getIndex() + 1) / size));
                        int y = (int) Math.round((parent == null ? origY : parent.getY())
                                + ky * Math.sin(hpos + Math.PI * (item.getIndex() + 1) / size));

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
        body.add(resources);
        // -----------------------------------------

        // -----------------------------------------
        // Create connections
        // -----------------------------------------
        final WebMarkupContainer jsPlace = new WebMarkupContainerNoVeil("jsPlace");
        jsPlace.setOutputMarkupId(true);
        body.add(jsPlace);

        jsPlace.add(new Behavior() {

            private static final long serialVersionUID = 2661717818979056044L;

            @Override
            public void renderHead(final Component component, final IHeaderResponse response) {
                final StringBuilder jsPlumbConf = new StringBuilder();
                jsPlumbConf.append(String.format(Locale.US, "activate(%.2f);", 0.68f));

                createConnections(connections).forEach(str -> {
                    jsPlumbConf.append(str);
                });

                response.render(OnDomReadyHeaderItem.forScript(jsPlumbConf.toString()));
            }
        });

        jsPlace.add(new AbstractAjaxTimerBehavior(Duration.seconds(2)) {

            private static final long serialVersionUID = -4426283634345968585L;

            @Override
            protected void onTimer(final AjaxRequestTarget target) {
                target.appendJavaScript("checkConnection()");

                if (getUpdateInterval().seconds() < 5.0) {
                    setUpdateInterval(Duration.seconds(5));
                } else if (getUpdateInterval().seconds() < 10.0) {
                    setUpdateInterval(Duration.seconds(10));
                } else if (getUpdateInterval().seconds() < 15.0) {
                    setUpdateInterval(Duration.seconds(15));
                } else if (getUpdateInterval().seconds() < 20.0) {
                    setUpdateInterval(Duration.seconds(20));
                } else if (getUpdateInterval().seconds() < 30.0) {
                    setUpdateInterval(Duration.seconds(30));
                } else if (getUpdateInterval().seconds() < 60.0) {
                    setUpdateInterval(Duration.seconds(60));
                }
            }
        });
        // -----------------------------------------

        newlyCreatedContainer = new WebMarkupContainer("newlyCreatedContainer");
        newlyCreatedContainer.setOutputMarkupId(true);
        body.add(newlyCreatedContainer);

        newlyCreated = new ListView<TopologyNode>("newlyCreated", new ArrayList<TopologyNode>()) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem<TopologyNode> item) {
                item.add(topologyNodePanel("el", item.getModelObject()));
            }
        };
        newlyCreated.setOutputMarkupId(true);
        newlyCreated.setReuseItems(true);

        newlyCreatedContainer.add(newlyCreated);
    }

    private List<String> createConnections(final Map<Serializable, Map<Serializable, TopologyNode>> targets) {
        List<String> list = new ArrayList<>();

        targets.entrySet().forEach(source -> {
            source.getValue().entrySet().forEach(target -> {
                list.add(String.format("connect('%s','%s','%s');",
                        source.getKey(),
                        target.getKey(),
                        target.getValue().getKind()));
            });
        });
        return list;
    }

    private TopologyNodePanel topologyNodePanel(final String id, final TopologyNode node) {

        final TopologyNodePanel panel = new TopologyNodePanel(id, node);
        panel.setMarkupId(String.valueOf(node.getKey()));
        panel.setOutputMarkupId(true);

        final List<Behavior> behaviors = new ArrayList<>();

        behaviors.add(new Behavior() {

            private static final long serialVersionUID = 2661717818979056044L;

            @Override
            public void renderHead(final Component component, final IHeaderResponse response) {
                response.render(OnDomReadyHeaderItem.forScript(String.format("setPosition('%s', %d, %d)",
                        node.getKey(), node.getX(), node.getY())));
            }
        });

        behaviors.add(new AjaxEventBehavior(Constants.ON_CLICK) {

            private static final long serialVersionUID = -9027652037484739586L;

            @Override
            protected String findIndicatorId() {
                return StringUtils.EMPTY;
            }

            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                togglePanel.toggleWithContent(target, node);
                target.appendJavaScript(String.format(
                        "$('.window').removeClass(\"active-window\").addClass(\"inactive-window\"); "
                        + "$(document.getElementById('%s'))."
                        + "removeClass(\"inactive-window\").addClass(\"active-window\");", node.getKey()));
            }
        });

        panel.add(behaviors.toArray(new Behavior[] {}));

        return panel;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onEvent(final IEvent<?> event) {
        super.onEvent(event);

        if (event.getPayload() instanceof CreateEvent) {
            final CreateEvent resourceCreateEvent = CreateEvent.class.cast(event.getPayload());

            final TopologyNode node = new TopologyNode(
                    resourceCreateEvent.getKey(),
                    resourceCreateEvent.getDisplayName(),
                    resourceCreateEvent.getKind());

            newlyCreated.getModelObject().add(node);
            resourceCreateEvent.getTarget().add(newlyCreatedContainer);

            resourceCreateEvent.getTarget().appendJavaScript(String.format(
                    "window.Wicket.WebSocket.send('"
                    + "{\"kind\":\"%s\",\"target\":\"%s\",\"source\":\"%s\",\"scope\":\"%s\"}"
                    + "');",
                    SupportedOperation.ADD_ENDPOINT,
                    resourceCreateEvent.getKey(),
                    resourceCreateEvent.getParent(),
                    resourceCreateEvent.getKind()));
        }
    }

    private static class WebMarkupContainerNoVeil extends WebMarkupContainer implements IAjaxIndicatorAware {

        private static final long serialVersionUID = 6883930486048460708L;

        WebMarkupContainerNoVeil(final String id) {
            super(id);
        }

        @Override
        public String getAjaxIndicatorMarkupId() {
            return StringUtils.EMPTY;
        }
    }
}

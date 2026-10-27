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

// Graph connections, status updates and page initialization.

(function (topology) {
    let checkRunId = 0;

    const def = {
        paintStyle: {
            lineWidth: 2,
            strokeStyle: "#94a3b8",
            outlineColor: "rgba(15, 23, 42, 0.18)",
            outlineWidth: 1.5,
            lineCap: "round",
            lineJoin: "round"
        },
        connectorPaintStyle: {
            lineWidth: 2
        },
        anchor: "AutoDefault",
        detachable: false,
        endpointStyle: {
            fillStyle: "#cbd5e1",
            outlineColor: "rgba(15, 23, 42, 0.18)",
            outlineWidth: 1,
            radius: 3
        }
    };

    const failedConnectorStyle = {
        lineWidth: 2,
        strokeStyle: "rgba(148, 163, 184, 0.55)",
        outlineColor: "rgba(15, 23, 42, 0.18)",
        outlineWidth: 1.5
    };

    const failedConnectorHoverStyle = {
        strokeStyle: "#FFFFFF"
    };

    const failedEndpointStyle = {
        fillStyle: "rgba(148, 163, 184, 0.45)",
        outlineColor: "rgba(15, 23, 42, 0.18)",
        outlineWidth: 1,
        radius: 3
    };

    const disabledConnectorStyle = {
        lineWidth: 2,
        strokeStyle: "#fb7185",
        outlineColor: "rgba(15, 23, 42, 0.18)",
        outlineWidth: 1.5
    };

    const disabledConnectorHoverStyle = {
        strokeStyle: "#FF8C00"
    };

    const disabledEndpointStyle = {
        fillStyle: "rgba(251, 113, 133, 0.8)",
        outlineColor: "rgba(15, 23, 42, 0.18)",
        outlineWidth: 1,
        radius: 3
    };

    const enabledConnectorStyle = {
        lineWidth: 2,
        strokeStyle: "#22c55e",
        outlineColor: "rgba(15, 23, 42, 0.18)",
        outlineWidth: 1.5
    };

    const enabledConnectorHoverStyle = {
        strokeStyle: "#16a34a"
    };

    const enabledEndpointStyle = {
        fillStyle: "rgba(34, 197, 94, 0.35)",
        outlineColor: "rgba(15, 23, 42, 0.18)",
        outlineWidth: 1,
        radius: 3
    };

    // Connect existing nodes once jsPlumb is ready.
    function connect(source, target, scope) {
        jsPlumb.ready(function () {
            if (jsPlumb.select({source: source, target: target, scope: scope}) != null) {
                jsPlumb.connect({source: source, target: target, scope: scope}, def);
            }
        });
    }

    // Position a new node near its source, make it draggable and connect it.
    function addEndpoint(source, target, scope) {
        const sourceElement = $(document.getElementById(source));

        const top = parseFloat(sourceElement.css("top")) + 10;
        const left = parseFloat(sourceElement.css("left")) - 150;

        topology.setPosition(target, left, top);
        jsPlumb.ready(function () {
            jsPlumb.draggable(jsPlumb.getSelector(document.getElementById(target)));
            jsPlumb.connect({source: source, target: target, scope: scope}, def);
        });
    }

    // Apply a connection state to both its incoming lines and endpoints.
    function topologyApplyStyle(targetName, connectorStyle, hoverStyle, endpointStyle) {
        jsPlumb.ready(function () {
            jsPlumb.select({target: targetName}).setPaintStyle(connectorStyle).setHoverPaintStyle(hoverStyle);
            jsPlumb.selectEndpoints({element: [targetName]}).setPaintStyle(endpointStyle);
        });
    }

    // Display the reachable state for a node.
    function enable(targetName) {
        topologyApplyStyle(targetName, enabledConnectorStyle, enabledConnectorHoverStyle, enabledEndpointStyle);
    }

    // Display the unreachable state for a node.
    function disable(targetName) {
        topologyApplyStyle(targetName, disabledConnectorStyle, disabledConnectorHoverStyle, disabledEndpointStyle);
    }

    // Display a failed connection check for a node.
    function failure(targetName) {
        topologyApplyStyle(targetName, failedConnectorStyle, failedConnectorHoverStyle, failedEndpointStyle);
    }

    // Send checks in batches; a newer run stops any requests not yet sent.
    function checkConnection() {
        jsPlumb.ready(function () {
            const items = [];

            jsPlumb.select({scope: "CONNECTOR"}).each(function (connection) {
                const id = connection && connection.target ? connection.target.id : null;
                if (id) {
                    items.push({kind: "CHECK_CONNECTOR", id: id});
                }
            });
            jsPlumb.select({scope: "RESOURCE"}).each(function (connection) {
                const id = connection && connection.target ? connection.target.id : null;
                if (id) {
                    items.push({kind: "CHECK_RESOURCE", id: id});
                }
            });

            checkRunId++;
            const runId = checkRunId;

            const batchSize = 25;
            let idx = 0;

            // Send the next batch only if this is still the latest check run.
            function step() {
                if (checkRunId !== runId) {
                    return;
                }
                const end = Math.min(idx + batchSize, items.length);

                // Keep the last known colors while checks run; sending a request needs no repaint.
                for (let i = idx; i < end; i++) {
                    const it = items[i];
                    Wicket.WebSocket.send(JSON.stringify({kind: it.kind, target: it.id}));
                }

                idx = end;
                if (idx < items.length) {
                    window.setTimeout(step, 0);
                }
            }

            step();
        });
    }

    // Queue WebSocket results by node and skip states already applied.
    function initTopologyStatuses() {
        const pendingStatusByTarget = new Map();
        const appliedStatusByTarget = new Map();
        let flushScheduled = false;

        // Map backend connection states to their visual styles.
        function applyStatus(targetId, status) {
            if (!targetId) {
                return;
            }
            switch (status) {
                case 'REACHABLE':
                    enable(targetId);
                    break;
                case 'UNREACHABLE':
                    disable(targetId);
                    break;
                case 'FAILURE':
                    failure(targetId);
                    break;
                default:
                    break;
            }
        }

        // Apply a small batch of pending states, then yield to browser input.
        function flushStatuses() {
            flushScheduled = false;
            let count = 0;
            for (const [targetId, status] of pendingStatusByTarget) {
                pendingStatusByTarget.delete(targetId);
                if (appliedStatusByTarget.get(targetId) !== status) {
                    applyStatus(targetId, status);
                    appliedStatusByTarget.set(targetId, status);
                }
                // Apply a small batch, leaving the browser free to handle input.
                if (++count >= 8) {
                    break;
                }
            }
            if (pendingStatusByTarget.size > 0) {
                scheduleFlush();
            }
        }

        // Keep at most one timer waiting to apply queued states.
        function scheduleFlush() {
            if (flushScheduled) {
                return;
            }
            flushScheduled = true;
            window.setTimeout(flushStatuses, 32);
        }

        Wicket.Event.subscribe("/websocket/message", function (_jqEvent, message) {
            const val = JSON.parse(message);
            if (!val.target || val.status === "UNKNOWN") {
                return;
            }
            pendingStatusByTarget.set(val.target, val.status);
            scheduleFlush();
        });
    }

    // Keep a complete saved layout; rebuild the tree if any node lacks a valid position.
    function restoreTopologyLayout() {
        jsPlumb.ready(function () {
            const state = topology.getState();
            const nodes = $("#drawing .window").toArray();
            if (nodes.length > 0 && nodes.every(node => topology.hasPosition(state, node.id))) {
                topology.treeBounds = null;
                topology.recenterToTree();
            } else {
                topology.autoLayoutTree({centerInView: false});
            }
        });
    }

    // Initialize storage, dragging, navigation and the saved or default zoom.
    function activate(zoom) {
        topology.initStorage();
        jsPlumb.ready(function () {
            jsPlumb.draggable(jsPlumb.getSelector(".window"));
            jsPlumb.setContainer("drawing");
            topology.initZoom();

            jsPlumb.Defaults.MaxConnections = 1000;

            topology.initPanning();

            const val = topology.getState();
            if (!Number.isFinite(val.__zoom__) || val.__zoom__ <= 0) {
                topology.setZoom($("#drawing")[0], zoom);
            } else {
                topology.setZoom($("#drawing")[0], val.__zoom__);
            }
        });
    }

    jsPlumb.importDefaults({
        Connector: ["Bezier", { curviness: 10 }],
        DragOptions: {
            cursor: "pointer",
            zIndex: 2000,
            stop: topology.savePositions
        },
        HoverClass: "connector-hover"
    });

    Object.assign(topology, {
        connect,
        addEndpoint,
        enable,
        disable,
        failure,
        checkConnection,
        restoreTopologyLayout,
        activate
    });

    // Preserve the entry points invoked by Wicket and existing page scripts.
    Object.assign(window, {
        activate: topology.activate,
        setPosition: topology.setPosition,
        restoreTopologyLayout: topology.restoreTopologyLayout,
        autoLayoutTree: topology.autoLayoutTree,
        recenterToTree: topology.recenterToTree,
        checkConnection: topology.checkConnection,
        connect: topology.connect,
        addEndpoint: topology.addEndpoint,
        setZoom: topology.setZoom,
        zoomIn: topology.zoomIn,
        zoomOut: topology.zoomOut,
        enable: topology.enable,
        disable: topology.disable,
        failure: topology.failure
    });

    jQuery(initTopologyStatuses);
})(window.SyncopeTopology);

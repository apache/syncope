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

// Shared state, persistence and geometry.

window.SyncopeTopology = window.SyncopeTopology || {};

(function (topology) {
    const config = topology.config = {
        minZoom: 0.1,
        maxZoom: 2,
        zoomStep: 1.1,
        nodeSize: {w: 140, h: 55}
    };

    topology.treeBounds = null;

    // Read a CSS coordinate as a number, falling back when it is missing or invalid.
    function topologyParsePx(val, fallback) {
        if (val == null) {
            return fallback;
        }
        if (typeof val === 'number') {
            return val;
        }
        const n = parseFloat(String(val).replace("px", ""));
        return isNaN(n) ? fallback : n;
    }

    // Show the shared loading overlay when it is available.
    function topologyShowVeil() {
        const veil = document.getElementById("veil");
        if (veil) {
            veil.style.display = "block";
        }
    }

    // Hide the shared loading overlay.
    function topologyHideVeil() {
        const veil = document.getElementById("veil");
        if (veil) {
            veil.style.display = "none";
        }
    }

    // Schedule layout work and clean up its loading-overlay timer afterward.
    function topologyRunWithOptionalVeil(fn, options) {
        options = options || {};
        const delayMs = options.delayMs != null ? options.delayMs : 120;
        let shown = false;

        const t = window.setTimeout(function () {
            topologyShowVeil();
            shown = true;
        }, delayMs);

        window.setTimeout(function () {
            try {
                fn();
            } finally {
                window.clearTimeout(t);
                if (shown) {
                    topologyHideVeil();
                }
            }
        }, 0);
    }

    let topologyState;

    // Load the browser layout, keeping an empty state if storage cannot be read.
    function initTopologyStorage() {
        topologyState = Object.create(null);
        try {
            const saved = JSON.parse(window.localStorage.getItem("syncope.topology"));
            if (saved && typeof saved === "object" && !Array.isArray(saved)) {
                topologyState = saved;
            }
        } catch (err) {
            console.debug("Unable to restore topology state", err);
        }
    }

    // Return the in-memory layout, loading it on first use.
    function getTopology() {
        if (topologyState == null) {
            initTopologyStorage();
        }
        return topologyState;
    }

    // Update the in-memory layout and persist it when browser storage is available.
    function saveTopology(state) {
        topologyState = state;
        try {
            window.localStorage.setItem("syncope.topology", JSON.stringify(state));
        } catch (err) {
            // Keep the current page usable even when browser storage is unavailable or full.
            console.debug("Unable to persist topology state", err);
        }
    }

    // Accept only saved entries with finite coordinates for both axes.
    function topologyHasPosition(state, id) {
        const position = Object.prototype.hasOwnProperty.call(state, id) ? state[id] : null;
        return position && Number.isFinite(parseFloat(position.left)) && Number.isFinite(parseFloat(position.top));
    }

    // Prefer the active jsPlumb zoom, then a valid saved zoom, then the default scale.
    function topologyGetZoom() {
        const state = getTopology();
        const current = jsPlumb.getZoom && jsPlumb.getZoom();
        if (Number.isFinite(current) && current > 0) {
            return current;
        }
        return Number.isFinite(state.__zoom__) && state.__zoom__ > 0 ? state.__zoom__ : 1;
    }

    // Save current nodes and zoom, discard stale entries and invalidate cached bounds.
    function topologySavePositions() {
        const state = Object.create(null);
        state.__zoom__ = topologyGetZoom();
        const nodes = $("#drawing .window").toArray();
        for (const node of nodes) {
            state[node.id] = {left: $(node).css("left"), top: $(node).css("top")};
        }
        saveTopology(state);
        topology.treeBounds = null;
    }

    // Use the saved node position when valid, otherwise use the supplied coordinates.
    function setPosition(id, x, y) {
        const val = getTopology();

        try {
            // Connector server IDs can contain characters that are special in CSS selectors.
            const element = $(document.getElementById(id));

            if (!topologyHasPosition(val, id)) {
                element.css("left", x + "px");
                element.css("top", y + "px");
            } else {
                element.css("left", val[id].left);
                element.css("top", val[id].top);
            }
        } catch (err) {
            console.log("Failure setting position for ", id);
        }
    }

    // Snapshot current node positions and unscaled sizes for layout or centering.
    function topologyMeasureNodes() {
        const layout = {nodeIds: [], nodeSizeById: {}, positions: {}};
        for (const node of $("#drawing .window").toArray()) {
            if (!node || !node.id) {
                continue;
            }
            layout.nodeIds.push(node.id);
            layout.nodeSizeById[node.id] = {
                w: node.offsetWidth || config.nodeSize.w,
                h: node.offsetHeight || config.nodeSize.h
            };
            layout.positions[node.id] = {
                x: topologyParsePx($(node).css("left"), 0),
                y: topologyParsePx($(node).css("top"), 0)
            };
        }
        return layout;
    }

    // Measure only nodes with positions, optionally translating them into drawing coordinates.
    function topologyGetBounds(layout, offset = {x: 0, y: 0}, padding = 0) {
        let bounds = null;
        for (const id of layout.nodeIds) {
            const position = layout.positions[id];
            if (!position) {
                continue;
            }
            const size = layout.nodeSizeById[id] || config.nodeSize;
            const x = position.x + offset.x + padding;
            const y = position.y + offset.y + padding;
            if (bounds == null) {
                bounds = {minX: x, minY: y, maxX: x + size.w, maxY: y + size.h};
            } else {
                bounds.minX = Math.min(bounds.minX, x);
                bounds.minY = Math.min(bounds.minY, y);
                bounds.maxX = Math.max(bounds.maxX, x + size.w);
                bounds.maxY = Math.max(bounds.maxY, y + size.h);
            }
        }
        return bounds;
    }

    Object.assign(topology, {
        parsePx: topologyParsePx,
        runWithOptionalVeil: topologyRunWithOptionalVeil,
        initStorage: initTopologyStorage,
        getState: getTopology,
        saveState: saveTopology,
        hasPosition: topologyHasPosition,
        getZoom: topologyGetZoom,
        savePositions: topologySavePositions,
        setPosition,
        measureNodes: topologyMeasureNodes,
        getBounds: topologyGetBounds
    });
})(window.SyncopeTopology);

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

// Zoom, centering and background panning.

(function (topology) {
    const config = topology.config;

    // Apply the bounded scale to the drawing and jsPlumb, then synchronize zoom controls.
    function setZoom(el, zoom, instance, transformOrigin) {
        zoom = Math.max(config.minZoom, Math.min(config.maxZoom, zoom));
        transformOrigin = transformOrigin || [0.5, 0.5];
        instance = instance || jsPlumb;
        el = el || instance.getContainer();

        const p = ["webkit", "moz", "ms", "o"],
            s = "scale(" + zoom + ")",
            oString = (transformOrigin[0] * 100) + "% " + (transformOrigin[1] * 100) + "%";

        for (let i = 0; i < p.length; i++) {
            el.style[p[i] + "Transform"] = s;
            el.style[p[i] + "TransformOrigin"] = oString;
        }

        el.style["transform"] = s;
        el.style["transformOrigin"] = oString;

        instance.setZoom(zoom);
        $("#topology-zoom").val(Math.round(zoom * 100));
        $("#topology-zoom-value").text(Math.round(zoom * 100) + "%");
    }

    // Change scale around the viewport center and persist the resulting zoom.
    function topologyZoomTo(zoom, el) {
        const $drawing = $(el || document.getElementById("drawing"));
        const $topology = $("#topology");
        zoom = Math.max(config.minZoom, Math.min(config.maxZoom, zoom));
        const ratio = zoom / topology.getZoom();
        const originX = $drawing.width() / 2;
        const originY = $drawing.height() / 2;
        const centerX = $topology.width() / 2;
        const centerY = $topology.height() / 2;
        const left = topology.parsePx($drawing.css("left"), 0);
        const top = topology.parsePx($drawing.css("top"), 0);

        // Keep the graph point at the viewport center fixed while its scale changes.
        $drawing.css({
            left: centerX - originX - (centerX - left - originX) * ratio,
            top: centerY - originY - (centerY - top - originY) * ratio
        });
        setZoom($drawing[0], zoom);

        const state = topology.getState();
        state.__zoom__ = zoom;
        topology.saveState(state);
    }

    // Increase the current scale by ten percent around the viewport center.
    function zoomIn(el) {
        topologyZoomTo(topology.getZoom() * config.zoomStep, el);
    }

    // Apply the inverse zoom step around the viewport center.
    function zoomOut(el) {
        topologyZoomTo(topology.getZoom() / config.zoomStep, el);
    }

    // Normalize Ctrl+wheel input and use it for graph zoom instead of browser zoom.
    function topologyOnWheel(event) {
        if (!event.ctrlKey || event.deltaY === 0) {
            return;
        }
        event.preventDefault();
        const unit = event.deltaMode === 1 ? 16 : event.deltaMode === 2 ? this.clientHeight : 1;
        const delta = Math.max(-100, Math.min(100, event.deltaY * unit));
        topologyZoomTo(topology.getZoom() * Math.exp(-delta * 0.002));
    }

    // Bind slider and wheel input; zoom buttons are handled by Wicket Ajax actions.
    function initTopologyZoom() {
        $("#topology-zoom").off(".topologyzoom").on("input.topologyzoom", function () {
            topologyZoomTo(Number(this.value) / 100);
        });
        const topology = document.getElementById("topology");
        topology.removeEventListener("wheel", topologyOnWheel);
        topology.addEventListener("wheel", topologyOnWheel, {passive: false});
    }

    // Translate the drawing to center the supplied bounds, accounting for scale and origin.
    function topologyRecenterOnBBox(bbox) {
        if (!bbox) {
            return;
        }
        const $topology = $("#topology");
        const $drawing = $("#drawing");
        if ($topology.length === 0 || $drawing.length === 0) {
            return;
        }

        const topoW = $topology.width() || 0;
        const topoH = $topology.height() || 0;
        if (topoW <= 0 || topoH <= 0) {
            return;
        }

        const z = topology.getZoom();
        const drawingW = $drawing.width() || 0;
        const drawingH = $drawing.height() || 0;
        const originX = drawingW * 0.5;
        const originY = drawingH * 0.5;

        const cx = (bbox.minX + bbox.maxX) / 2;
        const cy = (bbox.minY + bbox.maxY) / 2;

        const viewportCenterX = topoW / 2;
        const viewportCenterY = topoH / 2;

        const newLeft = viewportCenterX - originX - (cx - originX) * z;
        const newTop = viewportCenterY - originY - (cy - originY) * z;

        $drawing.css("left", Math.round(newLeft) + "px");
        $drawing.css("top", Math.round(newTop) + "px");
        jsPlumb.repaintEverything();
    }

    // Center cached tree bounds, or measure current nodes when those bounds are unavailable.
    function recenterToTree() {
        topology.runWithOptionalVeil(function () {
            jsPlumb.ready(function () {
                const bbox = topology.treeBounds || topology.getBounds(topology.measureNodes());
                topologyRecenterOnBBox(bbox);
            });
        });
    }

    // Pan the background while leaving node dragging to jsPlumb.
    function initTopologyPanning() {
        const $topology = $("#topology");
        const $drawing = $("#drawing");
        if ($topology.length === 0 || $drawing.length === 0) {
            return;
        }

        $topology.off(".topopanning");
        $(document).off(".topopanning");

        let dragging = false;
        let startX = 0;
        let startY = 0;
        let startLeft = 0;
        let startTop = 0;

        // Keep node and connector interactions from starting a background pan.
        function panBlocked(target) {
            return $(target).closest(".window, ._jsPlumb_connector, ._jsPlumb_endpoint, ._jsPlumb_overlay").length > 0;
        }

        $topology.on("mousedown.topopanning", function (e) {
            if (e.button !== 0) {
                return;
            }
            if (panBlocked(e.target)) {
                return;
            }

            dragging = true;
            startX = e.clientX;
            startY = e.clientY;
            startLeft = topology.parsePx($drawing.css("left"), 0);
            startTop = topology.parsePx($drawing.css("top"), 0);
            $topology.addClass("topology-panning");
            e.preventDefault();
        });

        $(document).on("mousemove.topopanning", function (e) {
            if (!dragging) {
                return;
            }
            const dx = e.clientX - startX;
            const dy = e.clientY - startY;
            $drawing.css("left", (startLeft + dx) + "px");
            $drawing.css("top", (startTop + dy) + "px");
        });

        $(document).on("mouseup.topopanning", function () {
            if (!dragging) {
                return;
            }
            dragging = false;
            $("#topology").removeClass("topology-panning");
            jsPlumb.repaintEverything();
        });
    }

    Object.assign(topology, {
        setZoom,
        zoomIn,
        zoomOut,
        initZoom: initTopologyZoom,
        recenterOnBounds: topologyRecenterOnBBox,
        recenterToTree,
        initPanning: initTopologyPanning
    });
})(window.SyncopeTopology);

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

// Tree layout and disconnected node placement.

(function (topology) {
    const config = topology.config;

    // Append a child only once, preserving its original order.
    function topologyUniqPush(arr, value) {
        for (let i = 0; i < arr.length; i++) {
            if (arr[i] === value) {
                return;
            }
        }
        arr.push(value);
    }

    // Build child lists and incoming-edge counts, ignoring invalid edges and self-links.
    function topologyBuildChildren(connections) {
        const childrenById = {};
        const indegree = {};
        for (let i = 0; i < connections.length; i++) {
            const c = connections[i];
            if (!c || !c.source || !c.target) {
                continue;
            }
            const sourceId = c.source.id;
            const targetId = c.target.id;
            if (!sourceId || !targetId || sourceId === targetId) {
                continue;
            }

            if (!childrenById[sourceId]) {
                childrenById[sourceId] = [];
            }
            topologyUniqPush(childrenById[sourceId], targetId);

            if (indegree[targetId] == null) {
                indegree[targetId] = 1;
            } else {
                indegree[targetId] = indegree[targetId] + 1;
            }
            if (indegree[sourceId] == null) {
                indegree[sourceId] = 0;
            }
        }
        return {childrenById: childrenById, indegree: indegree};
    }

    // Prefer the Syncope root, then a node without incoming edges, then the first node.
    function topologyPickRoot(allNodeIds, childrenById, indegree) {
        const rootEl = $("#drawing .window.topology_root")[0];
        if (rootEl && rootEl.id) {
            return rootEl.id;
        }

        for (let i = 0; i < allNodeIds.length; i++) {
            const id = allNodeIds[i];
            if ((indegree[id] == null || indegree[id] === 0) && childrenById[id] && childrenById[id].length > 0) {
                return id;
            }
        }

        return allNodeIds.length > 0 ? allNodeIds[0] : null;
    }

    // Cache subtree widths and stop recursion when a cycle is encountered.
    function topologyComputeSubtreeWidth(nodeId, childrenById, nodeSizeById, hGap, visiting, memo) {
        if (memo[nodeId] != null) {
            return memo[nodeId];
        }
        if (visiting[nodeId]) {
            memo[nodeId] = nodeSizeById[nodeId] ? nodeSizeById[nodeId].w : config.nodeSize.w;
            return memo[nodeId];
        }

        visiting[nodeId] = true;
        const children = childrenById[nodeId] || [];
        const ownW = nodeSizeById[nodeId] ? nodeSizeById[nodeId].w : config.nodeSize.w;
        if (children.length === 0) {
            memo[nodeId] = ownW;
            visiting[nodeId] = false;
            return memo[nodeId];
        }

        let total = 0;
        for (let i = 0; i < children.length; i++) {
            total += topologyComputeSubtreeWidth(children[i], childrenById, nodeSizeById, hGap, visiting, memo);
        }
        total += hGap * (children.length - 1);
        memo[nodeId] = Math.max(ownW, total);
        visiting[nodeId] = false;
        return memo[nodeId];
    }

    // Center each node above its children and avoid placing the same node twice.
    function topologyAssignPositions(nodeId, childrenById, nodeSizeById, subtreeWById, levelY, xLeft, hGap, vGap, outPos, visited) {
        if (visited[nodeId]) {
            return;
        }
        visited[nodeId] = true;

        const nodeSize = nodeSizeById[nodeId] || config.nodeSize;
        const subtreeW = subtreeWById[nodeId] != null ? subtreeWById[nodeId] : nodeSize.w;
        const xCenter = xLeft + subtreeW / 2;
        outPos[nodeId] = {x: xCenter - nodeSize.w / 2, y: levelY};

        const children = childrenById[nodeId] || [];
        if (children.length === 0) {
            return;
        }

        const nextY = levelY + nodeSize.h + vGap;
        let childX = xLeft;
        for (let i = 0; i < children.length; i++) {
            const childId = children[i];
            const childSubtreeW = subtreeWById[childId] != null
                ? subtreeWById[childId]
                : (nodeSizeById[childId] ? nodeSizeById[childId].w : config.nodeSize.w);

            topologyAssignPositions(childId, childrenById, nodeSizeById, subtreeWById, nextY, childX, hGap, vGap, outPos, visited);
            childX += childSubtreeW + hGap;
        }
    }

    // Compute relative tree positions; disconnected nodes remain without a position.
    function topologyLayoutTree(layout, graph, rootId, hGap, vGap) {
        const subtreeWidths = {};
        for (const id of layout.nodeIds) {
            topologyComputeSubtreeWidth(id, graph.childrenById, layout.nodeSizeById, hGap, {}, subtreeWidths);
        }
        layout.positions = {};
        topologyAssignPositions(rootId, graph.childrenById, layout.nodeSizeById, subtreeWidths,
            0, 0, hGap, vGap, layout.positions, {});
    }

    // Anchor the layout to the current viewport center or to the root's existing position.
    function topologyLayoutOffset(layout, rootId, centerInView) {
        if (centerInView) {
            const bounds = topology.getBounds(layout);
            if (!bounds) {
                return {x: 0, y: 0};
            }
            const $topology = $("#topology");
            const $drawing = $("#drawing");
            const zoom = topology.getZoom();
            return {
                x: (($topology.width() || 0) / 2 - topology.parsePx($drawing.css("left"), 0)) / zoom
                    - (bounds.minX + bounds.maxX) / 2,
                y: (($topology.height() || 0) / 2 - topology.parsePx($drawing.css("top"), 0)) / zoom
                    - (bounds.minY + bounds.maxY) / 2
            };
        }
        const root = document.getElementById(rootId);
        let left = topology.parsePx(root && root.style ? root.style.left : null, 0);
        let top = topology.parsePx(root && root.style ? root.style.top : null, 0);
        if (isNaN(left) || isNaN(top) || (left === 0 && top === 0)) {
            left = topology.parsePx($(root).css("left"), 0);
            top = topology.parsePx($(root).css("top"), 0);
        }
        const position = layout.positions[rootId];
        return {x: left - (position ? position.x : 0), y: top - (position ? position.y : 0)};
    }

    // Apply rounded tree positions and track the extents used to place disconnected nodes.
    function topologyPlaceTree(layout, offset, padding) {
        let placed = null;
        for (const id of layout.nodeIds) {
            const position = layout.positions[id];
            const node = document.getElementById(id);
            if (!position || !node) {
                continue;
            }
            const size = layout.nodeSizeById[id] || config.nodeSize;
            const x = Math.round(position.x + offset.x + padding);
            const y = Math.round(position.y + offset.y + padding);
            node.style.left = x + "px";
            node.style.top = y + "px";
            if (placed == null) {
                placed = {minX: x, maxX: x, maxY: y};
            } else {
                placed.minX = Math.min(placed.minX, x);
                placed.maxX = Math.max(placed.maxX, x + size.w);
                placed.maxY = Math.max(placed.maxY, y + size.h);
            }
        }
        return placed;
    }

    // Arrange nodes outside the root's tree in columns beside the connected nodes.
    function topologyPlaceDisconnected(layout, placed, hGap, padding) {
        const startX = (placed ? placed.maxX : 0) + hGap * 2;
        const startY = placed ? placed.minX - 20 : 0;
        const maxColH = Math.max(420, placed ? placed.maxY - startY : 420);
        let colX = startX;
        let rowY = startY;
        let colW = 0;
        for (const id of layout.nodeIds) {
            if (layout.positions[id]) {
                continue;
            }
            const size = layout.nodeSizeById[id] || config.nodeSize;
            if ((rowY - startY) + size.h > maxColH) {
                colX += colW + hGap;
                rowY = startY;
                colW = 0;
            }
            const node = document.getElementById(id);
            if (node) {
                node.style.left = Math.round(colX + padding) + "px";
                node.style.top = Math.round(rowY + padding) + "px";
            }
            rowY += size.h + 12;
            colW = Math.max(colW, size.w);
        }
    }

    // Lay out the tree and disconnected nodes, save their positions and center the view.
    function autoLayoutTree(options) {
        options = options || {};
        const hGap = options.hGap != null ? options.hGap : 70;
        const vGap = options.vGap != null ? options.vGap : 80;
        const padding = options.padding != null ? options.padding : 10;
        const centerInView = options.centerInView != null ? options.centerInView : false;

        topology.runWithOptionalVeil(function () {
            jsPlumb.ready(function () {
                try {
                    const layout = topology.measureNodes();
                    const connections = jsPlumb.getAllConnections ? jsPlumb.getAllConnections() : [];
                    const graph = topologyBuildChildren(connections);
                    const rootId = topologyPickRoot(layout.nodeIds, graph.childrenById, graph.indegree);
                    if (!rootId) {
                        return;
                    }
                    topologyLayoutTree(layout, graph, rootId, hGap, vGap);
                    const offset = topologyLayoutOffset(layout, rootId, centerInView);

                    if (jsPlumb.setSuspendDrawing) {
                        jsPlumb.setSuspendDrawing(true);
                    }
                    const placed = topologyPlaceTree(layout, offset, padding);
                    topologyPlaceDisconnected(layout, placed, hGap, padding);
                    topology.savePositions();
                    topology.treeBounds = topology.getBounds(layout, offset, padding);

                    if (jsPlumb.setSuspendDrawing) {
                        jsPlumb.setSuspendDrawing(false, true);
                    } else {
                        jsPlumb.repaintEverything();
                    }
                    if (topology.treeBounds) {
                        topology.recenterToTree();
                    }
                } catch (err) {
                    console.debug("autoLayoutTree failed", err);
                }
            });
        });
    }

    Object.assign(topology, {
        autoLayoutTree
    });
})(window.SyncopeTopology);

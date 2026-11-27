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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TopologyTest {

    @Test
    void toMarkupId() {
        assertEquals("resource-aGVsbG8gd29ybGQ",
                Topology.toMarkupId(TopologyNode.Kind.RESOURCE, "hello world"));
        assertEquals("connector-aGVsbG8td29ybGQ",
                Topology.toMarkupId(TopologyNode.Kind.CONNECTOR, "hello-world"));
        assertEquals("fs_path-aGVsbG8ud29ybGQ",
                Topology.toMarkupId(TopologyNode.Kind.FS_PATH, "hello.world"));
        assertEquals("resource-Y2xlYW5faWQ",
                Topology.toMarkupId(TopologyNode.Kind.RESOURCE, "clean_id"));
        assertEquals("syncope-",
                Topology.toMarkupId(TopologyNode.Kind.SYNCOPE, ""));
        assertEquals("resource-YUBiI2M",
                Topology.toMarkupId(TopologyNode.Kind.RESOURCE, "a@b#c"));
        assertEquals("connector_server-Y29ubmlkOi8vZXhhbXBsZQ",
                Topology.toMarkupId(TopologyNode.Kind.CONNECTOR_SERVER, "connid://example"));
        // base64url alphabet, without padding
        assertEquals("resource-YS9iK2M_ZD0",
                Topology.toMarkupId(TopologyNode.Kind.RESOURCE, "a/b+c?d="));
    }
}

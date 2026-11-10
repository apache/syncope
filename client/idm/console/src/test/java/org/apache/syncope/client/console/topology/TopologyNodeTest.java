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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class TopologyNodeTest {

    @Test
    void constructor() {
        TopologyNode node = new TopologyNode("key1", "display1", TopologyNode.Kind.RESOURCE);

        assertNotNull(node);
        assertEquals("key1", node.getKey());
        assertEquals("display1", node.getDisplayName());
        assertEquals(TopologyNode.Kind.RESOURCE, node.getKind());
    }

    @Test
    void kinds() {
        assertEquals(5, TopologyNode.Kind.values().length);
        assertEquals(TopologyNode.Kind.RESOURCE, TopologyNode.Kind.valueOf("RESOURCE"));
        assertEquals(TopologyNode.Kind.CONNECTOR, TopologyNode.Kind.valueOf("CONNECTOR"));
        assertEquals(TopologyNode.Kind.CONNECTOR_SERVER, TopologyNode.Kind.valueOf("CONNECTOR_SERVER"));
        assertEquals(TopologyNode.Kind.FS_PATH, TopologyNode.Kind.valueOf("FS_PATH"));
        assertEquals(TopologyNode.Kind.SYNCOPE, TopologyNode.Kind.valueOf("SYNCOPE"));
    }

    @Test
    void statuses() {
        assertEquals(4, TopologyNode.Status.values().length);
        assertEquals(TopologyNode.Status.UNKNOWN, TopologyNode.Status.valueOf("UNKNOWN"));
        assertEquals(TopologyNode.Status.REACHABLE, TopologyNode.Status.valueOf("REACHABLE"));
        assertEquals(TopologyNode.Status.UNREACHABLE, TopologyNode.Status.valueOf("UNREACHABLE"));
        assertEquals(TopologyNode.Status.FAILURE, TopologyNode.Status.valueOf("FAILURE"));
    }

    @Test
    void settersAndGetters() {
        TopologyNode node = new TopologyNode("key", "name", TopologyNode.Kind.CONNECTOR);

        node.setDisplayName("new name");
        assertEquals("new name", node.getDisplayName());

        node.setConnectionDisplayName("conn name");
        assertEquals("conn name", node.getConnectionDisplayName());

        node.setKind(TopologyNode.Kind.FS_PATH);
        assertEquals(TopologyNode.Kind.FS_PATH, node.getKind());

        node.setHost("host");
        assertEquals("host", node.getHost());

        node.setPort(1234);
        assertEquals(1234, node.getPort());

        node.setX(100);
        assertEquals(100, node.getX());

        node.setY(200);
        assertEquals(200, node.getY());
    }
}

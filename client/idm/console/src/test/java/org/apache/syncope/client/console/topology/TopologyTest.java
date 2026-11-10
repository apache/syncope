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
    void sanitizeMarkupId() {
        assertEquals("hello_world", Topology.sanitizeMarkupId("hello world"));
        assertEquals("hello-world", Topology.sanitizeMarkupId("hello-world"));
        assertEquals("hello.world", Topology.sanitizeMarkupId("hello.world"));
        assertEquals("clean_id", Topology.sanitizeMarkupId("clean_id"));
        assertEquals("", Topology.sanitizeMarkupId(""));
        assertEquals("a_b_c", Topology.sanitizeMarkupId("a@b#c"));
        assertEquals("connid___example", Topology.sanitizeMarkupId("connid://example"));
    }
}

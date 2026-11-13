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
package org.apache.syncope.common.lib.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ConnectorCapabilityTest {

    @Test
    void capabilities() {
        assertEquals(8, ConnectorCapability.values().length);
        assertTrue(List.of(ConnectorCapability.AUTHENTICATE,
                ConnectorCapability.CREATE,
                ConnectorCapability.UPDATE,
                ConnectorCapability.UPDATE_DELTA,
                ConnectorCapability.DELETE,
                ConnectorCapability.SEARCH,
                ConnectorCapability.SYNC,
                ConnectorCapability.LIVE_SYNC).contains(ConnectorCapability.AUTHENTICATE));
        assertTrue(List.of(ConnectorCapability.AUTHENTICATE,
                ConnectorCapability.CREATE,
                ConnectorCapability.UPDATE,
                ConnectorCapability.UPDATE_DELTA,
                ConnectorCapability.DELETE,
                ConnectorCapability.SEARCH,
                ConnectorCapability.SYNC,
                ConnectorCapability.LIVE_SYNC).contains(ConnectorCapability.DELETE));
    }
}

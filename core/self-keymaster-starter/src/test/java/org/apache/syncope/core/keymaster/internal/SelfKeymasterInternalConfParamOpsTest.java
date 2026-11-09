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
package org.apache.syncope.core.keymaster.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.jackson.SyncopeJsonMapper;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class SelfKeymasterInternalConfParamOpsTest {

    private final InternalConfParamHelper helper = mock(InternalConfParamHelper.class);

    private final ConfParamOps ops = new SelfKeymasterInternalConfParamOps(helper);

    @Test
    void list() {
        Map<String, Object> expected = Map.of("key", "value");
        when(helper.list()).thenReturn(expected);

        assertSame(expected, ops.list("domain"));
        verify(helper).list();
    }

    @Test
    void getWhenNotFoundReturnsDefault() {
        when(helper.get("key")).thenReturn(null);

        assertEquals("default", ops.get("domain", "key", "default", String.class));
        verify(helper).get("key");
    }

    @Test
    void getDeserializesValue() {
        when(helper.get("key")).thenReturn(SyncopeJsonMapper.shared().valueToTree("value"));

        assertEquals("value", ops.get("domain", "key", "default", String.class));
        verify(helper).get("key");
    }

    @Test
    void setNullRemoves() {
        ops.set("domain", "key", null);

        verify(helper).remove("key");
    }

    @Test
    void setSerializesAndCallsHelper() {
        Map<String, Object> value = Map.of("a", 1);
        JsonNode expectedTree = SyncopeJsonMapper.shared().valueToTree(value);

        ops.set("domain", "key", value);

        verify(helper).set("key", expectedTree);
    }

    @Test
    void remove() {
        ops.remove("domain", "key");

        verify(helper).remove("key");
    }

    @Test
    void opsIsInstanceOfConfParamOps() {
        assertTrue(ops instanceof ConfParamOps);
    }
}

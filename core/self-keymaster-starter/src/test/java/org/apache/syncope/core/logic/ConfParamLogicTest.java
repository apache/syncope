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
package org.apache.syncope.core.logic;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.apache.syncope.core.keymaster.internal.InternalConfParamHelper;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class ConfParamLogicTest {

    private final InternalConfParamHelper helper = mock(InternalConfParamHelper.class);

    private final ConfParamLogic logic = new ConfParamLogic(helper);

    @Test
    void list() {
        Map<String, Object> expected = Map.of("key", "value");
        when(helper.list()).thenReturn(expected);

        assertSame(expected, logic.list());
        verify(helper).list();
    }

    @Test
    void get() {
        JsonNode expected = mock(JsonNode.class);
        when(helper.get("key")).thenReturn(expected);

        assertSame(expected, logic.get("key"));
        verify(helper).get("key");
    }

    @Test
    void set() {
        JsonNode value = mock(JsonNode.class);

        logic.set("key", value);

        verify(helper).set("key", value);
    }

    @Test
    void remove() {
        logic.remove("key");

        verify(helper).remove("key");
    }

    @Test
    void resolveReferenceThrows() {
        assertThrows(UnsupportedOperationException.class,
                () -> logic.resolveReference(null, new Object[0]));
    }
}

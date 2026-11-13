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
package org.apache.syncope.core.keymaster.rest.cxf.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import org.apache.syncope.core.logic.ConfParamLogic;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class ConfParamServiceImplTest {

    private final ConfParamLogic logic = mock(ConfParamLogic.class);

    private final ConfParamServiceImpl service = new ConfParamServiceImpl(logic);

    @Test
    void list() {
        Map<String, Object> expected = Map.of("key", "value");
        when(logic.list()).thenReturn(expected);

        assertSame(expected, service.list());
        verify(logic).list();
    }

    @Test
    void get() {
        JsonNode expected = mock(JsonNode.class);
        when(logic.get("key")).thenReturn(expected);

        Response response = service.get("key");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertSame(expected, response.getEntity());
        verify(logic).get("key");
    }

    @Test
    void set() {
        String json = "{\"key\":\"value\"}";
        InputStream body = new ByteArrayInputStream(json.getBytes());

        service.set("key", body);

        verify(logic).set("key", org.apache.syncope.common.lib.jackson.SyncopeJsonMapper.shared().
                readTree(json));
    }

    @Test
    void remove() {
        service.remove("key");

        verify(logic).remove("key");
    }
}

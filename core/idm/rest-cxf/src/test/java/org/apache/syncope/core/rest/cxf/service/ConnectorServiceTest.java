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
package org.apache.syncope.core.rest.cxf.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.core.logic.ConnectorLogic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ConnectorServiceTest {

    private ConnectorLogic logic;

    private ConnectorServiceImpl service;

    @BeforeEach
    void setUp() {
        logic = mock(ConnectorLogic.class);
        service = new ConnectorServiceImpl(logic);
        ReflectionTestUtils.setField(service, "uriInfo", mock(UriInfo.class));
    }

    @Test
    void list() {
        when(logic.list(anyString())).thenReturn(List.of());
        assertEquals(List.of(), service.list("en"));
    }

    @Test
    void read() {
        ConnInstanceTO to = new ConnInstanceTO();
        to.setKey("key");
        when(logic.read(eq("key"), anyString())).thenReturn(to);
        assertSame(to, service.read("key", "en"));
    }
}

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.core.logic.ResourceLogic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ResourceServiceTest {

    private ResourceLogic logic;

    private ResourceServiceImpl service;

    @BeforeEach
    void setUp() {
        logic = mock(ResourceLogic.class);
        service = new ResourceServiceImpl(logic);
        ReflectionTestUtils.setField(service, "uriInfo", mock(UriInfo.class));
    }

    @Test
    void list() {
        when(logic.list()).thenReturn(List.of());
        assertEquals(List.of(), service.list());
    }

    @Test
    void read() {
        ResourceTO to = new ResourceTO();
        to.setKey("key");
        when(logic.read("key")).thenReturn(to);
        assertSame(to, service.read("key"));
    }
}

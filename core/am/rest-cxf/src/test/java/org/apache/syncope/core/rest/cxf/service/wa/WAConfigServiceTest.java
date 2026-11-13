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
package org.apache.syncope.core.rest.cxf.service.wa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.rest.api.service.wa.WAConfigService.PushSubject;
import org.apache.syncope.core.logic.wa.WAConfigLogic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WAConfigServiceTest {

    private WAConfigLogic logic;

    private WAConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        logic = mock(WAConfigLogic.class);
        service = new WAConfigServiceImpl(logic);
    }

    @Test
    void list() {
        Attr attr = new Attr.Builder("schema").value("value").build();
        when(logic.list()).thenReturn(List.of(attr));
        assertEquals(List.of(attr), service.list());
    }

    @Test
    void delete() {
        service.delete("schema");
        verify(logic).delete("schema");
    }

    @Test
    void get() {
        Attr attr = new Attr.Builder("schema").value("value").build();
        when(logic.get("schema")).thenReturn(attr);
        assertSame(attr, service.get("schema"));
    }

    @Test
    void set() {
        Attr attr = new Attr.Builder("schema").value("value").build();
        service.set(attr);
        verify(logic).set(attr);
    }

    @Test
    void pushToWA() {
        service.pushToWA(PushSubject.conf, List.of("service"));
        verify(logic).pushToWA(PushSubject.conf, List.of("service"));
    }
}

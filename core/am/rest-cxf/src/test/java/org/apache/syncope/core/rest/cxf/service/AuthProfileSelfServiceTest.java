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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.syncope.common.lib.to.AuthProfileTO;
import org.apache.syncope.core.logic.AuthProfileLogic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthProfileSelfServiceTest {

    private AuthProfileLogic logic;

    private AuthProfileSelfServiceImpl service;

    @BeforeEach
    void setUp() {
        logic = mock(AuthProfileLogic.class);
        service = new AuthProfileSelfServiceImpl(logic);
    }

    @Test
    void read() {
        AuthProfileTO to = new AuthProfileTO();
        to.setKey("key");
        when(logic.selfRead()).thenReturn(to);
        assertSame(to, service.read());
    }

    @Test
    void update() {
        AuthProfileTO to = new AuthProfileTO();
        to.setKey("key");
        service.update(to);
        verify(logic).selfUpdate(to);
    }

    @Test
    void delete() {
        service.delete();
        verify(logic).selfDelete();
    }
}

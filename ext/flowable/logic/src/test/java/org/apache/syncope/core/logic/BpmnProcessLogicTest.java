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

import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.List;
import org.apache.syncope.common.lib.to.BpmnProcess;
import org.apache.syncope.common.lib.types.BpmnProcessFormat;
import org.apache.syncope.core.flowable.api.BpmnProcessManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BpmnProcessLogicTest {

    private BpmnProcessManager bpmnProcessManager;

    private BpmnProcessLogic logic;

    @BeforeEach
    void setup() {
        bpmnProcessManager = mock(BpmnProcessManager.class);
        logic = new BpmnProcessLogic(bpmnProcessManager);
    }

    @Test
    void delegate() {
        List<BpmnProcess> processes = List.of(new BpmnProcess());
        when(bpmnProcessManager.getProcesses()).thenReturn(processes);

        OutputStream os = mock(OutputStream.class);

        assertSame(processes, logic.list());

        logic.exportDefinition("key", BpmnProcessFormat.JSON, os);
        verify(bpmnProcessManager).exportProcess("key", BpmnProcessFormat.JSON, os);

        logic.exportDiagram("key", os);
        verify(bpmnProcessManager).exportDiagram("key", os);

        logic.importDefinition("key", BpmnProcessFormat.XML, "definition");
        verify(bpmnProcessManager).importProcess("key", BpmnProcessFormat.XML, "definition");

        logic.delete("key");
        verify(bpmnProcessManager).deleteProcess("key");
    }

    @Test
    void resolveReference() {
        assertThrows(UnresolvedReferenceException.class, () -> logic.resolveReference((Method) null));
    }
}

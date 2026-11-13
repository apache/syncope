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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.util.List;
import org.apache.syncope.common.rest.api.beans.CSVPullSpec;
import org.apache.syncope.core.logic.ReconciliationLogic;
import org.apache.syncope.core.persistence.api.search.AnySearchCondVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ReconciliationServiceTest {

    private ReconciliationLogic logic;

    private ReconciliationServiceImpl service;

    @BeforeEach
    void setUp() {
        logic = mock(ReconciliationLogic.class);
        service = new ReconciliationServiceImpl(mock(AnySearchCondVisitor.class), logic);
        ReflectionTestUtils.setField(service, "uriInfo", mock(UriInfo.class));
    }

    @Test
    void pull() {
        when(logic.pull(any(CSVPullSpec.class), any(InputStream.class))).thenReturn(List.of());
        service.pull(new CSVPullSpec(), mock(InputStream.class));
        verify(logic).pull(any(CSVPullSpec.class), any(InputStream.class));
    }
}

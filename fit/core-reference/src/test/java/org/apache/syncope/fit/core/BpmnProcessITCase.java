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
package org.apache.syncope.fit.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.to.BpmnProcess;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BpmnProcessITCase extends AbstractITCase {

    private static String USER_WORKFLOW_KEY = null;

    @BeforeAll
    public static void findDefault() {
        assumeFalse(CLIENT_FACTORY.getContentType() == SyncopeClientFactoryBean.ContentType.YAML);
        assumeTrue(IS_FLOWABLE_ENABLED);

        BPMN_PROCESS_SERVICE.list().stream().
                filter(BpmnProcess::isUserWorkflow).findAny().
                ifPresent(process -> USER_WORKFLOW_KEY = process.getKey());
        assertNotNull(USER_WORKFLOW_KEY);
    }

    @BeforeEach
    public void check() {
        assumeFalse(CLIENT_FACTORY.getContentType() == SyncopeClientFactoryBean.ContentType.YAML);
        assumeTrue(IS_FLOWABLE_ENABLED);
    }

    @Test
    public void exportUserWorkflowProcess() throws IOException {
        Response response = BPMN_PROCESS_SERVICE.get(USER_WORKFLOW_KEY);
        assertTrue(response.getMediaType().toString().
                startsWith(CLIENT_FACTORY.getContentType().getMediaType().toString()));
        String definition = response.readEntity(String.class);
        assertNotNull(definition);
        assertFalse(definition.isEmpty());
    }

    @Test
    public void updateUserWorkflowProcess() throws IOException {
        Response response = BPMN_PROCESS_SERVICE.get(USER_WORKFLOW_KEY);
        String definition = response.readEntity(String.class);

        BPMN_PROCESS_SERVICE.set(USER_WORKFLOW_KEY, definition);
    }
}

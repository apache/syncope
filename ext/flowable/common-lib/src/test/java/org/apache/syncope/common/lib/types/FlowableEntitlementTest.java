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
package org.apache.syncope.common.lib.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class FlowableEntitlementTest {

    @Test
    void values() {
        assertEquals(
                List.of(
                        FlowableEntitlement.BPMN_PROCESS_DELETE,
                        FlowableEntitlement.BPMN_PROCESS_GET,
                        FlowableEntitlement.BPMN_PROCESS_SET,
                        FlowableEntitlement.USER_REQUEST_CANCEL,
                        FlowableEntitlement.USER_REQUEST_FORM_CLAIM,
                        FlowableEntitlement.USER_REQUEST_FORM_LIST,
                        FlowableEntitlement.USER_REQUEST_FORM_SUBMIT,
                        FlowableEntitlement.USER_REQUEST_FORM_UNCLAIM,
                        FlowableEntitlement.USER_REQUEST_LIST,
                        FlowableEntitlement.USER_REQUEST_START,
                        FlowableEntitlement.WORKFLOW_TASK_LIST),
                List.copyOf(FlowableEntitlement.values()));

        assertTrue(FlowableEntitlement.values().contains(FlowableEntitlement.USER_REQUEST_FORM_CLAIM));
        assertThrows(
                UnsupportedOperationException.class,
                () -> FlowableEntitlement.values().add("unexpected"));
    }
}

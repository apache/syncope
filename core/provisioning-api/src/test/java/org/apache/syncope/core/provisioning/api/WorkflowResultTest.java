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
package org.apache.syncope.core.provisioning.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class WorkflowResultTest extends AbstractTest {

    @Test
    public void test(final @Mock PropagationByResource<String> propByRes) {
        String result = "result";
        Set<String> performedTasks = new HashSet<>();
        performedTasks.add("TEST");
        WorkflowResult<String> workflowResult = new WorkflowResult<>(result, propByRes, performedTasks);
        WorkflowResult<String> workflowResult2 = new WorkflowResult<>(result, propByRes, performedTasks);

        assertTrue(workflowResult.equals(workflowResult));
        assertTrue(workflowResult.equals(workflowResult2));
        assertFalse(workflowResult.equals(null));
        assertFalse(workflowResult.equals(String.class));

        result = "newResult";
        workflowResult.setResult(result);
        assertEquals(result, workflowResult.getResult());

        assertEquals(propByRes, workflowResult2.getPropByRes());
    }
}

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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

public class UserWorkflowResultTest extends AbstractTest {

    @Test
    public void test() {
        PropagationByResource<String> propByRes = new PropagationByResource<>();
        PropagationByResource<Pair<String, String>> propByLinkedAccount = new PropagationByResource<>();
        UserWorkflowResult<String> userWorkflowResult;
        UserWorkflowResult<String> userWorkflowResult2;
        String result = "true";
        String performedTask = "testTask";
        Set<String> performedTasks = new HashSet<>();
        performedTasks.add("testTask1");
        performedTasks.add("testTask2");
        performedTasks.add("testTask3");
        Object nullObj = null;

        userWorkflowResult = new UserWorkflowResult<>(result, propByRes, propByLinkedAccount, performedTask);
        userWorkflowResult2 = new UserWorkflowResult<>(result, propByRes, propByLinkedAccount, performedTasks);

        assertNotEquals(userWorkflowResult.hashCode(), userWorkflowResult2.hashCode());
        assertFalse(userWorkflowResult.equals(Object.class));
        assertFalse(userWorkflowResult.equals(nullObj));
        assertTrue(userWorkflowResult.equals(userWorkflowResult2));
        assertTrue(userWorkflowResult2.equals(userWorkflowResult2));
        assertNotEquals(userWorkflowResult.toString(), userWorkflowResult2.toString());
        assertEquals(performedTasks, userWorkflowResult2.getPerformedTasks());
    }
}

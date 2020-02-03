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
package org.apache.syncope.core.provisioning.api.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.core.provisioning.api.AbstractTest;
import org.junit.jupiter.api.Test;

public class AfterHandlingEventTest extends AbstractTest {

    @Test
    public void test() {
     String who = "testUser";
        AuditElements.EventCategoryType type = AuditElements.EventCategoryType.CUSTOM;
        String category = SyncopeConstants.REALM_ANYTYPE.toLowerCase();
        String subcategory = UUID.randomUUID().toString();
        String event = "testEvent";
        AuditElements.Result condition = AuditElements.Result.SUCCESS;
        Object before = "before";
        Object output = "output";
        Object[] input = new String[] { "value1", "value2" };
        AfterHandlingEvent afterHandlingEvent = new AfterHandlingEvent(
                who,
                type,
                category,
                subcategory, 
                event,
                condition,
                before, 
                output, 
                input);
        
        assertEquals(who, afterHandlingEvent.getWho());
        assertEquals(type, afterHandlingEvent.getType());
        assertEquals(category, afterHandlingEvent.getCategory());
        assertEquals(subcategory, afterHandlingEvent.getSubcategory());
        assertEquals(event, afterHandlingEvent.getEvent());
        assertEquals(condition, afterHandlingEvent.getCondition());
        assertEquals(before, afterHandlingEvent.getBefore());
        assertEquals(output, afterHandlingEvent.getOutput());
        assertEquals(input, afterHandlingEvent.getInput());
    }
}

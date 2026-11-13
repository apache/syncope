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
import static org.junit.jupiter.api.Assertions.assertSame;

import org.apache.syncope.common.lib.to.LiveSyncTaskTO;
import org.apache.syncope.common.lib.to.MacroTaskTO;
import org.apache.syncope.common.lib.to.NotificationTaskTO;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.junit.jupiter.api.Test;

class TaskTypeTest {

    @Test
    void toClass() {
        assertEquals(PropagationTaskTO.class, TaskType.PROPAGATION.getToClass());
        assertEquals(NotificationTaskTO.class, TaskType.NOTIFICATION.getToClass());
        assertEquals(SchedTaskTO.class, TaskType.SCHEDULED.getToClass());
        assertEquals(LiveSyncTaskTO.class, TaskType.LIVE_SYNC.getToClass());
        assertEquals(PullTaskTO.class, TaskType.PULL.getToClass());
        assertEquals(PushTaskTO.class, TaskType.PUSH.getToClass());
        assertEquals(MacroTaskTO.class, TaskType.MACRO.getToClass());
    }

    @Test
    void fromTOClass() {
        assertSame(TaskType.PUSH, TaskType.fromTOClass(PushTaskTO.class));
        assertSame(TaskType.LIVE_SYNC, TaskType.fromTOClass(LiveSyncTaskTO.class));
        assertSame(TaskType.PULL, TaskType.fromTOClass(PullTaskTO.class));
        assertSame(TaskType.NOTIFICATION, TaskType.fromTOClass(NotificationTaskTO.class));
        assertSame(TaskType.PROPAGATION, TaskType.fromTOClass(PropagationTaskTO.class));
        assertSame(TaskType.MACRO, TaskType.fromTOClass(MacroTaskTO.class));
        assertSame(TaskType.SCHEDULED, TaskType.fromTOClass(SchedTaskTO.class));
    }
}

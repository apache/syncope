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

import org.apache.syncope.common.lib.to.LiveSyncTaskTO;
import org.apache.syncope.common.lib.to.MacroTaskTO;
import org.apache.syncope.common.lib.to.NotificationTaskTO;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.TaskTO;

public enum TaskType {

    PROPAGATION(PropagationTaskTO.class),
    NOTIFICATION(NotificationTaskTO.class),
    SCHEDULED(SchedTaskTO.class),
    LIVE_SYNC(LiveSyncTaskTO.class),
    PULL(PullTaskTO.class),
    PUSH(PushTaskTO.class),
    MACRO(MacroTaskTO.class);

    private final Class<? extends TaskTO> toClass;

    TaskType(final Class<? extends TaskTO> toClass) {
        this.toClass = toClass;
    }

    public Class<? extends TaskTO> getToClass() {
        return toClass;
    }

    public static TaskType fromTOClass(final Class<? extends TaskTO> clazz) {
        return PushTaskTO.class.isAssignableFrom(clazz)
                ? TaskType.PUSH
                : LiveSyncTaskTO.class.isAssignableFrom(clazz)
                ? TaskType.LIVE_SYNC
                : PullTaskTO.class.isAssignableFrom(clazz)
                ? TaskType.PULL
                : NotificationTaskTO.class.isAssignableFrom(clazz)
                ? TaskType.NOTIFICATION
                : PropagationTaskTO.class.isAssignableFrom(clazz)
                ? TaskType.PROPAGATION
                : MacroTaskTO.class.isAssignableFrom(clazz)
                ? TaskType.MACRO
                : TaskType.SCHEDULED;
    }
}

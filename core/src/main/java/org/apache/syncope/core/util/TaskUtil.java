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
package org.apache.syncope.core.util;

import org.apache.syncope.core.persistence.beans.NotificationTask;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.SchedTask;
import org.apache.syncope.core.persistence.beans.SyncTask;
import org.apache.syncope.core.persistence.beans.Task;
import org.apache.syncope.to.NotificationTaskTO;
import org.apache.syncope.to.PropagationTaskTO;
import org.apache.syncope.to.SchedTaskTO;
import org.apache.syncope.to.SyncTaskTO;
import org.apache.syncope.to.TaskTO;

public enum TaskUtil {

    PROPAGATION,
    SCHED,
    SYNC,
    NOTIFICATION;

    public <T extends Task> Class<T> taskClass() {
        Class result = null;

        switch (this) {
            case PROPAGATION:
                result = PropagationTask.class;
                break;
            case SCHED:
                result = SchedTask.class;
                break;
            case SYNC:
                result = SyncTask.class;
                break;
            case NOTIFICATION:
                result = NotificationTask.class;
                break;
        }

        return result;
    }

    public <T extends Task> T newTask() {
        T result = null;

        switch (this) {
            case PROPAGATION:
                result = (T) new PropagationTask();
                break;
            case SCHED:
                result = (T) new SchedTask();
                break;
            case SYNC:
                result = (T) new SyncTask();
                break;
            case NOTIFICATION:
                result = (T) new NotificationTask();
                break;
        }

        return result;
    }

    public <T extends TaskTO> Class<T> taskTOClass() {
        Class result = null;

        switch (this) {
            case PROPAGATION:
                result = PropagationTaskTO.class;
                break;
            case SCHED:
                result = SchedTaskTO.class;
                break;
            case SYNC:
                result = SyncTaskTO.class;
                break;
            case NOTIFICATION:
                result = NotificationTaskTO.class;
                break;
        }

        return result;
    }

    public <T extends TaskTO> T newTaskTO() {
        T result = null;

        switch (this) {
            case PROPAGATION:
                result = (T) new PropagationTaskTO();
                break;
            case SCHED:
                result = (T) new SchedTaskTO();
                break;
            case SYNC:
                result = (T) new SyncTaskTO();
                break;
            case NOTIFICATION:
                result = (T) new NotificationTaskTO();
        }

        return result;
    }
}

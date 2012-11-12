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
package org.apache.syncope.core.propagation;

import org.apache.syncope.propagation.PropagationException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.TaskExec;
import org.apache.syncope.types.PropagationTaskExecStatus;

public class PriorityPropagationTaskExecutor extends AbstractPropagationTaskExecutor {

    /**
     * Sort the given collection by looking at related ExternalResource's priority, then execute.
     */
    @Override
    public void execute(final Collection<PropagationTask> tasks, final PropagationHandler handler)
            throws PropagationException {

        final List<PropagationTask> prioritizedTasks = new ArrayList<PropagationTask>(tasks);
        Collections.sort(prioritizedTasks, new PriorityComparator());

        for (PropagationTask task : prioritizedTasks) {
            LOG.debug("Execution started for {}", task);

            TaskExec execution = execute(task, handler);

            LOG.debug("Execution finished for {}, {}", task, execution);

            // Propagation is interrupted as soon as the result of the
            // communication with a primary resource is in error
            PropagationTaskExecStatus execStatus;
            try {
                execStatus = PropagationTaskExecStatus.valueOf(execution.getStatus());
            } catch (IllegalArgumentException e) {
                LOG.error("Unexpected execution status found {}", execution.getStatus());
                execStatus = PropagationTaskExecStatus.FAILURE;
            }
            if (task.getResource().isPropagationPrimary() && !execStatus.isSuccessful()) {
                throw new PropagationException(task.getResource().getName(), execution.getMessage());
            }
        }
    }

    protected static class PriorityComparator implements Comparator<PropagationTask> {

        @Override
        public int compare(final PropagationTask task1, final PropagationTask task2) {
            return task1.getResource().getPropagationPriority() > task2.getResource().getPropagationPriority()
                    ? -1
                    : task1.getResource().getPropagationPriority() == task2.getResource().getPropagationPriority()
                    ? 0
                    : 1;
        }
    }
}

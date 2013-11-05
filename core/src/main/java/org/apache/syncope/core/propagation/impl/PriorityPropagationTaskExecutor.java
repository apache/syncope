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
package org.apache.syncope.core.propagation.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.syncope.common.types.AuditElements;
import org.apache.syncope.common.types.PropagationTaskExecStatus;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.TaskExec;
import org.apache.syncope.core.propagation.PropagationException;
import org.apache.syncope.core.propagation.PropagationReporter;

public class PriorityPropagationTaskExecutor extends AbstractPropagationTaskExecutor {

    /**
     * Sort the given collection by looking at related ExternalResource's priority, then execute.
     * {@inheritDoc}
     */
    @Override
    public void execute(final Collection<PropagationTask> tasks, final PropagationReporter reporter) {
        final List<PropagationTask> prioritizedTasks = new ArrayList<PropagationTask>(tasks);
        Collections.sort(prioritizedTasks, new PriorityComparator());

        try {
            for (PropagationTask task : prioritizedTasks) {
                LOG.debug("Execution started for {}", task);

                TaskExec execution = execute(task, reporter);

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
        } finally {
            notificationManager.createTasks(
                    AuditElements.EventCategoryType.PROPAGATION,
                    null,
                    null,
                    null,
                    null,
                    reporter instanceof DefaultPropagationReporter
                    ? ((DefaultPropagationReporter) reporter).getStatuses() : null,
                    tasks);

            auditManager.audit(
                    AuditElements.EventCategoryType.PROPAGATION,
                    null,
                    null,
                    null,
                    null,
                    reporter instanceof DefaultPropagationReporter
                    ? ((DefaultPropagationReporter) reporter).getStatuses() : null,
                    tasks);
        }
    }

    /**
     * Compare propagation tasks according to related ExternalResource's priority.
     *
     * @see PropagationTask
     * @see org.apache.syncope.core.persistence.beans.ExternalResource#propagationPriority
     */
    protected static class PriorityComparator implements Comparator<PropagationTask>, Serializable {

        private static final long serialVersionUID = -1969355670784448878L;

        @Override
        public int compare(final PropagationTask task1, final PropagationTask task2) {
            int prop1 = task1.getResource().getPropagationPriority() == null
                    ? Integer.MIN_VALUE
                    : task1.getResource().getPropagationPriority().intValue();
            int prop2 = task2.getResource().getPropagationPriority() == null
                    ? Integer.MIN_VALUE
                    : task2.getResource().getPropagationPriority().intValue();

            return prop1 > prop2
                    ? 1
                    : prop1 == prop2
                    ? 0
                    : -1;
        }
    }
}

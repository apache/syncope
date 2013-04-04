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

import java.util.Collection;

import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.TaskExec;

public interface PropagationTaskExecutor {

    /**
     * Execute the given PropagationTask and returns the generated TaskExec.
     *
     * @param task to be executed
     * @return the generated TaskExec
     */
    TaskExec execute(PropagationTask task);

    /**
     * Execute the given PropagationTask, invoke the given handler and returns the generated TaskExec.
     *
     * @param task to be executed
     * @param handler to be invoked
     * @return the generated TaskExec
     */
    TaskExec execute(PropagationTask task, PropagationHandler handler);

    /**
     * Execute a collection of PropagationTask objects.
     *
     * @param tasks to be executed
     * @throws PropagationException if propagation goes wrong: propagation is interrupted as soon as the result of the
     * communication with a primary resource is in error
     */
    void execute(Collection<PropagationTask> tasks) throws PropagationException;

    /**
     * Execute a collection of PropagationTask objects and invoke the given handler on each of these.
     *
     * @param tasks to be execute, in given order
     * @param handler propagation handler
     * @throws PropagationException if propagation goes wrong: propagation is interrupted as soon as the result of the
     * communication with a primary resource is in error
     */
    void execute(Collection<PropagationTask> tasks, PropagationHandler handler) throws PropagationException;
}

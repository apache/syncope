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
package org.apache.syncope.core.persistence.neo4j.entity.task;

import java.util.List;
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractGeneratedKeyNode;

public abstract class AbstractTask<T extends Task<T>> extends AbstractGeneratedKeyNode implements Task<T> {

    private static final long serialVersionUID = 5837401178128177511L;

    protected abstract List<? extends AbstractTaskExec<T>> executions();

    protected abstract Class<? extends AbstractTaskExec<T>> executionClass();

    protected abstract boolean doAdd(TaskExec<T> exec);
    
    @Override
    public boolean add(final TaskExec<T> exec) {
        Class<? extends AbstractTaskExec<T>> clazz = executionClass();
        checkType(exec, clazz);
        return exec != null
                && !executions().contains(clazz.cast(exec))
                && doAdd(exec);
    }

    @Override
    public List<? extends TaskExec<T>> getExecs() {
        return executions();
    }
}

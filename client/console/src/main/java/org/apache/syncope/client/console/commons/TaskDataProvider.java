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
package org.apache.syncope.client.console.commons;

import org.apache.syncope.common.lib.to.TaskTO;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;

public abstract class TaskDataProvider<T extends TaskTO> extends DirectoryDataProvider<T> {

    private static final long serialVersionUID = -20112718133295756L;

    protected final TaskType taskType;

    public TaskDataProvider(final int paginatorRows, final TaskType taskType) {
        super(paginatorRows);

        setSort("start", SortOrder.ASCENDING);
        this.taskType = taskType;
    }

    @Override
    public IModel<T> model(final T object) {
        return new CompoundPropertyModel<>(object);
    }
}

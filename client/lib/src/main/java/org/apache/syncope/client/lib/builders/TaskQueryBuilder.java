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
package org.apache.syncope.client.lib.builders;

import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.rest.api.beans.TaskQuery;

public class TaskQueryBuilder extends AbstractQueryBuilder<TaskQuery, TaskQueryBuilder> {

    @Override
    protected TaskQuery newInstance() {
        return new TaskQuery();
    }

    @Override
    public TaskQueryBuilder page(final Integer page) {
        return TaskQueryBuilder.class.cast(super.page(page));
    }

    @Override
    public TaskQueryBuilder size(final Integer size) {
        return TaskQueryBuilder.class.cast(super.size(size));
    }

    @Override
    public TaskQueryBuilder orderBy(final String orderBy) {
        return TaskQueryBuilder.class.cast(super.orderBy(orderBy));
    }

    public TaskQueryBuilder resource(final String resource) {
        getInstance().setResource(resource);
        return this;
    }

    public TaskQueryBuilder anyTypeKind(final AnyTypeKind anyTypeKind) {
        getInstance().setAnyTypeKind(anyTypeKind);
        return this;
    }

    public TaskQueryBuilder anyTypeKey(final Long anyTypeKey) {
        getInstance().setAnyTypeKey(anyTypeKey);
        return this;
    }
}

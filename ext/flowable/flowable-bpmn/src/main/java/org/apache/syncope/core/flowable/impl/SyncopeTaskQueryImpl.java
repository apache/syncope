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
package org.apache.syncope.core.flowable.impl;

import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.service.impl.TaskQueryImpl;

public class SyncopeTaskQueryImpl extends TaskQueryImpl {

    private static final long serialVersionUID = 734215641378485689L;

    protected boolean withFormKey;

    protected SyncopeTaskQueryImpl currentOrQueryObject;

    public SyncopeTaskQueryImpl(final CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    public TaskQuery taskWithFormKey() {
        if (orActive) {
            currentOrQueryObject.withFormKey = true;
        } else {
            this.withFormKey = true;
        }
        return this;
    }

    public boolean isWithFormKey() {
        return withFormKey;
    }
}

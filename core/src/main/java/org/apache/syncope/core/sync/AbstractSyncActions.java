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
package org.apache.syncope.core.sync;

import java.util.List;
import org.apache.syncope.core.sync.impl.AbstractSyncopeResultHandler;
import org.quartz.JobExecutionException;

public interface AbstractSyncActions<T extends AbstractSyncopeResultHandler<?, ?>> {

    /**
     * Action to be executed before to start the synchronization task execution.
     *
     * @param handler synchronization handler being executed.
     * @throws JobExecutionException in case of generic failure.
     */
    void beforeAll(final SyncProfile<?, ?> profile) throws JobExecutionException;

    /**
     * Action to be executed after the synchronization task completion.
     *
     * @param handler synchronization handler being executed.
     * @param results synchronization result
     * @throws JobExecutionException in case of generic failure
     */
    void afterAll(final SyncProfile<?, ?> profile, final List<SyncResult> results) throws JobExecutionException;
}

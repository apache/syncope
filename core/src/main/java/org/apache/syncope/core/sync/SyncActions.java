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
import org.apache.syncope.client.mod.UserMod;
import org.apache.syncope.client.to.UserTO;
import org.apache.syncope.core.persistence.beans.SyncTask;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.quartz.JobExecutionException;

/**
 * Interface for actions to be performed during SyncJob execution.
 */
public interface SyncActions {

    /**
     * Action to be executed before to start the synchronization task execution.
     *
     * @param task synchronization task to be executed.
     * @throws JobExecutionException in case of generic failure.
     */
    void beforeAll(final SyncTask task) throws JobExecutionException;

    /**
     * Action to be executed before to create a synchronized user locally.
     *
     * @param delta retrieved synchronization information.
     * @param user user to be created.
     * @return synchronization information used for user status evaluation and to be passed to the 'after' method.
     * @throws JobExecutionException in case of generic failure.
     */
    SyncDelta beforeCreate(final SyncDelta delta, final UserTO user) throws JobExecutionException;

    /**
     * Action to be executed before to update a synchronized user locally.
     *
     * @param delta retrieved synchronization information.
     * @param user local user information.
     * @param userMod modification.
     * @return synchronization information used for logging and to be passed to the 'after' method.
     * @throws JobExecutionException in case of generic failure.
     */
    SyncDelta beforeUpdate(final SyncDelta delta, final UserTO user, final UserMod userMod)
            throws JobExecutionException;

    /**
     * Action to be executed before to delete a synchronized user locally.
     *
     * @param delta retrieved synchronization information.
     * @param userlocal user to be deleted.
     * @return synchronization information used for logging and to be passed to the 'after' method.
     * @throws JobExecutionException in case of generic failure.
     */
    SyncDelta beforeDelete(final SyncDelta delta, final UserTO user) throws JobExecutionException;

    /**
     * Action to be executed after each local user synchronization.
     *
     * @param delta retrieved synchronization information (may be modified by 'beforeCreate/beforeUpdate/beforeDelete').
     * @param user synchronized local user.
     * @param result global synchronization results at the current synchronization step.
     * @throws JobExecutionException in case of generic failure.
     */
    void after(final SyncDelta delta, final UserTO user, final SyncResult result) throws JobExecutionException;

    /**
     * Action to be executed after the synchronization task completion.
     *
     * @param task executed synchronization task.
     * @param results synchronization result.
     * @throws JobExecutionException in case of generic failure.
     */
    void afterAll(final SyncTask task, final List<SyncResult> results) throws JobExecutionException;
}

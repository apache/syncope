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

import java.util.Map;
import java.util.Set;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.sync.impl.AbstractSyncopeResultHandler;
import org.identityconnectors.framework.common.objects.Attribute;
import org.quartz.JobExecutionException;

/**
 * Interface for actions to be performed during PushJob execution.
 */
public interface PushActions extends AbstractSyncActions<AbstractSyncopeResultHandler> {

    /**
     * Action to be executed before to create a synchronized user locally.
     *
     * @param handler synchronization handler being executed.
     * @param subject user / role to be created
     * @param delta info to be pushed out (accountId, attributes)
     * @return info to be pushed out (accountId, attributes).
     * @throws JobExecutionException in case of generic failure
     */
    <T extends AbstractAttributable> Map.Entry<String, Set<Attribute>> beforeCreate(
            final AbstractSyncopeResultHandler handler,
            final T subject,
            final Map.Entry<String, Set<Attribute>> delta) throws JobExecutionException;

    /**
     * Action to be executed before to update a synchronized user locally.
     *
     * @param handler synchronization handler being executed.
     * @param subject user / role to be created
     * @param delta info to be pushed out (accountId, attributes)
     * @return info to be pushed out (accountId, attributes).
     * @throws JobExecutionException in case of generic failure
     */
    <T extends AbstractAttributable> Map.Entry<String, Set<Attribute>> beforeUpdate(
            final AbstractSyncopeResultHandler handler,
            final T subject,
            final Map.Entry<String, Set<Attribute>> delta) throws JobExecutionException;

    /**
     * Action to be executed after each local user synchronization.
     *
     * @param handler synchronization handler being executed.
     * @param subject user / role to be created
     * @param delta info pushed out (accountId, attributes)
     * @param result operation result.
     * @throws JobExecutionException in case of generic failure
     */
    <T extends AbstractAttributable> void after(
            final AbstractSyncopeResultHandler handler,
            final T subject,
            final Map.Entry<String, Set<Attribute>> delta,
            final SyncResult result) throws JobExecutionException;
}

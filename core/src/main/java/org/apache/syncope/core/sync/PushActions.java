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
public interface PushActions extends AbstractSyncActions<AbstractSyncopeResultHandler<?, ?>> {

    /**
     * Action to be executed before to assign (link & provision) a synchronized user to the resource.
     *
     * @param handler synchronization handler being executed.
     * @param delta info to be pushed out (accountId, attributes).
     * @param subject user / role to be created.
     * @return info to be pushed out (accountId, attributes).
     * @throws JobExecutionException in case of generic failure
     */
    <T extends AbstractAttributable> Map.Entry<String, Set<Attribute>> beforeAssign(
            final AbstractSyncopeResultHandler<?, ?> handler,
            final Map.Entry<String, Set<Attribute>> delta,
            final T subject) throws JobExecutionException;

    /**
     * Action to be executed before to provision a synchronized user to the resource.
     *
     * @param handler synchronization handler being executed.
     * @param delta info to be pushed out (accountId, attributes).
     * @param subject user / role to be created.
     * @return info to be pushed out (accountId, attributes).
     * @throws JobExecutionException in case of generic failure
     */
    <T extends AbstractAttributable> Map.Entry<String, Set<Attribute>> beforeProvision(
            final AbstractSyncopeResultHandler<?, ?> handler,
            final Map.Entry<String, Set<Attribute>> delta,
            final T subject) throws JobExecutionException;

    /**
     * Action to be executed before to update a synchronized user on the resource.
     *
     * @param handler synchronization handler being executed.
     * @param delta info to be pushed out (accountId, attributes).
     * @param subject user / role to be updated.
     * @return info to be pushed out (accountId, attributes).
     * @throws JobExecutionException in case of generic failure
     */
    <T extends AbstractAttributable> Map.Entry<String, Set<Attribute>> beforeUpdate(
            final AbstractSyncopeResultHandler<?, ?> handler,
            final Map.Entry<String, Set<Attribute>> delta,
            final T subject) throws JobExecutionException;

    /**
     * Action to be executed before to link a synchronized user to the resource.
     *
     * @param handler synchronization handler being executed.
     * @param delta info to be pushed out (accountId, attributes).
     * @param subject user / role to be created.
     * @return info to be pushed out (accountId, attributes).
     * @throws JobExecutionException in case of generic failure
     */
    <T extends AbstractAttributable> Map.Entry<String, Set<Attribute>> beforeLink(
            final AbstractSyncopeResultHandler<?, ?> handler,
            final Map.Entry<String, Set<Attribute>> delta,
            final T subject) throws JobExecutionException;

    /**
     * Action to be executed before to unlink a synchronized user from the resource.
     *
     * @param handler synchronization handler being executed.
     * @param delta info to be pushed out (accountId, attributes).
     * @param subject user / role to be created.
     * @return info to be pushed out (accountId, attributes).
     * @throws JobExecutionException in case of generic failure
     */
    <T extends AbstractAttributable> Map.Entry<String, Set<Attribute>> beforeUnlink(
            final AbstractSyncopeResultHandler<?, ?> handler,
            final Map.Entry<String, Set<Attribute>> delta,
            final T subject) throws JobExecutionException;

    /**
     * Action to be executed before to unassign a synchronized user from the resource.
     *
     * @param handler synchronization handler being executed.
     * @param delta info to be pushed out (accountId, attributes).
     * @param subject user / role to be created.
     * @return info to be pushed out (accountId, attributes).
     * @throws JobExecutionException in case of generic failure
     */
    <T extends AbstractAttributable> Map.Entry<String, Set<Attribute>> beforeUnassign(
            final AbstractSyncopeResultHandler<?, ?> handler,
            final Map.Entry<String, Set<Attribute>> delta,
            final T subject) throws JobExecutionException;

    /**
     * Action to be executed before to unassign a synchronized user from the resource.
     *
     * @param handler synchronization handler being executed.
     * @param delta info to be pushed out (accountId, attributes).
     * @param subject user / role to be created.
     * @return info to be pushed out (accountId, attributes).
     * @throws JobExecutionException in case of generic failure
     */
    <T extends AbstractAttributable> Map.Entry<String, Set<Attribute>> beforeDeprovision(
            final AbstractSyncopeResultHandler<?, ?> handler,
            final Map.Entry<String, Set<Attribute>> delta,
            final T subject) throws JobExecutionException;

    /**
     * Action to be executed before delete a synchronized user locally and from the resource.
     *
     * @param handler synchronization handler being executed.
     * @param delta info to be pushed out (accountId, attributes).
     * @param subject user / role to be created.
     * @return info to be pushed out (accountId, attributes).
     * @throws JobExecutionException in case of generic failure
     */
    <T extends AbstractAttributable> Map.Entry<String, Set<Attribute>> beforeDelete(
            final AbstractSyncopeResultHandler<?, ?> handler,
            final Map.Entry<String, Set<Attribute>> delta,
            final T subject) throws JobExecutionException;

    /**
     * Action to be executed after each local user synchronization.
     *
     * @param handler synchronization handler being executed.
     * @param delta info pushed out (accountId, attributes)
     * @param subject synchronized user / role.
     * @param result operation result.
     * @throws JobExecutionException in case of generic failure
     */
    <T extends AbstractAttributable> void after(
            final AbstractSyncopeResultHandler<?, ?> handler,
            final Map.Entry<String, Set<Attribute>> delta,
            final T subject,
            final SyncResult result) throws JobExecutionException;
}

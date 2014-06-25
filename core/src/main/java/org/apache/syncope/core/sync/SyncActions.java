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

import org.apache.syncope.common.mod.AbstractAttributableMod;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.core.sync.impl.AbstractSyncopeResultHandler;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.quartz.JobExecutionException;

/**
 * Interface for actions to be performed during SyncJob execution.
 */
public interface SyncActions extends AbstractSyncActions<AbstractSyncopeResultHandler<?, ?>> {

    /**
     * Action to be executed before to create a synchronized user / role locally.
     * User/role is created locally upon synchronization in case of the un-matching rule
     * {@link org.apache.syncope.common.types.UnmatchingRule#PROVISION} (default un-matching rule) is applied.
     *
     * @param profile profile of the synchronization being executed.
     * @param delta retrieved synchronization information
     * @param subject user / role to be created
     * @return synchronization information used for user status evaluation and to be passed to the 'after' method.
     * @throws JobExecutionException in case of generic failure
     */
    <T extends AbstractAttributableTO> SyncDelta beforeCreate(
            final SyncProfile<?, ?> profile,
            final SyncDelta delta,
            final T subject) throws JobExecutionException;

    /**
     * Action to be executed before creating (and linking to the resource) a synchronized user / role locally.
     * User/role is created locally and linked to the synchronized resource upon synchronization in case of the
     * un-matching rule {@link org.apache.syncope.common.types.UnmatchingRule#ASSIGN} is applied.
     *
     * @param profile profile of the synchronization being executed.
     * @param delta retrieved synchronization information
     * @param subject user / role to be created
     * @return synchronization information used for user status evaluation and to be passed to the 'after' method.
     * @throws JobExecutionException in case of generic failure
     */
    <T extends AbstractAttributableTO> SyncDelta beforeAssign(
            final SyncProfile<?, ?> profile,
            final SyncDelta delta,
            final T subject) throws JobExecutionException;

    /**
     * Action to be executed before unlinking resource from the synchronized user / role and de-provisioning.
     * User/role is unlinked and de-provisioned from the synchronized resource upon synchronization in case of the
     * matching rule {@link org.apache.syncope.common.types.MatchingRule#UNASSIGN} is applied.
     *
     * @param profile profile of the synchronization being executed.
     * @param delta retrieved synchronization information
     * @param subject user / role to be created
     * @return synchronization information used for user status evaluation and to be passed to the 'after' method.
     * @throws JobExecutionException in case of generic failure
     */
    <T extends AbstractAttributableTO> SyncDelta beforeUnassign(
            final SyncProfile<?, ?> profile,
            final SyncDelta delta,
            final T subject) throws JobExecutionException;

    /**
     * Action to be executed before de-provisioning action only.
     * User/role is de-provisioned (without unlinking) from the synchronized resource upon synchronization in case of
     * the matching rule {@link org.apache.syncope.common.types.MatchingRule#DEPROVISION} is applied.
     *
     * @param profile profile of the synchronization being executed.
     * @param delta retrieved synchronization information
     * @param subject user / role to be created
     * @return synchronization information used for user status evaluation and to be passed to the 'after' method.
     * @throws JobExecutionException in case of generic failure
     */
    <T extends AbstractAttributableTO> SyncDelta beforeDeprovision(
            final SyncProfile<?, ?> profile,
            final SyncDelta delta,
            final T subject) throws JobExecutionException;

    /**
     * Action to be executed before unlinking resource from the synchronized user / role.
     * User/role is unlinked (without de-provisioning) from the synchronized resource upon synchronization in case of
     * the matching rule {@link org.apache.syncope.common.types.MatchingRule#UNLINK} is applied.
     *
     * @param profile profile of the synchronization being executed.
     * @param delta retrieved synchronization information
     * @param subject user / role to be created
     * @return synchronization information used for user status evaluation and to be passed to the 'after' method.
     * @throws JobExecutionException in case of generic failure
     */
    <T extends AbstractAttributableTO> SyncDelta beforeUnlink(
            final SyncProfile<?, ?> profile,
            final SyncDelta delta,
            final T subject) throws JobExecutionException;

    /**
     * Action to be executed before linking resource to the synchronized user / role.
     * User/role is linked (without updating) to the synchronized resource upon synchronization in case of
     * the matching rule {@link org.apache.syncope.common.types.MatchingRule#LINK} is applied.
     *
     * @param profile profile of the synchronization being executed.
     * @param delta retrieved synchronization information
     * @param subject user / role to be created
     * @return synchronization information used for user status evaluation and to be passed to the 'after' method.
     * @throws JobExecutionException in case of generic failure
     */
    <T extends AbstractAttributableTO> SyncDelta beforeLink(
            final SyncProfile<?, ?> profile,
            final SyncDelta delta,
            final T subject) throws JobExecutionException;

    /**
     * Action to be executed before to update a synchronized user / role locally.
     * User/role is updated upon synchronization in case of the matching rule
     * {@link org.apache.syncope.common.types.MatchingRule#UPDATE} (default matching rule) is applied.
     *
     * @param profile profile of the synchronization being executed.
     * @param delta retrieved synchronization information
     * @param subject local user / role information
     * @param subjectMod modification
     * @return synchronization information used for logging and to be passed to the 'after' method.
     * @throws JobExecutionException in case of generic failure.
     */
    <T extends AbstractAttributableTO, K extends AbstractAttributableMod> SyncDelta beforeUpdate(
            final SyncProfile<?, ?> profile,
            final SyncDelta delta,
            final T subject,
            final K subjectMod)
            throws JobExecutionException;

    /**
     * Action to be executed before to delete a synchronized user / role locally.
     *
     * @param profile profile of the synchronization being executed.
     * @param delta retrieved synchronization information
     * @param subject local user / role to be deleted
     * @return synchronization information used for logging and to be passed to the 'after' method.
     * @throws JobExecutionException in case of generic failure
     */
    <T extends AbstractAttributableTO> SyncDelta beforeDelete(
            final SyncProfile<?, ?> profile,
            final SyncDelta delta,
            final T subject) throws JobExecutionException;

    /**
     * Action to be executed after each local user / role synchronization.
     *
     * @param profile profile of the synchronization being executed.
     * @param delta retrieved synchronization information (may be modified by 'beforeCreate/beforeUpdate/beforeDelete')
     * @param subject synchronized local user / role
     * @param result global synchronization results at the current synchronization step
     * @throws JobExecutionException in case of generic failure
     */
    <T extends AbstractAttributableTO> void after(
            final SyncProfile<?, ?> profile,
            final SyncDelta delta,
            final T subject,
            final SyncResult result) throws JobExecutionException;
}

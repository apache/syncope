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
package org.apache.syncope.core.provisioning.api.sync;

import org.apache.syncope.common.lib.mod.AnyMod;
import org.apache.syncope.common.lib.to.AnyTO;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.quartz.JobExecutionException;

/**
 * Interface for actions to be performed during SyncJob execution.
 * <br/>
 * All methods can throw {@link IgnoreProvisionException} to make the current any object ignored by the synchronization
 * process.
 */
public interface SyncActions extends ProvisioningActions {

    /**
     * Action to be executed before to create a synchronized user / group locally.
     * User/group is created locally upon synchronization in case of the un-matching rule
     * {@link org.apache.syncope.common.types.UnmatchingRule#PROVISION} (default un-matching rule) is applied.
     *
     * @param profile profile of the synchronization being executed.
     * @param delta retrieved synchronization information
     * @param any any object
     * @return synchronization information used for user status evaluation and to be passed to the 'after' method.
     * @throws JobExecutionException in case of generic failure
     */
    <T extends AnyTO> SyncDelta beforeProvision(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            T any) throws JobExecutionException;

    /**
     * Action to be executed before creating (and linking to the resource) a synchronized user / group locally.
     * User/group is created locally and linked to the synchronized resource upon synchronization in case of the
     * un-matching rule {@link org.apache.syncope.common.types.UnmatchingRule#ASSIGN} is applied.
     *
     * @param profile profile of the synchronization being executed.
     * @param delta retrieved synchronization information
     * @param any any object
     * @return synchronization information used for user status evaluation and to be passed to the 'after' method.
     * @throws JobExecutionException in case of generic failure
     */
    <T extends AnyTO> SyncDelta beforeAssign(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            T any) throws JobExecutionException;

    /**
     * Action to be executed before unlinking resource from the synchronized user / group and de-provisioning.
     * User/group is unlinked and de-provisioned from the synchronized resource upon synchronization in case of the
     * matching rule {@link org.apache.syncope.common.types.MatchingRule#UNASSIGN} is applied.
     *
     * @param profile profile of the synchronization being executed.
     * @param delta retrieved synchronization information
     * @param any any object
     * @return synchronization information used for user status evaluation and to be passed to the 'after' method.
     * @throws JobExecutionException in case of generic failure
     */
    <T extends AnyTO> SyncDelta beforeUnassign(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            T any) throws JobExecutionException;

    /**
     * Action to be executed before de-provisioning action only.
     * User/group is de-provisioned (without unlinking) from the synchronized resource upon synchronization in case of
     * the matching rule {@link org.apache.syncope.common.types.MatchingRule#DEPROVISION} is applied.
     *
     * @param profile profile of the synchronization being executed.
     * @param delta retrieved synchronization information
     * @param any any object
     * @return synchronization information used for user status evaluation and to be passed to the 'after' method.
     * @throws JobExecutionException in case of generic failure
     */
    <T extends AnyTO> SyncDelta beforeDeprovision(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            T any) throws JobExecutionException;

    /**
     * Action to be executed before unlinking resource from the synchronized user / group.
     * User/group is unlinked (without de-provisioning) from the synchronized resource upon synchronization in case of
     * the matching rule {@link org.apache.syncope.common.types.MatchingRule#UNLINK} is applied.
     *
     * @param profile profile of the synchronization being executed.
     * @param delta retrieved synchronization information
     * @param any any object
     * @return synchronization information used for user status evaluation and to be passed to the 'after' method.
     * @throws JobExecutionException in case of generic failure
     */
    <T extends AnyTO> SyncDelta beforeUnlink(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            T any) throws JobExecutionException;

    /**
     * Action to be executed before linking resource to the synchronized user / group.
     * User/group is linked (without updating) to the synchronized resource upon synchronization in case of
     * the matching rule {@link org.apache.syncope.common.types.MatchingRule#LINK} is applied.
     *
     * @param profile profile of the synchronization being executed.
     * @param delta retrieved synchronization information
     * @param any any object
     * @return synchronization information used for user status evaluation and to be passed to the 'after' method.
     * @throws JobExecutionException in case of generic failure
     */
    <T extends AnyTO> SyncDelta beforeLink(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            T any) throws JobExecutionException;

    /**
     * Action to be executed before to update a synchronized user / group locally.
     * User/group is updated upon synchronization in case of the matching rule
     * {@link org.apache.syncope.common.types.MatchingRule#UPDATE} (default matching rule) is applied.
     *
     * @param profile profile of the synchronization being executed.
     * @param delta retrieved synchronization information
     * @param any any object
     * @param anyMod modification
     * @return synchronization information used for logging and to be passed to the 'after' method.
     * @throws JobExecutionException in case of generic failure.
     */
    <T extends AnyTO, K extends AnyMod> SyncDelta beforeUpdate(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            T any,
            K anyMod)
            throws JobExecutionException;

    /**
     * Action to be executed before to delete a synchronized user / group locally.
     *
     * @param profile profile of the synchronization being executed.
     * @param delta retrieved synchronization information
     * @param any any object to be deleted
     * @return synchronization information used for logging and to be passed to the 'after' method.
     * @throws JobExecutionException in case of generic failure
     */
    <T extends AnyTO> SyncDelta beforeDelete(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            T any) throws JobExecutionException;

    /**
     * Action to be executed when user / group synchronization goes on error.
     *
     * @param profile profile of the synchronization being executed.
     * @param delta retrieved synchronization information (may be modified by
     * 'beforeProvision/beforeUpdate/beforeDelete')
     * @param result global synchronization results at the current synchronization step
     * @param error error being reported
     * @throws JobExecutionException in case of generic failure
     */
    void onError(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            ProvisioningResult result,
            Exception error) throws JobExecutionException;

    /**
     * Action to be executed after each local user / group synchronization.
     *
     * @param profile profile of the synchronization being executed.
     * @param delta retrieved synchronization information (may be modified by
     * 'beforeProvision/beforeUpdate/beforeDelete')
     * @param any any object
     * @param result global synchronization results at the current synchronization step
     * @throws JobExecutionException in case of generic failure
     */
    <T extends AnyTO> void after(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            T any,
            ProvisioningResult result) throws JobExecutionException;
}

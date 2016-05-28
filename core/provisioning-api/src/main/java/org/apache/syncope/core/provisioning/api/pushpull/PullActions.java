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
package org.apache.syncope.core.provisioning.api.pushpull;

import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.to.AnyTO;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.quartz.JobExecutionException;

/**
 * Interface for actions to be performed during pull.
 * All methods can throw {@link IgnoreProvisionException} to make the current any object ignored by the pull
 * process.
 */
public interface PullActions extends ProvisioningActions {

    /**
     * Action to be executed before to create a pulled user / group locally.
     * User/group is created locally upon pull in case of the un-matching rule
     * {@link org.apache.syncope.common.lib.types.UnmatchingRule#PROVISION} (default un-matching rule) is applied.
     *
     * @param <A> concrete any object
     * @param profile profile of the pull being executed.
     * @param delta retrieved pull information
     * @param any any object
     * @return pull information used for user status evaluation and to be passed to the 'after' method.
     * @throws JobExecutionException in case of generic failure
     */
    <A extends AnyTO> SyncDelta beforeProvision(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            A any) throws JobExecutionException;

    /**
     * Action to be executed before creating (and linking to the resource) a pulled user / group locally.
     * User/group is created locally and linked to the pulled resource upon pull in case of the
     * un-matching rule {@link org.apache.syncope.common.lib.types.UnmatchingRule#ASSIGN} is applied.
     *
     * @param <A> concrete any object
     * @param profile profile of the pull being executed.
     * @param delta retrieved pull information
     * @param any any object
     * @return pull information used for user status evaluation and to be passed to the 'after' method.
     * @throws JobExecutionException in case of generic failure
     */
    <A extends AnyTO> SyncDelta beforeAssign(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            A any) throws JobExecutionException;

    /**
     * Action to be executed before unlinking resource from the pulled user / group and de-provisioning.
     * User/group is unlinked and de-provisioned from the pulled resource upon pull in case of the
     * matching rule {@link org.apache.syncope.common.lib.types.MatchingRule#UNASSIGN} is applied.
     *
     * @param <A> concrete any object
     * @param profile profile of the pull being executed.
     * @param delta retrieved pull information
     * @param any any object
     * @return pull information used for user status evaluation and to be passed to the 'after' method.
     * @throws JobExecutionException in case of generic failure
     */
    <A extends AnyTO> SyncDelta beforeUnassign(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            A any) throws JobExecutionException;

    /**
     * Action to be executed before de-provisioning action only.
     * User/group is de-provisioned (without unlinking) from the pulled resource upon pull in case of
     * the matching rule {@link org.apache.syncope.common.lib.types.MatchingRule#DEPROVISION} is applied.
     *
     * @param <A> concrete any object
     * @param profile profile of the pull being executed.
     * @param delta retrieved pull information
     * @param any any object
     * @return pull information used for user status evaluation and to be passed to the 'after' method.
     * @throws JobExecutionException in case of generic failure
     */
    <A extends AnyTO> SyncDelta beforeDeprovision(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            A any) throws JobExecutionException;

    /**
     * Action to be executed before unlinking resource from the pulled user / group.
     * User/group is unlinked (without de-provisioning) from the pulled resource upon pull in case of
     * the matching rule {@link org.apache.syncope.common.lib.types.MatchingRule#UNLINK} is applied.
     *
     * @param <A> concrete any object
     * @param profile profile of the pull being executed.
     * @param delta retrieved pull information
     * @param any any object
     * @return pull information used for user status evaluation and to be passed to the 'after' method.
     * @throws JobExecutionException in case of generic failure
     */
    <A extends AnyTO> SyncDelta beforeUnlink(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            A any) throws JobExecutionException;

    /**
     * Action to be executed before linking resource to the pulled user / group.
     * User/group is linked (without updating) to the pulled resource upon pull in case of
     * the matching rule {@link org.apache.syncope.common.lib.types.MatchingRule#LINK} is applied.
     *
     * @param <A> concrete any object
     * @param profile profile of the pull being executed.
     * @param delta retrieved pull information
     * @param any any object
     * @return pull information used for user status evaluation and to be passed to the 'after' method.
     * @throws JobExecutionException in case of generic failure
     */
    <A extends AnyTO> SyncDelta beforeLink(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            A any) throws JobExecutionException;

    /**
     * Action to be executed before to update a pulled user / group locally.
     * User/group is updated upon pull in case of the matching rule
     * {@link org.apache.syncope.common.lib.types.MatchingRule#UPDATE} (default matching rule) is applied.
     *
     * @param <M> concrete any object
     * @param <P> any object modifications
     * @param profile profile of the pull being executed.
     * @param delta retrieved pull information
     * @param any any object
     * @param anyPatch modification
     * @return pull information used for logging and to be passed to the 'after' method.
     * @throws JobExecutionException in case of generic failure.
     */
    <M extends AnyTO, P extends AnyPatch> SyncDelta beforeUpdate(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            M any,
            P anyPatch)
            throws JobExecutionException;

    /**
     * Action to be executed before to delete a pulled user / group locally.
     *
     * @param <A> concrete any object
     * @param profile profile of the pull being executed.
     * @param delta retrieved pull information
     * @param any any object to be deleted
     * @return pull information used for logging and to be passed to the 'after' method.
     * @throws JobExecutionException in case of generic failure
     */
    <A extends AnyTO> SyncDelta beforeDelete(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            A any) throws JobExecutionException;

    /**
     * Action to be executed after each local user / group pull.
     *
     * @param <A> concrete any object
     * @param profile profile of the pull being executed.
     * @param delta retrieved pull information (may be modified by beforeProvision / beforeUpdate /
     * beforeDelete)
     * @param any any object
     * @param result global pull results at the current pull step
     * @throws JobExecutionException in case of generic failure
     */
    <A extends AnyTO> void after(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            A any,
            ProvisioningReport result) throws JobExecutionException;

    /**
     * Action to be executed in case an exception is thrown during pull.
     *
     * @param profile profile of the pull being executed.
     * @param delta retrieved pull information (may be modified by beforeProvision / beforeUpdate /
     * beforeDelete)
     * @param e the exception thrown
     * @return an instance of the given exception type is that is to be thrown; {@code NULL} otherwise
     * @throws JobExecutionException in case of generic failure
     */
    IgnoreProvisionException onError(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            Exception e) throws JobExecutionException;
}

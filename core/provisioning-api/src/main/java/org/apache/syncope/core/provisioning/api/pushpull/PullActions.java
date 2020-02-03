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

import java.util.Set;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.core.persistence.api.entity.resource.OrgUnit;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.quartz.JobExecutionException;

/**
 * Interface for actions to be performed during pull.
 * All methods can throw {@link IgnoreProvisionException} to make the current any object ignored by the pull
 * process.
 */
public interface PullActions extends ProvisioningActions {

    /**
     * Return additional attributes to include in the result from the underlying connector.
     *
     * @param profile profile of the pull being executed.
     * @param orgUnit Realm provisioning information
     * @return additional attributes to include in the result from the underlying connector
     */
    default Set<String> moreAttrsToGet(ProvisioningProfile<?, ?> profile, OrgUnit orgUnit) {
        return Set.of();
    }

    /**
     * Return additional attributes to include in the result from the underlying connector.
     *
     * @param profile profile of the pull being executed.
     * @param provision Any provisioning information
     * @return additional attributes to include in the result from the underlying connector
     */
    default Set<String> moreAttrsToGet(ProvisioningProfile<?, ?> profile, Provision provision) {
        return Set.of();
    }

    /**
     * Pre-process the pull information received by the underlying connector, before any internal activity occurs.
     *
     * @param profile profile of the pull being executed.
     * @param delta retrieved pull information
     * @return pull information, possibly altered.
     */
    default SyncDelta preprocess(ProvisioningProfile<?, ?> profile, SyncDelta delta) {
        return delta;
    }

    /**
     * Action to be executed before to create a pulled entity locally.
     * The entity is created locally upon pull in case of the un-matching rule
     * {@link org.apache.syncope.common.lib.types.UnmatchingRule#PROVISION} (default un-matching rule) is applied.
     *
     * @param profile profile of the pull being executed.
     * @param delta retrieved pull information
     * @param createReq create request
     * @throws JobExecutionException in case of generic failure
     */
    default void beforeProvision(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            AnyCR createReq) throws JobExecutionException {
    }

    /**
     * Action to be executed before locally creating a linked account.
     *
     * @param profile profile of the pull being executed.
     * @param delta retrieved pull information
     * @param linkedAccount create request
     * @throws JobExecutionException in case of generic failure
     */
    default void beforeProvision(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            LinkedAccountTO linkedAccount) throws JobExecutionException {
    }

    /**
     * Action to be executed before to create a pulled realm locally.
     * The realm is created locally upon pull in case of the un-matching rule
     * {@link org.apache.syncope.common.lib.types.UnmatchingRule#PROVISION} (default un-matching rule) is applied.
     *
     * @param profile profile of the pull being executed.
     * @param delta retrieved pull information
     * @param realm realm
     * @throws JobExecutionException in case of generic failure
     */
    default void beforeProvision(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            RealmTO realm) throws JobExecutionException {
    }

    /**
     * Action to be executed before creating (and linking to the resource) a pulled entity locally.
     * The entity is created locally and linked to the pulled resource upon pull in case of the
     * un-matching rule {@link org.apache.syncope.common.lib.types.UnmatchingRule#ASSIGN} is applied.
     *
     * @param profile profile of the pull being executed.
     * @param delta retrieved pull information
     * @param createReq create request
     * @throws JobExecutionException in case of generic failure
     */
    default void beforeAssign(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            AnyCR createReq) throws JobExecutionException {
    }

    /**
     * Action to be executed before locally creating a linked account.
     *
     * @param profile profile of the pull being executed.
     * @param delta retrieved pull information
     * @param linkedAccount linked account
     * @throws JobExecutionException in case of generic failure
     */
    default void beforeAssign(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            LinkedAccountTO linkedAccount) throws JobExecutionException {
    }

    /**
     * Action to be executed before creating (and linking to the resource) a pulled realm locally.
     * The realm is created locally and linked to the pulled resource upon pull in case of the
     * un-matching rule {@link org.apache.syncope.common.lib.types.UnmatchingRule#ASSIGN} is applied.
     *
     * @param profile profile of the pull being executed.
     * @param delta retrieved pull information
     * @param realm realm
     * @throws JobExecutionException in case of generic failure
     */
    default void beforeAssign(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            RealmTO realm) throws JobExecutionException {
    }

    /**
     * Action to be executed before unlinking resource from the pulled entity and de-provisioning.
     * The entity is unlinked and de-provisioned from the pulled resource upon pull in case of the
     * matching rule {@link org.apache.syncope.common.lib.types.MatchingRule#UNASSIGN} is applied.
     *
     * @param profile profile of the pull being executed.
     * @param delta retrieved pull information
     * @param entity entity
     * @throws JobExecutionException in case of generic failure
     */
    default void beforeUnassign(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            EntityTO entity) throws JobExecutionException {
    }

    /**
     * Action to be executed before de-provisioning action only.
     * The entity is de-provisioned (without unlinking) from the pulled resource upon pull in case of
     * the matching rule {@link org.apache.syncope.common.lib.types.MatchingRule#DEPROVISION} is applied.
     *
     * @param profile profile of the pull being executed.
     * @param delta retrieved pull information
     * @param entity entity
     * @throws JobExecutionException in case of generic failure
     */
    default void beforeDeprovision(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            EntityTO entity) throws JobExecutionException {
    }

    /**
     * Action to be executed before unlinking resource from the pulled entity.
     * The entity is unlinked (without de-provisioning) from the pulled resource upon pull in case of
     * the matching rule {@link org.apache.syncope.common.lib.types.MatchingRule#UNLINK} is applied.
     *
     * @param profile profile of the pull being executed.
     * @param delta retrieved pull information
     * @param entity entity
     * @throws JobExecutionException in case of generic failure
     */
    default void beforeUnlink(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            EntityTO entity) throws JobExecutionException {
    }

    /**
     * Action to be executed before linking resource to the pulled entity.
     * The entity is linked (without updating) to the pulled resource upon pull in case of
     * the matching rule {@link org.apache.syncope.common.lib.types.MatchingRule#LINK} is applied.
     *
     * @param profile profile of the pull being executed.
     * @param delta retrieved pull information
     * @param entity entity
     * @throws JobExecutionException in case of generic failure
     */
    default void beforeLink(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            EntityTO entity) throws JobExecutionException {
    }

    /**
     * Action to be executed before to update a pulled entity locally.
     * The entity is updated upon pull in case of the matching rule
     * {@link org.apache.syncope.common.lib.types.MatchingRule#UPDATE} (default matching rule) is applied.
     *
     * @param profile profile of the pull being executed.
     * @param delta retrieved pull information
     * @param entity entity
     * @param anyUR modification
     * @throws JobExecutionException in case of generic failure.
     */
    default void beforeUpdate(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            EntityTO entity,
            AnyUR anyUR) throws JobExecutionException {
    }

    /**
     * Action to be executed before to delete a pulled entity locally.
     *
     * @param profile profile of the pull being executed.
     * @param delta retrieved pull information
     * @param entity entity
     * @throws JobExecutionException in case of generic failure
     */
    default void beforeDelete(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            EntityTO entity) throws JobExecutionException {
    }

    /**
     * Action to be executed after each local entity pull.
     *
     * @param profile profile of the pull being executed.
     * @param delta retrieved pull information (may be modified by beforeProvision / beforeUpdate /
     * beforeDelete)
     * @param entity entity
     * @param result global pull results at the current pull step
     * @throws JobExecutionException in case of generic failure
     */
    default void after(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            EntityTO entity,
            ProvisioningReport result) throws JobExecutionException {

        // do nothing
    }

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
    default IgnoreProvisionException onError(
            ProvisioningProfile<?, ?> profile,
            SyncDelta delta,
            Exception e) throws JobExecutionException {

        return null;
    }
}

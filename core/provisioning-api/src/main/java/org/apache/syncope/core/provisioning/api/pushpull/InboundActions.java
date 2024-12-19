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
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.syncope.common.lib.to.OrgUnit;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.identityconnectors.framework.common.objects.LiveSyncDelta;

/**
 * Interface for actions to be performed during inbound.
 * All methods can throw {@link IgnoreProvisionException} to make the current any object ignored by the inbound
 * process.
 */
public interface InboundActions extends ProvisioningActions {

    /**
     * Return additional attributes to include in the result from the underlying connector.
     *
     * @param profile profile of the inbound being executed.
     * @param orgUnit Realm provisioning information
     * @return additional attributes to include in the result from the underlying connector
     */
    default Set<String> moreAttrsToGet(ProvisioningProfile<?, ?> profile, OrgUnit orgUnit) {
        return Set.of();
    }

    /**
     * Return additional attributes to include in the result from the underlying connector.
     *
     * @param profile profile of the inbound being executed.
     * @param provision Any provisioning information
     * @return additional attributes to include in the result from the underlying connector
     */
    default Set<String> moreAttrsToGet(ProvisioningProfile<?, ?> profile, Provision provision) {
        return Set.of();
    }

    /**
     * Pre-process the inbound information received by the underlying connector, before any internal activity occurs.
     *
     * @param <T> sync delta class
     * @param profile profile of the inbound being executed.
     * @param delta retrieved inbound information
     * @return inbound information, possibly altered.
     */
    default <T extends LiveSyncDelta> T preprocess(ProvisioningProfile<?, ?> profile, T delta) {
        return delta;
    }

    /**
     * Action to be executed before to create a inbounded entity locally.
     * The entity is created locally upon inbound in case of the un-matching rule
     * {@link org.apache.syncope.common.lib.types.UnmatchingRule#PROVISION} (default un-matching rule) is applied.
     *
     * @param profile profile of the inbound being executed.
     * @param delta retrieved inbound information
     * @param createReq create request
     */
    default void beforeProvision(
            ProvisioningProfile<?, ?> profile,
            LiveSyncDelta delta,
            AnyCR createReq) {
    }

    /**
     * Action to be executed before locally creating a linked account.
     *
     * @param profile profile of the inbound being executed.
     * @param delta retrieved inbound information
     * @param linkedAccount create request
     */
    default void beforeProvision(
            ProvisioningProfile<?, ?> profile,
            LiveSyncDelta delta,
            LinkedAccountTO linkedAccount) {
    }

    /**
     * Action to be executed before to create a inbounded realm locally.
     * The realm is created locally upon inbound in case of the un-matching rule
     * {@link org.apache.syncope.common.lib.types.UnmatchingRule#PROVISION} (default un-matching rule) is applied.
     *
     * @param profile profile of the inbound being executed.
     * @param delta retrieved inbound information
     * @param realm realm
     */
    default void beforeProvision(
            ProvisioningProfile<?, ?> profile,
            LiveSyncDelta delta,
            RealmTO realm) {
    }

    /**
     * Action to be executed before creating (and linking to the resource) a inbounded entity locally.
     * The entity is created locally and linked to the inbounded resource upon inbound in case of the
     * un-matching rule {@link org.apache.syncope.common.lib.types.UnmatchingRule#ASSIGN} is applied.
     *
     * @param profile profile of the inbound being executed.
     * @param delta retrieved inbound information
     * @param createReq create request
     */
    default void beforeAssign(
            ProvisioningProfile<?, ?> profile,
            LiveSyncDelta delta,
            AnyCR createReq) {
    }

    /**
     * Action to be executed before locally creating a linked account.
     *
     * @param profile profile of the inbound being executed.
     * @param delta retrieved inbound information
     * @param linkedAccount linked account
     */
    default void beforeAssign(
            ProvisioningProfile<?, ?> profile,
            LiveSyncDelta delta,
            LinkedAccountTO linkedAccount) {
    }

    /**
     * Action to be executed before creating (and linking to the resource) a inbounded realm locally.
     * The realm is created locally and linked to the inbounded resource upon inbound in case of the
     * un-matching rule {@link org.apache.syncope.common.lib.types.UnmatchingRule#ASSIGN} is applied.
     *
     * @param profile profile of the inbound being executed.
     * @param delta retrieved inbound information
     * @param realm realm
     */
    default void beforeAssign(
            ProvisioningProfile<?, ?> profile,
            LiveSyncDelta delta,
            RealmTO realm) {
    }

    /**
     * Action to be executed before unlinking resource from the inbounded entity and de-provisioning.
     * The entity is unlinked and de-provisioned from the inbounded resource upon inbound in case of the
     * matching rule {@link org.apache.syncope.common.lib.types.MatchingRule#UNASSIGN} is applied.
     *
     * @param profile profile of the inbound being executed.
     * @param delta retrieved inbound information
     * @param entity entity
     */
    default void beforeUnassign(
            ProvisioningProfile<?, ?> profile,
            LiveSyncDelta delta,
            EntityTO entity) {
    }

    /**
     * Action to be executed before de-provisioning action only.
     * The entity is de-provisioned (without unlinking) from the inbounded resource upon inbound in case of
     * the matching rule {@link org.apache.syncope.common.lib.types.MatchingRule#DEPROVISION} is applied.
     *
     * @param profile profile of the inbound being executed.
     * @param delta retrieved inbound information
     * @param entity entity
     */
    default void beforeDeprovision(
            ProvisioningProfile<?, ?> profile,
            LiveSyncDelta delta,
            EntityTO entity) {
    }

    /**
     * Action to be executed before unlinking resource from the inbounded entity.
     * The entity is unlinked (without de-provisioning) from the inbounded resource upon inbound in case of
     * the matching rule {@link org.apache.syncope.common.lib.types.MatchingRule#UNLINK} is applied.
     *
     * @param profile profile of the inbound being executed.
     * @param delta retrieved inbound information
     * @param entity entity
     */
    default void beforeUnlink(
            ProvisioningProfile<?, ?> profile,
            LiveSyncDelta delta,
            EntityTO entity) {
    }

    /**
     * Action to be executed before linking resource to the inbounded entity.
     * The entity is linked (without updating) to the inbounded resource upon inbound in case of
     * the matching rule {@link org.apache.syncope.common.lib.types.MatchingRule#LINK} is applied.
     *
     * @param profile profile of the inbound being executed.
     * @param delta retrieved inbound information
     * @param entity entity
     */
    default void beforeLink(
            ProvisioningProfile<?, ?> profile,
            LiveSyncDelta delta,
            EntityTO entity) {
    }

    /**
     * Action to be executed before to update a inbounded entity locally.
     * The entity is updated upon inbound in case of the matching rule
     * {@link org.apache.syncope.common.lib.types.MatchingRule#UPDATE} (default matching rule) is applied.
     *
     * @param profile profile of the inbound being executed.
     * @param delta retrieved inbound information
     * @param entity entity
     * @param anyUR modification
     * @throws JobExecutionException in case of generic failure.
     */
    default void beforeUpdate(
            ProvisioningProfile<?, ?> profile,
            LiveSyncDelta delta,
            EntityTO entity,
            AnyUR anyUR) throws JobExecutionException {
    }

    /**
     * Action to be executed before to delete a inbounded entity locally.
     *
     * @param profile profile of the inbound being executed.
     * @param delta retrieved inbound information
     * @param entity entity
     */
    default void beforeDelete(
            ProvisioningProfile<?, ?> profile,
            LiveSyncDelta delta,
            EntityTO entity) {
    }

    /**
     * Action to be executed after each local entity inbound.
     *
     * @param profile profile of the inbound being executed.
     * @param delta retrieved inbound information (may be modified by beforeProvisionTO / beforeUpdate /
     * beforeDelete)
     * @param entity entity
     * @param result global inbound results at the current inbound step
     * @throws JobExecutionException in case of generic failure
     */
    default void after(
            ProvisioningProfile<?, ?> profile,
            LiveSyncDelta delta,
            EntityTO entity,
            ProvisioningReport result) throws JobExecutionException {

        // do nothing
    }

    /**
     * Action to be executed in case an exception is thrown during inbound.
     *
     * @param profile profile of the inbound being executed.
     * @param delta retrieved inbound information (may be modified by beforeProvisionTO / beforeUpdate /
     * beforeDelete)
     * @param e the exception thrown
     * @return an instance of the given exception type is that is to be thrown; {@code NULL} otherwise
     */
    default IgnoreProvisionException onError(
            ProvisioningProfile<?, ?> profile,
            LiveSyncDelta delta,
            Exception e) {

        return null;
    }
}

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

import java.util.Collections;
import java.util.Set;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.core.persistence.api.entity.Entity;

/**
 * Interface for actions to be performed during push.
 * All methods can throw {@link IgnoreProvisionException} to make the current entity ignored by the push process.
 */
public interface PushActions extends ProvisioningActions {

    /**
     * Return additional attributes to include in the result from the underlying connector.
     *
     * @param profile profile of the pull being executed.
     * @param entity entity
     * @return additional attributes to include in the result from the underlying connector
     */
    default Set<String> moreAttrsToGet(ProvisioningProfile<?, ?> profile, Entity entity) {
        return Collections.emptySet();
    }

    /**
     * Action to be executed before to assign (link &amp; provision) a pushed entity to the resource.
     *
     * @param profile profile of the push being executed.
     * @param entity entity to be created.
     * @return entity.
     */
    default Entity beforeAssign(
            ProvisioningProfile<?, ?> profile,
            Entity entity) {

        return entity;
    }

    /**
     * Action to be executed before to provision a pushed entity to the resource.
     *
     * @param profile profile of the push being executed.
     * @param entity entity to be created.
     * @return entity.
     */
    default Entity beforeProvision(
            ProvisioningProfile<?, ?> profile,
            Entity entity) {

        return entity;
    }

    /**
     * Action to be executed before to update a pushed entity on the resource.
     *
     * @param profile profile of the push being executed.
     * @param entity entity to be updated.
     * @return entity.
     */
    default Entity beforeUpdate(
            ProvisioningProfile<?, ?> profile,
            Entity entity) {

        return entity;
    }

    /**
     * Action to be executed before to link a pushed entity to the resource.
     *
     * @param profile profile of the push being executed.
     * @param entity entity to be created.
     * @return entity.
     */
    default Entity beforeLink(
            ProvisioningProfile<?, ?> profile,
            Entity entity) {

        return entity;
    }

    /**
     * Action to be executed before to unlink a pushed entity from the resource.
     *
     * @param profile profile of the push being executed.
     * @param entity entity to be created.
     * @return entity.
     */
    default Entity beforeUnlink(
            ProvisioningProfile<?, ?> profile,
            Entity entity) {

        return entity;
    }

    /**
     * Action to be executed before to unassign a pushed entity from the resource.
     *
     * @param profile profile of the push being executed.
     * @param entity entity to be created.
     * @return entity.
     */
    default Entity beforeUnassign(
            ProvisioningProfile<?, ?> profile,
            Entity entity) {

        return entity;
    }

    /**
     * Action to be executed before to unassign a pushed entity from the resource.
     *
     * @param profile profile of the push being executed.
     * @param entity entity to be created.
     * @return entity.
     */
    default Entity beforeDeprovision(
            ProvisioningProfile<?, ?> profile,
            Entity entity) {

        return entity;
    }

    /**
     * Action to be executed before delete a pushed entity locally and from the resource.
     *
     * @param profile profile of the push being executed.
     * @param entity entity to be created.
     * @return entity.
     */
    default Entity beforeDelete(
            ProvisioningProfile<?, ?> profile,
            Entity entity) {

        return entity;
    }

    /**
     * Action to be executed after entity push goes on error.
     *
     * @param profile profile of the push being executed.
     * @param entity pushed entity.
     * @param result operation result.
     * @param error error being reported
     */
    default void onError(
            ProvisioningProfile<?, ?> profile,
            Entity entity,
            ProvisioningReport result,
            Exception error) {

        // do nothing
    }

    /**
     * Action to be executed after each local entity push.
     *
     * @param profile profile of the push being executed.
     * @param entity pushed entity.
     * @param result operation result.
     */
    default void after(
            ProvisioningProfile<?, ?> profile,
            Entity entity,
            ProvisioningReport result) {

        // do nothing
    }
}

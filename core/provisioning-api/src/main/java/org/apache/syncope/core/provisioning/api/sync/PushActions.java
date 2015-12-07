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

import org.apache.syncope.core.persistence.api.entity.Any;
import org.quartz.JobExecutionException;

/**
 * Interface for actions to be performed during push.
 * All methods can throw {@link IgnoreProvisionException} to make the current any ignored by the push process.
 */
public interface PushActions extends ProvisioningActions {

    /**
     * Action to be executed before to assign (link &amp; provision) a synchronized any object to the resource.
     *
     * @param <A> concrete any object
     * @param profile profile of the push being executed.
     * @param any any object to be created.
     * @return any.
     * @throws JobExecutionException in case of generic failure
     */
    <A extends Any<?>> A beforeAssign(
            ProvisioningProfile<?, ?> profile,
            A any) throws JobExecutionException;

    /**
     * Action to be executed before to provision a synchronized any object to the resource.
     *
     * @param <A> concrete any object
     * @param profile profile of the push being executed.
     * @param any any object to be created.
     * @return any.
     * @throws JobExecutionException in case of generic failure
     */
    <A extends Any<?>> A beforeProvision(
            ProvisioningProfile<?, ?> profile,
            A any) throws JobExecutionException;

    /**
     * Action to be executed before to update a synchronized any object on the resource.
     *
     * @param <A> concrete any object
     * @param profile profile of the push being executed.
     * @param any any object to be updated.
     * @return any.
     * @throws JobExecutionException in case of generic failure
     */
    <A extends Any<?>> A beforeUpdate(
            ProvisioningProfile<?, ?> profile,
            A any) throws JobExecutionException;

    /**
     * Action to be executed before to link a synchronized any object to the resource.
     *
     * @param <A> concrete any object
     * @param profile profile of the push being executed.
     * @param any any object to be created.
     * @return any.
     * @throws JobExecutionException in case of generic failure
     */
    <A extends Any<?>> A beforeLink(
            ProvisioningProfile<?, ?> profile,
            A any) throws JobExecutionException;

    /**
     * Action to be executed before to unlink a synchronized any object from the resource.
     *
     * @param <A> concrete any object
     * @param profile profile of the push being executed.
     * @param any any object to be created.
     * @return any.
     * @throws JobExecutionException in case of generic failure
     */
    <A extends Any<?>> A beforeUnlink(
            ProvisioningProfile<?, ?> profile,
            A any) throws JobExecutionException;

    /**
     * Action to be executed before to unassign a synchronized any object from the resource.
     *
     * @param <A> concrete any object
     * @param profile profile of the push being executed.
     * @param any any object to be created.
     * @return any.
     * @throws JobExecutionException in case of generic failure
     */
    <A extends Any<?>> A beforeUnassign(
            ProvisioningProfile<?, ?> profile,
            A any) throws JobExecutionException;

    /**
     * Action to be executed before to unassign a synchronized any object from the resource.
     *
     * @param <A> concrete any object
     * @param profile profile of the push being executed.
     * @param any any object to be created.
     * @return any.
     * @throws JobExecutionException in case of generic failure
     */
    <A extends Any<?>> A beforeDeprovision(
            ProvisioningProfile<?, ?> profile,
            A any) throws JobExecutionException;

    /**
     * Action to be executed before delete a synchronized any object locally and from the resource.
     *
     * @param <A> concrete any object
     * @param profile profile of the push being executed.
     * @param any any object to be created.
     * @return any.
     * @throws JobExecutionException in case of generic failure
     */
    <A extends Any<?>> A beforeDelete(
            ProvisioningProfile<?, ?> profile,
            A any) throws JobExecutionException;

    /**
     * Action to be executed after any object push goes on error.
     *
     * @param <A> concrete any object
     * @param profile profile of the push being executed.
     * @param any synchronized any object.
     * @param result operation result.
     * @param error error being reported
     * @throws JobExecutionException in case of generic failure
     */
    <A extends Any<?>> void onError(
            ProvisioningProfile<?, ?> profile,
            A any,
            ProvisioningReport result,
            Exception error) throws JobExecutionException;

    /**
     * Action to be executed after each local any object push.
     *
     * @param <A> concrete any object
     * @param profile profile of the push being executed.
     * @param any synchronized any object.
     * @param result operation result.
     * @throws JobExecutionException in case of generic failure
     */
    <A extends Any<?>> void after(
            ProvisioningProfile<?, ?> profile,
            A any,
            ProvisioningReport result) throws JobExecutionException;
}

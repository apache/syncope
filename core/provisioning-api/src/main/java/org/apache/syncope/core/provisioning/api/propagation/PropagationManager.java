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
package org.apache.syncope.core.provisioning.api.propagation;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.Item;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.provisioning.api.UserWorkflowResult;
import org.identityconnectors.framework.common.objects.Attribute;

@SuppressWarnings("squid:S00107")
public interface PropagationManager {

    /**
     * Create the any object tasks for every associated resource, unless in {@code noPropResourceKeys}.
     *
     * @param kind any object type kind
     * @param key any object key
     * @param enable whether any object should be enabled or not
     * @param propByRes operation to be performed per resource
     * @param vAttrs virtual attributes to be set
     * @param noPropResourceKeys external resources performing not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTaskInfo> getCreateTasks(
            AnyTypeKind kind,
            String key,
            Boolean enable,
            PropagationByResource<String> propByRes,
            Collection<Attr> vAttrs,
            Collection<String> noPropResourceKeys);

    /**
     * Create the user tasks for every associated resource, unless in {@code noPropResourceKeys}.
     *
     * @param key user key
     * @param password to be set
     * @param enable whether user must be enabled or not
     * @param propByRes operation to be performed per resource
     * @param propByLinkedAccount operation to be performed for linked accounts
     * @param vAttrs virtual attributes to be set
     * @param noPropResourceKeys external resources not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTaskInfo> getUserCreateTasks(
            String key,
            String password,
            Boolean enable,
            PropagationByResource<String> propByRes,
            PropagationByResource<Pair<String, String>> propByLinkedAccount,
            Collection<Attr> vAttrs,
            Collection<String> noPropResourceKeys);

    /**
     * Create the update tasks for the any object on each resource associated, unless in {@code noPropResourceKeys}.
     *
     * @param kind any object type kind
     * @param key any object key
     * @param changePwd whether password should be included for propagation attributes or not
     * @param enable whether any object should be enabled or not, may be null to leave unchanged
     * @param propByRes operation to be performed per resource
     * @param propByLinkedAccount operation to be performed for linked accounts
     * @param vAttrs virtual attributes to be set
     * @param noPropResourceKeys external resource keys not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTaskInfo> getUpdateTasks(
            AnyTypeKind kind,
            String key,
            boolean changePwd,
            Boolean enable,
            PropagationByResource<String> propByRes,
            PropagationByResource<Pair<String, String>> propByLinkedAccount,
            Collection<Attr> vAttrs,
            Collection<String> noPropResourceKeys);

    /**
     * Create the update tasks for the user on each resource associated, unless in {@code noPropResourceKeys}.
     *
     * @param wfResult user to be propagated (and info associated), as per result from workflow
     * @param changePwd whether password should be included for propagation attributes or not
     * @param noPropResourceKeys external resources not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTaskInfo> getUserUpdateTasks(
            UserWorkflowResult<Pair<UserUR, Boolean>> wfResult,
            boolean changePwd,
            Collection<String> noPropResourceKeys);

    /**
     * Create the update tasks for the user on each resource associated; propagate password update only to requested
     * resources.
     *
     * @param wfResult user to be propagated (and info associated), as per result from workflow
     * @return list of propagation tasks
     */
    List<PropagationTaskInfo> getUserUpdateTasks(UserWorkflowResult<Pair<UserUR, Boolean>> wfResult);

    /**
     * Create the delete tasks for the any object from each resource associated, unless in {@code noPropResourceKeys}.
     *
     * @param kind any object type kind
     * @param key any object key
     * @param propByRes operation to be performed per resource
     * @param propByLinkedAccount operation to be performed for linked accounts
     * @param noPropResourceKeys external resource keys not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTaskInfo> getDeleteTasks(
            AnyTypeKind kind,
            String key,
            PropagationByResource<String> propByRes,
            PropagationByResource<Pair<String, String>> propByLinkedAccount,
            Collection<String> noPropResourceKeys);

    PropagationTaskInfo newTask(
            DerAttrHandler derAttrHandler,
            Any<?> any,
            ExternalResource resource,
            ResourceOperation operation,
            Provision provision,
            Stream<? extends Item> mappingItems,
            Pair<String, Set<Attribute>> preparedAttrs);

    /**
     * Create the needed tasks for the realm for each resource associated, unless in {@code noPropResourceKeys}.
     *
     * @param realm realm
     * @param propByRes operation to be performed per resource
     * @param noPropResourceKeys external resource keys not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTaskInfo> createTasks(
            Realm realm,
            PropagationByResource<String> propByRes,
            Collection<String> noPropResourceKeys);
}

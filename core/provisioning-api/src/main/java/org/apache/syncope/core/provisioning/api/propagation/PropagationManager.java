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
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.UserWorkflowResult;
import org.identityconnectors.framework.common.objects.Attribute;

@SuppressWarnings("squid:S00107")
public interface PropagationManager {

    /**
     * Name for special propagation attribute used to indicate whether there are attributes, marked as mandatory in the
     * mapping but not to be propagated.
     */
    String MANDATORY_MISSING_ATTR_NAME = "__MANDATORY_MISSING__";

    /**
     * Name for special propagation attribute used to indicate whether there are attributes, marked as mandatory in the
     * mapping but about to be propagated as null or empty.
     */
    String MANDATORY_NULL_OR_EMPTY_ATTR_NAME = "__MANDATORY_NULL_OR_EMPTY__";

    /**
     * Create the tasks for every associated resource, unless in {@code excludedResources}.
     *
     * @param kind any type kind
     * @param key any key
     * @param enable whether any should be enabled or not
     * @param propByRes operation to be performed per resource
     * @param vAttrs virtual attributes to be set
     * @param excludedResources external resources performing not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTaskInfo> getCreateTasks(
            AnyTypeKind kind,
            String key,
            Boolean enable,
            PropagationByResource<String> propByRes,
            Collection<Attr> vAttrs,
            Collection<String> excludedResources);

    /**
     * Create the user tasks for every associated resource, unless in {@code excludedResources}.
     *
     * @param key user key
     * @param password to be set
     * @param enable whether user must be enabled or not
     * @param propByRes operation to be performed per resource
     * @param propByLinkedAccount operation to be performed for linked accounts
     * @param vAttrs virtual attributes to be set
     * @param excludedResources external resources not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTaskInfo> getUserCreateTasks(
            String key,
            String password,
            Boolean enable,
            PropagationByResource<String> propByRes,
            PropagationByResource<Pair<String, String>> propByLinkedAccount,
            Collection<Attr> vAttrs,
            Collection<String> excludedResources);

    /**
     * Create the update tasks on each resource associated, unless in {@code excludedResources}.
     *
     * @param anyUR update request
     * @param kind any type kind
     * @param key any key
     * @param changePwdRes the resources in which the password must be included in the propagation attributes
     * @param enable whether any should be enabled or not, may be null to leave unchanged
     * @param propByRes operation to be performed per resource
     * @param propByLinkedAccount operation to be performed for linked accounts
     * @param vAttrs virtual attributes to be set
     * @param excludedResources external resource keys not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTaskInfo> getUpdateTasks(
            AnyUR anyUR,
            AnyTypeKind kind,
            String key,
            List<String> changePwdRes,
            Boolean enable,
            PropagationByResource<String> propByRes,
            PropagationByResource<Pair<String, String>> propByLinkedAccount,
            Collection<Attr> vAttrs,
            Collection<String> excludedResources);

    /**
     * Create the update tasks for the user on each resource associated, unless in {@code excludedResources}.
     *
     * @param wfResult user to be propagated (and info associated), as per result from workflow
     * @param changePwdRes the resources in which the password must be included in the propagation attributes
     * @param excludedResources external resources not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTaskInfo> getUserUpdateTasks(
            UserWorkflowResult<Pair<UserUR, Boolean>> wfResult,
            List<String> changePwdRes,
            Collection<String> excludedResources);

    /**
     * Create the update tasks for the user on each resource associated; propagate password update only to requested
     * resources.
     *
     * @param wfResult user to be propagated (and info associated), as per result from workflow
     * @return list of propagation tasks
     */
    List<PropagationTaskInfo> getUserUpdateTasks(UserWorkflowResult<Pair<UserUR, Boolean>> wfResult);

    /**
     * Create the delete tasks from each resource associated, unless in {@code excludedResources}.
     *
     * @param kind any type kind
     * @param key any key
     * @param propByRes operation to be performed per resource
     * @param propByLinkedAccount operation to be performed for linked accounts
     * @param excludedResources external resource keys not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTaskInfo> getDeleteTasks(
            AnyTypeKind kind,
            String key,
            PropagationByResource<String> propByRes,
            PropagationByResource<Pair<String, String>> propByLinkedAccount,
            Collection<String> excludedResources);

    PropagationTaskInfo newTask(
            DerAttrHandler derAttrHandler,
            Any any,
            ExternalResource resource,
            ResourceOperation operation,
            Provision provision,
            Stream<Item> mappingItems,
            Pair<String, Set<Attribute>> preparedAttrs);

    /**
     * Create the needed tasks for the realm for each resource associated, unless in {@code excludedResources}.
     *
     * @param realm realm
     * @param propByRes operation to be performed per resource
     * @param excludedResources external resource keys not to be considered for propagation
     * @return list of propagation tasks
     */
    List<PropagationTaskInfo> createTasks(
            Realm realm,
            PropagationByResource<String> propByRes,
            Collection<String> excludedResources);

    /**
     * Prepare attributes for propagation.
     *
     * @param kind any type kind
     * @param key any key
     * @param password to be set (for users)
     * @param changePwdRes the resources in which the password must be included in the propagation attributes (for 
     * users)
     * @param enable whether any should be enabled or not, may be null to leave unchanged
     * @param excludedResources external resource keys not to be considered for propagation
     * @return map with prepared attributes per External Resource
     */
    Map<Pair<String, String>, Set<Attribute>> prepareAttrs(
            AnyTypeKind kind,
            String key,
            String password,
            List<String> changePwdRes,
            Boolean enable,
            Collection<String> excludedResources);

    /**
     * Prepare attributes for propagation.
     *
     * @param realm realm
     * @return map with prepared attributes per External Resource
     */
    Map<Pair<String, String>, Set<Attribute>> prepareAttrs(Realm realm);

    /**
     * Enrich the provided tasks with attribute deltas.
     *
     * @param tasks propagation tasks
     * @param beforeAttrs attribute values before update
     * @return enriched propagation tasks
     */
    List<PropagationTaskInfo> setAttributeDeltas(
            List<PropagationTaskInfo> tasks,
            Map<Pair<String, String>, Set<Attribute>> beforeAttrs);
}

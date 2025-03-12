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
package org.apache.syncope.core.provisioning.java;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.provisioning.api.GroupProvisioningManager;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.workflow.api.GroupWorkflowAdapter;
import org.identityconnectors.framework.common.objects.Attribute;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class DefaultGroupProvisioningManager implements GroupProvisioningManager {

    protected final GroupWorkflowAdapter gwfAdapter;

    protected final PropagationManager propagationManager;

    protected final PropagationTaskExecutor taskExecutor;

    protected final GroupDataBinder groupDataBinder;

    protected final GroupDAO groupDAO;

    protected final VirAttrHandler virtAttrHandler;

    public DefaultGroupProvisioningManager(
            final GroupWorkflowAdapter gwfAdapter,
            final PropagationManager propagationManager,
            final PropagationTaskExecutor taskExecutor,
            final GroupDataBinder groupDataBinder,
            final GroupDAO groupDAO,
            final VirAttrHandler virtAttrHandler) {

        this.gwfAdapter = gwfAdapter;
        this.propagationManager = propagationManager;
        this.taskExecutor = taskExecutor;
        this.groupDataBinder = groupDataBinder;
        this.groupDAO = groupDAO;
        this.virtAttrHandler = virtAttrHandler;
    }

    @Override
    public Pair<String, List<PropagationStatus>> create(
            final GroupCR groupCR, final boolean nullPriorityAsync, final String creator, final String context) {

        WorkflowResult<String> created = gwfAdapter.create(groupCR, creator, context);

        List<PropagationTaskInfo> tasks = propagationManager.getCreateTasks(
                AnyTypeKind.GROUP,
                created.getResult(),
                null,
                created.getPropByRes(),
                groupCR.getVirAttrs(),
                Set.of());
        PropagationReporter propagationReporter = taskExecutor.execute(tasks, nullPriorityAsync, creator);

        return Pair.of(created.getResult(), propagationReporter.getStatuses());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public Pair<String, List<PropagationStatus>> create(
            final GroupCR groupCR,
            final Map<String, String> groupOwnerMap,
            final Set<String> excludedResources,
            final boolean nullPriorityAsync,
            final String creator,
            final String context) {

        WorkflowResult<String> created = gwfAdapter.create(groupCR, creator, context);

        // see ConnObjectUtils#getAnyTOFromConnObject for GroupOwnerSchema
        groupCR.getPlainAttr(StringUtils.EMPTY).
                ifPresent(groupOwner -> groupOwnerMap.put(created.getResult(), groupOwner.getValues().getFirst()));

        List<PropagationTaskInfo> tasks = propagationManager.getCreateTasks(
                AnyTypeKind.GROUP,
                created.getResult(),
                null,
                created.getPropByRes(),
                groupCR.getVirAttrs(),
                excludedResources);
        PropagationReporter propagationReporter = taskExecutor.execute(tasks, nullPriorityAsync, creator);

        return Pair.of(created.getResult(), propagationReporter.getStatuses());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public Pair<GroupUR, List<PropagationStatus>> update(
            final GroupUR groupUR,
            final Set<String> excludedResources,
            final boolean nullPriorityAsync,
            final String updater,
            final String context) {

        Map<Pair<String, String>, Set<Attribute>> beforeAttrs = propagationManager.prepareAttrs(
                AnyTypeKind.GROUP,
                groupUR.getKey(),
                null,
                List.of(),
                null,
                excludedResources);

        WorkflowResult<GroupUR> updated = gwfAdapter.update(groupUR, updater, context);

        List<PropagationTaskInfo> tasks = propagationManager.setAttributeDeltas(
                propagationManager.getUpdateTasks(
                        updated.getResult(),
                        AnyTypeKind.GROUP,
                        updated.getResult().getKey(),
                        List.of(),
                        null,
                        updated.getPropByRes(),
                        null,
                        groupUR.getVirAttrs(),
                        excludedResources),
                beforeAttrs);
        PropagationReporter propagationReporter = taskExecutor.execute(tasks, nullPriorityAsync, updater);

        return Pair.of(updated.getResult(), propagationReporter.getStatuses());
    }

    @Override
    public List<PropagationStatus> delete(
            final String key, final boolean nullPriorityAsync, final String eraser, final String context) {

        return delete(key, Set.of(), nullPriorityAsync, eraser, context);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public List<PropagationStatus> delete(
            final String key,
            final Set<String> excludedResources,
            final boolean nullPriorityAsync,
            final String eraser,
            final String context) {

        List<PropagationTaskInfo> taskInfos = new ArrayList<>();

        // Generate propagation tasks for deleting users and any objects from group resources, 
        // if they are on those resources only because of the reason being deleted (see SYNCOPE-357)
        groupDataBinder.findUsersWithTransitiveResources(key).forEach((anyKey, propByRes) -> {
            taskInfos.addAll(propagationManager.getDeleteTasks(
                    AnyTypeKind.USER,
                    anyKey,
                    propByRes,
                    null,
                    excludedResources));
        });
        groupDataBinder.findAnyObjectsWithTransitiveResources(key).forEach((anyKey, propByRes) -> {
            taskInfos.addAll(propagationManager.getDeleteTasks(
                    AnyTypeKind.ANY_OBJECT,
                    anyKey,
                    propByRes,
                    null,
                    excludedResources));
        });

        // Generate propagation tasks for deleting this group from resources
        taskInfos.addAll(propagationManager.getDeleteTasks(
                AnyTypeKind.GROUP,
                key,
                null,
                null,
                null));

        PropagationReporter propagationReporter = taskExecutor.execute(taskInfos, nullPriorityAsync, eraser);

        gwfAdapter.delete(key, eraser, context);

        return propagationReporter.getStatuses();
    }

    @Override
    public String link(final GroupUR groupUR, final String updater, final String context) {
        return gwfAdapter.update(groupUR, updater, context).getResult().getKey();
    }

    @Override
    public String unlink(final GroupUR groupUR, final String updater, final String context) {
        return gwfAdapter.update(groupUR, updater, context).getResult().getKey();
    }

    @Override
    public List<PropagationStatus> provision(
            final String key,
            final Collection<String> resources,
            final boolean nullPriorityAsync,
            final String executor) {

        PropagationByResource<String> propByRes = new PropagationByResource<>();
        propByRes.addAll(ResourceOperation.UPDATE, resources);

        List<PropagationTaskInfo> taskInfos = propagationManager.getUpdateTasks(
                null,
                AnyTypeKind.GROUP,
                key,
                List.of(),
                null,
                propByRes,
                null,
                null,
                null);
        PropagationReporter propagationReporter = taskExecutor.execute(taskInfos, nullPriorityAsync, executor);

        return propagationReporter.getStatuses();
    }

    @Override
    public List<PropagationStatus> deprovision(
            final String key,
            final Collection<String> resources,
            final boolean nullPriorityAsync,
            final String executor) {

        PropagationByResource<String> propByRes = new PropagationByResource<>();
        propByRes.addAll(ResourceOperation.DELETE, resources);

        List<PropagationTaskInfo> taskInfos = propagationManager.getDeleteTasks(
                AnyTypeKind.GROUP,
                key,
                propByRes,
                null,
                groupDAO.findAllResourceKeys(key).stream().
                        filter(resource -> !resources.contains(resource)).
                        toList());
        PropagationReporter propagationReporter = taskExecutor.execute(taskInfos, nullPriorityAsync, executor);

        return propagationReporter.getStatuses();
    }
}

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
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.patch.GroupPatch;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.provisioning.api.GroupProvisioningManager;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.apache.syncope.core.workflow.api.GroupWorkflowAdapter;

public class DefaultGroupProvisioningManager implements GroupProvisioningManager {

    @Autowired
    protected GroupWorkflowAdapter gwfAdapter;

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @Autowired
    protected GroupDAO groupDAO;

    @Autowired
    protected VirAttrHandler virtAttrHandler;

    @Override
    public Pair<String, List<PropagationStatus>> create(final GroupTO group, final boolean nullPriorityAsync) {
        return create(group, Collections.<String>emptySet(), nullPriorityAsync);
    }

    @Override
    public Pair<String, List<PropagationStatus>> create(
            final GroupTO groupTO, final Set<String> excludedResources, final boolean nullPriorityAsync) {

        WorkflowResult<String> created = gwfAdapter.create(groupTO);

        List<PropagationTask> tasks = propagationManager.getCreateTasks(
                AnyTypeKind.GROUP,
                created.getResult(),
                created.getPropByRes(),
                groupTO.getVirAttrs(),
                excludedResources);
        PropagationReporter propagationReporter =
                ApplicationContextProvider.getBeanFactory().getBean(PropagationReporter.class);
        taskExecutor.execute(tasks, propagationReporter, nullPriorityAsync);

        return new ImmutablePair<>(created.getResult(), propagationReporter.getStatuses());
    }

    @Override
    public Pair<String, List<PropagationStatus>> create(
            final GroupTO groupTO,
            final Map<String, String> groupOwnerMap,
            final Set<String> excludedResources,
            final boolean nullPriorityAsync) {

        WorkflowResult<String> created = gwfAdapter.create(groupTO);

        // see ConnObjectUtils#getAnyTOFromConnObject for GroupOwnerSchema
        AttrTO groupOwner = groupTO.getPlainAttrMap().get(StringUtils.EMPTY);
        if (groupOwner != null) {
            groupOwnerMap.put(created.getResult(), groupOwner.getValues().iterator().next());
        }

        List<PropagationTask> tasks = propagationManager.getCreateTasks(
                AnyTypeKind.GROUP,
                created.getResult(),
                created.getPropByRes(),
                groupTO.getVirAttrs(),
                excludedResources);
        PropagationReporter propagationReporter =
                ApplicationContextProvider.getBeanFactory().getBean(PropagationReporter.class);
        taskExecutor.execute(tasks, propagationReporter, nullPriorityAsync);

        return new ImmutablePair<>(created.getResult(), null);
    }

    @Override
    public Pair<String, List<PropagationStatus>> update(final GroupPatch groupPatch, final boolean nullPriorityAsync) {
        return update(groupPatch, Collections.<String>emptySet(), nullPriorityAsync);
    }

    @Override
    public Pair<String, List<PropagationStatus>> update(
            final GroupPatch groupPatch, final Set<String> excludedResources, final boolean nullPriorityAsync) {

        WorkflowResult<String> updated = gwfAdapter.update(groupPatch);

        List<PropagationTask> tasks = propagationManager.getUpdateTasks(
                AnyTypeKind.GROUP,
                updated.getResult(),
                false,
                null,
                updated.getPropByRes(),
                groupPatch.getVirAttrs(),
                excludedResources);
        PropagationReporter propagationReporter =
                ApplicationContextProvider.getBeanFactory().getBean(PropagationReporter.class);
        taskExecutor.execute(tasks, propagationReporter, nullPriorityAsync);

        return new ImmutablePair<>(updated.getResult(), propagationReporter.getStatuses());
    }

    @Override
    public List<PropagationStatus> delete(final String key, final boolean nullPriorityAsync) {
        return delete(key, Collections.<String>emptySet(), nullPriorityAsync);
    }

    @Override
    public List<PropagationStatus> delete(
            final String key, final Set<String> excludedResources, final boolean nullPriorityAsync) {

        List<PropagationTask> tasks = new ArrayList<>();

        // Generate propagation tasks for deleting users and any objects from group resources, 
        // if they are on those resources only because of the reason being deleted (see SYNCOPE-357)
        for (Map.Entry<String, PropagationByResource> entry
                : groupDAO.findUsersWithTransitiveResources(key).entrySet()) {

            tasks.addAll(propagationManager.getDeleteTasks(
                    AnyTypeKind.USER,
                    entry.getKey(),
                    entry.getValue(),
                    excludedResources));
        }
        for (Map.Entry<String, PropagationByResource> entry
                : groupDAO.findAnyObjectsWithTransitiveResources(key).entrySet()) {

            tasks.addAll(propagationManager.getDeleteTasks(
                    AnyTypeKind.ANY_OBJECT,
                    entry.getKey(),
                    entry.getValue(),
                    excludedResources));
        }

        // Generate propagation tasks for deleting this group from resources
        tasks.addAll(propagationManager.getDeleteTasks(
                AnyTypeKind.GROUP,
                key,
                null,
                null));

        PropagationReporter propagationReporter =
                ApplicationContextProvider.getBeanFactory().getBean(PropagationReporter.class);
        taskExecutor.execute(tasks, propagationReporter, nullPriorityAsync);

        gwfAdapter.delete(key);

        return propagationReporter.getStatuses();
    }

    @Override
    public String unlink(final GroupPatch groupPatch) {
        WorkflowResult<String> updated = gwfAdapter.update(groupPatch);
        return updated.getResult();
    }

    @Override
    public List<PropagationStatus> provision(
            final String key, final Collection<String> resources, final boolean nullPriorityAsync) {

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.addAll(ResourceOperation.UPDATE, resources);

        List<PropagationTask> tasks = propagationManager.getUpdateTasks(
                AnyTypeKind.GROUP,
                key,
                false,
                null,
                propByRes,
                null,
                null);
        PropagationReporter propagationReporter =
                ApplicationContextProvider.getBeanFactory().getBean(PropagationReporter.class);
        taskExecutor.execute(tasks, propagationReporter, nullPriorityAsync);

        return propagationReporter.getStatuses();
    }

    @Override
    public List<PropagationStatus> deprovision(
            final String key, final Collection<String> resources, final boolean nullPriorityAsync) {

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.addAll(ResourceOperation.DELETE, resources);

        List<PropagationTask> tasks = propagationManager.getDeleteTasks(
                AnyTypeKind.GROUP,
                key,
                propByRes,
                CollectionUtils.removeAll(groupDAO.authFind(key).getResourceKeys(), resources));
        PropagationReporter propagationReporter =
                ApplicationContextProvider.getBeanFactory().getBean(PropagationReporter.class);
        taskExecutor.execute(tasks, propagationReporter, nullPriorityAsync);

        return propagationReporter.getStatuses();
    }

    @Override
    public String link(final GroupPatch groupPatch) {
        return gwfAdapter.update(groupPatch).getResult();
    }

}

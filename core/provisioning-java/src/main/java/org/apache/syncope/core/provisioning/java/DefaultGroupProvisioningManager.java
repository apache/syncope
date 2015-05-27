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
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.mod.GroupMod;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.provisioning.api.GroupProvisioningManager;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.propagation.PropagationException;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.misc.spring.ApplicationContextProvider;
import org.apache.syncope.core.workflow.api.GroupWorkflowAdapter;

public class DefaultGroupProvisioningManager implements GroupProvisioningManager {

    private static final Logger LOG = LoggerFactory.getLogger(GroupProvisioningManager.class);

    @Autowired
    protected GroupWorkflowAdapter gwfAdapter;

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @Autowired
    protected GroupDAO groupDAO;

    @Override
    public Pair<Long, List<PropagationStatus>> create(final GroupTO group) {
        return create(group, Collections.<String>emptySet());
    }

    @Override
    public Pair<Long, List<PropagationStatus>> create(final GroupTO groupTO, final Set<String> excludedResources) {
        WorkflowResult<Long> created = gwfAdapter.create(groupTO);

        List<PropagationTask> tasks = propagationManager.getGroupCreateTasks(
                created, groupTO.getVirAttrs(), excludedResources);
        PropagationReporter propagationReporter = ApplicationContextProvider.getApplicationContext().getBean(
                PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        return new ImmutablePair<>(created.getResult(), propagationReporter.getStatuses());
    }

    @Override
    public Pair<Long, List<PropagationStatus>> create(
            final GroupTO groupTO, final Map<Long, String> groupOwnerMap, final Set<String> excludedResources) {

        WorkflowResult<Long> created = gwfAdapter.create(groupTO);
        AttrTO groupOwner = groupTO.getPlainAttrMap().get(StringUtils.EMPTY);
        if (groupOwner != null) {
            groupOwnerMap.put(created.getResult(), groupOwner.getValues().iterator().next());
        }

        List<PropagationTask> tasks = propagationManager.getGroupCreateTasks(
                created, groupTO.getVirAttrs(), excludedResources);

        taskExecutor.execute(tasks);

        return new ImmutablePair<>(created.getResult(), null);
    }

    @Override
    public Pair<Long, List<PropagationStatus>> update(final GroupMod groupObjectMod) {
        return update(groupObjectMod, Collections.<String>emptySet());
    }

    @Override
    public Pair<Long, List<PropagationStatus>> update(
            final GroupMod groupMod, final Set<String> excludedResources) {

        WorkflowResult<Long> updated = gwfAdapter.update(groupMod);

        List<PropagationTask> tasks = propagationManager.getGroupUpdateTasks(updated,
                groupMod.getVirAttrsToRemove(), groupMod.getVirAttrsToUpdate(), excludedResources);
        PropagationReporter propagationReporter =
                ApplicationContextProvider.getApplicationContext().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        return new ImmutablePair<>(updated.getResult(), propagationReporter.getStatuses());
    }

    @Override
    public List<PropagationStatus> delete(final Long groupObjectKey) {
        return delete(groupObjectKey, Collections.<String>emptySet());
    }

    @Override
    public List<PropagationStatus> delete(final Long groupKey, final Set<String> excludedResources) {
        List<PropagationTask> tasks = new ArrayList<>();

        Group group = groupDAO.authFind(groupKey);
        if (group != null) {
            // Generate propagation tasks for deleting users from group resources, if they are on those resources only
            // because of the reason being deleted (see SYNCOPE-357)
            for (Map.Entry<Long, PropagationByResource> entry
                    : groupDAO.findUsersWithTransitiveResources(group.getKey()).entrySet()) {

                WorkflowResult<Long> wfResult =
                        new WorkflowResult<>(entry.getKey(), entry.getValue(), Collections.<String>emptySet());
                tasks.addAll(propagationManager.getUserDeleteTasks(wfResult.getResult(), excludedResources));
            }
            for (Map.Entry<Long, PropagationByResource> entry
                    : groupDAO.findAnyObjectsWithTransitiveResources(group.getKey()).entrySet()) {

                WorkflowResult<Long> wfResult =
                        new WorkflowResult<>(entry.getKey(), entry.getValue(), Collections.<String>emptySet());
                tasks.addAll(propagationManager.getAnyObjectDeleteTasks(wfResult.getResult(), excludedResources));
            }

            // Generate propagation tasks for deleting this group from resources
            tasks.addAll(propagationManager.getGroupDeleteTasks(group.getKey()));
        }

        PropagationReporter propagationReporter = ApplicationContextProvider.getApplicationContext().
                getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        gwfAdapter.delete(groupKey);

        return propagationReporter.getStatuses();
    }

    @Override
    public Long unlink(final GroupMod groupMod) {
        WorkflowResult<Long> updated = gwfAdapter.update(groupMod);
        return updated.getResult();
    }

    @Override
    public List<PropagationStatus> deprovision(final Long groupKey, final Collection<String> resources) {
        Group group = groupDAO.authFind(groupKey);

        Collection<String> noPropResourceName = CollectionUtils.removeAll(group.getResourceNames(), resources);

        List<PropagationTask> tasks = propagationManager.getGroupDeleteTasks(
                groupKey, new HashSet<>(resources), noPropResourceName);
        PropagationReporter propagationReporter =
                ApplicationContextProvider.getApplicationContext().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }
        return propagationReporter.getStatuses();
    }

    @Override
    public Long link(final GroupMod groupMod) {
        return gwfAdapter.update(groupMod).getResult();
    }

}

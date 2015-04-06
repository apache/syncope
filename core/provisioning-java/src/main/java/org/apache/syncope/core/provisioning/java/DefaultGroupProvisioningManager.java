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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.mod.GroupMod;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.core.persistence.api.GroupEntitlementUtil;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.provisioning.api.GroupProvisioningManager;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.propagation.PropagationException;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.misc.security.AuthContextUtil;
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
    public Map.Entry<Long, List<PropagationStatus>> create(final GroupTO subject) {
        return create(subject, Collections.<String>emptySet());
    }

    @Override
    public Map.Entry<Long, List<PropagationStatus>> create(final GroupTO subject, final Set<String> excludedResources) {
        WorkflowResult<Long> created = gwfAdapter.create(subject);

        AuthContextUtil.extendAuthContext(created.getResult(), GroupEntitlementUtil.getEntitlementNameFromGroupKey(created.getResult()));

        List<PropagationTask> tasks =
                propagationManager.getGroupCreateTaskIds(created, subject.getVirAttrs(), excludedResources);
        PropagationReporter propagationReporter = ApplicationContextProvider.getApplicationContext().getBean(
                PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        return new AbstractMap.SimpleEntry<>(created.getResult(), propagationReporter.getStatuses());
    }

    @Override
    public Map.Entry<Long, List<PropagationStatus>> create(
            final GroupTO groupTO, final Map<Long, String> groupOwnerMap, final Set<String> excludedResources) {

        WorkflowResult<Long> created = gwfAdapter.create(groupTO);
        AttrTO groupOwner = groupTO.getPlainAttrMap().get(StringUtils.EMPTY);
        if (groupOwner != null) {
            groupOwnerMap.put(created.getResult(), groupOwner.getValues().iterator().next());
        }

        AuthContextUtil.extendAuthContext(created.getResult(), 
                GroupEntitlementUtil.getEntitlementNameFromGroupKey(created.getResult()));

        List<PropagationTask> tasks = propagationManager.getGroupCreateTaskIds(
                created, groupTO.getVirAttrs(), excludedResources);

        taskExecutor.execute(tasks);

        return new AbstractMap.SimpleEntry<>(created.getResult(), null);
    }

    @Override
    public Map.Entry<Long, List<PropagationStatus>> update(final GroupMod subjectMod) {
        return update(subjectMod, Collections.<String>emptySet());
    }

    @Override
    public Map.Entry<Long, List<PropagationStatus>> update(
            final GroupMod subjectMod, final Set<String> excludedResources) {

        WorkflowResult<Long> updated = gwfAdapter.update(subjectMod);

        List<PropagationTask> tasks = propagationManager.getGroupUpdateTaskIds(updated,
                subjectMod.getVirAttrsToRemove(), subjectMod.getVirAttrsToUpdate());
        PropagationReporter propagationReporter =
                ApplicationContextProvider.getApplicationContext().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        Map.Entry<Long, List<PropagationStatus>> result = new AbstractMap.SimpleEntry<>(
                updated.getResult(), propagationReporter.getStatuses());
        return result;
    }

    @Override
    public List<PropagationStatus> delete(final Long subjectKey) {
        final List<Group> toBeDeprovisioned = new ArrayList<>();

        final Group syncopeGroup = groupDAO.find(subjectKey);

        if (syncopeGroup != null) {
            toBeDeprovisioned.add(syncopeGroup);

            final List<Group> descendants = groupDAO.findDescendants(toBeDeprovisioned.get(0));
            if (descendants != null) {
                toBeDeprovisioned.addAll(descendants);
            }
        }

        final List<PropagationTask> tasks = new ArrayList<>();

        for (Group group : toBeDeprovisioned) {
            // Generate propagation tasks for deleting users from group resources, if they are on those resources only
            // because of the reason being deleted (see SYNCOPE-357)
            for (Map.Entry<Long, PropagationByResource> entry : groupDAO.findUsersWithIndirectResources(group.
                    getKey()).entrySet()) {

                WorkflowResult<Long> wfResult =
                        new WorkflowResult<>(entry.getKey(), entry.getValue(), Collections.<String>emptySet());
                tasks.addAll(propagationManager.getUserDeleteTaskIds(wfResult));
            }

            // Generate propagation tasks for deleting this group from resources
            tasks.addAll(propagationManager.getGroupDeleteTaskIds(group.getKey()));
        }

        PropagationReporter propagationReporter = ApplicationContextProvider.getApplicationContext().
                getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        try {
            gwfAdapter.delete(subjectKey);
        } catch (RuntimeException e) {
            throw e;
        }

        return propagationReporter.getStatuses();
    }

    @Override
    public Long unlink(final GroupMod subjectMod) {
        WorkflowResult<Long> updated = gwfAdapter.update(subjectMod);
        return updated.getResult();
    }

    @Override
    public List<PropagationStatus> deprovision(final Long groupKey, final Collection<String> resources) {
        Group group = groupDAO.authFetch(groupKey);

        Set<String> noPropResourceName = group.getResourceNames();
        noPropResourceName.removeAll(resources);

        List<PropagationTask> tasks = propagationManager.getGroupDeleteTaskIds(
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
    public Long link(final GroupMod subjectMod) {
        return gwfAdapter.update(subjectMod).getResult();
    }

}

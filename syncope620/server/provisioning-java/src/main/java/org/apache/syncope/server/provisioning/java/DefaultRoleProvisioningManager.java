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
package org.apache.syncope.server.provisioning.java;

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
import org.apache.syncope.common.lib.mod.RoleMod;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.server.persistence.api.RoleEntitlementUtil;
import org.apache.syncope.server.persistence.api.dao.RoleDAO;
import org.apache.syncope.server.persistence.api.entity.role.Role;
import org.apache.syncope.server.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.server.provisioning.api.RoleProvisioningManager;
import org.apache.syncope.server.provisioning.api.WorkflowResult;
import org.apache.syncope.server.provisioning.api.propagation.PropagationException;
import org.apache.syncope.server.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.server.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.server.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.server.misc.security.AuthContextUtil;
import org.apache.syncope.server.misc.spring.ApplicationContextProvider;
import org.apache.syncope.server.workflow.api.RoleWorkflowAdapter;
import org.springframework.stereotype.Component;

@Component
public class DefaultRoleProvisioningManager implements RoleProvisioningManager {

    private static final Logger LOG = LoggerFactory.getLogger(RoleProvisioningManager.class);

    @Autowired
    protected RoleWorkflowAdapter rwfAdapter;

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @Autowired
    protected RoleDAO roleDAO;

    @Override
    public Map.Entry<Long, List<PropagationStatus>> create(final RoleTO subject) {
        return create(subject, Collections.<String>emptySet());
    }

    @Override
    public Map.Entry<Long, List<PropagationStatus>> create(final RoleTO subject, final Set<String> excludedResources) {
        WorkflowResult<Long> created = rwfAdapter.create(subject);

        AuthContextUtil.extendAuthContext(
                created.getResult(), RoleEntitlementUtil.getEntitlementNameFromRoleKey(created.getResult()));

        List<PropagationTask> tasks = propagationManager.getRoleCreateTaskIds(created, subject.getVirAttrs());
        PropagationReporter propagationReporter = ApplicationContextProvider.getApplicationContext().getBean(
                PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        Map.Entry<Long, List<PropagationStatus>> result = new AbstractMap.SimpleEntry<Long, List<PropagationStatus>>(
                created.getResult(), propagationReporter.getStatuses());
        return result;
    }

    @Override
    public Map.Entry<Long, List<PropagationStatus>> create(
            final RoleTO roleTO, final Map<Long, String> roleOwnerMap, final Set<String> excludedResources) {

        WorkflowResult<Long> created = rwfAdapter.create((RoleTO) roleTO);
        AttrTO roleOwner = roleTO.getAttrMap().get(StringUtils.EMPTY);
        if (roleOwner != null) {
            roleOwnerMap.put(created.getResult(), roleOwner.getValues().iterator().next());
        }

        AuthContextUtil.extendAuthContext(
                created.getResult(), RoleEntitlementUtil.getEntitlementNameFromRoleKey(created.getResult()));

        List<PropagationTask> tasks = propagationManager.getRoleCreateTaskIds(
                created, roleTO.getVirAttrs(), excludedResources);

        taskExecutor.execute(tasks);

        return new AbstractMap.SimpleEntry<Long, List<PropagationStatus>>(created.getResult(), null);
    }

    @Override
    public Map.Entry<Long, List<PropagationStatus>> update(final RoleMod subjectMod) {
        return update(subjectMod, Collections.<String>emptySet());
    }

    @Override
    public Map.Entry<Long, List<PropagationStatus>> update(
            final RoleMod subjectMod, final Set<String> excludedResources) {

        WorkflowResult<Long> updated = rwfAdapter.update(subjectMod);

        List<PropagationTask> tasks = propagationManager.getRoleUpdateTaskIds(updated,
                subjectMod.getVirAttrsToRemove(), subjectMod.getVirAttrsToUpdate());
        PropagationReporter propagationReporter = ApplicationContextProvider.getApplicationContext().getBean(
                PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        Map.Entry<Long, List<PropagationStatus>> result = new AbstractMap.SimpleEntry<Long, List<PropagationStatus>>(
                updated.getResult(), propagationReporter.getStatuses());
        return result;
    }

    @Override
    public List<PropagationStatus> delete(final Long subjectId) {
        final List<Role> toBeDeprovisioned = new ArrayList<>();

        final Role syncopeRole = roleDAO.find(subjectId);

        if (syncopeRole != null) {
            toBeDeprovisioned.add(syncopeRole);

            final List<Role> descendants = roleDAO.findDescendants(toBeDeprovisioned.get(0));
            if (descendants != null) {
                toBeDeprovisioned.addAll(descendants);
            }
        }

        final List<PropagationTask> tasks = new ArrayList<>();

        for (Role role : toBeDeprovisioned) {
            // Generate propagation tasks for deleting users from role resources, if they are on those resources only
            // because of the reason being deleted (see SYNCOPE-357)
            for (Map.Entry<Long, PropagationByResource> entry : roleDAO.findUsersWithIndirectResources(role.
                    getKey()).entrySet()) {

                WorkflowResult<Long> wfResult =
                        new WorkflowResult<>(entry.getKey(), entry.getValue(), Collections.<String>emptySet());
                tasks.addAll(propagationManager.getUserDeleteTaskIds(wfResult));
            }

            // Generate propagation tasks for deleting this role from resources
            tasks.addAll(propagationManager.getRoleDeleteTaskIds(role.getKey()));
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
            rwfAdapter.delete(subjectId);
        } catch (RuntimeException e) {
            throw e;
        }

        return propagationReporter.getStatuses();
    }

    @Override
    public Long unlink(final RoleMod subjectMod) {
        WorkflowResult<Long> updated = rwfAdapter.update(subjectMod);
        return updated.getResult();
    }

    @Override
    public List<PropagationStatus> deprovision(final Long roleKey, final Collection<String> resources) {
        final Role role = roleDAO.authFetch(roleKey);

        final Set<String> noPropResourceName = role.getResourceNames();
        noPropResourceName.removeAll(resources);

        final List<PropagationTask> tasks = propagationManager.getRoleDeleteTaskIds(roleKey, new HashSet<String>(
                resources), noPropResourceName);
        PropagationReporter propagationReporter = ApplicationContextProvider.getApplicationContext().getBean(
                PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }
        return propagationReporter.getStatuses();
    }

    @Override
    public Long link(final RoleMod subjectMod) {
        return rwfAdapter.update(subjectMod).getResult();
    }

}

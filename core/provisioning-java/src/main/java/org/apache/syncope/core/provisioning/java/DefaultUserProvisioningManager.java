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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.common.lib.mod.MembershipMod;
import org.apache.syncope.common.lib.mod.StatusMod;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.core.provisioning.api.propagation.PropagationException;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.api.sync.ProvisioningResult;
import org.apache.syncope.core.misc.spring.ApplicationContextProvider;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class DefaultUserProvisioningManager implements UserProvisioningManager {

    private static final Logger LOG = LoggerFactory.getLogger(UserProvisioningManager.class);

    @Autowired
    protected UserWorkflowAdapter uwfAdapter;

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @Autowired
    protected VirAttrHandler virtAttrHandler;

    @Autowired
    protected UserDAO userDAO;

    @Override
    public Map.Entry<Long, List<PropagationStatus>> create(final UserTO userTO) {
        return create(userTO, true, false, null, Collections.<String>emptySet());
    }

    @Override
    public Map.Entry<Long, List<PropagationStatus>> create(final UserTO userTO, final boolean storePassword) {
        return create(userTO, storePassword, false, null, Collections.<String>emptySet());
    }

    @Override
    public Map.Entry<Long, List<PropagationStatus>> create(final UserTO userTO, final boolean storePassword,
            final boolean disablePwdPolicyCheck, final Boolean enabled, final Set<String> excludedResources) {

        WorkflowResult<Map.Entry<Long, Boolean>> created;
        try {
            created = uwfAdapter.create(userTO, disablePwdPolicyCheck, enabled, storePassword);
        } catch (PropagationException e) {
            throw e;
        }

        List<PropagationTask> tasks = propagationManager.getUserCreateTaskIds(
                created, userTO.getPassword(), userTO.getVirAttrs(), excludedResources, userTO.getMemberships());
        PropagationReporter propagationReporter =
                ApplicationContextProvider.getApplicationContext().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        return new AbstractMap.SimpleEntry<>(created.getResult().getKey(), propagationReporter.getStatuses());
    }

    @Override
    public Map.Entry<Long, List<PropagationStatus>> update(final UserMod userMod) {
        return update(userMod, false);
    }

    @Override
    public Map.Entry<Long, List<PropagationStatus>> update(final UserMod userMod, final boolean removeMemberships) {
        WorkflowResult<Map.Entry<UserMod, Boolean>> updated = uwfAdapter.update(userMod);

        List<PropagationTask> tasks = propagationManager.getUserUpdateTaskIds(updated);
        if (tasks.isEmpty()) {
            // SYNCOPE-459: take care of user virtual attributes ...
            final PropagationByResource propByResVirAttr = virtAttrHandler.fillVirtual(
                    updated.getResult().getKey().getKey(),
                    userMod.getVirAttrsToRemove(),
                    userMod.getVirAttrsToUpdate());
            // SYNCOPE-501: update only virtual attributes (if any of them changed), password propagation is
            // not required, take care also of membership virtual attributes
            boolean addOrUpdateMemberships = false;
            for (MembershipMod membershipMod : userMod.getMembershipsToAdd()) {
                if (!virtAttrHandler.fillMembershipVirtual(
                        updated.getResult().getKey().getKey(),
                        membershipMod.getRole(),
                        null,
                        membershipMod.getVirAttrsToRemove(),
                        membershipMod.getVirAttrsToUpdate(),
                        false).isEmpty()) {
                    addOrUpdateMemberships = true;
                }
            }
            tasks.addAll(!propByResVirAttr.isEmpty() || addOrUpdateMemberships || removeMemberships
                    ? propagationManager.getUserUpdateTaskIds(updated, false, null)
                    : Collections.<PropagationTask>emptyList());
        }
        PropagationReporter propagationReporter = ApplicationContextProvider.getApplicationContext().
                getBean(PropagationReporter.class);
        if (!tasks.isEmpty()) {
            try {
                taskExecutor.execute(tasks, propagationReporter);
            } catch (PropagationException e) {
                LOG.error("Error propagation primary resource", e);
                propagationReporter.onPrimaryResourceFailure(tasks);
            }
        }

        Map.Entry<Long, List<PropagationStatus>> result = new AbstractMap.SimpleEntry<>(
                updated.getResult().getKey().getKey(), propagationReporter.getStatuses());
        return result;
    }

    @Override
    public List<PropagationStatus> delete(final Long userKey) {
        return delete(userKey, Collections.<String>emptySet());
    }

    @Override
    public List<PropagationStatus> delete(final Long subjectId, final Set<String> excludedResources) {
        // Note here that we can only notify about "delete", not any other
        // task defined in workflow process definition: this because this
        // information could only be available after uwfAdapter.delete(), which
        // will also effectively remove user from db, thus making virtually
        // impossible by NotificationManager to fetch required user information
        List<PropagationTask> tasks = propagationManager.getUserDeleteTaskIds(subjectId, excludedResources);

        PropagationReporter propagationReporter =
                ApplicationContextProvider.getApplicationContext().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        try {
            uwfAdapter.delete(subjectId);
        } catch (PropagationException e) {
            throw e;
        }

        return propagationReporter.getStatuses();
    }

    @Override
    public Long unlink(final UserMod userMod) {
        WorkflowResult<Map.Entry<UserMod, Boolean>> updated = uwfAdapter.update(userMod);
        return updated.getResult().getKey().getKey();
    }

    @Override
    public Long link(final UserMod subjectMod) {
        return uwfAdapter.update(subjectMod).getResult().getKey().getKey();
    }

    @Override
    public Map.Entry<Long, List<PropagationStatus>> activate(final User user, final StatusMod statusMod) {
        WorkflowResult<Long> updated;
        if (statusMod.isOnSyncope()) {
            updated = uwfAdapter.activate(user.getKey(), statusMod.getToken());
        } else {
            updated = new WorkflowResult<>(user.getKey(), null, statusMod.getType().name().toLowerCase());
        }

        List<PropagationStatus> statuses = propagateStatus(user, statusMod);
        return new AbstractMap.SimpleEntry<>(updated.getResult(), statuses);
    }

    @Override
    public Map.Entry<Long, List<PropagationStatus>> reactivate(final User user, final StatusMod statusMod) {
        WorkflowResult<Long> updated;
        if (statusMod.isOnSyncope()) {
            updated = uwfAdapter.reactivate(user.getKey());
        } else {
            updated = new WorkflowResult<>(user.getKey(), null, statusMod.getType().name().toLowerCase());
        }

        List<PropagationStatus> statuses = propagateStatus(user, statusMod);
        return new AbstractMap.SimpleEntry<>(updated.getResult(), statuses);
    }

    @Override
    public Map.Entry<Long, List<PropagationStatus>> suspend(final User user, final StatusMod statusMod) {
        WorkflowResult<Long> updated;
        if (statusMod.isOnSyncope()) {
            updated = uwfAdapter.suspend(user.getKey());
        } else {
            updated = new WorkflowResult<>(user.getKey(), null, statusMod.getType().name().toLowerCase());
        }

        List<PropagationStatus> statuses = propagateStatus(user, statusMod);
        return new AbstractMap.SimpleEntry<>(updated.getResult(), statuses);
    }

    protected List<PropagationStatus> propagateStatus(final User user, final StatusMod statusMod) {
        Set<String> resourcesToBeExcluded = new HashSet<>(user.getResourceNames());
        resourcesToBeExcluded.removeAll(statusMod.getResourceNames());

        List<PropagationTask> tasks = propagationManager.getUserUpdateTaskIds(
                user, statusMod.getType() != StatusMod.ModType.SUSPEND, resourcesToBeExcluded);
        PropagationReporter propReporter =
                ApplicationContextProvider.getApplicationContext().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propReporter.onPrimaryResourceFailure(tasks);
        }

        return propReporter.getStatuses();

    }

    @Override
    public void innerSuspend(final User user, final boolean propagate) {
        final WorkflowResult<Long> updated = uwfAdapter.suspend(user);

        // propagate suspension if and only if it is required by policy
        if (propagate) {
            UserMod userMod = new UserMod();
            userMod.setKey(updated.getResult());

            final List<PropagationTask> tasks = propagationManager.getUserUpdateTaskIds(
                    new WorkflowResult<Map.Entry<UserMod, Boolean>>(
                            new AbstractMap.SimpleEntry<>(userMod, Boolean.FALSE),
                            updated.getPropByRes(), updated.getPerformedTasks()));

            taskExecutor.execute(tasks);
        }
    }

    @Override
    public List<PropagationStatus> deprovision(final Long userKey, final Collection<String> resources) {
        final User user = userDAO.authFetch(userKey);

        final Set<String> noPropResourceName = user.getResourceNames();
        noPropResourceName.removeAll(resources);

        final List<PropagationTask> tasks =
                propagationManager.getUserDeleteTaskIds(userKey, new HashSet<>(resources), noPropResourceName);
        final PropagationReporter propagationReporter =
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
    public Map.Entry<Long, List<PropagationStatus>> update(final UserMod userMod, final Long key,
            final ProvisioningResult result, final Boolean enabled, final Set<String> excludedResources) {

        WorkflowResult<Map.Entry<UserMod, Boolean>> updated;
        try {
            updated = uwfAdapter.update(userMod);
        } catch (Exception e) {
            LOG.error("Update of user {} failed, trying to sync its status anyway (if configured)", key, e);

            result.setStatus(ProvisioningResult.Status.FAILURE);
            result.setMessage("Update failed, trying to sync status anyway (if configured)\n" + e.getMessage());

            updated = new WorkflowResult<Map.Entry<UserMod, Boolean>>(
                    new AbstractMap.SimpleEntry<>(userMod, false), new PropagationByResource(),
                    new HashSet<String>());
        }

        if (enabled != null) {
            User user = userDAO.find(key);

            WorkflowResult<Long> enableUpdate = null;
            if (user.isSuspended() == null) {
                enableUpdate = uwfAdapter.activate(key, null);
            } else if (enabled && user.isSuspended()) {
                enableUpdate = uwfAdapter.reactivate(key);
            } else if (!enabled && !user.isSuspended()) {
                enableUpdate = uwfAdapter.suspend(key);
            }

            if (enableUpdate != null) {
                if (enableUpdate.getPropByRes() != null) {
                    updated.getPropByRes().merge(enableUpdate.getPropByRes());
                    updated.getPropByRes().purge();
                }
                updated.getPerformedTasks().addAll(enableUpdate.getPerformedTasks());
            }
        }

        PropagationReporter propagationReporter = ApplicationContextProvider.getApplicationContext().
                getBean(PropagationReporter.class);

        List<PropagationTask> tasks = propagationManager.getUserUpdateTaskIds(
                updated, updated.getResult().getKey().getPassword() != null, excludedResources);

        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        return new AbstractMap.SimpleEntry<>(updated.getResult().getKey().getKey(),
                propagationReporter.getStatuses());

    }

    @Override
    public void requestPasswordReset(final Long id) {
        uwfAdapter.requestPasswordReset(id);
    }

    @Override
    public void confirmPasswordReset(final User user, final String token, final String password) {
        uwfAdapter.confirmPasswordReset(user.getKey(), token, password);

        List<PropagationTask> tasks = propagationManager.getUserUpdateTaskIds(user, null, null);
        PropagationReporter propReporter =
                ApplicationContextProvider.getApplicationContext().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propReporter.onPrimaryResourceFailure(tasks);
        }
    }
}

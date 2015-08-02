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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.mod.StatusMod;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.provisioning.api.propagation.PropagationException;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.api.sync.ProvisioningResult;
import org.apache.syncope.core.misc.spring.ApplicationContextProvider;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
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
    public Pair<Long, List<PropagationStatus>> create(final UserTO userTO) {
        return create(userTO, true, false, null, Collections.<String>emptySet());
    }

    @Override
    public Pair<Long, List<PropagationStatus>> create(final UserTO userTO, final boolean storePassword) {
        return create(userTO, storePassword, false, null, Collections.<String>emptySet());
    }

    @Override
    public Pair<Long, List<PropagationStatus>> create(final UserTO userTO, final Set<String> excludedResources) {
        return create(userTO, false, false, null, excludedResources);
    }

    @Override
    public Pair<Long, List<PropagationStatus>> create(final UserTO userTO, final boolean storePassword,
            final boolean disablePwdPolicyCheck, final Boolean enabled, final Set<String> excludedResources) {

        WorkflowResult<Pair<Long, Boolean>> created =
                uwfAdapter.create(userTO, disablePwdPolicyCheck, enabled, storePassword);

        List<PropagationTask> tasks = propagationManager.getUserCreateTasks(
                created.getResult().getKey(),
                created.getResult().getValue(),
                created.getPropByRes(),
                userTO.getPassword(),
                userTO.getVirAttrs(),
                excludedResources);
        PropagationReporter propagationReporter =
                ApplicationContextProvider.getBeanFactory().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        return new ImmutablePair<>(created.getResult().getKey(), propagationReporter.getStatuses());
    }

    @Override
    public Pair<Long, List<PropagationStatus>> update(final UserMod userMod) {
        WorkflowResult<Pair<UserMod, Boolean>> updated = uwfAdapter.update(userMod);

        List<PropagationTask> tasks = propagationManager.getUserUpdateTasks(updated);
        if (tasks.isEmpty()) {
            // SYNCOPE-459: take care of user virtual attributes ...
            PropagationByResource propByResVirAttr = virtAttrHandler.fillVirtual(
                    updated.getResult().getKey().getKey(),
                    AnyTypeKind.USER,
                    userMod.getVirAttrsToRemove(),
                    userMod.getVirAttrsToUpdate());
            tasks.addAll(!propByResVirAttr.isEmpty()
                    ? propagationManager.getUserUpdateTasks(updated, false, null)
                    : Collections.<PropagationTask>emptyList());
        }
        PropagationReporter propagationReporter = ApplicationContextProvider.getBeanFactory().
                getBean(PropagationReporter.class);
        if (!tasks.isEmpty()) {
            try {
                taskExecutor.execute(tasks, propagationReporter);
            } catch (PropagationException e) {
                LOG.error("Error propagation primary resource", e);
                propagationReporter.onPrimaryResourceFailure(tasks);
            }
        }

        return new ImmutablePair<>(updated.getResult().getKey().getKey(), propagationReporter.getStatuses());
    }

    @Override
    public Pair<Long, List<PropagationStatus>> update(final UserMod userMod, final Set<String> excludedResources) {
        return update(userMod, userMod.getKey(), new ProvisioningResult(), null, excludedResources);
    }

    @Override
    public Pair<Long, List<PropagationStatus>> update(final UserMod userMod, final Long key,
            final ProvisioningResult result, final Boolean enabled, final Set<String> excludedResources) {

        WorkflowResult<Pair<UserMod, Boolean>> updated;
        try {
            updated = uwfAdapter.update(userMod);
        } catch (Exception e) {
            LOG.error("Update of user {} failed, trying to sync its status anyway (if configured)", key, e);

            result.setStatus(ProvisioningResult.Status.FAILURE);
            result.setMessage("Update failed, trying to sync status anyway (if configured)\n" + e.getMessage());

            updated = new WorkflowResult<Pair<UserMod, Boolean>>(
                    new ImmutablePair<>(userMod, false), new PropagationByResource(),
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

        List<PropagationTask> tasks = propagationManager.getUserUpdateTasks(
                updated, updated.getResult().getKey().getPassword() != null, excludedResources);
        PropagationReporter propagationReporter = ApplicationContextProvider.getBeanFactory().
                getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        return new ImmutablePair<>(updated.getResult().getKey().getKey(), propagationReporter.getStatuses());
    }

    @Override
    public List<PropagationStatus> delete(final Long key) {
        return delete(key, Collections.<String>emptySet());
    }

    @Override
    public List<PropagationStatus> delete(final Long key, final Set<String> excludedResources) {
        // Note here that we can only notify about "delete", not any other
        // task defined in workflow process definition: this because this
        // information could only be available after uwfAdapter.delete(), which
        // will also effectively remove user from db, thus making virtually
        // impossible by NotificationManager to fetch required user information
        List<PropagationTask> tasks = propagationManager.getUserDeleteTasks(key, excludedResources);

        PropagationReporter propagationReporter =
                ApplicationContextProvider.getBeanFactory().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        try {
            uwfAdapter.delete(key);
        } catch (PropagationException e) {
            throw e;
        }

        return propagationReporter.getStatuses();
    }

    @Override
    public Long unlink(final UserMod userMod) {
        WorkflowResult<Pair<UserMod, Boolean>> updated = uwfAdapter.update(userMod);
        return updated.getResult().getKey().getKey();
    }

    @Override
    public Long link(final UserMod userMod) {
        return uwfAdapter.update(userMod).getResult().getKey().getKey();
    }

    @Override
    public Pair<Long, List<PropagationStatus>> activate(final StatusMod statusMod) {
        WorkflowResult<Long> updated = statusMod.isOnSyncope()
                ? uwfAdapter.activate(statusMod.getKey(), statusMod.getToken())
                : new WorkflowResult<>(statusMod.getKey(), null, statusMod.getType().name().toLowerCase());

        return new ImmutablePair<>(updated.getResult(), propagateStatus(statusMod));
    }

    @Override
    public Pair<Long, List<PropagationStatus>> reactivate(final StatusMod statusMod) {
        WorkflowResult<Long> updated = statusMod.isOnSyncope()
                ? uwfAdapter.reactivate(statusMod.getKey())
                : new WorkflowResult<>(statusMod.getKey(), null, statusMod.getType().name().toLowerCase());

        return new ImmutablePair<>(updated.getResult(), propagateStatus(statusMod));
    }

    @Override
    public Pair<Long, List<PropagationStatus>> suspend(final StatusMod statusMod) {
        WorkflowResult<Long> updated = statusMod.isOnSyncope()
                ? uwfAdapter.suspend(statusMod.getKey())
                : new WorkflowResult<>(statusMod.getKey(), null, statusMod.getType().name().toLowerCase());

        return new ImmutablePair<>(updated.getResult(), propagateStatus(statusMod));
    }

    protected List<PropagationStatus> propagateStatus(final StatusMod statusMod) {
        Collection<String> noPropResourceNames = CollectionUtils.removeAll(
                userDAO.findAllResourceNames(userDAO.find(statusMod.getKey())), statusMod.getResourceNames());

        List<PropagationTask> tasks = propagationManager.getUserUpdateTasks(
                statusMod.getKey(), statusMod.getType() != StatusMod.ModType.SUSPEND, noPropResourceNames);
        PropagationReporter propReporter =
                ApplicationContextProvider.getBeanFactory().getBean(PropagationReporter.class);
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

            final List<PropagationTask> tasks = propagationManager.getUserUpdateTasks(
                    new WorkflowResult<Pair<UserMod, Boolean>>(
                            new ImmutablePair<>(userMod, Boolean.FALSE),
                            updated.getPropByRes(), updated.getPerformedTasks()));

            taskExecutor.execute(tasks);
        }
    }

    @Override
    public List<PropagationStatus> provision(
            final Long key, final boolean changePwd, final String password, final Collection<String> resources) {

        UserMod userMod = new UserMod();
        userMod.setKey(key);
        userMod.getResourcesToAdd().addAll(resources);

        if (changePwd) {
            StatusMod statusMod = new StatusMod();
            statusMod.setOnSyncope(false);
            statusMod.getResourceNames().addAll(resources);
            userMod.setPwdPropRequest(statusMod);
            userMod.setPassword(password);
        }

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.addAll(ResourceOperation.UPDATE, resources);

        WorkflowResult<Pair<UserMod, Boolean>> wfResult = new WorkflowResult<Pair<UserMod, Boolean>>(
                ImmutablePair.of(userMod, (Boolean) null), propByRes, "update");

        List<PropagationTask> tasks = propagationManager.getUserUpdateTasks(wfResult, changePwd, null);
        PropagationReporter propagationReporter =
                ApplicationContextProvider.getBeanFactory().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        return propagationReporter.getStatuses();
    }

    @Override
    public List<PropagationStatus> deprovision(final Long key, final Collection<String> resources) {
        User user = userDAO.authFind(key);

        List<PropagationTask> tasks = propagationManager.getUserDeleteTasks(
                key,
                new HashSet<>(resources),
                CollectionUtils.removeAll(userDAO.findAllResourceNames(user), resources));
        PropagationReporter propagationReporter =
                ApplicationContextProvider.getBeanFactory().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        return propagationReporter.getStatuses();
    }

    @Override
    public void requestPasswordReset(final Long key) {
        uwfAdapter.requestPasswordReset(key);
    }

    @Override
    public void confirmPasswordReset(final Long key, final String token, final String password) {
        uwfAdapter.confirmPasswordReset(key, token, password);

        UserMod userMod = new UserMod();
        userMod.setKey(key);
        userMod.setPassword(password);

        List<PropagationTask> tasks = propagationManager.getUserUpdateTasks(
                new WorkflowResult<Pair<UserMod, Boolean>>(
                        new ImmutablePair<UserMod, Boolean>(userMod, null), null, "confirmPasswordReset"),
                true, null);
        PropagationReporter propReporter =
                ApplicationContextProvider.getBeanFactory().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propReporter.onPrimaryResourceFailure(tasks);
        }
    }
}

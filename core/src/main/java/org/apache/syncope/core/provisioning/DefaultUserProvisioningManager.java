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
package org.apache.syncope.core.provisioning;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.common.mod.StatusMod;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.to.PropagationStatus;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.propagation.PropagationByResource;
import org.apache.syncope.core.propagation.PropagationException;
import org.apache.syncope.core.propagation.PropagationReporter;
import org.apache.syncope.core.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.propagation.impl.PropagationManager;
import org.apache.syncope.core.rest.data.UserDataBinder;
import org.apache.syncope.core.sync.SyncResult;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.apache.syncope.core.workflow.user.UserWorkflowAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class DefaultUserProvisioningManager implements UserProvisioningManager{

    private static final Logger LOG = LoggerFactory.getLogger(DefaultUserProvisioningManager.class);

    @Autowired
    protected UserWorkflowAdapter uwfAdapter;

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;
    
    @Autowired
    protected UserDataBinder binder;    

    @Autowired
    protected UserDAO userDAO;   

    @Override
    public Map.Entry<Long, List<PropagationStatus>> create(final UserTO userTO){
        return create(userTO, true);
    }
    
    public Map.Entry<Long, List<PropagationStatus>> create(final UserTO userTO, boolean storePassword) {
        WorkflowResult<Map.Entry<Long, Boolean>> created;
        try {
            created = uwfAdapter.create(userTO,storePassword);
        } catch (RuntimeException e) {
            throw e;
        }

        List<PropagationTask> tasks = propagationManager.getUserCreateTaskIds(
                created, userTO.getPassword(), userTO.getVirAttrs(), userTO.getMemberships());

        PropagationReporter propagationReporter = ApplicationContextProvider.getApplicationContext().
                getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        Map.Entry<Long, List<PropagationStatus>> result = new AbstractMap.SimpleEntry<Long, List<PropagationStatus>>(
                created.getResult().getKey(), propagationReporter.getStatuses());
        return result;
    }
    
    @Override
    public Map.Entry<Long, List<PropagationStatus>> create(UserTO userTO, boolean storePassword, boolean disablePwdPolicyCheck, Boolean enabled, Set<String> excludedResources) {
                WorkflowResult<Map.Entry<Long, Boolean>> created;
        try {
            created = uwfAdapter.create(userTO,storePassword);
        } catch (RuntimeException e) {
            throw e;
        }

        List<PropagationTask> tasks = propagationManager.getUserCreateTaskIds(
                created, userTO.getPassword(), userTO.getVirAttrs(), excludedResources, null);
        PropagationReporter propagationReporter = ApplicationContextProvider.getApplicationContext().
                getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        Map.Entry<Long, List<PropagationStatus>> result = new AbstractMap.SimpleEntry<Long, List<PropagationStatus>>(
                created.getResult().getKey(), propagationReporter.getStatuses());
        return result;
    }

    @Override
    public Map.Entry<Long, List<PropagationStatus>> update(final UserMod userMod) {
        return update(userMod, false);
    }
    
    @Override
    public Map.Entry<Long, List<PropagationStatus>> update(UserMod userMod, boolean removeMemberships) {
        WorkflowResult<Map.Entry<UserMod, Boolean>> updated;
        try {
            updated = uwfAdapter.update(userMod);
        } catch (RuntimeException e) {
            throw e;
        }

        List<PropagationTask> tasks = propagationManager.getUserUpdateTaskIds(updated);

        PropagationReporter propagationReporter = ApplicationContextProvider.getApplicationContext().
                getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        Map.Entry<Long, List<PropagationStatus>> result = new AbstractMap.SimpleEntry<Long, List<PropagationStatus>>(
                updated.getResult().getKey().getId(), propagationReporter.getStatuses());
        return result;    
    }
       
    @Override
    public List<PropagationStatus> delete(final Long userId) {

        return delete(userId, Collections.<String>emptySet());
    }

    @Override
    public List<PropagationStatus> delete(Long subjectId, Set<String> excludedResources) {
        List<PropagationTask> tasks = propagationManager.getUserDeleteTaskIds(subjectId,excludedResources);

        PropagationReporter propagationReporter = ApplicationContextProvider.getApplicationContext().
                getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        try {
            uwfAdapter.delete(subjectId);
        } catch (RuntimeException e) {
            throw e;
        }

        return propagationReporter.getStatuses();
    }

    
    @Override
    public Long unlink(UserMod userMod) {
        WorkflowResult<Map.Entry<UserMod, Boolean>> updated = uwfAdapter.update(userMod);
        return updated.getResult().getKey().getId();
    }

    @Override
    public Long link(UserMod subjectMod) {
        return uwfAdapter.update(subjectMod).getResult().getKey().getId();
    }
    
    @Override
    public Map.Entry<Long, List<PropagationStatus>> activate(SyncopeUser user, StatusMod statusMod) {
        WorkflowResult<Long> updated;
        if (statusMod.isOnSyncope()) {
            updated = uwfAdapter.activate(user.getId(), statusMod.getToken());
        } else {
            updated = new WorkflowResult<Long>(user.getId(), null, statusMod.getType().name().toLowerCase());
        }
  
        List<PropagationStatus> statuses = propagateStatus(user, statusMod);
        return new AbstractMap.SimpleEntry<Long, List<PropagationStatus>>(updated.getResult(), statuses);
    }

    @Override
    public Map.Entry<Long, List<PropagationStatus>> reactivate(SyncopeUser user, StatusMod statusMod) {
        WorkflowResult<Long> updated;
        if (statusMod.isOnSyncope()) {
            updated = uwfAdapter.reactivate(user.getId());
        } else {
            updated = new WorkflowResult<Long>(user.getId(), null, statusMod.getType().name().toLowerCase());
        }
        
        List<PropagationStatus> statuses = propagateStatus(user, statusMod);
        return new AbstractMap.SimpleEntry<Long, List<PropagationStatus>>(updated.getResult(), statuses);
    }

    @Override
    public Map.Entry<Long, List<PropagationStatus>> suspend(SyncopeUser user, StatusMod statusMod) {
        WorkflowResult<Long> updated;
        if (statusMod.isOnSyncope()) {
            updated = uwfAdapter.suspend(user.getId());
        } else {
            updated = new WorkflowResult<Long>(user.getId(), null, statusMod.getType().name().toLowerCase());
        }
        
        List<PropagationStatus> statuses = propagateStatus(user, statusMod);
        return new AbstractMap.SimpleEntry<Long, List<PropagationStatus>>(updated.getResult(), statuses);
    }
    
    public List<PropagationStatus> propagateStatus(SyncopeUser user, StatusMod statusMod){
                
        Set<String> resourcesToBeExcluded = new HashSet<String>(user.getResourceNames());
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
    public List<PropagationStatus> deprovision(Long userId, Collection<String> resources) {
        
        final SyncopeUser user = binder.getUserFromId(userId);        
        
        final Set<String> noPropResourceName = user.getResourceNames();
        noPropResourceName.removeAll(resources);
        
        final List<PropagationTask> tasks =
                propagationManager.getUserDeleteTaskIds(userId, new HashSet<String>(resources), noPropResourceName);
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
    public Map.Entry<Long, List<PropagationStatus>> updateInSync(final UserMod userMod,final Long id, final SyncResult result, Boolean enabled, Set<String> excludedResources){
        
        WorkflowResult<Map.Entry<UserMod, Boolean>> updated;
        try {
            updated = uwfAdapter.update(userMod);
        } catch (Exception e) {
            LOG.error("Update of user {} failed, trying to sync its status anyway (if configured)", id, e);

            result.setStatus(SyncResult.Status.FAILURE);
            result.setMessage("Update failed, trying to sync status anyway (if configured)\n" + e.getMessage());

            updated = new WorkflowResult<Map.Entry<UserMod, Boolean>>(
                    new AbstractMap.SimpleEntry<UserMod, Boolean>(userMod, false), new PropagationByResource(),
                    new HashSet<String>());
        }

        if (enabled != null) {
            SyncopeUser user = userDAO.find(id);

            WorkflowResult<Long> enableUpdate = null;
            if (user.isSuspended() == null) {
                enableUpdate = uwfAdapter.activate(id, null);
            } else if (enabled && user.isSuspended()) {
                enableUpdate = uwfAdapter.reactivate(id);
            } else if (!enabled && !user.isSuspended()) {
                enableUpdate = uwfAdapter.suspend(id);
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

            List<PropagationTask> tasks = propagationManager.getUserUpdateTaskIds(updated,updated.getResult().getKey().getPassword() != null,excludedResources);
                
            try {
                    taskExecutor.execute(tasks, propagationReporter);
            } catch (PropagationException e) {
                    LOG.error("Error propagation primary resource", e);
                    propagationReporter.onPrimaryResourceFailure(tasks);
            }
            
            return new AbstractMap.SimpleEntry<Long, List<PropagationStatus>>(updated.getResult().getKey().getId(), propagationReporter.getStatuses());

    }

    @Override
    public void innerSuspend(SyncopeUser user, boolean suspend) {
        
            final WorkflowResult<Long> updated = uwfAdapter.suspend(user);

            // propagate suspension if and only if it is required by policy
            if (suspend) {
                UserMod userMod = new UserMod();
                userMod.setId(updated.getResult());

                final List<PropagationTask> tasks = propagationManager.getUserUpdateTaskIds(
                        new WorkflowResult<Map.Entry<UserMod, Boolean>>(
                                new AbstractMap.SimpleEntry<UserMod, Boolean>(userMod, Boolean.FALSE),
                                updated.getPropByRes(), updated.getPerformedTasks()));

                taskExecutor.execute(tasks);
            }            
    }

    @Override
    public void requestPasswordReset(Long id) {
        uwfAdapter.requestPasswordReset(id);
    }

    @Override
    public void confirmPasswordReset(SyncopeUser user, String token, String password) {
            
        uwfAdapter.confirmPasswordReset(user.getId(), token, password);

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

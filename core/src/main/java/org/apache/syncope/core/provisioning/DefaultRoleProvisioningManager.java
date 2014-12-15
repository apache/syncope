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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.syncope.common.mod.RoleMod;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.to.PropagationStatus;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.propagation.PropagationException;
import org.apache.syncope.core.propagation.PropagationReporter;
import org.apache.syncope.core.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.propagation.impl.PropagationManager;
import org.apache.syncope.core.rest.data.RoleDataBinder;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.apache.syncope.core.util.EntitlementUtil;
import org.apache.syncope.core.workflow.WorkflowResult;
import org.apache.syncope.core.workflow.role.RoleWorkflowAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.commons.lang3.StringUtils;

public class DefaultRoleProvisioningManager implements RoleProvisioningManager{

    private static final Logger LOG = LoggerFactory.getLogger(DefaultRoleProvisioningManager.class);
    @Autowired
    protected RoleWorkflowAdapter rwfAdapter;
    @Autowired
    protected PropagationManager propagationManager;
    @Autowired
    protected PropagationTaskExecutor taskExecutor;    
    @Autowired
    protected RoleDAO roleDAO;
    @Autowired
    protected RoleDataBinder binder;
    
    @Override
    public Map.Entry<Long, List<PropagationStatus>> create(RoleTO subject) {
        return create(subject, Collections.<String>emptySet());
    }

    @Override
    public Map.Entry<Long, List<PropagationStatus>> create(RoleTO subject, Set<String> excludedResources) {
        
        WorkflowResult<Long> created;
        try{
            created = rwfAdapter.create(subject);
        }
        catch(RuntimeException e){
            throw e;
        }

        EntitlementUtil.extendAuthContext(created.getResult());

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
    public Map.Entry<Long, List<PropagationStatus>> createInSync(RoleTO roleTO, Map<Long, String> roleOwnerMap,Set<String> excludedResources) throws PropagationException{
        
        WorkflowResult<Long> created = rwfAdapter.create((RoleTO) roleTO);
        AttributeTO roleOwner = roleTO.getAttrMap().get(StringUtils.EMPTY);
        if (roleOwner != null) {
            roleOwnerMap.put(created.getResult(), roleOwner.getValues().iterator().next());
        }

        EntitlementUtil.extendAuthContext(created.getResult());

        List<PropagationTask> tasks = propagationManager.getRoleCreateTaskIds(created,
                roleTO.getVirAttrs(), excludedResources);

        taskExecutor.execute(tasks);
        
        Map.Entry<Long, List<PropagationStatus>> result = new AbstractMap.SimpleEntry<Long, List<PropagationStatus>>(
                created.getResult(), null);
        return result;
    }
    
    @Override
    public Map.Entry<Long, List<PropagationStatus>> update(RoleMod subjectMod) {
        
        return update(subjectMod, Collections.<String>emptySet());
    }
    
    @Override
    public Map.Entry<Long, List<PropagationStatus>> update(RoleMod subjectMod, Set<String> excludedResources) {
                
        WorkflowResult<Long> updated;
        
        try{
            updated = rwfAdapter.update(subjectMod);
        }
        catch(RuntimeException e){
            throw e;
        }

        List<PropagationTask> tasks = propagationManager.getRoleUpdateTaskIds(updated,
                subjectMod.getVirAttrsToRemove(), subjectMod.getVirAttrsToUpdate(),excludedResources);
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
    public List<PropagationStatus> delete(Long subjectId) {

        final List<SyncopeRole> toBeDeprovisioned = new ArrayList<SyncopeRole>();

        final SyncopeRole syncopeRole = roleDAO.find(subjectId);

        if (syncopeRole != null) {
            toBeDeprovisioned.add(syncopeRole);

            final List<SyncopeRole> descendants = roleDAO.findDescendants(toBeDeprovisioned.get(0));
            if (descendants != null) {
                toBeDeprovisioned.addAll(descendants);
            }
        }

        final List<PropagationTask> tasks = new ArrayList<PropagationTask>();

        for (SyncopeRole role : toBeDeprovisioned) {
            // Generate propagation tasks for deleting users from role resources, if they are on those resources only
            // because of the reason being deleted (see SYNCOPE-357)
            for (WorkflowResult<Long> wfResult : binder.getUsersOnResourcesOnlyBecauseOfRole(role.getId())) {
                tasks.addAll(propagationManager.getUserDeleteTaskIds(wfResult));
            }

            // Generate propagation tasks for deleting this role from resources
            tasks.addAll(propagationManager.getRoleDeleteTaskIds(role.getId()));
        }

        PropagationReporter propagationReporter = ApplicationContextProvider.getApplicationContext().getBean(
                PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        try{
            rwfAdapter.delete(subjectId);
        }
        catch(RuntimeException e){
            throw  e;
        }
        
        return propagationReporter.getStatuses();
    }

    @Override
    public Long unlink(RoleMod subjectMod) {
        WorkflowResult<Long> updated = rwfAdapter.update(subjectMod);
        return updated.getResult();
    }
    
    @Override
    public List<PropagationStatus> deprovision(final Long roleId, final Collection<String> resources){
        final SyncopeRole role = binder.getRoleFromId(roleId);
        
        final Set<String> noPropResourceName = role.getResourceNames();
        noPropResourceName.removeAll(resources);
        
        final List<PropagationTask> tasks = propagationManager.getRoleDeleteTaskIds(roleId, new HashSet<String>(resources), noPropResourceName);
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
    public Long link(RoleMod subjectMod) {
        return rwfAdapter.update(subjectMod).getResult();
    }
    
}

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
package org.apache.syncope.core.quartz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.to.PropagationStatus;
import org.apache.syncope.common.types.ResourceOperation;
import org.apache.syncope.core.persistence.beans.Entitlement;
import org.apache.syncope.core.persistence.beans.PropagationTask;
import org.apache.syncope.core.persistence.beans.TaskExec;
import org.apache.syncope.core.persistence.dao.EntitlementDAO;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.propagation.PropagationByResource;
import org.apache.syncope.core.propagation.PropagationException;
import org.apache.syncope.core.propagation.PropagationReporter;
import org.apache.syncope.core.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.propagation.impl.PropagationManager;
import org.apache.syncope.core.util.ApplicationContextProvider;
import org.identityconnectors.common.Pair;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

public class RoleMemberProvisionTaskJob extends AbstractTaskJob {

    public static final String PROVISION_ACTION_JOBDETAIL_KEY = "provisionAction";

    public static final String ROLE_ID_JOBDETAIL_KEY = "roleId";

    public enum Action {
        PROVISION,
        DEPROVISION;

    }

    @Autowired
    private EntitlementDAO entitlementDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private PropagationManager propagationManager;

    @Autowired
    private PropagationTaskExecutor taskExecutor;

    private Long roleId;

    private Action action;

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        roleId = context.getMergedJobDataMap().getLong(ROLE_ID_JOBDETAIL_KEY);
        action = (Action) context.getMergedJobDataMap().get(PROVISION_ACTION_JOBDETAIL_KEY);

        super.execute(context);
    }

    @Override
    protected String doExecute(final boolean dryRun) throws JobExecutionException {
        // PRE: grant all authorities (i.e. setup the SecurityContextHolder)
        final List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();

        for (Entitlement entitlement : entitlementDAO.findAll()) {
            authorities.add(new SimpleGrantedAuthority(entitlement.getName()));
        }

        final UserDetails userDetails = new User("admin", "FAKE_PASSWORD", true, true, true, true, authorities);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, "FAKE_PASSWORD", authorities));

        try {
            return executeWithSecurityContext();
        } finally {
            // POST: clean up the SecurityContextHolder
            SecurityContextHolder.clearContext();
        }
    }

    protected String executeWithSecurityContext() {
        Map<Long, List<PropagationTask>> propagationTasks = new HashMap<Long, List<PropagationTask>>();

        Pair<Set<String>, List<Long>> resAndMembs = roleDAO.findResourcesAndMembers(roleId);

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.addAll(
                action == Action.PROVISION
                        ? ResourceOperation.UPDATE
                        : ResourceOperation.DELETE,
                resAndMembs.getKey());

        for (Long userId : resAndMembs.getValue()) {
            propagationTasks.put(
                    userId,
                    propagationManager.getUserUpdateTaskIds(userId, propByRes));
        }

        StringBuilder result = new StringBuilder("Role ").append(roleId).append(" members ");
        if (action == Action.DEPROVISION) {
            result.append("de");
        }
        result.append("provision\n\n");

        PropagationReporter propagationReporter =
                ApplicationContextProvider.getApplicationContext().getBean(PropagationReporter.class);
        for (Map.Entry<Long, List<PropagationTask>> entry : propagationTasks.entrySet()) {
            if (entry.getValue().isEmpty()) {
                result.append("User ").append(entry.getKey()).append('\t').
                        append("No propagations\n\n");
            } else {
                try {
                    taskExecutor.execute(entry.getValue(), propagationReporter);
                } catch (PropagationException e) {
                    LOG.error("Error propagation primary resource", e);
                    propagationReporter.onPrimaryResourceFailure(entry.getValue());
                }

                for (PropagationStatus status : propagationReporter.getStatuses()) {
                    result.append("User ").append(entry.getKey()).append('\t').
                            append("Resource ").append(status.getResource()).append('\t').
                            append(status.getStatus());
                    if (StringUtils.isNotBlank(status.getFailureReason())) {
                        result.append('\n').append(status.getFailureReason()).append('\n');
                    }
                    result.append("\n");
                }
                result.append("\n");
            }
        }

        return result.toString();
    }

    @Override
    protected boolean hasToBeRegistered(final TaskExec execution) {
        // always record execution result
        return true;
    }

}

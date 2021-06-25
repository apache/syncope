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
package org.apache.syncope.core.provisioning.java.job;

import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ProvisionAction;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.search.MembershipCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.AnyObjectProvisioningManager;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class GroupMemberProvisionTaskJobDelegate extends AbstractSchedTaskJobDelegate {

    public static final String ACTION_JOBDETAIL_KEY = "action";

    public static final String GROUP_KEY_JOBDETAIL_KEY = "groupKey";

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private AnySearchDAO searchDAO;

    @Autowired
    private UserProvisioningManager userProvisioningManager;

    @Autowired
    private AnyObjectProvisioningManager anyObjectProvisioningManager;

    private String groupKey;

    private ProvisionAction action;

    @Transactional
    @Override
    public void execute(final String taskKey, final boolean dryRun, final JobExecutionContext context)
            throws JobExecutionException {

        groupKey = context.getMergedJobDataMap().getString(GROUP_KEY_JOBDETAIL_KEY);
        action = (ProvisionAction) context.getMergedJobDataMap().get(ACTION_JOBDETAIL_KEY);

        super.execute(taskKey, dryRun, context);
    }

    @Override
    protected String doExecute(final boolean dryRun, final String executor, final JobExecutionContext context)
            throws JobExecutionException {

        Group group = groupDAO.authFind(groupKey);

        StringBuilder result = new StringBuilder("Group ").append(group.getName()).append(" members ");
        if (action == ProvisionAction.DEPROVISION) {
            result.append("de");
        }
        result.append("provision\n\n");

        status.set(result.toString());

        MembershipCond membershipCond = new MembershipCond();
        membershipCond.setGroup(groupKey);
        List<User> users = searchDAO.search(SearchCond.getLeaf(membershipCond), AnyTypeKind.USER);
        Collection<String> gResources = groupDAO.findAllResourceKeys(groupKey);
        status.set("About to "
                + (action == ProvisionAction.DEPROVISION ? "de" : "") + "provision "
                + users.size() + " users from " + gResources);

        for (int i = 0; i < users.size() && !interrupt; i++) {
            List<PropagationStatus> statuses = action == ProvisionAction.DEPROVISION
                    ? userProvisioningManager.deprovision(
                            users.get(i).getKey(), gResources, false, executor, getClass().getSimpleName())
                    : userProvisioningManager.provision(
                            users.get(i).getKey(), true, null, gResources, false, executor, getClass().getSimpleName());
            for (PropagationStatus propagationStatus : statuses) {
                result.append("User ").append(users.get(i).getKey()).append('\t').
                        append("Resource ").append(propagationStatus.getResource()).append('\t').
                        append(propagationStatus.getStatus());
                if (StringUtils.isNotBlank(propagationStatus.getFailureReason())) {
                    result.append('\n').append(propagationStatus.getFailureReason()).append('\n');
                }
                result.append('\n');
            }
            result.append('\n');
        }
        if (interrupt) {
            LOG.debug("Group assignment interrupted");
            interrupted = true;
            return result.append("\n*** Group assignment interrupted ***\n").toString();
        }

        membershipCond = new MembershipCond();
        membershipCond.setGroup(groupKey);
        List<AnyObject> anyObjects = searchDAO.search(SearchCond.getLeaf(membershipCond), AnyTypeKind.ANY_OBJECT);
        status.set("About to "
                + (action == ProvisionAction.DEPROVISION ? "de" : "") + "provision "
                + anyObjects.size() + " any objects from " + gResources);

        for (int i = 0; i < anyObjects.size() && !interrupt; i++) {
            List<PropagationStatus> statuses = action == ProvisionAction.DEPROVISION
                    ? anyObjectProvisioningManager.deprovision(
                            anyObjects.get(i).getKey(), gResources, false, executor, getClass().getSimpleName())
                    : anyObjectProvisioningManager.provision(
                            anyObjects.get(i).getKey(), gResources, false, executor, getClass().getSimpleName());

            for (PropagationStatus propagationStatus : statuses) {
                result.append(anyObjects.get(i).getType().getKey()).append(' ').
                        append(anyObjects.get(i).getKey()).append('\t').
                        append("Resource ").append(propagationStatus.getResource()).append('\t').
                        append(propagationStatus.getStatus());
                if (StringUtils.isNotBlank(propagationStatus.getFailureReason())) {
                    result.append('\n').append(propagationStatus.getFailureReason()).append('\n');
                }
                result.append('\n');
            }
            result.append('\n');
        }
        if (interrupt) {
            LOG.debug("Group assignment interrupted");
            interrupted = true;
            result.append("\n*** Group assignment interrupted ***\n");
        }

        return result.toString();
    }

    @Override
    protected boolean hasToBeRegistered(final TaskExec execution) {
        // always record execution result
        return true;
    }
}

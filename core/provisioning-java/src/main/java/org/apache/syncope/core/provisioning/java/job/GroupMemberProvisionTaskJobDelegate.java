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

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.BulkMembersActionType;
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

    public static final String ACTION_TYPE_JOBDETAIL_KEY = "actionType";

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

    private BulkMembersActionType actionType;

    @Transactional
    @Override
    public void execute(final String taskKey, final boolean dryRun, final JobExecutionContext context)
            throws JobExecutionException {

        groupKey = context.getMergedJobDataMap().getString(GROUP_KEY_JOBDETAIL_KEY);
        actionType = (BulkMembersActionType) context.getMergedJobDataMap().get(ACTION_TYPE_JOBDETAIL_KEY);

        super.execute(taskKey, dryRun, context);
    }

    @Override
    protected String doExecute(final boolean dryRun) throws JobExecutionException {
        Group group = groupDAO.authFind(groupKey);

        StringBuilder result = new StringBuilder("Group ").append(group.getName()).append(" members ");
        if (actionType == BulkMembersActionType.DEPROVISION) {
            result.append("de");
        }
        result.append("provision\n\n");

        MembershipCond membershipCond = new MembershipCond();
        membershipCond.setGroup(groupKey);
        List<User> users = searchDAO.search(SearchCond.getLeafCond(membershipCond), AnyTypeKind.USER);
        for (User user : users) {
            List<PropagationStatus> statuses = actionType == BulkMembersActionType.DEPROVISION
                    ? userProvisioningManager.deprovision(user.getKey(), group.getResourceKeys(), false)
                    : userProvisioningManager.provision(user.getKey(), true, null, group.getResourceKeys(), false);
            for (PropagationStatus status : statuses) {
                result.append("User ").append(user.getKey()).append('\t').
                        append("Resource ").append(status.getResource()).append('\t').
                        append(status.getStatus());
                if (StringUtils.isNotBlank(status.getFailureReason())) {
                    result.append('\n').append(status.getFailureReason()).append('\n');
                }
                result.append("\n");
            }
            result.append("\n");
        }

        membershipCond = new MembershipCond();
        membershipCond.setGroup(groupKey);
        List<AnyObject> anyObjects = searchDAO.search(SearchCond.getLeafCond(membershipCond), AnyTypeKind.ANY_OBJECT);
        for (AnyObject anyObject : anyObjects) {
            List<PropagationStatus> statuses = actionType == BulkMembersActionType.DEPROVISION
                    ? anyObjectProvisioningManager.deprovision(anyObject.getKey(), group.getResourceKeys(), false)
                    : anyObjectProvisioningManager.provision(anyObject.getKey(), group.getResourceKeys(), false);

            for (PropagationStatus status : statuses) {
                result.append(anyObject.getType().getKey()).append(' ').append(anyObject.getKey()).append('\t').
                        append("Resource ").append(status.getResource()).append('\t').
                        append(status.getStatus());
                if (StringUtils.isNotBlank(status.getFailureReason())) {
                    result.append('\n').append(status.getFailureReason()).append('\n');
                }
                result.append("\n");
            }
            result.append("\n");
        }

        return result.toString();
    }

    @Override
    protected boolean hasToBeRegistered(final TaskExec execution) {
        // always record execution result
        return true;
    }

}

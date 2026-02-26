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
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ProvisionAction;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.search.MembershipCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.AnyObjectProvisioningManager;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.job.JobExecutionContext;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.api.job.StoppableJobDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

public class GroupMemberProvisionTaskJobDelegate
        extends AbstractSchedTaskJobDelegate<SchedTask>
        implements StoppableJobDelegate {

    public static final String ACTION_JOBDETAIL_KEY = "action";

    public static final String GROUP_KEY_JOBDETAIL_KEY = "groupKey";

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private AnySearchDAO anySearchDAO;

    @Autowired
    private UserProvisioningManager userProvisioningManager;

    @Autowired
    private AnyObjectProvisioningManager anyObjectProvisioningManager;

    private String groupKey;

    private ProvisionAction action;

    private volatile boolean stopRequested = false;

    @Override
    public void stop() {
        stopRequested = true;
    }

    @Transactional
    @Override
    public void execute(
            final TaskType taskType,
            final String taskKey,
            final JobExecutionContext context)
            throws JobExecutionException {

        groupKey = (String) context.getData().get(GROUP_KEY_JOBDETAIL_KEY);
        action = (ProvisionAction) context.getData().get(ACTION_JOBDETAIL_KEY);

        super.execute(taskType, taskKey, context);
    }

    @Override
    protected String doExecute(final JobExecutionContext context) {
        Group group = groupDAO.authFind(groupKey);

        StringBuilder result = new StringBuilder("Group ").append(group.getName()).append(" members ");
        if (action == ProvisionAction.DEPROVISION) {
            result.append("de");
        }
        result.append("provision\n\n");

        setStatus(result.toString());

        Collection<String> gResources = groupDAO.findAllResourceKeys(groupKey);

        MembershipCond membershipCond = new MembershipCond();
        membershipCond.setGroup(groupKey);
        SearchCond cond = SearchCond.of(membershipCond);

        long userCount = anySearchDAO.count(
                realmDAO.getRoot(),
                true,
                SyncopeConstants.FULL_ADMIN_REALMS,
                cond,
                AnyTypeKind.USER);

        setStatus("About to "
                + (action == ProvisionAction.DEPROVISION ? "de" : "") + "provision "
                + userCount + " users " + (action == ProvisionAction.DEPROVISION ? "from " : "to ") + gResources);

        long pages = (userCount / AnyDAO.DEFAULT_PAGE_SIZE) + 1;
        Sort sort = Sort.by(Sort.Direction.ASC, "creationDate");

        for (int page = 0; page < pages && !stopRequested; page++) {
            setStatus("Processing " + userCount + " users: page " + page + " of " + pages);

            List<User> users = anySearchDAO.search(
                    realmDAO.getRoot(),
                    true,
                    SyncopeConstants.FULL_ADMIN_REALMS,
                    cond,
                    PageRequest.of(page, AnyDAO.DEFAULT_PAGE_SIZE, sort),
                    AnyTypeKind.USER);

            for (int i = 0; i < users.size() && !stopRequested; i++) {
                User user = users.get(i);

                List<PropagationStatus> statuses = action == ProvisionAction.DEPROVISION
                        ? userProvisioningManager.deprovision(
                                user.getKey(), gResources, false, executor)
                        : userProvisioningManager.provision(
                                user.getKey(), true, null, gResources, false, executor);
                for (PropagationStatus propagationStatus : statuses) {
                    result.append("User ").append(user.getKey()).append('\t').
                            append("Resource ").append(propagationStatus.getResource()).append('\t').
                            append(propagationStatus.getStatus());
                    if (StringUtils.isNotBlank(propagationStatus.getFailureReason())) {
                        result.append('\n').append(propagationStatus.getFailureReason()).append('\n');
                    }
                    result.append('\n');
                }
                result.append('\n');
            }
        }

        if (stopRequested) {
            result.append("\nStop was requested");
            return result.toString();
        }

        long anyObjectCount = anySearchDAO.count(
                realmDAO.getRoot(),
                true,
                SyncopeConstants.FULL_ADMIN_REALMS,
                SearchCond.of(membershipCond),
                AnyTypeKind.ANY_OBJECT);
        setStatus("About to "
                + (action == ProvisionAction.DEPROVISION ? "de" : "") + "provision "
                + anyObjectCount + " any objects from " + gResources);

        pages = (anyObjectCount / AnyDAO.DEFAULT_PAGE_SIZE) + 1;

        for (int page = 0; page < pages && !stopRequested; page++) {
            setStatus("Processing " + anyObjectCount + " anyObjects: page " + page + " of " + pages);

            List<AnyObject> anyObjects = anySearchDAO.search(
                    realmDAO.getRoot(),
                    true,
                    SyncopeConstants.FULL_ADMIN_REALMS,
                    cond,
                    PageRequest.of(page, AnyDAO.DEFAULT_PAGE_SIZE, sort),
                    AnyTypeKind.ANY_OBJECT);

            for (int i = 0; i < anyObjects.size() && !stopRequested; i++) {
                AnyObject anyObject = anyObjects.get(i);

                List<PropagationStatus> statuses = action == ProvisionAction.DEPROVISION
                        ? anyObjectProvisioningManager.deprovision(
                                anyObject.getKey(), gResources, false, executor)
                        : anyObjectProvisioningManager.provision(
                                anyObject.getKey(), gResources, false, executor);

                for (PropagationStatus propagationStatus : statuses) {
                    result.append(anyObject.getType().getKey()).append(' ').
                            append(anyObject.getKey()).append('\t').
                            append("Resource ").append(propagationStatus.getResource()).append('\t').
                            append(propagationStatus.getStatus());
                    if (StringUtils.isNotBlank(propagationStatus.getFailureReason())) {
                        result.append('\n').append(propagationStatus.getFailureReason()).append('\n');
                    }
                    result.append('\n');
                }
                result.append('\n');
            }
        }

        if (stopRequested) {
            result.append("\nStop was requested");
        }

        return result.toString();
    }

    @Override
    protected boolean hasToBeRegistered(final TaskExec<?> execution) {
        // always record execution result
        return true;
    }
}

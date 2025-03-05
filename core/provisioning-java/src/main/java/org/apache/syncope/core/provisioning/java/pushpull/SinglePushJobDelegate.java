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
package org.apache.syncope.core.provisioning.java.pushpull;

import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.PushActions;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePushResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopeSinglePushExecutor;
import org.apache.syncope.core.provisioning.api.pushpull.UserPushResultHandler;
import org.springframework.beans.factory.annotation.Autowired;

public class SinglePushJobDelegate extends PushJobDelegate implements SyncopeSinglePushExecutor {

    @Autowired
    protected ImplementationDAO implementationDAO;

    protected void before(
            final ExternalResource resource,
            final Connector connector,
            final PushTaskTO pushTaskTO,
            final String executor) {

        LOG.debug("Executing push on {}", resource);

        taskType = TaskType.PUSH;

        task = entityFactory.newEntity(PushTask.class);
        task.setResource(resource);
        task.setMatchingRule(pushTaskTO.getMatchingRule() == null
                ? MatchingRule.LINK : pushTaskTO.getMatchingRule());
        task.setUnmatchingRule(pushTaskTO.getUnmatchingRule() == null
                ? UnmatchingRule.ASSIGN : pushTaskTO.getUnmatchingRule());
        task.setPerformCreate(pushTaskTO.isPerformCreate());
        task.setPerformUpdate(pushTaskTO.isPerformUpdate());
        task.setPerformDelete(pushTaskTO.isPerformDelete());
        task.setSyncStatus(pushTaskTO.isSyncStatus());

        profile = new ProvisioningProfile<>(
                connector,
                taskType,
                task,
                ConflictResolutionAction.FIRSTMATCH,
                getPushActions(pushTaskTO.getActions().stream().
                        map(implementationDAO::findById).flatMap(Optional::stream).
                        toList()),
                executor,
                false);

        for (PushActions action : profile.getActions()) {
            action.beforeAll(profile);
        }
    }

    @Override
    public List<ProvisioningReport> push(
            final ExternalResource resource,
            final Provision provision,
            final Connector connector,
            final Any any,
            final PushTaskTO pushTaskTO,
            final String executor) throws JobExecutionException {

        try {
            before(resource, connector, pushTaskTO, executor);
            dispatcher = new PushResultHandlerDispatcher(profile, this);

            AnyType anyType = anyTypeDAO.findById(provision.getAnyType()).
                    orElseThrow(() -> new NotFoundException("AnyType" + provision.getAnyType()));

            dispatcher.addHandlerSupplier(provision.getAnyType(), () -> {
                SyncopePushResultHandler handler;
                switch (anyType.getKind()) {
                    case USER:
                        handler = buildUserHandler();
                        break;

                    case GROUP:
                        handler = buildGroupHandler();
                        break;

                    case ANY_OBJECT:
                    default:
                        handler = buildAnyObjectHandler();
                }
                handler.setProfile(profile);
                return handler;
            });

            doHandle(List.of(any), dispatcher, resource);

            for (PushActions action : profile.getActions()) {
                action.afterAll(profile);
            }

            return profile.getResults();
        } catch (Exception e) {
            throw e instanceof final JobExecutionException jobExecutionException
                    ? jobExecutionException
                    : new JobExecutionException("While pushing to connector", e);
        }
    }

    @Override
    public ProvisioningReport push(
            final ExternalResource resource,
            final Provision provision,
            final Connector connector,
            final LinkedAccount account,
            final PushTaskTO pushTaskTO,
            final String executor) throws JobExecutionException {

        try {
            before(resource, connector, pushTaskTO, executor);

            UserPushResultHandler handler = buildUserHandler();
            handler.setProfile(profile);

            handler.handle(account, provision);

            for (PushActions action : profile.getActions()) {
                action.afterAll(profile);
            }

            return profile.getResults().getFirst();
        } catch (Exception e) {
            throw e instanceof final JobExecutionException jobExecutionException
                    ? jobExecutionException
                    : new JobExecutionException("While pushing to connector", e);
        }
    }
}

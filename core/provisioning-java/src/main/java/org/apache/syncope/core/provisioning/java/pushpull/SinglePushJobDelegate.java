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

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.core.provisioning.api.pushpull.PushActions;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePushResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopeSinglePushExecutor;
import org.apache.syncope.core.provisioning.api.pushpull.UserPushResultHandler;
import org.apache.syncope.core.spring.ImplementationManager;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

public class SinglePushJobDelegate extends PushJobDelegate implements SyncopeSinglePushExecutor {

    @Autowired
    private ImplementationDAO implementationDAO;

    private List<PushActions> before(
            final Provision provision,
            final Connector connector,
            final PushTaskTO pushTaskTO,
            final String executor) throws JobExecutionException {

        LOG.debug("Executing push on {}", provision.getResource());

        List<PushActions> actions = new ArrayList<>();
        pushTaskTO.getActions().forEach(key -> {
            Implementation impl = implementationDAO.find(key);
            if (impl == null || !IdMImplementationType.PUSH_ACTIONS.equals(impl.getType())) {
                LOG.debug("Invalid " + Implementation.class.getSimpleName() + " {}, ignoring...", key);
            } else {
                try {
                    actions.add(ImplementationManager.build(impl));
                } catch (Exception e) {
                    LOG.warn("While building {}", impl, e);
                }
            }
        });

        PushTask pushTask = entityFactory.newEntity(PushTask.class);
        pushTask.setResource(provision.getResource());
        pushTask.setMatchingRule(pushTaskTO.getMatchingRule() == null
                ? MatchingRule.LINK : pushTaskTO.getMatchingRule());
        pushTask.setUnmatchingRule(pushTaskTO.getUnmatchingRule() == null
                ? UnmatchingRule.ASSIGN : pushTaskTO.getUnmatchingRule());
        pushTask.setPerformCreate(pushTaskTO.isPerformCreate());
        pushTask.setPerformUpdate(pushTaskTO.isPerformUpdate());
        pushTask.setPerformDelete(pushTaskTO.isPerformDelete());
        pushTask.setSyncStatus(pushTaskTO.isSyncStatus());

        profile = new ProvisioningProfile<>(connector, pushTask);
        profile.setExecutor(executor);
        profile.getActions().addAll(actions);
        profile.setConflictResolutionAction(ConflictResolutionAction.FIRSTMATCH);

        for (PushActions action : actions) {
            action.beforeAll(profile);
        }

        return actions;
    }

    @Override
    public List<ProvisioningReport> push(
            final Provision provision,
            final Connector connector,
            final Any<?> any,
            final PushTaskTO pushTaskTO,
            final String executor) throws JobExecutionException {

        try {
            List<PushActions> actions = before(provision, connector, pushTaskTO, executor);

            SyncopePushResultHandler handler;
            switch (provision.getAnyType().getKind()) {
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

            doHandle(List.of(any), handler, provision.getResource());

            for (PushActions action : actions) {
                action.afterAll(profile);
            }

            return profile.getResults();
        } catch (Exception e) {
            throw e instanceof JobExecutionException
                    ? (JobExecutionException) e
                    : new JobExecutionException("While pushing to connector", e);
        }
    }

    @Override
    public ProvisioningReport push(
            final Provision provision,
            final Connector connector,
            final LinkedAccount account,
            final PushTaskTO pushTaskTO,
            final String executor) throws JobExecutionException {

        try {
            List<PushActions> actions = before(provision, connector, pushTaskTO, executor);

            UserPushResultHandler handler = buildUserHandler();
            handler.setProfile(profile);

            handler.handle(account, provision);

            for (PushActions action : actions) {
                action.afterAll(profile);
            }

            return profile.getResults().get(0);
        } catch (Exception e) {
            throw e instanceof JobExecutionException
                    ? (JobExecutionException) e
                    : new JobExecutionException("While pushing to connector", e);
        }
    }
}

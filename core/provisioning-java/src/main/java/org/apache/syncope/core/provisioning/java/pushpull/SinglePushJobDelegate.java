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
import java.util.Arrays;
import java.util.List;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningReport;
import org.apache.syncope.core.provisioning.api.pushpull.PushActions;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePushResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopeSinglePushExecutor;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.stereotype.Component;

@Component
public class SinglePushJobDelegate extends PushJobDelegate implements SyncopeSinglePushExecutor {

    @Override
    public List<ProvisioningReport> push(
            final Provision provision,
            final Connector connector,
            final Any<?> any,
            final PushTaskTO pushTaskTO) throws JobExecutionException {

        LOG.debug("Executing push on {}", provision.getResource());

        List<PushActions> actions = new ArrayList<>();
        for (String className : pushTaskTO.getActionsClassNames()) {
            try {
                Class<?> actionsClass = Class.forName(className);

                PushActions pushActions = (PushActions) ApplicationContextProvider.getBeanFactory().
                        createBean(actionsClass, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, true);
                actions.add(pushActions);
            } catch (Exception e) {
                LOG.info("Class '{}' not found", className, e);
            }
        }

        try {
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
            profile.getActions().addAll(actions);
            profile.setResAct(null);

            for (PushActions action : actions) {
                action.beforeAll(profile);
            }

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

            doHandle(Arrays.asList(any), handler, pushTask.getResource());

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
}

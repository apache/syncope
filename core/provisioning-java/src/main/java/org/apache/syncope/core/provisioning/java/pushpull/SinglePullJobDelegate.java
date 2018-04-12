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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.collections.IteratorChain;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.syncope.common.lib.types.ImplementationType;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.pushpull.GroupPullResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningReport;
import org.apache.syncope.core.provisioning.api.pushpull.PullActions;
import org.apache.syncope.core.provisioning.api.pushpull.ReconFilterBuilder;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePullResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopeSinglePullExecutor;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.apache.syncope.core.spring.ImplementationManager;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SinglePullJobDelegate extends PullJobDelegate implements SyncopeSinglePullExecutor {

    @Autowired
    private ImplementationDAO implementationDAO;

    @Override
    public List<ProvisioningReport> pull(
            final Provision provision,
            final Connector connector,
            final String connObjectKey,
            final String connObjectValue,
            final Realm realm,
            final boolean remediation,
            final List<String> actionKeys) throws JobExecutionException {

        LOG.debug("Executing pull on {}", provision.getResource());

        List<PullActions> actions = new ArrayList<>();
        actionKeys.forEach(key -> {
            Implementation impl = implementationDAO.find(key);
            if (impl == null || impl.getType() != ImplementationType.PULL_ACTIONS) {
                LOG.debug("Invalid " + Implementation.class.getSimpleName() + " {}, ignoring...", key);
            } else {
                try {
                    actions.add(ImplementationManager.build(impl));
                } catch (Exception e) {
                    LOG.warn("While building {}", impl, e);
                }
            }
        });

        try {
            Set<MappingItem> linkinMappingItems = virSchemaDAO.findByProvision(provision).stream().
                    map(virSchema -> virSchema.asLinkingMappingItem()).collect(Collectors.toSet());
            Iterator<MappingItem> mapItems = new IteratorChain<>(
                    provision.getMapping().getItems().iterator(),
                    linkinMappingItems.iterator());
            OperationOptions options = MappingUtils.buildOperationOptions(mapItems);

            PullTask pullTask = entityFactory.newEntity(PullTask.class);
            pullTask.setResource(provision.getResource());
            pullTask.setMatchingRule(MatchingRule.UPDATE);
            pullTask.setUnmatchingRule(UnmatchingRule.PROVISION);
            pullTask.setPullMode(PullMode.FILTERED_RECONCILIATION);
            pullTask.setPerformCreate(true);
            pullTask.setPerformUpdate(true);
            pullTask.setRemediation(remediation);
            pullTask.setDestinationRealm(realm);

            profile = new ProvisioningProfile<>(connector, pullTask);
            profile.setDryRun(false);
            profile.setResAct(ConflictResolutionAction.FIRSTMATCH);
            profile.getActions().addAll(actions);

            for (PullActions action : actions) {
                action.beforeAll(profile);
            }

            SyncopePullResultHandler handler;
            GroupPullResultHandler ghandler = buildGroupHandler();
            switch (provision.getAnyType().getKind()) {
                case USER:
                    handler = buildUserHandler();
                    break;

                case GROUP:
                    handler = ghandler;
                    break;

                case ANY_OBJECT:
                default:
                    handler = buildAnyObjectHandler();
            }
            handler.setProfile(profile);
            handler.setPullExecutor(this);

            // execute filtered pull
            connector.filteredReconciliation(
                    provision.getObjectClass(),
                    new AccountReconciliationFilterBuilder(connObjectKey, connObjectValue),
                    handler,
                    options);

            try {
                setGroupOwners(ghandler);
            } catch (Exception e) {
                LOG.error("While setting group owners", e);
            }

            for (PullActions action : actions) {
                action.afterAll(profile);
            }

            return profile.getResults();
        } catch (Exception e) {
            throw e instanceof JobExecutionException
                    ? (JobExecutionException) e
                    : new JobExecutionException("While pulling from connector", e);
        }
    }

    class AccountReconciliationFilterBuilder implements ReconFilterBuilder {

        private final String key;

        private final String value;

        AccountReconciliationFilterBuilder(final String key, final String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public Filter build() {
            return FilterBuilder.equalTo(AttributeBuilder.build(key, value));
        }
    }
}

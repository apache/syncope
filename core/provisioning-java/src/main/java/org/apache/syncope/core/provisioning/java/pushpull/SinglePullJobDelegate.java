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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.task.AnyTemplatePullTask;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.pushpull.GroupPullResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningReport;
import org.apache.syncope.core.provisioning.api.pushpull.PullActions;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePullResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopeSinglePullExecutor;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.apache.syncope.core.provisioning.java.utils.TemplateUtils;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.stereotype.Component;

@Component
public class SinglePullJobDelegate extends PullJobDelegate implements SyncopeSinglePullExecutor {

    @Autowired
    private TemplateUtils templateUtils;

    @Override
    public List<ProvisioningReport> pull(
            final Provision provision,
            final Connector connector,
            final String connObjectKey,
            final String connObjectValue,
            final Realm realm,
            final PullTaskTO pullTaskTO) throws JobExecutionException {

        LOG.debug("Executing pull on {}", provision.getResource());

        List<PullActions> actions = new ArrayList<>();
        for (String className : pullTaskTO.getActionsClassNames()) {
            try {
                Class<?> actionsClass = Class.forName(className);
                PullActions pullActions = (PullActions) ApplicationContextProvider.getBeanFactory().
                        createBean(actionsClass, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, true);

                actions.add(pullActions);
            } catch (Exception e) {
                LOG.warn("Class '{}' not found", className, e);
            }
        }

        try {
            Set<MappingItem> linkinMappingItems = new HashSet<>();
            for (VirSchema virSchema : virSchemaDAO.findByProvision(provision)) {
                linkinMappingItems.add(virSchema.asLinkingMappingItem());
            }
            Iterator<MappingItem> mapItems = IteratorUtils.chainedIterator(
                    provision.getMapping().getItems().iterator(),
                    linkinMappingItems.iterator());
            OperationOptions options = MappingUtils.buildOperationOptions(mapItems);

            PullTask pullTask = entityFactory.newEntity(PullTask.class);
            pullTask.setResource(provision.getResource());
            pullTask.setMatchingRule(pullTaskTO.getMatchingRule() == null
                    ? MatchingRule.UPDATE : pullTaskTO.getMatchingRule());
            pullTask.setUnmatchingRule(pullTaskTO.getUnmatchingRule() == null
                    ? UnmatchingRule.PROVISION : pullTaskTO.getUnmatchingRule());
            pullTask.setPullMode(PullMode.FILTERED_RECONCILIATION);
            pullTask.setPerformCreate(pullTaskTO.isPerformCreate());
            pullTask.setPerformUpdate(pullTaskTO.isPerformUpdate());
            pullTask.setPerformDelete(pullTaskTO.isPerformDelete());
            pullTask.setSyncStatus(pullTaskTO.isSyncStatus());
            pullTask.setDestinationRealm(realm);
            // validate JEXL expressions from templates and proceed if fine
            templateUtils.check(pullTaskTO.getTemplates(), ClientExceptionType.InvalidPullTask);
            for (Map.Entry<String, AnyTO> entry : pullTaskTO.getTemplates().entrySet()) {
                AnyType type = anyTypeDAO.find(entry.getKey());
                if (type == null) {
                    LOG.debug("Invalid AnyType {} specified, ignoring...", entry.getKey());
                } else {
                    AnyTemplatePullTask anyTemplate = pullTask.getTemplate(type);
                    if (anyTemplate == null) {
                        anyTemplate = entityFactory.newEntity(AnyTemplatePullTask.class);
                        anyTemplate.setAnyType(type);
                        anyTemplate.setPullTask(pullTask);

                        pullTask.add(anyTemplate);
                    }
                    anyTemplate.set(entry.getValue());
                }
            }

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

    class AccountReconciliationFilterBuilder extends DefaultReconciliationFilterBuilder {

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

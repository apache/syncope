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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.task.AnyTemplatePullTask;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.pushpull.GroupPullResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.PullActions;
import org.apache.syncope.core.provisioning.api.pushpull.ReconFilterBuilder;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePullResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopeSinglePullExecutor;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.apache.syncope.core.provisioning.java.utils.TemplateUtils;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

public class SinglePullJobDelegate extends PullJobDelegate implements SyncopeSinglePullExecutor {

    @Autowired
    protected ImplementationDAO implementationDAO;

    @Autowired
    protected RealmDAO realmDAO;

    @Override
    public List<ProvisioningReport> pull(
            final ExternalResource resource,
            final Provision provision,
            final Connector connector,
            final ReconFilterBuilder reconFilterBuilder,
            final Set<String> moreAttrsToGet,
            final PullTaskTO pullTaskTO,
            final String executor) throws JobExecutionException {

        LOG.debug("Executing pull on {}", resource);

        taskType = TaskType.PULL;
        try {
            task = entityFactory.newEntity(PullTask.class);
            task.setResource(resource);
            task.setMatchingRule(pullTaskTO.getMatchingRule() == null
                    ? MatchingRule.UPDATE : pullTaskTO.getMatchingRule());
            task.setUnmatchingRule(pullTaskTO.getUnmatchingRule() == null
                    ? UnmatchingRule.PROVISION : pullTaskTO.getUnmatchingRule());
            task.setPullMode(PullMode.FILTERED_RECONCILIATION);
            task.setPerformCreate(pullTaskTO.isPerformCreate());
            task.setPerformUpdate(pullTaskTO.isPerformUpdate());
            task.setPerformDelete(pullTaskTO.isPerformDelete());
            task.setSyncStatus(pullTaskTO.isSyncStatus());
            task.setDestinationRealm(realmDAO.findByFullPath(pullTaskTO.getDestinationRealm()));
            task.setRemediation(pullTaskTO.isRemediation());

            // validate JEXL expressions from templates and proceed if fine
            TemplateUtils.check(pullTaskTO.getTemplates(), ClientExceptionType.InvalidPullTask);
            pullTaskTO.getTemplates().forEach((type, template) -> {
                AnyType anyType = anyTypeDAO.find(type);
                if (anyType == null) {
                    LOG.debug("Invalid AnyType {} specified, ignoring...", type);
                } else {
                    AnyTemplatePullTask anyTemplate = task.getTemplate(anyType.getKey()).orElse(null);
                    if (anyTemplate == null) {
                        anyTemplate = entityFactory.newEntity(AnyTemplatePullTask.class);
                        anyTemplate.setAnyType(anyType);
                        anyTemplate.setPullTask(task);

                        task.add(anyTemplate);
                    }
                    anyTemplate.set(template);
                }
            });

            profile = new ProvisioningProfile<>(connector, task);
            profile.setDryRun(false);
            profile.setConflictResolutionAction(ConflictResolutionAction.FIRSTMATCH);
            profile.getActions().addAll(getPullActions(pullTaskTO.getActions().stream().
                    map(implementationDAO::find).filter(Objects::nonNull).collect(Collectors.toList())));
            profile.setExecutor(executor);

            for (PullActions action : profile.getActions()) {
                action.beforeAll(profile);
            }

            AnyType anyType = anyTypeDAO.find(provision.getAnyType());

            SyncopePullResultHandler handler;
            GroupPullResultHandler ghandler = buildGroupHandler();
            switch (anyType.getKind()) {
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
            Set<String> matg = new HashSet<>(moreAttrsToGet);
            profile.getActions().forEach(a -> matg.addAll(a.moreAttrsToGet(profile, provision)));

            Stream<Item> mapItems = Stream.concat(
                    MappingUtils.getPullItems(provision.getMapping().getItems().stream()),
                    virSchemaDAO.find(task.getResource().getKey(), anyType.getKey()).stream().
                            map(VirSchema::asLinkingMappingItem));

            connector.filteredReconciliation(
                    new ObjectClass(provision.getObjectClass()),
                    reconFilterBuilder,
                    handler,
                    MappingUtils.buildOperationOptions(mapItems, matg.toArray(String[]::new)));

            try {
                setGroupOwners(ghandler);
            } catch (Exception e) {
                LOG.error("While setting group owners", e);
            }

            for (PullActions action : profile.getActions()) {
                action.afterAll(profile);
            }

            return profile.getResults();
        } catch (Exception e) {
            throw e instanceof JobExecutionException
                    ? (JobExecutionException) e
                    : new JobExecutionException("While pulling from connector", e);
        } finally {
            setStatus(null);
        }
    }
}

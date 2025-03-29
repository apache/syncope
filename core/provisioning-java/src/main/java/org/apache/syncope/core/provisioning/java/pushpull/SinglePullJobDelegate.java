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
import java.util.Optional;
import java.util.Set;
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
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.task.AnyTemplatePullTask;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.api.pushpull.InboundActions;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.ReconFilterBuilder;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePullResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopeSinglePullExecutor;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.apache.syncope.core.provisioning.java.utils.TemplateUtils;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.springframework.beans.factory.annotation.Autowired;

public class SinglePullJobDelegate extends PullJobDelegate implements SyncopeSinglePullExecutor {

    @Autowired
    protected ImplementationDAO implementationDAO;

    @Autowired
    protected RealmSearchDAO realmSearchDAO;

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
            task.setName(pullTaskTO.getName());
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
            task.setDestinationRealm(realmSearchDAO.findByFullPath(pullTaskTO.getDestinationRealm()).
                    orElseThrow(() -> new NotFoundException("Realm " + pullTaskTO.getDestinationRealm())));
            task.setRemediation(pullTaskTO.isRemediation());

            // validate JEXL expressions from templates and proceed if fine
            TemplateUtils.check(pullTaskTO.getTemplates(), ClientExceptionType.InvalidPullTask);
            pullTaskTO.getTemplates().forEach((type, template) -> anyTypeDAO.findById(type).ifPresentOrElse(
                    anyType -> {
                        AnyTemplatePullTask anyTemplate = task.getTemplate(anyType.getKey()).orElse(null);
                        if (anyTemplate == null) {
                            anyTemplate = entityFactory.newEntity(AnyTemplatePullTask.class);
                            anyTemplate.setAnyType(anyType);
                            anyTemplate.setPullTask(task);

                            task.add(anyTemplate);
                        }
                        anyTemplate.set(template);
                    },
                    () -> LOG.debug("Invalid AnyType {} specified, ignoring...", type)));

            profile = new ProvisioningProfile<>(
                    connector,
                    taskType,
                    task,
                    ConflictResolutionAction.FIRSTMATCH,
                    getInboundActions(pullTaskTO.getActions().stream().
                            map(implementationDAO::findById).flatMap(Optional::stream).
                            toList()),
                    executor,
                    false);

            dispatcher = new PullResultHandlerDispatcher(profile, this);

            for (InboundActions action : profile.getActions()) {
                action.beforeAll(profile);
            }

            AnyType anyType = anyTypeDAO.findById(provision.getAnyType()).
                    orElseThrow(() -> new NotFoundException("AnyType" + provision.getAnyType()));

            dispatcher.addHandlerSupplier(provision.getObjectClass(), () -> {
                SyncopePullResultHandler handler;
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
                return handler;
            });

            // execute filtered pull
            Set<String> matg = new HashSet<>(moreAttrsToGet);
            profile.getActions().forEach(a -> matg.addAll(a.moreAttrsToGet(profile, provision)));

            Stream<Item> mapItems = Stream.concat(
                    MappingUtils.getInboundItems(provision.getMapping().getItems().stream()),
                    virSchemaDAO.findByResourceAndAnyType(task.getResource().getKey(), anyType.getKey()).stream().
                            map(VirSchema::asLinkingMappingItem));

            connector.filteredReconciliation(
                    new ObjectClass(provision.getObjectClass()),
                    reconFilterBuilder,
                    dispatcher,
                    MappingUtils.buildOperationOptions(mapItems, matg.toArray(String[]::new)));

            try {
                setGroupOwners();
            } catch (Exception e) {
                LOG.error("While setting group owners", e);
            }

            for (InboundActions action : profile.getActions()) {
                action.afterAll(profile);
            }

            return profile.getResults();
        } catch (Exception e) {
            throw e instanceof final JobExecutionException jobExecutionException
                    ? jobExecutionException
                    : new JobExecutionException("While pulling from connector", e);
        }
    }
}

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
package org.apache.syncope.core.provisioning.java.pushpull.stream;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.Mapping;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.policy.PullCorrelationRuleEntity;
import org.apache.syncope.core.persistence.api.entity.policy.PullPolicy;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.pushpull.GroupPullResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.PullActions;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePullResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.stream.SyncopeStreamPullExecutor;
import org.apache.syncope.core.provisioning.java.pushpull.PullJobDelegate;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

public class StreamPullJobDelegate extends PullJobDelegate implements SyncopeStreamPullExecutor {

    @Autowired
    private ImplementationDAO implementationDAO;

    @Autowired
    private RealmDAO realmDAO;

    private PullPolicy pullPolicy(
            final AnyType anyType,
            final ConflictResolutionAction conflictResolutionAction,
            final String pullCorrelationRule) {

        PullCorrelationRuleEntity pullCorrelationRuleEntity = null;
        if (pullCorrelationRule != null) {
            Implementation impl = implementationDAO.find(pullCorrelationRule);
            if (impl == null || !IdMImplementationType.PULL_CORRELATION_RULE.equals(impl.getType())) {
                LOG.debug("Invalid " + Implementation.class.getSimpleName() + " {}, ignoring...", pullCorrelationRule);
            } else {
                pullCorrelationRuleEntity = entityFactory.newEntity(PullCorrelationRuleEntity.class);
                pullCorrelationRuleEntity.setAnyType(anyType);
                pullCorrelationRuleEntity.setImplementation(impl);
            }
        }

        PullPolicy pullPolicy = entityFactory.newEntity(PullPolicy.class);
        pullPolicy.setConflictResolutionAction(conflictResolutionAction);

        if (pullCorrelationRuleEntity != null) {
            pullPolicy.add(pullCorrelationRuleEntity);
            pullCorrelationRuleEntity.setPullPolicy(pullPolicy);
        }

        return pullPolicy;
    }

    private Provision provision(
            final AnyType anyType,
            final String keyColumn,
            final List<String> columns) throws JobExecutionException {

        Provision provision = new Provision();
        provision.setAnyType(anyType.getKey());
        provision.setObjectClass(anyType.getKey());

        Mapping mapping = new Mapping();
        provision.setMapping(mapping);

        AnyUtils anyUtils = anyUtilsFactory.getInstance(anyType.getKind());
        if (anyUtils.getField(keyColumn) == null) {
            PlainSchema keyColumnSchema = plainSchemaDAO.find(keyColumn);
            if (keyColumnSchema == null) {
                throw new JobExecutionException("Plain Schema for key column not found: " + keyColumn);
            }
        }

        Item connObjectKeyItem = new Item();
        connObjectKeyItem.setExtAttrName(keyColumn);
        connObjectKeyItem.setIntAttrName(keyColumn);
        connObjectKeyItem.setPurpose(MappingPurpose.PULL);
        mapping.setConnObjectKeyItem(connObjectKeyItem);

        columns.stream().
                filter(column -> anyUtils.getField(column) != null
                || plainSchemaDAO.find(column) != null || virSchemaDAO.find(column) != null).
                map(column -> {
                    Item item = new Item();
                    item.setExtAttrName(column);
                    item.setIntAttrName(column);
                    item.setPurpose(MappingPurpose.PULL);
                    mapping.add(item);
                    return item;
                }).forEach(mapping::add);

        return provision;
    }

    private ExternalResource externalResource(
            final AnyType anyType,
            final String keyColumn,
            final List<String> columns,
            final ConflictResolutionAction conflictResolutionAction,
            final String pullCorrelationRule) throws JobExecutionException {

        Provision provision = provision(anyType, keyColumn, columns);

        ExternalResource resource = entityFactory.newEntity(ExternalResource.class);
        resource.setKey("StreamPull_" + SecureRandomUtils.generateRandomUUID().toString());
        resource.getProvisions().add(provision);

        resource.setPullPolicy(pullPolicy(anyType, conflictResolutionAction, pullCorrelationRule));

        return resource;
    }

    @Override
    public List<ProvisioningReport> pull(
            final AnyType anyType,
            final String keyColumn,
            final List<String> columns,
            final ConflictResolutionAction conflictResolutionAction,
            final String pullCorrelationRule,
            final Connector connector,
            final PullTaskTO pullTaskTO) throws JobExecutionException {

        LOG.debug("Executing stream pull");

        taskType = TaskType.PULL;
        try {
            ExternalResource resource =
                    externalResource(anyType, keyColumn, columns, conflictResolutionAction, pullCorrelationRule);
            Provision provision = resource.getProvisions().get(0);

            task = entityFactory.newEntity(PullTask.class);
            task.setResource(resource);
            task.setMatchingRule(pullTaskTO.getMatchingRule());
            task.setUnmatchingRule(pullTaskTO.getUnmatchingRule());
            task.setPullMode(PullMode.FULL_RECONCILIATION);
            task.setPerformCreate(true);
            task.setPerformUpdate(true);
            task.setPerformDelete(false);
            task.setSyncStatus(false);
            task.setDestinationRealm(realmDAO.findByFullPath(pullTaskTO.getDestinationRealm()));
            task.setRemediation(pullTaskTO.isRemediation());

            profile = new ProvisioningProfile<>(connector, task);
            profile.setDryRun(false);
            profile.setConflictResolutionAction(conflictResolutionAction);
            profile.getActions().addAll(getPullActions(pullTaskTO.getActions().stream().
                    map(implementationDAO::find).filter(Objects::nonNull).collect(Collectors.toList())));

            for (PullActions action : profile.getActions()) {
                action.beforeAll(profile);
            }

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
            Set<String> moreAttrsToGet = new HashSet<>();
            profile.getActions().forEach(a -> moreAttrsToGet.addAll(a.moreAttrsToGet(profile, provision)));

            Stream<Item> mapItems = Stream.concat(
                    MappingUtils.getPullItems(provision.getMapping().getItems().stream()),
                    virSchemaDAO.find(resource.getKey(), anyType.getKey()).stream().
                            map(VirSchema::asLinkingMappingItem));

            connector.fullReconciliation(
                    new ObjectClass(provision.getObjectClass()),
                    handler,
                    MappingUtils.buildOperationOptions(mapItems, moreAttrsToGet.toArray(String[]::new)));

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
                    : new JobExecutionException("While stream pulling", e);
        } finally {
            setStatus(null);
        }
    }
}

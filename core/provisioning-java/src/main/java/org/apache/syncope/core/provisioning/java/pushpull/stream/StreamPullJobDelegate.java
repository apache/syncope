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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.policy.PullCorrelationRuleEntity;
import org.apache.syncope.core.persistence.api.entity.policy.PullPolicy;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.Item;
import org.apache.syncope.core.persistence.api.entity.resource.Mapping;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.provisioning.api.pushpull.GroupPullResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.pushpull.PullActions;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePullResultHandler;
import org.apache.syncope.core.spring.ImplementationManager;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.syncope.core.provisioning.api.pushpull.stream.SyncopeStreamPullExecutor;
import org.apache.syncope.core.provisioning.java.pushpull.PullJobDelegate;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.identityconnectors.framework.common.objects.ObjectClass;

public class StreamPullJobDelegate extends PullJobDelegate implements SyncopeStreamPullExecutor {

    @Autowired
    private ImplementationDAO implementationDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

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

        Provision provision = entityFactory.newEntity(Provision.class);
        provision.setAnyType(anyType);
        provision.setObjectClass(new ObjectClass(anyType.getKey()));

        Mapping mapping = entityFactory.newEntity(Mapping.class);
        provision.setMapping(mapping);
        mapping.setProvision(provision);

        AnyUtils anyUtils = anyUtilsFactory.getInstance(anyType.getKind());
        if (anyUtils.getField(keyColumn) == null) {
            PlainSchema keyColumnSchema = plainSchemaDAO.find(keyColumn);
            if (keyColumnSchema == null) {
                throw new JobExecutionException("Plain Schema for key column not found: " + keyColumn);
            }
        }

        MappingItem connObjectKeyItem = entityFactory.newEntity(MappingItem.class);
        connObjectKeyItem.setExtAttrName(keyColumn);
        connObjectKeyItem.setIntAttrName(keyColumn);
        connObjectKeyItem.setPurpose(MappingPurpose.PULL);
        mapping.setConnObjectKeyItem(connObjectKeyItem);

        columns.stream().
                filter(column -> anyUtils.getField(column) != null
                || plainSchemaDAO.find(column) != null || virSchemaDAO.find(column) != null).
                map(column -> {
                    MappingItem item = entityFactory.newEntity(MappingItem.class);
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
        resource.add(provision);
        provision.setResource(resource);

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

        List<PullActions> actions = new ArrayList<>();
        pullTaskTO.getActions().forEach(key -> {
            Implementation impl = implementationDAO.find(key);
            if (impl == null || !IdMImplementationType.PULL_ACTIONS.equals(impl.getType())) {
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
            ExternalResource resource =
                    externalResource(anyType, keyColumn, columns, conflictResolutionAction, pullCorrelationRule);
            Provision provision = resource.getProvisions().get(0);

            PullTask pullTask = entityFactory.newEntity(PullTask.class);
            pullTask.setResource(resource);
            pullTask.setMatchingRule(pullTaskTO.getMatchingRule());
            pullTask.setUnmatchingRule(pullTaskTO.getUnmatchingRule());
            pullTask.setPullMode(PullMode.FULL_RECONCILIATION);
            pullTask.setPerformCreate(true);
            pullTask.setPerformUpdate(true);
            pullTask.setPerformDelete(false);
            pullTask.setSyncStatus(false);
            pullTask.setDestinationRealm(realmDAO.findByFullPath(pullTaskTO.getDestinationRealm()));
            pullTask.setRemediation(pullTaskTO.isRemediation());

            profile = new ProvisioningProfile<>(connector, pullTask);
            profile.setDryRun(false);
            profile.setConflictResolutionAction(conflictResolutionAction);
            profile.getActions().addAll(actions);

            for (PullActions action : actions) {
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
            actions.forEach(action -> moreAttrsToGet.addAll(action.moreAttrsToGet(profile, provision)));

            Stream<? extends Item> mapItems = Stream.concat(
                    MappingUtils.getPullItems(provision.getMapping().getItems().stream()),
                    virSchemaDAO.findByProvision(provision).stream().map(VirSchema::asLinkingMappingItem));

            connector.fullReconciliation(
                    provision.getObjectClass(),
                    handler,
                    MappingUtils.buildOperationOptions(mapItems, moreAttrsToGet.toArray(new String[0])));

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
                    : new JobExecutionException("While stream pulling", e);
        }
    }
}

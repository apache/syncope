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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.Mapping;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.pushpull.AnyObjectPushResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.GroupPushResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.PushActions;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePushResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.UserPushResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.stream.SyncopeStreamPushExecutor;
import org.apache.syncope.core.provisioning.java.pushpull.PushJobDelegate;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;

public class StreamPushJobDelegate extends PushJobDelegate implements SyncopeStreamPushExecutor {

    @Autowired
    private ImplementationDAO implementationDAO;

    @Override
    protected AnyObjectPushResultHandler buildAnyObjectHandler() {
        return (AnyObjectPushResultHandler) ApplicationContextProvider.getBeanFactory().createBean(
                StreamAnyObjectPushResultHandler.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
    }

    @Override
    protected UserPushResultHandler buildUserHandler() {
        return (UserPushResultHandler) ApplicationContextProvider.getBeanFactory().
                createBean(StreamUserPushResultHandler.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
    }

    @Override
    protected GroupPushResultHandler buildGroupHandler() {
        return (GroupPushResultHandler) ApplicationContextProvider.getBeanFactory().
                createBean(StreamGroupPushResultHandler.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);
    }

    private ExternalResource externalResource(
            final AnyType anyType,
            final List<String> columns,
            final List<String> propagationActions) throws JobExecutionException {

        Provision provision = new Provision();
        provision.setAnyType(anyType.getKey());
        provision.setObjectClass(anyType.getKey());

        Mapping mapping = new Mapping();
        provision.setMapping(mapping);

        Item connObjectKeyItem = new Item();
        connObjectKeyItem.setExtAttrName("key");
        connObjectKeyItem.setIntAttrName("key");
        connObjectKeyItem.setPurpose(MappingPurpose.NONE);
        mapping.setConnObjectKeyItem(connObjectKeyItem);

        columns.stream().map(column -> {
            Item item = new Item();
            item.setExtAttrName(column);
            item.setIntAttrName(column);
            item.setPurpose(MappingPurpose.PROPAGATION);
            mapping.add(item);
            return item;
        }).forEach(mapping::add);

        ExternalResource resource = entityFactory.newEntity(ExternalResource.class);
        resource.setKey("StreamPush_" + SecureRandomUtils.generateRandomUUID().toString());
        resource.getProvisions().add(provision);

        propagationActions.forEach(key -> {
            Implementation impl = implementationDAO.find(key);
            if (impl == null || !IdMImplementationType.PROPAGATION_ACTIONS.equals(impl.getType())) {
                LOG.debug("Invalid " + Implementation.class.getSimpleName() + " {}, ignoring...", key);
            } else {
                resource.add(impl);
            }
        });

        return resource;
    }

    @Override
    public List<ProvisioningReport> push(
            final AnyType anyType,
            final List<? extends Any<?>> anys,
            final List<String> columns,
            final Connector connector,
            final List<String> propagationActions,
            final PushTaskTO pushTaskTO,
            final String executor) throws JobExecutionException {

        LOG.debug("Executing stream push as {}", executor);

        taskType = TaskType.PUSH;
        try {
            ExternalResource resource = externalResource(anyType, columns, propagationActions);

            task = entityFactory.newEntity(PushTask.class);
            task.setResource(resource);
            task.setMatchingRule(pushTaskTO.getMatchingRule());
            task.setUnmatchingRule(pushTaskTO.getUnmatchingRule());
            task.setPerformCreate(true);
            task.setPerformUpdate(true);
            task.setPerformDelete(true);
            task.setSyncStatus(false);

            profile = new ProvisioningProfile<>(connector, task);
            profile.setExecutor(executor);
            profile.getActions().addAll(getPushActions(pushTaskTO.getActions().stream().
                    map(implementationDAO::find).filter(Objects::nonNull).collect(Collectors.toList())));
            profile.setConflictResolutionAction(ConflictResolutionAction.FIRSTMATCH);

            for (PushActions action : profile.getActions()) {
                action.beforeAll(profile);
            }

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

            doHandle(anys, handler, resource);

            for (PushActions action : profile.getActions()) {
                action.afterAll(profile);
            }

            return profile.getResults();
        } catch (Exception e) {
            throw e instanceof JobExecutionException
                    ? (JobExecutionException) e
                    : new JobExecutionException("While stream pushing", e);
        } finally {
            setStatus(null);
        }
    }
}

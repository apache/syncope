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
import java.util.List;
import org.apache.syncope.common.lib.to.ProvisioningReport;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.Mapping;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.pushpull.AnyObjectPushResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.GroupPushResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.PushActions;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePushResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.stream.SyncopeStreamPushExecutor;
import org.apache.syncope.core.provisioning.api.pushpull.UserPushResultHandler;
import org.apache.syncope.core.provisioning.java.pushpull.PushJobDelegate;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.spring.ImplementationManager;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.identityconnectors.framework.common.objects.ObjectClass;
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

        Provision provision = entityFactory.newEntity(Provision.class);
        provision.setAnyType(anyType);
        provision.setObjectClass(new ObjectClass(anyType.getKey()));

        Mapping mapping = entityFactory.newEntity(Mapping.class);
        provision.setMapping(mapping);
        mapping.setProvision(provision);

        MappingItem connObjectKeyItem = entityFactory.newEntity(MappingItem.class);
        connObjectKeyItem.setExtAttrName("key");
        connObjectKeyItem.setIntAttrName("key");
        connObjectKeyItem.setPurpose(MappingPurpose.NONE);
        mapping.setConnObjectKeyItem(connObjectKeyItem);

        columns.stream().map(column -> {
            MappingItem item = entityFactory.newEntity(MappingItem.class);
            item.setExtAttrName(column);
            item.setIntAttrName(column);
            item.setPurpose(MappingPurpose.PROPAGATION);
            mapping.add(item);
            return item;
        }).forEach(mapping::add);

        ExternalResource resource = entityFactory.newEntity(ExternalResource.class);
        resource.setKey("StreamPush_" + SecureRandomUtils.generateRandomUUID().toString());
        resource.add(provision);
        provision.setResource(resource);

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

        List<PushActions> pushActions = new ArrayList<>();
        pushTaskTO.getActions().forEach(key -> {
            Implementation impl = implementationDAO.find(key);
            if (impl == null || !IdMImplementationType.PUSH_ACTIONS.equals(impl.getType())) {
                LOG.debug("Invalid " + Implementation.class.getSimpleName() + " {}, ignoring...", key);
            } else {
                try {
                    pushActions.add(ImplementationManager.build(impl));
                } catch (Exception e) {
                    LOG.warn("While building {}", impl, e);
                }
            }
        });

        try {
            ExternalResource resource = externalResource(anyType, columns, propagationActions);
            Provision provision = resource.getProvisions().get(0);

            PushTask pushTask = entityFactory.newEntity(PushTask.class);
            pushTask.setResource(resource);
            pushTask.setMatchingRule(pushTaskTO.getMatchingRule());
            pushTask.setUnmatchingRule(pushTaskTO.getUnmatchingRule());
            pushTask.setPerformCreate(true);
            pushTask.setPerformUpdate(true);
            pushTask.setPerformDelete(true);
            pushTask.setSyncStatus(false);

            profile = new ProvisioningProfile<>(connector, pushTask);
            profile.setExecutor(executor);
            profile.getActions().addAll(pushActions);
            profile.setConflictResolutionAction(ConflictResolutionAction.FIRSTMATCH);

            for (PushActions action : pushActions) {
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

            doHandle(anys, handler, provision.getResource());

            for (PushActions action : pushActions) {
                action.afterAll(profile);
            }

            return profile.getResults();
        } catch (Exception e) {
            throw e instanceof JobExecutionException
                    ? (JobExecutionException) e
                    : new JobExecutionException("While stream pushing", e);
        }
    }
}

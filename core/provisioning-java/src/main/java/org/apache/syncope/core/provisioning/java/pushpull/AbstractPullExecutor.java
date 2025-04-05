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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.task.ProvisioningTask;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.ProvisionSorter;
import org.apache.syncope.core.provisioning.api.job.JobExecutionContext;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.api.pushpull.AnyObjectPullResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.GroupPullResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.InboundActions;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.RealmPullResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePullExecutor;
import org.apache.syncope.core.provisioning.api.pushpull.UserPullResultHandler;
import org.apache.syncope.core.spring.implementation.ImplementationManager;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.springframework.beans.factory.annotation.Autowired;

abstract class AbstractPullExecutor<T extends ProvisioningTask<T>>
        extends AbstractProvisioningJobDelegate<T>
        implements SyncopePullExecutor {

    @Autowired
    protected GroupDAO groupDAO;

    @Autowired
    protected PlainSchemaDAO plainSchemaDAO;

    @Autowired
    protected VirSchemaDAO virSchemaDAO;

    @Autowired
    protected InboundMatcher inboundMatcher;

    @Autowired
    protected PlainAttrValidationManager validator;

    protected final Map<String, SyncToken> latestSyncTokens = Collections.synchronizedMap(new HashMap<>());

    private final Map<String, MutablePair<Integer, String>> handled = new HashMap<>();

    protected final Map<String, InboundActions> perContextActions = new ConcurrentHashMap<>();

    protected ProvisionSorter provisionSorter;

    protected ProvisioningProfile<PullTask, InboundActions> profile;

    protected PullResultHandlerDispatcher dispatcher;

    protected GroupPullResultHandler ghandler;

    protected RealmPullResultHandler buildRealmHandler() {
        return ApplicationContextProvider.getBeanFactory().createBean(DefaultRealmPullResultHandler.class);
    }

    protected AnyObjectPullResultHandler buildAnyObjectHandler() {
        return ApplicationContextProvider.getBeanFactory().createBean(DefaultAnyObjectPullResultHandler.class);
    }

    protected UserPullResultHandler buildUserHandler() {
        return ApplicationContextProvider.getBeanFactory().createBean(DefaultUserPullResultHandler.class);
    }

    protected GroupPullResultHandler buildGroupHandler() {
        return ApplicationContextProvider.getBeanFactory().createBean(DefaultGroupPullResultHandler.class);
    }

    protected List<InboundActions> getInboundActions(final List<? extends Implementation> impls) {
        List<InboundActions> result = new ArrayList<>();

        impls.forEach(impl -> {
            try {
                result.add(ImplementationManager.build(
                        impl,
                        () -> perContextActions.get(impl.getKey()),
                        instance -> perContextActions.put(impl.getKey(), instance)));
            } catch (Exception e) {
                LOG.warn("While building {}", impl, e);
            }
        });

        return result;
    }

    @Override
    protected void init(
            final TaskType taskType,
            final String taskKey,
            final JobExecutionContext context) throws JobExecutionException {

        super.init(taskType, taskKey, context);

        provisionSorter = getProvisionSorter(task);

        latestSyncTokens.clear();
    }

    protected void setGroupOwners() {
        ghandler.getGroupOwnerMap().forEach((groupKey, ownerKey) -> {
            Group group = groupDAO.findById(groupKey).orElseThrow(() -> new NotFoundException("Group " + groupKey));

            if (StringUtils.isBlank(ownerKey)) {
                group.setGroupOwner(null);
                group.setUserOwner(null);
            } else {
                inboundMatcher.match(
                        anyTypeDAO.getUser(),
                        ownerKey,
                        profile.getTask().getResource(),
                        profile.getConnector()).ifPresentOrElse(
                        userMatch -> group.setUserOwner((User) userMatch.getAny()),
                        () -> inboundMatcher.match(
                                anyTypeDAO.getGroup(),
                                ownerKey,
                                profile.getTask().getResource(),
                                profile.getConnector()).
                                ifPresent(groupMatch -> group.setGroupOwner((Group) groupMatch.getAny())));
            }

            groupDAO.save(group);
        });
    }

    @Override
    public void setLatestSyncToken(final String objectClass, final SyncToken latestSyncToken) {
        latestSyncTokens.put(objectClass, latestSyncToken);
    }

    @Override
    public void reportHandled(final String objectClass, final Name name) {
        synchronized (handled) {
            MutablePair<Integer, String> pair = Optional.ofNullable(handled.get(objectClass)).orElseGet(() -> {
                MutablePair<Integer, String> p = MutablePair.of(0, null);
                handled.put(objectClass, p);
                return p;
            });
            pair.setLeft(pair.getLeft() + 1);
            pair.setRight(name.getNameValue());

            if (!handled.isEmpty()) {
                StringBuilder builder = new StringBuilder("Processed:\n");
                handled.forEach((k, v) -> builder.append(' ').append(v.getLeft()).append('\t').
                        append(k).
                        append(" / latest: ").append(v.getRight()).
                        append('\n'));
                setStatus(builder.toString());
            }
        }
    }
}

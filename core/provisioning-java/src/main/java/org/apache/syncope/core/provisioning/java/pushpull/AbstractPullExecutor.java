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
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.request.StringReplacePatchItem;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.task.ProvisioningTask;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
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
import org.springframework.context.ConfigurableApplicationContext;

abstract class AbstractPullExecutor<T extends ProvisioningTask<T>>
        extends AbstractProvisioningJobDelegate<T>
        implements SyncopePullExecutor {

    @Autowired
    protected PlainSchemaDAO plainSchemaDAO;

    @Autowired
    protected InboundMatcher inboundMatcher;

    @Autowired
    protected PlainAttrValidationManager validator;

    @Autowired
    protected ConfigurableApplicationContext ctx;

    protected final Map<String, SyncToken> latestSyncTokens = Collections.synchronizedMap(new HashMap<>());

    private final Map<String, MutablePair<Integer, String>> handled = new HashMap<>();

    protected final Map<String, InboundActions> perContextActions = new ConcurrentHashMap<>();

    protected ProvisionSorter provisionSorter;

    protected ProvisioningProfile<PullTask, InboundActions> profile;

    protected PullResultHandlerDispatcher dispatcher;

    protected GroupPullResultHandler ghandler;

    protected PullResultHandlerDispatcher buildDispatcher() {
        return ctx.getBeanFactory().createBean(PullResultHandlerDispatcher.class).
                init(profile, this);
    }

    protected RealmPullResultHandler buildRealmHandler() {
        return ctx.getBeanFactory().createBean(DefaultRealmPullResultHandler.class);
    }

    protected AnyObjectPullResultHandler buildAnyObjectHandler() {
        return ctx.getBeanFactory().createBean(DefaultAnyObjectPullResultHandler.class);
    }

    protected UserPullResultHandler buildUserHandler() {
        return ctx.getBeanFactory().createBean(DefaultUserPullResultHandler.class);
    }

    protected GroupPullResultHandler buildGroupHandler() {
        return ctx.getBeanFactory().createBean(DefaultGroupPullResultHandler.class);
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
            GroupUR req = new GroupUR();
            req.setKey(groupKey);

            if (StringUtils.isBlank(ownerKey)) {
                req.setUserOwner(new StringReplacePatchItem.Builder().operation(PatchOperation.DELETE).build());
                req.setGroupOwner(new StringReplacePatchItem.Builder().operation(PatchOperation.DELETE).build());
            } else {
                inboundMatcher.match(
                        anyTypeDAO.getUser(),
                        ownerKey,
                        profile.getTask().getResource(),
                        profile.getConnector()).ifPresentOrElse(
                        userMatch -> req.setUserOwner(new StringReplacePatchItem.Builder().
                                value(userMatch.getAny().getKey()).build()),
                        () -> inboundMatcher.match(
                                anyTypeDAO.getGroup(),
                                ownerKey,
                                profile.getTask().getResource(),
                                profile.getConnector()).
                                ifPresent(groupMatch -> req.setGroupOwner(new StringReplacePatchItem.Builder().
                                value(groupMatch.getAny().getKey()).build())));
            }

            if (req.isEmpty()) {
                LOG.warn("Unable to set owner {} for group {}", ownerKey, groupKey);
            } else {
                ghandler.updateOwner(req);
            }
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

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
package org.apache.syncope.core.provisioning.java;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.provisioning.api.AnyObjectProvisioningManager;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.workflow.api.AnyObjectWorkflowAdapter;
import org.identityconnectors.framework.common.objects.Attribute;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class DefaultAnyObjectProvisioningManager implements AnyObjectProvisioningManager {

    protected final AnyObjectWorkflowAdapter awfAdapter;

    protected final PropagationManager propagationManager;

    protected final PropagationTaskExecutor taskExecutor;

    protected final AnyObjectDAO anyObjectDAO;

    protected final VirAttrHandler virtAttrHandler;

    public DefaultAnyObjectProvisioningManager(
            final AnyObjectWorkflowAdapter awfAdapter,
            final PropagationManager propagationManager,
            final PropagationTaskExecutor taskExecutor,
            final AnyObjectDAO anyObjectDAO,
            final VirAttrHandler virtAttrHandler) {

        this.awfAdapter = awfAdapter;
        this.propagationManager = propagationManager;
        this.taskExecutor = taskExecutor;
        this.anyObjectDAO = anyObjectDAO;
        this.virtAttrHandler = virtAttrHandler;
    }

    @Override
    public Pair<String, List<PropagationStatus>> create(
            final AnyObjectCR anyObjectCR,
            final boolean nullPriorityAsync,
            final String creator,
            final String context) {

        return create(anyObjectCR, Set.of(), nullPriorityAsync, creator, context);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public Pair<String, List<PropagationStatus>> create(
            final AnyObjectCR anyObjectCR,
            final Set<String> excludedResources,
            final boolean nullPriorityAsync,
            final String creator,
            final String context) {

        WorkflowResult<String> created = awfAdapter.create(anyObjectCR, creator, context);

        List<PropagationTaskInfo> taskInfos = propagationManager.getCreateTasks(
                AnyTypeKind.ANY_OBJECT,
                created.getResult(),
                null,
                created.getPropByRes(),
                anyObjectCR.getVirAttrs(),
                excludedResources);
        PropagationReporter propagationReporter = taskExecutor.execute(taskInfos, nullPriorityAsync, creator);

        return Pair.of(created.getResult(), propagationReporter.getStatuses());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public Pair<AnyObjectUR, List<PropagationStatus>> update(
            final AnyObjectUR anyObjectUR,
            final Set<String> excludedResources,
            final boolean nullPriorityAsync,
            final String updater,
            final String context) {

        Map<Pair<String, String>, Set<Attribute>> beforeAttrs = propagationManager.prepareAttrs(
                AnyTypeKind.ANY_OBJECT,
                anyObjectUR.getKey(),
                null,
                List.of(),
                null,
                excludedResources);

        WorkflowResult<AnyObjectUR> updated = awfAdapter.update(anyObjectUR, updater, context);

        List<PropagationTaskInfo> taskInfos = propagationManager.setAttributeDeltas(
                propagationManager.getUpdateTasks(
                        updated.getResult(),
                        AnyTypeKind.ANY_OBJECT,
                        updated.getResult().getKey(),
                        List.of(),
                        null,
                        updated.getPropByRes(),
                        null,
                        anyObjectUR.getVirAttrs(),
                        excludedResources),
                beforeAttrs);
        PropagationReporter propagationReporter = taskExecutor.execute(taskInfos, nullPriorityAsync, updater);

        return Pair.of(updated.getResult(), propagationReporter.getStatuses());
    }

    @Override
    public List<PropagationStatus> delete(
            final String key, final boolean nullPriorityAsync, final String eraser, final String context) {

        return delete(key, Set.of(), nullPriorityAsync, eraser, context);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public List<PropagationStatus> delete(
            final String key,
            final Set<String> excludedResources,
            final boolean nullPriorityAsync,
            final String eraser,
            final String context) {

        PropagationByResource<String> propByRes = new PropagationByResource<>();
        propByRes.set(ResourceOperation.DELETE, anyObjectDAO.findAllResourceKeys(key));

        // Note here that we can only notify about "delete", not any other
        // task defined in workflow process definition: this because this
        // information could only be available after awfAdapter.delete(), which
        // will also effectively remove user from db, thus making virtually
        // impossible by NotificationManager to fetch required any object information
        List<PropagationTaskInfo> taskInfos = propagationManager.getDeleteTasks(
                AnyTypeKind.ANY_OBJECT,
                key,
                propByRes,
                null,
                excludedResources);
        PropagationReporter propagationReporter = taskExecutor.execute(taskInfos, nullPriorityAsync, eraser);

        awfAdapter.delete(key, eraser, context);

        return propagationReporter.getStatuses();
    }

    @Override
    public String unlink(final AnyObjectUR anyObjectUR, final String updater, final String context) {
        return awfAdapter.update(anyObjectUR, updater, context).getResult().getKey();
    }

    @Override
    public String link(final AnyObjectUR anyObjectUR, final String updater, final String context) {
        return awfAdapter.update(anyObjectUR, updater, context).getResult().getKey();
    }

    @Override
    public List<PropagationStatus> provision(
            final String key,
            final Collection<String> resources,
            final boolean nullPriorityAsync,
            final String executor) {

        PropagationByResource<String> propByRes = new PropagationByResource<>();
        propByRes.addAll(ResourceOperation.UPDATE, resources);

        List<PropagationTaskInfo> taskInfos = propagationManager.getUpdateTasks(
                null,
                AnyTypeKind.ANY_OBJECT,
                key,
                List.of(),
                null,
                propByRes,
                null,
                null,
                null);
        PropagationReporter propagationReporter = taskExecutor.execute(taskInfos, nullPriorityAsync, executor);

        return propagationReporter.getStatuses();
    }

    @Override
    public List<PropagationStatus> deprovision(
            final String key,
            final Collection<String> resources,
            final boolean nullPriorityAsync,
            final String executor) {

        PropagationByResource<String> propByRes = new PropagationByResource<>();
        propByRes.addAll(ResourceOperation.DELETE, resources);

        List<PropagationTaskInfo> taskInfos = propagationManager.getDeleteTasks(
                AnyTypeKind.ANY_OBJECT,
                key,
                propByRes,
                null,
                anyObjectDAO.findAllResourceKeys(key).stream().
                        filter(resource -> !resources.contains(resource)).
                        toList());
        PropagationReporter propagationReporter = taskExecutor.execute(taskInfos, nullPriorityAsync, executor);

        return propagationReporter.getStatuses();
    }
}

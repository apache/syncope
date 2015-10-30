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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.patch.AnyObjectPatch;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.provisioning.api.AnyObjectProvisioningManager;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.misc.spring.ApplicationContextProvider;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.apache.syncope.core.workflow.api.AnyObjectWorkflowAdapter;
import org.springframework.beans.factory.annotation.Autowired;

public class DefaultAnyObjectProvisioningManager implements AnyObjectProvisioningManager {

    @Autowired
    protected AnyObjectWorkflowAdapter awfAdapter;

    @Autowired
    protected PropagationManager propagationManager;

    @Autowired
    protected PropagationTaskExecutor taskExecutor;

    @Autowired
    protected VirAttrHandler virtAttrHandler;

    @Autowired
    protected AnyObjectDAO anyObjectDAO;

    @Override
    public Pair<Long, List<PropagationStatus>> create(final AnyObjectTO anyObjectTO, final boolean nullPriorityAsync) {
        return create(anyObjectTO, Collections.<String>emptySet(), nullPriorityAsync);
    }

    @Override
    public Pair<Long, List<PropagationStatus>> create(
            final AnyObjectTO anyObjectTO, final Set<String> excludedResources, final boolean nullPriorityAsync) {

        WorkflowResult<Long> created = awfAdapter.create(anyObjectTO);

        List<PropagationTask> tasks = propagationManager.getCreateTasks(
                AnyTypeKind.ANY_OBJECT,
                created.getResult(),
                created.getPropByRes(),
                anyObjectTO.getVirAttrs(),
                excludedResources);
        PropagationReporter propagationReporter =
                ApplicationContextProvider.getBeanFactory().getBean(PropagationReporter.class);
        taskExecutor.execute(tasks, propagationReporter, nullPriorityAsync);

        return new ImmutablePair<>(created.getResult(), propagationReporter.getStatuses());
    }

    @Override
    public Pair<Long, List<PropagationStatus>> update(
            final AnyObjectPatch anyObjectPatch, final boolean nullPriorityAsync) {

        return update(anyObjectPatch, Collections.<String>emptySet(), nullPriorityAsync);
    }

    @Override
    public Pair<Long, List<PropagationStatus>> update(
            final AnyObjectPatch anyObjectPatch, final Set<String> excludedResources, final boolean nullPriorityAsync) {

        WorkflowResult<Long> updated = awfAdapter.update(anyObjectPatch);

        List<PropagationTask> tasks = propagationManager.getUpdateTasks(
                AnyTypeKind.ANY_OBJECT,
                updated.getResult(),
                false,
                null,
                updated.getPropByRes(),
                anyObjectPatch.getVirAttrs(),
                excludedResources);
        PropagationReporter propagationReporter =
                ApplicationContextProvider.getBeanFactory().getBean(PropagationReporter.class);
        taskExecutor.execute(tasks, propagationReporter, nullPriorityAsync);

        return new ImmutablePair<>(updated.getResult(), propagationReporter.getStatuses());
    }

    @Override
    public List<PropagationStatus> delete(final Long key, final boolean nullPriorityAsync) {
        return delete(key, Collections.<String>emptySet(), nullPriorityAsync);
    }

    @Override
    public List<PropagationStatus> delete(
            final Long key, final Set<String> excludedResources, final boolean nullPriorityAsync) {

        List<PropagationTask> tasks = propagationManager.getDeleteTasks(
                AnyTypeKind.ANY_OBJECT,
                key,
                null,
                excludedResources);
        PropagationReporter propagationReporter =
                ApplicationContextProvider.getBeanFactory().getBean(PropagationReporter.class);
        taskExecutor.execute(tasks, propagationReporter, nullPriorityAsync);

        awfAdapter.delete(key);

        return propagationReporter.getStatuses();
    }

    @Override
    public Long unlink(final AnyObjectPatch anyObjectPatch) {
        return awfAdapter.update(anyObjectPatch).getResult();
    }

    @Override
    public Long link(final AnyObjectPatch anyObjectPatch) {
        return awfAdapter.update(anyObjectPatch).getResult();
    }

    @Override
    public List<PropagationStatus> provision(
            final Long key, final Collection<String> resources, final boolean nullPriorityAsync) {

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.addAll(ResourceOperation.UPDATE, resources);

        List<PropagationTask> tasks = propagationManager.getUpdateTasks(
                AnyTypeKind.ANY_OBJECT,
                key,
                false,
                null,
                propByRes,
                null,
                null);
        PropagationReporter propagationReporter =
                ApplicationContextProvider.getBeanFactory().getBean(PropagationReporter.class);
        taskExecutor.execute(tasks, propagationReporter, nullPriorityAsync);

        return propagationReporter.getStatuses();
    }

    @Override
    public List<PropagationStatus> deprovision(
            final Long key, final Collection<String> resources, final boolean nullPriorityAsync) {

        PropagationByResource propByRes = new PropagationByResource();
        propByRes.addAll(ResourceOperation.DELETE, resources);

        List<PropagationTask> tasks = propagationManager.getDeleteTasks(
                AnyTypeKind.ANY_OBJECT,
                key,
                propByRes,
                CollectionUtils.removeAll(anyObjectDAO.findAllResourceNames(anyObjectDAO.authFind(key)), resources));
        PropagationReporter propagationReporter =
                ApplicationContextProvider.getBeanFactory().getBean(PropagationReporter.class);
        taskExecutor.execute(tasks, propagationReporter, nullPriorityAsync);

        return propagationReporter.getStatuses();
    }
}

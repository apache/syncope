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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.mod.AnyObjectMod;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.provisioning.api.AnyObjectProvisioningManager;
import org.apache.syncope.core.provisioning.api.WorkflowResult;
import org.apache.syncope.core.provisioning.api.propagation.PropagationException;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.misc.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.apache.syncope.core.workflow.api.AnyObjectWorkflowAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class DefaultAnyObjectProvisioningManager implements AnyObjectProvisioningManager {

    private static final Logger LOG = LoggerFactory.getLogger(AnyObjectProvisioningManager.class);

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
    public Pair<Long, List<PropagationStatus>> create(final AnyObjectTO anyObjectTO) {
        return create(anyObjectTO, Collections.<String>emptySet());
    }

    @Override
    public Pair<Long, List<PropagationStatus>> create(
            final AnyObjectTO anyObjectTO, final Set<String> excludedResources) {

        WorkflowResult<Long> created = awfAdapter.create(anyObjectTO);

        List<PropagationTask> tasks = propagationManager.getAnyObjectCreateTasks(
                created, anyObjectTO.getVirAttrs(), excludedResources);
        PropagationReporter propagationReporter =
                ApplicationContextProvider.getApplicationContext().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        return new ImmutablePair<>(created.getResult(), propagationReporter.getStatuses());
    }

    @Override
    public Pair<Long, List<PropagationStatus>> update(final AnyObjectMod anyObjectMod) {
        return update(anyObjectMod, Collections.<String>emptySet());
    }

    @Override
    public Pair<Long, List<PropagationStatus>> update(
            final AnyObjectMod anyObjectMod, final Set<String> excludedResources) {

        WorkflowResult<Long> updated = awfAdapter.update(anyObjectMod);

        List<PropagationTask> tasks = propagationManager.getAnyObjectUpdateTasks(updated,
                anyObjectMod.getVirAttrsToRemove(), anyObjectMod.getVirAttrsToUpdate(), null);
        if (tasks.isEmpty()) {
            // SYNCOPE-459: take care of user virtual attributes ...
            PropagationByResource propByResVirAttr = virtAttrHandler.fillVirtual(
                    updated.getResult(),
                    AnyTypeKind.ANY_OBJECT,
                    anyObjectMod.getVirAttrsToRemove(),
                    anyObjectMod.getVirAttrsToUpdate());
            tasks.addAll(!propByResVirAttr.isEmpty()
                    ? propagationManager.getAnyObjectUpdateTasks(updated, null, null, null)
                    : Collections.<PropagationTask>emptyList());
        }

        PropagationReporter propagationReporter =
                ApplicationContextProvider.getApplicationContext().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        return new ImmutablePair<>(updated.getResult(), propagationReporter.getStatuses());
    }

    @Override
    public List<PropagationStatus> delete(final Long anyObjectKey) {
        return delete(anyObjectKey, Collections.<String>emptySet());
    }

    @Override
    public List<PropagationStatus> delete(final Long anyObjectKey, final Set<String> excludedResources) {
        List<PropagationTask> tasks = new ArrayList<>();

        AnyObject anyObject = anyObjectDAO.authFind(anyObjectKey);
        if (anyObject != null) {
            tasks.addAll(propagationManager.getAnyObjectDeleteTasks(anyObject.getKey()));
        }

        PropagationReporter propagationReporter = ApplicationContextProvider.getApplicationContext().
                getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }

        awfAdapter.delete(anyObjectKey);

        return propagationReporter.getStatuses();
    }

    @Override
    public Long unlink(final AnyObjectMod anyObjectMod) {
        return awfAdapter.update(anyObjectMod).getResult();
    }

    @Override
    public Long link(final AnyObjectMod anyObjectMod) {
        return awfAdapter.update(anyObjectMod).getResult();
    }

    @Override
    public List<PropagationStatus> deprovision(final Long anyObjectKey, final Collection<String> resources) {
        AnyObject anyObject = anyObjectDAO.authFind(anyObjectKey);

        Collection<String> noPropResourceName = CollectionUtils.removeAll(anyObject.getResourceNames(), resources);

        List<PropagationTask> tasks = propagationManager.getAnyObjectDeleteTasks(
                anyObjectKey, new HashSet<>(resources), noPropResourceName);
        PropagationReporter propagationReporter =
                ApplicationContextProvider.getApplicationContext().getBean(PropagationReporter.class);
        try {
            taskExecutor.execute(tasks, propagationReporter);
        } catch (PropagationException e) {
            LOG.error("Error propagation primary resource", e);
            propagationReporter.onPrimaryResourceFailure(tasks);
        }
        return propagationReporter.getStatuses();
    }
}

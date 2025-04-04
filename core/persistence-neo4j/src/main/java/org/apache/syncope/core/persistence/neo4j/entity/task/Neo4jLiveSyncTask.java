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
package org.apache.syncope.core.persistence.neo4j.entity.task;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.task.AnyTemplateLiveSyncTask;
import org.apache.syncope.core.persistence.api.entity.task.LiveSyncTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jImplementation;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jImplementationRelationship;
import org.apache.syncope.core.persistence.neo4j.entity.SortedSetList;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.PostLoad;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jLiveSyncTask.NODE)
public class Neo4jLiveSyncTask extends Neo4jInboundTask<LiveSyncTask> implements LiveSyncTask {

    private static final long serialVersionUID = -6265995715460303850L;

    public static final String NODE = "LiveSyncTask";

    public static final String LIVE_SYNC_MAPPER_REL = "LIVE_SYNC_MAPPER";

    public static final String LIVE_SYNC_TASK_LIVE_SYNC_ACTIONS_REL = "LIVE_SYNC_TASK_LIVE_SYNC_ACTIONS";

    public static final String LIVE_SYNC_TASK_TEMPLATE_REL = "LIVE_SYNC_TASK_TEMPLATE";

    public static final String LIVE_SYNC_TASK_EXEC_REL = "LIVE_SYNC_TASK_EXEC";

    @Min(1)
    @NotNull
    private Integer delaySecondsAcrossInvocations = 5;

    @Relationship(type = LIVE_SYNC_MAPPER_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jImplementation liveSyncDeltaMapper;

    @Relationship(type = LIVE_SYNC_TASK_LIVE_SYNC_ACTIONS_REL, direction = Relationship.Direction.OUTGOING)
    private SortedSet<Neo4jImplementationRelationship> actions = new TreeSet<>();

    @Transient
    private List<Neo4jImplementation> sortedActions = new SortedSetList<>(
            actions, Neo4jImplementationRelationship.builder());

    @Relationship(type = LIVE_SYNC_TASK_TEMPLATE_REL, direction = Relationship.Direction.INCOMING)
    private List<Neo4jAnyTemplateLiveSyncTask> templates = new ArrayList<>();

    @Relationship(type = LIVE_SYNC_TASK_EXEC_REL, direction = Relationship.Direction.INCOMING)
    private List<Neo4jLiveSyncTaskExec> executions = new ArrayList<>();

    @Override
    public int getDelaySecondsAcrossInvocations() {
        return Optional.ofNullable(delaySecondsAcrossInvocations).orElse(5);
    }

    @Override
    public void setDelaySecondsAcrossInvocations(final int delaySecondsAcrossInvocations) {
        this.delaySecondsAcrossInvocations = delaySecondsAcrossInvocations;
    }

    @Override
    public Implementation getLiveSyncDeltaMapper() {
        return liveSyncDeltaMapper;
    }

    @Override
    public void setLiveSyncDeltaMapper(final Implementation liveSyncDeltaMapper) {
        checkType(liveSyncDeltaMapper, Neo4jImplementation.class);
        checkImplementationType(liveSyncDeltaMapper, IdMImplementationType.LIVE_SYNC_DELTA_MAPPER);
        this.liveSyncDeltaMapper = (Neo4jImplementation) liveSyncDeltaMapper;
    }

    @Override
    public boolean add(final Implementation action) {
        checkType(action, Neo4jImplementation.class);
        checkImplementationType(action, IdMImplementationType.INBOUND_ACTIONS);
        return sortedActions.contains((Neo4jImplementation) action) || sortedActions.add((Neo4jImplementation) action);
    }

    @Override
    public List<? extends Implementation> getActions() {
        return sortedActions;
    }

    @Override
    public boolean add(final AnyTemplateLiveSyncTask template) {
        checkType(template, Neo4jAnyTemplateLiveSyncTask.class);
        return this.templates.add((Neo4jAnyTemplateLiveSyncTask) template);
    }

    @Override
    public Optional<? extends AnyTemplateLiveSyncTask> getTemplate(final String anyType) {
        return templates.stream().
                filter(template -> anyType != null && anyType.equals(template.getAnyType().getKey())).
                findFirst();
    }

    @Override
    public List<? extends AnyTemplateLiveSyncTask> getTemplates() {
        return templates;
    }

    @Override
    protected boolean doAdd(final TaskExec<SchedTask> exec) {
        return executions.add((Neo4jLiveSyncTaskExec) exec);
    }

    @Override
    protected Class<? extends AbstractTaskExec<SchedTask>> executionClass() {
        return Neo4jLiveSyncTaskExec.class;
    }

    @Override
    protected List<? extends AbstractTaskExec<SchedTask>> executions() {
        return executions;
    }

    @PostLoad
    public void postLoad() {
        sortedActions = new SortedSetList<>(actions, Neo4jImplementationRelationship.builder());
    }
}

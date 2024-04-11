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

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.task.AnyTemplatePullTask;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jImplementation;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jImplementationRelationship;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRealm;
import org.apache.syncope.core.persistence.neo4j.entity.SortedSetList;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.PostLoad;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jPullTask.NODE)
public class Neo4jPullTask extends Neo4jProvisioningTask<PullTask> implements PullTask {

    private static final long serialVersionUID = -4141057723006682563L;

    public static final String NODE = "PullTask";

    public static final String PULL_TASK_RECON_FILTER_BUIDER_REL = "PULL_TASK_RECON_FILTER_BUIDER";

    public static final String PULL_TASK_PULL_ACTIONS_REL = "PULL_TASK_PULL_ACTIONS";

    public static final String PULL_TASK_TEMPLATE_REL = "PULL_TASK_TEMPLATE";

    public static final String PULL_TASK_EXEC_REL = "PULL_TASK_EXEC";

    @NotNull
    private PullMode pullMode;

    @Relationship(type = PULL_TASK_RECON_FILTER_BUIDER_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jImplementation reconFilterBuilder;

    @NotNull
    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jRealm destinationRealm;

    @Relationship(type = PULL_TASK_PULL_ACTIONS_REL, direction = Relationship.Direction.OUTGOING)
    private SortedSet<Neo4jImplementationRelationship> actions = new TreeSet<>();

    @Transient
    private List<Neo4jImplementation> sortedActions = new SortedSetList(actions);

    @Relationship(type = PULL_TASK_TEMPLATE_REL, direction = Relationship.Direction.INCOMING)
    private List<Neo4jAnyTemplatePullTask> templates = new ArrayList<>();

    @Relationship(type = PULL_TASK_EXEC_REL, direction = Relationship.Direction.INCOMING)
    private List<Neo4jPullTaskExec> executions = new ArrayList<>();

    @NotNull
    private Boolean remediation = false;

    @Override
    public PullMode getPullMode() {
        return pullMode;
    }

    @Override
    public void setPullMode(final PullMode pullMode) {
        this.pullMode = pullMode;
    }

    @Override
    public Implementation getReconFilterBuilder() {
        return reconFilterBuilder;
    }

    @Override
    public void setReconFilterBuilder(final Implementation reconFilterBuilder) {
        checkType(reconFilterBuilder, Neo4jImplementation.class);
        checkImplementationType(reconFilterBuilder, IdMImplementationType.RECON_FILTER_BUILDER);
        this.reconFilterBuilder = (Neo4jImplementation) reconFilterBuilder;
    }

    @Override
    public Realm getDestinationRealm() {
        return destinationRealm;
    }

    @Override
    public void setDestinationRealm(final Realm destinationRealm) {
        checkType(destinationRealm, Neo4jRealm.class);
        this.destinationRealm = (Neo4jRealm) destinationRealm;
    }

    @Override
    public boolean add(final Implementation action) {
        checkType(action, Neo4jImplementation.class);
        checkImplementationType(action, IdMImplementationType.PULL_ACTIONS);
        return sortedActions.contains((Neo4jImplementation) action) || sortedActions.add((Neo4jImplementation) action);
    }

    @Override
    public List<? extends Implementation> getActions() {
        return sortedActions;
    }

    @Override
    public boolean add(final AnyTemplatePullTask template) {
        checkType(template, Neo4jAnyTemplatePullTask.class);
        return this.templates.add((Neo4jAnyTemplatePullTask) template);
    }

    @Override
    public Optional<? extends AnyTemplatePullTask> getTemplate(final String anyType) {
        return templates.stream().
                filter(template -> anyType != null && anyType.equals(template.getAnyType().getKey())).
                findFirst();
    }

    @Override
    public List<? extends AnyTemplatePullTask> getTemplates() {
        return templates;
    }

    @Override
    public void setRemediation(final boolean remediation) {
        this.remediation = remediation;
    }

    @Override
    public boolean isRemediation() {
        return concurrentSettings != null ? true : remediation;
    }

    @Override
    protected boolean doAdd(final TaskExec<SchedTask> exec) {
        return executions.add((Neo4jPullTaskExec) exec);
    }

    @Override
    protected Class<? extends AbstractTaskExec<SchedTask>> executionClass() {
        return Neo4jPullTaskExec.class;
    }

    @Override
    protected List<? extends AbstractTaskExec<SchedTask>> executions() {
        return executions;
    }

    @PostLoad
    public void postLoad() {
        sortedActions = new SortedSetList(actions);
    }
}

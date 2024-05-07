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

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jImplementation;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jImplementationRelationship;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRealm;
import org.apache.syncope.core.persistence.neo4j.entity.SortedSetList;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.PostLoad;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jPushTask.NODE)
public class Neo4jPushTask extends Neo4jProvisioningTask<PushTask> implements PushTask {

    private static final long serialVersionUID = -4141057723006682564L;

    public static final String NODE = "PushTask";

    public static final String PUSH_TASK_PUSH_ACTIONS_REL = "PUSH_TASK_PUSH_ACTIONS";

    public static final String PUSH_TASK_EXEC_REL = "PUSH_TASK_EXEC";

    protected static final TypeReference<HashMap<String, String>> FILTER_TYPEREF =
            new TypeReference<HashMap<String, String>>() {
    };

    @NotNull
    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jRealm sourceRealm;

    private String filters;

    @Transient
    private Map<String, String> filterMap = new HashMap<>();

    @Relationship(type = PUSH_TASK_PUSH_ACTIONS_REL, direction = Relationship.Direction.OUTGOING)
    private SortedSet<Neo4jImplementationRelationship> actions = new TreeSet<>();

    @Transient
    private List<Neo4jImplementation> sortedActions = new SortedSetList<>(
            actions, Neo4jImplementationRelationship.builder());

    @Relationship(type = PUSH_TASK_EXEC_REL, direction = Relationship.Direction.INCOMING)
    private List<Neo4jPushTaskExec> executions = new ArrayList<>();

    @Override
    public Neo4jRealm getSourceRealm() {
        return sourceRealm;
    }

    @Override
    public void setSourceRealm(final Realm sourceRealm) {
        checkType(sourceRealm, Neo4jRealm.class);
        this.sourceRealm = (Neo4jRealm) sourceRealm;
    }

    @Override
    public boolean add(final Implementation action) {
        checkType(action, Neo4jImplementation.class);
        checkImplementationType(action, IdMImplementationType.PUSH_ACTIONS);
        return sortedActions.contains((Neo4jImplementation) action) || sortedActions.add((Neo4jImplementation) action);
    }

    @Override
    public List<? extends Implementation> getActions() {
        return sortedActions;
    }

    @Override
    public Optional<String> getFilter(final String anyType) {
        return Optional.ofNullable(filterMap.get(anyType));
    }

    @Override
    public Map<String, String> getFilters() {
        return filterMap;
    }

    @Override
    protected boolean doAdd(final TaskExec<SchedTask> exec) {
        return executions.add((Neo4jPushTaskExec) exec);
    }

    @Override
    protected Class<? extends AbstractTaskExec<SchedTask>> executionClass() {
        return Neo4jPushTaskExec.class;
    }

    @Override
    protected List<? extends AbstractTaskExec<SchedTask>> executions() {
        return executions;
    }

    protected void json2map(final boolean clearFirst) {
        if (clearFirst) {
            getFilters().clear();
        }
        if (filters != null) {
            getFilters().putAll(POJOHelper.deserialize(filters, FILTER_TYPEREF));
        }
    }

    @PostLoad
    public void postLoad() {
        sortedActions = new SortedSetList<>(actions, Neo4jImplementationRelationship.builder());
        json2map(false);
    }

    public void postSave() {
        json2map(true);
    }

    public void map2json() {
        filters = POJOHelper.serialize(getFilters());
    }
}

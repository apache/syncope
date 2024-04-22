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
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.syncope.common.lib.command.CommandArgs;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.task.MacroTask;
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

@Node(Neo4jMacroTask.NODE)
public class Neo4jMacroTask extends Neo4jSchedTask implements MacroTask {

    private static final long serialVersionUID = 8261850094316787406L;

    public static final String NODE = "MacroTask";

    public static final String MACRO_TASK_EXEC_REL = "MACRO_TASK_EXEC";

    public static final String MACRO_TASK_COMMANDS_REL = "MACRO_TASK_COMMANDS";

    protected static final TypeReference<List<CommandArgs>> TYPEREF = new TypeReference<List<CommandArgs>>() {
    };

    @NotNull
    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jRealm realm;

    @NotNull
    private Boolean continueOnError = false;

    @NotNull
    private Boolean saveExecs = true;

    @Relationship(type = MACRO_TASK_COMMANDS_REL, direction = Relationship.Direction.OUTGOING)
    private SortedSet<Neo4jImplementationRelationship> commands = new TreeSet<>();

    @Transient
    private List<Neo4jImplementation> sortedCommands = new SortedSetList(commands);

    private String commandArgs;

    @Transient
    private final List<CommandArgs> commandArgsList = new ArrayList<>();

    @Relationship(type = MACRO_TASK_EXEC_REL, direction = Relationship.Direction.INCOMING)
    private List<Neo4jMacroTaskExec> executions = new ArrayList<>();

    @Override
    public Realm getRealm() {
        return realm;
    }

    @Override
    public void setRealm(final Realm realm) {
        checkType(realm, Neo4jRealm.class);
        this.realm = (Neo4jRealm) realm;
    }

    @Override
    public void add(final Implementation command, final CommandArgs args) {
        checkType(command, Neo4jImplementation.class);
        checkImplementationType(command, IdRepoImplementationType.COMMAND);
        sortedCommands.add((Neo4jImplementation) command);
        getCommandArgs().add(args);
    }

    @Override
    public List<Neo4jImplementation> getCommands() {
        return sortedCommands;
    }

    @Override
    public List<CommandArgs> getCommandArgs() {
        return commandArgsList;
    }

    @Override
    public boolean isContinueOnError() {
        return continueOnError == null ? false : continueOnError;
    }

    @Override
    public void setContinueOnError(final boolean continueOnError) {
        this.continueOnError = continueOnError;
    }

    @Override
    public boolean isSaveExecs() {
        return saveExecs == null ? true : saveExecs;
    }

    @Override
    public void setSaveExecs(final boolean saveExecs) {
        this.saveExecs = saveExecs;
    }

    @Override
    protected boolean doAdd(final TaskExec<SchedTask> exec) {
        return executions.add((Neo4jMacroTaskExec) exec);
    }

    @Override
    protected Class<? extends AbstractTaskExec<SchedTask>> executionClass() {
        return Neo4jMacroTaskExec.class;
    }

    @Override
    protected List<? extends AbstractTaskExec<SchedTask>> executions() {
        return executions;
    }

    protected void json2list(final boolean clearFirst) {
        if (clearFirst) {
            getCommandArgs().clear();
        }
        if (commandArgs != null) {
            getCommandArgs().addAll(POJOHelper.deserialize(commandArgs, TYPEREF));
        }
    }

    @PostLoad
    public void postLoad() {
        sortedCommands = new SortedSetList(commands);
        json2list(false);
    }

    public void postSave() {
        json2list(true);
    }

    public void list2json() {
        commandArgs = POJOHelper.serialize(getCommandArgs(), TYPEREF);
    }
}

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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.task.FormPropertyDef;
import org.apache.syncope.core.persistence.api.entity.task.MacroTask;
import org.apache.syncope.core.persistence.api.entity.task.MacroTaskCommand;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jImplementation;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRealm;
import org.apache.syncope.core.persistence.neo4j.entity.SortedSetList;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.PostLoad;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jMacroTask.NODE)
public class Neo4jMacroTask extends Neo4jSchedTask implements MacroTask {

    private static final long serialVersionUID = 8261850094316787406L;

    public static final String NODE = "MacroTask";

    public static final String MACRO_TASK_FORM_PROPERTY_DEF_REL = "MACRO_TASK_FORM_PROPERTY_DEF_REL";

    public static final String MACRO_TASK_MACRO_ACTIONS_REL = "MACRO_TASK_MACRO_ACTIONS";

    public static final String MACRO_TASK_EXEC_REL = "MACRO_TASK_EXEC";

    public static final String MACRO_TASK_COMMANDS_REL = "MACRO_TASK_COMMANDS";

    @NotNull
    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jRealm realm;

    @NotNull
    private Boolean continueOnError = false;

    @NotNull
    private Boolean saveExecs = true;

    @Relationship(type = MACRO_TASK_COMMANDS_REL, direction = Relationship.Direction.OUTGOING)
    private SortedSet<Neo4jMacroTaskCommandRelationship> commands = new TreeSet<>();

    @Transient
    private List<Neo4jMacroTaskCommand> sortedCommands = new SortedSetList<>(
            commands, Neo4jMacroTaskCommandRelationship.builder());

    @Relationship(type = MACRO_TASK_FORM_PROPERTY_DEF_REL, direction = Relationship.Direction.INCOMING)
    @Valid
    private SortedSet<Neo4jFormPropertyDefRelationship> formPropertyDefs = new TreeSet<>();

    @Transient
    private List<Neo4jFormPropertyDef> sortedFormPropertyDefs = new SortedSetList<>(
            formPropertyDefs, Neo4jFormPropertyDefRelationship.builder());

    @Relationship(type = MACRO_TASK_MACRO_ACTIONS_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jImplementation macroActions;

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

    @Override
    public void add(final MacroTaskCommand macroTaskCommand) {
        checkType(macroTaskCommand, Neo4jMacroTaskCommand.class);
        sortedCommands.add((Neo4jMacroTaskCommand) macroTaskCommand);
    }

    @Override
    public List<? extends MacroTaskCommand> getCommands() {
        return sortedCommands;
    }

    @Override
    public void add(final FormPropertyDef formPropertyDef) {
        checkType(formPropertyDef, Neo4jFormPropertyDef.class);
        sortedFormPropertyDefs.add((Neo4jFormPropertyDef) formPropertyDef);
    }

    @Override
    public List<? extends FormPropertyDef> getFormPropertyDefs() {
        return sortedFormPropertyDefs;
    }

    @Override
    public Implementation getMacroActions() {
        return macroActions;
    }

    @Override
    public void setMacroAction(final Implementation macroActions) {
        checkType(macroActions, Neo4jImplementation.class);
        checkImplementationType(macroActions, IdRepoImplementationType.MACRO_ACTIONS);
        this.macroActions = (Neo4jImplementation) macroActions;
    }

    @PostLoad
    public void postLoad() {
        sortedCommands = new SortedSetList<>(commands, Neo4jMacroTaskCommandRelationship.builder());
        sortedFormPropertyDefs = new SortedSetList<>(formPropertyDefs, Neo4jFormPropertyDefRelationship.builder());
    }
}

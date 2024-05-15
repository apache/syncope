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
package org.apache.syncope.core.persistence.jpa.entity.task;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.task.FormPropertyDef;
import org.apache.syncope.core.persistence.api.entity.task.MacroTask;
import org.apache.syncope.core.persistence.api.entity.task.MacroTaskCommand;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.jpa.entity.JPAImplementation;
import org.apache.syncope.core.persistence.jpa.entity.JPARealm;

@Entity
@Table(name = JPAMacroTask.TABLE)
public class JPAMacroTask extends JPASchedTask implements MacroTask {

    private static final long serialVersionUID = 8261850094316787406L;

    public static final String TABLE = "MacroTask";

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private JPARealm realm;

    @NotNull
    private Boolean continueOnError = false;

    @NotNull
    private Boolean saveExecs = true;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "macroTask")
    @OrderBy("idx")
    private List<JPAMacroTaskCommand> macroTaskCommands = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "macroTask")
    @OrderBy("idx")
    @Valid
    private List<JPAFormPropertyDef> formPropertyDefs = new ArrayList<>();

    @ManyToOne(fetch = FetchType.EAGER)
    private JPAImplementation macroActions;

    @OneToMany(targetEntity = JPAMacroTaskExec.class,
            cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "task")
    private List<TaskExec<SchedTask>> executions = new ArrayList<>();

    @Override
    public Realm getRealm() {
        return realm;
    }

    @Override
    public void setRealm(final Realm realm) {
        checkType(realm, JPARealm.class);
        this.realm = (JPARealm) realm;
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
    protected Class<? extends TaskExec<SchedTask>> executionClass() {
        return JPAMacroTaskExec.class;
    }

    @Override
    protected List<TaskExec<SchedTask>> executions() {
        return executions;
    }

    @Override
    public void add(final MacroTaskCommand macroTaskCommand) {
        checkType(macroTaskCommand, JPAMacroTaskCommand.class);
        ((JPAMacroTaskCommand) macroTaskCommand).setIdx(macroTaskCommands.size());
        macroTaskCommands.add((JPAMacroTaskCommand) macroTaskCommand);
    }

    @Override
    public List<? extends MacroTaskCommand> getCommands() {
        return macroTaskCommands;
    }

    @Override
    public void add(final FormPropertyDef formPropertyDef) {
        checkType(formPropertyDef, JPAFormPropertyDef.class);
        ((JPAFormPropertyDef) formPropertyDef).setIdx(formPropertyDefs.size());
        formPropertyDefs.add((JPAFormPropertyDef) formPropertyDef);
    }

    @Override
    public List<? extends FormPropertyDef> getFormPropertyDefs() {
        return formPropertyDefs;
    }

    @Override
    public Implementation getMacroActions() {
        return macroActions;
    }

    @Override
    public void setMacroAction(final Implementation macroActions) {
        checkType(macroActions, JPAImplementation.class);
        checkImplementationType(macroActions, IdRepoImplementationType.MACRO_ACTIONS);
        this.macroActions = (JPAImplementation) macroActions;
    }
}

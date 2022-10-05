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

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import org.apache.syncope.common.lib.command.CommandArgs;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.task.MacroTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.jpa.entity.JPAImplementation;
import org.apache.syncope.core.persistence.jpa.entity.JPARealm;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

@Entity
@Table(name = JPAMacroTask.TABLE)
public class JPAMacroTask extends JPASchedTask implements MacroTask {

    private static final long serialVersionUID = 8261850094316787406L;

    public static final String TABLE = "MacroTask";

    protected static final TypeReference<List<CommandArgs>> TYPEREF = new TypeReference<List<CommandArgs>>() {
    };

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private JPARealm realm;

    @NotNull
    private Boolean continueOnError = false;

    @NotNull
    private Boolean saveExecs = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = TABLE + "Commands",
            joinColumns =
            @JoinColumn(name = "task_id"),
            inverseJoinColumns =
            @JoinColumn(name = "implementation_id"),
            uniqueConstraints =
            @UniqueConstraint(columnNames = { "task_id", "implementation_id" }))
    private List<JPAImplementation> commands = new ArrayList<>();

    @Lob
    private String commandArgs;

    @Transient
    private final List<CommandArgs> commandArgsList = new ArrayList<>();

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
    public void add(final Implementation command, final CommandArgs args) {
        checkType(command, JPAImplementation.class);
        checkImplementationType(command, IdRepoImplementationType.COMMAND);
        commands.add((JPAImplementation) command);

        getCommandArgs().add(args);
    }

    @Override
    public List<JPAImplementation> getCommands() {
        return commands;
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
    protected Class<? extends TaskExec<SchedTask>> executionClass() {
        return JPAMacroTaskExec.class;
    }

    @Override
    protected List<TaskExec<SchedTask>> executions() {
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
        json2list(false);
    }

    @PostPersist
    @PostUpdate
    public void postSave() {
        json2list(true);
    }

    @PrePersist
    @PreUpdate
    public void list2json() {
        commandArgs = POJOHelper.serialize(getCommandArgs(), TYPEREF);
    }
}

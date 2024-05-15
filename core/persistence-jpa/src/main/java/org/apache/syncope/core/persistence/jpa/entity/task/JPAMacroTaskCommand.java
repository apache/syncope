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

import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.Optional;
import org.apache.syncope.common.lib.command.CommandArgs;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.task.MacroTask;
import org.apache.syncope.core.persistence.api.entity.task.MacroTaskCommand;
import org.apache.syncope.core.persistence.jpa.entity.AbstractGeneratedKeyEntity;
import org.apache.syncope.core.persistence.jpa.entity.JPAImplementation;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

@Entity
@Table(name = JPAMacroTaskCommand.TABLE)
public class JPAMacroTaskCommand extends AbstractGeneratedKeyEntity implements MacroTaskCommand {

    private static final long serialVersionUID = -8388668645348044783L;

    public static final String TABLE = "MacroTaskCommand";

    private int idx;

    @ManyToOne(optional = false)
    private JPAMacroTask macroTask;

    @OneToOne(optional = false)
    private JPAImplementation command;

    @Lob
    private String args;

    public void setIdx(final int idx) {
        this.idx = idx;
    }

    @Override
    public JPAMacroTask getMacroTask() {
        return macroTask;
    }

    @Override
    public void setMacroTask(final MacroTask macroTask) {
        checkType(macroTask, JPAMacroTask.class);
        this.macroTask = (JPAMacroTask) macroTask;
    }

    @Override
    public Implementation getCommand() {
        return command;
    }

    @Override
    public void setCommand(final Implementation command) {
        checkType(command, JPAImplementation.class);
        checkImplementationType(command, IdRepoImplementationType.COMMAND);
        this.command = (JPAImplementation) command;
    }

    @Override
    public CommandArgs getArgs() {
        return Optional.ofNullable(args).
                map(a -> POJOHelper.deserialize(a, CommandArgs.class)).
                orElse(null);
    }

    @Override
    public void setArgs(final CommandArgs args) {
        this.args = Optional.ofNullable(args).map(POJOHelper::serialize).orElse(null);
    }
}

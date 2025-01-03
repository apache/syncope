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
import java.util.Optional;
import org.apache.syncope.common.lib.command.CommandArgs;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.task.MacroTask;
import org.apache.syncope.core.persistence.api.entity.task.MacroTaskCommand;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractGeneratedKeyNode;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jImplementation;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jMacroTaskCommand.NODE)
public class Neo4jMacroTaskCommand extends AbstractGeneratedKeyNode implements MacroTaskCommand {

    private static final long serialVersionUID = -8388668645348044783L;

    public static final String NODE = "MacroTaskCommand";

    @NotNull
    @Relationship(type = Neo4jMacroTask.MACRO_TASK_COMMANDS_REL, direction = Relationship.Direction.OUTGOING)
    private Neo4jMacroTask macroTask;

    @NotNull
    @Relationship(direction = Relationship.Direction.OUTGOING)
    private Neo4jImplementation command;

    private String args;

    @Override
    public Neo4jMacroTask getMacroTask() {
        return macroTask;
    }

    @Override
    public void setMacroTask(final MacroTask macroTask) {
        checkType(macroTask, Neo4jMacroTask.class);
        this.macroTask = (Neo4jMacroTask) macroTask;
    }

    @Override
    public Implementation getCommand() {
        return command;
    }

    @Override
    public void setCommand(final Implementation command) {
        checkType(command, Neo4jImplementation.class);
        checkImplementationType(command, IdRepoImplementationType.COMMAND);
        this.command = (Neo4jImplementation) command;
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

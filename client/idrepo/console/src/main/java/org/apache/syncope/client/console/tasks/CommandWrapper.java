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
package org.apache.syncope.client.console.tasks;

import java.io.Serializable;
import org.apache.syncope.common.lib.command.CommandTO;

public class CommandWrapper implements Serializable {

    private static final long serialVersionUID = -2423427579112218652L;

    private final boolean isNew;

    private CommandTO command;

    public CommandWrapper(final boolean isNew) {
        this.isNew = isNew;
    }

    public boolean isNew() {
        return isNew;
    }

    public CommandTO getCommand() {
        return command;
    }

    public CommandWrapper setCommand(final CommandTO command) {
        this.command = command;
        return this;
    }

    @Override
    public String toString() {
        return "CommandWrapper{" + "isNew=" + isNew + ", command=" + command + '}';
    }
}

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
package org.apache.syncope.client.cli.util;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.List;
import org.apache.syncope.client.cli.Command;
import org.apache.syncope.client.cli.commands.AbstractCommand;
import org.junit.Test;

public class CommandUtilsTest {

    @Test
    public void fromArgs() throws InstantiationException, IllegalAccessException {
        String commandName = "logger";
        AbstractCommand command = CommandUtils.fromArgs(commandName);
        assertEquals(commandName, command.getClass().getAnnotation(Command.class).name());

        String wrongCommandName = "wrong";
        try {
            CommandUtils.fromArgs(wrongCommandName);
            fail(wrongCommandName + " isn't a right command, why you are here?");
        } catch (IllegalArgumentException ex) {
            assertEquals(IllegalArgumentException.class, ex.getClass());
            assertEquals(wrongCommandName + " is not a valid command", ex.getMessage());
        }
    }

    @Test
    public void commands() throws InstantiationException, IllegalAccessException {
        List<AbstractCommand> commands = CommandUtils.commands();
        assertFalse(commands.isEmpty());
        assertEquals(22, commands.size());
    }
}

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
package org.apache.syncope.common.lib.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CommandTest {

    @Test
    void commandTO() {
        CommandArgs args = new CommandArgs();
        args.setCtx(Map.of("key", "value"));

        CommandTO command = new CommandTO.Builder("command").args(args).build();

        assertEquals("command", command.getKey());
        assertEquals(args, command.getArgs());
    }

    @Test
    void commandTOWithoutArgs() {
        CommandTO command = new CommandTO.Builder("command").build();

        assertEquals("command", command.getKey());
        assertTrue(command.getArgs() == null);
    }

    @Test
    void commandArgs() {
        CommandArgs args = new CommandArgs();

        assertTrue(args.getCtx().isEmpty());

        Map<String, Serializable> ctx = Map.of("key", "value");
        args.setCtx(ctx);
        assertEquals(ctx, args.getCtx());
    }

    @Test
    void commandOutput() {
        CommandArgs args = new CommandArgs();
        CommandTO command = new CommandTO.Builder("command").args(args).build();

        CommandOutput output = new CommandOutput.Builder(command).output("result").build();

        assertEquals("command", output.getKey());
        assertEquals(args, output.getArgs());
        assertEquals("result", output.getOutput());
    }

    @Test
    void commandOutputFromScratch() {
        CommandOutput output = new CommandOutput.Builder("command").output("result").build();

        assertEquals("command", output.getKey());
        assertEquals("result", output.getOutput());
    }
}

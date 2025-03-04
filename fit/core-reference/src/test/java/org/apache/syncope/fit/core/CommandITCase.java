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
package org.apache.syncope.fit.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.ws.rs.core.Response;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.command.CommandOutput;
import org.apache.syncope.common.lib.command.CommandTO;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.CommandQuery;
import org.apache.syncope.common.rest.api.beans.RealmQuery;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.core.reference.TestCommand;
import org.apache.syncope.fit.core.reference.TestCommandArgs;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CommandITCase extends AbstractITCase {

    @BeforeAll
    public static void testCommandSetup() {
        ImplementationTO command = null;
        try {
            command = IMPLEMENTATION_SERVICE.read(
                    IdRepoImplementationType.COMMAND, TestCommand.class.getSimpleName());
        } catch (SyncopeClientException e) {
            if (e.getType().getResponseStatus() == Response.Status.NOT_FOUND) {
                command = new ImplementationTO();
                command.setKey(TestCommand.class.getSimpleName());
                command.setEngine(ImplementationEngine.JAVA);
                command.setType(IdRepoImplementationType.COMMAND);
                command.setBody(TestCommand.class.getName());
                Response response = IMPLEMENTATION_SERVICE.create(command);
                command = IMPLEMENTATION_SERVICE.read(
                        command.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                assertNotNull(command);
            }
        }
        assertNotNull(command);
    }

    @Test
    public void listCommands() {
        PagedResult<CommandTO> commands = COMMAND_SERVICE.search(new CommandQuery.Builder().page(1).size(100).build());
        assertEquals(1, commands.getTotalCount());
        assertEquals(1, commands.getResult().size());

        CommandTO command = commands.getResult().getFirst();
        assertNotNull(command);
        assertEquals(TestCommand.class.getSimpleName(), command.getKey());
        assertTrue(command.getArgs() instanceof TestCommandArgs);
    }

    @Test
    public void argsValidationFailure() {
        CommandTO command = COMMAND_SERVICE.search(new CommandQuery.Builder().
                keyword(TestCommand.class.getSimpleName()).page(1).size(1).build()).getResult().getFirst();

        try {
            COMMAND_SERVICE.run(command);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.InvalidValues, e.getType());
        }
    }

    @Test
    public void runCommand() {
        CommandTO command = COMMAND_SERVICE.search(
                new CommandQuery.Builder().page(1).size(1).build()).getResult().getFirst();
        TestCommandArgs args = ((TestCommandArgs) command.getArgs());
        args.setParentRealm("/even/two");
        args.setRealmName("realm123");
        args.setPrinterName("printer124");

        CommandOutput output = COMMAND_SERVICE.run(command);
        assertNotNull(output);

        AnyObjectTO printer = null;
        try {
            printer = ANY_OBJECT_SERVICE.read(PRINTER, args.getPrinterName());
            assertNotNull(printer);
            assertEquals(args.getParentRealm() + "/" + args.getRealmName(), printer.getRealm());
            assertFalse(REALM_SERVICE.search(
                    new RealmQuery.Builder().base(printer.getRealm()).build()).getResult().isEmpty());
        } finally {
            if (printer != null) {
                ANY_OBJECT_SERVICE.delete(printer.getKey());
            }
            REALM_SERVICE.delete(args.getParentRealm() + "/" + args.getRealmName());
        }
    }
}

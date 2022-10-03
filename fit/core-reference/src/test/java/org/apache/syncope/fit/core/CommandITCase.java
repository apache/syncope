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

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.CommandTaskTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.ExecSpecs;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.core.reference.TestCommand;
import org.apache.syncope.fit.core.reference.TestCommandArgs;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CommandITCase extends AbstractITCase {

    private static String COMMAND_TASK_KEY;

    @BeforeAll
    public static void testCommandSetup() throws JsonProcessingException {
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
                command.setBody(JSON_MAPPER.writeValueAsString(new TestCommandArgs()));
                Response response = IMPLEMENTATION_SERVICE.create(command);
                command = IMPLEMENTATION_SERVICE.read(
                        command.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                assertNotNull(command);
            }
        }
        assertNotNull(command);

        if (COMMAND_TASK_KEY == null) {
            CommandTaskTO task = new CommandTaskTO();
            task.setName(TestCommand.class.getSimpleName());
            task.setActive(true);
            task.setRealm("/odd");
            task.setCommand(TestCommand.class.getSimpleName());

            Response response = TASK_SERVICE.create(TaskType.COMMAND, task);
            task = getObject(response.getLocation(), TaskService.class, CommandTaskTO.class);
            COMMAND_TASK_KEY = task.getKey();
        }
    }

    @AfterAll
    public static void cleanup() {
        TestCommandArgs args = new TestCommandArgs();
        try {
            ANY_OBJECT_SERVICE.delete(args.getPrinterName());
            REALM_SERVICE.delete(args.getParentRealm() + "/" + args.getRealmName());
        } catch (Exception e) {
            // ignore
        }
    }

    @Test
    public void runCommand() {
        int preExecs = TASK_SERVICE.read(TaskType.COMMAND, COMMAND_TASK_KEY, true).getExecutions().size();
        ExecTO execution = TASK_SERVICE.execute(new ExecSpecs.Builder().key(COMMAND_TASK_KEY).build());
        assertNotNull(execution.getExecutor());

        await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                return preExecs < TASK_SERVICE.read(TaskType.COMMAND, COMMAND_TASK_KEY, true).getExecutions().size();
            } catch (Exception e) {
                return false;
            }
        });

        TestCommandArgs args = new TestCommandArgs();
        AnyObjectTO printer = ANY_OBJECT_SERVICE.read(args.getPrinterName());
        assertNotNull(printer);
        assertEquals(args.getParentRealm() + "/" + args.getRealmName(), printer.getRealm());
        assertFalse(REALM_SERVICE.list(printer.getRealm()).isEmpty());
    }

    @Test
    public void cantRunCommand() {
        // 1. create Role for task execution
        RoleTO role = new RoleTO();
        role.setKey("new" + getUUIDString());
        role.getRealms().add("/even");
        role.getEntitlements().add(IdRepoEntitlement.TASK_EXECUTE);
        role = createRole(role);
        assertNotNull(role);

        // 2. create User with such a Role granted
        UserCR userCR = UserITCase.getUniqueSample("cantrunncommand@test.org");
        userCR.getRoles().add(role.getKey());
        UserTO userTO = createUser(userCR).getEntity();
        assertNotNull(userTO);

        // 3. attempt to run the command task -> fail
        TaskService taskService = CLIENT_FACTORY.create(
                userTO.getUsername(), "password123").getService(TaskService.class);
        try {
            taskService.execute(new ExecSpecs.Builder().key(COMMAND_TASK_KEY).build());
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.DelegatedAdministration, e.getType());
        }
    }
}

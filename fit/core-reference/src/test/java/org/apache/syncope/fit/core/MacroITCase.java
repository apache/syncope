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

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.command.CommandTO;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.MacroTaskTO;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ExecStatus;
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

public class MacroITCase extends AbstractITCase {

    private static String MACRO_TASK_KEY;

    private static final TestCommandArgs TCA = new TestCommandArgs();

    static {
        TCA.setParentRealm("/odd");
        TCA.setRealmName("macro");
        TCA.setPrinterName("aprinter112");
    }

    @BeforeAll
    public static void testCommandsSetup() throws Exception {
        CommandITCase.testCommandSetup();

        ImplementationTO transformer = null;
        try {
            transformer = IMPLEMENTATION_SERVICE.read(
                    IdRepoImplementationType.COMMAND, "GroovyCommand");
        } catch (SyncopeClientException e) {
            if (e.getType().getResponseStatus() == Response.Status.NOT_FOUND) {
                transformer = new ImplementationTO();
                transformer.setKey("GroovyCommand");
                transformer.setEngine(ImplementationEngine.GROOVY);
                transformer.setType(IdRepoImplementationType.COMMAND);
                transformer.setBody(IOUtils.toString(
                        MacroITCase.class.getResourceAsStream("/GroovyCommand.groovy"), StandardCharsets.UTF_8));
                Response response = IMPLEMENTATION_SERVICE.create(transformer);
                transformer = IMPLEMENTATION_SERVICE.read(
                        transformer.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                assertNotNull(transformer.getKey());
            }
        }
        assertNotNull(transformer);

        if (MACRO_TASK_KEY == null) {
            MacroTaskTO task = new MacroTaskTO();
            task.setName("Test Macro");
            task.setActive(true);
            task.setRealm("/odd");
            task.getCommands().add(new CommandTO.Builder("GroovyCommand").build());
            task.getCommands().add(new CommandTO.Builder(TestCommand.class.getSimpleName()).args(TCA).build());

            Response response = TASK_SERVICE.create(TaskType.MACRO, task);
            task = getObject(response.getLocation(), TaskService.class, MacroTaskTO.class);
            MACRO_TASK_KEY = task.getKey();
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
    public void execute() {
        int preExecs = TASK_SERVICE.read(TaskType.MACRO, MACRO_TASK_KEY, true).getExecutions().size();
        ExecTO execution = TASK_SERVICE.execute(new ExecSpecs.Builder().key(MACRO_TASK_KEY).build());
        assertNotNull(execution.getExecutor());

        await().atMost(MAX_WAIT_SECONDS, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                return preExecs < TASK_SERVICE.read(TaskType.MACRO, MACRO_TASK_KEY, true).getExecutions().size();
            } catch (Exception e) {
                return false;
            }
        });

        ExecTO exec = TASK_SERVICE.read(TaskType.MACRO, MACRO_TASK_KEY, true).getExecutions().get(preExecs);
        assertEquals(ExecStatus.SUCCESS.name(), exec.getStatus());

        AnyObjectTO printer = ANY_OBJECT_SERVICE.read(TCA.getPrinterName());
        assertNotNull(printer);
        assertEquals(TCA.getParentRealm() + "/" + TCA.getRealmName(), printer.getRealm());
        assertFalse(REALM_SERVICE.list(printer.getRealm()).isEmpty());
    }

    @Test
    public void cantExecute() {
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

        // 3. attempt to run the macro task -> fail
        TaskService taskService = CLIENT_FACTORY.create(
                userTO.getUsername(), "password123").getService(TaskService.class);
        try {
            taskService.execute(new ExecSpecs.Builder().key(MACRO_TASK_KEY).build());
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.DelegatedAdministration, e.getType());
        }
    }
}

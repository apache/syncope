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
package org.apache.syncope.fit.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.syncope.client.cli.Command;
import org.apache.syncope.client.cli.commands.connector.ConnectorCommand;
import org.apache.syncope.client.cli.commands.entitlement.EntitlementCommand;
import org.apache.syncope.client.cli.commands.group.GroupCommand;
import org.apache.syncope.client.cli.commands.install.InstallCommand;
import org.apache.syncope.client.cli.commands.logger.LoggerCommand;
import org.apache.syncope.client.cli.commands.policy.PolicyCommand;
import org.apache.syncope.client.cli.commands.report.ReportCommand;
import org.apache.syncope.client.cli.commands.role.RoleCommand;
import org.apache.syncope.client.cli.commands.user.UserCommand;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.BeforeClass;
import org.junit.Test;

public class CLIITCase extends AbstractITCase {

    private static final String SCRIPT_FILENAME = "syncopeadm";

    private static ProcessBuilder PROCESS_BUILDER;

    @BeforeClass
    public static void install() {
        Properties props = new Properties();
        InputStream propStream = null;
        Process process = null;
        try {
            propStream = CLIITCase.class.getResourceAsStream("/cli-test.properties");
            props.load(propStream);

            File workDir = new File(props.getProperty("cli-work.dir"));
            PROCESS_BUILDER = new ProcessBuilder().directory(workDir);

            PROCESS_BUILDER.command(getCommand(
                    new InstallCommand().getClass().getAnnotation(Command.class).name(),
                    InstallCommand.Options.SETUP_DEBUG.getOptionName()));
            process = PROCESS_BUILDER.start();
            process.waitFor();

            File cliPropertiesFile = new File(workDir + File.separator + "cli.properties");
            assertTrue(cliPropertiesFile.exists());
        } catch (IOException | InterruptedException e) {
            fail(e.getMessage());
        } finally {
            IOUtils.closeQuietly(propStream);
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static String[] getCommand(final String... arguments) {
        List<String> command = new ArrayList<>();

        if (SystemUtils.IS_OS_WINDOWS) {
            command.add("cmd.exe");
            command.add("/C");
            command.add(SCRIPT_FILENAME + ".bat");
        } else {
            command.add("/bin/bash");
            command.add(SCRIPT_FILENAME + ".sh");
        }

        command.addAll(Arrays.asList(arguments));

        return command.toArray(new String[command.size()]);
    }

    @Test
    public void runScriptWithoutOptions() {
        Process process = null;
        try {
            PROCESS_BUILDER.command(getCommand());
            process = PROCESS_BUILDER.start();

            String result = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8).
                    replaceAll("(?m)^\\s*$[\n\r]{1,}", "");
            assertTrue(result.startsWith("Usage: Main [options]"));
            assertTrue(result.contains(
                    new EntitlementCommand().getClass().getAnnotation(Command.class).name()
                    + " "
                    + EntitlementCommand.EntitlementOptions.HELP.getOptionName()));
            assertTrue(result.contains(
                    new GroupCommand().getClass().getAnnotation(Command.class).name()
                    + " "
                    + GroupCommand.GroupOptions.HELP.getOptionName()));
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    @Test
    public void entitlementCount() {
        Process process = null;
        try {
            PROCESS_BUILDER.command(getCommand(
                    new EntitlementCommand().getClass().getAnnotation(Command.class).name(),
                    EntitlementCommand.EntitlementOptions.LIST.getOptionName()));
            process = PROCESS_BUILDER.start();

            long entitlements = IOUtils.readLines(process.getInputStream(), StandardCharsets.UTF_8).
                    stream().filter(line -> line.startsWith("-")).count();
            assertEquals(syncopeService.platform().getEntitlements().size(), entitlements);
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    @Test
    public void connectorCount() {
        Process process = null;
        try {
            PROCESS_BUILDER.command(getCommand(
                    new ConnectorCommand().getClass().getAnnotation(Command.class).name(),
                    ConnectorCommand.ConnectorOptions.LIST_BUNDLES.getOptionName()));
            process = PROCESS_BUILDER.start();

            long bundles = IOUtils.readLines(process.getInputStream(), StandardCharsets.UTF_8).
                    stream().filter(line -> line.startsWith(" > BUNDLE NAME:")).count();
            assertEquals(connectorService.getBundles(null).size(), bundles);
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    @Test
    public void userRead() {
        final String userKey1 = "1417acbe-cbf6-4277-9372-e75e04f97000";
        final String userKey2 = "74cd8ece-715a-44a4-a736-e17b46c4e7e6";
        final String userKey3 = "b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee";
        final String userKey4 = "c9b2dec2-00a7-4855-97c0-d854842b4b24";
        final String userKey5 = "823074dc-d280-436d-a7dd-07399fae48ec";
        Process process1 = null;
        Process process2 = null;
        Process process3 = null;
        try {
            PROCESS_BUILDER.command(getCommand(
                    new UserCommand().getClass().getAnnotation(Command.class).name(),
                    UserCommand.UserOptions.READ_BY_KEY.getOptionName(),
                    String.valueOf(userKey1)));
            process1 = PROCESS_BUILDER.start();
            String result = IOUtils.toString(process1.getInputStream(), StandardCharsets.UTF_8);
            assertTrue(result.contains("username: " + userService.read(userKey1).getUsername()));

            PROCESS_BUILDER.command(getCommand(
                    new UserCommand().getClass().getAnnotation(Command.class).name(),
                    UserCommand.UserOptions.READ_BY_KEY.getOptionName(),
                    String.valueOf(userKey1), String.valueOf(userKey2),
                    String.valueOf(userKey3), String.valueOf(userKey4), String.valueOf(userKey5)));
            process2 = PROCESS_BUILDER.start();
            long users = IOUtils.readLines(process2.getInputStream(), StandardCharsets.UTF_8).
                    stream().filter(line -> line.startsWith(" > USER KEY:")).count();
            assertEquals(5, users);

            PROCESS_BUILDER.command(getCommand(
                    new UserCommand().getClass().getAnnotation(Command.class).name(),
                    UserCommand.UserOptions.READ_BY_KEY.getOptionName(),
                    String.valueOf(userKey1), String.valueOf(userKey2),
                    String.valueOf(userKey3), String.valueOf(userKey4), String.valueOf(userKey5)));
            process3 = PROCESS_BUILDER.start();
            String result3 = IOUtils.toString(process3.getInputStream(), StandardCharsets.UTF_8);
            assertTrue(
                    result3.contains("username: " + userService.read(userKey1).getUsername())
                    && result3.contains("username: " + userService.read(userKey2).getUsername())
                    && result3.contains("username: " + userService.read(userKey3).getUsername())
                    && result3.contains("username: " + userService.read(userKey4).getUsername())
                    && result3.contains("username: " + userService.read(userKey5).getUsername()));
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            if (process1 != null) {
                process1.destroy();
            }
            if (process2 != null) {
                process2.destroy();
            }
            if (process3 != null) {
                process3.destroy();
            }
        }
    }

    @Test
    public void roleRead() {
        final String roleId = "Search for realm evenTwo";
        Process process = null;
        try {
            PROCESS_BUILDER.command(getCommand(
                    new RoleCommand().getClass().getAnnotation(Command.class).name(),
                    RoleCommand.RoleOptions.READ.getOptionName(),
                    roleId));
            process = PROCESS_BUILDER.start();
            final String result = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
            assertTrue(result.contains(roleService.read(roleId).getEntitlements().iterator().next()));
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    @Test
    public void reportNotExists() {
        Process process = null;
        try {
            PROCESS_BUILDER.command(getCommand(
                    new ReportCommand().getClass().getAnnotation(Command.class).name(),
                    ReportCommand.ReportOptions.READ.getOptionName(),
                    "72"));
            process = PROCESS_BUILDER.start();
            final String result = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
            assertTrue(result.contains("- Report 72 doesn't exist"));
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    @Test
    public void policyError() {
        Process process = null;
        try {
            PROCESS_BUILDER.command(getCommand(
                    new PolicyCommand().getClass().getAnnotation(Command.class).name(),
                    PolicyCommand.PolicyOptions.READ.getOptionName(),
                    "wrong"));
            process = PROCESS_BUILDER.start();
            final String result = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
            assertTrue(result.contains("- Policy wrong doesn't exist"));
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    @Test
    public void lastStatements() {
        Process process = null;
        try {
            PROCESS_BUILDER.command(getCommand(
                    new LoggerCommand().getClass().getAnnotation(Command.class).name(),
                    LoggerCommand.LoggerOptions.LAST_STATEMENTS.getOptionName(),
                    "connid"));
            process = PROCESS_BUILDER.start();
            final String result = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
            assertTrue(result.contains("\"level\" : \"DEBUG\","));
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }
}

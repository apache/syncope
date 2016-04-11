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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.syncope.client.cli.Command;
import org.apache.syncope.client.cli.commands.connector.ConnectorCommand;
import org.apache.syncope.client.cli.commands.entitlement.EntitlementCommand;
import org.apache.syncope.client.cli.commands.group.GroupCommand;
import org.apache.syncope.client.cli.commands.install.InstallCommand;
import org.apache.syncope.client.cli.commands.policy.PolicyCommand;
import org.apache.syncope.client.cli.commands.report.ReportCommand;
import org.apache.syncope.client.cli.commands.role.RoleCommand;
import org.apache.syncope.client.cli.commands.user.UserCommand;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class CLIITCase extends AbstractITCase {

    private static final String SCRIPT_FILENAME = "syncopeadm";

    private static ProcessBuilder PROCESS_BUILDER;

    @BeforeClass
    public static void install() {
        Properties props = new Properties();
        InputStream propStream = null;
        try {
            propStream = CLIITCase.class.getResourceAsStream("/cli-test.properties");
            props.load(propStream);

            File workDir = new File(props.getProperty("cli-work.dir"));
            PROCESS_BUILDER = new ProcessBuilder();
            PROCESS_BUILDER.directory(workDir);

            PROCESS_BUILDER.command(getCommand(
                    new InstallCommand().getClass().getAnnotation(Command.class).name(),
                    InstallCommand.Options.SETUP_DEBUG.getOptionName()));
            Process process = PROCESS_BUILDER.start();
            process.waitFor();

            File cliPropertiesFile = new File(workDir + File.separator + "cli.properties");
            assertTrue(cliPropertiesFile.exists());
        } catch (IOException | InterruptedException e) {
            fail(e.getMessage());
        } finally {
            IOUtils.closeQuietly(propStream);
        }
    }

    private static String[] getCommand(final String... arguments) {
        List<String> command = new ArrayList<>();

        if (SystemUtils.IS_OS_WINDOWS) {
            command.add("cmd");
            command.add(SCRIPT_FILENAME + ".bat");
        } else {
            command.add("/bin/bash");
            command.add(SCRIPT_FILENAME + ".sh");
        }

        CollectionUtils.addAll(command, arguments);

        return command.toArray(new String[command.size()]);
    }

    @Test
    public void runScriptWithoutOptions() {
        try {
            PROCESS_BUILDER.command(getCommand());
            Process process = PROCESS_BUILDER.start();

            String result = IOUtils.toString(process.getInputStream());
            assertTrue(result.startsWith("\nUsage: Main [options]"));
            assertTrue(result.contains(
                    new EntitlementCommand().getClass().getAnnotation(Command.class).name()
                    + " "
                    + EntitlementCommand.EntitlementOptions.HELP.getOptionName()));
            assertTrue(result.contains(
                    new GroupCommand().getClass().getAnnotation(Command.class).name()
                    + " "
                    + GroupCommand.GroupOptions.HELP.getOptionName()));
            process.destroy();
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void entitlementCount() {
        try {
            PROCESS_BUILDER.command(getCommand(
                    new EntitlementCommand().getClass().getAnnotation(Command.class).name(),
                    EntitlementCommand.EntitlementOptions.LIST.getOptionName()));
            Process process = PROCESS_BUILDER.start();

            long entitlements = IterableUtils.countMatches(IOUtils.readLines(process.getInputStream()),
                    new Predicate<String>() {

                @Override
                public boolean evaluate(final String line) {
                    return line.startsWith("-");
                }
            });
            assertEquals(syncopeService.platform().getEntitlements().size(), entitlements);

            process.destroy();
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void connectorCount() {
        try {
            PROCESS_BUILDER.command(getCommand(
                    new ConnectorCommand().getClass().getAnnotation(Command.class).name(),
                    ConnectorCommand.ConnectorOptions.LIST_BUNDLES.getOptionName()));
            Process process = PROCESS_BUILDER.start();

            long bundles = IterableUtils.countMatches(IOUtils.readLines(process.getInputStream()),
                    new Predicate<String>() {

                @Override
                public boolean evaluate(final String line) {
                    return line.startsWith(" > BUNDLE NAME:");
                }
            });
            assertEquals(connectorService.getBundles(null).size(), bundles);

            process.destroy();
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void userRead() {
        final long userId1 = 1;
        final long userId2 = 2;
        final long userId3 = 3;
        final long userId4 = 4;
        final long userId5 = 5;
        try {
            PROCESS_BUILDER.command(getCommand(
                    new UserCommand().getClass().getAnnotation(Command.class).name(),
                    UserCommand.UserOptions.READ_BY_ID.getOptionName(),
                    String.valueOf(userId1)));
            Process process = PROCESS_BUILDER.start();
            String result = IOUtils.toString(process.getInputStream());
            assertTrue(result.contains("username: " + userService.read(userId1).getUsername()));
            process.destroy();

            PROCESS_BUILDER.command(getCommand(
                    new UserCommand().getClass().getAnnotation(Command.class).name(),
                    UserCommand.UserOptions.READ_BY_ID.getOptionName(),
                    String.valueOf(userId1), String.valueOf(userId2),
                    String.valueOf(userId3), String.valueOf(userId4), String.valueOf(userId5)));
            Process process2 = PROCESS_BUILDER.start();
            long users = IterableUtils.countMatches(IOUtils.readLines(process2.getInputStream()),
                    new Predicate<String>() {

                @Override
                public boolean evaluate(final String line) {
                    return line.startsWith(" > USER ID:");
                }
            });
            assertEquals(5, users);

            process2.destroy();

            PROCESS_BUILDER.command(getCommand(
                    new UserCommand().getClass().getAnnotation(Command.class).name(),
                    UserCommand.UserOptions.READ_BY_ID.getOptionName(),
                    String.valueOf(userId1), String.valueOf(userId2),
                    String.valueOf(userId3), String.valueOf(userId4), String.valueOf(userId5)));
            Process process3 = PROCESS_BUILDER.start();
            String result3 = IOUtils.toString(process3.getInputStream());
            assertTrue(
                    result3.contains("username: " + userService.read(userId1).getUsername())
                    && result3.contains("username: " + userService.read(userId2).getUsername())
                    && result3.contains("username: " + userService.read(userId3).getUsername())
                    && result3.contains("username: " + userService.read(userId4).getUsername())
                    && result3.contains("username: " + userService.read(userId5).getUsername()));
            process3.destroy();
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void roleRead() {
        final String roleId = "Search for realm evenTwo";
        try {
            PROCESS_BUILDER.command(getCommand(
                    new RoleCommand().getClass().getAnnotation(Command.class).name(),
                    RoleCommand.RoleOptions.READ.getOptionName(),
                    roleId));
            final Process process = PROCESS_BUILDER.start();
            final String result = IOUtils.toString(process.getInputStream());
            assertTrue(result.contains(roleService.read(roleId).getEntitlements().iterator().next()));

            process.destroy();
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void reportNotExists() {
        try {
            PROCESS_BUILDER.command(getCommand(
                    new ReportCommand().getClass().getAnnotation(Command.class).name(),
                    ReportCommand.ReportOptions.READ.getOptionName(),
                    "72"));
            final Process process = PROCESS_BUILDER.start();
            final String result = IOUtils.toString(process.getInputStream());
            assertTrue(result.contains("- Report 72 doesn't exist"));

            process.destroy();
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void policyError() {
        try {
            PROCESS_BUILDER.command(getCommand(
                    new PolicyCommand().getClass().getAnnotation(Command.class).name(),
                    PolicyCommand.PolicyOptions.READ.getOptionName(),
                    "wrong"));
            final Process process = PROCESS_BUILDER.start();
            final String result = IOUtils.toString(process.getInputStream());
            assertTrue(result.contains(
                    "- Error reading wrong. It isn't a valid policy value because it isn't a boolean value"));

            process.destroy();
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
}

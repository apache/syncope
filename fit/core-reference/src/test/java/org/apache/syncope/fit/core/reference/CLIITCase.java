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
package org.apache.syncope.fit.core.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class CLIITCase extends AbstractITCase {

    private static final String LINUX_SCRIPT_DIR = "/target/cli-test/syncope-client-cli-2.0.0-SNAPSHOT";

    private static final String LINUX_SCRIPT_FILENAME = "syncopeadm.sh";

    private static ProcessBuilder processBuilder;

    @BeforeClass
    public static void install() {
        try {
            final File f = new File(".");
            final File buildDirectory = new File(f.getCanonicalPath() + LINUX_SCRIPT_DIR);
            processBuilder = new ProcessBuilder();
            processBuilder.directory(buildDirectory);
            final String[] command = {"/bin/bash", LINUX_SCRIPT_FILENAME, "install", "--setup-debug"};
            processBuilder.command(command);
            final Process process = processBuilder.start();
            process.waitFor();
            final File cliPropertiesFile = new File(buildDirectory + "/cli.properties");
            assertTrue(cliPropertiesFile.exists());
        } catch (final IOException | InterruptedException ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void runScriptWithoutOptions() {
        try {
            final String[] command = {"/bin/bash", LINUX_SCRIPT_FILENAME};
            processBuilder.command(command);
            final Process process = processBuilder.start();
            final String result = readScriptOutput(process.getInputStream());
            assertTrue(result.startsWith("\nUsage: Main [options]"));
            assertTrue(result.contains("entitlement --help"));
            assertTrue(result.contains("group --help"));
            process.destroy();
        } catch (IOException ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void entitlementCount() {
        try {
            final String[] command = {"/bin/bash", LINUX_SCRIPT_FILENAME, "entitlement", "--list"};
            processBuilder.command(command);
            final Process process = processBuilder.start();
            final BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            int entitlementsNumber = 0;
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("-")) {
                    entitlementsNumber++;
                }
            }
            assertEquals(112, entitlementsNumber);
        } catch (IOException ex) {
            fail(ex.getMessage());
        }
    }

    @Test
    public void connectorCount() {
        try {
            final String[] command = {"/bin/bash", LINUX_SCRIPT_FILENAME, "connector", "--list-bundles"};
            processBuilder.command(command);
            final Process process = processBuilder.start();
            final BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            int bundlesNumber = 0;
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(" > BUNDLE NAME:")) {
                    bundlesNumber++;
                }
            }
            assertEquals(8, bundlesNumber);
        } catch (IOException ex) {
            fail(ex.getMessage());
        }
    }

    private static String readScriptOutput(final InputStream inputStream) throws IOException {
        final BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        final StringBuilder resultBuilder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            resultBuilder.append(line).append("\n");
        }
        return resultBuilder.toString();
    }
}

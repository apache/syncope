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
package org.apache.syncope.client.cli;

import java.io.File;
import org.apache.syncope.client.cli.commands.install.InstallConfigFileTemplate;

public final class ArgsManager {

    public static void validator(final String[] args) throws IllegalArgumentException {
        if (args.length == 0) {
            throw new IllegalArgumentException("Syntax error: no options");
        } else if (!"install".equalsIgnoreCase(args[0])) {
            final File configFile = new File(InstallConfigFileTemplate.configurationFilePath());
            if (!configFile.exists()) {
                throw new IllegalArgumentException(
                        "It seems you need to first setup the CLI client. Run install --setup.");
            }
        }
    }

    public static String[] operands(final String[] args) {
        final String[] operands = new String[args.length - 1];
        for (int i = 1; i < args.length; i++) {
            operands[i - 1] = args[i];
        }
        return operands;
    }

    private ArgsManager() {
        // private constructor for static utility class
    }

}

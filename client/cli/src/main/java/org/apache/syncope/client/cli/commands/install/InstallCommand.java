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
package org.apache.syncope.client.cli.commands.install;

import java.io.FileNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.cli.Command;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.commands.AbstractCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(name = "install")
public class InstallCommand extends AbstractCommand {

    private static final Logger LOG = LoggerFactory.getLogger(InstallCommand.class);

    private final InstallResultManager installResultManager = new InstallResultManager();

    private static final String HELP_MESSAGE = "\nUsage: install [options]\n"
            + "  Options:\n"
            + "    --help \n"
            + "    --setup\n"
            + "    --setup-debug\n";

    @Override
    public void execute(final Input input) {
        if (StringUtils.isBlank(input.getOption())) {
            input.setOption(Options.HELP.getOptionName());
        }

        switch (Options.fromName(input.getOption())) {
            case SETUP:
                try {
                    new InstallSetup().setup();
                } catch (final FileNotFoundException | IllegalAccessException ex) {
                    LOG.error("Error installing CLI", ex);
                    installResultManager.genericError(ex.getMessage());
                    break;
                }
                break;
            case SETUP_DEBUG:
                try {
                    new InstallSetupForDebug().setup();
                } catch (final FileNotFoundException | IllegalAccessException ex) {
                    LOG.error("Error installing CLI", ex);
                    installResultManager.genericError(ex.getMessage());
                    break;
                }
                break;
            case HELP:
                System.out.println(HELP_MESSAGE);
                break;
            default:
                installResultManager.defaultOptionMessage(input.getOption(), HELP_MESSAGE);
        }
    }

    @Override
    public String getHelpMessage() {
        return HELP_MESSAGE;
    }

    public enum Options {

        HELP("--help"),
        SETUP("--setup"),
        SETUP_DEBUG("--setup-debug");

        private final String optionName;

        Options(final String optionName) {
            this.optionName = optionName;
        }

        public String getOptionName() {
            return optionName;
        }

        public boolean equalsOptionName(final String otherName) {
            return (otherName == null) ? false : optionName.equals(otherName);
        }

        public static Options fromName(final String name) {
            Options optionToReturn = HELP;
            for (final Options option : Options.values()) {
                if (option.equalsOptionName(name)) {
                    optionToReturn = option;
                }
            }
            return optionToReturn;
        }
    }
}

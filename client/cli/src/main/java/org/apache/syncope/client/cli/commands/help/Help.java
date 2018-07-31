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
package org.apache.syncope.client.cli.commands.help;

import org.apache.syncope.client.cli.Command;
import org.apache.syncope.client.cli.commands.AbstractCommand;
import org.apache.syncope.client.cli.util.CommandUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Help {

    private static final Logger LOG = LoggerFactory.getLogger(Help.class);

    private final HelpResultManager helpResultManager = new HelpResultManager();

    public void help() {
        final StringBuilder generalHelpBuilder = new StringBuilder("General help\n");
        try {
            for (final AbstractCommand command : CommandUtils.commands()) {
                generalHelpBuilder.append("Command: ")
                        .append(command.getClass().getAnnotation(Command.class).name())
                        .append("\n")
                        .append(command.getHelpMessage())
                        .append("\n")
                        .append(" \n");
            }
            helpResultManager.toView(generalHelpBuilder.toString());
        } catch (Exception e) {
            LOG.error("Error helping", e);
            helpResultManager.genericMessage(e.getMessage());
        }
    }
}

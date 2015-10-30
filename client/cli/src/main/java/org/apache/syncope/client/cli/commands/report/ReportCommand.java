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
package org.apache.syncope.client.cli.commands.report;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.cli.Command;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.commands.AbstractCommand;

@Command(name = "report")
public class ReportCommand extends AbstractCommand {

    private static final String HELP_MESSAGE = "Usage: report [options]\n"
            + "  Options:\n"
            + "    --help \n"
            + "    --details\n"
            + "    --list \n"
            + "    --list-jobs \n"
            + "    --read \n"
            + "       Syntax: --read {REPORT-ID} {REPORT-ID} [...] \n"
            + "    --delete \n"
            + "       Syntax: --delete {REPORT-ID} {REPORT-ID} [...]\n"
            + "    --execute \n"
            + "       Syntax: --execute {REPORT-ID} \n"
            + "    --read-execution \n"
            + "       Syntax: --read-execution {EXECUTION-ID} {EXECUTION-ID} [...]\n"
            + "    --delete-execution \n"
            + "       Syntax: --delete-execution {EXECUTION-ID} {EXECUTION-ID} [...]\n"
            + "    --export-execution-result \n"
            + "       Syntax: --export-execution-result {EXECUTION-ID} {EXECUTION-ID} [...] {FORMAT}\n"
            + "          Format: CSV / HTML / PDF / XML / RTF";

    @Override
    public void execute(final Input input) {
        if (StringUtils.isBlank(input.getOption())) {
            input.setOption(Options.HELP.getOptionName());
        }

        switch (Options.fromName(input.getOption())) {
            case LIST:
                new ReportList(input).list();
                break;
            case DETAILS:
                new ReportDetails(input).details();
                break;
            case LIST_JOBS:
                new ReportListJobs(input).list();
                break;
            case READ:
                new ReportRead(input).read();
                break;
            case DELETE:
                new ReportDelete(input).delete();
                break;
            case EXECUTE:
                new ReportExecute(input).execute();
                break;
            case READ_EXECUTION:
                new ReportReadExecution(input).read();
                break;
            case DELETE_EXECUTION:
                new ReportDeleteExecution(input).delete();
                break;
            case EXPORT_EXECUTION_RESULT:
                new ReportExportExecution(input).export();
                break;
            case HELP:
                System.out.println(HELP_MESSAGE);
                break;
            default:
                new ReportResultManager().defaultError(input.getOption(), HELP_MESSAGE);
        }
    }

    @Override
    public String getHelpMessage() {
        return HELP_MESSAGE;
    }

    private enum Options {

        HELP("--help"),
        DETAILS("--details"),
        LIST("--list"),
        LIST_JOBS("--list-jobs"),
        READ("--read"),
        DELETE("--delete"),
        EXECUTE("--execute"),
        READ_EXECUTION("--read-execution"),
        DELETE_EXECUTION("--delete-execution"),
        EXPORT_EXECUTION_RESULT("--export-execution-result");

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

        public static List<String> toList() {
            final List<String> options = new ArrayList<>();
            for (final Options value : values()) {
                options.add(value.getOptionName());
            }
            return options;
        }
    }
}

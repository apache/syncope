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

    private final ReportResultManager reportResultManager = new ReportResultManager();

    @Override
    public void execute(final Input input) {
        if (StringUtils.isBlank(input.getOption())) {
            input.setOption(ReportOptions.HELP.getOptionName());
        }

        switch (ReportOptions.fromName(input.getOption())) {
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
            case DELETE_EXECUTION:
                new ReportDeleteExecution(input).delete();
                break;
            case EXPORT_EXECUTION_RESULT:
                new ReportExportExecution(input).export();
                break;
            case HELP:
                System.out.println(getHelpMessage());
                break;
            default:
                reportResultManager.defaultOptionMessage(input.getOption(), getHelpMessage());
        }
    }

    @Override
    public String getHelpMessage() {
        return reportResultManager.commandHelpMessage(getClass());
    }

    public enum ReportOptions {

        HELP("--help"),
        DETAILS("--details"),
        LIST("--list"),
        LIST_JOBS("--list-jobs"),
        READ("--read"),
        DELETE("--delete"),
        EXECUTE("--execute"),
        DELETE_EXECUTION("--delete-execution"),
        EXPORT_EXECUTION_RESULT("--export-execution-result");

        private final String optionName;

        ReportOptions(final String optionName) {
            this.optionName = optionName;
        }

        public String getOptionName() {
            return optionName;
        }

        public boolean equalsOptionName(final String otherName) {
            return (otherName == null) ? false : optionName.equals(otherName);
        }

        public static ReportOptions fromName(final String name) {
            ReportOptions optionToReturn = HELP;
            for (final ReportOptions option : ReportOptions.values()) {
                if (option.equalsOptionName(name)) {
                    optionToReturn = option;
                }
            }
            return optionToReturn;
        }

        public static List<String> toList() {
            final List<String> options = new ArrayList<>();
            for (final ReportOptions value : values()) {
                options.add(value.getOptionName());
            }
            return options;
        }
    }
}

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
package org.apache.syncope.client.cli.commands;

import java.io.IOException;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.ws.WebServiceException;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.cli.Command;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.client.cli.messages.Messages;
import org.apache.syncope.client.cli.util.CommandUtils;
import org.apache.syncope.client.cli.util.XMLUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ReportExecTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.types.JobStatusType;
import org.apache.syncope.common.lib.types.ReportExecExportFormat;
import org.apache.syncope.common.rest.api.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

@Command(name = "report")
public class ReportCommand extends AbstractCommand {

    private static final Logger LOG = LoggerFactory.getLogger(ReportCommand.class);

    private static final String HELP_MESSAGE = "Usage: report [options]\n"
            + "  Options:\n"
            + "    --help \n"
            + "    --list \n"
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
            + "          Format: CSV / HTML / PDF / XML / RTF"
            + "    --reportlet-class";
    
    @Override
    public void execute(final Input input) {
        LOG.debug("Option: {}", input.getOption());
        LOG.debug("Parameters:");
        for (final String parameter : input.getParameters()) {
            LOG.debug("   > " + parameter);
        }

        String[] parameters = input.getParameters();

        if (StringUtils.isBlank(input.getOption())) {
            input.setOption(Options.HELP.getOptionName());
        }

        final ReportService reportService = SyncopeServices.get(ReportService.class);
        switch (Options.fromName(input.getOption())) {
            case LIST:
                try {
                    for (final ReportTO reportTO : reportService.list()) {
                        System.out.println(reportTO);
                    }
                } catch (final SyncopeClientException ex) {
                    Messages.printMessage(ex.getMessage());
                }
                break;
            case LIST_JOBS:
                try {
                    for (final JobStatusType jobStatusType : JobStatusType.values()) {
                        System.out.println("Report execution for " + jobStatusType);
                        final List<ReportExecTO> reportExecTOs = reportService.listJobs(jobStatusType);
                        for (final ReportExecTO reportExecTO : reportExecTOs) {
                            System.out.println(" - Report execution id: " + reportExecTO.getKey());
                            System.out.println(" - Report execution status: " + reportExecTO.getStatus());
                            System.out.println(" - Report execution start date: " + reportExecTO.getStartDate());
                            System.out.println(" - Report execution end date: " + reportExecTO.getEndDate());
                            System.out.println();
                        }
                    }
                } catch (final SyncopeClientException ex) {
                    Messages.printMessage(ex.getMessage());
                }
                break;
            case READ:
                final String readErrorMessage = "report --read {REPORT-ID} {REPORT-ID} [...]";
                if (parameters.length >= 1) {
                    for (final String parameter : parameters) {
                        try {
                            System.out.println(reportService.read(Long.valueOf(parameter)));
                        } catch (final NumberFormatException ex) {
                            Messages.printIdNotNumberDeletedMessage("report", parameter);
                        } catch (final WebServiceException | SyncopeClientException ex) {
                            if (ex.getMessage().startsWith("NotFound")) {
                                Messages.printNofFoundMessage("Report", parameter);
                            } else {
                                Messages.printMessage(ex.getMessage());
                            }
                        }
                    }
                } else {
                    Messages.printCommandOptionMessage(readErrorMessage);
                }
                break;
            case DELETE:
                final String deleteErrorMessage = "report --delete {REPORT-ID} {REPORT-ID} [...]";

                if (parameters.length >= 1) {
                    for (final String parameter : parameters) {
                        try {
                            reportService.delete(Long.valueOf(parameter));
                            Messages.printDeletedMessage("Report", parameter);
                        } catch (final WebServiceException | SyncopeClientException ex) {
                            if (ex.getMessage().startsWith("NotFound")) {
                                Messages.printNofFoundMessage("Report", parameter);
                            } else if (ex.getMessage().startsWith("DataIntegrityViolation")) {
                                Messages.printMessage("You cannot delete report " + parameter);
                            } else {
                                Messages.printMessage(ex.getMessage());
                            }
                        } catch (final NumberFormatException ex) {
                            Messages.printIdNotNumberDeletedMessage("report", parameter);
                        }
                    }
                } else {
                    Messages.printCommandOptionMessage(deleteErrorMessage);
                }
                break;
            case EXECUTE:
                final String executeErrorMessage = "report --execute {REPORT-ID}";

                if (parameters.length == 1) {

                    try {
                        final Long reportIdToExecute = Long.valueOf(parameters[0]);
                        reportService.execute(reportIdToExecute);
                        final List<ReportExecTO> executionList
                                = reportService.read(reportIdToExecute).getExecutions();
                        final ReportExecTO lastExecution = executionList.get(executionList.size() - 1);
                        System.out.println(" - Report execution id: " + lastExecution.getKey());
                        System.out.println(" - Report execution status: " + lastExecution.getStatus());
                        System.out.println(" - Report execution start date: " + lastExecution.getStartDate());
                    } catch (final WebServiceException | SyncopeClientException ex) {
                        System.out.println("Error:");
                        if (ex.getMessage().startsWith("NotFound")) {
                            Messages.printNofFoundMessage("Report", parameters[0]);
                        } else if (ex.getMessage().startsWith("DataIntegrityViolation")) {
                            Messages.printMessage("You cannot delete report " + parameters[0]);
                        } else {
                            Messages.printMessage(ex.getMessage());
                        }
                    } catch (final NumberFormatException ex) {
                        Messages.printIdNotNumberDeletedMessage("report", parameters[0]);
                    }
                } else {
                    Messages.printCommandOptionMessage(executeErrorMessage);
                }
                break;
            case READ_EXECUTION:
                final String readExecutionErrorMessage = "report --read-execution {EXECUTION-ID} {EXECUTION-ID} [...]";

                if (parameters.length >= 1) {
                    for (final String parameter : parameters) {

                        try {
                            ReportExecTO reportExecTO = reportService.readExecution(Long.valueOf(parameter));
                            System.out.println(" - Report execution id: " + reportExecTO.getKey());
                            System.out.println(" - Report execution status: " + reportExecTO.getStatus());
                            System.out.println(" - Report execution start date: " + reportExecTO.getStartDate());
                            System.out.println(" - Report execution end date: " + reportExecTO.getEndDate());
                        } catch (final WebServiceException | SyncopeClientException ex) {
                            System.out.println("Error:");
                            if (ex.getMessage().startsWith("NotFound")) {
                                Messages.printNofFoundMessage("Report", parameter);
                            } else {
                                Messages.printMessage(ex.getMessage());
                            }
                        } catch (final NumberFormatException ex) {
                            Messages.printIdNotNumberDeletedMessage("report", parameter);
                        }
                    }
                } else {
                    Messages.printCommandOptionMessage(readExecutionErrorMessage);
                }
                break;
            case DELETE_EXECUTION:
                final String deleteExecutionErrorMessage
                        = "report --delete-execution {EXECUTION-ID} {EXECUTION-ID} [...]";

                if (parameters.length >= 1) {
                    for (final String parameter : parameters) {

                        try {
                            reportService.deleteExecution(Long.valueOf(parameter));
                            Messages.printDeletedMessage("Report execution", parameter);
                        } catch (final WebServiceException | SyncopeClientException ex) {
                            if (ex.getMessage().startsWith("NotFound")) {
                                Messages.printNofFoundMessage("Report", parameter);
                            } else if (ex.getMessage().startsWith("DataIntegrityViolation")) {
                                System.out.println(" - You cannot delete report " + parameter);
                            } else {
                                System.out.println(ex.getMessage());
                            }
                        } catch (final NumberFormatException ex) {
                            Messages.printIdNotNumberDeletedMessage("report", parameter);
                        }
                    }
                } else {
                    Messages.printCommandOptionMessage(deleteExecutionErrorMessage);
                }
                break;
            case EXPORT_EXECUTION_RESULT:
                final String exportExecutionErrorMessage
                        = "report --export-execution-result {EXECUTION-ID} {EXECUTION-ID} [...] {FORMAT}\n"
                        + "          Format: CSV / HTML / PDF / XML / RTF";

                if (parameters.length >= 2) {
                    parameters = Arrays.copyOf(parameters, parameters.length - 1);
                    for (final String parameter : parameters) {
                        try {
                            final ReportExecExportFormat format = ReportExecExportFormat.valueOf(input.lastParameter());
                            final Long exportId = Long.valueOf(parameter);
                            final SequenceInputStream report = (SequenceInputStream) reportService.
                                    exportExecutionResult(exportId, format).getEntity();
                            switch (format) {
                                case XML:
                                    final String xmlFinalName = "export_" + exportId + ".xml";
                                    XMLUtils.createXMLFile(report, xmlFinalName);
                                    Messages.printMessage(xmlFinalName + " successfully created");
                                    break;
                                case CSV:
                                    Messages.printMessage(format + " doesn't supported");
                                    break;
                                case PDF:
                                    Messages.printMessage(format + " doesn't supported");
                                    break;
                                case HTML:
                                    Messages.printMessage(format + " doesn't supported");
                                    break;
                                case RTF:
                                    Messages.printMessage(format + " doesn't supported");
                                    break;
                                default:
                                    Messages.printMessage(format + " doesn't supported");
                                    break;
                            }
                        } catch (final WebServiceException | SyncopeClientException ex) {
                            if (ex.getMessage().startsWith("NotFound")) {
                                Messages.printNofFoundMessage("Report", parameter);
                            } else {
                                System.out.println(ex.getMessage());
                            }
                        } catch (final NumberFormatException ex) {
                            Messages.printIdNotNumberDeletedMessage("report", parameter);
                        } catch (IOException | ParserConfigurationException | SAXException | TransformerException e) {
                            System.out.println(" - Error creating " + "export_" + parameter + " " + e.getMessage());
                        } catch (final IllegalArgumentException ex) {
                            Messages.printTypeNotValidMessage(
                                    "format", input.firstParameter(),
                                    CommandUtils.fromEnumToArray(ReportExecExportFormat.class));
                        }
                        break;
                    }
                } else {
                    Messages.printCommandOptionMessage(exportExecutionErrorMessage);
                }
                break;
            case HELP:
                System.out.println(HELP_MESSAGE);
                break;
            default:
                Messages.printDefaultMessage(input.getOption(), HELP_MESSAGE);
        }
    }

    @Override
    public String getHelpMessage() {
        return HELP_MESSAGE;
    }

    private enum Options {

        HELP("--help"),
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

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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.client.cli.util.XMLUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ReportExecTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.types.ReportExecExportFormat;
import org.apache.syncope.common.rest.api.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

@Parameters(
        commandNames = "report",
        optionPrefixes = "-",
        separators = "=",
        commandDescription = "Apache Syncope report service")
public class ReportCommand extends AbstractCommand {

    private static final Logger LOG = LoggerFactory.getLogger(ReportCommand.class);

    private final String helpMessage = "Usage: report [options]\n"
            + "  Options:\n"
            + "    -h, --help \n"
            + "    -l, --list \n"
            + "    -r, --read \n"
            + "       Syntax: -r={POLICY-ID} \n"
            + "    -d, --delete \n"
            + "       Syntax: -d={POLICY-ID} \n"
            + "    -e, --execute \n"
            + "       Syntax: -e={POLICY-ID} \n"
            + "    -re, --read-executecution \n"
            + "       Syntax: -re={EXECUTION-ID} \n"
            + "    -de, --delete-executecution \n"
            + "       Syntax: -de={EXECUTION-ID} \n"
            + "    -eer, --export-executecution-result \n"
            + "       Syntax: -eer={EXECUTION-ID} \n"
            + "    -rc, --reportlet-class";

    @Parameter(names = { "-r", "--read" })
    private Long reportIdToRead = -1L;

    @Parameter(names = { "-d", "--delete" })
    private Long reportIdToDelete = -1L;

    @Parameter(names = { "-e", "--execute" })
    private Long reportIdToExecute = -1L;

    @Parameter(names = { "-re", "--read-execution" })
    private Long executionIdToRead = -1L;

    @Parameter(names = { "-de", "--delete-execution" })
    private Long executionIdToDelete = -1L;

    @Parameter(names = { "-eer", "--export-execution-result" })
    private Long exportId = -1L;

    @Override
    public void execute() {
        final ReportService reportService = SyncopeServices.get(ReportService.class);
        LOG.debug("Report service successfully created");

        if (help) {
            LOG.debug("- report help command");
            System.out.println(helpMessage);
        } else if (list) {
            LOG.debug("- report list command");
            try {
                for (ReportTO reportTO : reportService.list(SyncopeClient.getListQueryBuilder().build()).getResult()) {
                    System.out.println(reportTO);
                }
            } catch (final SyncopeClientException ex) {
                System.out.println(" - Error: " + ex.getMessage());
            }
        } else if (reportIdToRead > -1L) {
            LOG.debug("- report read {} command", reportIdToRead);
            try {
                System.out.println(reportService.read(reportIdToRead));
            } catch (final SyncopeClientException ex) {
                System.out.println(" - Error: " + ex.getMessage());
            }
        } else if (reportIdToDelete > -1L) {
            try {
                LOG.debug("- report delete {} command", reportIdToDelete);
                reportService.delete(reportIdToDelete);
                System.out.println(" - Report " + reportIdToDelete + " deleted!");
            } catch (final SyncopeClientException ex) {
                System.out.println(" - Error: " + ex.getMessage());
            }
        } else if (reportIdToExecute > -1L) {
            try {
                LOG.debug("- report execute {} command", reportIdToExecute);
                reportService.execute(reportIdToExecute);
                final List<ReportExecTO> executionList = reportService.read(reportIdToExecute).getExecutions();
                final ReportExecTO lastExecution = executionList.get(executionList.size() - 1);
                System.out.println(" - Report execution id: " + lastExecution.getKey());
                System.out.println(" - Report execution status: " + lastExecution.getStatus());
                System.out.println(" - Report execution start date: " + lastExecution.getStartDate());
            } catch (final SyncopeClientException ex) {
                System.out.println(" - Error: " + ex.getMessage());
            }
        } else if (executionIdToRead > -1L) {
            try {
                LOG.debug("- report execution read {} command", executionIdToRead);
                ReportExecTO reportExecTO = reportService.readExecution(executionIdToRead);
                System.out.println(" - Report execution id: " + reportExecTO.getKey());
                System.out.println(" - Report execution status: " + reportExecTO.getStatus());
                System.out.println(" - Report execution start date: " + reportExecTO.getStartDate());
                System.out.println(" - Report execution end date: " + reportExecTO.getEndDate());
            } catch (final SyncopeClientException ex) {
                System.out.println(" - Error: " + ex.getMessage());
            }
        } else if (executionIdToDelete > -1L) {
            try {
                LOG.debug("- report execution delete {} command", executionIdToDelete);
                reportService.deleteExecution(executionIdToDelete);
                System.out.println(" - Report execution " + executionIdToDelete + "successfyllt deleted!");
            } catch (final SyncopeClientException ex) {
                System.out.println(" - Error: " + ex.getMessage());
            }
        } else if (exportId > -1L) {
            LOG.debug("- report export command for report: {}", exportId);

            try {
                XMLUtils.createXMLFile((SequenceInputStream) reportService.exportExecutionResult(exportId,
                        ReportExecExportFormat.XML).getEntity(), "export_" + exportId + ".xml");
                System.out.println(" - " + "export_" + exportId + " successfully created");
            } catch (final IOException ex) {
                LOG.error("Error creating xml file", ex);
                System.out.println(" - Error creating " + "export_" + exportId + " " + ex.getMessage());
            } catch (final ParserConfigurationException ex) {
                LOG.error("Error creating xml file", ex);
                System.out.println(" - Error creating " + "export_" + exportId + " " + ex.getMessage());
            } catch (final SAXException ex) {
                LOG.error("Error creating xml file", ex);
                System.out.println(" - Error creating " + "export_" + exportId + " " + ex.getMessage());
            } catch (final TransformerConfigurationException ex) {
                LOG.error("Error creating xml file", ex);
                System.out.println(" - Error creating " + "export_" + exportId + " " + ex.getMessage());
            } catch (final TransformerException ex) {
                LOG.error("Error creating xml file", ex);
                System.out.println(" - Error creating export_" + exportId + " " + ex.getMessage());
            } catch (final SyncopeClientException ex) {
                LOG.error("Error calling configuration service", ex);
                System.out.println(" - Error calling configuration service " + ex.getMessage());
            }
        } else {
            System.out.println(helpMessage);
        }
    }

}

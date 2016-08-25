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

import javax.xml.ws.WebServiceException;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportExecute extends AbstractReportCommand {

    private static final Logger LOG = LoggerFactory.getLogger(ReportExecute.class);

    private static final String EXECUTE_HELP_MESSAGE = "report --execute {REPORT-KEY}";

    private final Input input;

    public ReportExecute(final Input input) {
        this.input = input;
    }

    public void execute() {
        if (input.parameterNumber() == 1) {
            try {
                reportSyncopeOperations.execute(input.firstParameter());
                reportResultManager.genericMessage("Report " + input.firstParameter() + "executed.");
                reportResultManager.genericMessage(
                        "Run "
                        + ReportCommand.ReportOptions.READ.getOptionName()
                        + " "
                        + input.firstParameter()
                        + " to see the results.");
            } catch (final WebServiceException | SyncopeClientException ex) {
                LOG.error("Error executin report", ex);
                if (ex.getMessage().startsWith("NotFound")) {
                    reportResultManager.notFoundError("Report", input.firstParameter());
                } else if (ex.getMessage().startsWith("DataIntegrityViolation")) {
                    reportResultManager.genericError("You cannot delete report " + input.firstParameter());
                } else {
                    reportResultManager.genericError(ex.getMessage());
                }
            } catch (final NumberFormatException ex) {
                LOG.error("Error executin report", ex);
                reportResultManager.numberFormatException("report", input.firstParameter());
            }
        } else {
            reportResultManager.commandOptionError(EXECUTE_HELP_MESSAGE);
        }
    }
}

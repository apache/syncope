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
import javax.xml.ws.WebServiceException;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ReportTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportRead extends AbstractReportCommand {

    private static final Logger LOG = LoggerFactory.getLogger(ReportRead.class);

    private static final String READ_HELP_MESSAGE = "report --read {REPORT-KEY} {REPORT-KEY} [...]";

    private final Input input;

    public ReportRead(final Input input) {
        this.input = input;
    }

    public void read() {
        if (input.parameterNumber() >= 1) {
            final List<ReportTO> reportTOs = new ArrayList<>();
            for (final String parameter : input.getParameters()) {
                try {
                    reportTOs.add(reportSyncopeOperations.read(parameter));
                } catch (final NumberFormatException ex) {
                    LOG.error("Error reading report", ex);
                    reportResultManager.numberFormatException("report", parameter);
                } catch (final WebServiceException | SyncopeClientException ex) {
                    LOG.error("Error reading report", ex);
                    if (ex.getMessage().startsWith("NotFound")) {
                        reportResultManager.notFoundError("Report", parameter);
                    } else {
                        reportResultManager.genericError(ex.getMessage());
                    }
                }
            }
            reportResultManager.printReports(reportTOs);
        } else {
            reportResultManager.commandOptionError(READ_HELP_MESSAGE);
        }
    }
}

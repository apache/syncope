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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ReportTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportDetails extends AbstractReportCommand {

    private static final Logger LOG = LoggerFactory.getLogger(ReportDetails.class);

    private static final String LIST_HELP_MESSAGE = "report --details";

    private final Input input;

    public ReportDetails(final Input input) {
        this.input = input;
    }

    public void details() {
        if (input.parameterNumber() == 0) {
            try {
                final Map<String, String> details = new LinkedHashMap<>();
                final List<ReportTO> reportTOs = reportSyncopeOperations.list();
                int withoutExecutions = 0;
                for (final ReportTO reportTO : reportTOs) {
                    if (reportTO.getExecutions().isEmpty()) {
                        withoutExecutions++;
                    }
                }
                details.put("Total numbers", String.valueOf(reportTOs.size()));
                details.put("Never executed", String.valueOf(withoutExecutions));
                reportResultManager.printDetails(details);
            } catch (final SyncopeClientException ex) {
                LOG.error("Error reading details about report", ex);
                reportResultManager.genericError(ex.getMessage());
            }
        } else {
            reportResultManager.unnecessaryParameters(input.listParameters(), LIST_HELP_MESSAGE);
        }
    }
}

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

import java.util.List;
import java.util.Map;
import org.apache.syncope.client.cli.commands.CommonsResultManager;
import org.apache.syncope.common.lib.report.AbstractReportletConf;
import org.apache.syncope.common.lib.report.UserReportletConf;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.ReportTO;

public class ReportResultManager extends CommonsResultManager {

    public void printReports(final List<ReportTO> reportTOs) {
        System.out.println("");
        for (final ReportTO reportTO : reportTOs) {
            printReport(reportTO);
        }
    }

    private void printReport(final ReportTO reportTO) {
        System.out.println(" > REPORT KEY: " + reportTO.getKey());
        System.out.println("    name: " + reportTO.getName());
        System.out.println("    cron expression: " + reportTO.getCronExpression());
        System.out.println("    latest execution status: " + reportTO.getLatestExecStatus());
        System.out.println("    start date: " + reportTO.getStart());
        System.out.println("    end date: " + reportTO.getEnd());
        System.out.println("    CONF:");
        for (final AbstractReportletConf reportletConf : reportTO.getReportletConfs()) {
            printReportletConf(reportletConf);
        }
        System.out.println("    EXECUTIONS:");
        printReportExecutions(reportTO.getExecutions());
        System.out.println("");
    }

    private void printReportletConf(final AbstractReportletConf reportletConf) {
        if (reportletConf instanceof UserReportletConf) {
            final UserReportletConf userReportletConf = (UserReportletConf) reportletConf;
            System.out.println("       name: " + userReportletConf.getName());
            System.out.println("       features: " + userReportletConf.getFeatures());
            System.out.println("       plain attributes: " + userReportletConf.getPlainAttrs());
            System.out.println("       derived attributes: " + userReportletConf.getDerAttrs());
            System.out.println("       virtual attributes: " + userReportletConf.getVirAttrs());
            System.out.println("       matching condition: " + userReportletConf.getMatchingCond());
        }
    }

    public void printReportExecutions(final List<ExecTO> reportExecTOs) {
        for (final ExecTO reportExecTO : reportExecTOs) {
            System.out.println("       REPORT EXEC KEY: " + reportExecTO.getKey());
            System.out.println("       status: " + reportExecTO.getStatus());
            System.out.println("       message: " + reportExecTO.getMessage());
            System.out.println("       start date: " + reportExecTO.getStart());
            System.out.println("       end date: " + reportExecTO.getEnd());
            System.out.println("       report: " + reportExecTO.getRefDesc());
        }
    }

    public void printJobs(final List<JobTO> jobTOs) {
        for (final JobTO jobTO : jobTOs) {
            System.out.println("       REPORT: " + jobTO.getRefDesc());
            System.out.println("       start date: " + jobTO.getStart());
            System.out.println("       running: " + jobTO.isRunning());
            System.out.println("       scheduled: " + jobTO.isScheduled());
        }
    }

    public void printDetails(final Map<String, String> details) {
        printDetails("reports details", details);
    }
}

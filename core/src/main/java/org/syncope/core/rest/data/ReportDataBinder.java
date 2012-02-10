/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.core.rest.data;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.syncope.client.report.ReportletConf;
import org.syncope.client.to.ReportExecTO;
import org.syncope.client.to.ReportTO;
import org.syncope.core.persistence.beans.Report;
import org.syncope.core.persistence.beans.ReportExec;

@Component
public class ReportDataBinder {

    private static final String[] IGNORE_REPORT_PROPERTIES = {
        "id", "reportlets", "executions"};

    private static final String[] IGNORE_REPORT_EXECUTION_PROPERTIES = {
        "id", "report", "execResult"};

    public void getReport(final Report report, final ReportTO reportTO) {
        BeanUtils.copyProperties(reportTO, report, IGNORE_REPORT_PROPERTIES);
        report.getReportletConfs().clear();
        for (ReportletConf conf : reportTO.getReportletConfs()) {
            report.addReportletConf(conf);
        }
    }

    public ReportTO getReportTO(final Report report) {
        ReportTO reportTO = new ReportTO();
        reportTO.setId(report.getId());
        BeanUtils.copyProperties(report, reportTO, IGNORE_REPORT_PROPERTIES);

        reportTO.setReportletConfs(report.getReportletConfs());

        for (ReportExec reportExec : report.getExecs()) {
            reportTO.addExec(getReportExecTO(reportExec));
        }

        return reportTO;
    }

    public ReportExecTO getReportExecTO(final ReportExec execution) {
        ReportExecTO executionTO = new ReportExecTO();
        executionTO.setId(execution.getId());
        BeanUtils.copyProperties(execution, executionTO,
                IGNORE_REPORT_EXECUTION_PROPERTIES);
        if (execution.getId() != null) {
            executionTO.setId(execution.getId());
        }
        executionTO.setReport(execution.getReport().getId());

        return executionTO;
    }
}

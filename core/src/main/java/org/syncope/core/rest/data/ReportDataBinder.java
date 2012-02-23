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
package org.syncope.core.rest.data;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;
import org.syncope.client.report.ReportletConf;
import org.syncope.client.to.ReportExecTO;
import org.syncope.client.to.ReportTO;
import org.syncope.core.init.JobInstanceLoader;
import org.syncope.core.persistence.beans.Report;
import org.syncope.core.persistence.beans.ReportExec;
import org.syncope.core.persistence.dao.ReportExecDAO;

@Component
public class ReportDataBinder {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            ReportDataBinder.class);

    private static final String[] IGNORE_REPORT_PROPERTIES = {
        "id", "reportlets", "executions", "latestExecStatus"};

    private static final String[] IGNORE_REPORT_EXECUTION_PROPERTIES = {
        "id", "report", "execResult"};

    @Autowired
    private ReportExecDAO reportExecDAO;

    @Autowired
    private SchedulerFactoryBean scheduler;

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

        ReportExec latestExec = reportExecDAO.findLatestStarted(report);
        reportTO.setLatestExecStatus(latestExec == null
                ? "" : latestExec.getStatus());

        for (ReportExec reportExec : report.getExecs()) {
            reportTO.addExecution(getReportExecTO(reportExec));
        }

        String triggerName = JobInstanceLoader.getTriggerName(
                JobInstanceLoader.getJobName(report));

        Trigger trigger;
        try {
            trigger = scheduler.getScheduler().getTrigger(triggerName,
                    Scheduler.DEFAULT_GROUP);
        } catch (SchedulerException e) {
            LOG.warn("While trying to get to " + triggerName, e);
            trigger = null;
        }

        if (trigger != null) {
            reportTO.setLastExec(trigger.getPreviousFireTime());
            reportTO.setNextExec(trigger.getNextFireTime());
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

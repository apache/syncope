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
package org.apache.syncope.server.provisioning.java.data;

import java.util.HashSet;
import java.util.Set;
import org.apache.syncope.server.provisioning.api.data.ReportDataBinder;
import org.apache.syncope.common.lib.report.AbstractReportletConf;
import org.apache.syncope.common.lib.report.ReportletConf;
import org.apache.syncope.common.lib.to.ReportExecTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.server.persistence.api.dao.ReportExecDAO;
import org.apache.syncope.server.persistence.api.entity.Report;
import org.apache.syncope.server.persistence.api.entity.ReportExec;
import org.apache.syncope.server.provisioning.api.job.JobNamer;
import org.apache.syncope.server.misc.spring.BeanUtils;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;

@Component
public class ReportDataBinderImpl implements ReportDataBinder {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ReportDataBinder.class);

    private static final String[] IGNORE_REPORT_PROPERTIES = { "key", "reportlets", "executions" };

    private static final String[] IGNORE_REPORT_EXECUTION_PROPERTIES = { "key", "report", "execResult" };

    @Autowired
    private ReportExecDAO reportExecDAO;

    @Autowired
    private SchedulerFactoryBean scheduler;

    @Override
    public void getReport(final Report report, final ReportTO reportTO) {
        BeanUtils.copyProperties(reportTO, report, IGNORE_REPORT_PROPERTIES);

        // 1. remove all reportlet confs
        Set<ReportletConf> toRemove = new HashSet<>();
        for (ReportletConf conf : report.getReportletConfs()) {
            toRemove.add(conf);
        }
        for (ReportletConf conf : toRemove) {
            report.removeReportletConf(conf);
        }

        // 2. take all reportlet confs from reportTO
        for (ReportletConf conf : reportTO.getReportletConfs()) {
            report.addReportletConf(conf);
        }
    }

    @Override
    public ReportTO getReportTO(final Report report) {
        ReportTO reportTO = new ReportTO();
        reportTO.setKey(report.getKey());
        BeanUtils.copyProperties(report, reportTO, IGNORE_REPORT_PROPERTIES);

        copyReportletConfs(report, reportTO);

        ReportExec latestExec = reportExecDAO.findLatestStarted(report);
        reportTO.setLatestExecStatus(latestExec == null
                ? ""
                : latestExec.getStatus());

        reportTO.setStartDate(latestExec == null
                ? null
                : latestExec.getStartDate());

        reportTO.setEndDate(latestExec == null
                ? null
                : latestExec.getEndDate());

        for (ReportExec reportExec : report.getExecs()) {
            reportTO.getExecutions().add(getReportExecTO(reportExec));
        }

        String triggerName = JobNamer.getTriggerName(JobNamer.getJobName(report));

        Trigger trigger;
        try {
            trigger = scheduler.getScheduler().getTrigger(new TriggerKey(triggerName, Scheduler.DEFAULT_GROUP));
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

    private void copyReportletConfs(final Report report, final ReportTO reportTO) {
        reportTO.getReportletConfs().clear();
        for (ReportletConf reportletConf : report.getReportletConfs()) {
            reportTO.getReportletConfs().add((AbstractReportletConf) reportletConf);
        }
    }

    @Override
    public ReportExecTO getReportExecTO(final ReportExec execution) {
        ReportExecTO executionTO = new ReportExecTO();
        executionTO.setKey(execution.getKey());
        BeanUtils.copyProperties(execution, executionTO, IGNORE_REPORT_EXECUTION_PROPERTIES);
        if (execution.getKey() != null) {
            executionTO.setKey(execution.getKey());
        }
        executionTO.setReport(execution.getReport().getKey());

        return executionTO;
    }
}

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
package org.apache.syncope.core.provisioning.java.data;

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.report.AbstractReportletConf;
import org.apache.syncope.core.provisioning.api.data.ReportDataBinder;
import org.apache.syncope.common.lib.report.ReportletConf;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.JobType;
import org.apache.syncope.core.persistence.api.dao.ReportExecDAO;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.api.entity.ReportExec;
import org.apache.syncope.core.provisioning.api.job.JobNamer;
import org.apache.syncope.core.spring.BeanUtils;
import org.apache.syncope.core.persistence.api.dao.ReportTemplateDAO;
import org.apache.syncope.core.persistence.api.entity.ReportTemplate;
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

    private static final Logger LOG = LoggerFactory.getLogger(ReportDataBinder.class);

    private static final String[] IGNORE_REPORT_PROPERTIES = { "key", "template", "reportlets", "executions" };

    private static final String[] IGNORE_REPORT_EXECUTION_PROPERTIES = { "key", "report", "execResult" };

    @Autowired
    private ReportTemplateDAO reportTemplateDAO;

    @Autowired
    private ReportExecDAO reportExecDAO;

    @Autowired
    private SchedulerFactoryBean scheduler;

    @Override
    public void getReport(final Report report, final ReportTO reportTO) {
        BeanUtils.copyProperties(reportTO, report, IGNORE_REPORT_PROPERTIES);

        ReportTemplate template = reportTemplateDAO.find(reportTO.getTemplate());
        if (template == null) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);
            sce.getElements().add("template");
            throw sce;
        }
        report.setTemplate(template);

        report.removeAllReportletConfs();
        for (ReportletConf conf : reportTO.getReportletConfs()) {
            report.add(conf);
        }
    }

    @Override
    public ReportTO getReportTO(final Report report) {
        ReportTO reportTO = new ReportTO();
        reportTO.setKey(report.getKey());
        reportTO.setTemplate(report.getTemplate().getKey());

        BeanUtils.copyProperties(report, reportTO, IGNORE_REPORT_PROPERTIES);

        reportTO.getReportletConfs().clear();
        for (ReportletConf reportletConf : report.getReportletConfs()) {
            reportTO.getReportletConfs().add((AbstractReportletConf) reportletConf);
        }

        ReportExec latestExec = reportExecDAO.findLatestStarted(report);
        if (latestExec == null) {
            reportTO.setLatestExecStatus(StringUtils.EMPTY);
        } else {
            reportTO.setLatestExecStatus(latestExec.getStatus());
            reportTO.setStart(latestExec.getStart());
            reportTO.setEnd(latestExec.getEnd());

            reportTO.setLastExec(reportTO.getStart());
        }

        for (ReportExec reportExec : report.getExecs()) {
            reportTO.getExecutions().add(getExecTO(reportExec));
        }

        String triggerName = JobNamer.getTriggerName(JobNamer.getJobKey(report).getName());
        try {
            Trigger trigger = scheduler.getScheduler().getTrigger(new TriggerKey(triggerName, Scheduler.DEFAULT_GROUP));

            if (trigger != null) {
                reportTO.setLastExec(trigger.getPreviousFireTime());
                reportTO.setNextExec(trigger.getNextFireTime());
            }
        } catch (SchedulerException e) {
            LOG.warn("While trying to get to " + triggerName, e);
        }

        return reportTO;
    }

    @Override
    public String buildRefDesc(final Report report) {
        return "Report "
                + report.getKey() + " "
                + report.getName();
    }

    @Override
    public ExecTO getExecTO(final ReportExec execution) {
        ExecTO execTO = new ExecTO();
        execTO.setKey(execution.getKey());
        BeanUtils.copyProperties(execution, execTO, IGNORE_REPORT_EXECUTION_PROPERTIES);
        if (execution.getKey() != null) {
            execTO.setKey(execution.getKey());
        }
        execTO.setJobType(JobType.REPORT);
        execTO.setRefKey(execution.getReport().getKey());
        execTO.setRefDesc(buildRefDesc(execution.getReport()));

        return execTO;
    }
}

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

import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.core.provisioning.api.data.ReportDataBinder;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.JobType;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.ReportExecDAO;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.api.entity.ReportExec;
import org.apache.syncope.core.provisioning.api.job.JobNamer;
import org.apache.syncope.core.persistence.api.dao.ReportTemplateDAO;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.ReportTemplate;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

public class ReportDataBinderImpl extends AbstractExecutableDatabinder implements ReportDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(ReportDataBinder.class);

    protected final ReportTemplateDAO reportTemplateDAO;

    protected final ReportExecDAO reportExecDAO;

    protected final ImplementationDAO implementationDAO;

    protected final SchedulerFactoryBean scheduler;

    public ReportDataBinderImpl(
            final ReportTemplateDAO reportTemplateDAO,
            final ReportExecDAO reportExecDAO,
            final ImplementationDAO implementationDAO,
            final SchedulerFactoryBean scheduler) {

        this.reportTemplateDAO = reportTemplateDAO;
        this.reportExecDAO = reportExecDAO;
        this.implementationDAO = implementationDAO;
        this.scheduler = scheduler;
    }

    @Override
    public void getReport(final Report report, final ReportTO reportTO) {
        report.setName(reportTO.getName());
        report.setCronExpression(reportTO.getCronExpression());
        report.setActive(reportTO.isActive());

        ReportTemplate template = reportTemplateDAO.find(reportTO.getTemplate());
        if (template == null) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);
            sce.getElements().add("template");
            throw sce;
        }
        report.setTemplate(template);

        reportTO.getReportlets().forEach(reportletKey -> {
            Implementation reportlet = implementationDAO.find(reportletKey);
            if (reportlet == null) {
                LOG.debug("Invalid " + Implementation.class.getSimpleName() + " {}, ignoring...", reportletKey);
            } else {
                report.add(reportlet);
            }
        });
        // remove all implementations not contained in the TO
        report.getReportlets().removeIf(reportlet -> !reportTO.getReportlets().contains(reportlet.getKey()));
    }

    @Override
    public ReportTO getReportTO(final Report report) {
        ReportTO reportTO = new ReportTO();
        reportTO.setKey(report.getKey());
        reportTO.setTemplate(report.getTemplate().getKey());
        reportTO.setName(report.getName());
        reportTO.setCronExpression(report.getCronExpression());
        reportTO.setActive(report.isActive());

        reportTO.getReportlets().addAll(
                report.getReportlets().stream().map(Entity::getKey).collect(Collectors.toList()));

        ReportExec latestExec = reportExecDAO.findLatestStarted(report);
        if (latestExec == null) {
            reportTO.setLatestExecStatus(StringUtils.EMPTY);
        } else {
            reportTO.setLatestExecStatus(latestExec.getStatus());
            reportTO.setStart(latestExec.getStart());
            reportTO.setEnd(latestExec.getEnd());

            reportTO.setLastExecutor(latestExec.getExecutor());
            reportTO.setLastExec(reportTO.getStart());
        }

        reportTO.getExecutions().addAll(report.getExecs().stream().
                map(this::getExecTO).collect(Collectors.toList()));

        String triggerName = JobNamer.getTriggerName(JobNamer.getJobKey(report).getName());
        try {
            Trigger trigger = scheduler.getScheduler().getTrigger(new TriggerKey(triggerName, Scheduler.DEFAULT_GROUP));
            if (trigger != null) {
                reportTO.setLastExec(toOffsetDateTime(trigger.getPreviousFireTime()));
                reportTO.setNextExec(toOffsetDateTime(trigger.getNextFireTime()));
            }
        } catch (SchedulerException e) {
            LOG.warn("While trying to get to " + triggerName, e);
        }

        return reportTO;
    }

    @Override
    public String buildRefDesc(final Report report) {
        return "Report "
                + report.getKey() + ' '
                + report.getName();
    }

    @Override
    public ExecTO getExecTO(final ReportExec execution) {
        ExecTO execTO = new ExecTO();
        execTO.setKey(execution.getKey());
        execTO.setJobType(JobType.REPORT);
        execTO.setRefKey(execution.getReport().getKey());
        execTO.setRefDesc(buildRefDesc(execution.getReport()));
        execTO.setStatus(execution.getStatus());
        execTO.setMessage(execution.getMessage());
        execTO.setStart(execution.getStart());
        execTO.setEnd(execution.getEnd());

        execTO.setExecutor(execution.getExecutor());
        return execTO;
    }
}

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

import java.util.Comparator;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.JobType;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.ReportExecDAO;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.api.entity.ReportExec;
import org.apache.syncope.core.provisioning.api.data.ReportDataBinder;
import org.apache.syncope.core.provisioning.api.job.JobNamer;
import org.apache.syncope.core.provisioning.java.job.SyncopeTaskScheduler;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportDataBinderImpl extends AbstractExecutableDatabinder implements ReportDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(ReportDataBinder.class);

    protected final ReportExecDAO reportExecDAO;

    protected final ImplementationDAO implementationDAO;

    protected final SyncopeTaskScheduler scheduler;

    public ReportDataBinderImpl(
            final ReportExecDAO reportExecDAO,
            final ImplementationDAO implementationDAO,
            final SyncopeTaskScheduler scheduler) {

        this.reportExecDAO = reportExecDAO;
        this.implementationDAO = implementationDAO;
        this.scheduler = scheduler;
    }

    @Override
    public void getReport(final Report report, final ReportTO reportTO) {
        report.setName(reportTO.getName());
        report.setMimeType(reportTO.getMimeType());
        report.setFileExt(reportTO.getFileExt());
        report.setCronExpression(reportTO.getCronExpression());
        report.setActive(reportTO.isActive());

        Implementation jobDelegate = implementationDAO.findById(reportTO.getJobDelegate()).orElse(null);
        if (jobDelegate == null || !IdRepoImplementationType.REPORT_DELEGATE.equals(jobDelegate.getType())) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidImplementation);
            sce.getElements().add("Null or invalid JobDelegate, expected " + IdRepoImplementationType.REPORT_DELEGATE);
            throw sce;
        }
        report.setJobDelegate(jobDelegate);
    }

    @Override
    public ReportTO getReportTO(final Report report) {
        ReportTO reportTO = new ReportTO();
        reportTO.setKey(report.getKey());
        reportTO.setName(report.getName());
        reportTO.setMimeType(report.getMimeType());
        reportTO.setFileExt(report.getFileExt());
        reportTO.setCronExpression(report.getCronExpression());
        reportTO.setJobDelegate(report.getJobDelegate().getKey());
        reportTO.setActive(report.isActive());

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

        reportTO.getExecutions().addAll(report.getExecs().stream().map(this::getExecTO).toList());

        reportTO.getExecutions().stream().max(Comparator.comparing(ExecTO::getStart)).
                map(ExecTO::getStart).ifPresent(reportTO::setLastExec);

        scheduler.getNextTrigger(AuthContextUtils.getDomain(), JobNamer.getJobName(report)).
                ifPresent(reportTO::setNextExec);

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

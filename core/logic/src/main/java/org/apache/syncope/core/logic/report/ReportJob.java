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
package org.apache.syncope.core.logic.report;

import org.apache.syncope.core.misc.security.AuthContextUtils;
import org.apache.syncope.core.provisioning.api.job.JobInstanceLoader;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Quartz job for executing a given report.
 */
@DisallowConcurrentExecution
public class ReportJob implements Job {

    /**
     * Key, set by the caller, for identifying the report to be executed.
     */
    private Long reportKey;

    @Autowired
    private ReportJobDelegate delegate;

    /**
     * Report id setter.
     *
     * @param reportKey to be set
     */
    public void setReportKey(final Long reportKey) {
        this.reportKey = reportKey;
    }

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        try {
            AuthContextUtils.execWithAuthContext(context.getMergedJobDataMap().getString(JobInstanceLoader.DOMAIN),
                    new AuthContextUtils.Executable<Void>() {

                        @Override
                        public Void exec() {
                            try {
                                delegate.execute(reportKey);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }

                            return null;
                        }
                    });
        } catch (RuntimeException e) {
            throw new JobExecutionException(e.getCause());
        }
    }
}

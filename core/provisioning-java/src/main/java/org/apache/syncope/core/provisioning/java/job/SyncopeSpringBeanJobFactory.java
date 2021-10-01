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
package org.apache.syncope.core.provisioning.java.job;

import java.util.Optional;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.provisioning.java.job.report.ReportJob;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.quartz.spi.TriggerFiredBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

public class SyncopeSpringBeanJobFactory extends SpringBeanJobFactory {

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeSpringBeanJobFactory.class);

    @Override
    protected Object createJobInstance(final TriggerFiredBundle bundle) throws Exception {
        Object job = super.createJobInstance(bundle);

        if (bundle.getJobDetail().getJobDataMap() != null) {
            if (job instanceof ReportJob) {
                Optional.ofNullable(bundle.getJobDetail().getJobDataMap().getString(JobManager.REPORT_KEY)).
                        ifPresent(((ReportJob) job)::setReportKey);
            } else if (job instanceof TaskJob) {
                Optional.ofNullable(bundle.getJobDetail().getJobDataMap().getString(JobManager.TASK_KEY)).
                        ifPresent(((TaskJob) job)::setTaskKey);
            }
        }

        DefaultListableBeanFactory factory = ApplicationContextProvider.getBeanFactory();
        try {
            if (factory.containsSingleton(bundle.getJobDetail().getKey().getName())) {
                factory.destroySingleton(bundle.getJobDetail().getKey().getName());
            }
            factory.registerSingleton(bundle.getJobDetail().getKey().getName(), job);
        } catch (Exception e) {
            LOG.error("While attempting to replace job instance as singleton Spring bean", e);
        }

        return job;
    }
}

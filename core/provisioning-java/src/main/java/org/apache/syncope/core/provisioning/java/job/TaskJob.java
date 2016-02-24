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

import org.apache.commons.lang3.ClassUtils;
import org.apache.syncope.core.misc.security.AuthContextUtils;
import org.apache.syncope.core.misc.spring.ApplicationContextProvider;
import org.apache.syncope.core.provisioning.api.job.SchedTaskJobDelegate;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.apache.syncope.core.provisioning.api.job.JobManager;

public class TaskJob extends AbstractInterruptableJob {

    public static final String DRY_RUN_JOBDETAIL_KEY = "dryRun";

    public static final String DELEGATE_CLASS_KEY = "delegateClass";

    /**
     * Task execution status.
     */
    public enum Status {

        SUCCESS,
        FAILURE

    }

    /**
     * Key, set by the caller, for identifying the task to be executed.
     */
    private Long taskKey;

    /**
     * Task key setter.
     *
     * @param taskKey to be set
     */
    public void setTaskKey(final Long taskKey) {
        this.taskKey = taskKey;
    }

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        super.execute(context);

        try {
            AuthContextUtils.execWithAuthContext(context.getMergedJobDataMap().getString(JobManager.DOMAIN_KEY),
                    new AuthContextUtils.Executable<Void>() {

                @Override
                public Void exec() {
                    try {
                        Class<?> delegateClass =
                                ClassUtils.getClass(context.getMergedJobDataMap().getString(DELEGATE_CLASS_KEY));

                        ((SchedTaskJobDelegate) ApplicationContextProvider.getBeanFactory().
                                createBean(delegateClass, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false)).
                                execute(taskKey, context.getMergedJobDataMap().getBoolean(DRY_RUN_JOBDETAIL_KEY));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    return null;
                }
            }
            );
        } catch (RuntimeException e) {
            throw new JobExecutionException(e.getCause());
        }
    }
}

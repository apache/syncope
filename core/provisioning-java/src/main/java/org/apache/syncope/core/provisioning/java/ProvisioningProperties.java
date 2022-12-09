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
package org.apache.syncope.core.provisioning.java;

import java.util.ArrayList;
import java.util.List;
import org.quartz.impl.jdbcjobstore.DriverDelegate;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("provisioning")
public class ProvisioningProperties {

    public static class QuartzProperties {

        private Class<? extends DriverDelegate> delegate;

        private String sql;

        private boolean disableInstance = false;

        private boolean waitForJobsToCompleteOnShutdown = true;

        private int idleWaitTime = 30000;

        private int misfireThreshold = 60000;

        public Class<? extends DriverDelegate> getDelegate() {
            return delegate;
        }

        public void setDelegate(final Class<? extends DriverDelegate> delegate) {
            this.delegate = delegate;
        }

        public String getSql() {
            return sql;
        }

        public void setSql(final String sql) {
            this.sql = sql;
        }

        public boolean isDisableInstance() {
            return disableInstance;
        }

        public void setDisableInstance(final boolean disableInstance) {
            this.disableInstance = disableInstance;
        }

        public boolean isWaitForJobsToCompleteOnShutdown() {
            return waitForJobsToCompleteOnShutdown;
        }

        public void setWaitForJobsToCompleteOnShutdown(final boolean waitForJobsToCompleteOnShutdown) {
            this.waitForJobsToCompleteOnShutdown = waitForJobsToCompleteOnShutdown;
        }

        public int getIdleWaitTime() {
            return idleWaitTime;
        }

        public void setIdleWaitTime(final int idleWaitTime) {
            this.idleWaitTime = idleWaitTime;
        }

        public int getMisfireThreshold() {
            return misfireThreshold;
        }

        public void setMisfireThreshold(final int misfireThreshold) {
            this.misfireThreshold = misfireThreshold;
        }
    }

    private final ExecutorProperties asyncConnectorFacadeExecutor = new ExecutorProperties();

    private final ExecutorProperties propagationTaskExecutorAsyncExecutor = new ExecutorProperties();

    private String virAttrCacheSpec = "maximumSize=5000,expireAfterAccess=1m";

    private final List<String> connIdLocation = new ArrayList<>();

    private final QuartzProperties quartz = new QuartzProperties();

    public String getVirAttrCacheSpec() {
        return virAttrCacheSpec;
    }

    public void setVirAttrCacheSpec(final String virAttrCacheSpec) {
        this.virAttrCacheSpec = virAttrCacheSpec;
    }

    public ExecutorProperties getAsyncConnectorFacadeExecutor() {
        return asyncConnectorFacadeExecutor;
    }

    public ExecutorProperties getPropagationTaskExecutorAsyncExecutor() {
        return propagationTaskExecutorAsyncExecutor;
    }

    public List<String> getConnIdLocation() {
        return connIdLocation;
    }

    public QuartzProperties getQuartz() {
        return quartz;
    }
}

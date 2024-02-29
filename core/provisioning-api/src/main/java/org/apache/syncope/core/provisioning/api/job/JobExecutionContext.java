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
package org.apache.syncope.core.provisioning.api.job;

import java.util.HashMap;
import java.util.Map;

public class JobExecutionContext {

    private final String domain;

    private final String jobName;

    private final String executor;

    private final boolean dryRun;

    private final Map<String, Object> data = new HashMap<>();

    public JobExecutionContext(final String domain, final String jobName, final String executor, final boolean dryRun) {
        this.domain = domain;
        this.jobName = jobName;
        this.executor = executor;
        this.dryRun = dryRun;
    }

    public String getDomain() {
        return domain;
    }

    public String getJobName() {
        return jobName;
    }

    public String getExecutor() {
        return executor;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public Map<String, Object> getData() {
        return data;
    }
}

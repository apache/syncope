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
package org.apache.syncope.core.provisioning.api.event;

import org.springframework.context.ApplicationEvent;

public class JobStatusEvent extends ApplicationEvent {

    private static final long serialVersionUID = 373067530016978592L;

    private final String domain;

    private final String jobName;

    private final String jobStatus;

    public JobStatusEvent(final Object source, final String domain, final String jobName, final String jobStatus) {
        super(source);
        this.domain = domain;
        this.jobName = jobName;
        this.jobStatus = jobStatus;
    }

    public String getDomain() {
        return domain;
    }

    public String getJobName() {
        return jobName;
    }

    public String getJobStatus() {
        return jobStatus;
    }

    @Override
    public String toString() {
        return "JobStatusEvent{"
                + "domain=" + domain
                + ", jobName=" + jobName
                + ", jobStatus=" + jobStatus
                + '}';
    }
}

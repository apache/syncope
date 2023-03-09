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
package org.apache.syncope.common.lib.to;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.ws.rs.PathParam;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class ReportTO extends AbstractStartEndBean implements NamedEntityTO {

    private static final long serialVersionUID = 5274568072084814410L;

    private String key;

    private String name;

    private String mimeType;

    private String fileExt;

    private String cronExpression;

    private String jobDelegate;

    private boolean active;

    private final List<ExecTO> executions = new ArrayList<>();

    private String latestExecStatus;

    private OffsetDateTime lastExec;

    private OffsetDateTime nextExec;

    private String lastExecutor;

    @Override
    public String getKey() {
        return key;
    }

    @PathParam("key")
    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }

    public String getFileExt() {
        return fileExt;
    }

    public void setFileExt(final String fileExt) {
        this.fileExt = fileExt;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(final String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getJobDelegate() {
        return jobDelegate;
    }

    public void setJobDelegate(final String jobDelegate) {
        this.jobDelegate = jobDelegate;
    }

    @JacksonXmlElementWrapper(localName = "executions")
    @JacksonXmlProperty(localName = "execution")
    public List<ExecTO> getExecutions() {
        return executions;
    }

    public String getLatestExecStatus() {
        return latestExecStatus;
    }

    public void setLatestExecStatus(final String latestExecStatus) {
        this.latestExecStatus = latestExecStatus;
    }

    public OffsetDateTime getLastExec() {
        return lastExec;
    }

    public void setLastExec(final OffsetDateTime lastExec) {
        this.lastExec = lastExec;
    }

    public OffsetDateTime getNextExec() {
        return nextExec;
    }

    public void setNextExec(final OffsetDateTime nextExec) {
        this.nextExec = nextExec;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    public String getLastExecutor() {
        return lastExecutor;
    }

    public void setLastExecutor(final String lastExecutor) {
        this.lastExecutor = lastExecutor;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(key).
                append(name).
                append(mimeType).
                append(fileExt).
                append(cronExpression).
                append(jobDelegate).
                append(executions).
                append(latestExecStatus).
                append(lastExec).
                append(nextExec).
                append(active).
                append(lastExecutor).
                build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ReportTO other = (ReportTO) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(key, other.key).
                append(name, other.name).
                append(mimeType, other.mimeType).
                append(fileExt, other.fileExt).
                append(cronExpression, other.cronExpression).
                append(jobDelegate, other.jobDelegate).
                append(executions, other.executions).
                append(latestExecStatus, other.latestExecStatus).
                append(lastExec, other.lastExec).
                append(nextExec, other.nextExec).
                append(active, other.active).
                append(lastExecutor, other.lastExecutor).
                build();
    }
}

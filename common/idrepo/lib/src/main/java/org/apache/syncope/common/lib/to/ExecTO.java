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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.types.JobType;

public class ExecTO extends AbstractStartEndBean implements EntityTO {

    private static final long serialVersionUID = -4621191979198357081L;

    private String key;

    private JobType jobType;

    private String refKey;

    private String refDesc;

    private String status;

    private String message;

    private String executor;

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(final JobType jobType) {
        this.jobType = jobType;
    }

    public String getRefKey() {
        return refKey;
    }

    public void setRefKey(final String refKey) {
        this.refKey = refKey;
    }

    public String getRefDesc() {
        return refDesc;
    }

    public void setRefDesc(final String refDesc) {
        this.refDesc = refDesc;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getExecutor() {
        return executor;
    }

    public void setExecutor(final String executor) {
        this.executor = executor;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(key).
                append(jobType).
                append(refKey).
                append(refDesc).
                append(status).
                append(message).
                append(executor).
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
        final ExecTO other = (ExecTO) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(key, other.key).
                append(jobType, other.jobType).
                append(refKey, other.refKey).
                append(refDesc, other.refDesc).
                append(status, other.status).
                append(message, other.message).
                append(executor, other.executor).
                build();
    }
}

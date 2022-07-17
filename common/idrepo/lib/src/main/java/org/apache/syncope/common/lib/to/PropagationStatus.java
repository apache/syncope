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
import org.apache.syncope.common.lib.BaseBean;
import org.apache.syncope.common.lib.types.ExecStatus;

/**
 * Single propagation status.
 */
public class PropagationStatus implements BaseBean {

    private static final long serialVersionUID = 3921498450222857690L;

    /**
     * Object before propagation.
     */
    private ConnObject beforeObj;

    /**
     * Object after propagation.
     */
    private ConnObject afterObj;

    /**
     * Propagated resource name.
     */
    private String resource;

    /**
     * Propagation task execution status.
     */
    private ExecStatus status;

    /**
     * Propagation task execution failure message.
     */
    private String failureReason;

    public ConnObject getAfterObj() {
        return afterObj;
    }

    public void setAfterObj(final ConnObject afterObj) {
        this.afterObj = afterObj;
    }

    public ConnObject getBeforeObj() {
        return beforeObj;
    }

    public void setBeforeObj(final ConnObject beforeObj) {
        this.beforeObj = beforeObj;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(final String resource) {
        this.resource = resource;
    }

    public ExecStatus getStatus() {
        return status;
    }

    public void setStatus(final ExecStatus status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(final String failureReason) {
        this.failureReason = failureReason;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(beforeObj).
                append(afterObj).
                append(resource).
                append(status).
                append(failureReason).
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
        final PropagationStatus other = (PropagationStatus) obj;
        return new EqualsBuilder().
                append(beforeObj, other.beforeObj).
                append(afterObj, other.afterObj).
                append(resource, other.resource).
                append(status, other.status).
                append(failureReason, other.failureReason).
                build();
    }
}

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
package org.apache.syncope.core.sync;

import java.util.Collection;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.syncope.types.AttributableType;
import org.apache.syncope.types.ResourceOperation;
import org.apache.syncope.types.TraceLevel;

public class SyncResult {
    public enum Status {
        SUCCESS,
        FAILURE
    }

    private String message;

    private Status status;

    private AttributableType subjectType;

    private ResourceOperation operation;

    private Long id;

    private String name;

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public AttributableType getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(final AttributableType subjectType) {
        this.subjectType = subjectType;
    }

    public ResourceOperation getOperation() {
        return operation;
    }

    public void setOperation(final ResourceOperation operation) {
        this.operation = operation;
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }

    /**
     * Human readable report string, using the given trace level.
     *
     * @param level trace level
     * @return String for certain levels, null for level NONE
     */
    public String getReportString(final TraceLevel level) {
        if (level == TraceLevel.SUMMARY) {
            // No per entry log in this case.
            return null;
        } else if (level == TraceLevel.FAILURES && status == Status.FAILURE) {
            // only report failures
            return String.format("Failed %s (id/name): %d/%s with message: %s", operation, id, name, message);
        } else {
            // All
            return String.format("%s %s (id/name): %d/%s %s", operation, status, id, name,
                    StringUtils.isBlank(message)
                    ? ""
                    : "with message: " + message);
        }
    }

    /**
     * Helper method to invoke logging per synchronization result for the given trace level.
     *
     * @param results synchronization result
     * @param level trace level
     * @return report as string
     */
    public static String produceReport(final Collection<SyncResult> results, final TraceLevel level) {
        StringBuilder sb = new StringBuilder();
        for (SyncResult result : results) {
            sb.append(result.getReportString(level)).append("\n");
        }
        return sb.toString();
    }
}

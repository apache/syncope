/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.scheduling;

import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.syncope.core.scheduling.AbstractJob.Status;
import org.syncope.types.TraceLevel;

public class SyncResult {

    static enum Operation {

        CREATE,
        UPDATE,
        DELETE

    }

    private String message;

    private Status status;

    private Operation operation;

    private Long userId;

    private String username;

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public Operation getOperation() {
        return this.operation;
    }

    public void setOperation(Operation t) {
        this.operation = t;
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this,
                ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }

    /**
     * Human readable report string, using the given trace level.
     * @param level trace level
     * @return String for certain levels, null for level NONE
     */
    public String getReportString(final TraceLevel level) {
        if (level == TraceLevel.SUMMARY) {
            // No per entry log in this case.
            return null;
        } else if (level == TraceLevel.FAILURES
                && status == Status.FAILURE) {

            // only report failures
            return String.format("Failed %s (id/name): %d/%s with message: %s",
                    operation, userId, username, message);
        } else {
            // All
            return String.format("%s %s (id/ name): %d/ %s %s", operation,
                    status,
                    userId, username,
                    StringUtils.isEmpty(message) ? "" : "with message: "
                    + message);
        }
    }

    /**
     * Helper method to invoke logging per synchronization result for the
     * given trace level.
     * @param results synchronization result
     * @param level trace level
     * @return report as string
     */
    public static String reportSetOfSynchronizationResult(
            final Collection<SyncResult> results,
            final TraceLevel level) {

        StringBuilder sb = new StringBuilder();
        for (SyncResult sr : results) {
            sb.append(sr.getReportString(level)).append("\n");
        }
        return sb.toString();
    }
}

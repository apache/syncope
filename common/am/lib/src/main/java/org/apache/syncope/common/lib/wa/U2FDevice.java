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
package org.apache.syncope.common.lib.wa;

import java.time.OffsetDateTime;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.syncope.common.lib.BaseBean;

public class U2FDevice implements BaseBean {

    private static final long serialVersionUID = 1185073386484048953L;

    public static class Builder {

        private final U2FDevice instance = new U2FDevice();

        public U2FDevice.Builder issueDate(final OffsetDateTime issued) {
            instance.setIssueDate(issued);
            return this;
        }

        public U2FDevice.Builder record(final String record) {
            instance.setRecord(record);
            return this;
        }

        public U2FDevice.Builder id(final long id) {
            instance.setId(id);
            return this;
        }

        public U2FDevice build() {
            return instance;
        }
    }

    private long id;

    private String record;

    private OffsetDateTime issueDate;

    public String getRecord() {
        return record;
    }

    public void setRecord(final String record) {
        this.record = record;
    }

    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public OffsetDateTime getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(final OffsetDateTime issueDate) {
        this.issueDate = issueDate;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(record)
                .append(id)
                .append(issueDate)
                .toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        U2FDevice other = (U2FDevice) obj;
        return new EqualsBuilder()
                .append(this.record, other.record)
                .append(this.id, other.id)
                .append(this.issueDate, other.issueDate)
                .isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("record", record)
                .append("id", id)
                .append("issueDate", issueDate)
                .toString();
    }
}

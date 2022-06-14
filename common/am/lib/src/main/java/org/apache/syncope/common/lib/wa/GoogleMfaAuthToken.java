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

import java.time.LocalDateTime;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.syncope.common.lib.BaseBean;

public class GoogleMfaAuthToken implements BaseBean {

    private static final long serialVersionUID = 2185073386484048953L;

    public static class Builder {

        private final GoogleMfaAuthToken instance = new GoogleMfaAuthToken();

        public GoogleMfaAuthToken.Builder issueDate(final LocalDateTime issueDate) {
            instance.setIssueDate(issueDate);
            return this;
        }

        public GoogleMfaAuthToken.Builder token(final int otp) {
            instance.setOtp(otp);
            return this;
        }

        public GoogleMfaAuthToken build() {
            return instance;
        }
    }

    private int otp;

    private LocalDateTime issueDate;

    public int getOtp() {
        return otp;
    }

    public void setOtp(final int otp) {
        this.otp = otp;
    }

    public LocalDateTime getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(final LocalDateTime issueDate) {
        this.issueDate = issueDate;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(otp)
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
        GoogleMfaAuthToken other = (GoogleMfaAuthToken) obj;
        return new EqualsBuilder()
                .append(this.otp, other.otp)
                .append(this.issueDate, other.issueDate)
                .isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("token", otp)
                .append("issueDate", issueDate)
                .toString();
    }
}

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

import java.time.ZonedDateTime;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.syncope.common.lib.BaseBean;

public class MfaTrustedDevice implements BaseBean {

    private static final long serialVersionUID = 5120423450725182470L;

    private long id;

    private String name;

    private String deviceFingerprint;

    private String recordKey;

    private ZonedDateTime recordDate;

    private ZonedDateTime expirationDate;

    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDeviceFingerprint() {
        return deviceFingerprint;
    }

    public void setDeviceFingerprint(final String deviceFingerprint) {
        this.deviceFingerprint = deviceFingerprint;
    }

    public ZonedDateTime getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(final ZonedDateTime recordDate) {
        this.recordDate = recordDate;
    }

    public String getRecordKey() {
        return recordKey;
    }

    public void setRecordKey(final String recordKey) {
        this.recordKey = recordKey;
    }

    public ZonedDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(final ZonedDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(id)
                .append(name)
                .append(deviceFingerprint)
                .append(recordDate)
                .append(recordKey)
                .append(expirationDate)
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
        MfaTrustedDevice other = (MfaTrustedDevice) obj;
        return new EqualsBuilder()
                .append(this.id, other.id)
                .append(this.name, other.name)
                .append(this.deviceFingerprint, other.deviceFingerprint)
                .append(this.recordDate, other.recordDate)
                .append(this.recordKey, other.recordKey)
                .append(this.expirationDate, other.expirationDate)
                .isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("name", name)
                .append("deviceFingerprint", deviceFingerprint)
                .append("recordDate", recordDate)
                .append("recordKey", recordKey)
                .append("expirationDate", expirationDate)
                .toString();
    }
}

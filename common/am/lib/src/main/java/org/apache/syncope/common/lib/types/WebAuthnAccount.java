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
package org.apache.syncope.common.lib.types;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.syncope.common.lib.BaseBean;

import java.util.List;

public class WebAuthnAccount implements BaseBean {

    private static final long serialVersionUID = 2285073386484048953L;

    private String key;

    private List<WebAuthnDeviceCredential> records;

    private String owner;

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public List<WebAuthnDeviceCredential> getRecords() {
        return records;
    }

    public void setRecords(final List<WebAuthnDeviceCredential> record) {
        this.records = record;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(final String owner) {
        this.owner = owner;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .appendSuper(super.hashCode())
            .append(key)
            .append(records)
            .append(owner)
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
        WebAuthnAccount rhs = (WebAuthnAccount) obj;
        return new EqualsBuilder()
            .appendSuper(super.equals(obj))
            .append(this.key, rhs.key)
            .append(this.records, rhs.records)
            .append(this.owner, rhs.owner)
            .isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("key", key)
            .append("records", records)
            .append("owner", owner)
            .toString();
    }

    public static class Builder {

        private final WebAuthnAccount instance = new WebAuthnAccount();

        public WebAuthnAccount.Builder records(final List<WebAuthnDeviceCredential> records) {
            instance.setRecords(records);
            return this;
        }

        public WebAuthnAccount.Builder owner(final String owner) {
            instance.setOwner(owner);
            return this;
        }

        public WebAuthnAccount.Builder key(final String key) {
            instance.setKey(key);
            return this;
        }

        public WebAuthnAccount build() {
            return instance;
        }
    }
}

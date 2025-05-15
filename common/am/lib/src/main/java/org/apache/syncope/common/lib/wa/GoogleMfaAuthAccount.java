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
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.syncope.common.lib.BaseBean;

public class GoogleMfaAuthAccount implements BaseBean {

    private static final long serialVersionUID = 1274073386484048953L;

    public static class Builder {

        private final GoogleMfaAuthAccount instance = new GoogleMfaAuthAccount();

        public GoogleMfaAuthAccount.Builder id(final Long id) {
            instance.setId(id);
            return this;
        }

        public GoogleMfaAuthAccount.Builder name(final String name) {
            instance.setName(name);
            return this;
        }

        public GoogleMfaAuthAccount.Builder username(final String username) {
            instance.setUsername(username);
            return this;
        }

        public GoogleMfaAuthAccount.Builder secretKey(final String key) {
            instance.setSecretKey(key);
            return this;
        }

        public GoogleMfaAuthAccount.Builder validationCode(final Integer code) {
            instance.setValidationCode(code);
            return this;
        }

        public GoogleMfaAuthAccount.Builder scratchCodes(final List<Integer> codes) {
            instance.setScratchCodes(codes);
            return this;
        }

        public GoogleMfaAuthAccount.Builder registrationDate(final ZonedDateTime date) {
            instance.setRegistrationDate(date);
            return this;
        }

        public GoogleMfaAuthAccount build() {
            return instance;
        }
    }

    private long id;

    private String name;

    private String username;

    private String secretKey;

    private int validationCode;

    private List<Integer> scratchCodes = new ArrayList<>(0);

    private ZonedDateTime registrationDate;

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

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(final String secretKey) {
        this.secretKey = secretKey;
    }

    public int getValidationCode() {
        return validationCode;
    }

    public void setValidationCode(final int validationCode) {
        this.validationCode = validationCode;
    }

    public List<Integer> getScratchCodes() {
        return scratchCodes;
    }

    public void setScratchCodes(final List<Integer> scratchCodes) {
        this.scratchCodes = scratchCodes;
    }

    public ZonedDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(final ZonedDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(id).
                append(name).
                append(username).
                append(secretKey).
                append(validationCode).
                append(scratchCodes).
                append(registrationDate).
                build();
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
        GoogleMfaAuthAccount other = (GoogleMfaAuthAccount) obj;
        return new EqualsBuilder().
                append(id, other.id).
                append(name, other.name).
                append(username, other.username).
                append(secretKey, other.secretKey).
                append(validationCode, other.validationCode).
                append(scratchCodes, other.scratchCodes).
                append(registrationDate, other.registrationDate).
                build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append(id).
                append(name).
                append(username).
                append(secretKey).
                append(validationCode).
                append(scratchCodes).
                append(registrationDate).
                build();
    }
}

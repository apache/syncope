/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@XmlRootElement(name = "googleMfaAuthAccount")
@XmlType
public class GoogleMfaAuthAccount implements Serializable {
    private static final long serialVersionUID = 1274073386484048953L;

    private String key;

    private String secretKey;

    private int validationCode;

    private List<Integer> scratchCodes = new ArrayList<>(0);

    private String owner;

    private Date registrationDate;

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(final String owner) {
        this.owner = owner;
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

    public Date getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(final Date registrationDate) {
        this.registrationDate = registrationDate;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .appendSuper(super.hashCode())
            .append(key)
            .append(secretKey)
            .append(owner)
            .append(scratchCodes)
            .append(validationCode)
            .append(registrationDate)
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
        GoogleMfaAuthAccount rhs = (GoogleMfaAuthAccount) obj;
        return new EqualsBuilder()
            .appendSuper(super.equals(obj))
            .append(this.key, rhs.key)
            .append(this.secretKey, rhs.secretKey)
            .append(this.owner, rhs.owner)
            .append(this.scratchCodes, rhs.scratchCodes)
            .append(this.registrationDate, rhs.registrationDate)
            .append(this.validationCode, rhs.validationCode)
            .isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("key", key)
            .append("secretKey", secretKey)
            .append("owner", owner)
            .append("scratchCodes", scratchCodes)
            .append("registrationDate", registrationDate)
            .append("validationCode", validationCode)
            .toString();
    }

    public static class Builder {

        private final GoogleMfaAuthAccount instance = new GoogleMfaAuthAccount();

        public GoogleMfaAuthAccount.Builder registrationDate(final Date date) {
            instance.setRegistrationDate(date);
            return this;
        }

        public GoogleMfaAuthAccount.Builder scratchCodes(final List<Integer> codes) {
            instance.setScratchCodes(codes);
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

        public GoogleMfaAuthAccount.Builder owner(final String owner) {
            instance.setOwner(owner);
            return this;
        }

        public GoogleMfaAuthAccount.Builder key(final String key) {
            instance.setKey(key);
            return this;
        }

        public GoogleMfaAuthAccount build() {
            return instance;
        }
    }
}

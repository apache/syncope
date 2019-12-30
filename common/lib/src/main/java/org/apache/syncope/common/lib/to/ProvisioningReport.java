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

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.syncope.common.lib.BaseBean;
import org.apache.syncope.common.lib.types.ResourceOperation;

@XmlRootElement(name = "provisioningReport")
@XmlType
public class ProvisioningReport extends BaseBean {

    private static final long serialVersionUID = -5822836001006407497L;

    @XmlEnum
    public enum Status {

        SUCCESS,
        IGNORE,
        FAILURE

    }

    private String message;

    private Status status;

    private String anyType;

    private ResourceOperation operation;

    private String key;

    private String name;

    private String uidValue;

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

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public String getAnyType() {
        return anyType;
    }

    public void setAnyType(final String anyType) {
        this.anyType = anyType;
    }

    public ResourceOperation getOperation() {
        return operation;
    }

    public void setOperation(final ResourceOperation operation) {
        this.operation = operation;
    }

    public String getUidValue() {
        return uidValue;
    }

    public void setUidValue(final String uidValue) {
        this.uidValue = uidValue;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(message).
                append(status).
                append(anyType).
                append(operation).
                append(key).
                append(name).
                append(uidValue).
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
        final ProvisioningReport other = (ProvisioningReport) obj;
        return new EqualsBuilder().
                append(message, other.message).
                append(status, other.status).
                append(anyType, other.anyType).
                append(operation, other.operation).
                append(key, other.key).
                append(name, other.name).
                append(uidValue, other.uidValue).
                build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append(message).
                append(status).
                append(anyType).
                append(operation).
                append(key).
                append(name).
                append(uidValue).
                build();
    }
}

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
package org.apache.syncope.common.lib.scim;

import java.io.Serializable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class SCIMItem implements Serializable {

    private static final long serialVersionUID = -5280546918760080971L;

    private String intAttrName;

    private String extAttrName;

    private String required = "false";

    private String caseExact = "false";

    private String mutability = "readWrite";

    private String returned = "default";

    private String uniqueness = "none";

    private String multiValued = "false";

    public String getIntAttrName() {
        return intAttrName;
    }

    public void setIntAttrName(final String intAttrName) {
        this.intAttrName = intAttrName;
    }

    public String getExtAttrName() {
        return extAttrName;
    }

    public void setExtAttrName(final String extAttrName) {
        this.extAttrName = extAttrName;
    }

    public String getRequired() {
        return required;
    }

    public void setRequired(final String required) {
        this.required = required;
    }

    public String getCaseExact() {
        return caseExact;
    }

    public void setCaseExact(final String caseExact) {
        this.caseExact = caseExact;
    }

    public String getMutability() {
        return mutability;
    }

    public void setMutability(final String mutability) {
        this.mutability = mutability;
    }

    public String getReturned() {
        return returned;
    }

    public void setReturned(final String returned) {
        this.returned = returned;
    }

    public String getUniqueness() {
        return uniqueness;
    }

    public void setUniqueness(final String uniqueness) {
        this.uniqueness = uniqueness;
    }

    public String getMultiValued() {
        return multiValued;
    }

    public void setMultiValued(final String multiValued) {
        this.multiValued = multiValued;
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
        SCIMItem other = (SCIMItem) obj;
        return new EqualsBuilder().
                append(intAttrName, other.intAttrName).
                append(extAttrName, other.extAttrName).
                append(required, other.required).
                append(caseExact, other.caseExact).
                append(mutability, other.mutability).
                append(returned, other.returned).
                append(uniqueness, other.uniqueness).
                append(multiValued, other.multiValued).
                build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(intAttrName).
                append(extAttrName).
                append(required).
                append(caseExact).
                append(mutability).
                append(returned).
                append(uniqueness).
                append(multiValued).
                build();
    }
}

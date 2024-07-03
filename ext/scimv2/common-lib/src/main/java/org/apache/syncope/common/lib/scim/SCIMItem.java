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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.to.Item;

public class SCIMItem extends Item {

    private boolean caseExact = false;

    private boolean mutability = false;

    private SCIMReturned returned = SCIMReturned.DEFAULT;

    private boolean uniqueness = false;

    private boolean multiValued = false;

    public boolean isCaseExact() {
        return caseExact;
    }

    public void setCaseExact(final boolean caseExact) {
        this.caseExact = caseExact;
    }

    public boolean isMutability() {
        return mutability;
    }

    public void setMutability(final boolean mutability) {
        this.mutability = mutability;
    }

    public SCIMReturned getReturned() {
        return returned;
    }

    public void setReturned(final SCIMReturned returned) {
        this.returned = returned;
    }

    public boolean isUniqueness() {
        return uniqueness;
    }

    public void setUniqueness(final boolean uniqueness) {
        this.uniqueness = uniqueness;
    }

    public boolean isMultiValued() {
        return multiValued;
    }

    public void setMultiValued(final boolean multiValued) {
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
                append(mandatoryCondition, other.mandatoryCondition).
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
                append(mandatoryCondition).
                append(caseExact).
                append(mutability).
                append(returned).
                append(uniqueness).
                append(multiValued).
                build();
    }
}

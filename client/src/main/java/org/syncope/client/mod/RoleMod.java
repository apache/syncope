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
package org.syncope.client.mod;

import java.util.ArrayList;
import java.util.List;

public class RoleMod extends AbstractAttributableMod {

    private static final long serialVersionUID = 7455805264680210747L;

    private String name;

    private Boolean inheritAttributes;

    private Boolean inheritDerivedAttributes;

    private Boolean inheritVirtualAttributes;

    private Boolean inheritAccountPolicy;

    private Boolean inheritPasswordPolicy;

    private List<String> entitlements;

    private ReferenceMod passwordPolicy;

    private ReferenceMod accountPolicy;

    public RoleMod() {
        super();

        entitlements = new ArrayList<String>();
    }

    public Boolean getInheritAttributes() {
        return inheritAttributes;
    }

    public void setInheritAttributes(
            final Boolean inheritAttributes) {
        this.inheritAttributes = inheritAttributes;
    }

    public Boolean getInheritDerivedAttributes() {
        return inheritDerivedAttributes;
    }

    public void setInheritDerivedAttributes(
            final Boolean inheritDerivedAttributes) {
        this.inheritDerivedAttributes = inheritDerivedAttributes;
    }

    public Boolean getInheritVirtualAttributes() {
        return inheritVirtualAttributes;
    }

    public void setInheritVirtualAttributes(
            final Boolean inheritVirtualAttributes) {
        this.inheritVirtualAttributes = inheritVirtualAttributes;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public boolean addEntitlement(final String entitlement) {
        return entitlements.add(entitlement);
    }

    public boolean removeEntitlement(final String entitlement) {
        return entitlements.remove(entitlement);
    }

    public List<String> getEntitlements() {
        return entitlements;
    }

    public void setEntitlements(final List<String> entitlements) {
        this.entitlements.clear();
        if (entitlements != null && !entitlements.isEmpty()) {
            this.entitlements.addAll(entitlements);
        }
    }

    public ReferenceMod getPasswordPolicy() {
        return passwordPolicy;
    }

    public void setPasswordPolicy(final ReferenceMod passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }

    public Boolean getInheritPasswordPolicy() {
        return inheritPasswordPolicy;
    }

    public void setInheritPasswordPolicy(final Boolean inheritPasswordPolicy) {
        this.inheritPasswordPolicy = inheritPasswordPolicy;
    }

    public ReferenceMod getAccountPolicy() {
        return accountPolicy;
    }

    public void setAccountPolicy(final ReferenceMod accountPolicy) {
        this.accountPolicy = accountPolicy;
    }

    public Boolean getInheritAccountPolicy() {
        return inheritAccountPolicy;
    }

    public void setInheritAccountPolicy(final Boolean inheritAccountPolicy) {
        this.inheritAccountPolicy = inheritAccountPolicy;
    }
}

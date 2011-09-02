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
import org.syncope.client.to.PasswordPolicyTO;

public class RoleMod extends AbstractAttributableMod {

    private static final long serialVersionUID = 7455805264680210747L;

    private String name;

    private boolean changeInheritAttributes;

    private boolean changeInheritDerivedAttributes;

    private boolean changeInheritVirtualAttributes;

    private List<String> entitlements;

    private PasswordPolicyTO passwordPolicy;

    public RoleMod() {
        super();

        entitlements = new ArrayList<String>();
    }

    public boolean isChangeInheritAttributes() {
        return changeInheritAttributes;
    }

    public void setChangeInheritAttributes(boolean changeInheritAttributes) {
        this.changeInheritAttributes = changeInheritAttributes;
    }

    public boolean isChangeInheritDerivedAttributes() {
        return changeInheritDerivedAttributes;
    }

    public void setChangeInheritDerivedAttributes(
            boolean changeInheritDerivedAttributes) {

        this.changeInheritDerivedAttributes = changeInheritDerivedAttributes;
    }

    public boolean isChangeInheritVirtualAttributes() {
        return changeInheritVirtualAttributes;
    }

    public void setChangeInheritVirtualAttributes(
            boolean changeInheritVirtualAttributes) {

        this.changeInheritVirtualAttributes = changeInheritVirtualAttributes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean addEntitlement(String entitlement) {
        return entitlements.add(entitlement);
    }

    public boolean removeEntitlement(String entitlement) {
        return entitlements.remove(entitlement);
    }

    public List<String> getEntitlements() {
        return entitlements;
    }

    public void setEntitlements(List<String> entitlements) {
        this.entitlements.clear();
        if (entitlements != null || !entitlements.isEmpty()) {
            this.entitlements.addAll(entitlements);
        }
    }

    public PasswordPolicyTO getPasswordPolicy() {
        return passwordPolicy;
    }

    public void setPasswordPolicy(PasswordPolicyTO passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }
}

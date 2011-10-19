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
package org.syncope.client.to;

import java.util.ArrayList;
import java.util.List;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties({"displayName", "empty"})
public class RoleTO extends AbstractAttributableTO {

    private static final long serialVersionUID = -7785920258290147542L;

    private String name;

    private long parent;

    private boolean inheritAttributes;

    private boolean inheritDerivedAttributes;

    private boolean inheritVirtualAttributes;

    private boolean inheritPasswordPolicy;

    private boolean inheritAccountPolicy;

    private List<String> entitlements;

    private Long passwordPolicy;

    private Long accountPolicy;

    public RoleTO() {
        entitlements = new ArrayList<String>();
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public long getParent() {
        return parent;
    }

    public void setParent(final long parent) {
        this.parent = parent;
    }

    public boolean isInheritAttributes() {
        return inheritAttributes;
    }

    public void setInheritAttributes(final boolean inheritAttributes) {
        this.inheritAttributes = inheritAttributes;
    }

    public boolean isInheritDerivedAttributes() {
        return inheritDerivedAttributes;
    }

    public void setInheritDerivedAttributes(
            final boolean inheritDerivedAttributes) {

        this.inheritDerivedAttributes = inheritDerivedAttributes;
    }

    public boolean isInheritVirtualAttributes() {
        return inheritVirtualAttributes;
    }

    public void setInheritVirtualAttributes(boolean inheritVirtualAttributes) {
        this.inheritVirtualAttributes = inheritVirtualAttributes;
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
        if (entitlements != null && !entitlements.isEmpty()) {
            this.entitlements.addAll(entitlements);
        }
    }

    public Long getPasswordPolicy() {
        return passwordPolicy;
    }

    public void setPasswordPolicy(Long passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }

    public boolean isInheritPasswordPolicy() {
        return inheritPasswordPolicy;
    }

    /**
     * Specify if password policy must be inherited.
     * In this case eventual passwordPolicy occurrence will be ignored.
     * @param inheritPasswordPolicy 'true' to inherit policy, false otherwise.
     */
    public void setInheritPasswordPolicy(boolean inheritPasswordPolicy) {
        this.inheritPasswordPolicy = inheritPasswordPolicy;
    }

    public Long getAccountPolicy() {
        return accountPolicy;
    }

    public void setAccountPolicy(Long accountPolicy) {
        this.accountPolicy = accountPolicy;
    }

    public boolean isInheritAccountPolicy() {
        return inheritAccountPolicy;
    }

    /**
     * Specify if account policy must be inherited.
     * In this case eventual accountPolicy occurrence will be ignored.
     * @param inheritAccountPolicy 'true' to inherit policy, false otherwise.
     */
    public void setInheritAccountPolicy(boolean inheritAccountPolicy) {
        this.inheritAccountPolicy = inheritAccountPolicy;
    }

    public String getDisplayName() {
        return getId() + " " + getName();
    }

    public String getEmpty() {
        return "";
    }

    public static long fromDisplayName(final String displayName) {
        long result = 0;
        if (displayName != null && !displayName.isEmpty()
                && displayName.indexOf(' ') != -1) {

            try {
                result = Long.valueOf(displayName.split(" ")[0]);
            } catch (NumberFormatException e) {
                // just to avoid PMD warning about "empty catch block"
                result = 0;
            }
        }

        return result;
    }
}

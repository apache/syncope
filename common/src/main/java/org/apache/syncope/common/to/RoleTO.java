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
package org.apache.syncope.common.to;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "role")
@XmlType
@JsonIgnoreProperties({"displayName"})
public class RoleTO extends AbstractAttributableTO {

    private static final long serialVersionUID = -7785920258290147542L;

    private String name;

    private long parent;

    private Long userOwner;

    private Long roleOwner;

    private boolean inheritOwner;

    private boolean inheritTemplates;

    private boolean inheritAttrs;

    private boolean inheritDerAttrs;

    private boolean inheritVirAttrs;

    private boolean inheritPasswordPolicy;

    private boolean inheritAccountPolicy;

    private final List<String> entitlements = new ArrayList<String>();

    private List<String> rAttrTemplates = new ArrayList<String>();

    private List<String> rDerAttrTemplates = new ArrayList<String>();

    private List<String> rVirAttrTemplates = new ArrayList<String>();

    private List<String> mAttrTemplates = new ArrayList<String>();

    private List<String> mDerAttrTemplates = new ArrayList<String>();

    private List<String> mVirAttrTemplates = new ArrayList<String>();

    private Long passwordPolicy;

    private Long accountPolicy;

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

    public Long getUserOwner() {
        return userOwner;
    }

    public void setUserOwner(final Long userOwner) {
        this.userOwner = userOwner;
    }

    public Long getRoleOwner() {
        return roleOwner;
    }

    public void setRoleOwner(final Long roleOwner) {
        this.roleOwner = roleOwner;
    }

    public boolean isInheritOwner() {
        return inheritOwner;
    }

    public void setInheritOwner(final boolean inheritOwner) {
        this.inheritOwner = inheritOwner;
    }

    public boolean isInheritTemplates() {
        return inheritTemplates;
    }

    public void setInheritTemplates(boolean inheritTemplates) {
        this.inheritTemplates = inheritTemplates;
    }

    public boolean isInheritAttrs() {
        return inheritAttrs;
    }

    public void setInheritAttrs(final boolean inheritAttrs) {
        this.inheritAttrs = inheritAttrs;
    }

    public boolean isInheritDerAttrs() {
        return inheritDerAttrs;
    }

    public void setInheritDerAttrs(final boolean inheritDerAttrs) {
        this.inheritDerAttrs = inheritDerAttrs;
    }

    public boolean isInheritVirAttrs() {
        return inheritVirAttrs;
    }

    public void setInheritVirAttrs(final boolean inheritVirAttrs) {
        this.inheritVirAttrs = inheritVirAttrs;
    }

    @XmlElementWrapper(name = "entitlements")
    @XmlElement(name = "entitlement")
    @JsonProperty("entitlements")
    public List<String> getEntitlements() {
        return entitlements;
    }

    @XmlElementWrapper(name = "rAttrTemplates")
    @XmlElement(name = "rAttrTemplate")
    @JsonProperty("rAttrTemplates")
    public List<String> getRAttrTemplates() {
        return rAttrTemplates;
    }

    @XmlElementWrapper(name = "rDerAttrTemplates")
    @XmlElement(name = "rDerAttrTemplate")
    @JsonProperty("rDerAttrTemplates")
    public List<String> getRDerAttrTemplates() {
        return rDerAttrTemplates;
    }

    @XmlElementWrapper(name = "rVirAttrTemplates")
    @XmlElement(name = "rVirAttrTemplate")
    @JsonProperty("rVirAttrTemplates")
    public List<String> getRVirAttrTemplates() {
        return rVirAttrTemplates;
    }

    @XmlElementWrapper(name = "mAttrTemplates")
    @XmlElement(name = "mAttrTemplate")
    @JsonProperty("mAttrTemplates")
    public List<String> getMAttrTemplates() {
        return mAttrTemplates;
    }

    @XmlElementWrapper(name = "mDerAttrTemplates")
    @XmlElement(name = "mDerAttrTemplate")
    @JsonProperty("mDerAttrTemplates")
    public List<String> getMDerAttrTemplates() {
        return mDerAttrTemplates;
    }

    @XmlElementWrapper(name = "mVirAttrTemplates")
    @XmlElement(name = "mVirAttrTemplate")
    @JsonProperty("mVirAttrTemplates")
    public List<String> getMVirAttrTemplates() {
        return mVirAttrTemplates;
    }

    public Long getPasswordPolicy() {
        return passwordPolicy;
    }

    public void setPasswordPolicy(final Long passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }

    public boolean isInheritPasswordPolicy() {
        return inheritPasswordPolicy;
    }

    /**
     * Specify if password policy must be inherited. In this case eventual passwordPolicy occurrence will be ignored.
     *
     * @param inheritPasswordPolicy 'true' to inherit policy, false otherwise.
     */
    public void setInheritPasswordPolicy(final boolean inheritPasswordPolicy) {
        this.inheritPasswordPolicy = inheritPasswordPolicy;
    }

    public Long getAccountPolicy() {
        return accountPolicy;
    }

    public void setAccountPolicy(final Long accountPolicy) {
        this.accountPolicy = accountPolicy;
    }

    public boolean isInheritAccountPolicy() {
        return inheritAccountPolicy;
    }

    /**
     * Specify if account policy must be inherited. In this case eventual accountPolicy occurrence will be ignored.
     *
     * @param inheritAccountPolicy 'true' to inherit policy, false otherwise.
     */
    public void setInheritAccountPolicy(final boolean inheritAccountPolicy) {
        this.inheritAccountPolicy = inheritAccountPolicy;
    }

    public String getDisplayName() {
        return getId() + " " + getName();
    }

    public static long fromDisplayName(final String displayName) {
        long result = 0;
        if (displayName != null && !displayName.isEmpty() && displayName.indexOf(' ') != -1) {
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

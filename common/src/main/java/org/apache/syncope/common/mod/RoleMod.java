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
package org.apache.syncope.common.mod;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "roleMod")
@XmlType
public class RoleMod extends AbstractAttributableMod {

    private static final long serialVersionUID = 7455805264680210747L;

    private String name;

    private ReferenceMod userOwner;

    private ReferenceMod roleOwner;

    private Boolean inheritOwner;

    private Boolean inheritAttributes;

    private Boolean inheritDerivedAttributes;

    private Boolean inheritVirtualAttributes;

    private Boolean inheritAccountPolicy;

    private Boolean inheritPasswordPolicy;

    private boolean modEntitlements;
    
    private List<String> entitlements = new ArrayList<String>();

    private ReferenceMod passwordPolicy;

    private ReferenceMod accountPolicy;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public ReferenceMod getUserOwner() {
        return userOwner;
    }

    public void setUserOwner(ReferenceMod userOwner) {
        this.userOwner = userOwner;
    }

    public ReferenceMod getRoleOwner() {
        return roleOwner;
    }

    public void setRoleOwner(ReferenceMod roleOwner) {
        this.roleOwner = roleOwner;
    }

    public Boolean getInheritOwner() {
        return inheritOwner;
    }

    public void setInheritOwner(Boolean inheritOwner) {
        this.inheritOwner = inheritOwner;
    }

    public Boolean getInheritAttributes() {
        return inheritAttributes;
    }

    public void setInheritAttributes(final Boolean inheritAttributes) {
        this.inheritAttributes = inheritAttributes;
    }

    public Boolean getInheritDerivedAttributes() {
        return inheritDerivedAttributes;
    }

    public void setInheritDerivedAttributes(final Boolean inheritDerivedAttributes) {
        this.inheritDerivedAttributes = inheritDerivedAttributes;
    }

    public Boolean getInheritVirtualAttributes() {
        return inheritVirtualAttributes;
    }

    public void setInheritVirtualAttributes(final Boolean inheritVirtualAttributes) {
        this.inheritVirtualAttributes = inheritVirtualAttributes;
    }

    public boolean isModEntitlements() {
        return modEntitlements;
    }

    public void setModEntitlements(final boolean modEntitlements) {
        this.modEntitlements = modEntitlements;
    }    

    @XmlElementWrapper(name = "entitlements")
    @XmlElement(name = "entitlement")
    @JsonProperty("entitlements")
    public List<String> getEntitlements() {
        return entitlements;
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

    @JsonIgnore
    @Override
    public boolean isEmpty() {
        return super.isEmpty() && name == null && userOwner == null && roleOwner == null
                && inheritOwner == null && inheritAccountPolicy == null && inheritPasswordPolicy == null
                && inheritAttributes == null && inheritDerivedAttributes == null && inheritVirtualAttributes == null
                && accountPolicy == null && passwordPolicy == null && (entitlements == null || entitlements.isEmpty());
    }
}

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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "group")
@XmlType
@JsonIgnoreProperties({ "displayName" })
public class GroupTO extends AbstractSubjectTO {

    private static final long serialVersionUID = -7785920258290147542L;

    private String name;

    private long parent;

    private Long userOwner;

    private Long groupOwner;

    private boolean inheritOwner;

    private boolean inheritTemplates;

    private boolean inheritPlainAttrs;

    private boolean inheritDerAttrs;

    private boolean inheritVirAttrs;

    private boolean inheritPasswordPolicy;

    private boolean inheritAccountPolicy;

    private final List<String> entitlements = new ArrayList<>();

    private final List<String> gPlainAttrTemplates = new ArrayList<>();

    private final List<String> gDerAttrTemplates = new ArrayList<>();

    private final List<String> gVirAttrTemplates = new ArrayList<>();

    private final List<String> mPlainAttrTemplates = new ArrayList<>();

    private final List<String> mDerAttrTemplates = new ArrayList<>();

    private final List<String> mVirAttrTemplates = new ArrayList<>();

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

    public Long getGroupOwner() {
        return groupOwner;
    }

    public void setGroupOwner(final Long groupOwner) {
        this.groupOwner = groupOwner;
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

    public void setInheritTemplates(final boolean inheritTemplates) {
        this.inheritTemplates = inheritTemplates;
    }

    public boolean isInheritPlainAttrs() {
        return inheritPlainAttrs;
    }

    public void setInheritPlainAttrs(final boolean inheritPlainAttrs) {
        this.inheritPlainAttrs = inheritPlainAttrs;
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

    @XmlElementWrapper(name = "gPlainAttrTemplates")
    @XmlElement(name = "gPlainAttrTemplate")
    @JsonProperty("gPlainAttrTemplates")
    public List<String> getGPlainAttrTemplates() {
        return gPlainAttrTemplates;
    }

    @XmlElementWrapper(name = "gDerAttrTemplates")
    @XmlElement(name = "gDerAttrTemplate")
    @JsonProperty("gDerAttrTemplates")
    public List<String> getGDerAttrTemplates() {
        return gDerAttrTemplates;
    }

    @XmlElementWrapper(name = "gVirAttrTemplates")
    @XmlElement(name = "gVirAttrTemplate")
    @JsonProperty("gVirAttrTemplates")
    public List<String> getGVirAttrTemplates() {
        return gVirAttrTemplates;
    }

    @XmlElementWrapper(name = "mPlainAttrTemplates")
    @XmlElement(name = "mPlainAttrTemplate")
    @JsonProperty("mPlainAttrTemplates")
    public List<String> getMPlainAttrTemplates() {
        return mPlainAttrTemplates;
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
        return getKey() + " " + getName();
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

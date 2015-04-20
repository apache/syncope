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
package org.apache.syncope.common.lib.mod;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "groupMod")
@XmlType
public class GroupMod extends AbstractSubjectMod {

    private static final long serialVersionUID = 7455805264680210747L;

    private String name;

    private ReferenceMod userOwner;

    private ReferenceMod groupOwner;

    private Boolean inheritOwner;

    private Boolean inheritTemplates;

    private Boolean inheritPlainAttrs;

    private Boolean inheritDerAttrs;

    private Boolean inheritVirAttrs;

    private Boolean inheritAccountPolicy;

    private Boolean inheritPasswordPolicy;

    private boolean modGAttrTemplates;

    private final List<String> gPlainAttrTemplates = new ArrayList<>();

    private boolean modGDerAttrTemplates;

    private final List<String> gDerAttrTemplates = new ArrayList<>();

    private boolean modGVirAttrTemplates;

    private final List<String> gVirAttrTemplates = new ArrayList<>();

    private boolean modMAttrTemplates;

    private final List<String> mPlainAttrTemplates = new ArrayList<>();

    private boolean modMDerAttrTemplates;

    private final List<String> mDerAttrTemplates = new ArrayList<>();

    private boolean modMVirAttrTemplates;

    private final List<String> mVirAttrTemplates = new ArrayList<>();

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

    public void setUserOwner(final ReferenceMod userOwner) {
        this.userOwner = userOwner;
    }

    public ReferenceMod getGroupOwner() {
        return groupOwner;
    }

    public void setGroupOwner(final ReferenceMod groupOwner) {
        this.groupOwner = groupOwner;
    }

    public Boolean getInheritOwner() {
        return inheritOwner;
    }

    public void setInheritOwner(final Boolean inheritOwner) {
        this.inheritOwner = inheritOwner;
    }

    public Boolean getInheritTemplates() {
        return inheritTemplates;
    }

    public void setInheritTemplates(final Boolean inheritTemplates) {
        this.inheritTemplates = inheritTemplates;
    }

    public Boolean getInheritPlainAttrs() {
        return inheritPlainAttrs;
    }

    public void setInheritPlainAttrs(final Boolean inheritAttrs) {
        this.inheritPlainAttrs = inheritAttrs;
    }

    public Boolean getInheritDerAttrs() {
        return inheritDerAttrs;
    }

    public void setInheritDerAttrs(final Boolean inheritDerAttrs) {
        this.inheritDerAttrs = inheritDerAttrs;
    }

    public Boolean getInheritVirAttrs() {
        return inheritVirAttrs;
    }

    public void setInheritVirAttrs(final Boolean inheritVirAttrs) {
        this.inheritVirAttrs = inheritVirAttrs;
    }

    public boolean isModGAttrTemplates() {
        return modGAttrTemplates;
    }

    public void setModGAttrTemplates(final boolean modGAttrTemplates) {
        this.modGAttrTemplates = modGAttrTemplates;
    }

    @XmlElementWrapper(name = "gPlainAttrTemplates")
    @XmlElement(name = "gAttrTemplate")
    @JsonProperty("gPlainAttrTemplates")
    public List<String> getGPlainAttrTemplates() {
        return gPlainAttrTemplates;
    }

    public boolean isModGDerAttrTemplates() {
        return modGDerAttrTemplates;
    }

    public void setModGDerAttrTemplates(final boolean modGDerAttrTemplates) {
        this.modGDerAttrTemplates = modGDerAttrTemplates;
    }

    @XmlElementWrapper(name = "gDerAttrTemplates")
    @XmlElement(name = "gDerAttrTemplate")
    @JsonProperty("gDerAttrTemplates")
    public List<String> getGDerAttrTemplates() {
        return gDerAttrTemplates;
    }

    public boolean isModGVirAttrTemplates() {
        return modGVirAttrTemplates;
    }

    public void setModGVirAttrTemplates(final boolean modGVirAttrTemplates) {
        this.modGVirAttrTemplates = modGVirAttrTemplates;
    }

    @XmlElementWrapper(name = "gVirAttrTemplates")
    @XmlElement(name = "gVirAttrTemplate")
    @JsonProperty("gVirAttrTemplates")
    public List<String> getGVirAttrTemplates() {
        return gVirAttrTemplates;
    }

    public boolean isModMAttrTemplates() {
        return modMAttrTemplates;
    }

    public void setModMAttrTemplates(final boolean modMAttrTemplates) {
        this.modMAttrTemplates = modMAttrTemplates;
    }

    @XmlElementWrapper(name = "mPlainAttrTemplates")
    @XmlElement(name = "mAttrTemplate")
    @JsonProperty("mPlainAttrTemplates")
    public List<String> getMPlainAttrTemplates() {
        return mPlainAttrTemplates;
    }

    public boolean isModMDerAttrTemplates() {
        return modMDerAttrTemplates;
    }

    public void setModMDerAttrTemplates(final boolean modMDerAttrTemplates) {
        this.modMDerAttrTemplates = modMDerAttrTemplates;
    }

    @XmlElementWrapper(name = "mDerAttrTemplates")
    @XmlElement(name = "mDerAttrTemplate")
    @JsonProperty("mDerAttrTemplates")
    public List<String> getMDerAttrTemplates() {
        return mDerAttrTemplates;
    }

    public boolean isModMVirAttrTemplates() {
        return modMVirAttrTemplates;
    }

    public void setModMVirAttrTemplates(final boolean modMVirAttrTemplates) {
        this.modMVirAttrTemplates = modMVirAttrTemplates;
    }

    @XmlElementWrapper(name = "mVirAttrTemplates")
    @XmlElement(name = "mVirAttrTemplate")
    @JsonProperty("mVirAttrTemplates")
    public List<String> getMVirAttrTemplates() {
        return mVirAttrTemplates;
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
        return super.isEmpty() && name == null && userOwner == null && groupOwner == null
                && inheritTemplates == null && inheritOwner == null
                && inheritAccountPolicy == null && inheritPasswordPolicy == null
                && inheritPlainAttrs == null && inheritDerAttrs == null && inheritVirAttrs == null
                && accountPolicy == null && passwordPolicy == null
                && gPlainAttrTemplates.isEmpty() && gDerAttrTemplates.isEmpty() && gVirAttrTemplates.isEmpty()
                && mPlainAttrTemplates.isEmpty() && mDerAttrTemplates.isEmpty() && mVirAttrTemplates.isEmpty();
    }
}

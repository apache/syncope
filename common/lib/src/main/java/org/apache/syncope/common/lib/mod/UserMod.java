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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "userMod")
@XmlType
public class UserMod extends AnyMod {

    private static final long serialVersionUID = 3081848906558106204L;

    private String username;

    private String password;

    private final List<Long> relationshipsToAdd = new ArrayList<>();

    private final List<Long> relationshipsToRemove = new ArrayList<>();

    private final List<Long> membershipsToAdd = new ArrayList<>();

    private final List<Long> membershipsToRemove = new ArrayList<>();

    private final Set<Long> rolesToAdd = new HashSet<>();

    private final Set<Long> rolesToRemove = new HashSet<>();

    private StatusMod pwdPropRequest;

    private Long securityQuestion;

    private String securityAnswer;

    private boolean mustChangePassword;

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    @XmlElementWrapper(name = "relationshipsToAdd")
    @XmlElement(name = "relationship")
    @JsonProperty("relationshipsToAdd")
    public List<Long> getRelationshipsToAdd() {
        return relationshipsToAdd;
    }

    @XmlElementWrapper(name = "urelationshipsToRemove")
    @XmlElement(name = "urelationship")
    @JsonProperty("urelationshipsToRemove")
    public List<Long> getRelationshipsToRemove() {
        return relationshipsToRemove;
    }

    @XmlElementWrapper(name = "membershipsToAdd")
    @XmlElement(name = "membership")
    @JsonProperty("membershipsToAdd")
    public List<Long> getMembershipsToAdd() {
        return membershipsToAdd;
    }

    @XmlElementWrapper(name = "membershipsToRemove")
    @XmlElement(name = "membership")
    @JsonProperty("membershipsToRemove")
    public List<Long> getMembershipsToRemove() {
        return membershipsToRemove;
    }

    @XmlElementWrapper(name = "rolesToAdd")
    @XmlElement(name = "role")
    @JsonProperty("rolesToAdd")
    public Set<Long> getRolesToAdd() {
        return rolesToAdd;
    }

    @XmlElementWrapper(name = "rolesToRemove")
    @XmlElement(name = "role")
    @JsonProperty("rolesToRemove")
    public Set<Long> getRolesToRemove() {
        return rolesToRemove;
    }

    public StatusMod getPwdPropRequest() {
        return pwdPropRequest;
    }

    public void setPwdPropRequest(final StatusMod pwdPropRequest) {
        this.pwdPropRequest = pwdPropRequest;
    }

    public Long getSecurityQuestion() {
        return securityQuestion;
    }

    public void setSecurityQuestion(final Long securityQuestion) {
        this.securityQuestion = securityQuestion;
    }

    public String getSecurityAnswer() {
        return securityAnswer;
    }

    public void setSecurityAnswer(final String securityAnswer) {
        this.securityAnswer = securityAnswer;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(final boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    @JsonIgnore
    @Override
    public boolean isEmpty() {
        return super.isEmpty()
                && password == null
                && username == null
                && pwdPropRequest == null
                && securityQuestion == null
                && securityAnswer == null
                && !mustChangePassword;
    }
}

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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.types.AnyTypeKind;

@XmlRootElement(name = "user")
@XmlType
public class UserTO extends AnyTO implements RelatableTO, GroupableTO {

    private static final long serialVersionUID = 7791304495192615740L;

    private String password;

    private final List<String> roles = new ArrayList<>();

    private final List<String> dynRoles = new ArrayList<>();

    private String token;

    private Date tokenExpireTime;

    private String username;

    private Date lastLoginDate;

    private Date changePwdDate;

    private Integer failedLogins;

    private String securityQuestion;

    private String securityAnswer;

    private boolean mustChangePassword;

    private final List<RelationshipTO> relationships = new ArrayList<>();

    private final List<MembershipTO> memberships = new ArrayList<>();

    private final List<String> dynGroups = new ArrayList<>();

    @Override
    public String getType() {
        return AnyTypeKind.USER.name();
    }

    @Override
    public void setType(final String type) {
        // fixed
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    @XmlElementWrapper(name = "roles")
    @XmlElement(name = "role")
    @JsonProperty("roles")
    public List<String> getRoles() {
        return roles;
    }

    @XmlElementWrapper(name = "dynRoles")
    @XmlElement(name = "role")
    @JsonProperty("dynRoles")
    public List<String> getDynRoles() {
        return dynRoles;
    }

    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    public Date getTokenExpireTime() {
        return tokenExpireTime == null
                ? null
                : new Date(tokenExpireTime.getTime());
    }

    public void setTokenExpireTime(final Date tokenExpireTime) {
        if (tokenExpireTime != null) {
            this.tokenExpireTime = new Date(tokenExpireTime.getTime());
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public Date getChangePwdDate() {
        return changePwdDate;
    }

    public Integer getFailedLogins() {
        return failedLogins;
    }

    public Date getLastLoginDate() {
        return lastLoginDate;
    }

    public void setChangePwdDate(final Date changePwdDate) {
        this.changePwdDate = changePwdDate;
    }

    public void setFailedLogins(final Integer failedLogins) {
        this.failedLogins = failedLogins;
    }

    public void setLastLoginDate(final Date lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }

    public String getSecurityQuestion() {
        return securityQuestion;
    }

    public void setSecurityQuestion(final String securityQuestion) {
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

    @XmlElementWrapper(name = "relationships")
    @XmlElement(name = "relationship")
    @JsonProperty("relationships")
    @Override
    public List<RelationshipTO> getRelationships() {
        return relationships;
    }

    @JsonIgnore
    @Override
    public Map<Pair<String, String>, RelationshipTO> getRelationshipMap() {
        Map<Pair<String, String>, RelationshipTO> result = new HashMap<>(getRelationships().size());
        for (RelationshipTO relationship : getRelationships()) {
            result.put(Pair.of(relationship.getType(), relationship.getRightKey()), relationship);
        }
        return Collections.unmodifiableMap(result);
    }

    @XmlElementWrapper(name = "memberships")
    @XmlElement(name = "membership")
    @JsonProperty("memberships")
    @Override
    public List<MembershipTO> getMemberships() {
        return memberships;
    }

    @JsonIgnore
    @Override
    public Map<String, MembershipTO> getMembershipMap() {
        Map<String, MembershipTO> result = new HashMap<>(getMemberships().size());
        for (MembershipTO membership : getMemberships()) {
            result.put(membership.getRightKey(), membership);
        }
        result = Collections.unmodifiableMap(result);

        return result;
    }

    @XmlElementWrapper(name = "dynGroups")
    @XmlElement(name = "role")
    @JsonProperty("dynGroups")
    @Override
    public List<String> getDynGroups() {
        return dynGroups;
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE) {

            @Override
            protected boolean accept(final Field f) {
                return super.accept(f) && !f.getName().equals("password");
            }
        }.toString();
    }
}

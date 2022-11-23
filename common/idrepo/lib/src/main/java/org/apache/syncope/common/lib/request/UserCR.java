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
package org.apache.syncope.common.lib.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.to.GroupableRelatableTO;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.RelationshipTO;

@JsonPropertyOrder(value = { "_class", "username" })
@Schema(allOf = { AnyCR.class })
public class UserCR extends AnyCR implements GroupableRelatableTO {

    private static final long serialVersionUID = 2808404532469484940L;

    public static class Builder extends AnyCR.Builder<UserCR, Builder> {

        public Builder(final String realm, final String username) {
            super(realm);
            getInstance().setUsername(username);
        }

        @Override
        protected UserCR newInstance() {
            return new UserCR();
        }

        public Builder password(final String password) {
            getInstance().setPassword(password);
            return this;
        }

        public Builder storePassword(final boolean storePassword) {
            getInstance().setStorePassword(storePassword);
            return this;
        }

        public Builder securityQuestion(final String securityQuestion) {
            getInstance().setSecurityQuestion(securityQuestion);
            return this;
        }

        public Builder securityAnswer(final String securityAnswer) {
            getInstance().setSecurityAnswer(securityAnswer);
            return this;
        }

        public Builder mustChangePassword(final boolean mustChangePassword) {
            getInstance().setMustChangePassword(mustChangePassword);
            return this;
        }

        public Builder relationship(final RelationshipTO relationship) {
            getInstance().getRelationships().add(relationship);
            return this;
        }

        public Builder relationships(final RelationshipTO... relationships) {
            getInstance().getRelationships().addAll(List.of(relationships));
            return this;
        }

        public Builder relationships(final Collection<RelationshipTO> relationships) {
            getInstance().getRelationships().addAll(relationships);
            return this;
        }

        public Builder membership(final MembershipTO membership) {
            getInstance().getMemberships().add(membership);
            return this;
        }

        public Builder memberships(final MembershipTO... memberships) {
            getInstance().getMemberships().addAll(List.of(memberships));
            return this;
        }

        public Builder memberships(final Collection<MembershipTO> memberships) {
            getInstance().getMemberships().addAll(memberships);
            return this;
        }

        public Builder role(final String role) {
            getInstance().getRoles().add(role);
            return this;
        }

        public Builder roles(final String... roles) {
            getInstance().getRoles().addAll(List.of(roles));
            return this;
        }

        public Builder roles(final Collection<String> roles) {
            getInstance().getRoles().addAll(roles);
            return this;
        }

        public Builder linkedAccount(final LinkedAccountTO linkedAccount) {
            getInstance().getLinkedAccounts().add(linkedAccount);
            return this;
        }

        public Builder linkedAccounts(final LinkedAccountTO... linkedAccounts) {
            getInstance().getLinkedAccounts().addAll(List.of(linkedAccounts));
            return this;
        }

        public Builder linkedAccounts(final Collection<LinkedAccountTO> linkedAccounts) {
            getInstance().getLinkedAccounts().addAll(linkedAccounts);
            return this;
        }
    }

    private String username;

    private String password;

    private boolean storePassword = true;

    private String securityQuestion;

    private String securityAnswer;

    private boolean mustChangePassword;

    private final List<RelationshipTO> relationships = new ArrayList<>();

    private final List<MembershipTO> memberships = new ArrayList<>();

    private final Set<String> roles = new HashSet<>();

    private final List<LinkedAccountTO> linkedAccounts = new ArrayList<>();

    @JacksonXmlProperty(localName = "_class", isAttribute = true)
    @JsonProperty("_class")
    @Schema(name = "_class", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "org.apache.syncope.common.lib.request.UserCR")
    @Override
    public String getDiscriminator() {
        return getClass().getName();
    }

    @JsonProperty(required = true)
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

    public boolean isStorePassword() {
        return storePassword;
    }

    public void setStorePassword(final boolean storePassword) {
        this.storePassword = storePassword;
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

    @JsonIgnore
    @Override
    public Optional<RelationshipTO> getRelationship(final String type, final String otherKey) {
        return relationships.stream().filter(
                relationship -> type.equals(relationship.getType()) && otherKey.equals(relationship.getOtherEndKey())).
                findFirst();
    }

    @JacksonXmlElementWrapper(localName = "relationships")
    @JacksonXmlProperty(localName = "relationship")
    @Override
    public List<RelationshipTO> getRelationships() {
        return relationships;
    }

    @JsonIgnore
    @Override
    public Optional<MembershipTO> getMembership(final String groupKey) {
        return memberships.stream().filter(membership -> groupKey.equals(membership.getGroupKey())).findFirst();
    }

    @JacksonXmlElementWrapper(localName = "memberships")
    @JacksonXmlProperty(localName = "membership")
    @Override
    public List<MembershipTO> getMemberships() {
        return memberships;
    }

    @JsonIgnore
    @Override
    public List<MembershipTO> getDynMemberships() {
        return List.of();
    }

    @JacksonXmlElementWrapper(localName = "roles")
    @JacksonXmlProperty(localName = "role")
    public Set<String> getRoles() {
        return roles;
    }

    @JacksonXmlElementWrapper(localName = "linkedAccounts")
    @JacksonXmlProperty(localName = "linkedAccount")
    public List<LinkedAccountTO> getLinkedAccounts() {
        return linkedAccounts;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(username).
                append(roles).
                append(securityQuestion).
                append(securityAnswer).
                append(mustChangePassword).
                append(relationships).
                append(memberships).
                append(linkedAccounts).
                build();
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
        final UserCR other = (UserCR) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(username, other.username).
                append(roles, other.roles).
                append(securityQuestion, other.securityQuestion).
                append(securityAnswer, other.securityAnswer).
                append(mustChangePassword, other.mustChangePassword).
                append(relationships, other.relationships).
                append(memberships, other.memberships).
                append(linkedAccounts, other.linkedAccounts).
                build();
    }
}

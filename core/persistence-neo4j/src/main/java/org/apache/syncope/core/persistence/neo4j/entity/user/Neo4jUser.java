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
package org.apache.syncope.core.persistence.neo4j.entity.user;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.SecurityQuestion;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.URelationship;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.common.validation.AttributableCheck;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractGroupableRelatable;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyTypeClass;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jExternalResource;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRole;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node(Neo4jUser.NODE)
@AttributableCheck
public class Neo4jUser
        extends AbstractGroupableRelatable<User, UMembership, AnyObject, URelationship>
        implements User {

    private static final long serialVersionUID = -3905046855521446823L;

    public static final String NODE = "SyncopeUser";

    public static final String USER_AUX_CLASSES_REL = "USER_AUX_CLASSES";

    public static final String USER_RESOURCE_REL = "USER_RESOURCE";

    public static final String ROLE_MEMBERSHIP_REL = "ROLE_MEMBERSHIP";

    public static final String USER_GROUP_MEMBERSHIP_REL = "USER_GROUP_MEMBERSHIP";

    public static final String USER_SECURITY_QUESTION_REL = "USER_SECURITY_QUESTION";

    protected static final TypeReference<List<String>> TYPEREF = new TypeReference<List<String>>() {
    };

    protected String password;

    @CompositeProperty(converterRef = "plainAttrsConverter")
    protected Map<String, PlainAttr> plainAttrs = new HashMap<>();

    protected String token;

    protected OffsetDateTime tokenExpireTime;

    protected CipherAlgorithm cipherAlgorithm;

    protected String passwordHistory;

    /**
     * Subsequent failed logins.
     */
    protected Integer failedLogins;

    /**
     * Username/Login.
     */
    @NotNull(message = "Blank username")
    protected String username;

    /**
     * Last successful login date.
     */
    protected OffsetDateTime lastLoginDate;

    /**
     * Change password date.
     */
    protected OffsetDateTime changePwdDate;

    protected Boolean suspended = false;

    protected Boolean mustChangePassword = false;

    /**
     * Provisioning external resources.
     */
    @Relationship(type = USER_RESOURCE_REL, direction = Relationship.Direction.OUTGOING)
    protected List<Neo4jExternalResource> resources = new ArrayList<>();

    @Relationship(type = USER_AUX_CLASSES_REL, direction = Relationship.Direction.OUTGOING)
    protected List<Neo4jAnyTypeClass> auxClasses = new ArrayList<>();

    @Relationship(type = Neo4jURelationship.SOURCE_REL, direction = Relationship.Direction.INCOMING)
    protected List<Neo4jURelationship> relationships = new ArrayList<>();

    @Relationship(type = USER_GROUP_MEMBERSHIP_REL, direction = Relationship.Direction.INCOMING)
    protected List<Neo4jUMembership> memberships = new ArrayList<>();

    @Relationship(type = ROLE_MEMBERSHIP_REL, direction = Relationship.Direction.OUTGOING)
    protected List<Neo4jRole> roles = new ArrayList<>();

    @Relationship(type = USER_SECURITY_QUESTION_REL, direction = Relationship.Direction.OUTGOING)
    protected Neo4jSecurityQuestion securityQuestion;

    protected String securityAnswer;

    @Relationship(direction = Relationship.Direction.INCOMING)
    @Valid
    protected List<Neo4jLinkedAccount> linkedAccounts = new ArrayList<>();

    @Override
    protected Map<String, PlainAttr> plainAttrs() {
        return plainAttrs;
    }

    @Override
    public AnyType getType() {
        return ApplicationContextProvider.getBeanFactory().getBean(AnyTypeDAO.class).getUser();
    }

    @Override
    public void setType(final AnyType type) {
        // nothing to do
    }

    @Override
    public boolean add(final ExternalResource resource) {
        checkType(resource, Neo4jExternalResource.class);
        return resources.contains((Neo4jExternalResource) resource) || resources.add((Neo4jExternalResource) resource);
    }

    @Override
    public List<? extends ExternalResource> getResources() {
        return resources;
    }

    @Override
    public boolean add(final Role role) {
        checkType(role, Neo4jRole.class);
        return roles.contains((Neo4jRole) role) || roles.add((Neo4jRole) role);
    }

    @Override
    public List<? extends Role> getRoles() {
        return roles;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setEncodedPassword(final String password, final CipherAlgorithm cipherAlgorithm) {
        this.password = password;
        this.cipherAlgorithm = cipherAlgorithm;
        setMustChangePassword(false);
    }

    protected String encode(final String value) throws Exception {
        return ApplicationContextProvider.getApplicationContext().getBean(EncryptorManager.class).getInstance().encode(
                value,
                Optional.ofNullable(cipherAlgorithm).
                        orElseGet(() -> CipherAlgorithm.valueOf(
                        ApplicationContextProvider.getBeanFactory().getBean(ConfParamOps.class).get(
                                AuthContextUtils.getDomain(),
                                "password.cipher.algorithm",
                                CipherAlgorithm.AES.name(),
                                String.class))));
    }

    @Override
    public void setPassword(final String password) {
        try {
            this.password = encode(password);
            setMustChangePassword(false);
        } catch (Exception e) {
            LOG.error("Could not encode password", e);
            this.password = null;
        }
    }

    @Override
    public CipherAlgorithm getCipherAlgorithm() {
        return cipherAlgorithm;
    }

    @Override
    public void setCipherAlgorithm(final CipherAlgorithm cipherAlgorithm) {
        if (this.cipherAlgorithm == null || cipherAlgorithm == null) {
            this.cipherAlgorithm = cipherAlgorithm;
        } else {
            throw new IllegalArgumentException("Cannot override existing cipher algorithm");
        }
    }

    @Override
    public boolean canDecodeSecrets() {
        return this.cipherAlgorithm != null && this.cipherAlgorithm.isInvertible();
    }

    @Override
    public boolean add(final PlainAttr attr) {
        if (attr.getMembership() == null) {
            return plainAttrs.put(attr.getSchema(), attr) != null;
        }

        return memberships().stream().
                filter(membership -> membership.getKey().equals(attr.getMembership())).findFirst().
                map(membership -> membership.add(attr)).
                orElse(false);
    }

    @Override
    public void generateToken(final int tokenLength, final int tokenExpireTime) {
        this.token = SecureRandomUtils.generateRandomPassword(tokenLength);
        this.tokenExpireTime = OffsetDateTime.now().plusMinutes(tokenExpireTime);
    }

    @Override
    public void removeToken() {
        this.token = null;
        this.tokenExpireTime = null;
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public OffsetDateTime getTokenExpireTime() {
        return tokenExpireTime;
    }

    @Override
    public boolean checkToken(final String token) {
        return Optional.ofNullable(this.token).
                map(s -> s.equals(token) && !hasTokenExpired()).
                orElseGet(() -> token == null);
    }

    @Override
    public boolean hasTokenExpired() {
        return Optional.ofNullable(tokenExpireTime).
                filter(expireTime -> expireTime.isBefore(OffsetDateTime.now())).
                isPresent();
    }

    @Override
    public void addToPasswordHistory(final String password) {
        List<String> ph = getPasswordHistory();
        ph.add(password);
        passwordHistory = POJOHelper.serialize(ph);
    }

    @Override
    public void removeOldestEntriesFromPasswordHistory(final int n) {
        List<String> ph = getPasswordHistory();
        ph.subList(n, ph.size());
        passwordHistory = POJOHelper.serialize(ph);
    }

    @Override
    public List<String> getPasswordHistory() {
        return passwordHistory == null
                ? new ArrayList<>(0)
                : POJOHelper.deserialize(passwordHistory, TYPEREF);
    }

    @Override
    public OffsetDateTime getChangePwdDate() {
        return changePwdDate;
    }

    @Override
    public void setChangePwdDate(final OffsetDateTime changePwdDate) {
        this.changePwdDate = changePwdDate;
    }

    @Override
    public Integer getFailedLogins() {
        return failedLogins == null ? 0 : failedLogins;
    }

    @Override
    public void setFailedLogins(final Integer failedLogins) {
        this.failedLogins = failedLogins;
    }

    @Override
    public OffsetDateTime getLastLoginDate() {
        return lastLoginDate;
    }

    @Override
    public void setLastLoginDate(final OffsetDateTime lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(final String username) {
        this.username = username;
    }

    @Override
    public void setSuspended(final Boolean suspended) {
        this.suspended = suspended;
    }

    @Override
    public Boolean isSuspended() {
        return suspended;
    }

    @Override
    public void setMustChangePassword(final boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    @Override
    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    @Override
    public SecurityQuestion getSecurityQuestion() {
        return securityQuestion;
    }

    @Override
    public void setSecurityQuestion(final SecurityQuestion securityQuestion) {
        checkType(securityQuestion, Neo4jSecurityQuestion.class);
        this.securityQuestion = (Neo4jSecurityQuestion) securityQuestion;
    }

    @Override
    public String getSecurityAnswer() {
        return securityAnswer;
    }

    @Override
    public void setSecurityAnswer(final String securityAnswer) {
        try {
            this.securityAnswer = encode(securityAnswer);
        } catch (Exception e) {
            LOG.error("Could not encode security answer", e);
            this.securityAnswer = null;
        }
    }

    @Override
    public boolean add(final AnyTypeClass auxClass) {
        checkType(auxClass, Neo4jAnyTypeClass.class);
        return auxClasses.contains((Neo4jAnyTypeClass) auxClass) || auxClasses.add((Neo4jAnyTypeClass) auxClass);
    }

    @Override
    public List<? extends AnyTypeClass> getAuxClasses() {
        return auxClasses;
    }

    @Override
    public boolean add(final URelationship relationship) {
        checkType(relationship, Neo4jURelationship.class);
        return this.relationships.add((Neo4jURelationship) relationship);
    }

    @Override
    public List<? extends URelationship> getRelationships() {
        return relationships;
    }

    @Override
    public boolean add(final UMembership membership) {
        checkType(membership, Neo4jUMembership.class);
        return this.memberships.add((Neo4jUMembership) membership);
    }

    @Override
    public boolean remove(final UMembership membership) {
        checkType(membership, Neo4jUMembership.class);
        return this.memberships.remove((Neo4jUMembership) membership);
    }

    @Override
    protected List<Neo4jUMembership> memberships() {
        return memberships;
    }

    @Override
    public boolean add(final LinkedAccount account) {
        checkType(account, Neo4jLinkedAccount.class);
        return linkedAccounts.contains((Neo4jLinkedAccount) account)
                || linkedAccounts.add((Neo4jLinkedAccount) account);
    }

    @Override
    public Optional<? extends LinkedAccount> getLinkedAccount(final String resource, final String connObjectKeyValue) {
        return linkedAccounts.stream().
                filter(account -> account.getResource().getKey().equals(resource)
                && account.getConnObjectKeyValue().equals(connObjectKeyValue)).
                findFirst();
    }

    @Override
    public List<? extends LinkedAccount> getLinkedAccounts(final String resource) {
        return linkedAccounts.stream().
                filter(account -> account.getResource().getKey().equals(resource)).
                toList();
    }

    @Override
    public List<? extends LinkedAccount> getLinkedAccounts() {
        return linkedAccounts;
    }
}

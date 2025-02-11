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
package org.apache.syncope.core.persistence.jpa.entity.user;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
import org.apache.syncope.core.persistence.jpa.entity.AbstractGroupableRelatable;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyTypeClass;
import org.apache.syncope.core.persistence.jpa.entity.JPAExternalResource;
import org.apache.syncope.core.persistence.jpa.entity.JPARole;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.SecureRandomUtils;

@Entity
@Table(name = JPAUser.TABLE)
@EntityListeners({ JSONUserListener.class })
@Cacheable
public class JPAUser
        extends AbstractGroupableRelatable<User, UMembership, AnyObject, URelationship>
        implements User {

    private static final long serialVersionUID = -3905046855521446823L;

    public static final String TABLE = "SyncopeUser";

    protected static final TypeReference<List<String>> TYPEREF = new TypeReference<List<String>>() {
    };

    @Column(nullable = true)
    protected String password;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns =
            @JoinColumn(name = "user_id"),
            inverseJoinColumns =
            @JoinColumn(name = "role_id"),
            uniqueConstraints =
            @UniqueConstraint(columnNames = { "user_id", "role_id" }))
    protected List<JPARole> roles = new ArrayList<>();

    private String plainAttrs;

    @Transient
    private final List<PlainAttr> plainAttrsList = new ArrayList<>();

    @Lob
    protected String token;

    protected OffsetDateTime tokenExpireTime;

    @Column(nullable = true)
    @Enumerated(EnumType.STRING)
    protected CipherAlgorithm cipherAlgorithm;

    @Lob
    protected String passwordHistory;

    /**
     * Subsequent failed logins.
     */
    @Column(nullable = true)
    protected Integer failedLogins;

    /**
     * Username/Login.
     */
    @Column(unique = true)
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
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns =
            @JoinColumn(name = "user_id"),
            inverseJoinColumns =
            @JoinColumn(name = "resource_id"),
            uniqueConstraints =
            @UniqueConstraint(columnNames = { "user_id", "resource_id" }))
    protected List<JPAExternalResource> resources = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(joinColumns =
            @JoinColumn(name = "user_id"),
            inverseJoinColumns =
            @JoinColumn(name = "anyTypeClass_id"),
            uniqueConstraints =
            @UniqueConstraint(columnNames = { "user_id", "anyTypeClass_id" }))
    protected List<JPAAnyTypeClass> auxClasses = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "leftEnd")
    @Valid
    protected List<JPAURelationship> relationships = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "leftEnd")
    @Valid
    protected List<JPAUMembership> memberships = new ArrayList<>();

    @ManyToOne(fetch = FetchType.EAGER)
    protected JPASecurityQuestion securityQuestion;

    @Column(nullable = true)
    protected String securityAnswer;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "owner")
    @Valid
    protected List<JPALinkedAccount> linkedAccounts = new ArrayList<>();

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
        checkType(resource, JPAExternalResource.class);
        return resources.contains((JPAExternalResource) resource) || resources.add((JPAExternalResource) resource);
    }

    @Override
    public List<? extends ExternalResource> getResources() {
        return resources;
    }

    @Override
    public boolean add(final Role role) {
        checkType(role, JPARole.class);
        return roles.contains((JPARole) role) || roles.add((JPARole) role);
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
    public List<PlainAttr> getPlainAttrsList() {
        return plainAttrsList;
    }

    @Override
    public String getPlainAttrsJSON() {
        return plainAttrs;
    }

    @Override
    public void setPlainAttrsJSON(final String plainAttrs) {
        this.plainAttrs = plainAttrs;
    }

    @Override
    public boolean add(final PlainAttr attr) {
        return plainAttrsList.add(attr);
    }

    @Override
    public boolean remove(final PlainAttr attr) {
        return plainAttrsList.removeIf(a -> a.getSchema().equals(attr.getSchema())
                && Objects.equals(a.getMembership(), attr.getMembership()));
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
        checkType(securityQuestion, JPASecurityQuestion.class);
        this.securityQuestion = (JPASecurityQuestion) securityQuestion;
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
        checkType(auxClass, JPAAnyTypeClass.class);
        return auxClasses.contains((JPAAnyTypeClass) auxClass) || auxClasses.add((JPAAnyTypeClass) auxClass);
    }

    @Override
    public List<? extends AnyTypeClass> getAuxClasses() {
        return auxClasses;
    }

    @Override
    public boolean add(final URelationship relationship) {
        checkType(relationship, JPAURelationship.class);
        return this.relationships.add((JPAURelationship) relationship);
    }

    @Override
    public List<? extends URelationship> getRelationships() {
        return relationships;
    }

    @Override
    public boolean add(final UMembership membership) {
        checkType(membership, JPAUMembership.class);
        return this.memberships.add((JPAUMembership) membership);
    }

    @Override
    public boolean remove(final UMembership membership) {
        checkType(membership, JPAUMembership.class);
        plainAttrsList.removeIf(attr -> Objects.equals(attr.getMembership(), membership.getKey()));
        return this.memberships.remove((JPAUMembership) membership);
    }

    @Override
    public List<? extends UMembership> getMemberships() {
        return memberships;
    }

    @Override
    public boolean add(final LinkedAccount account) {
        checkType(account, JPALinkedAccount.class);
        return linkedAccounts.contains((JPALinkedAccount) account) || linkedAccounts.add((JPALinkedAccount) account);
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

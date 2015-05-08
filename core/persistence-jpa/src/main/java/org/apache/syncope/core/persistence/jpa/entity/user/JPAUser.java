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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.entity.membership.Membership;
import org.apache.syncope.core.persistence.api.entity.user.SecurityQuestion;
import org.apache.syncope.core.persistence.api.entity.user.UDerAttr;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.UVirAttr;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.validation.entity.UserCheck;
import org.apache.syncope.core.persistence.jpa.entity.AbstractSubject;
import org.apache.syncope.core.persistence.jpa.entity.JPAExternalResource;
import org.apache.syncope.core.persistence.jpa.entity.JPASecurityQuestion;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMembership;
import org.apache.syncope.core.misc.security.Encryptor;
import org.apache.syncope.core.misc.security.SecureRandomUtils;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.jpa.entity.JPARole;

@Entity
@Table(name = JPAUser.TABLE)
@Cacheable
@UserCheck
public class JPAUser extends AbstractSubject<UPlainAttr, UDerAttr, UVirAttr> implements User {

    private static final long serialVersionUID = -3905046855521446823L;

    public static final String TABLE = "SyncopeUser";

    @Id
    private Long id;

    @Column(nullable = true)
    private String password;

    @Transient
    private String clearPassword;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns =
            @JoinColumn(name = "user_id"),
            inverseJoinColumns =
            @JoinColumn(name = "role_id"))
    private List<JPARole> roles;

    @OneToMany(cascade = CascadeType.MERGE, mappedBy = "user")
    @Valid
    private List<JPAMembership> memberships;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<JPAUPlainAttr> plainAttrs;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<JPAUDerAttr> derAttrs;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<JPAUVirAttr> virAttrs;

    private String workflowId;

    @Column(nullable = true)
    private String status;

    @Lob
    private String token;

    @Temporal(TemporalType.TIMESTAMP)
    private Date tokenExpireTime;

    @Column(nullable = true)
    @Enumerated(EnumType.STRING)
    private CipherAlgorithm cipherAlgorithm;

    @ElementCollection
    @Column(name = "passwordHistoryValue")
    @CollectionTable(name = "SyncopeUser_passwordHistory", joinColumns =
            @JoinColumn(name = "SyncopeUser_id", referencedColumnName = "id"))
    private List<String> passwordHistory;

    /**
     * Subsequent failed logins.
     */
    @Column(nullable = true)
    private Integer failedLogins;

    /**
     * Username/Login.
     */
    @Column(unique = true)
    @NotNull(message = "Blank username")
    private String username;

    /**
     * Last successful login date.
     */
    @Column(nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastLoginDate;

    /**
     * Change password date.
     */
    @Column(nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    private Date changePwdDate;

    @Basic
    @Min(0)
    @Max(1)
    private Integer suspended;

    /**
     * Provisioning external resources.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns =
            @JoinColumn(name = "user_id"),
            inverseJoinColumns =
            @JoinColumn(name = "resource_name"))
    @Valid
    private Set<JPAExternalResource> resources;

    @ManyToOne(fetch = FetchType.EAGER)
    private JPASecurityQuestion securityQuestion;

    @Column(nullable = true)
    private String securityAnswer;

    public JPAUser() {
        super();

        roles = new ArrayList<>();
        memberships = new ArrayList<>();
        plainAttrs = new ArrayList<>();
        derAttrs = new ArrayList<>();
        virAttrs = new ArrayList<>();
        passwordHistory = new ArrayList<>();
        failedLogins = 0;
        suspended = getBooleanAsInteger(Boolean.FALSE);
        resources = new HashSet<>();
    }

    @Override
    public Long getKey() {
        return id;
    }

    @Override
    protected Set<JPAExternalResource> internalGetResources() {
        return resources;
    }

    @Override
    public boolean addRole(final Role role) {
        checkType(role, JPARole.class);
        return roles.contains((JPARole) role) || roles.add((JPARole) role);
    }

    @Override
    public boolean removeRole(final Role role) {
        checkType(role, JPARole.class);
        return roles.remove((JPARole) role);
    }

    @Override
    public List<? extends Role> getRoles() {
        return roles;
    }

    @Override
    public boolean addMembership(final Membership membership) {
        checkType(membership, JPAMembership.class);
        return memberships.contains((JPAMembership) membership) || memberships.add((JPAMembership) membership);
    }

    @Override
    public boolean removeMembership(final Membership membership) {
        checkType(membership, JPAMembership.class);
        return memberships.remove((JPAMembership) membership);
    }

    @Override
    public Membership getMembership(final Long groupKey) {
        return CollectionUtils.find(getMemberships(), new Predicate<Membership>() {

            @Override
            public boolean evaluate(final Membership membership) {
                return membership.getGroup() != null && groupKey.equals(membership.getGroup().getKey());
            }
        });
    }

    @Override
    public List<? extends Membership> getMemberships() {
        return memberships;
    }

    @Override
    public Collection<Long> getStaticGroupKeys() {
        return CollectionUtils.collect(memberships, new Transformer<Membership, Long>() {

            @Override
            public Long transform(final Membership membership) {
                return membership.getGroup().getKey();
            }
        }, new HashSet<Long>());
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getClearPassword() {
        return clearPassword;
    }

    @Override
    public void removeClearPassword() {
        clearPassword = null;
    }

    @Override
    public void setEncodedPassword(final String password, final CipherAlgorithm cipherAlgoritm) {
        this.clearPassword = null;

        this.password = password;
        this.cipherAlgorithm = cipherAlgoritm;
    }

    @Override
    public void setPassword(final String password, final CipherAlgorithm cipherAlgoritm) {
        this.clearPassword = password;

        try {
            this.password = Encryptor.getInstance().encode(password, cipherAlgoritm);
            this.cipherAlgorithm = cipherAlgoritm;
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
    public boolean canDecodePassword() {
        return this.cipherAlgorithm != null && this.cipherAlgorithm.isInvertible();
    }

    @Override
    public boolean addPlainAttr(final UPlainAttr attr) {
        checkType(attr, JPAUPlainAttr.class);
        return plainAttrs.add((JPAUPlainAttr) attr);
    }

    @Override
    public boolean removePlainAttr(final UPlainAttr attr) {
        checkType(attr, JPAUPlainAttr.class);
        return plainAttrs.remove((JPAUPlainAttr) attr);
    }

    @Override
    public List<? extends UPlainAttr> getPlainAttrs() {
        return plainAttrs;
    }

    @Override
    public boolean addDerAttr(final UDerAttr attr) {
        checkType(attr, JPAUDerAttr.class);
        return derAttrs.add((JPAUDerAttr) attr);
    }

    @Override
    public boolean removeDerAttr(final UDerAttr attr) {
        checkType(attr, JPAUDerAttr.class);
        return derAttrs.remove((JPAUDerAttr) attr);
    }

    @Override
    public List<? extends UDerAttr> getDerAttrs() {
        return derAttrs;
    }

    @Override
    public boolean addVirAttr(final UVirAttr attr) {
        checkType(attr, JPAUVirAttr.class);
        return virAttrs.add((JPAUVirAttr) attr);
    }

    @Override
    public boolean removeVirAttr(final UVirAttr attr) {
        checkType(attr, JPAUVirAttr.class);
        return virAttrs.remove((JPAUVirAttr) attr);
    }

    @Override
    public List<? extends UVirAttr> getVirAttrs() {
        return virAttrs;
    }

    @Override
    public String getWorkflowId() {
        return workflowId;
    }

    @Override
    public void setWorkflowId(final String workflowId) {
        this.workflowId = workflowId;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public void setStatus(final String status) {
        this.status = status;
    }

    @Override
    public void generateToken(final int tokenLength, final int tokenExpireTime) {
        this.token = SecureRandomUtils.generateRandomPassword(tokenLength);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, tokenExpireTime);
        this.tokenExpireTime = calendar.getTime();
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
    public Date getTokenExpireTime() {
        return tokenExpireTime == null
                ? null
                : new Date(tokenExpireTime.getTime());
    }

    @Override
    public boolean checkToken(final String token) {
        return this.token == null
                ? token == null
                : this.token.equals(token) && !hasTokenExpired();
    }

    @Override
    public boolean hasTokenExpired() {
        return tokenExpireTime == null
                ? false
                : tokenExpireTime.before(new Date());
    }

    @Override
    public void setCipherAlgorithm(final CipherAlgorithm cipherAlgorithm) {
        this.cipherAlgorithm = cipherAlgorithm;
    }

    @Override
    public List<String> getPasswordHistory() {
        return passwordHistory;
    }

    @Override
    public Date getChangePwdDate() {
        return changePwdDate == null
                ? null
                : new Date(changePwdDate.getTime());
    }

    @Override
    public void setChangePwdDate(final Date changePwdDate) {
        this.changePwdDate = changePwdDate == null
                ? null
                : new Date(changePwdDate.getTime());
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
    public Date getLastLoginDate() {
        return lastLoginDate == null
                ? null
                : new Date(lastLoginDate.getTime());
    }

    @Override
    public void setLastLoginDate(final Date lastLoginDate) {
        this.lastLoginDate = lastLoginDate == null
                ? null
                : new Date(lastLoginDate.getTime());
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
        this.suspended = getBooleanAsInteger(suspended);
    }

    @Override
    public Boolean isSuspended() {
        return suspended == null ? null : isBooleanAsInteger(suspended);
    }

    @Override
    public boolean verifyPasswordHistory(final String password, final int size) {
        boolean res = false;

        if (size > 0) {
            try {
                res = passwordHistory.subList(size >= passwordHistory.size()
                        ? 0
                        : passwordHistory.size() - size, passwordHistory.size()).contains(cipherAlgorithm == null
                                        ? password
                                        : Encryptor.getInstance().encode(password, cipherAlgorithm));
            } catch (Exception e) {
                LOG.error("Error evaluating password history", e);
            }
        }

        return res;
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
        this.securityAnswer = securityAnswer;
    }

}

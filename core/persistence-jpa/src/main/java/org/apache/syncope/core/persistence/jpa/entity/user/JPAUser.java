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
import java.util.Date;
import java.util.List;
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
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.entity.user.SecurityQuestion;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAExternalResource;
import org.apache.syncope.core.persistence.jpa.entity.JPASecurityQuestion;
import org.apache.syncope.core.spring.security.Encryptor;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.URelationship;
import org.apache.syncope.core.persistence.jpa.entity.AbstractGroupableRelatable;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyTypeClass;
import org.apache.syncope.core.persistence.jpa.entity.JPARole;

@Entity
@Table(name = JPAUser.TABLE)
@Cacheable
public class JPAUser
        extends AbstractGroupableRelatable<User, UMembership, UPlainAttr, AnyObject, URelationship>
        implements User {

    private static final long serialVersionUID = -3905046855521446823L;

    public static final String TABLE = "SyncopeUser";

    private static final Encryptor ENCRYPTOR = Encryptor.getInstance();

    @Column(nullable = true)
    private String password;

    @Transient
    private String clearPassword;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns =
            @JoinColumn(name = "user_id"),
            inverseJoinColumns =
            @JoinColumn(name = "role_id"))
    private List<JPARole> roles = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<JPAUPlainAttr> plainAttrs = new ArrayList<>();

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
            @JoinColumn(name = "user_id", referencedColumnName = "id"))
    private List<String> passwordHistory = new ArrayList<>();

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

    @Column(nullable = true)
    private String lastRecertificator;

    @Column(nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastRecertification;

    @Basic
    @Min(0)
    @Max(1)
    private Integer suspended = getBooleanAsInteger(Boolean.FALSE);

    @Basic
    @Min(0)
    @Max(1)
    private Integer mustChangePassword = getBooleanAsInteger(Boolean.FALSE);

    /**
     * Provisioning external resources.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns =
            @JoinColumn(name = "user_id"),
            inverseJoinColumns =
            @JoinColumn(name = "resource_id"))
    private List<JPAExternalResource> resources = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(joinColumns =
            @JoinColumn(name = "user_id"),
            inverseJoinColumns =
            @JoinColumn(name = "anyTypeClass_id"))
    private List<JPAAnyTypeClass> auxClasses = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "leftEnd")
    @Valid
    private List<JPAURelationship> relationships = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "leftEnd")
    @Valid
    private List<JPAUMembership> memberships = new ArrayList<>();

    @ManyToOne(fetch = FetchType.EAGER)
    private JPASecurityQuestion securityQuestion;

    @Column(nullable = true)
    private String securityAnswer;

    @Override
    public AnyType getType() {
        return ApplicationContextProvider.getBeanFactory().getBean(AnyTypeDAO.class).findUser();
    }

    @Override
    public void setType(final AnyType type) {
        // nothing to do
    }

    @Override
    public boolean add(final ExternalResource resource) {
        checkType(resource, JPAExternalResource.class);
        return resources.add((JPAExternalResource) resource);
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
    public String getClearPassword() {
        return clearPassword;
    }

    public void setClearPassword(final String clearPassword) {
        this.clearPassword = clearPassword;
    }

    @Override
    public void removeClearPassword() {
        setClearPassword(null);
    }

    @Override
    public void setEncodedPassword(final String password, final CipherAlgorithm cipherAlgoritm) {
        this.clearPassword = null;

        this.password = password;
        this.cipherAlgorithm = cipherAlgoritm;
        setMustChangePassword(false);
    }

    @Override
    public void setPassword(final String password, final CipherAlgorithm cipherAlgoritm) {
        this.clearPassword = password;

        try {
            this.password = ENCRYPTOR.encode(password, cipherAlgoritm);
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
    public boolean add(final UPlainAttr attr) {
        checkType(attr, JPAUPlainAttr.class);
        return plainAttrs.add((JPAUPlainAttr) attr);
    }

    @Override
    protected List<? extends UPlainAttr> internalGetPlainAttrs() {
        return plainAttrs;
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
    public String getLastRecertificator() {
        return lastRecertificator;
    }

    @Override
    public void setLastRecertificator(final String lastRecertificator) {
        this.lastRecertificator = lastRecertificator;
    }

    @Override
    public Date getLastRecertification() {
        if (lastRecertification != null) {
            return new Date(lastRecertification.getTime());
        }
        return null;
    }

    @Override
    public void setLastRecertification(final Date lastRecertification) {
        if (lastRecertification != null) {
            this.lastRecertification = new Date(lastRecertification.getTime());
        } else {
            this.lastRecertification = null;
        }
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
    public void setMustChangePassword(final boolean mustChangePassword) {
        this.mustChangePassword = getBooleanAsInteger(mustChangePassword);
    }

    @Override
    public boolean isMustChangePassword() {
        return isBooleanAsInteger(mustChangePassword);
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
                        : ENCRYPTOR.encode(password, cipherAlgorithm));
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

    @Override
    public boolean add(final AnyTypeClass auxClass) {
        checkType(auxClass, JPAAnyTypeClass.class);
        return this.auxClasses.add((JPAAnyTypeClass) auxClass);
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
    public List<? extends UMembership> getMemberships() {
        return memberships;
    }

}

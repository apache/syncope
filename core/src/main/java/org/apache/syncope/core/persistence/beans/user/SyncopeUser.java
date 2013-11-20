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
package org.apache.syncope.core.persistence.beans.user;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
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
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.apache.syncope.common.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.beans.AbstractAttr;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractDerAttr;
import org.apache.syncope.core.persistence.beans.AbstractVirAttr;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.membership.Membership;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.validation.entity.SyncopeUserCheck;
import org.apache.syncope.core.util.PasswordEncoder;
import org.apache.syncope.core.util.SecureRandomUtil;

/**
 * Syncope user bean.
 */
@Entity
@Cacheable
@SyncopeUserCheck
public class SyncopeUser extends AbstractAttributable {

    private static final long serialVersionUID = -3905046855521446823L;

    @Id
    private Long id;

    @NotNull(message = "Blank password")
    private String password;

    @Transient
    private String clearPassword;

    @OneToMany(cascade = CascadeType.MERGE, mappedBy = "syncopeUser")
    @Valid
    private List<Membership> memberships;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<UAttr> attrs;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<UDerAttr> derAttrs;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<UVirAttr> virAttrs;

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
    private Set<ExternalResource> resources;

    public SyncopeUser() {
        super();

        memberships = new ArrayList<Membership>();
        attrs = new ArrayList<UAttr>();
        derAttrs = new ArrayList<UDerAttr>();
        virAttrs = new ArrayList<UVirAttr>();
        passwordHistory = new ArrayList<String>();
        failedLogins = 0;
        suspended = getBooleanAsInteger(Boolean.FALSE);
        resources = new HashSet<ExternalResource>();
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    protected Set<ExternalResource> internalGetResources() {
        return resources;
    }

    public boolean addMembership(final Membership membership) {
        return memberships.contains(membership) || memberships.add(membership);
    }

    public boolean removeMembership(final Membership membership) {
        return memberships.remove(membership);
    }

    public Membership getMembership(final Long syncopeRoleId) {
        Membership result = null;
        Membership membership;
        for (Iterator<Membership> itor = getMemberships().iterator(); result == null && itor.hasNext();) {

            membership = itor.next();
            if (membership.getSyncopeRole() != null && syncopeRoleId.equals(membership.getSyncopeRole().getId())) {

                result = membership;
            }
        }
        return result;
    }

    public List<Membership> getMemberships() {
        return memberships;
    }

    public void setMemberships(final List<Membership> memberships) {
        this.memberships.clear();
        if (memberships != null && !memberships.isEmpty()) {
            this.memberships.addAll(memberships);
        }
    }

    public List<SyncopeRole> getRoles() {
        List<SyncopeRole> result = new ArrayList<SyncopeRole>();

        for (Membership membership : memberships) {
            if (membership.getSyncopeRole() != null) {
                result.add(membership.getSyncopeRole());
            }
        }

        return result;
    }

    public Set<Long> getRoleIds() {
        List<SyncopeRole> roles = getRoles();

        Set<Long> result = new HashSet<Long>(roles.size());
        for (SyncopeRole role : roles) {
            result.add(role.getId());
        }

        return result;
    }

    @Override
    public Set<ExternalResource> getResources() {
        Set<ExternalResource> result = new HashSet<ExternalResource>();
        result.addAll(super.getResources());
        for (SyncopeRole role : getRoles()) {
            result.addAll(role.getResources());
        }
        return result;
    }

    public Set<ExternalResource> getOwnResources() {
        return super.getResources();
    }

    public String getPassword() {
        return password;
    }

    public String getClearPassword() {
        return clearPassword;
    }

    public void removeClearPassword() {
        clearPassword = null;
    }

    public void setPassword(final String password, final CipherAlgorithm cipherAlgoritm, final int historySize) {
        // clear password
        this.clearPassword = password;

        try {
            this.password = PasswordEncoder.encode(password, cipherAlgoritm);
            this.cipherAlgorithm = cipherAlgoritm;
        } catch (Exception e) {
            LOG.error("Could not encode password", e);
            this.password = null;
        }
    }

    public CipherAlgorithm getCipherAlgorithm() {
        return cipherAlgorithm;
    }

    public boolean canDecodePassword() {
        return this.cipherAlgorithm != null && this.cipherAlgorithm.isInvertible();
    }

    @Override
    public <T extends AbstractAttr> boolean addAttr(final T attr) {
        if (!(attr instanceof UAttr)) {
            throw new ClassCastException("attribute is expected to be typed UAttr: " + attr.getClass().getName());
        }

        return attrs.add((UAttr) attr);
    }

    @Override
    public <T extends AbstractAttr> boolean removeAttr(final T attr) {
        if (!(attr instanceof UAttr)) {
            throw new ClassCastException("attribute is expected to be typed UAttr: " + attr.getClass().getName());
        }

        return attrs.remove((UAttr) attr);
    }

    @Override
    public List<? extends AbstractAttr> getAttrs() {
        return attrs;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setAttrs(final List<? extends AbstractAttr> attrs) {
        this.attrs.clear();
        if (attrs != null && !attrs.isEmpty()) {
            this.attrs.addAll((List<UAttr>) attrs);
        }
    }

    @Override
    public <T extends AbstractDerAttr> boolean addDerAttr(final T derAttr) {
        if (!(derAttr instanceof UDerAttr)) {
            throw new ClassCastException("attribute is expected to be typed UDerAttr: " + derAttr.getClass().getName());
        }

        return derAttrs.add((UDerAttr) derAttr);
    }

    @Override
    public <T extends AbstractDerAttr> boolean removeDerAttr(final T derAttr) {
        if (!(derAttr instanceof UDerAttr)) {
            throw new ClassCastException("attribute is expected to be typed UDerAttr: " + derAttr.getClass().getName());
        }
        return derAttrs.remove((UDerAttr) derAttr);
    }

    @Override
    public List<? extends AbstractDerAttr> getDerAttrs() {
        return derAttrs;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setDerAttrs(final List<? extends AbstractDerAttr> derAttr) {
        this.derAttrs.clear();
        if (derAttr != null && !derAttr.isEmpty()) {
            this.derAttrs.addAll((List<UDerAttr>) derAttr);
        }
    }

    @Override
    public <T extends AbstractVirAttr> boolean addVirAttr(final T virAttr) {
        if (!(virAttr instanceof UVirAttr)) {
            throw new ClassCastException("attribute is expected to be typed UVirAttr: " + virAttr.getClass().getName());
        }
        return virAttrs.add((UVirAttr) virAttr);
    }

    @Override
    public <T extends AbstractVirAttr> boolean removeVirAttr(final T virAttr) {
        if (!(virAttr instanceof UVirAttr)) {
            throw new ClassCastException("attribute is expected to be typed UVirAttr: " + virAttr.getClass().getName());
        }
        return virAttrs.remove((UVirAttr) virAttr);
    }

    @Override
    public List<? extends AbstractVirAttr> getVirAttrs() {
        return virAttrs;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setVirAttrs(final List<? extends AbstractVirAttr> virAttrs) {
        this.virAttrs.clear();
        if (virAttrs != null && !virAttrs.isEmpty()) {
            this.virAttrs.addAll((List<UVirAttr>) virAttrs);
        }
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(final String workflowId) {
        this.workflowId = workflowId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public void generateToken(final int tokenLength, final int tokenExpireTime) {
        this.token = SecureRandomUtil.generateRandomPassword(tokenLength);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, tokenExpireTime);
        this.tokenExpireTime = calendar.getTime();
    }

    public void removeToken() {
        this.token = null;
        this.tokenExpireTime = null;
    }

    public String getToken() {
        return token;
    }

    public Date getTokenExpireTime() {
        return tokenExpireTime == null
                ? null
                : new Date(tokenExpireTime.getTime());
    }

    public boolean checkToken(final String token) {
        return this.token == null || this.token.equals(token) && !hasTokenExpired();
    }

    public boolean hasTokenExpired() {
        return tokenExpireTime == null
                ? false
                : tokenExpireTime.before(new Date());
    }

    public void setCipherAlgorithm(final CipherAlgorithm cipherAlgorithm) {
        this.cipherAlgorithm = cipherAlgorithm;
    }

    public List<String> getPasswordHistory() {
        return passwordHistory;
    }

    public Date getChangePwdDate() {
        return changePwdDate == null
                ? null
                : new Date(changePwdDate.getTime());
    }

    public void setChangePwdDate(final Date changePwdDate) {
        this.changePwdDate = changePwdDate == null
                ? null
                : new Date(changePwdDate.getTime());
    }

    public Integer getFailedLogins() {
        return failedLogins == null ? 0 : failedLogins;
    }

    public void setFailedLogins(final Integer failedLogins) {
        this.failedLogins = failedLogins;
    }

    public Date getLastLoginDate() {
        return lastLoginDate == null
                ? null
                : new Date(lastLoginDate.getTime());
    }

    public void setLastLoginDate(final Date lastLoginDate) {
        this.lastLoginDate = lastLoginDate == null
                ? null
                : new Date(lastLoginDate.getTime());
    }

    public String getUsername() {
        return username;
    }

    int PASSWORD_LENGTH = 8;

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setSuspended(final Boolean suspended) {
        this.suspended = getBooleanAsInteger(suspended);
    }

    public Boolean isSuspended() {
        return suspended == null ? null : isBooleanAsInteger(suspended);
    }

    public boolean verifyPasswordHistory(final String password, final int size) {
        boolean res = false;

        if (size > 0) {
            try {
                res = passwordHistory.subList(size >= passwordHistory.size()
                        ? 0
                        : passwordHistory.size() - size, passwordHistory.size()).contains(cipherAlgorithm == null
                        ? password
                        : PasswordEncoder.encode(password, cipherAlgorithm));
            } catch (Exception e) {
                LOG.error("Error evaluating password history", e);
            }
        }

        return res;
    }
}

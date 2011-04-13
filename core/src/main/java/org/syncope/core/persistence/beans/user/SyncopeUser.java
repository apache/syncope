/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence.beans.user;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.Valid;
import org.apache.commons.lang.RandomStringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.syncope.core.persistence.beans.AbstractAttributable;
import org.syncope.core.persistence.beans.AbstractAttr;
import org.syncope.core.persistence.beans.AbstractDerAttr;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.membership.Membership;
import org.syncope.core.persistence.beans.role.SyncopeRole;

@Entity
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
public class SyncopeUser extends AbstractAttributable {

    @Id
    private Long id;

    private String password;

    @OneToMany(cascade = CascadeType.MERGE, mappedBy = "syncopeUser")
    @Valid
    private List<Membership> memberships;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<UAttr> attributes;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    @Valid
    private List<UDerAttr> derivedAttributes;

    @Column(nullable = true)
    private Long workflowId;

    @Lob
    private String token;

    @Temporal(TemporalType.TIMESTAMP)
    private Date tokenExpireTime;

    public SyncopeUser() {
        memberships = new ArrayList<Membership>();
        attributes = new ArrayList<UAttr>();
        derivedAttributes = new ArrayList<UDerAttr>();
    }

    @Override
    public Long getId() {
        return id;
    }

    public boolean addMembership(Membership membership) {
        return memberships.contains(membership) || memberships.add(membership);
    }

    public boolean removeMembership(Membership membership) {
        return memberships == null || memberships.remove(membership);
    }

    public Membership getMembership(Long syncopeRoleId) {
        Membership result = null;
        Membership membership = null;
        for (Iterator<Membership> itor =
                getMemberships().iterator();
                result == null && itor.hasNext();) {

            membership = itor.next();
            if (membership.getSyncopeRole() != null && syncopeRoleId.equals(
                    membership.getSyncopeRole().getId())) {

                result = membership;
            }
        }

        return result;
    }

    public List<Membership> getMemberships() {
        return memberships;
    }

    public void setMemberships(List<Membership> memberships) {
        this.memberships.clear();
        if (memberships != null && !memberships.isEmpty()) {
            this.memberships.addAll(memberships);
        }
    }

    public Set<SyncopeRole> getRoles() {
        Set<SyncopeRole> result = new HashSet<SyncopeRole>();

        for (Membership membership : memberships) {
            result.add(membership.getSyncopeRole());
        }

        return result;
    }

    @Override
    public Set<TargetResource> getInheritedTargetResources() {
        Set<TargetResource> inheritedTargetResources =
                new HashSet<TargetResource>();

        SyncopeRole role = null;

        for (Membership membership : memberships) {
            role = membership.getSyncopeRole();

            try {

                inheritedTargetResources.addAll(role.getTargetResources());

            } catch (Throwable t) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Invalid role " + role, t);
                }
            }
        }

        return inheritedTargetResources;
    }

    public String getPassword() {
        return password;
    }

    /**
     * TODO: password policies.
     * @param password the password to be set
     */
    public void setPassword(final String password) {
        if (password == null) {
            this.password = null;
        } else {
            try {
                MessageDigest algorithm = MessageDigest.getInstance("MD5");
                algorithm.reset();
                algorithm.update(password.getBytes());

                byte[] messageDigest = algorithm.digest();
                StringBuilder hexString = new StringBuilder();
                for (int i = 0; i < messageDigest.length; i++) {
                    hexString.append(
                            Integer.toHexString(0xFF & messageDigest[i]));
                }
                this.password = hexString.toString();
            } catch (NoSuchAlgorithmException e) {
                LOG.error("Could not find MD5 algorithm", e);
            }
        }
    }

    @Override
    public <T extends AbstractAttr> boolean addAttribute(T attribute) {
        return attributes.add((UAttr) attribute);
    }

    @Override
    public <T extends AbstractAttr> boolean removeAttribute(T attribute) {
        return attributes.remove((UAttr) attribute);
    }

    @Override
    public List<? extends AbstractAttr> getAttributes() {
        return attributes;
    }

    @Override
    public void setAttributes(List<? extends AbstractAttr> attributes) {
        this.attributes = (List<UAttr>) attributes;
    }

    @Override
    public <T extends AbstractDerAttr> boolean addDerivedAttribute(
            T derivedAttribute) {

        return derivedAttributes.add((UDerAttr) derivedAttribute);
    }

    @Override
    public <T extends AbstractDerAttr> boolean removeDerivedAttribute(
            T derivedAttribute) {

        return derivedAttributes.remove((UDerAttr) derivedAttribute);
    }

    @Override
    public List<? extends AbstractDerAttr> getDerivedAttributes() {
        return derivedAttributes;
    }

    @Override
    public void setDerivedAttributes(
            List<? extends AbstractDerAttr> derivedAttributes) {

        this.derivedAttributes = (List<UDerAttr>) derivedAttributes;
    }

    public Long getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(Long workflowEntryId) {
        this.workflowId = workflowEntryId;
    }

    public void generateToken(
            int tokenLength, int tokenExpireTime) {
        generateToken(tokenLength, tokenExpireTime, null);
    }

    public void generateToken(
            int tokenLength, int tokenExpireTime, String token) {

        if (token == null) {
            token = RandomStringUtils.randomAlphanumeric(tokenLength);
        }

        this.token = token;

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, tokenExpireTime);
        this.tokenExpireTime = calendar.getTime();
    }

    public void removeToken() {
        token = null;
        tokenExpireTime = null;
    }

    public String getToken() {
        return token;
    }

    public Date getTokenExpireTime() {
        return tokenExpireTime;
    }

    public boolean checkToken(final String token) {
        return this.token != null && this.token.equals(token)
                && tokenExpireTime.after(new Date());
    }
}

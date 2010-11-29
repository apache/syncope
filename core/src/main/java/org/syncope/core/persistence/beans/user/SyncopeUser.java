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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.QueryHint;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.apache.commons.lang.RandomStringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.syncope.core.persistence.beans.AbstractAttributable;
import org.syncope.core.persistence.beans.AbstractAttribute;
import org.syncope.core.persistence.beans.AbstractDerivedAttribute;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.membership.Membership;
import org.syncope.core.persistence.beans.role.SyncopeRole;

@Entity
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@NamedQueries({
    @NamedQuery(name = "SyncopeUser.find",
    query = "SELECT e FROM SyncopeUser e WHERE e.id = :id",
    hints = {
        @QueryHint(name = "org.hibernate.cacheable", value = "true")
    }),
    @NamedQuery(name = "SyncopeUser.findByWorkflowId",
    query = "SELECT e FROM SyncopeUser e WHERE e.workflowId = :workflowId",
    hints = {
        @QueryHint(name = "org.hibernate.cacheable", value = "true")
    })
})
public class SyncopeUser extends AbstractAttributable {

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE,
    generator = "SEQ_SyncopeUser")
    @TableGenerator(name = "SEQ_SyncopeUser", allocationSize = 100)
    private Long id;

    private String password;

    @OneToMany(cascade = CascadeType.MERGE, mappedBy = "syncopeUser")
    private List<Membership> memberships;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    private List<UserAttribute> attributes;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    private List<UserDerivedAttribute> derivedAttributes;

    @Column(nullable = true)
    private Long workflowId;

    @Lob
    private String token;

    @Temporal(TemporalType.TIMESTAMP)
    private Date tokenExpireTime;

    public SyncopeUser() {
        memberships = new ArrayList<Membership>();
        attributes = new ArrayList<UserAttribute>();
        derivedAttributes = new ArrayList<UserDerivedAttribute>();
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
     * @param password
     */
    public void setPassword(final String password) {
        if (password == null) {
            this.password = null;
        } else {
            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance("MD5");
                md.update(password.getBytes());
                this.password = new String(md.digest());
            } catch (NoSuchAlgorithmException e) {
                LOG.error("Could not find required digest ");
            }
        }
    }

    @Override
    public <T extends AbstractAttribute> boolean addAttribute(T attribute) {
        return attributes.add((UserAttribute) attribute);
    }

    @Override
    public <T extends AbstractAttribute> boolean removeAttribute(T attribute) {
        return attributes.remove((UserAttribute) attribute);
    }

    @Override
    public List<? extends AbstractAttribute> getAttributes() {
        return attributes;
    }

    @Override
    public void setAttributes(List<? extends AbstractAttribute> attributes) {
        this.attributes = (List<UserAttribute>) attributes;
    }

    @Override
    public <T extends AbstractDerivedAttribute> boolean addDerivedAttribute(
            T derivedAttribute) {

        return derivedAttributes.add((UserDerivedAttribute) derivedAttribute);
    }

    @Override
    public <T extends AbstractDerivedAttribute> boolean removeDerivedAttribute(
            T derivedAttribute) {

        return derivedAttributes.remove((UserDerivedAttribute) derivedAttribute);
    }

    @Override
    public List<? extends AbstractDerivedAttribute> getDerivedAttributes() {
        return derivedAttributes;
    }

    @Override
    public void setDerivedAttributes(
            List<? extends AbstractDerivedAttribute> derivedAttributes) {

        this.derivedAttributes = (List<UserDerivedAttribute>) derivedAttributes;
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

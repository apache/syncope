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

import java.security.KeyPair;
import java.security.Security;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.apache.commons.lang.RandomStringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.syncope.core.persistence.security.AsymmetricCipher;
import org.syncope.core.persistence.beans.AbstractAttributable;
import org.syncope.core.persistence.beans.AbstractAttribute;
import org.syncope.core.persistence.beans.AbstractDerivedAttribute;
import org.syncope.core.persistence.beans.membership.Membership;
import org.syncope.core.persistence.beans.role.SyncopeRole;

@Entity
public class SyncopeUser extends AbstractAttributable {

    static {
        BouncyCastleProvider securityProvider = new BouncyCastleProvider();
        if (Security.getProvider(securityProvider.getName()) == null) {
            Security.addProvider(securityProvider);
        }
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Basic
    @Lob
    private byte[] passwordKeyPair;
    @Basic
    @Lob
    private byte[] password;
    @OneToMany(cascade = CascadeType.MERGE, mappedBy = "syncopeUser")
    private List<Membership> memberships;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    private List<UserAttribute> attributes;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "owner")
    private List<UserDerivedAttribute> derivedAttributes;
    @Column(nullable = true)
    private Long workflowId;
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationTime;
    @Lob
    private String token;
    @Temporal(TemporalType.TIMESTAMP)
    private Date tokenExpireTime;

    public SyncopeUser() {
        memberships = new ArrayList<Membership>();
        attributes = new ArrayList<UserAttribute>();
        derivedAttributes = new ArrayList<UserDerivedAttribute>();
    }

    public Long getId() {
        return id;
    }

    public boolean addMembership(Membership membership) {
        return memberships.add(membership);
    }

    public boolean removeMembership(Membership membership) {
        return memberships.remove(membership);
    }

    public List<Membership> getMemberships() {
        return memberships;
    }

    public void setMemberships(List<Membership> memberships) {
        this.memberships = memberships;
    }

    public Set<SyncopeRole> getRoles() {
        Set<SyncopeRole> result = new HashSet<SyncopeRole>();

        for (Membership membership : memberships) {
            result.add(membership.getSyncopeRole());
        }

        return result;
    }

    public String getPassword() {
        if (password == null || passwordKeyPair == null) {
            return null;
        }

        String result = null;
        try {
            KeyPair kp = AsymmetricCipher.deserializeKeyPair(passwordKeyPair);
            result = new String(AsymmetricCipher.decrypt(password,
                    kp.getPrivate()));
        } catch (Throwable t) {
            log.error("Could not get the key pair and the password", t);
        }

        return result;
    }

    /**
     * TODO: password policies
     * @param password
     */
    public void setPassword(String password) {
        if (password == null) {
            this.password = null;
            this.passwordKeyPair = null;
            return;
        }

        try {
            KeyPair kp = AsymmetricCipher.generateKeyPair();
            this.password = AsymmetricCipher.encrypt(password.getBytes(),
                    kp.getPublic());
            this.passwordKeyPair = AsymmetricCipher.serializeKeyPair(kp);
        } catch (Throwable t) {
            log.error("Could not set the password and the key pair", t);
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

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public void generateToken(int tokenLength, int tokenExpireTime) {
        token = RandomStringUtils.randomAlphanumeric(tokenLength);

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

    public boolean checkToken(String token) {
        return this.token.equals(token) && tokenExpireTime.after(new Date());
    }
}

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
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.apache.commons.lang.RandomStringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.syncope.core.persistence.AsymmetricCipher;
import org.syncope.core.persistence.beans.AbstractAttributable;
import org.syncope.core.persistence.beans.AbstractAttribute;
import org.syncope.core.persistence.beans.AbstractDerivedAttribute;
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
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<SyncopeRole> roles;
    @OneToMany(cascade = CascadeType.ALL,
    fetch = FetchType.EAGER, mappedBy = "owner")
    private Set<UserAttribute> attributes;
    @OneToMany(cascade = CascadeType.ALL,
    fetch = FetchType.EAGER, mappedBy = "owner")
    private Set<UserDerivedAttribute> derivedAttributes;
    @Column(nullable = true)
    private Long workflowEntryId;
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationTime;
    @Lob
    private String token;
    @Temporal(TemporalType.TIMESTAMP)
    private Date tokenExpireTime;

    public SyncopeUser() {
        roles = new HashSet<SyncopeRole>();
        attributes = new HashSet<UserAttribute>();
        derivedAttributes = new HashSet<UserDerivedAttribute>();
    }

    public Long getId() {
        return id;
    }

    public boolean addRole(SyncopeRole role) {
        return roles.add(role);
    }

    public boolean removeRole(SyncopeRole role) {
        return roles.remove(role);
    }

    public Set<SyncopeRole> getRoles() {
        if (roles != null) {
            return roles;
        }
        return new HashSet<SyncopeRole>();
    }

    public void setRoles(Set<SyncopeRole> roles) {
        this.roles = roles;
    }

    public String getPassword() {
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
    public Set<? extends AbstractAttribute> getAttributes() {
        return attributes;
    }

    @Override
    public void setAttributes(Set<? extends AbstractAttribute> attributes) {
        this.attributes = (Set<UserAttribute>) attributes;
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
    public Set<? extends AbstractDerivedAttribute> getDerivedAttributes() {
        return derivedAttributes;
    }

    @Override
    public void setDerivedAttributes(
            Set<? extends AbstractDerivedAttribute> derivedAttributes) {

        this.derivedAttributes = (Set<UserDerivedAttribute>) derivedAttributes;
    }

    public Long getWorkflowEntryId() {
        return workflowEntryId;
    }

    public void setWorkflowEntryId(Long workflowEntryId) {
        this.workflowEntryId = workflowEntryId;
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

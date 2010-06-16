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
package org.syncope.core.persistence.beans;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;
import org.jasypt.util.password.PasswordEncryptor;
import org.jasypt.util.password.StrongPasswordEncryptor;

@Entity
public class SyncopeUser extends AbstractAttributableBean {

    @Transient
    final private static PasswordEncryptor passwordEncryptor =
            new StrongPasswordEncryptor();
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String password;
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<SyncopeRole> roles;

    public SyncopeUser() {
        attributes = new HashSet<Attribute>();
        derivedAttributes = new HashSet<DerivedAttribute>();
    }

    public Long getId() {
        return id;
    }

    public Set<SyncopeRole> getRoles() {
        return roles;
    }

    public void setRoles(Set<SyncopeRole> roles) {
        this.roles = roles;
    }

    public boolean checkPassword(String cleanPassword) {
        return passwordEncryptor.checkPassword(cleanPassword, password);
    }

    public String getPassword() {
        return password;
    }

    /**
     * TODO: password policies
     * @param password
     */
    public void setPassword(String password) {
        this.password = passwordEncryptor.encryptPassword(password);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        final SyncopeUser other = (SyncopeUser) obj;
        if (this.id != other.id
                && (this.id == null || !this.id.equals(other.id))) {

            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + (this.id != null ? this.id.hashCode() : 0);

        return hash;
    }
}

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
package org.syncope.client.to;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UserTO extends AbstractBaseTO {

    private long id;
    private String password;
    private Map<String, String> roles;
    private Map<String, Set<String>> attributes;
    private Map<String, String> derivedAttributes;
    private Date creationTime;
    private String token;
    private Date tokenExpireTime;

    public UserTO() {
        roles = new HashMap<String, String>();
        attributes = new HashMap<String, Set<String>>();
        derivedAttributes = new HashMap<String, String>();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String addRole(String role, String parent) {
        return roles.put(role, parent);
    }

    public String removeRole(String role) {
        return roles.remove(role);
    }

    public Map<String, String> getRoles() {
        return roles;
    }

    public void setRoles(Map<String, String> roles) {
        this.roles = roles;
    }

    public Set<String> addAttribute(String name, Set<String> values) {
        return attributes.put(name, values);
    }

    public Set<String> removeAttribute(String name) {
        return attributes.remove(name);
    }

    public Map<String, Set<String>> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Set<String>> attributes) {
        this.attributes = attributes;
    }

    public Map<String, String> getDerivedAttributes() {
        return derivedAttributes;
    }

    public void setDerivedAttributes(Map<String, String> derivedAttributes) {
        this.derivedAttributes = derivedAttributes;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Date getTokenExpireTime() {
        return tokenExpireTime;
    }

    public void setTokenExpireTime(Date tokenExpireTime) {
        this.tokenExpireTime = tokenExpireTime;
    }
}

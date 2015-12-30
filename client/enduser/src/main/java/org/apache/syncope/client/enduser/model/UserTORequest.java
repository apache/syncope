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
package org.apache.syncope.client.enduser.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.syncope.common.lib.to.AttrTO;

public class UserTORequest implements Serializable {

    private static final long serialVersionUID = -6763020920564016374L;

    private Long key;

    private String username;

    private String password;

    private Long securityQuestion;

    private String securityAnswer;

    private String realm;

    private Map<String, AttrTO> plainAttrs = new HashMap<>();

    private Map<String, AttrTO> derAttrs = new HashMap<>();

    private Map<String, AttrTO> virAttrs = new HashMap<>();

    private Set<String> resources = new HashSet<>();

    public UserTORequest() {
    }

    public Long getKey() {
        return key;
    }

    public void setKey(final Long key) {
        this.key = key;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public Long getSecurityQuestion() {
        return securityQuestion;
    }

    public void setSecurityQuestion(final Long securityQuestion) {
        this.securityQuestion = securityQuestion;
    }

    public String getSecurityAnswer() {
        return securityAnswer;
    }

    public void setSecurityAnswer(final String securityAnswer) {
        this.securityAnswer = securityAnswer;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(final String realm) {
        this.realm = realm;
    }

    public Map<String, AttrTO> getPlainAttrs() {
        return plainAttrs;
    }

    public void setPlainAttrs(final Map<String, AttrTO> plainAttrs) {
        this.plainAttrs = plainAttrs;
    }

    public Map<String, AttrTO> getDerAttrs() {
        return derAttrs;
    }

    public void setDerAttrs(final Map<String, AttrTO> derAttrs) {
        this.derAttrs = derAttrs;
    }

    public Map<String, AttrTO> getVirAttrs() {
        return virAttrs;
    }

    public void setVirAttrs(final Map<String, AttrTO> virAttrs) {
        this.virAttrs = virAttrs;
    }

    public Set<String> getResources() {
        return resources;
    }

    public void setResources(final Set<String> resources) {
        this.resources = resources;
    }

    public UserTORequest key(final Long value) {
        this.key = value;
        return this;
    }

    public UserTORequest username(final String value) {
        this.username = value;
        return this;
    }

    public UserTORequest password(final String value) {
        this.password = value;
        return this;
    }

    public UserTORequest securityQuestion(final Long value) {
        this.securityQuestion = value;
        return this;
    }

    public UserTORequest securityAnswer(final String value) {
        this.securityAnswer = value;
        return this;
    }

    public UserTORequest realm(final String value) {
        this.realm = value;
        return this;
    }

    public UserTORequest plainAttrs(final Map<String, AttrTO> value) {
        this.plainAttrs = value;
        return this;
    }

    public UserTORequest derAttrs(final Map<String, AttrTO> value) {
        this.derAttrs = value;
        return this;
    }

    public UserTORequest virAttrs(final Map<String, AttrTO> value) {
        this.virAttrs = value;
        return this;
    }

    public UserTORequest resources(final Set<String> value) {
        this.resources = value;
        return this;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}

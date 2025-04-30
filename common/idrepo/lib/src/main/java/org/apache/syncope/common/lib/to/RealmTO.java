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
package org.apache.syncope.common.lib.to;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.ws.rs.PathParam;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.Attr;

public class RealmTO implements NamedEntityTO, TemplatableTO, AttributableTO {

    private static final long serialVersionUID = 516330662956254391L;

    private String key;

    private String name;

    private String parent;

    private String fullPath;

    private String anyTypeClass;

    private String accountPolicy;

    private String passwordPolicy;

    private String authPolicy;

    private String accessPolicy;

    private String attrReleasePolicy;

    private String ticketExpirationPolicy;

    private final Set<Attr> plainAttrs = new TreeSet<>();

    private final Set<Attr> derAttrs = new TreeSet<>();

    private final Set<Attr> virAttrs = Set.of();

    private final List<String> actions = new ArrayList<>();

    private final Map<String, AnyTO> templates = new HashMap<>();

    private final List<String> resources = new ArrayList<>();

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(final String parent) {
        this.parent = parent;
    }

    public String getFullPath() {
        return fullPath;
    }

    @PathParam("fullPath")
    public void setFullPath(final String fullPath) {
        this.fullPath = fullPath;
    }

    public String getAnyTypeClass() {
        return anyTypeClass;
    }

    public void setAnyTypeClass(final String anyTypeClass) {
        this.anyTypeClass = anyTypeClass;
    }

    public String getAccountPolicy() {
        return accountPolicy;
    }

    public void setAccountPolicy(final String accountPolicy) {
        this.accountPolicy = accountPolicy;
    }

    public String getPasswordPolicy() {
        return passwordPolicy;
    }

    public void setPasswordPolicy(final String passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
    }

    public String getAuthPolicy() {
        return authPolicy;
    }

    public void setAuthPolicy(final String authPolicy) {
        this.authPolicy = authPolicy;
    }

    public String getAccessPolicy() {
        return accessPolicy;
    }

    public void setAccessPolicy(final String accessPolicy) {
        this.accessPolicy = accessPolicy;
    }

    public String getAttrReleasePolicy() {
        return attrReleasePolicy;
    }

    public void setAttrReleasePolicy(final String attrReleasePolicy) {
        this.attrReleasePolicy = attrReleasePolicy;
    }

    public String getTicketExpirationPolicy() {
        return ticketExpirationPolicy;
    }

    public void setTicketExpirationPolicy(final String ticketExpirationPolicy) {
        this.ticketExpirationPolicy = ticketExpirationPolicy;
    }

    @JacksonXmlElementWrapper(localName = "plainAttrs")
    @JacksonXmlProperty(localName = "plainAttr")
    @Override
    public Set<Attr> getPlainAttrs() {
        return plainAttrs;
    }

    @JsonIgnore
    @Override
    public Optional<Attr> getPlainAttr(final String schema) {
        return plainAttrs.stream().filter(attr -> attr.getSchema().equals(schema)).findFirst();
    }

    @JacksonXmlElementWrapper(localName = "derAttrs")
    @JacksonXmlProperty(localName = "derAttr")
    @Override
    public Set<Attr> getDerAttrs() {
        return derAttrs;
    }

    @JsonIgnore
    @Override
    public Optional<Attr> getDerAttr(final String schema) {
        return derAttrs.stream().filter(attr -> attr.getSchema().equals(schema)).findFirst();
    }

    @JacksonXmlElementWrapper(localName = "virAttrs")
    @JacksonXmlProperty(localName = "virAttr")
    @Override
    public Set<Attr> getVirAttrs() {
        return virAttrs;
    }

    @JsonIgnore
    @Override
    public Optional<Attr> getVirAttr(final String schema) {
        return Optional.empty();
    }

    @JacksonXmlElementWrapper(localName = "actions")
    @JacksonXmlProperty(localName = "action")
    public List<String> getActions() {
        return actions;
    }

    @Override
    public Map<String, AnyTO> getTemplates() {
        return templates;
    }

    @JacksonXmlElementWrapper(localName = "resources")
    @JacksonXmlProperty(localName = "resource")
    public List<String> getResources() {
        return resources;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        RealmTO other = (RealmTO) obj;
        return new EqualsBuilder().
                append(key, other.key).
                append(name, other.name).
                append(parent, other.parent).
                append(fullPath, other.fullPath).
                append(anyTypeClass, other.anyTypeClass).
                append(accountPolicy, other.accountPolicy).
                append(passwordPolicy, other.passwordPolicy).
                append(authPolicy, other.authPolicy).
                append(accessPolicy, other.accessPolicy).
                append(attrReleasePolicy, other.attrReleasePolicy).
                append(ticketExpirationPolicy, other.ticketExpirationPolicy).
                append(plainAttrs, other.plainAttrs).
                append(derAttrs, other.derAttrs).
                append(actions, other.actions).
                append(templates, other.templates).
                append(resources, other.resources).
                build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(key).
                append(name).
                append(parent).
                append(fullPath).
                append(anyTypeClass).
                append(accountPolicy).
                append(passwordPolicy).
                append(authPolicy).
                append(accessPolicy).
                append(attrReleasePolicy).
                append(ticketExpirationPolicy).
                append(plainAttrs).
                append(derAttrs).
                append(actions).
                append(templates).
                append(resources).
                build();
    }
}

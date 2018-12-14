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

import org.apache.syncope.common.lib.Attr;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.RealmMember;

@XmlType
@XmlSeeAlso({ UserTO.class, GroupTO.class, AnyObjectTO.class })
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "@class")
@JsonPropertyOrder(value = { "@class", "key", "type", "realm", "username", "name" })
@Schema(subTypes = { UserTO.class, GroupTO.class, AnyObjectTO.class }, discriminatorProperty = "@class")
public abstract class AnyTO extends AbstractAnnotatedBean implements EntityTO, RealmMember {

    private static final long serialVersionUID = -754311920679872084L;

    @XmlTransient
    @JsonProperty("@class")
    private String discriminator;

    private String key;

    private String type;

    private String realm;

    private final List<String> dynRealms = new ArrayList<>();

    private String status;

    private final Set<String> auxClasses = new HashSet<>();

    private final Set<Attr> plainAttrs = new HashSet<>();

    private final Set<Attr> derAttrs = new HashSet<>();

    private final Set<Attr> virAttrs = new HashSet<>();

    private final Set<String> resources = new HashSet<>();

    @Schema(name = "@class", required = true)
    public abstract String getDiscriminator();

    public void setDiscriminator(final String discriminator) {
        // do nothing
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    @Override
    public String getRealm() {
        return realm;
    }

    @Override
    public void setRealm(final String realm) {
        this.realm = realm;
    }

    @XmlElementWrapper(name = "dynRealms")
    @XmlElement(name = "dynRealmF")
    @JsonProperty("dynRealms")
    public List<String> getDynRealms() {
        return dynRealms;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    @XmlElementWrapper(name = "auxClasses")
    @XmlElement(name = "class")
    @JsonProperty("auxClasses")
    @Override
    public Set<String> getAuxClasses() {
        return auxClasses;
    }

    @XmlElementWrapper(name = "plainAttrs")
    @XmlElement(name = "attribute")
    @JsonProperty("plainAttrs")
    @Override
    public Set<Attr> getPlainAttrs() {
        return plainAttrs;
    }

    @JsonIgnore
    @Override
    public Optional<Attr> getPlainAttr(final String schema) {
        return plainAttrs.stream().filter(attr -> attr.getSchema().equals(schema)).findFirst();
    }

    @XmlElementWrapper(name = "derAttrs")
    @XmlElement(name = "attribute")
    @JsonProperty("derAttrs")
    @Override
    public Set<Attr> getDerAttrs() {
        return derAttrs;
    }

    @JsonIgnore
    @Override
    public Optional<Attr> getDerAttr(final String schema) {
        return derAttrs.stream().filter(attr -> attr.getSchema().equals(schema)).findFirst();
    }

    @XmlElementWrapper(name = "virAttrs")
    @XmlElement(name = "attribute")
    @JsonProperty("virAttrs")
    @Override
    public Set<Attr> getVirAttrs() {
        return virAttrs;
    }

    @JsonIgnore
    @Override
    public Optional<Attr> getVirAttr(final String schema) {
        return virAttrs.stream().filter(attr -> attr.getSchema().equals(schema)).findFirst();
    }

    @XmlElementWrapper(name = "resources")
    @XmlElement(name = "resource")
    @JsonProperty("resources")
    @Override
    public Set<String> getResources() {
        return resources;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(key).
                append(type).
                append(realm).
                append(dynRealms).
                append(status).
                append(auxClasses).
                append(plainAttrs).
                append(derAttrs).
                append(virAttrs).
                append(resources).
                build();
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
        final AnyTO other = (AnyTO) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(key, other.key).
                append(type, other.type).
                append(realm, other.realm).
                append(dynRealms, other.dynRealms).
                append(status, other.status).
                append(auxClasses, other.auxClasses).
                append(plainAttrs, other.plainAttrs).
                append(derAttrs, other.derAttrs).
                append(virAttrs, other.virAttrs).
                append(resources, other.resources).
                build();
    }
}

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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.RealmMember;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "_class")
@JsonPropertyOrder(value = { "_class", "key", "type", "realm", "username", "name" })
@Schema(subTypes = { UserTO.class, GroupTO.class, AnyObjectTO.class }, discriminatorProperty = "_class")
public abstract class AnyTO implements EntityTO, RealmMember {

    private static final long serialVersionUID = -754311920679872084L;

    @JacksonXmlProperty(localName = "_class", isAttribute = true)
    @JsonProperty("_class")
    private String discriminator;

    /**
     * Username of the user that has created the related instance.
     */
    private String creator;

    private OffsetDateTime creationDate;

    /**
     * Context information about create.
     */
    private String creationContext;

    /**
     * Username of the user that has performed the last modification to the related instance.
     */
    private String lastModifier;

    private OffsetDateTime lastChangeDate;

    /**
     * Context information about last change.
     */
    private String lastChangeContext;

    private String key;

    private String type;

    private String realm;

    private final List<String> dynRealms = new ArrayList<>();

    private String status;

    private final Set<String> auxClasses = new TreeSet<>();

    private final Set<Attr> plainAttrs = new TreeSet<>();

    private final Set<Attr> derAttrs = new TreeSet<>();

    private final Set<Attr> virAttrs = new TreeSet<>();

    private final Set<String> resources = new TreeSet<>();

    @Schema(name = "_class", requiredMode = Schema.RequiredMode.REQUIRED)
    public abstract String getDiscriminator();

    public void setDiscriminator(final String discriminator) {
        // do nothing
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(final String creator) {
        this.creator = creator;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(final OffsetDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public String getCreationContext() {
        return creationContext;
    }

    public void setCreationContext(final String creationContext) {
        this.creationContext = creationContext;
    }

    public String getLastModifier() {
        return lastModifier;
    }

    public void setLastModifier(final String lastModifier) {
        this.lastModifier = lastModifier;
    }

    public OffsetDateTime getLastChangeDate() {
        return lastChangeDate;
    }

    public void setLastChangeDate(final OffsetDateTime lastChangeDate) {
        this.lastChangeDate = lastChangeDate;
    }

    public String getLastChangeContext() {
        return lastChangeContext;
    }

    public void setLastChangeContext(final String lastChangeContext) {
        this.lastChangeContext = lastChangeContext;
    }

    @JsonIgnore
    public String getETagValue() {
        OffsetDateTime etagDate = getLastChangeDate() == null
                ? getCreationDate() : getLastChangeDate();
        return Optional.ofNullable(etagDate).
                map(date -> String.valueOf(date.toInstant().toEpochMilli())).
                orElse(StringUtils.EMPTY);
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

    @JacksonXmlElementWrapper(localName = "dynRealms")
    @JacksonXmlProperty(localName = "dynRealmF")
    public List<String> getDynRealms() {
        return dynRealms;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    @JacksonXmlElementWrapper(localName = "auxClasses")
    @JacksonXmlProperty(localName = "class")
    @Override
    public Set<String> getAuxClasses() {
        return auxClasses;
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
        return virAttrs.stream().filter(attr -> attr.getSchema().equals(schema)).findFirst();
    }

    @Override
    public Set<String> getResources() {
        return resources;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(creator).
                append(creationDate).
                append(creationContext).
                append(lastModifier).
                append(lastChangeDate).
                append(lastChangeContext).
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
                append(creator, other.creator).
                append(creationDate, other.creationDate).
                append(creationContext, other.creationContext).
                append(lastModifier, other.lastModifier).
                append(lastChangeDate, other.lastChangeDate).
                append(lastChangeContext, other.lastChangeContext).
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

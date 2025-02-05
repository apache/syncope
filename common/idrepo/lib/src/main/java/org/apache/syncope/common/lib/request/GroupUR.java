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
package org.apache.syncope.common.lib.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.to.TypeExtensionTO;

@Schema(allOf = { AnyUR.class })
public class GroupUR extends AnyUR {

    private static final long serialVersionUID = -460284378124440077L;

    public static class Builder extends AnyUR.Builder<GroupUR, Builder> {

        public Builder(final String key) {
            super(key);
        }

        @Override
        protected GroupUR newInstance() {
            return new GroupUR();
        }

        public Builder name(final StringReplacePatchItem name) {
            getInstance().setName(name);
            return this;
        }

        public Builder userOwner(final StringReplacePatchItem userOwner) {
            getInstance().setUserOwner(userOwner);
            return this;
        }

        public Builder groupOwner(final StringReplacePatchItem groupOwner) {
            getInstance().setGroupOwner(groupOwner);
            return this;
        }

        public Builder udynMembershipCond(final String udynMembershipCond) {
            getInstance().setUDynMembershipCond(udynMembershipCond);
            return this;
        }

        public Builder adynMembershipCond(final String type, final String fiql) {
            getInstance().getADynMembershipConds().put(type, fiql);
            return this;
        }

        public Builder adynMembershipConds(final Map<String, String> conds) {
            getInstance().getADynMembershipConds().putAll(conds);
            return this;
        }

        public Builder typeExtension(final TypeExtensionTO typeExtension) {
            getInstance().getTypeExtensions().add(typeExtension);
            return this;
        }

        public Builder typeExtensions(final TypeExtensionTO... typeExtensions) {
            getInstance().getTypeExtensions().addAll(List.of(typeExtensions));
            return this;
        }

        public Builder typeExtensions(final Collection<TypeExtensionTO> typeExtensions) {
            getInstance().getTypeExtensions().addAll(typeExtensions);
            return this;
        }
    }

    private StringReplacePatchItem name;

    private StringReplacePatchItem userOwner;

    private StringReplacePatchItem groupOwner;

    private String udynMembershipCond;

    private final Map<String, String> adynMembershipConds = new HashMap<>();

    private final List<TypeExtensionTO> typeExtensions = new ArrayList<>();

    private final Set<RelationshipUR> relationships = new HashSet<>();

    @JacksonXmlProperty(localName = "_class", isAttribute = true)
    @JsonProperty("_class")
    @Schema(name = "_class", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "org.apache.syncope.common.lib.request.GroupUR")
    @Override
    public String getDiscriminator() {
        return getClass().getName();
    }

    public StringReplacePatchItem getName() {
        return name;
    }

    public void setName(final StringReplacePatchItem name) {
        this.name = name;
    }

    public StringReplacePatchItem getUserOwner() {
        return userOwner;
    }

    public void setUserOwner(final StringReplacePatchItem userOwner) {
        this.userOwner = userOwner;
    }

    public StringReplacePatchItem getGroupOwner() {
        return groupOwner;
    }

    public void setGroupOwner(final StringReplacePatchItem groupOwner) {
        this.groupOwner = groupOwner;
    }

    public String getUDynMembershipCond() {
        return udynMembershipCond;
    }

    public void setUDynMembershipCond(final String udynMembershipCond) {
        this.udynMembershipCond = udynMembershipCond;
    }

    public Map<String, String> getADynMembershipConds() {
        return adynMembershipConds;
    }

    @JsonIgnore
    public Optional<TypeExtensionTO> getTypeExtension(final String anyType) {
        return typeExtensions.stream().filter(
                typeExtension -> anyType != null && anyType.equals(typeExtension.getAnyType())).findFirst();
    }

    @JacksonXmlElementWrapper(localName = "typeExtensions")
    @JacksonXmlProperty(localName = "typeExtension")
    public List<TypeExtensionTO> getTypeExtensions() {
        return typeExtensions;
    }

    @JacksonXmlElementWrapper(localName = "relationships")
    @JacksonXmlProperty(localName = "relationship")
    public Set<RelationshipUR> getRelationships() {
        return relationships;
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty()
                && name == null && userOwner == null && groupOwner == null && udynMembershipCond == null
                && adynMembershipConds.isEmpty() && typeExtensions.isEmpty() && relationships.isEmpty();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(name).
                append(userOwner).
                append(groupOwner).
                append(udynMembershipCond).
                append(adynMembershipConds).
                append(typeExtensions).
                append(relationships).
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
        final GroupUR other = (GroupUR) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(name, other.name).
                append(userOwner, other.userOwner).
                append(groupOwner, other.groupOwner).
                append(udynMembershipCond, other.udynMembershipCond).
                append(adynMembershipConds, other.adynMembershipConds).
                append(typeExtensions, other.typeExtensions).
                append(relationships, other.relationships).
                build();
    }
}

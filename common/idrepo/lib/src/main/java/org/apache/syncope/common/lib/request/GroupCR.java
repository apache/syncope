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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.to.RelatableTO;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.to.TypeExtensionTO;

@JsonPropertyOrder(value = { "_class", "name" })
@Schema(allOf = { AnyCR.class })
public class GroupCR extends AnyCR implements RelatableTO {

    private static final long serialVersionUID = -4559772531167385473L;

    public static class Builder extends AnyCR.Builder<GroupCR, Builder> {

        public Builder(final String realm, final String name) {
            super(realm);
            getInstance().setName(name);
        }

        @Override
        protected GroupCR newInstance() {
            return new GroupCR();
        }

        public Builder userOwner(final String userOwner) {
            getInstance().setUserOwner(userOwner);
            return this;
        }

        public Builder groupOwner(final String groupOwner) {
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

        public Builder relationship(final RelationshipTO relationship) {
            getInstance().getRelationships().add(relationship);
            return this;
        }

        public Builder relationships(final RelationshipTO... relationships) {
            getInstance().getRelationships().addAll(List.of(relationships));
            return this;
        }

        public Builder relationships(final Collection<RelationshipTO> relationships) {
            getInstance().getRelationships().addAll(relationships);
            return this;
        }
    }

    private String name;

    private String userOwner;

    private String groupOwner;

    private String udynMembershipCond;

    private final Map<String, String> adynMembershipConds = new HashMap<>();

    private final List<TypeExtensionTO> typeExtensions = new ArrayList<>();

    private final List<RelationshipTO> relationships = new ArrayList<>();

    @JacksonXmlProperty(localName = "_class", isAttribute = true)
    @JsonProperty("_class")
    @Schema(name = "_class", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "org.apache.syncope.common.lib.request.GroupCR")
    @Override
    public String getDiscriminator() {
        return getClass().getName();
    }

    @JsonProperty(required = true)
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getUserOwner() {
        return userOwner;
    }

    public void setUserOwner(final String userOwner) {
        this.userOwner = userOwner;
    }

    public String getGroupOwner() {
        return groupOwner;
    }

    public void setGroupOwner(final String groupOwner) {
        this.groupOwner = groupOwner;
    }

    public String getUDynMembershipCond() {
        return udynMembershipCond;
    }

    public void setUDynMembershipCond(final String uDynMembershipCond) {
        this.udynMembershipCond = uDynMembershipCond;
    }

    public Map<String, String> getADynMembershipConds() {
        return adynMembershipConds;
    }

    @JacksonXmlElementWrapper(localName = "typeExtensions")
    @JacksonXmlProperty(localName = "typeExtension")
    public List<TypeExtensionTO> getTypeExtensions() {
        return typeExtensions;
    }

    @JsonIgnore
    @Override
    public Optional<RelationshipTO> getRelationship(final String type, final String otherKey) {
        return relationships.stream().filter(
                relationship -> type.equals(relationship.getType()) && otherKey.equals(relationship.getOtherEndKey())).
                findFirst();
    }

    @JacksonXmlElementWrapper(localName = "relationships")
    @JacksonXmlProperty(localName = "relationship")
    @Override
    public List<RelationshipTO> getRelationships() {
        return relationships;
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
        final GroupCR other = (GroupCR) obj;
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

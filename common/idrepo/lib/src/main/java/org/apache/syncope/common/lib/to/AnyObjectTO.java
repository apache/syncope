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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Schema(allOf = { AnyTO.class })
public class AnyObjectTO extends AnyTO implements GroupableRelatableTO {

    private static final long serialVersionUID = 8841697496476959639L;

    private String name;

    private final List<RelationshipTO> relationships = new ArrayList<>();

    private final List<MembershipTO> memberships = new ArrayList<>();

    private final List<MembershipTO> dynMemberships = new ArrayList<>();

    @JacksonXmlProperty(localName = "_class", isAttribute = true)
    @JsonProperty("_class")
    @Schema(name = "_class", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "org.apache.syncope.common.lib.to.AnyObjectTO")
    @Override
    public String getDiscriminator() {
        return getClass().getName();
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
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

    @JsonIgnore
    @Override
    public Optional<MembershipTO> getMembership(final String groupKey) {
        return memberships.stream().filter(membership -> groupKey.equals(membership.getGroupKey())).findFirst();
    }

    @JacksonXmlElementWrapper(localName = "memberships")
    @JacksonXmlProperty(localName = "membership")
    @Override
    public List<MembershipTO> getMemberships() {
        return memberships;
    }

    @JacksonXmlElementWrapper(localName = "dynMemberships")
    @JacksonXmlProperty(localName = "dynMembership")
    @Override
    public List<MembershipTO> getDynMemberships() {
        return dynMemberships;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(name).
                append(relationships).
                append(memberships).
                append(dynMemberships).
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
        final AnyObjectTO other = (AnyObjectTO) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(name, other.name).
                append(relationships, other.relationships).
                append(memberships, other.memberships).
                append(dynMemberships, other.dynMemberships).
                build();
    }
}

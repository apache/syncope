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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Schema(allOf = { AnyUR.class })
public class AnyObjectUR extends AnyUR {

    private static final long serialVersionUID = -1644118942622556097L;

    public static class Builder extends AnyUR.Builder<AnyObjectUR, Builder> {

        public Builder(final String key) {
            super(key);
        }

        @Override
        protected AnyObjectUR newInstance() {
            return new AnyObjectUR();
        }

        public Builder name(final StringReplacePatchItem name) {
            getInstance().setName(name);
            return this;
        }

        public Builder relationship(final RelationshipUR relationship) {
            getInstance().getRelationships().add(relationship);
            return this;
        }

        public Builder relationships(final RelationshipUR... relationships) {
            getInstance().getRelationships().addAll(List.of(relationships));
            return this;
        }

        public Builder relationships(final Collection<RelationshipUR> relationships) {
            getInstance().getRelationships().addAll(relationships);
            return this;
        }

        public Builder membership(final MembershipUR membership) {
            getInstance().getMemberships().add(membership);
            return this;
        }

        public Builder memberships(final MembershipUR... memberships) {
            getInstance().getMemberships().addAll(List.of(memberships));
            return this;
        }

        public Builder memberships(final Collection<MembershipUR> memberships) {
            getInstance().getMemberships().addAll(memberships);
            return this;
        }
    }

    private StringReplacePatchItem name;

    private final Set<RelationshipUR> relationships = new HashSet<>();

    private final Set<MembershipUR> memberships = new HashSet<>();

    @JacksonXmlProperty(localName = "_class", isAttribute = true)
    @JsonProperty("_class")
    @Schema(name = "_class", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "org.apache.syncope.common.lib.request.AnyObjectUR")
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

    @JacksonXmlElementWrapper(localName = "relationships")
    @JacksonXmlProperty(localName = "relationship")
    public Set<RelationshipUR> getRelationships() {
        return relationships;
    }

    @JacksonXmlElementWrapper(localName = "memberships")
    @JacksonXmlProperty(localName = "membership")
    public Set<MembershipUR> getMemberships() {
        return memberships;
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty() && name == null && relationships.isEmpty() && memberships.isEmpty();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(name).
                append(relationships).
                append(memberships).
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
        final AnyObjectUR other = (AnyObjectUR) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(name, other.name).
                append(relationships, other.relationships).
                append(memberships, other.memberships).
                build();
    }
}

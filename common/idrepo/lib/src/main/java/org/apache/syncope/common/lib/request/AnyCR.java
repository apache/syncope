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
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.BaseBean;
import org.apache.syncope.common.lib.RealmMember;
import org.apache.syncope.common.lib.to.RelatableTO;
import org.apache.syncope.common.lib.to.RelationshipTO;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "_class")
@JsonPropertyOrder(value = { "_class" })
@Schema(subTypes = { UserCR.class, GroupCR.class, AnyObjectCR.class }, discriminatorProperty = "_class")
public abstract class AnyCR implements BaseBean, RealmMember, RelatableTO {

    private static final long serialVersionUID = -1180587903919947455L;

    protected abstract static class Builder<R extends AnyCR, B extends Builder<R, B>> {

        protected R instance;

        Builder(final String realm) {
            getInstance().setRealm(realm);
        }

        protected abstract R newInstance();

        protected final R getInstance() {
            if (instance == null) {
                instance = newInstance();
            }
            return instance;
        }

        @SuppressWarnings("unchecked")
        public B uManager(final String uManager) {
            getInstance().setUManager(uManager);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B gManager(final String gManager) {
            getInstance().setGManager(gManager);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B auxClass(final String auxClass) {
            getInstance().getAuxClasses().add(auxClass);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B auxClasses(final String... auxClasses) {
            getInstance().getAuxClasses().addAll(List.of(auxClasses));
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B auxClasses(final Collection<String> auxClasses) {
            getInstance().getAuxClasses().addAll(auxClasses);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B plainAttr(final Attr plainAttr) {
            getInstance().getPlainAttrs().add(plainAttr);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B plainAttrs(final Attr... plainAttrs) {
            getInstance().getPlainAttrs().addAll(List.of(plainAttrs));
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B plainAttrs(final Collection<Attr> plainAttrs) {
            getInstance().getPlainAttrs().addAll(plainAttrs);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B resource(final String resource) {
            getInstance().getResources().add(resource);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B resources(final String... resources) {
            getInstance().getResources().addAll(List.of(resources));
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B resources(final Collection<String> resources) {
            getInstance().getResources().addAll(resources);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B relationship(final RelationshipTO relationship) {
            getInstance().getRelationships().add(relationship);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B relationships(final RelationshipTO... relationships) {
            getInstance().getRelationships().addAll(List.of(relationships));
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B relationships(final Collection<RelationshipTO> relationships) {
            getInstance().getRelationships().addAll(relationships);
            return (B) this;
        }

        public R build() {
            return getInstance();
        }
    }

    @JsonProperty("_class")
    private String discriminator;

    private String realm;

    private String uManager;

    private String gManager;

    private final Set<String> auxClasses = new HashSet<>();

    private final Set<Attr> plainAttrs = new HashSet<>();

    private final Set<String> resources = new HashSet<>();

    private final List<RelationshipTO> relationships = new ArrayList<>();

    @Schema(name = "_class", requiredMode = Schema.RequiredMode.REQUIRED)
    public abstract String getDiscriminator();

    public void setDiscriminator(final String discriminator) {
        // do nothing
    }

    @JsonProperty(required = true)
    @Override
    public String getRealm() {
        return realm;
    }

    @Override
    public void setRealm(final String realm) {
        this.realm = realm;
    }

    public String getUManager() {
        return uManager;
    }

    public void setUManager(final String uManager) {
        this.uManager = uManager;
    }

    public String getGManager() {
        return gManager;
    }

    public void setGManager(final String gManager) {
        this.gManager = gManager;
    }

    @Override
    public Set<String> getAuxClasses() {
        return auxClasses;
    }

    @JsonIgnore
    @Override
    public Optional<Attr> getPlainAttr(final String schema) {
        return plainAttrs.stream().filter(attr -> attr.getSchema().equals(schema)).findFirst();
    }

    @Override
    public Set<Attr> getPlainAttrs() {
        return plainAttrs;
    }

    @JsonIgnore
    @Override
    public Optional<Attr> getDerAttr(final String schema) {
        return Optional.empty();
    }

    @JsonIgnore
    @Override
    public Set<Attr> getDerAttrs() {
        return Set.of();
    }

    @Override
    public Set<String> getResources() {
        return resources;
    }

    @JsonIgnore
    @Override
    public Optional<RelationshipTO> getRelationship(final String type, final String otherKey) {
        return relationships.stream().filter(
                relationship -> type.equals(relationship.getType()) && otherKey.equals(relationship.getOtherEndKey())).
                findFirst();
    }

    @Override
    public List<RelationshipTO> getRelationships() {
        return relationships;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(discriminator).
                append(realm).
                append(uManager).
                append(gManager).
                append(auxClasses).
                append(plainAttrs).
                append(resources).
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
        final AnyCR other = (AnyCR) obj;
        return new EqualsBuilder().
                append(discriminator, other.discriminator).
                append(realm, other.realm).
                append(uManager, other.uManager).
                append(gManager, other.gManager).
                append(auxClasses, other.auxClasses).
                append(plainAttrs, other.plainAttrs).
                append(resources, other.resources).
                append(relationships, other.relationships).
                build();
    }
}

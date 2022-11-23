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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "_class")
@JsonPropertyOrder(value = { "_class" })
@Schema(subTypes = { UserCR.class, GroupCR.class, AnyObjectCR.class }, discriminatorProperty = "_class")
public abstract class AnyCR implements BaseBean, RealmMember {

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
        public B virAttr(final Attr virAttr) {
            getInstance().getVirAttrs().add(virAttr);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B virAttrs(final Collection<Attr> virAttrs) {
            getInstance().getVirAttrs().addAll(virAttrs);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B virAttrs(final Attr... virAttrs) {
            getInstance().getVirAttrs().addAll(List.of(virAttrs));
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

        public R build() {
            return getInstance();
        }
    }

    @JacksonXmlProperty(localName = "_class", isAttribute = true)
    @JsonProperty("_class")
    private String discriminator;

    private String realm;

    private final Set<String> auxClasses = new HashSet<>();

    private final Set<Attr> plainAttrs = new HashSet<>();

    private final Set<Attr> virAttrs = new HashSet<>();

    private final Set<String> resources = new HashSet<>();

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

    @Override
    public Set<String> getAuxClasses() {
        return auxClasses;
    }

    @JsonIgnore
    @Override
    public Optional<Attr> getPlainAttr(final String schema) {
        return plainAttrs.stream().filter(attr -> attr.getSchema().equals(schema)).findFirst();
    }

    @JacksonXmlElementWrapper(localName = "plainAttrs")
    @JacksonXmlProperty(localName = "plainAttr")
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

    @JsonIgnore
    @Override
    public Optional<Attr> getVirAttr(final String schema) {
        return virAttrs.stream().filter(attr -> attr.getSchema().equals(schema)).findFirst();
    }

    @JacksonXmlElementWrapper(localName = "virAttrs")
    @JacksonXmlProperty(localName = "virAttr")
    @Override
    public Set<Attr> getVirAttrs() {
        return virAttrs;
    }

    @JacksonXmlElementWrapper(localName = "resources")
    @JacksonXmlProperty(localName = "resource")
    @Override
    public Set<String> getResources() {
        return resources;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(discriminator).
                append(realm).
                append(auxClasses).
                append(plainAttrs).
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
        final AnyCR other = (AnyCR) obj;
        return new EqualsBuilder().
                append(discriminator, other.discriminator).
                append(realm, other.realm).
                append(auxClasses, other.auxClasses).
                append(plainAttrs, other.plainAttrs).
                append(virAttrs, other.virAttrs).
                append(resources, other.resources).
                build();
    }
}

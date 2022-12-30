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
import jakarta.ws.rs.PathParam;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.BaseBean;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "_class")
@JsonPropertyOrder(value = { "_class", "key" })
@Schema(subTypes = { UserUR.class, GroupUR.class, AnyObjectUR.class }, discriminatorProperty = "_class")
public abstract class AnyUR implements BaseBean {

    private static final long serialVersionUID = -7445489774552440544L;

    protected abstract static class Builder<R extends AnyUR, B extends Builder<R, B>> {

        protected R instance;

        Builder(final String key) {
            getInstance().setKey(key);
        }

        protected abstract R newInstance();

        protected final R getInstance() {
            if (instance == null) {
                instance = newInstance();
            }
            return instance;
        }

        @SuppressWarnings("unchecked")
        public B realm(final StringReplacePatchItem realm) {
            getInstance().setRealm(realm);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B auxClass(final StringPatchItem auxClass) {
            getInstance().getAuxClasses().add(auxClass);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B auxClasses(final StringPatchItem... auxClasses) {
            getInstance().getAuxClasses().addAll(List.of(auxClasses));
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B auxClasses(final Collection<StringPatchItem> auxClasses) {
            getInstance().getAuxClasses().addAll(auxClasses);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B plainAttr(final AttrPatch plainAttr) {
            getInstance().getPlainAttrs().add(plainAttr);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B plainAttrs(final AttrPatch... plainAttrs) {
            getInstance().getPlainAttrs().addAll(List.of(plainAttrs));
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B plainAttrs(final Collection<AttrPatch> plainAttrs) {
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
        public B resource(final StringPatchItem resource) {
            getInstance().getResources().add(resource);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B resources(final StringPatchItem... resources) {
            getInstance().getResources().addAll(List.of(resources));
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B resources(final Collection<StringPatchItem> resources) {
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

    private String key;

    private StringReplacePatchItem realm;

    private final Set<StringPatchItem> auxClasses = new HashSet<>();

    private final Set<AttrPatch> plainAttrs = new HashSet<>();

    private final Set<Attr> virAttrs = new HashSet<>();

    private final Set<StringPatchItem> resources = new HashSet<>();

    @Schema(name = "_class", requiredMode = Schema.RequiredMode.REQUIRED)
    public abstract String getDiscriminator();

    public void setDiscriminator(final String discriminator) {
        // do nothing
    }

    @JsonProperty(required = true)
    public String getKey() {
        return key;
    }

    @PathParam("key")
    public void setKey(final String key) {
        this.key = key;
    }

    public StringReplacePatchItem getRealm() {
        return realm;
    }

    public void setRealm(final StringReplacePatchItem realm) {
        this.realm = realm;
    }

    @JacksonXmlElementWrapper(localName = "auxClasses")
    @JacksonXmlProperty(localName = "auxClass")
    public Set<StringPatchItem> getAuxClasses() {
        return auxClasses;
    }

    @JacksonXmlElementWrapper(localName = "plainAttrs")
    @JacksonXmlProperty(localName = "plainAttr")
    public Set<AttrPatch> getPlainAttrs() {
        return plainAttrs;
    }

    @JacksonXmlElementWrapper(localName = "virAttrs")
    @JacksonXmlProperty(localName = "virAttr")
    public Set<Attr> getVirAttrs() {
        return virAttrs;
    }

    @JacksonXmlElementWrapper(localName = "resources")
    @JacksonXmlProperty(localName = "resource")
    public Set<StringPatchItem> getResources() {
        return resources;
    }

    /**
     * @return true if no actual changes are defined
     */
    @JsonIgnore
    public boolean isEmpty() {
        return realm == null
                && auxClasses.isEmpty()
                && plainAttrs.isEmpty() && virAttrs.isEmpty()
                && resources.isEmpty();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(discriminator).
                append(key).
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
        final AnyUR other = (AnyUR) obj;
        return new EqualsBuilder().
                append(discriminator, other.discriminator).
                append(key, other.key).
                append(realm, other.realm).
                append(auxClasses, other.auxClasses).
                append(plainAttrs, other.plainAttrs).
                append(virAttrs, other.virAttrs).
                append(resources, other.resources).
                build();
    }
}

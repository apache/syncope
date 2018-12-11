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
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.AttributableReqEntity;

@XmlType
@XmlSeeAlso({ UserUR.class, GroupUR.class, AnyObjectUR.class })
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "@class")
@JsonPropertyOrder(value = { "@class" })
@Schema(subTypes = { UserCR.class, GroupCR.class, AnyObjectCR.class }, discriminatorProperty = "@class")
public abstract class AnyCR implements Serializable, AttributableReqEntity {

    private static final long serialVersionUID = -1180587903919947455L;

    protected abstract static class Builder<R extends AnyCR, B extends Builder<R, B>> {

        protected R instance;

        protected abstract R newInstance();

        protected R getInstance() {
            if (instance == null) {
                instance = newInstance();
            }
            return instance;
        }

        @SuppressWarnings("unchecked")
        public B realm(final String realm) {
            getInstance().setRealm(realm);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B auxClass(final String auxClass) {
            getInstance().getAuxClasses().add(auxClass);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B auxClasses(final String... auxClasses) {
            getInstance().getAuxClasses().addAll(Arrays.asList(auxClasses));
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B auxClasses(final Collection<String> auxClasses) {
            getInstance().getAuxClasses().addAll(auxClasses);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B plainAttr(final AttrTO plainAttr) {
            getInstance().getPlainAttrs().add(plainAttr);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B plainAttrs(final AttrTO... plainAttrs) {
            getInstance().getPlainAttrs().addAll(Arrays.asList(plainAttrs));
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B plainAttrs(final Collection<AttrTO> plainAttrs) {
            getInstance().getPlainAttrs().addAll(plainAttrs);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B virAttr(final AttrTO virAttr) {
            getInstance().getVirAttrs().add(virAttr);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B virAttrs(final Collection<AttrTO> virAttrs) {
            getInstance().getVirAttrs().addAll(virAttrs);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B virAttrs(final AttrTO... virAttrs) {
            getInstance().getVirAttrs().addAll(Arrays.asList(virAttrs));
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B resource(final String resource) {
            getInstance().getResources().add(resource);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B resources(final String... resources) {
            getInstance().getResources().addAll(Arrays.asList(resources));
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

    @XmlTransient
    @JsonProperty("@class")
    private String discriminator;

    private String realm;

    private final Set<String> auxClasses = new HashSet<>();

    private final Set<AttrTO> plainAttrs = new HashSet<>();

    private final Set<AttrTO> virAttrs = new HashSet<>();

    private final Set<String> resources = new HashSet<>();

    @Schema(name = "@class", required = true)
    public abstract String getDiscriminator();

    public void setDiscriminator(final String discriminator) {
        // do nothing
    }

    @JsonProperty(required = true)
    @XmlElement(required = true)
    @Override
    public String getRealm() {
        return realm;
    }

    @Override
    public void setRealm(final String realm) {
        this.realm = realm;
    }

    @XmlElementWrapper(name = "auxClasses")
    @XmlElement(name = "class")
    @JsonProperty("auxClasses")
    @Override
    public Set<String> getAuxClasses() {
        return auxClasses;
    }

    @JsonIgnore
    @Override
    public Optional<AttrTO> getPlainAttr(final String schema) {
        return plainAttrs.stream().filter(attr -> attr.getSchema().equals(schema)).findFirst();
    }

    @XmlElementWrapper(name = "plainAttrs")
    @XmlElement(name = "attribute")
    @JsonProperty("plainAttrs")
    @Override
    public Set<AttrTO> getPlainAttrs() {
        return plainAttrs;
    }

    @JsonIgnore
    @Override
    public Optional<AttrTO> getDerAttr(final String schema) {
        return Optional.empty();
    }

    @XmlTransient
    @JsonIgnore
    @Override
    public Set<AttrTO> getDerAttrs() {
        return Collections.emptySet();
    }

    @JsonIgnore
    @Override
    public Optional<AttrTO> getVirAttr(final String schema) {
        return virAttrs.stream().filter(attr -> attr.getSchema().equals(schema)).findFirst();
    }

    @XmlElementWrapper(name = "virAttrs")
    @XmlElement(name = "attribute")
    @JsonProperty("virAttrs")
    @Override
    public Set<AttrTO> getVirAttrs() {
        return virAttrs;
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

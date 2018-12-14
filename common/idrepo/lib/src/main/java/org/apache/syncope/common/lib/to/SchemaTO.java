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
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.ws.rs.PathParam;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.jaxb.XmlGenericMapAdapter;

@XmlRootElement(name = "schema")
@XmlType
@XmlSeeAlso({ PlainSchemaTO.class, DerSchemaTO.class, VirSchemaTO.class })
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "@class")
@JsonPropertyOrder(value = { "@class", "key" })
@Schema(subTypes = { PlainSchemaTO.class, DerSchemaTO.class, VirSchemaTO.class }, discriminatorProperty = "@class")
public abstract class SchemaTO implements EntityTO {

    private static final long serialVersionUID = 4088388951694301759L;

    @XmlTransient
    @JsonProperty("@class")
    private String discriminator;

    private String key;

    private String anyTypeClass;

    @XmlJavaTypeAdapter(XmlGenericMapAdapter.class)
    private final Map<Locale, String> labels = new HashMap<>();

    @Schema(name = "@class", required = true)
    public abstract String getDiscriminator();

    public void setDiscriminator(final String discriminator) {
        // do nothing
    }

    @Override
    public String getKey() {
        return key;
    }

    @PathParam("key")
    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    public String getAnyTypeClass() {
        return anyTypeClass;
    }

    public void setAnyTypeClass(final String anyTypeClass) {
        this.anyTypeClass = anyTypeClass;
    }

    @JsonIgnore
    public String getLabel(final Locale locale) {
        return labels.getOrDefault(locale, key);
    }

    @JsonProperty
    public Map<Locale, String> getLabels() {
        return labels;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(discriminator).
                append(key).
                append(anyTypeClass).
                append(labels).
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
        final SchemaTO other = (SchemaTO) obj;
        return new EqualsBuilder().
                append(discriminator, other.discriminator).
                append(key, other.key).
                append(anyTypeClass, other.anyTypeClass).
                append(labels, other.labels).
                build();
    }
}

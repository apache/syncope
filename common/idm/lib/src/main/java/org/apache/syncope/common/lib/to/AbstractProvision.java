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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "_class")
@JsonPropertyOrder(value = { "_class" })
@Schema(subTypes = { LiveSyncProvision.class, ResourceProvision.class }, discriminatorProperty = "_class")
public abstract class AbstractProvision implements Serializable {

    private static final long serialVersionUID = -4008175723725111052L;

    @JacksonXmlProperty(localName = "_class", isAttribute = true)
    @JsonProperty("_class")
    private String discriminator;

    private String anyType;

    private String objectClass;

    private final List<String> auxClasses = new ArrayList<>();

    private boolean ignoreCaseMatch;

    private Mapping mapping;

    @Schema(name = "_class", required = true)
    public abstract String getDiscriminator();

    public void setDiscriminator(final String discriminator) {
        // do nothing
    }

    public String getAnyType() {
        return anyType;
    }

    public void setAnyType(final String anyType) {
        this.anyType = anyType;
    }

    public String getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(final String objectClass) {
        this.objectClass = objectClass;
    }

    @JacksonXmlElementWrapper(localName = "auxClasses")
    @JacksonXmlProperty(localName = "class")
    public List<String> getAuxClasses() {
        return auxClasses;
    }

    public boolean isIgnoreCaseMatch() {
        return ignoreCaseMatch;
    }

    public void setIgnoreCaseMatch(final boolean ignoreCaseMatch) {
        this.ignoreCaseMatch = ignoreCaseMatch;
    }

    public Mapping getMapping() {
        return mapping;
    }

    public void setMapping(final Mapping mapping) {
        this.mapping = mapping;
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
        AbstractProvision other = (AbstractProvision) obj;
        return new EqualsBuilder().
                append(anyType, other.anyType).
                append(objectClass, other.objectClass).
                append(auxClasses, other.auxClasses).
                append(mapping, other.mapping).
                append(ignoreCaseMatch, other.ignoreCaseMatch).
                build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(anyType).
                append(objectClass).
                append(auxClasses).
                append(ignoreCaseMatch).
                append(mapping).
                build();
    }
}

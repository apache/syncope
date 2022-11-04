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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Schema(allOf = { AbstractProvision.class }, discriminatorProperty = "_class")
public class ResourceProvision extends AbstractProvision {

    private static final long serialVersionUID = -346966554338702676L;
    private String syncToken;

    private String uidOnCreate;

    private final List<String> virSchemas = new ArrayList<>();

    @JacksonXmlProperty(localName = "_class", isAttribute = true)
    @JsonProperty("_class")
    @Schema(name = "_class", required = true, example = "org.apache.syncope.common.lib.to.ResourceProvision")
    @Override
    public String getDiscriminator() {
        return getClass().getName();
    }

    public String getSyncToken() {
        return syncToken;
    }

    public void setSyncToken(final String syncToken) {
        this.syncToken = syncToken;
    }

    public String getUidOnCreate() {
        return uidOnCreate;
    }

    public void setUidOnCreate(final String uidOnCreate) {
        this.uidOnCreate = uidOnCreate;
    }

    @JacksonXmlElementWrapper(localName = "virSchemas")
    @JacksonXmlProperty(localName = "virSchema")
    public List<String> getVirSchemas() {
        return virSchemas;
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
        ResourceProvision other = (ResourceProvision) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(other)).
                append(syncToken, other.syncToken).
                append(uidOnCreate, other.uidOnCreate).
                append(virSchemas, other.virSchemas).
                build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(syncToken).
                append(uidOnCreate).
                append(virSchemas).
                build();
    }
}

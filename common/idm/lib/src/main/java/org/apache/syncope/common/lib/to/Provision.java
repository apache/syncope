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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Provision implements Serializable {

    private static final long serialVersionUID = 8298910216218007927L;

    private String anyType;

    private String objectClass;

    private final List<String> auxClasses = new ArrayList<>();

    private String syncToken;

    private boolean ignoreCaseMatch;

    private String uidOnCreate;

    private Mapping mapping;

    private final List<String> virSchemas = new ArrayList<>();

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

    public String getSyncToken() {
        return syncToken;
    }

    public void setSyncToken(final String syncToken) {
        this.syncToken = syncToken;
    }

    public boolean isIgnoreCaseMatch() {
        return ignoreCaseMatch;
    }

    public void setIgnoreCaseMatch(final boolean ignoreCaseMatch) {
        this.ignoreCaseMatch = ignoreCaseMatch;
    }

    public String getUidOnCreate() {
        return uidOnCreate;
    }

    public void setUidOnCreate(final String uidOnCreate) {
        this.uidOnCreate = uidOnCreate;
    }

    public Mapping getMapping() {
        return mapping;
    }

    public void setMapping(final Mapping mapping) {
        this.mapping = mapping;
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
        Provision other = (Provision) obj;
        return new EqualsBuilder().
                append(ignoreCaseMatch, other.ignoreCaseMatch).
                append(anyType, other.anyType).
                append(objectClass, other.objectClass).
                append(auxClasses, other.auxClasses).
                append(syncToken, other.syncToken).
                append(uidOnCreate, other.uidOnCreate).
                append(mapping, other.mapping).
                append(virSchemas, other.virSchemas).
                build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(anyType).
                append(objectClass).
                append(auxClasses).
                append(syncToken).
                append(ignoreCaseMatch).
                append(uidOnCreate).
                append(mapping).
                append(virSchemas).
                build();
    }
}

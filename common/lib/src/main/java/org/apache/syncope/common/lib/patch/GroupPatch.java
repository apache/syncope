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
package org.apache.syncope.common.lib.patch;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.jaxb.XmlGenericMapAdapter;
import org.apache.syncope.common.lib.to.TypeExtensionTO;

@XmlRootElement(name = "groupPatch")
@XmlType
@Schema(allOf = { AnyPatch.class })
public class GroupPatch extends AnyPatch {

    private static final long serialVersionUID = -460284378124440077L;

    private StringReplacePatchItem name;

    private StringReplacePatchItem userOwner;

    private StringReplacePatchItem groupOwner;

    private String udynMembershipCond;

    @XmlJavaTypeAdapter(XmlGenericMapAdapter.class)
    private final Map<String, String> adynMembershipConds = new HashMap<>();

    private final List<TypeExtensionTO> typeExtensions = new ArrayList<>();

    @JsonProperty("@class")
    @Schema(name = "@class", required = true, example = "org.apache.syncope.common.lib.patch.GroupPatch")
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

    public StringReplacePatchItem getUserOwner() {
        return userOwner;
    }

    public void setUserOwner(final StringReplacePatchItem userOwner) {
        this.userOwner = userOwner;
    }

    public StringReplacePatchItem getGroupOwner() {
        return groupOwner;
    }

    public void setGroupOwner(final StringReplacePatchItem groupOwner) {
        this.groupOwner = groupOwner;
    }

    public String getUDynMembershipCond() {
        return udynMembershipCond;
    }

    public void setUDynMembershipCond(final String udynMembershipCond) {
        this.udynMembershipCond = udynMembershipCond;
    }

    @JsonProperty
    public Map<String, String> getADynMembershipConds() {
        return adynMembershipConds;
    }

    @JsonIgnore
    public Optional<TypeExtensionTO> getTypeExtension(final String anyType) {
        return typeExtensions.stream().filter(
                typeExtension -> anyType != null && anyType.equals(typeExtension.getAnyType())).findFirst();
    }

    @XmlElementWrapper(name = "typeExtensions")
    @XmlElement(name = "typeExtension")
    @JsonProperty("typeExtensions")
    public List<TypeExtensionTO> getTypeExtensions() {
        return typeExtensions;
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty()
                && name == null && userOwner == null && groupOwner == null;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                appendSuper(super.hashCode()).
                append(name).
                append(userOwner).
                append(groupOwner).
                append(udynMembershipCond).
                append(adynMembershipConds).
                append(typeExtensions).
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
        final GroupPatch other = (GroupPatch) obj;
        return new EqualsBuilder().
                appendSuper(super.equals(obj)).
                append(name, other.name).
                append(userOwner, other.userOwner).
                append(groupOwner, other.groupOwner).
                append(udynMembershipCond, other.udynMembershipCond).
                append(adynMembershipConds, other.adynMembershipConds).
                append(typeExtensions, other.typeExtensions).
                build();
    }
}

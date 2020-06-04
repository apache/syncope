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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.types.AnyTypeKind;

@Schema(allOf = { AnyTO.class })
public class GroupTO extends AnyTO {

    private static final long serialVersionUID = -7785920258290147542L;

    private String name;

    private String userOwner;

    private String groupOwner;

    private String udynMembershipCond;

    private int staticUserMembershipCount;

    private int dynamicUserMembershipCount;

    private int staticAnyObjectMembershipCount;

    private int dynamicAnyObjectMembershipCount;

    private final Map<String, String> adynMembershipConds = new HashMap<>();

    private final List<TypeExtensionTO> typeExtensions = new ArrayList<>();

    @JacksonXmlProperty(localName = "_class", isAttribute = true)
    @JsonProperty("_class")
    @Schema(name = "_class", required = true, example = "org.apache.syncope.common.lib.to.GroupTO")
    @Override
    public String getDiscriminator() {
        return getClass().getName();
    }

    @Override
    public String getType() {
        return AnyTypeKind.GROUP.name();
    }

    @Override
    public void setType(final String type) {
        // fixed
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getUserOwner() {
        return userOwner;
    }

    public void setUserOwner(final String userOwner) {
        this.userOwner = userOwner;
    }

    public String getGroupOwner() {
        return groupOwner;
    }

    public void setGroupOwner(final String groupOwner) {
        this.groupOwner = groupOwner;
    }

    public String getUDynMembershipCond() {
        return udynMembershipCond;
    }

    public void setUDynMembershipCond(final String uDynMembershipCond) {
        this.udynMembershipCond = uDynMembershipCond;
    }

    public int getStaticUserMembershipCount() {
        return staticUserMembershipCount;
    }

    public void setStaticUserMembershipCount(final int staticUserMembershipCount) {
        this.staticUserMembershipCount = staticUserMembershipCount;
    }

    public int getDynamicUserMembershipCount() {
        return dynamicUserMembershipCount;
    }

    public void setDynamicUserMembershipCount(final int dynamicUserMembershipCount) {
        this.dynamicUserMembershipCount = dynamicUserMembershipCount;
    }

    public int getStaticAnyObjectMembershipCount() {
        return staticAnyObjectMembershipCount;
    }

    public void setStaticAnyObjectMembershipCount(final int staticAnyObjectMembershipCount) {
        this.staticAnyObjectMembershipCount = staticAnyObjectMembershipCount;
    }

    public int getDynamicAnyObjectMembershipCount() {
        return dynamicAnyObjectMembershipCount;
    }

    public void setDynamicAnyObjectMembershipCount(final int dynamicAnyObjectMembershipCount) {
        this.dynamicAnyObjectMembershipCount = dynamicAnyObjectMembershipCount;
    }

    public Map<String, String> getADynMembershipConds() {
        return adynMembershipConds;
    }

    @JsonIgnore
    public Optional<TypeExtensionTO> getTypeExtension(final String anyType) {
        return typeExtensions.stream().filter(
                typeExtension -> anyType != null && anyType.equals(typeExtension.getAnyType())).
                findFirst();
    }

    @JacksonXmlElementWrapper(localName = "typeExtensions")
    @JacksonXmlProperty(localName = "typeExtension")
    public List<TypeExtensionTO> getTypeExtensions() {
        return typeExtensions;
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
        final GroupTO other = (GroupTO) obj;
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

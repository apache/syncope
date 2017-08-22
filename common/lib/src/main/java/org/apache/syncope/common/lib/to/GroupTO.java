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
import org.apache.syncope.common.lib.jaxb.XmlGenericMapAdapter;
import org.apache.syncope.common.lib.types.AnyTypeKind;

@XmlRootElement(name = "group")
@XmlType
public class GroupTO extends AnyTO {

    private static final long serialVersionUID = -7785920258290147542L;

    private String name;

    private String userOwner;

    private String groupOwner;

    private String udynMembershipCond;

    @XmlJavaTypeAdapter(XmlGenericMapAdapter.class)
    @JsonIgnore
    private final Map<String, String> adynMembershipConds = new HashMap<>();

    private final List<TypeExtensionTO> typeExtensions = new ArrayList<>();

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

    @JsonProperty
    public Map<String, String> getADynMembershipConds() {
        return adynMembershipConds;
    }

    @JsonIgnore
    public Optional<TypeExtensionTO> getTypeExtension(final String anyType) {
        return typeExtensions.stream().filter(
                typeExtension -> anyType != null && anyType.equals(typeExtension.getAnyType())).
                findFirst();
    }

    @XmlElementWrapper(name = "typeExtensions")
    @XmlElement(name = "typeExtension")
    @JsonProperty("typeExtensions")
    public List<TypeExtensionTO> getTypeExtensions() {
        return typeExtensions;
    }

}

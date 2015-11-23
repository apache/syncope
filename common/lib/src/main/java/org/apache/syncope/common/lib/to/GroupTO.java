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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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

    private Long userOwner;

    private Long groupOwner;

    private String adynMembershipCond;

    private String udynMembershipCond;

    @XmlJavaTypeAdapter(XmlGenericMapAdapter.class)
    @JsonIgnore
    private final Map<String, Set<String>> typeExtensions = new HashMap<>();

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

    public Long getUserOwner() {
        return userOwner;
    }

    public void setUserOwner(final Long userOwner) {
        this.userOwner = userOwner;
    }

    public Long getGroupOwner() {
        return groupOwner;
    }

    public void setGroupOwner(final Long groupOwner) {
        this.groupOwner = groupOwner;
    }

    public String getADynMembershipCond() {
        return adynMembershipCond;
    }

    public void setADynMembershipCond(final String aDynMembershipCond) {
        this.adynMembershipCond = aDynMembershipCond;
    }

    public String getUDynMembershipCond() {
        return udynMembershipCond;
    }

    public void setUDynMembershipCond(final String uDynMembershipCond) {
        this.udynMembershipCond = uDynMembershipCond;
    }

    @JsonProperty
    public Map<String, Set<String>> getTypeExtensions() {
        return typeExtensions;
    }

    public static long fromDisplayName(final String displayName) {
        long result = 0;
        if (displayName != null && !displayName.isEmpty() && displayName.indexOf(' ') != -1) {
            try {
                result = Long.valueOf(displayName.split(" ")[0]);
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        return result;
    }
}

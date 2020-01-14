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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.PathParam;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@XmlRootElement(name = "role")
@XmlType
public class RoleTO implements EntityTO {

    private static final long serialVersionUID = 4560822655754800031L;

    private String key;

    private final Set<String> entitlements = new HashSet<>();

    private final List<String> realms = new ArrayList<>();

    private final List<String> dynRealms = new ArrayList<>();

    private String dynMembershipCond;

    private final Set<String> privileges = new HashSet<>();

    @Override
    public String getKey() {
        return key;
    }

    @PathParam("key")
    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    @XmlElementWrapper(name = "entitlements")
    @XmlElement(name = "entitlement")
    @JsonProperty("entitlements")
    public Set<String> getEntitlements() {
        return entitlements;
    }

    @XmlElementWrapper(name = "realms")
    @XmlElement(name = "realm")
    @JsonProperty("realms")
    public List<String> getRealms() {
        return realms;
    }

    @XmlElementWrapper(name = "dynRealms")
    @XmlElement(name = "dynRealm")
    @JsonProperty("dynRealms")
    public List<String> getDynRealms() {
        return dynRealms;
    }

    public String getDynMembershipCond() {
        return dynMembershipCond;
    }

    public void setDynMembershipCond(final String dynMembershipCond) {
        this.dynMembershipCond = dynMembershipCond;
    }

    @XmlElementWrapper(name = "privileges")
    @XmlElement(name = "privilege")
    @JsonProperty("privileges")
    public Set<String> getPrivileges() {
        return privileges;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        RoleTO roleTO = (RoleTO) o;

        return new EqualsBuilder().
                append(key, roleTO.key).
                append(entitlements, roleTO.entitlements).
                append(realms, roleTO.realms).
                append(dynRealms, roleTO.dynRealms).
                append(dynMembershipCond, roleTO.dynMembershipCond).
                append(privileges, roleTO.privileges).
                isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(key).
                append(entitlements).
                append(realms).
                append(dynRealms).
                append(dynMembershipCond).
                append(privileges).
                toHashCode();
    }
}

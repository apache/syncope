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
import org.apache.syncope.common.lib.AbstractBaseBean;

@XmlRootElement(name = "role")
@XmlType
public class RoleTO extends AbstractBaseBean implements EntityTO {

    private static final long serialVersionUID = 4560822655754800031L;

    private String key;

    private final Set<String> entitlements = new HashSet<>();

    private final List<String> realms = new ArrayList<>();

    private String dynMembershipCond;

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

    public String getDynMembershipCond() {
        return dynMembershipCond;
    }

    public void setDynMembershipCond(final String dynMembershipCond) {
        this.dynMembershipCond = dynMembershipCond;
    }

}

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
package org.apache.syncope.mod;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.codehaus.jackson.annotate.JsonIgnore;

@XmlRootElement
@XmlType
public class UserMod extends AbstractAttributableMod {

    private static final long serialVersionUID = 3081848906558106204L;

    private String password;

    private String username;

    private final Set<MembershipMod> membershipsToBeAdded = new HashSet<MembershipMod>();

    private final Set<Long> membershipsToBeRemoved = new HashSet<Long>();

    public boolean addMembershipToBeAdded(MembershipMod membershipMod) {
        return membershipsToBeAdded.add(membershipMod);
    }

    public boolean removeMembershipToBeAdded(MembershipMod membershipMod) {
        return membershipsToBeAdded.remove(membershipMod);
    }

    @XmlElement(name = "membershipMod")
    @XmlElementWrapper(name = "membershipsToBeAdded")
    public Set<MembershipMod> getMembershipsToBeAdded() {
        return membershipsToBeAdded;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean addMembershipToBeRemoved(Long membershipToBeRemoved) {
        return membershipsToBeRemoved.add(membershipToBeRemoved);
    }

    public boolean removeMembershipToBeRemoved(Long membershipToBeRemoved) {
        return membershipsToBeRemoved.remove(membershipToBeRemoved);
    }

    @XmlElement(name = "membershipMod")
    @XmlElementWrapper(name = "membershipsToBeRemoved")
    public Set<Long> getMembershipsToBeRemoved() {
        return membershipsToBeRemoved;
    }

    @JsonIgnore
    @Override
    public boolean isEmpty() {
        return super.isEmpty() && password == null && username == null && membershipsToBeAdded.isEmpty()
                && membershipsToBeRemoved.isEmpty();
    }
}

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
package org.apache.syncope.common.mod;

import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.to.PropagationRequestTO;
import org.codehaus.jackson.annotate.JsonIgnore;

@XmlRootElement(name = "userMod")
@XmlType
public class UserMod extends AbstractAttributableMod {

    private static final long serialVersionUID = 3081848906558106204L;

    private String password;

    private String username;

    private final Set<MembershipMod> membershipsToBeAdded;

    private final Set<Long> membershipsToBeRemoved;

    private PropagationRequestTO pwdPropRequest;

    public UserMod() {
        super();

        membershipsToBeAdded = new HashSet<MembershipMod>();
        membershipsToBeRemoved = new HashSet<Long>();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    @XmlElementWrapper(name = "membershipsToBeAdded")
    @XmlElement(name = "membership")
    public Set<MembershipMod> getMembershipsToBeAdded() {
        return membershipsToBeAdded;
    }

    public boolean addMembershipToBeAdded(final MembershipMod membershipMod) {
        return membershipsToBeAdded.add(membershipMod);
    }

    public boolean removeMembershipToBeAdded(final MembershipMod membershipMod) {
        return membershipsToBeAdded.remove(membershipMod);
    }

    public void setMembershipsToBeAdded(final Set<MembershipMod> membershipsToBeAdded) {
        if (this.membershipsToBeAdded != membershipsToBeAdded) {
            this.membershipsToBeAdded.clear();
            if (membershipsToBeAdded != null && !membershipsToBeAdded.isEmpty()) {
                this.membershipsToBeAdded.addAll(membershipsToBeAdded);
            }
        }
    }

    @XmlElementWrapper(name = "membershipsToBeRemoved")
    @XmlElement(name = "membership")
    public Set<Long> getMembershipsToBeRemoved() {
        return membershipsToBeRemoved;
    }

    public boolean addMembershipToBeRemoved(final Long membershipToBeRemoved) {
        return membershipsToBeRemoved.add(membershipToBeRemoved);
    }

    public boolean removeMembershipToBeRemoved(final Long membershipToBeRemoved) {
        return membershipsToBeRemoved.remove(membershipToBeRemoved);
    }

    public void setMembershipsToBeRemoved(final Set<Long> membershipsToBeRemoved) {
        if (this.membershipsToBeRemoved != membershipsToBeRemoved) {
            this.membershipsToBeRemoved.clear();
            if (membershipsToBeRemoved != null && !membershipsToBeRemoved.isEmpty()) {
                this.membershipsToBeRemoved.addAll(membershipsToBeRemoved);
            }
        }
    }

    public PropagationRequestTO getPwdPropRequest() {
        return pwdPropRequest;
    }

    public void setPwdPropRequest(final PropagationRequestTO pwdPropRequest) {
        this.pwdPropRequest = pwdPropRequest;
    }

    @JsonIgnore
    @Override
    public boolean isEmpty() {
        return super.isEmpty()
                && password == null
                && username == null
                && membershipsToBeAdded.isEmpty()
                && membershipsToBeRemoved.isEmpty()
                && pwdPropRequest == null;
    }
}

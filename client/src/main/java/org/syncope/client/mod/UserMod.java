/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.client.mod;

import java.util.HashSet;
import java.util.Set;
import org.codehaus.jackson.annotate.JsonIgnore;

public class UserMod extends AbstractAttributableMod {

    private static final long serialVersionUID = 3081848906558106204L;

    private String password;

    private String username;

    private Set<MembershipMod> membershipsToBeAddeded;

    private Set<Long> membershipsToBeRemoved;

    public UserMod() {
        super();

        membershipsToBeAddeded = new HashSet<MembershipMod>();
        membershipsToBeRemoved = new HashSet<Long>();
    }

    public boolean addMembershipToBeAdded(MembershipMod membershipMod) {
        return membershipsToBeAddeded.add(membershipMod);
    }

    public boolean removeMembershipToBeAdded(MembershipMod membershipMod) {
        return membershipsToBeAddeded.remove(membershipMod);
    }

    public Set<MembershipMod> getMembershipsToBeAdded() {
        return membershipsToBeAddeded;
    }

    public void setMembershipsToBeAdded(Set<MembershipMod> membershipMods) {
        this.membershipsToBeAddeded = membershipMods;
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

    public Set<Long> getMembershipsToBeRemoved() {
        return membershipsToBeRemoved;
    }

    public void setMembershipsToBeRemoved(Set<Long> membershipsToBeRemoved) {
        this.membershipsToBeRemoved = membershipsToBeRemoved;
    }

    @JsonIgnore
    @Override
    public boolean isEmpty() {
        return super.isEmpty()
                && password == null
                && username == null
                && membershipsToBeAddeded.isEmpty()
                && membershipsToBeRemoved.isEmpty();
    }
}

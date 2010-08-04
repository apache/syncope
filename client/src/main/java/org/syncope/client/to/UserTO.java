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
package org.syncope.client.to;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.syncope.client.mod.MembershipMod;
import org.syncope.client.mod.UserMod;

public class UserTO extends AbstractAttributableTO {

    private String password;
    private Set<MembershipTO> memberships;
    private Date creationTime;
    private String token;
    private Date tokenExpireTime;

    public UserTO() {
        super();

        memberships = new HashSet<MembershipTO>();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean addMembership(MembershipTO membershipTO) {
        return memberships.add(membershipTO);
    }

    public boolean removeMembership(MembershipTO membershipTO) {
        return memberships.remove(membershipTO);
    }

    public Set<MembershipTO> getMemberships() {
        return memberships;
    }

    public void setMemberships(Set<MembershipTO> memberships) {
        this.memberships = memberships;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Date getTokenExpireTime() {
        return tokenExpireTime;
    }

    public void setTokenExpireTime(Date tokenExpireTime) {
        this.tokenExpireTime = tokenExpireTime;
    }

    public UserMod buildUserMod() {
        UserMod userMod = new UserMod();
        userMod.setPassword(password);
        userMod = fillAbstractAttributableMod(userMod);

        MembershipMod membershipMod = null;
        for (MembershipTO membershipTO : memberships) {
            membershipMod = new MembershipMod();
            membershipMod.setRole(membershipTO.getRole());
            membershipMod =
                    membershipTO.fillAbstractAttributableMod(membershipMod);

            userMod.addMembershipMod(membershipMod);
        }

        return userMod;
    }
}

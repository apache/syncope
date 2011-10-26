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
package org.syncope.core.persistence.relationships;

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.membership.Membership;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.MembershipDAO;
import org.syncope.core.AbstractTest;
import org.syncope.core.persistence.dao.RoleDAO;

@Transactional
public class MembershipTest extends AbstractTest {

    @Autowired
    private MembershipDAO membershipDAO;

    @Autowired
    private RoleDAO syncopeRoleDAO;

    @Test
    public final void delete() {
        Membership membership = membershipDAO.find(4L);
        SyncopeUser user = membership.getSyncopeUser();
        SyncopeRole role = membership.getSyncopeRole();

        membershipDAO.delete(4L);

        membershipDAO.flush();

        for (Membership m : user.getMemberships()) {
            assertTrue(m.getId() != 4L);
        }
        for (Membership m : syncopeRoleDAO.findMemberships(role)) {
            assertTrue(m.getId() != 4L);
        }
    }

    @Test
    public final void deleteAndCreate() {
        Membership membership = membershipDAO.find(3L);
        SyncopeUser user = membership.getSyncopeUser();
        SyncopeRole role = membership.getSyncopeRole();

        // 1. delete that membership
        membershipDAO.delete(membership.getId());

        // if not flushing here, the INSERT below will be executed
        // before the DELETE above
        membershipDAO.flush();

        // 2. (in the same transaction) create new membership with same user
        // and role (in order to check the UNIQE constraint on Membership)
        membership = new Membership();
        membership.setSyncopeUser(user);
        membership.setSyncopeRole(role);

        membership = membershipDAO.save(membership);
    }
}

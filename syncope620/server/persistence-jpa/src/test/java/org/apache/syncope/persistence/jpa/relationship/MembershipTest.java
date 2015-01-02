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
package org.apache.syncope.persistence.jpa.relationship;

import static org.junit.Assert.assertTrue;

import org.apache.syncope.persistence.api.dao.MembershipDAO;
import org.apache.syncope.persistence.api.dao.RoleDAO;
import org.apache.syncope.persistence.api.entity.membership.Membership;
import org.apache.syncope.persistence.api.entity.role.Role;
import org.apache.syncope.persistence.api.entity.user.User;
import org.apache.syncope.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class MembershipTest extends AbstractTest {

    @Autowired
    private MembershipDAO membershipDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Test
    public void delete() {
        Membership membership = membershipDAO.find(4L);
        User user = membership.getUser();
        Role role = membership.getRole();

        membershipDAO.delete(4L);

        membershipDAO.flush();

        for (Membership m : user.getMemberships()) {
            assertTrue(m.getKey() != 4L);
        }
        for (Membership m : roleDAO.findMemberships(role)) {
            assertTrue(m.getKey() != 4L);
        }
    }

    @Test
    public void deleteAndCreate() {
        Membership membership = membershipDAO.find(3L);
        User user = membership.getUser();
        Role role = membership.getRole();

        // 1. delete that membership
        membershipDAO.delete(membership.getKey());

        // if not flushing here, the INSERT below will be executed
        // before the DELETE above
        membershipDAO.flush();

        // 2. (in the same transaction) create new membership with same user
        // and role (in order to check the UNIQE constraint on Membership)
        membership = entityFactory.newEntity(Membership.class);
        membership.setUser(user);
        membership.setRole(role);

        membership = membershipDAO.save(membership);
    }
}

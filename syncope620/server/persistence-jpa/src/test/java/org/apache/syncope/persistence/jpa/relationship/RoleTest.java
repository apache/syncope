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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.apache.syncope.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.persistence.api.dao.EntitlementDAO;
import org.apache.syncope.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.persistence.api.dao.PlainAttrValueDAO;
import org.apache.syncope.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.persistence.api.dao.PolicyDAO;
import org.apache.syncope.persistence.api.dao.RoleDAO;
import org.apache.syncope.persistence.api.dao.UserDAO;
import org.apache.syncope.persistence.api.entity.PasswordPolicy;
import org.apache.syncope.persistence.api.entity.role.RPlainAttr;
import org.apache.syncope.persistence.api.entity.role.RPlainAttrValue;
import org.apache.syncope.persistence.api.entity.role.RPlainSchema;
import org.apache.syncope.persistence.api.entity.role.Role;
import org.apache.syncope.persistence.api.entity.user.User;
import org.apache.syncope.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class RoleTest extends AbstractTest {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private PlainAttrDAO plainAttrDAO;

    @Autowired
    private PlainAttrValueDAO plainAttrValueDAO;

    @Autowired
    private EntitlementDAO entitlementDAO;

    @Autowired
    private PolicyDAO policyDAO;

    @Test(expected = InvalidEntityException.class)
    public void saveWithTwoOwners() {
        Role root = roleDAO.find("root", null);
        assertNotNull("did not find expected role", root);

        User user = userDAO.find(1L);
        assertNotNull("did not find expected user", user);

        Role role = entityFactory.newEntity(Role.class);
        role.setName("error");
        role.setUserOwner(user);
        role.setRoleOwner(root);

        roleDAO.save(role);
    }

    @Test
    public void findByOwner() {
        Role role = roleDAO.find(6L);
        assertNotNull("did not find expected role", role);

        User user = userDAO.find(5L);
        assertNotNull("did not find expected user", user);

        assertEquals(user, role.getUserOwner());

        Role child1 = roleDAO.find(7L);
        assertNotNull(child1);
        assertEquals(role, child1.getParent());

        Role child2 = roleDAO.find(10L);
        assertNotNull(child2);
        assertEquals(role, child2.getParent());

        List<Role> ownedRoles = roleDAO.findOwnedByUser(user.getKey());
        assertFalse(ownedRoles.isEmpty());
        assertEquals(2, ownedRoles.size());
        assertTrue(ownedRoles.contains(role));
        assertTrue(ownedRoles.contains(child1));
        assertFalse(ownedRoles.contains(child2));
    }

    public void createWithPasswordPolicy() {
        PasswordPolicy policy = (PasswordPolicy) policyDAO.find(4L);
        Role role = entityFactory.newEntity(Role.class);
        role.setName("roleWithPasswordPolicy");
        role.setPasswordPolicy(policy);

        Role actual = roleDAO.save(role);
        assertNotNull(actual);

        actual = roleDAO.find(actual.getKey());
        assertNotNull(actual);
        assertNotNull(actual.getPasswordPolicy());

        roleDAO.delete(actual.getKey());
        assertNull(roleDAO.find(actual.getKey()));

        assertNotNull(policyDAO.find(4L));
    }

    @Test
    public void delete() {
        roleDAO.delete(2L);

        roleDAO.flush();

        assertNull(roleDAO.find(2L));
        assertEquals(1, roleDAO.findByEntitlement(entitlementDAO.find("base")).size());
        assertEquals(userDAO.find(2L).getRoles().size(), 2);
        assertNull(plainAttrDAO.find(700L, RPlainAttr.class));
        assertNull(plainAttrValueDAO.find(41L, RPlainAttrValue.class));
        assertNotNull(plainSchemaDAO.find("icon", RPlainSchema.class));
    }
}

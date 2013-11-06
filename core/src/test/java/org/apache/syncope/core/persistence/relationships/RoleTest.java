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
package org.apache.syncope.core.persistence.relationships;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.syncope.core.persistence.beans.PasswordPolicy;
import org.apache.syncope.core.persistence.beans.role.RAttr;
import org.apache.syncope.core.persistence.beans.role.RAttrValue;
import org.apache.syncope.core.persistence.beans.role.RSchema;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.core.persistence.dao.AbstractDAOTest;
import org.apache.syncope.core.persistence.dao.AttrDAO;
import org.apache.syncope.core.persistence.dao.AttrValueDAO;
import org.apache.syncope.core.persistence.dao.EntitlementDAO;
import org.apache.syncope.core.persistence.dao.PolicyDAO;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.persistence.dao.SchemaDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.persistence.validation.entity.InvalidEntityException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class RoleTest extends AbstractDAOTest {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private SchemaDAO schemaDAO;

    @Autowired
    private AttrDAO attrDAO;

    @Autowired
    private AttrValueDAO attrValueDAO;

    @Autowired
    private EntitlementDAO entitlementDAO;

    @Autowired
    private PolicyDAO policyDAO;

    @Test(expected = InvalidEntityException.class)
    public void saveWithTwoOwners() {
        SyncopeRole root = roleDAO.find("root", null);
        assertNotNull("did not find expected role", root);

        SyncopeUser user = userDAO.find(1L);
        assertNotNull("did not find expected user", user);

        SyncopeRole role = new SyncopeRole();
        role.setName("error");
        role.setUserOwner(user);
        role.setRoleOwner(root);

        roleDAO.save(role);
    }

    @Test
    public void findByOwner() {
        SyncopeRole role = roleDAO.find(6L);
        assertNotNull("did not find expected role", role);

        SyncopeUser user = userDAO.find(5L);
        assertNotNull("did not find expected user", user);

        assertEquals(user, role.getUserOwner());

        SyncopeRole child1 = roleDAO.find(7L);
        assertNotNull(child1);
        assertEquals(role, child1.getParent());

        SyncopeRole child2 = roleDAO.find(10L);
        assertNotNull(child2);
        assertEquals(role, child2.getParent());

        List<SyncopeRole> ownedRoles = roleDAO.findOwnedByUser(user.getId());
        assertFalse(ownedRoles.isEmpty());
        assertEquals(2, ownedRoles.size());
        assertTrue(ownedRoles.contains(role));
        assertTrue(ownedRoles.contains(child1));
        assertFalse(ownedRoles.contains(child2));
    }

    public void createWithPasswordPolicy() {
        PasswordPolicy policy = (PasswordPolicy) policyDAO.find(4L);
        SyncopeRole role = new SyncopeRole();
        role.setName("roleWithPasswordPolicy");
        role.setPasswordPolicy(policy);

        SyncopeRole actual = roleDAO.save(role);
        assertNotNull(actual);

        actual = roleDAO.find(actual.getId());
        assertNotNull(actual);
        assertNotNull(actual.getPasswordPolicy());

        roleDAO.delete(actual.getId());
        assertNull(roleDAO.find(actual.getId()));

        assertNotNull(policyDAO.find(4L));
    }

    @Test
    public void delete() {
        roleDAO.delete(2L);

        roleDAO.flush();

        assertNull(roleDAO.find(2L));
        assertEquals(1, roleDAO.findByEntitlement(entitlementDAO.find("base")).size());
        assertEquals(userDAO.find(2L).getRoles().size(), 2);
        assertNull(attrDAO.find(700L, RAttr.class));
        assertNull(attrValueDAO.find(41L, RAttrValue.class));
        assertNotNull(schemaDAO.find("icon", RSchema.class));
    }
}

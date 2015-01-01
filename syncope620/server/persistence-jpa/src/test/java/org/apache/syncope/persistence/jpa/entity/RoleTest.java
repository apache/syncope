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
package org.apache.syncope.persistence.jpa.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import org.apache.syncope.persistence.api.dao.PolicyDAO;
import org.apache.syncope.persistence.api.dao.RoleDAO;
import org.apache.syncope.persistence.api.entity.AccountPolicy;
import org.apache.syncope.persistence.api.entity.PasswordPolicy;
import org.apache.syncope.persistence.api.entity.role.Role;
import org.apache.syncope.persistence.jpa.AbstractTest;
import org.apache.syncope.persistence.jpa.entity.role.JPARole;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class RoleTest extends AbstractTest {

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private PolicyDAO policyDAO;

    @Test
    public void findAll() {
        List<Role> list = roleDAO.findAll();
        assertEquals("did not get expected number of roles ", 14, list.size());
    }

    @Test
    public void findChildren() {
        assertEquals(3, roleDAO.findChildren(roleDAO.find(4L)).size());
    }

    @Test
    public void find() {
        Role role = roleDAO.find("root", null);
        assertNotNull("did not find expected role", role);
        role = roleDAO.find(null, null);
        assertNull("found role but did not expect it", role);
    }

    @Test
    public void inheritedAttributes() {
        Role director = roleDAO.find(7L);

        assertEquals(1, director.findLastInheritedAncestorPlainAttrs().size());
    }

    @Test
    public void inheritedDerivedAttributes() {
        Role director = roleDAO.find(7L);

        assertEquals(1, director.findLastInheritedAncestorDerAttrs().size());
    }

    @Test
    public void inheritedVirtualAttributes() {
        Role director = roleDAO.find(7L);

        assertEquals(1, director.findLastInheritedAncestorVirAttrs().size());
    }

    @Test
    public void inheritedPolicy() {
        Role role = roleDAO.find(7L);

        assertNotNull(role);

        assertNotNull(role.getAccountPolicy());
        assertNotNull(role.getPasswordPolicy());

        assertEquals(4, role.getPasswordPolicy().getKey(), 0);

        role = roleDAO.find(5L);

        assertNotNull(role);

        assertNull(role.getAccountPolicy());
        assertNull(role.getPasswordPolicy());
    }

    @Test
    public void save() {
        Role role = new JPARole();
        role.setName("secondChild");

        // verify inheritance password and account policies
        role.setInheritAccountPolicy(false);
        // not inherited so setter execution shouldn't be ignored
        role.setAccountPolicy((AccountPolicy) policyDAO.find(6L));

        role.setInheritPasswordPolicy(true);
        // inherited so setter execution should be ignored
        role.setPasswordPolicy((PasswordPolicy) policyDAO.find(4L));

        Role rootRole = roleDAO.find("root", null);
        role.setParent(rootRole);

        role = roleDAO.save(role);

        Role actual = roleDAO.find(role.getKey());
        assertNotNull("expected save to work", actual);

        assertNull(role.getPasswordPolicy());
        assertNotNull(role.getAccountPolicy());
        assertEquals(Long.valueOf(6), role.getAccountPolicy().getKey());
    }

    @Test
    public void delete() {
        Role role = roleDAO.find(4L);
        roleDAO.delete(role.getKey());

        Role actual = roleDAO.find(4L);
        assertNull("delete did not work", actual);

        Role children = roleDAO.find(7L);
        assertNull("delete of successors did not work", children);
    }
}

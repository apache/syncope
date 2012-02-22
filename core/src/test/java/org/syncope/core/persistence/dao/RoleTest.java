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
package org.syncope.core.persistence.dao;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.AbstractTest;
import org.syncope.core.persistence.beans.AccountPolicy;
import org.syncope.core.persistence.beans.PasswordPolicy;

@Transactional
public class RoleTest extends AbstractTest {

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private PolicyDAO policyDAO;

    @Test
    public void findAll() {
        List<SyncopeRole> list = roleDAO.findAll();
        assertEquals("did not get expected number of roles ", 9, list.size());
    }

    @Test
    public void findChildren() {
        assertEquals(2, roleDAO.findChildren(4L).size());
    }

    @Test
    public void find() {
        SyncopeRole role = roleDAO.find("root", null);
        assertNotNull("did not find expected role", role);
        role = roleDAO.find(null, null);
        assertNull("found role but did not expect it", role);
    }

    @Test
    public void inheritedAttributes() {
        SyncopeRole director = roleDAO.find(7L);

        assertEquals(1, director.findInheritedAttributes().size());
    }

    @Test
    public void inheritedDerivedAttributes() {
        SyncopeRole director = roleDAO.find(7L);

        assertEquals(1, director.findInheritedDerivedAttributes().size());
    }

    @Test
    public void inheritedVirtualAttributes() {
        SyncopeRole director = roleDAO.find(7L);

        assertEquals(1, director.findInheritedVirtualAttributes().size());
    }

    @Test
    public void inheritedPolicy() {
        SyncopeRole role = roleDAO.find(7L);

        assertNotNull(role);

        assertNotNull(role.getAccountPolicy());
        assertNotNull(role.getPasswordPolicy());

        assertEquals(Long.valueOf(4), role.getPasswordPolicy().getId());

        role = roleDAO.find(5L);

        assertNotNull(role);

        assertNull(role.getAccountPolicy());
        assertNull(role.getPasswordPolicy());
    }

    @Test
    public void save() {
        SyncopeRole role = new SyncopeRole();
        role.setName("secondChild");

        // verify inheritance password and account policies
        role.setInheritAccountPolicy(false);
        // not inherited so setter execution shouldn't be ignored
        role.setAccountPolicy((AccountPolicy) policyDAO.find(6L));

        role.setInheritPasswordPolicy(true);
        // inherited so setter execution should be ignored
        role.setPasswordPolicy((PasswordPolicy) policyDAO.find(4L));

        SyncopeRole rootRole = roleDAO.find("root", null);
        role.setParent(rootRole);

        role = roleDAO.save(role);

        SyncopeRole actual = roleDAO.find(role.getId());
        assertNotNull("expected save to work", actual);

        assertNull(role.getPasswordPolicy());
        assertNotNull(role.getAccountPolicy());
        assertEquals(Long.valueOf(6), role.getAccountPolicy().getId());
    }

    @Test
    public void delete() {
        SyncopeRole role = roleDAO.find(4L);
        roleDAO.delete(role.getId());

        SyncopeRole actual = roleDAO.find(4L);
        assertNull("delete did not work", actual);

        SyncopeRole children = roleDAO.find(7L);
        assertNull("delete of successors did not work", children);
    }
}

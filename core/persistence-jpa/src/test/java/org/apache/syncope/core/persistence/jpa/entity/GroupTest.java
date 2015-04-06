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
package org.apache.syncope.core.persistence.jpa.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.entity.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class GroupTest extends AbstractTest {

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private PolicyDAO policyDAO;

    @Test
    public void findAll() {
        List<Group> list = groupDAO.findAll();
        assertEquals("did not get expected number of groups ", 14, list.size());
    }

    @Test
    public void findChildren() {
        assertEquals(3, groupDAO.findChildren(groupDAO.find(4L)).size());
    }

    @Test
    public void find() {
        Group group = groupDAO.find("root", null);
        assertNotNull("did not find expected group", group);
        group = groupDAO.find(null, null);
        assertNull("found group but did not expect it", group);
    }

    @Test
    public void inheritedAttributes() {
        Group director = groupDAO.find(7L);

        assertEquals(1, director.findLastInheritedAncestorPlainAttrs().size());
    }

    @Test
    public void inheritedDerivedAttributes() {
        Group director = groupDAO.find(7L);

        assertEquals(1, director.findLastInheritedAncestorDerAttrs().size());
    }

    @Test
    public void inheritedVirtualAttributes() {
        Group director = groupDAO.find(7L);

        assertEquals(1, director.findLastInheritedAncestorVirAttrs().size());
    }

    @Test
    public void inheritedPolicy() {
        Group group = groupDAO.find(7L);
        assertNotNull(group);

        assertNotNull(group.getAccountPolicy());
        assertNotNull(group.getPasswordPolicy());

        assertEquals(4, group.getPasswordPolicy().getKey(), 0);

        group = groupDAO.find(5L);

        assertNotNull(group);

        assertNull(group.getAccountPolicy());
        assertNull(group.getPasswordPolicy());
    }

    @Test
    public void save() {
        Group group = entityFactory.newEntity(Group.class);
        group.setName("secondChild");

        // verify inheritance password and account policies
        group.setInheritAccountPolicy(false);
        // not inherited so setter execution shouldn't be ignored
        group.setAccountPolicy((AccountPolicy) policyDAO.find(6L));

        group.setInheritPasswordPolicy(true);
        // inherited so setter execution should be ignored
        group.setPasswordPolicy((PasswordPolicy) policyDAO.find(4L));

        Group rootGroup = groupDAO.find("root", null);
        group.setParent(rootGroup);

        group = groupDAO.save(group);

        Group actual = groupDAO.find(group.getKey());
        assertNotNull("expected save to work", actual);

        assertNull(group.getPasswordPolicy());
        assertNotNull(group.getAccountPolicy());
        assertEquals(Long.valueOf(6), group.getAccountPolicy().getKey());
    }

    @Test
    public void delete() {
        Group group = groupDAO.find(4L);
        groupDAO.delete(group.getKey());

        Group actual = groupDAO.find(4L);
        assertNull("delete did not work", actual);

        Group children = groupDAO.find(7L);
        assertNull("delete of successors did not work", children);
    }
}

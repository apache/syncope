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
package org.apache.syncope.core.persistence.neo4j.inner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.neo4j.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class GroupTest extends AbstractTest {

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private RealmSearchDAO realmSearchDAO;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Test
    public void findAll() {
        List<? extends Group> groups = groupDAO.findAll();
        assertEquals(16, groups.size());
    }

    @Test
    public void find() {
        Group group = groupDAO.findByName("additional").orElseThrow();
        assertNotNull(group);
        assertEquals(1, group.getTypeExtensions().size());
        assertEquals(2, group.getTypeExtension(anyTypeDAO.getUser()).get().getAuxClasses().size());
    }

    @Test
    public void findKeysByNamePattern() {
        List<String> groups = groupDAO.findKeysByNamePattern(".*child");
        assertEquals(2, groups.size());
        assertTrue(groups.contains("b1f7c12d-ec83-441f-a50e-1691daaedf3b"));
        assertTrue(groups.contains("f779c0d4-633b-4be5-8f57-32eb478a3ca5"));
    }

    @Test
    public void save() {
        Group group = entityFactory.newEntity(Group.class);
        group.setName("secondChild");
        group.setRealm(realmSearchDAO.findByFullPath(SyncopeConstants.ROOT_REALM).orElseThrow());

        group = groupDAO.save(group);

        assertTrue(groupDAO.findById(group.getKey()).isPresent());
    }

    @Test
    public void delete() {
        Group group = groupDAO.findById("8fb2d51e-c605-4e80-a72b-13ffecf1aa9a").orElseThrow();
        groupDAO.deleteById(group.getKey());

        assertTrue(groupDAO.findById("8fb2d51e-c605-4e80-a72b-13ffecf1aa9a").isEmpty());
    }
}

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
package org.apache.syncope.core.persistence.jpa.outer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class AnyTypeTest extends AbstractTest {

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private AnyTypeClassDAO anyTypeClassDAO;

    @Test
    public void findByClassesContaining() {
        List<AnyType> found = anyTypeDAO.findByClassesContaining(
                anyTypeClassDAO.findById("minimal user").orElseThrow());
        assertEquals(List.of(anyTypeDAO.getUser()), found);

        found = anyTypeDAO.findByClassesContaining(anyTypeClassDAO.findById("other").orElseThrow());
        assertEquals(List.of(anyTypeDAO.getUser()), found);

        found = anyTypeDAO.findByClassesContaining(anyTypeClassDAO.findById("minimal group").orElseThrow());
        assertEquals(List.of(anyTypeDAO.getGroup()), found);

        found = anyTypeDAO.findByClassesContaining(anyTypeClassDAO.findById("minimal printer").orElseThrow());
        assertEquals(List.of(anyTypeDAO.findById("PRINTER").orElseThrow()), found);
    }

    @Test
    public void manyToMany() {
        AnyTypeClass other = anyTypeClassDAO.findById("other").orElseThrow();

        AnyType user = anyTypeDAO.getUser();
        assertTrue(user.getClasses().contains(other));

        AnyType group = anyTypeDAO.getGroup();
        assertFalse(group.getClasses().contains(other));

        group.add(other);
        anyTypeDAO.save(group);

        entityManager.flush();

        user = anyTypeDAO.getUser();
        assertTrue(user.getClasses().contains(other));
        int userClassesBefore = user.getClasses().size();

        group = anyTypeDAO.getGroup();
        assertTrue(group.getClasses().contains(other));
        int groupClassesBefore = group.getClasses().size();

        anyTypeClassDAO.deleteById("other");

        entityManager.flush();

        user = anyTypeDAO.getUser();
        assertEquals(userClassesBefore, user.getClasses().size() + 1);

        group = anyTypeDAO.getGroup();
        assertEquals(groupClassesBefore, group.getClasses().size() + 1);
    }
}

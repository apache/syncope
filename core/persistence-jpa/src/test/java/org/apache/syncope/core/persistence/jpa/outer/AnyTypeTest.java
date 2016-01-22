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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class AnyTypeTest extends AbstractTest {

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private AnyTypeClassDAO anyTypeClassDAO;

    @Test
    public void manyToMany() {
        AnyTypeClass other = anyTypeClassDAO.find("other");
        assertNotNull(other);

        AnyType user = anyTypeDAO.findUser();
        assertTrue(user.getClasses().contains(other));

        AnyType group = anyTypeDAO.findGroup();
        assertFalse(group.getClasses().contains(other));

        group.add(other);
        anyTypeDAO.save(group);

        anyTypeDAO.flush();

        user = anyTypeDAO.findUser();
        assertTrue(user.getClasses().contains(other));
        int userClassesBefore = user.getClasses().size();

        group = anyTypeDAO.findGroup();
        assertTrue(group.getClasses().contains(other));
        int groupClassesBefore = group.getClasses().size();

        anyTypeClassDAO.delete("other");

        anyTypeDAO.flush();

        user = anyTypeDAO.findUser();
        assertEquals(userClassesBefore, user.getClasses().size() + 1);

        group = anyTypeDAO.findGroup();
        assertEquals(groupClassesBefore, group.getClasses().size() + 1);
    }

}

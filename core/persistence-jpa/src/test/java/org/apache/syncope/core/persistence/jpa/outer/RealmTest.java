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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class RealmTest extends AbstractTest {

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Test
    public void test() {
        Realm realm = realmDAO.findByFullPath("/odd");
        assertNotNull(realm);

        // need to remove this group in order to remove the realm, which is otherwise empty
        Group group = groupDAO.findByName("fake");
        assertNotNull(group);
        assertEquals(realm, group.getRealm());
        groupDAO.delete(group);

        Role role = roleDAO.find("User reviewer");
        assertTrue(role.getRealms().contains(realm));

        int beforeSize = role.getRealms().size();

        realmDAO.delete(realm);

        realmDAO.flush();

        role = roleDAO.find("User reviewer");
        assertEquals(beforeSize - 1, role.getRealms().size());
    }
}

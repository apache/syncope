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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.AnyMatchDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.DynRealmCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.persistence.api.entity.DynRealmMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class DynRealmTest extends AbstractTest {

    @Autowired
    private AnyMatchDAO anyMatcher;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private DynRealmDAO dynRealmDAO;

    @Autowired
    private AnySearchDAO searchDAO;

    @Autowired
    private UserDAO userDAO;

    @Test
    public void misc() {
        DynRealm dynRealm = entityFactory.newEntity(DynRealm.class);
        dynRealm.setKey("/name");

        DynRealmMembership memb = entityFactory.newEntity(DynRealmMembership.class);
        memb.setDynRealm(dynRealm);
        memb.setAnyType(anyTypeDAO.findUser());
        memb.setFIQLCond("cool==true");

        dynRealm.add(memb);
        memb.setDynRealm(dynRealm);

        // invalid key (starts with /)
        try {
            dynRealmDAO.save(dynRealm);
            fail("This should not happen");
        } catch (Exception e) {
            assertNotNull(e);
        }

        dynRealm.setKey("name");
        DynRealm actual = dynRealmDAO.saveAndRefreshDynMemberships(dynRealm);
        assertNotNull(actual);

        entityManager().flush();

        DynRealmCond dynRealmCond = new DynRealmCond();
        dynRealmCond.setDynRealm(actual.getKey());
        List<User> matching = searchDAO.search(SearchCond.getLeaf(dynRealmCond), AnyTypeKind.USER);
        assertNotNull(matching);
        assertFalse(matching.isEmpty());

        User user = matching.get(0);
        assertTrue(anyMatcher.matches(user, SearchCond.getLeaf(dynRealmCond)));

        assertTrue(userDAO.findDynRealms(user.getKey()).contains(actual.getKey()));
    }
}

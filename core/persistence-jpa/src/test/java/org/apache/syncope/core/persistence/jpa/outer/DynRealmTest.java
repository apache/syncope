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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.AnyMatchDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
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

@Transactional
public class DynRealmTest extends AbstractTest {

    @Autowired
    private AnyMatchDAO anyMatchDAO;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private DynRealmDAO dynRealmDAO;

    @Autowired
    private AnySearchDAO searchDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private PlainAttrValidationManager plainAttrValidationManager;

    @Test
    public void misc() {
        DynRealm dynRealm = entityFactory.newEntity(DynRealm.class);
        dynRealm.setKey("/name");

        DynRealmMembership memb = entityFactory.newEntity(DynRealmMembership.class);
        memb.setDynRealm(dynRealm);
        memb.setAnyType(anyTypeDAO.getUser());
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

        entityManager.flush();

        DynRealmCond dynRealmCond = new DynRealmCond();
        dynRealmCond.setDynRealm(actual.getKey());
        List<User> matching = searchDAO.search(SearchCond.of(dynRealmCond), AnyTypeKind.USER);
        assertNotNull(matching);
        assertFalse(matching.isEmpty());
        User user = matching.getFirst();
        assertTrue(anyMatchDAO.matches(user, SearchCond.of(dynRealmCond)));

        assertTrue(userDAO.findDynRealms(user.getKey()).contains(actual.getKey()));
    }

    @Test
    public void issueSYNCOPE1806() {
        // 1. create two dyn realms with same condition
        DynRealm realm1 = entityFactory.newEntity(DynRealm.class);
        realm1.setKey("realm1");

        DynRealmMembership memb1 = entityFactory.newEntity(DynRealmMembership.class);
        memb1.setDynRealm(realm1);
        memb1.setAnyType(anyTypeDAO.getUser());
        memb1.setFIQLCond("cool==true");

        realm1.add(memb1);
        memb1.setDynRealm(realm1);

        realm1 = dynRealmDAO.saveAndRefreshDynMemberships(realm1);

        DynRealm realm2 = entityFactory.newEntity(DynRealm.class);
        realm2.setKey("realm2");

        DynRealmMembership memb2 = entityFactory.newEntity(DynRealmMembership.class);
        memb2.setDynRealm(realm2);
        memb2.setAnyType(anyTypeDAO.getUser());
        memb2.setFIQLCond("cool==true");

        realm2.add(memb2);
        memb2.setDynRealm(realm2);

        realm2 = dynRealmDAO.saveAndRefreshDynMemberships(realm2);

        entityManager.flush();

        // 2. verify that dynamic members are the same
        DynRealmCond dynRealmCond1 = new DynRealmCond();
        dynRealmCond1.setDynRealm(realm1.getKey());
        List<User> matching1 = searchDAO.search(SearchCond.of(dynRealmCond1), AnyTypeKind.USER);

        DynRealmCond dynRealmCond2 = new DynRealmCond();
        dynRealmCond2.setDynRealm(realm2.getKey());
        List<User> matching2 = searchDAO.search(SearchCond.of(dynRealmCond2), AnyTypeKind.USER);

        assertEquals(matching1, matching2);
        assertEquals(1, matching1.size());
        assertTrue(matching1.stream().anyMatch(u -> "c9b2dec2-00a7-4855-97c0-d854842b4b24".equals(u.getKey())));

        // 3. update an user to let them become part of both dyn realms        
        anyUtilsFactory.getInstance(AnyTypeKind.USER).addAttr(
                plainAttrValidationManager,
                "823074dc-d280-436d-a7dd-07399fae48ec",
                plainSchemaDAO.findById("cool").orElseThrow(),
                "true");

        entityManager.flush();

        // 4. verify that dynamic members are still the same
        matching1 = searchDAO.search(SearchCond.of(dynRealmCond1), AnyTypeKind.USER);
        matching2 = searchDAO.search(SearchCond.of(dynRealmCond2), AnyTypeKind.USER);
        assertEquals(matching1, matching2);
        assertEquals(2, matching1.size());
        assertTrue(matching1.stream().anyMatch(u -> "c9b2dec2-00a7-4855-97c0-d854842b4b24".equals(u.getKey())));
        assertTrue(matching1.stream().anyMatch(u -> "823074dc-d280-436d-a7dd-07399fae48ec".equals(u.getKey())));
    }
}

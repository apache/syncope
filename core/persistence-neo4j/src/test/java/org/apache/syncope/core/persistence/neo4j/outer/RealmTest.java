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
package org.apache.syncope.core.persistence.neo4j.outer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.neo4j.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class RealmTest extends AbstractTest {

    @Autowired
    private AnyTypeClassDAO anyTypeClassDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private RealmSearchDAO realmSearchDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private ImplementationDAO implementationDAO;

    @Autowired
    private PlainAttrValidationManager validator;

    @Test
    public void plainAttrs() {
        Realm realm = realmSearchDAO.findByFullPath("/odd").orElseThrow();
        assertTrue(realm.getAnyTypeClasses().isEmpty());
        assertTrue(realm.getPlainAttrs().isEmpty());

        realm.add(anyTypeClassDAO.findById("other").orElseThrow());
        realm = realmDAO.save(realm);

        realm = realmDAO.findById(realm.getKey()).orElseThrow();
        assertEquals(anyTypeClassDAO.findById("other").orElseThrow(), realm.getAnyTypeClasses().iterator().next());

        PlainAttr aLong = new PlainAttr();
        aLong.setSchema("aLong");
        aLong.add(validator, "9");
        realm.add(aLong);

        realm = realmDAO.save(realm);

        realm = realmDAO.findById(realm.getKey()).orElseThrow();
        assertEquals(anyTypeClassDAO.findById("other").orElseThrow(), realm.getAnyTypeClasses().iterator().next());
        assertEquals(1, realm.getPlainAttrs().size());
        assertEquals(9, realm.getPlainAttr("aLong").orElseThrow().getValues().get(0).getLongValue());
    }

    @Test
    public void delete() {
        Realm realm = realmSearchDAO.findByFullPath("/odd").orElseThrow();

        // need to remove this group in order to remove the realm, which is otherwise empty
        Group group = groupDAO.findByName("fake").orElseThrow();
        assertEquals(realm, group.getRealm());
        groupDAO.delete(group);

        Role role = roleDAO.findById("User reviewer").orElseThrow();
        assertTrue(role.getRealms().contains(realm));

        int beforeSize = role.getRealms().size();

        realmDAO.delete(realm);

        role = roleDAO.findById("User reviewer").orElseThrow();
        assertEquals(beforeSize - 1, role.getRealms().size());
    }

    @Test
    public void addAndRemoveLogicActions() {
        Implementation implementation = entityFactory.newEntity(Implementation.class);
        implementation.setKey(UUID.randomUUID().toString());
        implementation.setEngine(ImplementationEngine.JAVA);
        implementation.setType(IdRepoImplementationType.LOGIC_ACTIONS);
        implementation.setBody("TestLogicActions");
        implementation = implementationDAO.save(implementation);

        Realm realm = realmDAO.findById("722f3d84-9c2b-4525-8f6e-e4b82c55a36c").orElseThrow();
        assertTrue(realm.getActions().isEmpty());

        realm.add(implementation);
        realmDAO.save(realm);

        realm = realmDAO.findById("722f3d84-9c2b-4525-8f6e-e4b82c55a36c").orElseThrow();
        assertEquals(1, realm.getActions().size());
        assertEquals(implementation, realm.getActions().getFirst());

        realm.getActions().clear();
        realm = realmDAO.save(realm);
        assertTrue(realm.getActions().isEmpty());

        realm = realmDAO.findById("722f3d84-9c2b-4525-8f6e-e4b82c55a36c").orElseThrow();
        assertTrue(realm.getActions().isEmpty());
    }

    @Test
    public void search() {
        Realm two = realmSearchDAO.findByFullPath("/even/two").orElseThrow();
        two.add(anyTypeClassDAO.findById("other").orElseThrow());
        two = realmDAO.save(two);

        two = realmDAO.findById(two.getKey()).orElseThrow();
        PlainAttr aLong = new PlainAttr();
        aLong.setSchema("aLong");
        aLong.add(validator, "42");
        two.add(aLong);
        two = realmDAO.save(two);

        Realm odd = realmSearchDAO.findByFullPath("/odd").orElseThrow();
        odd.add(anyTypeClassDAO.findById("other").orElseThrow());
        odd = realmDAO.save(odd);

        odd = realmDAO.findById(odd.getKey()).orElseThrow();
        PlainAttr oddLong = new PlainAttr();
        oddLong.setSchema("aLong");
        oddLong.add(validator, "99");
        odd.add(oddLong);
        realmDAO.save(odd);

        two = realmDAO.findById(two.getKey()).orElseThrow();
        assertEquals(anyTypeClassDAO.findById("other").orElseThrow(), two.getAnyTypeClasses().iterator().next());
        assertEquals(1, two.getPlainAttrs().size());
        assertEquals(42, two.getPlainAttr("aLong").orElseThrow().getValues().getFirst().getLongValue());

        odd = realmDAO.findById(odd.getKey()).orElseThrow();
        assertEquals(anyTypeClassDAO.findById("other").orElseThrow(), odd.getAnyTypeClasses().iterator().next());
        assertEquals(1, odd.getPlainAttrs().size());
        assertEquals(99, odd.getPlainAttr("aLong").orElseThrow().getValues().getFirst().getLongValue());

        AnyCond name = new AnyCond(AttrCond.Type.EQ);
        name.setSchema("name");
        name.setExpression("two");

        List<Realm> result = realmSearchDAO.search(
                Set.of(SyncopeConstants.ROOT_REALM),
                SearchCond.of(name),
                Pageable.unpaged());
        assertEquals(1, result.size());
        assertEquals("two", result.getFirst().getName());

        AttrCond attrEq = new AttrCond(AttrCond.Type.EQ);
        attrEq.setSchema("aLong");
        attrEq.setExpression("42");

        result = realmSearchDAO.search(
                Set.of(SyncopeConstants.ROOT_REALM),
                SearchCond.of(attrEq),
                Pageable.unpaged());
        assertEquals(1, result.size());
        assertEquals("two", result.getFirst().getName());

        result = realmSearchDAO.search(
                Set.of(SyncopeConstants.ROOT_REALM),
                SearchCond.and(SearchCond.of(name), SearchCond.of(attrEq)),
                Pageable.unpaged());
        assertEquals(1, result.size());
        assertEquals("two", result.getFirst().getName());

        AttrCond attrEq2 = new AttrCond(AttrCond.Type.EQ);
        attrEq2.setSchema("aLong");
        attrEq2.setExpression("99");

        result = realmSearchDAO.search(
                Set.of(SyncopeConstants.ROOT_REALM),
                SearchCond.or(SearchCond.of(attrEq), SearchCond.of(attrEq2)),
                Pageable.unpaged());
        assertTrue(result.stream().anyMatch(r -> "two".equals(r.getName())));
        assertTrue(result.stream().anyMatch(r -> "odd".equals(r.getName())));

        AttrCond attrIsNull = new AttrCond(AttrCond.Type.ISNULL);
        attrIsNull.setSchema("aLong");

        result = realmSearchDAO.search(
                Set.of(SyncopeConstants.ROOT_REALM),
                SearchCond.of(attrIsNull),
                Pageable.unpaged());
        result.forEach(r -> assertFalse("two".equals(r.getName()) || "odd".equals(r.getName())));

        AttrCond attrIsNotNull = new AttrCond(AttrCond.Type.ISNOTNULL);
        attrIsNotNull.setSchema("aLong");

        assertEquals(2, realmSearchDAO.count(
                Set.of(SyncopeConstants.ROOT_REALM),
                SearchCond.of(attrIsNotNull)));

        result = realmSearchDAO.search(
                Set.of(SyncopeConstants.ROOT_REALM),
                SearchCond.of(attrIsNotNull),
                Pageable.unpaged(Sort.by(new Sort.Order(Sort.Direction.ASC, "name"))));
        assertEquals("odd", result.get(0).getName());
        assertEquals("two", result.get(1).getName());

        result = realmSearchDAO.search(
                Set.of(SyncopeConstants.ROOT_REALM),
                SearchCond.of(attrIsNotNull),
                Pageable.unpaged(Sort.by(new Sort.Order(Sort.Direction.ASC, "aLong"))));
        assertEquals("two", result.get(0).getName());
        assertEquals("odd", result.get(1).getName());

        AttrCond cond1 = new AttrCond(AttrCond.Type.EQ);
        cond1.setSchema("aLong");
        cond1.setExpression("42");
        AttrCond cond2 = new AttrCond(AttrCond.Type.IEQ);
        cond2.setSchema("ctype");
        cond2.setExpression("string");
        assertDoesNotThrow(() -> realmSearchDAO.search(
                Set.of(SyncopeConstants.ROOT_REALM),
                SearchCond.and(SearchCond.of(cond1), SearchCond.of(cond2)),
                Pageable.unpaged()));
    }
}

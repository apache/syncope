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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.AuxClassCond;
import org.apache.syncope.core.persistence.api.dao.search.ResourceCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional
public class RealmSearchTest extends AbstractTest {

    @Autowired
    private AnyTypeClassDAO anyTypeClassDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private RealmSearchDAO realmSearchDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private PlainAttrValidationManager validator;

    @BeforeEach
    void prepare() {
        Realm two = realmSearchDAO.findByFullPath("/even/two").orElseThrow();
        two.add(anyTypeClassDAO.findById("other").orElseThrow());
        two.add(resourceDAO.findById("resource-ldap-orgunit").orElseThrow());
        two = realmDAO.save(two);
        entityManager.flush();

        two = realmDAO.findById(two.getKey()).orElseThrow();
        PlainAttr aLong = new PlainAttr();
        aLong.setSchema("aLong");
        aLong.add(validator, "42");
        two.add(aLong);
        two = realmDAO.save(two);
        entityManager.flush();

        Realm odd = realmSearchDAO.findByFullPath("/odd").orElseThrow();
        odd.add(anyTypeClassDAO.findById("other").orElseThrow());
        odd = realmDAO.save(odd);
        entityManager.flush();

        odd = realmDAO.findById(odd.getKey()).orElseThrow();
        PlainAttr oddLong = new PlainAttr();
        oddLong.setSchema("aLong");
        oddLong.add(validator, "99");
        odd.add(oddLong);
        realmDAO.save(odd);
        entityManager.flush();

        two = realmDAO.findById(two.getKey()).orElseThrow();
        assertEquals(anyTypeClassDAO.findById("other").orElseThrow(), two.getAnyTypeClasses().iterator().next());
        assertEquals(1, two.getPlainAttrs().size());
        assertEquals(42, two.getPlainAttr("aLong").orElseThrow().getValues().getFirst().getLongValue());

        odd = realmDAO.findById(odd.getKey()).orElseThrow();
        assertEquals(anyTypeClassDAO.findById("other").orElseThrow(), odd.getAnyTypeClasses().iterator().next());
        assertEquals(1, odd.getPlainAttrs().size());
        assertEquals(99, odd.getPlainAttr("aLong").orElseThrow().getValues().getFirst().getLongValue());
    }

    @Test
    public void byName() {
        AnyCond name = new AnyCond(AttrCond.Type.EQ);
        name.setSchema("name");
        name.setExpression("two");

        List<Realm> result = realmSearchDAO.search(
                Set.of(SyncopeConstants.ROOT_REALM),
                SearchCond.of(name),
                Pageable.unpaged());
        assertEquals(1, result.size());
        assertEquals("two", result.getFirst().getName());
    }

    @Test
    public void byAttr() {
        AttrCond attrEq = new AttrCond(AttrCond.Type.EQ);
        attrEq.setSchema("aLong");
        attrEq.setExpression("42");

        List<Realm> result = realmSearchDAO.search(
                Set.of(SyncopeConstants.ROOT_REALM),
                SearchCond.of(attrEq),
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
    }

    @Test
    public void byNameAndAttr() {
        AnyCond name = new AnyCond(AttrCond.Type.EQ);
        name.setSchema("name");
        name.setExpression("two");

        AttrCond attrEq = new AttrCond(AttrCond.Type.EQ);
        attrEq.setSchema("aLong");
        attrEq.setExpression("42");

        List<Realm> result = realmSearchDAO.search(
                Set.of(SyncopeConstants.ROOT_REALM),
                SearchCond.and(SearchCond.of(name), SearchCond.of(attrEq)),
                Pageable.unpaged());
        assertEquals(1, result.size());
        assertEquals("two", result.getFirst().getName());

    }

    @Test
    public void and() {
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

    @Test
    public void auxClass() {
        AuxClassCond auxClassCond = new AuxClassCond();
        auxClassCond.setAuxClass("other");

        List<Realm> result = realmSearchDAO.search(
                Set.of(SyncopeConstants.ROOT_REALM),
                SearchCond.of(auxClassCond),
                Pageable.unpaged(Sort.by(new Sort.Order(Sort.Direction.DESC, "aLong"))));
        assertEquals(2, result.size());
        assertEquals("two", result.get(1).getName());
        assertEquals("odd", result.get(0).getName());
    }

    @Test
    public void resource() {
        ResourceCond resourceCond = new ResourceCond();
        resourceCond.setResource("resource-ldap-orgunit");

        List<Realm> result = realmSearchDAO.search(
                Set.of(SyncopeConstants.ROOT_REALM),
                SearchCond.of(resourceCond),
                Pageable.unpaged(Sort.by(new Sort.Order(Sort.Direction.DESC, "aLong"))));
        assertEquals(1, result.size());
        assertEquals("two", result.getFirst().getName());
    }
}

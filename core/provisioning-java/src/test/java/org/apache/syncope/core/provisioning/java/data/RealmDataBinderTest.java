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
package org.apache.syncope.core.provisioning.java.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.data.RealmDataBinder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RealmDataBinderTest extends AbstractDataBinderTest {

    private static final String ROOT_KEY = "e4c28e7a-9dbf-4ee7-9441-93812a0d4a28";

    private static final String EVEN_KEY = "c5b75db1-fce7-470f-b780-3b9934d82a9d";

    private static final String TWO_KEY = "0679e069-7355-4b20-bd11-a5a0a5453c7c";

    private static final String ACCOUNT_POLICY_KEY = "20ab5a8c-4b0c-432c-b957-f7fb9784d9f7";

    private static final String PASSWORD_POLICY_KEY = "ce93fcda-dc3a-4369-a7b0-a6108c261c85";

    private static final String AUTH_POLICY_KEY = "b912a0d4-a890-416f-9ab8-84ab077eb028";

    private static final String ACCESS_POLICY_KEY = "419935c7-deb3-40b3-8a9a-683037e523a2";

    private static final String ATTR_RELEASE_POLICY_KEY = "319935c7-deb3-40b3-8a9a-683037e523a2";

    private static final String ORG_UNIT_RESOURCE = "resource-ldap-orgunit";

    @Autowired
    private RealmDataBinder binder;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private ImplementationDAO implementationDAO;

    @Autowired
    private EntityFactory entityFactory;

    protected Attr attr(final String schema, final String value) {
        return new Attr.Builder(schema).value(value).build();
    }

    @Test
    void getRealmTO() {
        RealmTO root = binder.getRealmTO(realmDAO.findById(ROOT_KEY).orElseThrow(), true);
        assertEquals(ROOT_KEY, root.getKey());
        assertEquals(SyncopeConstants.ROOT_REALM, root.getName());
        assertEquals(SyncopeConstants.ROOT_REALM, root.getFullPath());
        assertNull(root.getParent());
        assertEquals("986d1236-3ac5-4a19-810c-5ab21d79cba1", root.getPasswordPolicy());

        RealmTO two = binder.getRealmTO(realmDAO.findById(TWO_KEY).orElseThrow(), true);
        assertEquals(TWO_KEY, two.getKey());
        assertEquals("two", two.getName());
        assertEquals("/even/two", two.getFullPath());
        assertEquals(EVEN_KEY, two.getParent());
        assertEquals(ACCOUNT_POLICY_KEY, two.getAccountPolicy());
        assertEquals(PASSWORD_POLICY_KEY, two.getPasswordPolicy());
    }

    @Test
    void getRealmTONotAdmin() {
        RealmTO two = binder.getRealmTO(realmDAO.findById(TWO_KEY).orElseThrow(), false);
        assertEquals("/even/two", two.getFullPath());
        assertNull(two.getAccountPolicy());
        assertNull(two.getPasswordPolicy());
    }

    @Test
    void createUpdate() {
        Realm root = realmDAO.findById(ROOT_KEY).orElseThrow();

        RealmTO realmTO = new RealmTO();
        realmTO.setName("test");

        Realm created = realmDAO.save(binder.create(root, realmTO));

        RealmTO createdTO = binder.getRealmTO(created, true);
        assertNotNull(createdTO.getKey());
        assertEquals("test", createdTO.getName());
        assertEquals("/test", createdTO.getFullPath());
        assertEquals(ROOT_KEY, createdTO.getParent());

        createdTO.setName("test2");
        createdTO.setAccountPolicy("06e2ed52-6966-44aa-a177-a0ca7434201f");
        PropagationByResource<String> propByRes = binder.update(created, createdTO);
        assertNotNull(propByRes);
        realmDAO.save(created);

        RealmTO updated = binder.getRealmTO(created, true);
        assertEquals("test2", updated.getName());
        assertEquals("/test2", updated.getFullPath());
        assertEquals("06e2ed52-6966-44aa-a177-a0ca7434201f", updated.getAccountPolicy());
    }

    @Test
    void createFull() {
        Implementation action = entityFactory.newEntity(Implementation.class);
        action.setKey("TestRealmLogicActions");
        action.setEngine(ImplementationEngine.JAVA);
        action.setType(IdRepoImplementationType.LOGIC_ACTIONS);
        action.setBody("org.apache.syncope.core.provisioning.java.data.RealmDataBinderTest");
        implementationDAO.save(action);

        Realm root = realmDAO.findById(ROOT_KEY).orElseThrow();

        RealmTO realmTO = new RealmTO();
        realmTO.setName("full");
        realmTO.getAnyTypeClasses().add("minimal user");
        realmTO.getAnyTypeClasses().add("not-existing");
        realmTO.getPlainAttrs().add(attr("fullname", "Full Name"));
        realmTO.getPlainAttrs().add(attr("userId", "full@example.com"));
        realmTO.getPlainAttrs().add(attr("firstname", "Mario"));
        realmTO.getPlainAttrs().add(attr("surname", "Rossi"));
        realmTO.getPlainAttrs().add(attr("not-existing", "value"));
        realmTO.setAccountPolicy(ACCOUNT_POLICY_KEY);
        realmTO.setPasswordPolicy(PASSWORD_POLICY_KEY);
        realmTO.setAuthPolicy(AUTH_POLICY_KEY);
        realmTO.setAccessPolicy(ACCESS_POLICY_KEY);
        realmTO.setAttrReleasePolicy(ATTR_RELEASE_POLICY_KEY);
        realmTO.getActions().add(action.getKey());
        realmTO.getActions().add("not-existing");

        UserTO template = new UserTO();
        template.setUsername("'templated'");
        template.getPlainAttrs().add(attr("firstname", "'Mario'"));
        realmTO.getTemplates().put(AnyTypeKind.USER.name(), template);

        Realm created = realmDAO.save(binder.create(root, realmTO));
        entityManager.flush();

        RealmTO actual = binder.getRealmTO(created, true);
        assertEquals("full", actual.getName());
        assertEquals("/full", actual.getFullPath());
        assertEquals(ROOT_KEY, actual.getParent());
        assertEquals(1, actual.getAnyTypeClasses().size());
        assertEquals("minimal user", actual.getAnyTypeClasses().getFirst());
        assertEquals(ACCOUNT_POLICY_KEY, actual.getAccountPolicy());
        assertEquals(PASSWORD_POLICY_KEY, actual.getPasswordPolicy());
        assertEquals(AUTH_POLICY_KEY, actual.getAuthPolicy());
        assertEquals(ACCESS_POLICY_KEY, actual.getAccessPolicy());
        assertEquals(ATTR_RELEASE_POLICY_KEY, actual.getAttrReleasePolicy());
        assertEquals(1, actual.getActions().size());
        assertEquals(action.getKey(), actual.getActions().getFirst());
        assertTrue(actual.getTemplates().containsKey(AnyTypeKind.USER.name()));
        assertEquals("Full Name", actual.getPlainAttrs().stream().
                filter(a -> "fullname".equals(a.getSchema())).
                findFirst().orElseThrow().getValues().getFirst());
    }

    @Test
    void updateResources() {
        Realm two = realmDAO.findById(TWO_KEY).orElseThrow();
        RealmTO realmTO = binder.getRealmTO(two, true);
        realmTO.getResources().add(ORG_UNIT_RESOURCE);
        realmTO.getResources().add("not-existing");

        PropagationByResource<String> propByRes = binder.update(two, realmTO);
        assertNotNull(propByRes);
        realmDAO.save(two);
        entityManager.flush();

        RealmTO updated = binder.getRealmTO(two, true);
        assertEquals(1, updated.getResources().size());
        assertEquals(ORG_UNIT_RESOURCE, updated.getResources().getFirst());

        // now remove the resource again
        updated.getResources().clear();
        binder.update(two, updated);
        realmDAO.save(two);
        entityManager.flush();

        assertTrue(binder.getRealmTO(two, true).getResources().isEmpty());
    }

    @Test
    void createMissingMandatory() {
        Realm root = realmDAO.findById(ROOT_KEY).orElseThrow();

        RealmTO realmTO = new RealmTO();
        realmTO.setName("missing");
        realmTO.getAnyTypeClasses().add("minimal user");
        realmTO.getPlainAttrs().add(attr("firstname", "Mario"));

        assertThrows(SyncopeClientException.class, () -> binder.create(root, realmTO));
    }

    @Test
    void createInvalidTemplate() {
        Realm root = realmDAO.findById(ROOT_KEY).orElseThrow();

        RealmTO realmTO = new RealmTO();
        realmTO.setName("invalid-template");

        UserTO template = new UserTO();
        template.setUsername("not a valid ((");
        realmTO.getTemplates().put(AnyTypeKind.USER.name(), template);
        realmTO.getTemplates().put("not-existing", template);

        assertThrows(SyncopeClientException.class, () -> binder.create(root, realmTO));
    }
}

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
package org.apache.syncope.core.persistence.jpa.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.ConnPoolConf;
import org.apache.syncope.common.lib.types.EntitlementsHolder;
import org.apache.syncope.common.lib.types.IdMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.MasterDomain;
import org.apache.syncope.core.persistence.jpa.entity.JPAConnInstance;
import org.apache.syncope.core.persistence.jpa.entity.JPAExternalResource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = { MasterDomain.class, PersistenceUpgraderTestContext.class })
class VerifySyncope4Test {

    @DynamicPropertySource
    static void configureProperties(final DynamicPropertyRegistry registry) {
        registry.add("DB_URL", GenerateUpgradeSQLTest.JDBC_URL_SUPPLIER);
        registry.add("DB_USER", GenerateUpgradeSQLTest.DB_CRED_SUPPLIER);
        registry.add("DB_PASSWORD", GenerateUpgradeSQLTest.DB_CRED_SUPPLIER);
    }

    @BeforeAll
    public static void init() {
        EntitlementsHolder.getInstance().addAll(IdRepoEntitlement.values());
        EntitlementsHolder.getInstance().addAll(IdMEntitlement.values());
        EntitlementsHolder.getInstance().addAll(AMEntitlement.values());
    }

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private RelationshipTypeDAO relationshipTypeDAO;

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private EntityManager entityManager;

    @Test
    void connectors() {
        long count = connInstanceDAO.count();

        TypedQuery<ConnInstance> query = entityManager.createQuery(
                "SELECT e FROM " + JPAConnInstance.class.getSimpleName() + " e", ConnInstance.class);

        List<? extends ConnInstance> connectors = query.getResultList();

        assertEquals(count, connectors.size());

        ConnPoolConf poolConf = connInstanceDAO.findById(
                "74141a3b-0762-4720-a4aa-fc3e374ef3ef").orElseThrow().getPoolConf();
        assertEquals(3, poolConf.getMaxIdle());
        assertEquals(5, poolConf.getMaxObjects());
        assertEquals(10, poolConf.getMaxWait());
        assertEquals(5, poolConf.getMinEvictableIdleTimeMillis());
        assertEquals(2, poolConf.getMinIdle());
    }

    @Test
    void resources() {
        long count = resourceDAO.count();

        TypedQuery<ExternalResource> query = entityManager.createQuery(
                "SELECT e FROM " + JPAExternalResource.class.getSimpleName() + " e", ExternalResource.class);

        List<? extends ExternalResource> resources = query.getResultList();

        assertEquals(count, resources.size());

        assertEquals(
                "20ab5a8c-4b0c-432c-b957-f7fb9784d9f7",
                resourceDAO.findById("resource-testdb").orElseThrow().getAccountPolicy().getKey());
        assertEquals(
                "0194459e-2857-7f29-8969-18eb19821a25",
                resourceDAO.findById("resource-ldap").orElseThrow().getAccountPolicy().getKey());
    }

    @Test
    void plainSchemas() {
        long count = plainSchemaDAO.count();

        List<? extends PlainSchema> plainSchemas = plainSchemaDAO.findAll();

        assertEquals(count, plainSchemas.size());

        PlainSchema gender = plainSchemaDAO.findById("gender").orElseThrow();
        assertEquals(AttrSchemaType.Enum, gender.getType());
        assertEquals(2, gender.getEnumValues().size());
        assertTrue(gender.getEnumValues().containsKey("M"));
        assertTrue(gender.getEnumValues().containsValue("M"));
        assertTrue(gender.getEnumValues().containsKey("F"));
        assertTrue(gender.getEnumValues().containsValue("F"));
    }

    @Test
    void roles() {
        long count = roleDAO.count();

        List<? extends Role> roles = roleDAO.findAll();

        assertEquals(count, roles.size());

        Role role = roleDAO.findById("dynMembershipc60fa491").orElseThrow();
        assertEquals("cool==true", role.getDynMembershipCond());
    }

    @Test
    void relationshipTypes() {
        assertTrue(relationshipTypeDAO.findAll().stream().
                allMatch(r -> r.getLeftEndAnyType() != null && r.getRightEndAnyType() != null));
    }

    @Test
    void anyObjects() {
        long count = anyObjectDAO.count();

        List<? extends AnyObject> anyObjects = anyObjectDAO.findAll();

        assertEquals(count, anyObjects.size());
    }

    @Test
    void groups() {
        long count = groupDAO.count();

        List<? extends Group> groups = groupDAO.findAll();

        assertEquals(count, groups.size());
    }

    @Test
    void users() {
        long count = userDAO.count();

        List<? extends User> users = userDAO.findAll();

        assertEquals(count, users.size());

        assertEquals(
                "Antonio",
                userDAO.findByUsername("vivaldi").orElseThrow().
                        getPlainAttr("firstname").orElseThrow().
                        getValuesAsStrings().getFirst());
    }
}

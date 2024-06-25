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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.IdMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.neo4j.AbstractTest;
import org.apache.syncope.core.spring.security.SyncopeAuthenticationDetails;
import org.apache.syncope.core.spring.security.SyncopeGrantedAuthority;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class PlainSchemaTest extends AbstractTest {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private AnyTypeClassDAO anyTypeClassDAO;

    @BeforeAll
    public static void setAuthContext() {
        List<GrantedAuthority> authorities = Stream.concat(
                IdRepoEntitlement.values().stream(), IdMEntitlement.values().stream()).
                map(entitlement -> new SyncopeGrantedAuthority(entitlement, SyncopeConstants.ROOT_REALM)).
                collect(Collectors.toList());

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(
                        "admin", "FAKE_PASSWORD", authorities), "FAKE_PASSWORD", authorities);
        auth.setDetails(new SyncopeAuthenticationDetails(SyncopeConstants.MASTER_DOMAIN, null));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterAll
    public static void unsetAuthContext() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Test
    public void findByAnyTypeClasses() {
        List<? extends PlainSchema> found = plainSchemaDAO.findByAnyTypeClasses(
                List.of(anyTypeClassDAO.findById("minimal user").orElseThrow()));
        assertEquals(5, found.size());

        found = plainSchemaDAO.findByAnyTypeClasses(
                List.of(anyTypeClassDAO.findById("other").orElseThrow()));
        assertEquals(11, found.size());
    }

    @Test
    public void checkIdUniqueness() {
        assertNotNull(derSchemaDAO.findById("cn").orElseThrow());

        PlainSchema schema = entityFactory.newEntity(PlainSchema.class);
        schema.setKey("cn");
        schema.setType(AttrSchemaType.String);

        try {
            plainSchemaDAO.save(schema);
            fail("This should not happen");
        } catch (Exception e) {
            assertTrue(e instanceof DataIntegrityViolationException);
        }
    }

    private List<Item> getMappingItems(final String intAttrName) {
        return resourceDAO.findAll().stream().
                flatMap(resource -> resource.getProvisions().stream()).
                flatMap(provision -> provision.getMapping().getItems().stream()).
                filter(item -> intAttrName.equals(item.getIntAttrName())).
                toList();
    }

    @Test
    public void deleteFullname() {
        // fullname is mapped as ConnObjectKey for ws-target-resource-2, need to swap it otherwise validation errors 
        // will be raised
        resourceDAO.findById("ws-target-resource-2").orElseThrow().
                getProvisionByAnyType(AnyTypeKind.USER.name()).get().getMapping().getItems().
                forEach(item -> {
                    if ("fullname".equals(item.getIntAttrName())) {
                        item.setConnObjectKey(false);
                    } else if ("surname".equals(item.getIntAttrName())) {
                        item.setConnObjectKey(true);
                    }
                });

        // search for user schema fullname
        plainSchemaDAO.findById("fullname").orElseThrow();

        // check for associated mappings
        List<Item> mapItems = getMappingItems("fullname");
        assertFalse(mapItems.isEmpty());

        // delete user schema fullname
        plainSchemaDAO.deleteById("fullname");

        // check for schema deletion
        assertTrue(plainSchemaDAO.findById("fullname").isEmpty());

        // check for mappings deletion
        mapItems = getMappingItems("fullname");
        assertTrue(mapItems.isEmpty());

        assertFalse(userDAO.findByUsername("rossini").orElseThrow().getPlainAttr("fullname").isPresent());
        assertFalse(userDAO.findByUsername("vivaldi").orElseThrow().getPlainAttr("fullname").isPresent());
    }

    @Test
    public void deleteSurname() {
        // search for user schema surname
        PlainSchema schema = plainSchemaDAO.findById("surname").orElseThrow();

        // check for associated mappings
        List<Item> mapItems = getMappingItems("surname");
        assertFalse(mapItems.isEmpty());

        // check for labels
        assertEquals(2, schema.getLabels().size());

        // delete user schema surname
        plainSchemaDAO.deleteById("surname");

        // check for schema deletion
        assertTrue(plainSchemaDAO.findById("surname").isEmpty());
    }

    @Test
    public void deleteFirstname() {
        int pre = resourceDAO.findById("resource-db-pull").orElseThrow().
                getProvisionByAnyType(AnyTypeKind.USER.name()).get().getMapping().getItems().size();

        plainSchemaDAO.deleteById("firstname");

        assertTrue(plainSchemaDAO.findById("firstname").isEmpty());

        assertEquals(pre - 1, resourceDAO.findById("resource-db-pull").orElseThrow().
                getProvisionByAnyType(AnyTypeKind.USER.name()).get().getMapping().getItems().size());
    }
}

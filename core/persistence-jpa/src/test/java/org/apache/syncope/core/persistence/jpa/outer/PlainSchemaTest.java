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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.EntityExistsException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.apache.syncope.core.spring.security.SyncopeAuthenticationDetails;
import org.apache.syncope.core.spring.security.SyncopeGrantedAuthority;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class PlainSchemaTest extends AbstractTest {

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Autowired
    private PlainAttrDAO plainAttrDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @BeforeClass
    public static void setAuthContext() {
        List<GrantedAuthority> authorities = StandardEntitlement.values().stream().
                map(entitlement -> new SyncopeGrantedAuthority(entitlement, SyncopeConstants.ROOT_REALM)).
                collect(Collectors.toList());

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(
                        "admin", "FAKE_PASSWORD", authorities), "FAKE_PASSWORD", authorities);
        auth.setDetails(new SyncopeAuthenticationDetails("Master"));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterClass
    public static void unsetAuthContext() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Test
    public void checkIdUniqueness() {
        assertNotNull(derSchemaDAO.find("cn"));

        PlainSchema schema = entityFactory.newEntity(PlainSchema.class);
        schema.setKey("cn");
        schema.setType(AttrSchemaType.String);
        plainSchemaDAO.save(schema);

        try {
            plainSchemaDAO.flush();
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof EntityExistsException);
        }
    }

    @Test
    public void deleteFullname() {
        // fullname is mapped as ConnObjectKey for ws-target-resource-2, need to swap it otherwise validation errors 
        // will be raised
        for (MappingItem item : resourceDAO.find("ws-target-resource-2").
                getProvision(anyTypeDAO.findUser()).get().getMapping().getItems()) {

            if ("fullname".equals(item.getIntAttrName())) {
                item.setConnObjectKey(false);
            } else if ("surname".equals(item.getIntAttrName())) {
                item.setConnObjectKey(true);
            }
        }

        // search for user schema fullname
        PlainSchema schema = plainSchemaDAO.find("fullname");
        assertNotNull(schema);

        // check for associated mappings
        Set<MappingItem> mapItems = new HashSet<>();
        for (ExternalResource resource : resourceDAO.findAll()) {
            if (resource.getProvision(anyTypeDAO.findUser()).isPresent()
                    && resource.getProvision(anyTypeDAO.findUser()).get().getMapping() != null) {

                for (MappingItem mapItem : resource.getProvision(anyTypeDAO.findUser()).get().getMapping().getItems()) {
                    if (schema.getKey().equals(mapItem.getIntAttrName())) {
                        mapItems.add(mapItem);
                    }
                }
            }
        }
        assertFalse(mapItems.isEmpty());

        // delete user schema fullname
        plainSchemaDAO.delete("fullname");

        plainSchemaDAO.flush();

        // check for schema deletion
        schema = plainSchemaDAO.find("fullname");
        assertNull(schema);

        plainSchemaDAO.clear();

        // check for mappings deletion
        mapItems = new HashSet<>();
        for (ExternalResource resource : resourceDAO.findAll()) {
            if (resource.getProvision(anyTypeDAO.findUser()).isPresent()
                    && resource.getProvision(anyTypeDAO.findUser()).get().getMapping() != null) {

                for (MappingItem mapItem : resource.getProvision(anyTypeDAO.findUser()).get().getMapping().getItems()) {
                    if ("fullname".equals(mapItem.getIntAttrName())) {
                        mapItems.add(mapItem);
                    }
                }
            }
        }
        assertTrue(mapItems.isEmpty());

        assertNull(plainAttrDAO.find("01f22fbd-b672-40af-b528-686d9b27ebc4", UPlainAttr.class));
        assertNull(plainAttrDAO.find(UUID.randomUUID().toString(), UPlainAttr.class));
        assertFalse(userDAO.findByUsername("rossini").getPlainAttr("fullname").isPresent());
        assertFalse(userDAO.findByUsername("vivaldi").getPlainAttr("fullname").isPresent());
    }

    @Test
    public void deleteSurname() {
        // search for user schema fullname
        PlainSchema schema = plainSchemaDAO.find("surname");
        assertNotNull(schema);

        // check for associated mappings
        Set<MappingItem> mappings = new HashSet<>();
        for (ExternalResource resource : resourceDAO.findAll()) {
            if (resource.getProvision(anyTypeDAO.findUser()).isPresent()
                    && resource.getProvision(anyTypeDAO.findUser()).get().getMapping() != null) {

                for (MappingItem mapItem : resource.getProvision(anyTypeDAO.findUser()).get().getMapping().getItems()) {
                    if (schema.getKey().equals(mapItem.getIntAttrName())) {
                        mappings.add(mapItem);
                    }
                }
            }
        }
        assertFalse(mappings.isEmpty());

        // delete user schema fullname
        plainSchemaDAO.delete("surname");

        plainSchemaDAO.flush();

        // check for schema deletion
        schema = plainSchemaDAO.find("surname");
        assertNull(schema);
    }

    @Test
    public void deleteFirstname() {
        assertEquals(5, resourceDAO.find("resource-db-pull").
                getProvision(anyTypeDAO.findUser()).get().getMapping().getItems().size());

        plainSchemaDAO.delete("firstname");
        assertNull(plainSchemaDAO.find("firstname"));

        plainSchemaDAO.flush();

        assertEquals(4, resourceDAO.find("resource-db-pull").
                getProvision(anyTypeDAO.findUser()).get().getMapping().getItems().size());
    }
}

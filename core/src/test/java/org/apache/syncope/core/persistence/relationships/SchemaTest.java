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
package org.apache.syncope.core.persistence.relationships;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.apache.syncope.core.persistence.beans.AbstractMappingItem;
import org.apache.syncope.core.persistence.beans.user.UAttr;
import org.apache.syncope.core.persistence.beans.user.USchema;
import org.apache.syncope.core.persistence.dao.AttrDAO;
import org.apache.syncope.core.persistence.dao.ResourceDAO;
import org.apache.syncope.core.persistence.dao.SchemaDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.AbstractTest;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.types.AttributableType;
import org.junit.Test;

@Transactional
public class SchemaTest extends AbstractTest {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private SchemaDAO schemaDAO;

    @Autowired
    private AttrDAO attrDAO;

    @Autowired
    private ResourceDAO resourceDAO;

    @Test
    public void deleteFullname() {
        // search for user schema fullname
        USchema schema = schemaDAO.find("fullname", USchema.class);
        assertNotNull(schema);

        // check for associated mappings
        Set<AbstractMappingItem> mapItems = new HashSet<AbstractMappingItem>();
        for (ExternalResource resource : resourceDAO.findAll()) {
            if (resource.getUmapping() != null) {
                for (AbstractMappingItem mapItem : resource.getUmapping().getItems()) {
                    if (schema.getName().equals(mapItem.getIntAttrName())) {
                        mapItems.add(mapItem);
                    }
                }
            }
        }
        assertFalse(mapItems.isEmpty());

        // delete user schema fullname
        schemaDAO.delete("fullname", AttributableUtil.getInstance(AttributableType.USER));

        schemaDAO.flush();

        // check for schema deletion
        schema = schemaDAO.find("fullname", USchema.class);
        assertNull(schema);

        schemaDAO.clear();

        // check for mappings deletion
        mapItems = new HashSet<AbstractMappingItem>();
        for (ExternalResource resource : resourceDAO.findAll()) {
            if (resource.getUmapping() != null) {
                for (AbstractMappingItem mapItem : resource.getUmapping().getItems()) {
                    if ("fullname".equals(mapItem.getIntAttrName())) {
                        mapItems.add(mapItem);
                    }
                }
            }
        }
        assertTrue(mapItems.isEmpty());

        assertNull(attrDAO.find(100L, UAttr.class));
        assertNull(attrDAO.find(300L, UAttr.class));
        assertNull(userDAO.find(1L).getAttribute("fullname"));
        assertNull(userDAO.find(3L).getAttribute("fullname"));
    }

    @Test
    public void deleteSurname() {
        // search for user schema fullname
        USchema schema = schemaDAO.find("surname", USchema.class);
        assertNotNull(schema);

        // check for associated mappings
        Set<AbstractMappingItem> mappings = new HashSet<AbstractMappingItem>();
        for (ExternalResource resource : resourceDAO.findAll()) {
            if (resource.getUmapping() != null) {
                for (AbstractMappingItem mapItem : resource.getUmapping().getItems()) {
                    if (schema.getName().equals(mapItem.getIntAttrName())) {
                        mappings.add(mapItem);
                    }
                }
            }
        }
        assertFalse(mappings.isEmpty());

        // delete user schema fullname
        schemaDAO.delete("surname", AttributableUtil.getInstance(AttributableType.USER));

        schemaDAO.flush();

        // check for schema deletion
        schema = schemaDAO.find("surname", USchema.class);
        assertNull(schema);
    }

    @Test
    public void deleteALong() {
        assertEquals(6, resourceDAO.find("resource-db-sync").getUmapping().getItems().size());

        schemaDAO.delete("aLong", AttributableUtil.getInstance(AttributableType.USER));
        assertNull(schemaDAO.find("aLong", USchema.class));

        schemaDAO.flush();

        assertEquals(5, resourceDAO.find("resource-db-sync").getUmapping().getItems().size());
    }
}

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
package org.apache.syncope.core.persistence.jpa.inner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.syncope.core.persistence.api.dao.auth.WAConfigDAO;
import org.apache.syncope.core.persistence.api.entity.auth.WAConfigEntry;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.apache.syncope.core.persistence.jpa.entity.auth.JPAWAConfigEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class WAConfigTest extends AbstractTest {

    @Autowired
    private WAConfigDAO configDAO;

    @BeforeEach
    public void beforeEach() {
        entityManager().createQuery("DELETE FROM " + JPAWAConfigEntry.class.getSimpleName()).executeUpdate();
    }

    @Test
    public void saveCommaSeparatedValueStrings() {
        create("system.example.key[0]", Arrays.asList("value1", "value2", "value3"));
        assertFalse(configDAO.findAll().isEmpty());
    }

    @Test
    public void saveNumbers() {
        create("system.example.key[0]", List.of("1984"));
        assertFalse(configDAO.findAll().isEmpty());
    }

    @Test
    public void saveCollection() {
        WAConfigEntry entry = create("system.example.key[0]", new ArrayList<>(Arrays.asList("1", "2")));
        assertNotNull(entry.getValues());
        assertFalse(configDAO.findAll().isEmpty());
    }

    @Test
    public void saveList() {
        create("system.example.key[0].key1", List.of("value1"));
        assertFalse(configDAO.findAll().isEmpty());
    }

    @Test
    public void update() {
        WAConfigEntry entry = create("system.syncope.key[0]", Arrays.asList("1", "2", "3", "4"));
        assertNotNull(entry);
        entry.setValues(List.of("v1"));

        entry = configDAO.save(entry);
        assertNotNull(entry);
        assertNotNull(entry.getKey());
        WAConfigEntry found = configDAO.find(entry.getKey());
        assertNotNull(found);
        assertEquals(List.of("v1"), found.getValues());
    }

    private WAConfigEntry create(final String name, final List<String> value) {
        WAConfigEntry entry = entityFactory.newEntity(WAConfigEntry.class);
        entry.setKey(name);
        entry.setValues(value);
        configDAO.save(entry);
        assertNotNull(entry);
        assertNotNull(entry.getKey());
        assertNotNull(configDAO.find(entry.getKey()));
        return entry;
    }
}

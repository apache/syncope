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
package org.apache.syncope.core.persistence.neo4j.inner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.ClassUtils;
import org.apache.syncope.common.lib.attr.AttrRepoConf;
import org.apache.syncope.common.lib.attr.JDBCAttrRepoConf;
import org.apache.syncope.common.lib.attr.LDAPAttrRepoConf;
import org.apache.syncope.common.lib.attr.StubAttrRepoConf;
import org.apache.syncope.common.lib.attr.SyncopeAttrRepoConf;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.types.AttrRepoState;
import org.apache.syncope.core.persistence.api.dao.AttrRepoDAO;
import org.apache.syncope.core.persistence.api.entity.am.AttrRepo;
import org.apache.syncope.core.persistence.neo4j.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class AttrRepoTest extends AbstractTest {

    private static boolean isSpecificConf(final AttrRepoConf conf, final Class<? extends AttrRepoConf> clazz) {
        return ClassUtils.isAssignable(clazz, conf.getClass());
    }

    @Autowired
    private AttrRepoDAO attrRepoDAO;

    @Test
    public void findAll() {
        List<? extends AttrRepo> modules = attrRepoDAO.findAll();
        assertNotNull(modules);
        assertFalse(modules.isEmpty());
        assertTrue(modules.size() >= 4);
    }

    @Test
    public void find() {
        AttrRepo attrRepo = attrRepoDAO.findById("DefaultLDAPAttrRepo").orElseThrow();
        assertTrue(attrRepo.getConf() instanceof LDAPAttrRepoConf);

        attrRepo = attrRepoDAO.findById("DefaultJDBCAttrRepo").orElseThrow();
        assertTrue(attrRepo.getConf() instanceof JDBCAttrRepoConf);

        attrRepo = attrRepoDAO.findById("DefaultStubAttrRepo").orElseThrow();
        assertTrue(attrRepo.getConf() instanceof StubAttrRepoConf);
        assertEquals(1, attrRepo.getItems().size());

        attrRepo = attrRepoDAO.findById("DefaultSyncopeAttrRepo").orElseThrow();
        assertTrue(attrRepo.getConf() instanceof SyncopeAttrRepoConf);
    }

    @Test
    public void findByType() {
        List<? extends AttrRepo> attrRepos = attrRepoDAO.findAll();
        assertTrue(attrRepos.stream().anyMatch(
                attrRepo -> isSpecificConf(attrRepo.getConf(), LDAPAttrRepoConf.class)
                && attrRepo.getKey().equals("DefaultLDAPAttrRepo")));
        assertTrue(attrRepos.stream().anyMatch(
                attrRepo -> isSpecificConf(attrRepo.getConf(), JDBCAttrRepoConf.class)
                && attrRepo.getKey().equals("DefaultJDBCAttrRepo")));
        assertTrue(attrRepos.stream().anyMatch(
                attrRepo -> isSpecificConf(attrRepo.getConf(), SyncopeAttrRepoConf.class)
                && attrRepo.getKey().equals("DefaultSyncopeAttrRepo")));
        assertTrue(attrRepos.stream().anyMatch(
                attrRepo -> isSpecificConf(attrRepo.getConf(), StubAttrRepoConf.class)
                && attrRepo.getKey().equals("DefaultStubAttrRepo")));
    }

    private void saveAttrRepo(final String key, final AttrRepoConf conf) {
        AttrRepo attrRepo = entityFactory.newEntity(AttrRepo.class);
        attrRepo.setKey(key);
        attrRepo.setDescription("An attr repo");
        attrRepo.setState(AttrRepoState.ACTIVE);
        attrRepo.setConf(conf);

        Item keyMapping = new Item();
        keyMapping.setIntAttrName("uid");
        keyMapping.setExtAttrName("username");
        attrRepo.getItems().add(keyMapping);

        Item fullnameMapping = new Item();
        fullnameMapping.setIntAttrName("cn");
        fullnameMapping.setExtAttrName("fullname");
        attrRepo.getItems().add(fullnameMapping);

        attrRepo = attrRepoDAO.save(attrRepo);

        assertNotNull(attrRepo);
        assertNotNull(attrRepo.getKey());
        assertEquals(attrRepo, attrRepoDAO.findById(attrRepo.getKey()).orElseThrow());
        assertEquals(2, attrRepo.getItems().size());
    }

    @Test
    public void saveWithStubRepo() {
        StubAttrRepoConf conf = new StubAttrRepoConf();
        conf.getAttributes().put("attr1", UUID.randomUUID().toString());
        conf.getAttributes().put("attr2", UUID.randomUUID().toString());

        saveAttrRepo("StaticAttrRepoTest", conf);
    }

    @Test
    public void saveWithLdapRepo() {
        LDAPAttrRepoConf conf = new LDAPAttrRepoConf();
        conf.setBaseDn("dc=example,dc=org");
        conf.setSearchFilter("cn={user}");
        conf.setSubtreeSearch(true);
        conf.setLdapUrl("ldap://localhost:1389");
        conf.setBindCredential("Password");

        saveAttrRepo("LDAPAttrRepoTest", conf);
    }

    @Test
    public void saveWithJDBCRepo() {
        JDBCAttrRepoConf conf = new JDBCAttrRepoConf();
        conf.setSql("SELECT * FROM table WHERE name=?");
        conf.setUrl("jdbc:h2:mem:syncopedb;DB_CLOSE_DELAY=-1");
        conf.setUser("username");
        conf.setPassword("password");

        saveAttrRepo("JDBCAttrRepoTest", conf);
    }

    @Test
    public void saveWithSyncopeRepo() {
        SyncopeAttrRepoConf conf = new SyncopeAttrRepoConf();
        conf.setDomain("Master");

        saveAttrRepo("SyncopeAttrRepoTest", conf);
    }

    @Test
    public void updateWithLDAPRepo() {
        AttrRepo module = attrRepoDAO.findById("DefaultLDAPAttrRepo").orElseThrow();
        AttrRepoConf conf = module.getConf();
        LDAPAttrRepoConf.class.cast(conf).setBaseDn("dc=example2,dc=org");
        LDAPAttrRepoConf.class.cast(conf).setSearchFilter("cn={user2}");
        module.setConf(conf);

        module = attrRepoDAO.save(module);
        assertNotNull(module);
        assertNotNull(module.getKey());

        AttrRepo found = attrRepoDAO.findById(module.getKey()).orElseThrow();
        assertEquals("dc=example2,dc=org", LDAPAttrRepoConf.class.cast(found.getConf()).getBaseDn());
        assertEquals("cn={user2}", LDAPAttrRepoConf.class.cast(found.getConf()).getSearchFilter());
    }

    @Test
    public void updateWithJDBCRepo() {
        AttrRepo module = attrRepoDAO.findById("DefaultJDBCAttrRepo").orElseThrow();
        AttrRepoConf conf = module.getConf();
        JDBCAttrRepoConf.class.cast(conf).setSql("SELECT * FROM otherTable WHERE name=?");
        module.setConf(conf);

        module = attrRepoDAO.save(module);
        assertNotNull(module);
        assertNotNull(module.getKey());
        AttrRepo found = attrRepoDAO.findById(module.getKey()).orElseThrow();
        assertEquals("SELECT * FROM otherTable WHERE name=?", JDBCAttrRepoConf.class.cast(found.getConf()).getSql());
    }

    @Test
    public void updateWithStubRepo() {
        AttrRepo module = attrRepoDAO.findById("DefaultStubAttrRepo").orElseThrow();
        assertEquals(1, StubAttrRepoConf.class.cast(module.getConf()).getAttributes().size());
        AttrRepoConf conf = module.getConf();
        StubAttrRepoConf.class.cast(conf).getAttributes().put("attr3", UUID.randomUUID().toString());
        module.setConf(conf);

        module = attrRepoDAO.save(module);
        assertNotNull(module.getKey());
        AttrRepo found = attrRepoDAO.findById(module.getKey()).orElseThrow();
        assertEquals(2, StubAttrRepoConf.class.cast(found.getConf()).getAttributes().size());
    }

    @Test
    public void updateWithSyncopeRepo() {
        AttrRepo module = attrRepoDAO.findById("DefaultSyncopeAttrRepo").orElseThrow();

        AttrRepoConf conf = module.getConf();
        SyncopeAttrRepoConf.class.cast(conf).setDomain("Two");
        module.setConf(conf);

        module = attrRepoDAO.save(module);
        assertNotNull(module);
        assertNotNull(module.getKey());
        AttrRepo found = attrRepoDAO.findById(module.getKey()).orElseThrow();
        assertEquals("Two", SyncopeAttrRepoConf.class.cast(found.getConf()).getDomain());
    }

    @Test
    public void delete() {
        assertTrue(attrRepoDAO.findById("DefaultSyncopeAttrRepo").isPresent());

        attrRepoDAO.deleteById("DefaultSyncopeAttrRepo");

        assertTrue(attrRepoDAO.findById("DefaultSyncopeAttrRepo").isEmpty());
    }
}

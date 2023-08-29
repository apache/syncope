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
package org.apache.syncope.fit.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.attr.AttrRepoConf;
import org.apache.syncope.common.lib.attr.JDBCAttrRepoConf;
import org.apache.syncope.common.lib.attr.LDAPAttrRepoConf;
import org.apache.syncope.common.lib.attr.StubAttrRepoConf;
import org.apache.syncope.common.lib.attr.SyncopeAttrRepoConf;
import org.apache.syncope.common.lib.to.AttrRepoTO;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.types.AttrRepoState;
import org.apache.syncope.common.lib.types.CaseCanonicalizationMode;
import org.apache.syncope.common.rest.api.service.AttrRepoService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class AttrRepoITCase extends AbstractITCase {

    private enum AttrRepoSupportedType {
        STUB,
        SYNCOPE,
        LDAP,
        JDBC;

    };

    private static AttrRepoTO createAttrRepo(final AttrRepoTO attrRepo) {
        Response response = ATTR_REPO_SERVICE.create(attrRepo);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            Exception ex = CLIENT_FACTORY.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return getObject(response.getLocation(), AttrRepoService.class, attrRepo.getClass());
    }

    private static AttrRepoTO buildAttrRepoTO(final AttrRepoSupportedType type) {
        AttrRepoTO attrRepoTO = new AttrRepoTO();
        attrRepoTO.setKey("Test" + type + "AttrRepo" + getUUIDString());
        attrRepoTO.setDescription("A test " + type + " attr repo");
        attrRepoTO.setState(AttrRepoState.ACTIVE);

        AttrRepoConf conf;
        switch (type) {
            case LDAP:
                conf = new LDAPAttrRepoConf();
                LDAPAttrRepoConf.class.cast(conf).setBaseDn("dc=example,dc=org");
                LDAPAttrRepoConf.class.cast(conf).setSearchFilter("cn={user}");
                LDAPAttrRepoConf.class.cast(conf).setSubtreeSearch(true);
                LDAPAttrRepoConf.class.cast(conf).setLdapUrl("ldap://localhost:1389");
                LDAPAttrRepoConf.class.cast(conf).setBaseDn("cn=Directory Manager,dc=example,dc=org");
                LDAPAttrRepoConf.class.cast(conf).setBindCredential("Password");
                break;

            case JDBC:
                conf = new JDBCAttrRepoConf();
                JDBCAttrRepoConf.class.cast(conf).setSql("SELECT * FROM table WHERE name=?");
                JDBCAttrRepoConf.class.cast(conf).getUsername().add("name");
                JDBCAttrRepoConf.class.cast(conf).getQueryAttributes().put("key1", "value1");
                break;

            case SYNCOPE:
                conf = new SyncopeAttrRepoConf();
                SyncopeAttrRepoConf.class.cast(conf).setDomain(SyncopeConstants.MASTER_DOMAIN);
                break;

            case STUB:
            default:
                conf = new StubAttrRepoConf();
                StubAttrRepoConf.class.cast(conf).getAttributes().put("attr9", UUID.randomUUID().toString());
                StubAttrRepoConf.class.cast(conf).getAttributes().put("attr8", UUID.randomUUID().toString());
                break;
        }
        attrRepoTO.setConf(conf);

        Item keyMapping = new Item();
        keyMapping.setIntAttrName("uid");
        keyMapping.setExtAttrName("username");
        attrRepoTO.getItems().add(keyMapping);

        Item fullnameMapping = new Item();
        fullnameMapping.setIntAttrName("cn");
        fullnameMapping.setExtAttrName("fullname");
        attrRepoTO.getItems().add(fullnameMapping);

        return attrRepoTO;
    }

    private static boolean isSpecificConf(final AttrRepoConf conf, final Class<? extends AttrRepoConf> clazz) {
        return ClassUtils.isAssignable(clazz, conf.getClass());
    }

    @Test
    public void list() {
        List<AttrRepoTO> attrRepoTOs = ATTR_REPO_SERVICE.list();
        assertNotNull(attrRepoTOs);
        assertFalse(attrRepoTOs.isEmpty());

        assertTrue(attrRepoTOs.stream().anyMatch(
                attrRepo -> isSpecificConf(attrRepo.getConf(), LDAPAttrRepoConf.class)
                && attrRepo.getKey().equals("DefaultLDAPAttrRepo")));
        assertTrue(attrRepoTOs.stream().anyMatch(
                attrRepo -> isSpecificConf(attrRepo.getConf(), JDBCAttrRepoConf.class)
                && attrRepo.getKey().equals("DefaultJDBCAttrRepo")));
        assertTrue(attrRepoTOs.stream().anyMatch(
                attrRepo -> isSpecificConf(attrRepo.getConf(), StubAttrRepoConf.class)
                && attrRepo.getKey().equals("DefaultStubAttrRepo")));
        assertTrue(attrRepoTOs.stream().anyMatch(
                attrRepo -> isSpecificConf(attrRepo.getConf(), SyncopeAttrRepoConf.class)
                && attrRepo.getKey().equals("DefaultSyncopeAttrRepo")));
    }

    @Test
    public void getLDAPAttrRepo() {
        AttrRepoTO attrRepoTO = ATTR_REPO_SERVICE.read("DefaultLDAPAttrRepo");

        assertNotNull(attrRepoTO);
        assertTrue(StringUtils.isNotBlank(attrRepoTO.getDescription()));
        assertTrue(isSpecificConf(attrRepoTO.getConf(), LDAPAttrRepoConf.class));
        assertFalse(isSpecificConf(attrRepoTO.getConf(), JDBCAttrRepoConf.class));
    }

    @Test
    public void getJDBCAttrRepo() {
        AttrRepoTO attrRepoTO = ATTR_REPO_SERVICE.read("DefaultJDBCAttrRepo");

        assertNotNull(attrRepoTO);
        assertTrue(StringUtils.isNotBlank(attrRepoTO.getDescription()));
        assertTrue(isSpecificConf(attrRepoTO.getConf(), JDBCAttrRepoConf.class));
        assertFalse(isSpecificConf(attrRepoTO.getConf(), LDAPAttrRepoConf.class));
    }

    @Test
    public void getStubAttrRepo() {
        AttrRepoTO attrRepoTO = ATTR_REPO_SERVICE.read("DefaultStubAttrRepo");

        assertNotNull(attrRepoTO);
        assertTrue(StringUtils.isNotBlank(attrRepoTO.getDescription()));
        assertTrue(isSpecificConf(attrRepoTO.getConf(), StubAttrRepoConf.class));
        assertFalse(isSpecificConf(attrRepoTO.getConf(), SyncopeAttrRepoConf.class));
    }

    @Test
    public void getSyncopeAttrRepo() {
        AttrRepoTO attrRepoTO = ATTR_REPO_SERVICE.read("DefaultSyncopeAttrRepo");

        assertNotNull(attrRepoTO);
        assertTrue(StringUtils.isNotBlank(attrRepoTO.getDescription()));
        assertTrue(isSpecificConf(attrRepoTO.getConf(), SyncopeAttrRepoConf.class));
        assertFalse(isSpecificConf(attrRepoTO.getConf(), StubAttrRepoConf.class));
    }

    @Test
    public void create() {
        EnumSet.allOf(AttrRepoSupportedType.class).forEach(type -> {
            AttrRepoTO attrRepoTO = createAttrRepo(buildAttrRepoTO(type));
            assertNotNull(attrRepoTO);
            assertTrue(attrRepoTO.getDescription().contains("A test " + type + " attr repo"));
            assertEquals(2, attrRepoTO.getItems().size());
        });
    }

    @Test
    public void updateLDAPAttrRepo() {
        AttrRepoTO ldapAttrRepoTO = ATTR_REPO_SERVICE.read("DefaultLDAPAttrRepo");
        assertNotNull(ldapAttrRepoTO);

        AttrRepoTO newLdapAttrRepoTO = buildAttrRepoTO(AttrRepoSupportedType.LDAP);
        newLdapAttrRepoTO = createAttrRepo(newLdapAttrRepoTO);
        assertNotNull(newLdapAttrRepoTO);

        AttrRepoConf conf = ldapAttrRepoTO.getConf();
        assertNotNull(conf);
        LDAPAttrRepoConf.class.cast(conf).setSubtreeSearch(false);
        newLdapAttrRepoTO.setConf(conf);

        // update new attr repo
        ATTR_REPO_SERVICE.update(newLdapAttrRepoTO);
        newLdapAttrRepoTO = ATTR_REPO_SERVICE.read(newLdapAttrRepoTO.getKey());
        assertNotNull(newLdapAttrRepoTO);

        conf = newLdapAttrRepoTO.getConf();
        assertFalse(LDAPAttrRepoConf.class.cast(conf).isSubtreeSearch());
    }

    @Test
    public void updateJDBCAttrRepo() {
        AttrRepoTO jdbcAttrRepoTO = ATTR_REPO_SERVICE.read("DefaultJDBCAttrRepo");
        assertNotNull(jdbcAttrRepoTO);

        AttrRepoTO newJDBCAttrRepoTO = buildAttrRepoTO(AttrRepoSupportedType.JDBC);
        newJDBCAttrRepoTO = createAttrRepo(newJDBCAttrRepoTO);
        assertNotNull(newJDBCAttrRepoTO);

        AttrRepoConf conf = jdbcAttrRepoTO.getConf();
        assertNotNull(conf);
        JDBCAttrRepoConf.class.cast(conf).setCaseCanonicalization(CaseCanonicalizationMode.UPPER);
        newJDBCAttrRepoTO.setConf(conf);

        // update new attr repo
        ATTR_REPO_SERVICE.update(newJDBCAttrRepoTO);
        newJDBCAttrRepoTO = ATTR_REPO_SERVICE.read(newJDBCAttrRepoTO.getKey());
        assertNotNull(newJDBCAttrRepoTO);

        conf = newJDBCAttrRepoTO.getConf();
        assertEquals(CaseCanonicalizationMode.UPPER, JDBCAttrRepoConf.class.cast(conf).getCaseCanonicalization());
    }

    @Test
    public void updateStubAttrRepo() {
        AttrRepoTO staticAttrRepoTO = ATTR_REPO_SERVICE.read("DefaultStubAttrRepo");
        assertNotNull(staticAttrRepoTO);

        AttrRepoTO newStubAttrRepoTO = buildAttrRepoTO(AttrRepoSupportedType.STUB);
        newStubAttrRepoTO = createAttrRepo(newStubAttrRepoTO);
        assertNotNull(newStubAttrRepoTO);

        AttrRepoConf conf = staticAttrRepoTO.getConf();
        assertNotNull(conf);
        assertEquals(1, StubAttrRepoConf.class.cast(conf).getAttributes().size());
        StubAttrRepoConf.class.cast(conf).getAttributes().put("attr3", "value3");
        newStubAttrRepoTO.setConf(conf);

        // update new attr repo
        ATTR_REPO_SERVICE.update(newStubAttrRepoTO);
        newStubAttrRepoTO = ATTR_REPO_SERVICE.read(newStubAttrRepoTO.getKey());
        assertNotNull(newStubAttrRepoTO);

        conf = newStubAttrRepoTO.getConf();
        assertEquals(2, StubAttrRepoConf.class.cast(conf).getAttributes().size());
    }

    @Test
    public void updateSyncopeAttrRepo() {
        AttrRepoTO syncopeAttrRepoTO = ATTR_REPO_SERVICE.read("DefaultSyncopeAttrRepo");
        assertNotNull(syncopeAttrRepoTO);

        AttrRepoTO newSyncopeAttrRepoTO = buildAttrRepoTO(AttrRepoSupportedType.SYNCOPE);
        newSyncopeAttrRepoTO = createAttrRepo(newSyncopeAttrRepoTO);
        assertNotNull(newSyncopeAttrRepoTO);

        AttrRepoConf conf = syncopeAttrRepoTO.getConf();
        assertNotNull(conf);
        SyncopeAttrRepoConf.class.cast(conf).setDomain("Two");
        newSyncopeAttrRepoTO.setConf(conf);

        // update new attr repo
        ATTR_REPO_SERVICE.update(newSyncopeAttrRepoTO);
        newSyncopeAttrRepoTO = ATTR_REPO_SERVICE.read(newSyncopeAttrRepoTO.getKey());
        assertNotNull(newSyncopeAttrRepoTO);

        conf = newSyncopeAttrRepoTO.getConf();
        assertEquals("Two", SyncopeAttrRepoConf.class.cast(conf).getDomain());
    }

    @Test
    public void delete() throws IOException {
        EnumSet.allOf(AttrRepoSupportedType.class).forEach(type -> {
            AttrRepoTO read = createAttrRepo(buildAttrRepoTO(type));
            assertNotNull(read);

            ATTR_REPO_SERVICE.delete(read.getKey());

            try {
                ATTR_REPO_SERVICE.read(read.getKey());
                fail("This should not happen");
            } catch (SyncopeClientException e) {
                assertNotNull(e);
            }
        });
    }
}

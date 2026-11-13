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
package org.apache.syncope.common.lib.attr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.apache.syncope.common.lib.to.AttrRepoTO;
import org.junit.jupiter.api.Test;

class AttrRepoConfTest {

    private final AttrRepoTO attrRepo = new AttrRepoTO();

    private final AttrRepoConf.Mapper mapper = mock(AttrRepoConf.Mapper.class);

    @Test
    void stub() {
        StubAttrRepoConf conf = new StubAttrRepoConf();
        conf.getAttributes().put("cn", "value");

        Map<String, Object> expected = Map.of("stub", conf);
        when(mapper.map(attrRepo, conf)).thenReturn(expected);

        assertSame(expected, conf.map(attrRepo, mapper));
        verify(mapper).map(attrRepo, conf);
        assertEquals("value", conf.getAttributes().get("cn"));
    }

    @Test
    void ldap() {
        LDAPAttrRepoConf conf = new LDAPAttrRepoConf();
        conf.setUseAllQueryAttributes(false);
        conf.getQueryAttributes().put("uid", "cn");

        Map<String, Object> expected = Map.of("ldap", conf);
        when(mapper.map(attrRepo, conf)).thenReturn(expected);

        assertSame(expected, conf.map(attrRepo, mapper));
        verify(mapper).map(attrRepo, conf);
        assertFalse(conf.isUseAllQueryAttributes());
        assertEquals("cn", conf.getQueryAttributes().get("uid"));
    }

    @Test
    void jdbc() {
        JDBCAttrRepoConf conf = new JDBCAttrRepoConf();
        conf.setSingleRow(false);
        conf.setRequireAllAttributes(false);
        conf.setQueryType(JDBCAttrRepoConf.QueryType.OR);
        conf.getColumnMappings().put("cn", "name");
        conf.getUsername().add("uid");

        Map<String, Object> expected = Map.of("jdbc", conf);
        when(mapper.map(attrRepo, conf)).thenReturn(expected);

        assertSame(expected, conf.map(attrRepo, mapper));
        verify(mapper).map(attrRepo, conf);
        assertFalse(conf.isSingleRow());
        assertFalse(conf.isRequireAllAttributes());
        assertEquals(JDBCAttrRepoConf.QueryType.OR, conf.getQueryType());
        assertEquals("name", conf.getColumnMappings().get("cn"));
        assertEquals(1, conf.getUsername().size());
    }

    @Test
    void syncope() {
        SyncopeAttrRepoConf conf = new SyncopeAttrRepoConf();
        conf.setDomain("domain");
        conf.setSearchFilter("username==rossini");
        conf.setBasicAuthUsername("user");
        conf.setBasicAuthPassword("pass");
        conf.getHeaders().put("X-Test", "value");

        Map<String, Object> expected = Map.of("syncope", conf);
        when(mapper.map(attrRepo, conf)).thenReturn(expected);

        assertSame(expected, conf.map(attrRepo, mapper));
        verify(mapper).map(attrRepo, conf);
        assertEquals("domain", conf.getDomain());
        assertEquals("username==rossini", conf.getSearchFilter());
        assertEquals("user", conf.getBasicAuthUsername());
        assertEquals("pass", conf.getBasicAuthPassword());
        assertEquals("value", conf.getHeaders().get("X-Test"));
    }

    @Test
    void azureActiveDirectory() {
        AzureActiveDirectoryAttrRepoConf conf = new AzureActiveDirectoryAttrRepoConf();
        conf.setCaseInsensitive(true);
        conf.setClientId("clientId");
        conf.setTenant("tenant");

        Map<String, Object> expected = Map.of("azure", conf);
        when(mapper.map(attrRepo, conf)).thenReturn(expected);

        assertSame(expected, conf.map(attrRepo, mapper));
        verify(mapper).map(attrRepo, conf);
        assertTrue(conf.isCaseInsensitive());
        assertEquals("clientId", conf.getClientId());
        assertEquals("tenant", conf.getTenant());
        assertTrue(conf.getScope().contains("openid"));
    }

    @Test
    void okta() {
        OktaAttrRepoConf conf = new OktaAttrRepoConf();
        conf.setUsernameAttribute("email");
        conf.setApiToken("token");

        Map<String, Object> expected = Map.of("okta", conf);
        when(mapper.map(attrRepo, conf)).thenReturn(expected);

        assertSame(expected, conf.map(attrRepo, mapper));
        verify(mapper).map(attrRepo, conf);
        assertEquals("email", conf.getUsernameAttribute());
        assertEquals("token", conf.getApiToken());
        assertTrue(conf.getScopes().contains("okta.users.read"));
        conf.setOrganizationUrl("https://example.com");
        assertEquals("https://example.com", conf.getOrganizationUrl());
    }
}

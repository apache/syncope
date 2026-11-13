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
package org.apache.syncope.common.lib.password;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.apache.syncope.common.lib.to.PasswordManagementTO;
import org.junit.jupiter.api.Test;

class PasswordManagementConfTest {

    private final PasswordManagementTO passwordManagementTO = new PasswordManagementTO();

    private final PasswordManagementConf.Mapper mapper = mock(PasswordManagementConf.Mapper.class);

    @Test
    void syncope() {
        SyncopePasswordManagementConf conf = new SyncopePasswordManagementConf();
        conf.setSearchFilter("username==rossini");

        Map<String, Object> expected = Map.of("syncope", conf);
        when(mapper.map(passwordManagementTO, conf)).thenReturn(expected);

        assertSame(expected, conf.map(passwordManagementTO, mapper));
        verify(mapper).map(passwordManagementTO, conf);
        assertEquals("username==rossini", conf.getSearchFilter());
    }

    @Test
    void ldap() {
        LDAPPasswordManagementConf conf = new LDAPPasswordManagementConf();
        conf.setSearchFilter("cn={user}");

        Map<String, Object> expected = Map.of("ldap", conf);
        when(mapper.map(passwordManagementTO, conf)).thenReturn(expected);

        assertSame(expected, conf.map(passwordManagementTO, mapper));
        verify(mapper).map(passwordManagementTO, conf);
        assertEquals("cn={user}", conf.getSearchFilter());
    }

    @Test
    void jdbc() {
        JDBCPasswordManagementConf conf = new JDBCPasswordManagementConf();
        conf.setSql("SELECT 1");

        Map<String, Object> expected = Map.of("jdbc", conf);
        when(mapper.map(passwordManagementTO, conf)).thenReturn(expected);

        assertSame(expected, conf.map(passwordManagementTO, mapper));
        verify(mapper).map(passwordManagementTO, conf);
        assertEquals("SELECT 1", conf.getSql());
    }

    @Test
    void rest() {
        RESTPasswordManagementConf conf = new RESTPasswordManagementConf();
        conf.setEndpointUrlUser("https://example.com");

        Map<String, Object> expected = Map.of("rest", conf);
        when(mapper.map(passwordManagementTO, conf)).thenReturn(expected);

        assertSame(expected, conf.map(passwordManagementTO, mapper));
        verify(mapper).map(passwordManagementTO, conf);
        assertEquals("https://example.com", conf.getEndpointUrlUser());
    }
}

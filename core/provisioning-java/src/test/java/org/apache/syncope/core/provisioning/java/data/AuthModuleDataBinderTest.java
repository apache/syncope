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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AuthModuleTO;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.types.AuthModuleState;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.core.persistence.api.dao.AuthModuleDAO;
import org.apache.syncope.core.persistence.api.entity.am.AuthModule;
import org.apache.syncope.core.provisioning.api.data.AuthModuleDataBinder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AuthModuleDataBinderTest extends AbstractDataBinderTest {

    @Autowired
    private AuthModuleDataBinder binder;

    @Autowired
    private AuthModuleDAO authModuleDAO;

    @Test
    void get() {
        AuthModuleTO authModule = binder.getAuthModuleTO(
                authModuleDAO.findById("DefaultLDAPAuthModule").orElseThrow());
        assertEquals("DefaultLDAPAuthModule", authModule.getKey());
        assertEquals("LDAP auth module", authModule.getDescription());
        assertEquals(AuthModuleState.ACTIVE, authModule.getState());
        assertEquals(4, authModule.getItems().size());
        assertTrue(authModule.getItems().stream().
                anyMatch(item -> "mail".equals(item.getIntAttrName())
                && "email".equals(item.getExtAttrName())
                && MappingPurpose.NONE == item.getPurpose()));
    }

    @Test
    void createUpdate() {
        AuthModuleTO authModuleTO = new AuthModuleTO();
        authModuleTO.setKey("testAuthModule");
        authModuleTO.setDescription("a test auth module");
        authModuleTO.setState(AuthModuleState.STANDBY);
        authModuleTO.setOrder(3);
        authModuleTO.setConf(binder.getAuthModuleTO(
                authModuleDAO.findById("DefaultJDBCAuthModule").orElseThrow()).getConf());

        Item item = new Item();
        item.setIntAttrName("username");
        item.setExtAttrName("uid");
        item.setConnObjectKey(true);
        item.setMandatoryCondition("true");
        authModuleTO.getItems().add(item);

        AuthModule authModule = authModuleDAO.save(binder.create(authModuleTO));

        AuthModuleTO created = binder.getAuthModuleTO(authModule);
        assertEquals("testAuthModule", created.getKey());
        assertEquals("a test auth module", created.getDescription());
        assertEquals(AuthModuleState.STANDBY, created.getState());
        assertEquals(3, created.getOrder());
        assertEquals(1, created.getItems().size());
        assertTrue(created.getItems().getFirst().isConnObjectKey());

        created.setDescription("an updated test auth module");
        created.getItems().getFirst().setExtAttrName("username");
        binder.update(authModule, created);

        AuthModuleTO updated = binder.getAuthModuleTO(authModule);
        assertEquals("an updated test auth module", updated.getDescription());
        assertEquals("username", updated.getItems().getFirst().getExtAttrName());
    }

    @Test
    void invalidItems() {
        AuthModuleTO noIntAttrName = new AuthModuleTO();
        noIntAttrName.setKey("testAuthModule");
        noIntAttrName.getItems().add(new Item());
        assertThrows(SyncopeClientException.class, () -> binder.create(noIntAttrName));

        AuthModuleTO invalidMandatoryCondition = new AuthModuleTO();
        invalidMandatoryCondition.setKey("testAuthModule");
        Item item = new Item();
        item.setIntAttrName("username");
        item.setMandatoryCondition("1 +");
        invalidMandatoryCondition.getItems().add(item);
        assertThrows(SyncopeClientException.class, () -> binder.create(invalidMandatoryCondition));
    }
}

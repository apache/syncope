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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.syncope.common.lib.to.PasswordManagementTO;
import org.apache.syncope.core.persistence.api.dao.PasswordManagementDAO;
import org.apache.syncope.core.persistence.api.entity.am.PasswordManagement;
import org.apache.syncope.core.provisioning.api.data.PasswordManagementDataBinder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PasswordManagementDataBinderTest extends AbstractDataBinderTest {

    @Autowired
    private PasswordManagementDataBinder binder;

    @Autowired
    private PasswordManagementDAO passwordManagementDAO;

    @Test
    void get() {
        PasswordManagementTO passwordManagement = binder.getPasswordManagementTO(
                passwordManagementDAO.findById("DefaultSyncopePasswordManagement").orElseThrow());
        assertEquals("DefaultSyncopePasswordManagement", passwordManagement.getKey());
        assertEquals("Syncope password management module", passwordManagement.getDescription());
        assertTrue(passwordManagement.isEnabled());
    }

    @Test
    void createUpdate() {
        PasswordManagementTO passwordManagementTO = new PasswordManagementTO();
        passwordManagementTO.setKey("testPasswordManagement");
        passwordManagementTO.setDescription("a test password management");
        passwordManagementTO.setEnabled(false);
        passwordManagementTO.setConf(binder.getPasswordManagementTO(
                passwordManagementDAO.findById("DefaultSyncopePasswordManagement").orElseThrow()).getConf());

        PasswordManagement passwordManagement = passwordManagementDAO.save(binder.create(passwordManagementTO));

        PasswordManagementTO created = binder.getPasswordManagementTO(passwordManagement);
        assertEquals("testPasswordManagement", created.getKey());
        assertEquals("a test password management", created.getDescription());
        assertFalse(created.isEnabled());

        created.setDescription("an updated test password management");
        created.setEnabled(true);
        binder.update(passwordManagement, created);

        PasswordManagementTO updated = binder.getPasswordManagementTO(passwordManagement);
        assertEquals("an updated test password management", updated.getDescription());
        assertTrue(updated.isEnabled());
    }
}

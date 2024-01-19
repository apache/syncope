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
package org.apache.syncope.core.provisioning.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class DerAttrTest extends AbstractTest {

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private DerAttrHandler derAttrHandler;

    @Test
    public void derAttrFromSpecialAttrs() {
        DerSchema info = derSchemaDAO.findById("info").orElseThrow();
        assertEquals("username + ' - ' + creationDate + '[' + failedLogins + ']'", info.getExpression());

        User user = userDAO.findByUsername("vivaldi").orElseThrow();
        assertNotNull(user);

        String value = derAttrHandler.getValue(user, info);
        assertNotNull(value);
        assertFalse(value.isEmpty());
        assertTrue(value.startsWith("vivaldi - 2010-10-20"));
        assertTrue(value.endsWith("[0]"));
    }
}

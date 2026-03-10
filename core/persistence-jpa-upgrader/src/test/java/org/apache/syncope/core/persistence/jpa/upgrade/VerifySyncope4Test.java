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
package org.apache.syncope.core.persistence.jpa.upgrade;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.EntitlementsHolder;
import org.apache.syncope.common.lib.types.IdMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.group.GroupTypeExtension;
import org.apache.syncope.core.persistence.jpa.MasterDomain;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = { MasterDomain.class, PersistenceUpgraderTestContext.class })
class VerifySyncope4Test {

    @DynamicPropertySource
    static void configureProperties(final DynamicPropertyRegistry registry) {
        registry.add("DB_URL", GenerateUpgradeSQLTest.JDBC_URL_SUPPLIER);
        registry.add("DB_USER", GenerateUpgradeSQLTest.DB_CRED_SUPPLIER);
        registry.add("DB_PASSWORD", GenerateUpgradeSQLTest.DB_CRED_SUPPLIER);
    }

    @BeforeAll
    public static void init() {
        EntitlementsHolder.getInstance().addAll(IdRepoEntitlement.values());
        EntitlementsHolder.getInstance().addAll(IdMEntitlement.values());
        EntitlementsHolder.getInstance().addAll(AMEntitlement.values());
    }

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private AnyTypeClassDAO anyTypeClassDAO;

    @Test
    void groups() {
        Group group = groupDAO.findById("f779c0d4-633b-4be5-8f57-32eb478a3ca5").orElseThrow();
        assertFalse(group.getTypeExtensions().isEmpty());

        GroupTypeExtension te = group.getTypeExtension(anyTypeDAO.findById("PRINTER").orElseThrow()).orElseThrow();
        assertFalse(te.getAuxClasses().isEmpty());
        AnyTypeClass other = anyTypeClassDAO.findById("other").orElseThrow();
        assertTrue(te.getAuxClasses().stream().anyMatch(other::equals));
    }
}

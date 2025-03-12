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
package org.apache.syncope.common.keymaster.client.zookeeper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.UUID;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.KeymasterException;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.keymaster.client.api.model.JPADomain;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = { ZookeeperKeymasterClientContext.class, ZookeeperTestContext.class })
public class ZookeeperDomainOpsITCase {

    @Autowired
    private DomainOps domainOps;

    @Test
    public void crud() {
        String key = UUID.randomUUID().toString();

        domainOps.create(new JPADomain.Builder(key).
                jdbcDriver("org.h2.Driver").
                jdbcURL("jdbc:h2:mem:syncopetest;DB_CLOSE_DELAY=-1").
                dbUsername("sa").
                dbPassword("").
                databasePlatform("org.apache.openjpa.jdbc.sql.H2Dictionary").
                transactionIsolation(JPADomain.TransactionIsolation.TRANSACTION_READ_UNCOMMITTED).
                adminPassword("password").
                adminCipherAlgorithm(CipherAlgorithm.BCRYPT).
                build());

        JPADomain domain = (JPADomain) domainOps.read(key);
        assertEquals(JPADomain.TransactionIsolation.TRANSACTION_READ_UNCOMMITTED, domain.getTransactionIsolation());
        assertEquals("password", domain.getAdminPassword());
        assertEquals(CipherAlgorithm.BCRYPT, domain.getAdminCipherAlgorithm());
        assertEquals(10, domain.getPoolMaxActive());
        assertEquals(2, domain.getPoolMinIdle());

        List<Domain> list = domainOps.list();
        assertNotNull(list);
        assertEquals(domain, list.getFirst());

        try {
            domainOps.create(new JPADomain.Builder(domain.getKey()).build());
            fail();
        } catch (KeymasterException e) {
            assertNotNull(e);
        }

        domainOps.changeAdminPassword(key, "newpassword", CipherAlgorithm.SSHA512);

        domain = (JPADomain) domainOps.read(key);
        assertEquals("newpassword", domain.getAdminPassword());
        assertEquals(CipherAlgorithm.SSHA512, domain.getAdminCipherAlgorithm());

        domainOps.adjustPoolSize(key, 100, 23);

        domain = (JPADomain) domainOps.read(key);
        assertEquals(100, domain.getPoolMaxActive());
        assertEquals(23, domain.getPoolMinIdle());

        domainOps.delete(key);

        list = domainOps.list();
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void createMaster() {
        assertThrows(KeymasterException.class, () -> domainOps.create(
                new JPADomain.Builder(SyncopeConstants.MASTER_DOMAIN).build()));
    }
}

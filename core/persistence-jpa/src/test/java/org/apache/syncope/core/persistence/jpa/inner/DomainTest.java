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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.dao.DomainDAO;
import org.apache.syncope.core.persistence.api.entity.Domain;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class DomainTest extends AbstractTest {

    @Autowired
    private DomainDAO domainDAO;

    @Test
    public void find() {
        Domain two = domainDAO.find("Two");
        assertNotNull(two);
        assertEquals(CipherAlgorithm.SHA, two.getAdminCipherAlgorithm());

        assertNull(domainDAO.find("none"));
    }

    @Test
    public void findAll() {
        List<Domain> list = domainDAO.findAll();
        assertNotNull(list);
        assertFalse(list.isEmpty());
        for (Domain domain : list) {
            assertNotNull(domain);
        }
    }

    @Test
    public void save() {
        Domain domain = entityFactory.newEntity(Domain.class);
        domain.setKey("new");
        domain.setPassword("password", CipherAlgorithm.SSHA512);

        Domain actual = domainDAO.save(domain);
        assertNotNull(actual);
        assertEquals(CipherAlgorithm.SSHA512, actual.getAdminCipherAlgorithm());
        assertNotEquals("password", actual.getAdminPwd());
    }

    @Test
    public void delete() {
        Domain domain = entityFactory.newEntity(Domain.class);
        domain.setKey("todelete");
        domain.setPassword("password", CipherAlgorithm.SSHA512);

        Domain actual = domainDAO.save(domain);
        assertNotNull(actual);

        String id = actual.getKey();
        assertNotNull(domainDAO.find(id));

        domainDAO.delete(id);
        assertNull(domainDAO.find(id));
    }
}
